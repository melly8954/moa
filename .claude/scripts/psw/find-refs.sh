#!/usr/bin/env bash
# ID를 참조하는 곳을 찾는다 (harness-psw 1.2, 7.1, 10.5).
# 참조는 하위 → 상위로만 적으므로, 이 검색이 역방향(영향 범위)을 알려준다.
# 사용법: find-refs.sh ID [ID...]
#   접두사만 주면 해당 계열 전체를 찾는다 (예: FR-ORD-)
set -euo pipefail

if [[ $# -eq 0 ]]; then
  echo "사용법: find-refs.sh ID [ID...]" >&2
  exit 1
fi

root="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "git 저장소 안에서 실행해야 합니다" >&2; exit 1; }
cd "$root"

for id in "$@"; do
  echo "== $id =="
  # 아직 커밋하지 않은 파일도 찾는다. .gitignore 대상과 .claude/는 제외한다.
  git grep --untracked -n -F -e "$id" -- . ':!.claude' || echo "(참조 없음)"
done
