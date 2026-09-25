#!/usr/bin/env bash
# 문서 크기 신호를 보여준다 (harness-psw 1.3, 10.5).
# 신호일 뿐 강제하지 않는다. 기준을 넘은 파일은 분리를 "검토"한다.
# 토큰 수는 추정값이다: 한글 1자 ≈ 1토큰, 그 외 4자 ≈ 1토큰.
# 사용법: doc-size.sh [기준 토큰 수]   (기본 2000)
set -uo pipefail

root="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "git 저장소 안에서 실행해야 합니다" >&2; exit 1; }
cd "$root"
limit="${1:-2000}"

over=0
while IFS= read -r f; do
  est="$(LC_ALL=C.UTF-8 awk '
    { line = $0; h = gsub(/[가-힣]/, "", line); hangul += h; other += length(line) + 1 }
    END { printf "%d", hangul + other / 4 }
  ' "$f" 2>/dev/null)"
  (( est <= limit )) && continue
  over=$((over + 1))
  case "$f" in
    docs/design/architecture.md|docs/design/conventions.md) note="응집 우선. 넘으면 영역 단위 분할 검토" ;;
    *) note="분리 검토" ;;
  esac
  printf '%6d  %s  (%s)\n' "$est" "$f" "$note"
done < <(find docs -type f -name '*.md' 2>/dev/null | sort)

echo "기준 ${limit} 토큰 초과: ${over}개 (HTML 목업, 자동 생성 파일은 제외)"
