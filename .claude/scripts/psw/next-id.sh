#!/usr/bin/env bash
# 다음 ID를 출력한다 (harness-psw 1.2, 10.5).
# 삭제된 ID도 git 이력에서 찾아 재사용하지 않는다.
# 사용법: next-id.sh <종류>
#   미결:     OPEN
#   요구사항: FR-<도메인> | NFR-<영역> | SEC-<영역> | INT-<시스템> | DAT-<영역> | CON
#   화면:     SCR-<도메인>
set -euo pipefail

type="${1:-}"
case "$type" in
  OPEN|CON) ;;
  *)
    if ! [[ "$type" =~ ^(FR|NFR|SEC|INT|DAT|SCR)-[A-Z0-9]+$ ]]; then
      echo "사용법: next-id.sh OPEN|CON|FR-<코드>|NFR-<코드>|SEC-<코드>|INT-<코드>|DAT-<코드>|SCR-<코드>" >&2
      exit 1
    fi
    ;;
esac
width=3

root="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "git 저장소 안에서 실행해야 합니다" >&2; exit 1; }
cd "$root"

pattern="\b${type}-[0-9]+"

# 현재 문서, 과거 문서 내용(지운 줄 포함), 커밋 메시지에서 가장 큰 번호를 찾는다.
max="$(
  {
    grep -rhoE "$pattern" docs 2>/dev/null || true
    git log --all --format= -p -- docs 2>/dev/null || true
    git log --all --format=%B 2>/dev/null || true
  } | grep -oE "$pattern" | sed "s/^${type}-//" | sort -n | tail -1 || true
)"
max="${max:-0}"

printf '%s-%0*d\n' "$type" "$width" "$((10#$max + 1))"
