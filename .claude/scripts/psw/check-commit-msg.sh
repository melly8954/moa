#!/usr/bin/env bash
# 커밋 메시지와 트레일러를 검사한다 (harness-psw 5.4, 1.1, 10.5).
#
# 사용법
#   commit-msg hook: check-commit-msg.sh <메시지 파일>          (스테이징된 변경 기준)
#   CI:              check-commit-msg.sh --commit <커밋>          (그 커밋의 변경 기준)
# 통과하면 0, 아니면 1.
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
  msg="$(grep -vE '^#' "$msgfile")"
  base="$(git rev-parse --verify -q HEAD || true)"
  new_ref=""   # 인덱스
  changed="$(git diff --cached --name-status)"
fi

errors=0
err()  { echo "커밋 검사 오류: $*" >&2; errors=$((errors + 1)); }
warn() { echo "커밋 검사 경고: $*" >&2; }
clen() { LC_ALL=C.UTF-8 awk '{print length($0)}' <<<"$1"; }

subject="$(sed -n '1p' <<<"$msg")"

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
len="$(clen "$subject")"
if (( len > 72 )); then
  err "제목이 72자를 넘는다 (${len}자)"
elif (( len > 50 )); then
  warn "제목은 50자 이내를 권장한다 (${len}자)"
fi
type="${subject%%:*}"

# ---------- 트레일러 ----------
trailers="$(git interpret-trailers --parse <<<"$msg")"
trailer_values() { # trailer_values <키> → 쉼표로 나눈 값 목록
  sed -nE "s/^$1:[[:space:]]*//p" <<<"$trailers" | tr ',' '\n' | sed -E 's/^[[:space:]]+//; s/[[:space:]]+$//' | sed '/^$/d'
}
has_trailer() { grep -qE "^$1:" <<<"$trailers"; }

# 본문 줄 길이 (트레일러 제외)
body="$(sed '1,2d' <<<"$msg")"
while IFS= read -r line; do
  [[ -z "$line" ]] && continue
  grep -qxF "$line" <<<"$trailers" && continue
  (( $(clen "$line") > 100 )) && warn "본문 한 줄이 100자를 넘는다: ${line:0:30}..."
done <<<"$body"

while IFS= read -r t; do
  [[ -z "$t" ]] && continue
  key="${t%%:*}"
  case "$key" in
    Refs|Closes|Co-Authored-By|Signed-off-by) ;;
    *) warn "알 수 없는 트레일러: $key" ;;
  esac
done <<<"$trailers"

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
while IFS= read -r v; do
  [[ -z "$v" ]] && continue
  if [[ "$v" =~ ^((FR|NFR|SEC|INT|DAT)-[A-Z0-9]+-[0-9]+|CON-[0-9]+)$ ]]; then
    exists_in "$v" docs || err "Refs의 $v 가 문서에 없다"
  elif [[ "$v" =~ ^(FR|NFR|SEC|INT|DAT)-[A-Z0-9]+$ ]]; then
    exists_in "$v-" docs || err "Refs의 접두사 $v 가 문서에 없다"
  else
    err "Refs에는 요구사항 ID나 접두사만 쓴다: $v"
  fi
done < <(trailer_values Refs)

while IFS= read -r v; do
  [[ -z "$v" ]] && continue
  [[ "$v" =~ ^OPEN-[0-9]{3,}$ ]] || { err "Closes 형식이 아니다: $v"; continue; }
  exists_in "## $v" docs/open-question.md || err "Closes의 $v 항목이 docs/open-question.md에 없다"
done < <(trailer_values Closes)

# ---------- 필수 트레일러 ----------
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
    old_status="$(git show "$base:$path" 2>/dev/null | grep -m1 -oE 'status:[[:space:]]*[a-z]+' | sed -E 's/status:[[:space:]]*//' || true)"
  fi
  [[ "$old_status" == "approved" ]] || continue
  if [[ "$st" == D* ]]; then
    approved_touched+=("$path(삭제)")
    continue
  fi
  if [[ -n "$new_ref" ]]; then
    new_content="$(git show "$new_ref:$path" 2>/dev/null)"
  else
    new_content="$(git show ":$path" 2>/dev/null)"
  fi
  new_status="$(grep -m1 -oE 'status:[[:space:]]*[a-z]+' <<<"$new_content" | sed -E 's/status:[[:space:]]*//' || true)"
  if [[ "$new_status" == "approved" ]]; then
    err "approved 문서를 고치면 status를 draft로 되돌린다: $path"
  else
    approved_touched+=("$path")
  fi
done <<<"$changed"

if (( code_changed )) && [[ "$type" == "feat" || "$type" == "fix" ]]; then
  has_trailer Refs || err "feat·fix 코드 변경에는 Refs 트레일러가 필요하다"
fi
if (( spec_changed )); then
  has_trailer Refs || has_trailer Closes || err "REQ·설계 문서 변경에는 Refs 또는 Closes 트레일러가 필요하다"
fi
if [[ ${#approved_touched[@]} -gt 0 ]]; then
  warn "approved 문서를 바꿨다. 사용자가 수락한 변경인지 확인하고 다시 승인받는다 (harness-psw 7.3): ${approved_touched[*]}"
fi

if (( errors > 0 )); then
  echo "커밋 규칙: harness-psw 5.4 (docs/design/conventions.md)" >&2
  exit 1
fi
exit 0
