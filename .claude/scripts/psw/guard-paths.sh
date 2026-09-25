#!/usr/bin/env bash
# 역할별 편집 경로와 approved 전환을 막는다 (harness-psw 1.1, 9.4, 10.5).
#
# hook 모드: guard-paths.sh hook
#   Claude Code PreToolUse hook 입력(JSON)을 stdin으로 받는다.
#   하위 에이전트 안이면 agent_type을 역할로 쓴다. 막으면 exit 2.
# 검사 모드: guard-paths.sh check <역할> <경로...>
#   규칙을 손으로 확인할 때 쓴다. 막으면 exit 1.
#
# 역할
#   implementer  테스트 파일, docs/, .claude/, CLAUDE.md 편집 금지
#   verifier     테스트 파일만 편집 가능
#   reviewer     편집 금지
#   그 외        역할 제한 없음
# 모든 역할(메인 세션 포함)
#   - 파일에 "status: approved"를 새로 쓰지 못한다
#   - approve.sh를 실행하지 못한다
#   → 승인은 사용자가 직접 approve.sh를 실행해서 한다
set -uo pipefail

normalize_root() {
  local r="${1//\\//}"
  echo "${r%/}"
}

root="$(normalize_root "${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}")"

# 파일이 속한 git 작업 폴더를 루트로 쓴다 (worktree를 쓰더라도 올바른 상대 경로가 된다)
root_of() {
  local d="${1//\\//}"
  d="${d%/*}"
  while [[ -n "$d" && ! -d "$d" ]]; do d="${d%/*}"; done
  [[ -z "$d" ]] && { echo "$root"; return; }
  local top
  top="$(git -C "$d" rev-parse --show-toplevel 2>/dev/null)" || { echo "$root"; return; }
  if command -v cygpath >/dev/null 2>&1; then top="$(cygpath -m "$top")"; fi
  normalize_root "$top"
}

# 테스트 파일 패턴. 프로젝트별로 .claude/psw.conf의 PSW_TEST_GLOBS로 바꾼다.
PSW_TEST_GLOBS='tests/* test/* e2e/* __tests__/* */__tests__/* *.test.* *.spec.*'
[[ -f "$root/.claude/psw.conf" ]] && source "$root/.claude/psw.conf"

is_test_path() {
  local p="$1" g
  local -a globs
  # 패턴을 파일 이름으로 펼치지 않고 나눈다 (backend/src/test/*가 실제 폴더 목록으로 바뀌면 안 된다)
  read -r -a globs <<<"$PSW_TEST_GLOBS"
  for g in "${globs[@]}"; do
    # shellcheck disable=SC2053
    [[ "$p" == $g ]] && return 0
  done
  return 1
}

# 역할이 경로를 편집할 수 없으면 사유를 출력하고 1을 돌려준다.
deny_reason() {
  local role="$1" p="$2"
  case "$role" in
    implementer)
      if is_test_path "$p"; then echo "구현자는 테스트 파일을 고치지 않는다 ($p)"; return 1; fi
      case "$p" in
        docs/*|.claude/*|CLAUDE.md) echo "구현자는 문서·설정을 고치지 않는다 ($p)"; return 1 ;;
      esac
      ;;
    verifier)
      if is_test_path "$p"; then return 0; fi
      echo "검증자는 테스트 파일만 고친다 ($p)"; return 1
      ;;
    reviewer)
      echo "검토자는 파일을 고치지 않는다 ($p)"; return 1
      ;;
  esac
  return 0
}

to_relative() {
  local p="${1//\\//}"
  local base="$root"
  case "$p" in
    /*|[A-Za-z]:/*) base="$(root_of "$p")" ;;
  esac
  local lp="${p,,}" lb="${base,,}"
  if [[ "$lp" == "$lb/"* ]]; then
    p="${p:${#base}+1}"
  fi
  p="${p#./}"
  echo "$p"
}

mode="${1:-}"

if [[ "$mode" == "check" ]]; then
  role="${2:-}"
  shift 2 || true
  status=0
  for p in "$@"; do
    reason="$(deny_reason "$role" "$(to_relative "$p")")" || { echo "$reason"; status=1; }
  done
  exit $status
fi

if [[ "$mode" != "hook" ]]; then
  echo "사용법: guard-paths.sh hook | guard-paths.sh check <역할> <경로...>" >&2
  exit 1
fi

input="$(cat)"

json_string() { # json_string <키> : 첫 번째 문자열 값 (이스케이프는 \\ → \ , \" → " 만 되돌린다)
  grep -oE "\"$1\"[[:space:]]*:[[:space:]]*\"([^\"\\\\]|\\\\.)*\"" <<<"$input" | head -1 \
    | sed -E "s/^\"$1\"[[:space:]]*:[[:space:]]*\"//; s/\"\$//; s/\\\\\\\\/\\\\/g; s/\\\\\"/\"/g"
}

block() {
  echo "harness-psw: $1" >&2
  exit 2
}

tool="$(json_string tool_name)"
role="$(json_string agent_type)"

if [[ "$tool" == "Bash" ]]; then
  cmd="$(json_string command)"
  [[ "$cmd" == *approve.sh* ]] && block "approve.sh는 사용자가 직접 실행한다. 에이전트는 승인을 대신하지 않는다"
  exit 0
fi

# 새로 쓰는 내용(new_string, content)에 approved가 있으면 막는다
if grep -oE '"(new_string|content|new_source)"[[:space:]]*:[[:space:]]*"([^"\\]|\\.)*"' <<<"$input" \
    | grep -qE 'status:[[:space:]]*approved'; then
  block "문서를 approved로 바꾸는 것은 사용자만 한다 (approve.sh)"
fi

path="$(json_string file_path)"
[[ -z "$path" ]] && path="$(json_string notebook_path)"
[[ -z "$path" ]] && exit 0

reason="$(deny_reason "$role" "$(to_relative "$path")")" || block "$reason"
exit 0
