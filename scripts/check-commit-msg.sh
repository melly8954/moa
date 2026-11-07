#!/usr/bin/env bash
# 커밋 메시지 형식을 검사한다. 규칙은 docs/git.md (없는 저장소는 CLAUDE.md).
#
# 사용법
#   commit-msg hook: check-commit-msg.sh <메시지 파일>
#   CI:              check-commit-msg.sh --commit <커밋>
# 통과하면 0, 아니면 1.
set -uo pipefail

if [[ "${1:-}" == "--commit" ]]; then
  msg="$(git log -1 --format=%B "${2:?커밋을 지정한다}")"
else
  msg=""
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ "$line" == \#* ]] && continue
    msg+="$line"$'\n'
  done <"${1:?메시지 파일을 지정한다}"
fi

errors=0
err()  { echo "커밋 검사 오류: $*" >&2; errors=$((errors + 1)); }
warn() { echo "커밋 검사 경고: $*" >&2; }
clen() { local LC_ALL=C.UTF-8; len=${#1}; }   # 글자 수를 len에 담는다

mapfile -t msg_lines <<<"$msg"
subject="${msg_lines[0]:-}"

# merge, revert, fixup 커밋은 검사하지 않는다
if [[ "$subject" =~ ^(Merge|Revert|fixup!|squash!) ]]; then
  exit 0
fi

types='feat|fix|design|style|refactor|test|docs|chore'
# 저장소가 종류를 따로 정하면 .githooks/commit-types(한 줄, |로 구분)를 따른다
types_file="$(git rev-parse --show-toplevel)/.githooks/commit-types"
[[ -f "$types_file" ]] && types="$(tr -d '[:space:]' <"$types_file")"
if [[ "$subject" =~ ^($types)\( ]]; then
  err "scope를 쓰지 않는다: $subject"
elif ! [[ "$subject" =~ ^($types):\ .+ ]]; then
  err "제목 형식은 '<type>: <제목>'이다 (type: ${types//|/, }): $subject"
fi
clen "$subject"
if (( len > 72 )); then
  err "제목이 72자를 넘는다 (${len}자)"
elif (( len > 50 )); then
  warn "제목은 50자 이내를 권장한다 (${len}자)"
fi

if (( ${#msg_lines[@]} > 1 )) && [[ -n "${msg_lines[1]}" ]]; then
  err "제목 다음 줄은 비운다"
fi

mapfile -t trailer_lines < <(git interpret-trailers --parse <<<"$msg")
declare -A is_trailer=()
for t in "${trailer_lines[@]}"; do
  [[ -z "$t" ]] && continue
  is_trailer["$t"]=1
  case "${t%%:*}" in
    Co-Authored-By|Signed-off-by|Refs|Closes) ;;
    *) warn "알 수 없는 트레일러: ${t%%:*}" ;;
  esac
done

for line in "${msg_lines[@]:2}"; do
  [[ -z "$line" || -n "${is_trailer["$line"]:-}" ]] && continue
  clen "$line"
  (( len > 100 )) && warn "본문 한 줄이 100자를 넘는다: ${line:0:30}..."
done

if (( errors > 0 )); then
  rules=CLAUDE.md
  [[ -f "$(git rev-parse --show-toplevel)/docs/git.md" ]] && rules=docs/git.md
  echo "커밋 규칙: $rules" >&2
  exit 1
fi
exit 0
