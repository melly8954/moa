#!/usr/bin/env bash
# 설계 교차 검증의 스크립트 항목을 실행한다 (harness-psw 4.3, 10.5).
#   1 (경고) 승인된 must FR 중 화면에서 참조되지 않는 것 (서버 전용 기능이면 무시)
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
# 줄 목록에 그 줄이 있는가. 프로세스를 띄우지 않는다 (Windows에서는 프로세스 생성이 느리다)
has_id() { [[ $'\n'"$2"$'\n' == *$'\n'"$1"$'\n'* ]]; }
ID_RE='((FR|NFR|SEC|INT|DAT|SCR)-[A-Z0-9]+-[0-9]+|CON-[0-9]+)'

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

# 구분자는 |다 (탭은 read가 빈 열을 합쳐 버린다)
# 목업 메타 주석: "파일|id|refs" (메타 주석이 있는 파일만, 파일마다 첫 메타 주석)
mock_meta="$( [[ -n "$mockups" ]] && tr '\n' '\0' <<<"$mockups" | xargs -0 awk '
  FNR == 1 { done = 0 }
  !done && /<!--[[:space:]]*psw / {
    done = 1; id = ""; refs = ""
    n = split($0, part, "|")
    for (i = 1; i <= n; i++) {
      f = part[i]
      sub(/^[[:space:]]*<!--[[:space:]]*psw[[:space:]]+/, "", f)
      sub(/[[:space:]]*-->.*$/, "", f)
      sub(/^[[:space:]]+/, "", f); sub(/[[:space:]]+$/, "", f)
      if (f ~ /^id:/)   { sub(/^id:[[:space:]]*/, "", f); id = f }
      if (f ~ /^refs:/) { sub(/^refs:[[:space:]]*/, "", f); refs = f }
    }
    print FILENAME "|" id "|" refs
  }' || true)"

# 목업이 쓰는 컴포넌트: "파일|컴포넌트"
mock_components="$( [[ -n "$mockups" ]] && tr '\n' '\0' <<<"$mockups" | xargs -0 awk '
  {
    line = $0
    while (match(line, /data-component="[^"]*"/)) {
      c = substr(line, RSTART + 16, RLENGTH - 17)
      print FILENAME "|" c
      line = substr(line, RSTART + RLENGTH)
    }
  }' | sort -u || true)"

# components.md 표의 첫 열 (영문 대문자로 시작하는 컴포넌트 이름)
component_names="$(grep -oE '^\|[[:space:]]*[A-Z][A-Za-z0-9]*[[:space:]]*\|' "$D/ui/components.md" 2>/dev/null \
  | sed -E 's/^\|[[:space:]]*//; s/[[:space:]]*\|$//' | uniq_lines || true)"

# 설계 문서가 참조하는 ID: 화면(ui) 아래와 설계 전체
ids_in_ui="$(grep -rhoE "\b$ID_RE\b" "$D/ui" 2>/dev/null | uniq_lines || true)"
ids_in_design="$(grep -rhoE "\b$ID_RE\b" "$D" 2>/dev/null | uniq_lines || true)"

# 도메인 README 기능 목록의 우선순위: "ID<TAB>우선순위" (세 번째 열)
priorities="$(cat "$R/functional"/*/README.md 2>/dev/null \
  | awk -F'|' '$2 ~ /^[[:space:]]*FR-[A-Z0-9]+-[0-9]+[[:space:]]*$/ { id = $2; p = $4; gsub(/[[:space:]]/, "", id); gsub(/[[:space:]]/, "", p); if (!(id in seen)) { seen[id] = 1; print id "\t" p } }' || true)"
non_must="$(awk -F'\t' '$2 != "" && $2 != "must" { print $1 }' <<<"$priorities")"

# ---------- 1 ----------
echo "[1] (경고) 승인된 FR 중 화면에서 참조되지 않는 것"
targets="$fr_approved"
if [[ -z "$targets" ]]; then
  echo "  (approved FR이 없어 전체 FR로 검사한다)"
  targets="$fr_all"
fi
skipped=0
while IFS= read -r id; do
  [[ -z "$id" ]] && continue
  in_scope "$id" || continue
  # 우선순위가 must가 아닌 FR(이번 범위가 아닐 수 있음)은 경고하지 않는다
  if has_id "$id" "$non_must"; then skipped=$((skipped + 1)); continue; fi
  has_id "$id" "$ids_in_ui" || warn "$id 를 참조하는 화면 없음 (서버 전용 기능이면 무시)"
done <<<"$targets"
[[ $skipped -gt 0 ]] && echo "  (우선순위가 must가 아닌 FR ${skipped}개는 건너뜀)"

# ---------- 2 ----------
echo "[2] NFR·SEC·INT가 설계 문서에서 참조된다"
while IFS= read -r id; do
  [[ -z "$id" || "$id" == DAT-* ]] && continue
  has_id "$id" "$ids_in_design" || err "$id 를 참조하는 설계 문서 없음"
done <<<"$area_ids"

# ---------- 4 ----------
echo "[4] 목업의 data-component 값이 components.md에 있다"
while IFS='|' read -r f c; do
  [[ -z "$c" ]] && continue
  has_id "$c" "$component_names" || err "$f 가 components.md에 없는 컴포넌트 $c 사용"
done <<<"$mock_components"

# ---------- 5, 6(목업) ----------
echo "[5] 목업과 IA 화면 목록이 대응한다 / [6] 목업 refs"
meta_files="$(cut -d'|' -f1 <<<"$mock_meta")"
mock_ids="$(cut -d'|' -f2 <<<"$mock_meta" | uniq_lines)"
while IFS= read -r f; do
  [[ -z "$f" ]] && continue
  has_id "$f" "$meta_files" || err "$f 메타 주석 없음"
done <<<"$mockups"
while IFS='|' read -r f mid refs; do
  [[ -z "$f" ]] && continue
  if [[ -z "$mid" ]]; then err "$f 메타 주석 없음"; continue; fi
  in_scope "$mid" || continue
  has_id "$mid" "$scr_ids" || err "$mid ($f) 가 IA 화면 목록에 없음"
  [[ -z "$refs" ]] && err "$mid ($f) refs 비어 있음"
done <<<"$mock_meta"
while IFS= read -r id; do
  [[ -z "$id" ]] && continue
  in_scope "$id" || continue
  has_id "$id" "$mock_ids" || warn "$id 의 목업 없음"
done <<<"$scr_ids"

# ---------- 6 ----------
echo "[6] 화면의 refs가 비어 있지 않다"
# 행의 마지막 열이 refs다: "ID|refs"
scr_refs="$(awk -F'|' '{ id = $2; r = $(NF-1); gsub(/[[:space:]]/, "", id); gsub(/[[:space:]]/, "", r); print id "|" r }' <<<"$scr_rows")"
while IFS='|' read -r id refs; do
  [[ -z "$id" ]] && continue
  in_scope "$id" || continue
  [[ -z "$refs" ]] && err "$id (ia) refs 비어 있음"
done <<<"$scr_refs"

# ---------- 7 ----------
echo "[7] 참조한 ID가 정의돼 있다"
defined="$(printf '%s\n' "$fr_all" "$area_ids" "$con_ids" "$scr_ids" | uniq_lines)"
while IFS= read -r id; do
  [[ -z "$id" ]] && continue
  in_scope "$id" || [[ "$id" != FR-* && "$id" != SCR-* ]] || continue
  has_id "$id" "$defined" || err "정의되지 않은 ID $id ($(grep -rlF "$id" "$D" | head -3 | tr '\n' ' '))"
done <<<"$ids_in_design"

echo "== 결과: 오류 $errors, 경고 $warnings =="
[[ $errors -eq 0 ]]
