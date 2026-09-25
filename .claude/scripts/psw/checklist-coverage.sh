#!/usr/bin/env bash
# 인터뷰 체크리스트의 [필수] 항목이 처리됐는지 확인한다 (harness-psw 3.1, 10.5).
# 처리로 인정하는 것 (모두 체크리스트 ID로 찾는다)
#   - docs/req/ 안에 ID가 있음: 답을 반영한 자리의 주석(<!-- ck-dir-1 -->)이나 "해당 없음" 표의 행
#   - docs/open-question.md 항목에 ID가 있음 (미결로 등록됨)
# 모두 처리됐으면 0, 아니면 1로 끝난다.
# 사용법: checklist-coverage.sh [체크리스트 경로]
set -euo pipefail

root="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "git 저장소 안에서 실행해야 합니다" >&2; exit 1; }
cd "$root"

checklist="${1:-.claude/skills/psw-interview/references/checklist.md}"
[[ -f "$checklist" ]] || { echo "체크리스트가 없습니다: $checklist" >&2; exit 1; }

required="$(grep -oE '^- \[필수\] ck-[a-z]+-[0-9]+' "$checklist" | awk '{print $3}')"
handled="$(grep -rhoE 'ck-[a-z]+-[0-9]+' docs/req docs/open-question.md 2>/dev/null | sort -u || true)"

missing=()
total=0
while IFS= read -r id; do
  [[ -z "$id" ]] && continue
  total=$((total + 1))
  grep -qxF "$id" <<<"$handled" && continue
  missing+=("$id")
done <<<"$required"

echo "필수 항목: $total, 누락: ${#missing[@]}"
if [[ ${#missing[@]} -gt 0 ]]; then
  for id in "${missing[@]}"; do
    grep -E "^- \[필수\] $id " "$checklist" | sed 's/^- \[필수\] /누락: /'
  done
  exit 1
fi
echo "누락 없음"
