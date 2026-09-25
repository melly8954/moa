#!/usr/bin/env bash
# 피드백 루프 종료 조건을 확인한다 (harness-psw 7.1, 8.2, 10.5).
# 종료 가능하면 0, 아니면 1로 끝난다.
# 사용법: loop-status.sh [자리표시를 검사할 경로...]   (기본: docs)
set -euo pipefail

root="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "git 저장소 안에서 실행해야 합니다" >&2; exit 1; }
cd "$root"

OQ="docs/open-question.md"

scope=("$@")
[[ ${#scope[@]} -eq 0 ]] && scope=(docs)

# open-question.md의 항목을 "ID<TAB>상태" 줄로 낸다
open_items() {
  [[ -f "$OQ" ]] || return 0
  awk '
    /^## OPEN-[0-9]+/ { if (id != "") print id "\t" st; id = $2; st = ""; next }
    /^## / { if (id != "") print id "\t" st; id = ""; next }
    id != "" && st == "" && /^- 상태:/ { sub(/^- 상태:[[:space:]]*/, ""); sub(/[[:space:]]*$/, ""); st = $0 }
    END { if (id != "") print id "\t" st }
  ' "$OQ"
}

items="$(open_items)"
status_of() { awk -F'\t' -v id="$1" '$1 == id { print $2; found = 1; exit } END { if (!found) print "없음" }' <<<"$items"; }

blocking=0

echo "== 미결 ($OQ) =="
open_ids=()
deferred_ids=()
while IFS=$'\t' read -r id st; do
  [[ -z "$id" ]] && continue
  case "$st" in
    보류) deferred_ids+=("$id") ;;
    *)    open_ids+=("$id") ;;
  esac
done <<<"$items"
echo "미정: ${#open_ids[@]} ${open_ids[*]:-}"
echo "보류: ${#deferred_ids[@]} ${deferred_ids[*]:-}"

echo "== 자리표시 (${scope[*]}) =="
placeholders="$(grep -rnoHE '\[OPEN-[0-9]+\]' "${scope[@]}" 2>/dev/null | grep -v "^$OQ:" || true)"
if [[ -z "$placeholders" ]]; then
  echo "없음"
else
  while IFS= read -r line; do
    id="$(grep -oE 'OPEN-[0-9]+' <<<"${line##*:}")"
    loc="${line%:*}"
    case "$(status_of "$id")" in
      없음) echo "오류 - 끊긴 자리표시: $id ($loc)"; blocking=1 ;;
      보류) echo "보류: $id ($loc)" ;;
      *)    echo "미정: $id ($loc)"; blocking=1 ;;
    esac
  done <<<"$placeholders"
fi

# 경로를 지정하지 않았으면 자리표시가 없는 미정 항목도 종료를 막는다.
if [[ $# -eq 0 && ${#open_ids[@]} -gt 0 ]]; then
  blocking=1
fi

echo "== 결과 =="
if [[ $blocking -eq 0 ]]; then
  echo "종료 가능"
else
  echo "종료 불가"
fi
exit $blocking
