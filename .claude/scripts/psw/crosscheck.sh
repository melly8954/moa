#!/usr/bin/env bash
# 설계 교차 검증의 스크립트 항목을 실행한다 (harness-psw 4.3, 10.5).
#   1 (경고) 승인된 FR 중 화면에서 참조되지 않는 것 (서버 전용 기능이면 무시)
#   2 NFR·SEC·INT가 설계 문서 어딘가에서 참조된다
#   4 목업의 data-component 값이 components.md에 있다
#   5 목업마다 IA 화면 목록에 행이 있다 (IA 행에 목업이 없으면 경고)
#   6 화면의 refs가 비어 있지 않다
#   7 설계 문서가 참조하는 ID가 실제로 정의돼 있다
# 사용법: crosscheck.sh [도메인 코드]
#   도메인 코드(예: ORD)를 주면 항목 1, 5, 6, 7을 그 도메인 ID로 좁힌다
# 오류가 없으면 0, 있으면 1로 끝난다. 경고는 결과에 영향을 주지 않는다.
set -euo pipefail

root="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "git 저장소 안에서 실행해야 합니다" >&2; exit 1; }
cd "$root"

domain="${1:-}"
D="docs/design"
R="docs/req"
errors=0
warnings=0

err()  { echo "  오류: $*"; errors=$((errors + 1)); }
warn() { echo "  경고: $*"; warnings=$((warnings + 1)); }
in_scope() { [[ -z "$domain" || "$1" == *"-${domain}-"* ]]; }
uniq_lines() { sed '/^$/d' | sort -u; }
has_id() { grep -qxF "$1" <<<"$2"; }

# ---------- 정의 수집 ----------

fr_files="$(grep -rlE '^id:[[:space:]]*FR-' "$R/functional" 2>/dev/null || true)"
fr_all="$( [[ -n "$fr_files" ]] && grep -hE '^id:' $fr_files | grep -oE 'FR-[A-Z0-9]+-[0-9]+' | uniq_lines || true)"
fr_approved="$( [[ -n "$fr_files" ]] && grep -lE '^status:[[:space:]]*approved' $fr_files | xargs -r grep -hE '^id:' | grep -oE 'FR-[A-Z0-9]+-[0-9]+' | uniq_lines || true)"

area_ids="$(grep -rhoE '^\|[[:space:]]*(NFR|SEC|INT|DAT)-[A-Z0-9]+-[0-9]+' "$R/non-functional" "$R/security" "$R/integration" "$R/data" 2>/dev/null \
  | grep -oE '(NFR|SEC|INT|DAT)-[A-Z0-9]+-[0-9]+' | uniq_lines || true)"

con_ids="$(grep -hoE '^\|[[:space:]]*CON-[0-9]+' "$R/README.md" 2>/dev/null | grep -oE 'CON-[0-9]+' | uniq_lines || true)"

# IA 화면 목록: ui/ia/<domain>.md 표의 행
scr_rows="$(cat "$D"/ui/ia/*.md 2>/dev/null | grep -E '^\|[[:space:]]*SCR-[A-Z0-9]+-[0-9]+[[:space:]]*\|' || true)"
scr_ids="$(grep -oE '^\|[[:space:]]*SCR-[A-Z0-9]+-[0-9]+' <<<"$scr_rows" | grep -oE 'SCR-[A-Z0-9]+-[0-9]+' | uniq_lines || true)"

mockups="$(find "$D/ui/screens" -name '*.html' 2>/dev/null | sort || true)"
meta_field() { # meta_field <파일> <키>
  grep -m1 -E '<!--[[:space:]]*psw ' "$1" 2>/dev/null | tr '|' '\n' \
    | sed -nE "s/^[[:space:]]*(<!--[[:space:]]*psw[[:space:]]+)?$2:[[:space:]]*//p" | sed -E 's/[[:space:]]*-->.*$//; s/[[:space:]]*$//' || true
}

# components.md 표의 첫 열 (영문 대문자로 시작하는 컴포넌트 이름)
component_names="$(grep -oE '^\|[[:space:]]*[A-Z][A-Za-z0-9]*[[:space:]]*\|' "$D/ui/components.md" 2>/dev/null \
  | sed -E 's/^\|[[:space:]]*//; s/[[:space:]]*\|$//' | uniq_lines || true)"

# ---------- 1 ----------
echo "[1] (경고) 승인된 FR 중 화면에서 참조되지 않는 것"
targets="$fr_approved"
if [[ -z "$targets" ]]; then
  echo "  (approved FR이 없어 전체 FR로 검사한다)"
  targets="$fr_all"
fi
while IFS= read -r id; do
  [[ -z "$id" ]] && continue
  in_scope "$id" || continue
  grep -rqF "$id" "$D/ui" 2>/dev/null || warn "$id 를 참조하는 화면 없음 (서버 전용 기능이면 무시)"
done <<<"$targets"

# ---------- 2 ----------
echo "[2] NFR·SEC·INT가 설계 문서에서 참조된다"
while IFS= read -r id; do
  [[ -z "$id" || "$id" == DAT-* ]] && continue
  grep -rqF "$id" "$D" 2>/dev/null || err "$id 를 참조하는 설계 문서 없음"
done <<<"$area_ids"

# ---------- 4 ----------
echo "[4] 목업의 data-component 값이 components.md에 있다"
while IFS= read -r f; do
  [[ -z "$f" ]] && continue
  used="$(grep -oE 'data-component="[^"]*"' "$f" | sed -E 's/^data-component="//; s/"$//' | uniq_lines || true)"
  while IFS= read -r c; do
    [[ -z "$c" ]] && continue
    has_id "$c" "$component_names" || err "$f 가 components.md에 없는 컴포넌트 $c 사용"
  done <<<"$used"
done <<<"$mockups"

# ---------- 5, 6(목업) ----------
echo "[5] 목업과 IA 화면 목록이 대응한다 / [6] 목업 refs"
mock_ids=""
while IFS= read -r f; do
  [[ -z "$f" ]] && continue
  mid="$(meta_field "$f" id)"
  if [[ -z "$mid" ]]; then err "$f 메타 주석 없음"; continue; fi
  mock_ids+="$mid"$'\n'
  in_scope "$mid" || continue
  has_id "$mid" "$scr_ids" || err "$mid ($f) 가 IA 화면 목록에 없음"
  [[ -z "$(meta_field "$f" refs)" ]] && err "$mid ($f) refs 비어 있음"
done <<<"$mockups"
while IFS= read -r id; do
  [[ -z "$id" ]] && continue
  in_scope "$id" || continue
  has_id "$id" "$(uniq_lines <<<"$mock_ids")" || warn "$id 의 목업 없음"
done <<<"$scr_ids"

# ---------- 6 ----------
echo "[6] 화면의 refs가 비어 있지 않다"
while IFS= read -r row; do
  [[ -z "$row" ]] && continue
  id="$(grep -oE 'SCR-[A-Z0-9]+-[0-9]+' <<<"$row" | head -1)"
  in_scope "$id" || continue
  refs="$(awk -F'|' '{print $(NF-1)}' <<<"$row" | tr -d '[:space:]')"
  [[ -z "$refs" ]] && err "$id (ia) refs 비어 있음"
done <<<"$scr_rows"

# ---------- 7 ----------
echo "[7] 참조한 ID가 정의돼 있다"
defined="$(printf '%s\n' "$fr_all" "$area_ids" "$con_ids" "$scr_ids" | uniq_lines)"
refs_found="$(grep -rhoE '\b((FR|NFR|SEC|INT|DAT|SCR)-[A-Z0-9]+-[0-9]+|CON-[0-9]+)\b' "$D" 2>/dev/null | uniq_lines || true)"
while IFS= read -r id; do
  [[ -z "$id" ]] && continue
  in_scope "$id" || [[ "$id" != FR-* && "$id" != SCR-* ]] || continue
  has_id "$id" "$defined" || err "정의되지 않은 ID $id ($(grep -rlF "$id" "$D" | head -3 | tr '\n' ' '))"
done <<<"$refs_found"

echo "== 결과: 오류 $errors, 경고 $warnings =="
[[ $errors -eq 0 ]]
