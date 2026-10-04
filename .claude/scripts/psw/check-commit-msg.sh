#!/usr/bin/env bash
# 커밋 메시지와 트레일러를 검사한다 (harness-psw 5.4, 1.1, 10.5).
#
# 사용법
#   commit-msg hook: check-commit-msg.sh <메시지 파일>          (스테이징된 변경 기준)
#   CI:              check-commit-msg.sh --commit <커밋>          (그 커밋의 변경 기준)
# 통과하면 0, 아니면 1.
#
# Windows Git Bash는 프로세스 하나 띄우는 데 수십 ms, 바쁠 때는 1초 넘게 걸린다.
# 메시지 줄이나 ID마다 grep·sed·awk를 부르지 않고 Bash 안에서 처리한다.
# 외부 명령 수는 메시지 길이와 무관하고, 바뀐 REQ·설계 문서 하나마다 git show 2번만 늘어난다.
set -uo pipefail

root="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "git 저장소 안에서 실행해야 합니다" >&2; exit 1; }
cd "$root"

if [[ "${1:-}" == "--commit" ]]; then
  commit="${2:?커밋을 지정한다}"
  msg="$(git log -1 --format=%B "$commit")"
  base="$(git rev-parse --verify -q "$commit^" || true)"
  new_ref="$commit"
  changed="$(git diff-tree --no-commit-id --name-status -r --root "$commit")"
else
  msgfile="${1:?메시지 파일을 지정한다}"
  msg=""
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ "$line" == \#* ]] && continue
    msg+="$line"$'\n'
  done <"$msgfile"
  while [[ "$msg" == *$'\n' ]]; do msg="${msg%$'\n'}"; done
  base="$(git rev-parse --verify -q HEAD || true)"
  new_ref=""   # 인덱스
  changed="$(git diff --cached --name-status)"
fi

errors=0
err()  { echo "커밋 검사 오류: $*" >&2; errors=$((errors + 1)); }
warn() { echo "커밋 검사 경고: $*" >&2; }
clen() { local LC_ALL=C.UTF-8; len=${#1}; }   # 글자 수를 len에 담는다

mapfile -t msg_lines <<<"$msg"
subject="${msg_lines[0]:-}"

# merge, revert, fixup 커밋은 형식 검사를 하지 않는다
if [[ "$subject" =~ ^(Merge|Revert|fixup!|squash!) ]]; then
  exit 0
fi

# ---------- 제목 ----------
types='feat|fix|refactor|test|docs|chore|build|ci'
if [[ "$subject" =~ ^($types)\( ]]; then
  err "scope를 쓰지 않는다: $subject"
elif ! [[ "$subject" =~ ^($types):\ .+ ]]; then
  err "제목 형식은 '<type>: <제목>'이다 (type: ${types//|/, })"
fi
clen "$subject"
if (( len > 72 )); then
  err "제목이 72자를 넘는다 (${len}자)"
elif (( len > 50 )); then
  warn "제목은 50자 이내를 권장한다 (${len}자)"
fi
type="${subject%%:*}"

# ---------- 트레일러 ----------
mapfile -t trailer_lines < <(git interpret-trailers --parse <<<"$msg")
declare -A is_trailer=()
for t in "${trailer_lines[@]}"; do
  [[ -n "$t" ]] && is_trailer["$t"]=1
done
trailer_values() { # trailer_values <키> → 쉼표로 나눈 값을 vals 배열에 담는다
  vals=()
  local t rest v
  for t in "${trailer_lines[@]}"; do
    [[ "$t" == "$1:"* ]] || continue
    rest="${t#"$1:"},"
    while [[ -n "$rest" ]]; do
      v="${rest%%,*}"; rest="${rest#*,}"
      v="${v#"${v%%[![:space:]]*}"}"; v="${v%"${v##*[![:space:]]}"}"
      [[ -n "$v" ]] && vals+=("$v")
    done
  done
}
has_trailer() {
  local t
  for t in "${trailer_lines[@]}"; do [[ "$t" == "$1:"* ]] && return 0; done
  return 1
}

# 본문 줄 길이 (트레일러 제외)
for line in "${msg_lines[@]:2}"; do
  [[ -z "$line" ]] && continue
  [[ -n "${is_trailer["$line"]:-}" ]] && continue
  clen "$line"
  (( len > 100 )) && warn "본문 한 줄이 100자를 넘는다: ${line:0:30}..."
done

for t in "${trailer_lines[@]}"; do
  [[ -z "$t" ]] && continue
  key="${t%%:*}"
  case "$key" in
    Refs|Closes|Co-Authored-By|Signed-off-by) ;;
    *) warn "알 수 없는 트레일러: $key" ;;
  esac
done

# ID 존재 검사: 새 상태나 이전 상태에 있으면 인정한다 (해결된 OPEN은 이 커밋에서 지워진다)
exists_in() { # exists_in <문자열> <경로...>
  local needle="$1"; shift
  if [[ -n "$new_ref" ]]; then
    git grep -q -F -e "$needle" "$new_ref" -- "$@" 2>/dev/null && return 0
  else
    git grep -q --cached -F -e "$needle" -- "$@" 2>/dev/null && return 0
  fi
  [[ -n "$base" ]] && git grep -q -F -e "$needle" "$base" -- "$@" 2>/dev/null && return 0
  return 1
}

# Refs의 ID는 docs의 "하이픈을 낀 [A-Z0-9-] 덩어리"를 한 번에 뽑아 두고 그 안에서 찾는다.
# ID는 [A-Z0-9-]로만 이뤄지므로, docs에 ID가 들어 있으면 반드시 이 덩어리 중 하나 안에 있다.
# ID마다 git grep을 부르는 것과 결과가 같다.
new_tokens=""; base_tokens=""; new_loaded=0; base_loaded=0
id_tokens() { # id_tokens <git grep 대상 인자...>
  LC_ALL=C git grep -a -h -o -E '[A-Z0-9-]*-[A-Z0-9-]*' "$@" -- docs 2>/dev/null
}
ref_exists() { # ref_exists <ID 또는 접두사->
  if (( ! new_loaded )); then
    if [[ -n "$new_ref" ]]; then new_tokens=$'\n'"$(id_tokens "$new_ref")"$'\n'
    else new_tokens=$'\n'"$(id_tokens --cached)"$'\n'; fi
    new_loaded=1
  fi
  [[ "$new_tokens" == *"$1"* ]] && return 0
  [[ -z "$base" ]] && return 1
  if (( ! base_loaded )); then
    base_tokens=$'\n'"$(id_tokens "$base")"$'\n'
    base_loaded=1
  fi
  [[ "$base_tokens" == *"$1"* ]]
}
trailer_values Refs
for v in "${vals[@]}"; do
  if [[ "$v" =~ ^((FR|NFR|SEC|INT|DAT)-[A-Z0-9]+-[0-9]+|CON-[0-9]+)$ ]]; then
    ref_exists "$v" || err "Refs의 $v 가 문서에 없다"
  elif [[ "$v" =~ ^(FR|NFR|SEC|INT|DAT)-[A-Z0-9]+$ ]]; then
    ref_exists "$v-" || err "Refs의 접두사 $v 가 문서에 없다"
  else
    err "Refs에는 요구사항 ID나 접두사만 쓴다: $v"
  fi
done

trailer_values Closes
for v in "${vals[@]}"; do
  [[ "$v" =~ ^OPEN-[0-9]{3,}$ ]] || { err "Closes 형식이 아니다: $v"; continue; }
  exists_in "## $v" docs/open-question.md && continue
  # 이 커밋 안에서 등록하고 바로 해결해 어느 쪽에도 남지 않은 항목은 막지 않는다
  if [[ -n "$(git log --all -S "## $v" --format=%h -- docs/open-question.md 2>/dev/null)" ]]; then
    continue
  fi
  warn "Closes의 $v 항목을 open-question.md 어디에서도 찾지 못했다. 이 커밋에서 등록하고 바로 해결했다면 Closes를 빼도 된다"
done

# ---------- 필수 트레일러 ----------
status_of() { # status_of <문서 내용> → 첫 status: 값을 st_val에 담는다
  st_val=""
  [[ "$1" =~ status:[[:blank:]]*([a-z]+) ]] && st_val="${BASH_REMATCH[1]}"
  return 0
}
code_changed=0
spec_changed=0
approved_touched=()
while IFS=$'\t' read -r st path rest; do
  [[ -z "$st" ]] && continue
  [[ "$st" == R* || "$st" == C* ]] && path="$rest"
  case "$path" in
    docs/req/*.md|docs/design/*.md|docs/design/*.html) spec_changed=1 ;;
    docs/*|.claude/*|.githooks/*|CLAUDE.md|*/CLAUDE.md|*/AGENTS.md|README.md|.gitignore|.gitattributes|.env.example) ;;
    *) code_changed=1 ;;
  esac

  # approved 문서 변경 검사 (1.1)
  case "$path" in
    docs/req/*.md|docs/design/*.md|docs/design/*.html) ;;
    *) continue ;;
  esac
  old_status=""
  if [[ -n "$base" && "$st" != A* ]]; then
    status_of "$(git show "$base:$path" 2>/dev/null)"; old_status="$st_val"
  fi
  [[ "$old_status" == "approved" ]] || continue
  if [[ "$st" == D* ]]; then
    approved_touched+=("$path(삭제)")
    continue
  fi
  if [[ -n "$new_ref" ]]; then
    status_of "$(git show "$new_ref:$path" 2>/dev/null)"
  else
    status_of "$(git show ":$path" 2>/dev/null)"
  fi
  if [[ "$st_val" == "approved" ]]; then
    err "approved 문서를 고치면 status를 draft로 되돌린다: $path"
  else
    approved_touched+=("$path")
  fi
done <<<"$changed"

# 트레일러는 메시지 맨 끝 한 덩어리만 읽힌다. 빈 줄로 나뉜 앞 덩어리의 Refs·Closes는 무시된다
split_hint=""
if ! has_trailer Refs && ! has_trailer Closes; then
  for line in "${msg_lines[@]}"; do
    if [[ "$line" == Refs:* || "$line" == Closes:* ]]; then
      split_hint=" (Refs·Closes 줄이 있지만 트레일러로 읽히지 않았다. 트레일러는 빈 줄 없이 맨 끝 한 덩어리로 둔다)"
      break
    fi
  done
fi
if (( code_changed )) && [[ "$type" == "feat" || "$type" == "fix" ]]; then
  has_trailer Refs || err "feat·fix 코드 변경에는 Refs 트레일러가 필요하다$split_hint"
fi
if (( spec_changed )); then
  has_trailer Refs || has_trailer Closes || err "REQ·설계 문서 변경에는 Refs 또는 Closes 트레일러가 필요하다$split_hint"
fi
if [[ ${#approved_touched[@]} -gt 0 ]]; then
  warn "approved 문서를 바꿨다. 사용자가 수락한 변경인지 확인하고 다시 승인받는다 (harness-psw 7.3): ${approved_touched[*]}"
fi

if (( errors > 0 )); then
  echo "커밋 규칙: harness-psw 5.4 (docs/design/conventions.md)" >&2
  exit 1
fi
exit 0
