#!/usr/bin/env bash
# 스펙 번호 중복 검사: specs/ 아래 <숫자>-<이름> 폴더의 번호가 겹치면 겹친 폴더를 출력하고 실패한다.
# 팀원이 각자 /speckit-specify를 돌리면 같은 번호(예: 008-party, 008-feed)가 git 충돌 없이 함께 병합될 수 있다.
# 번호 예약 규칙은 specs/README.md "스펙 번호" 절. CI는 .github/workflows/specs.yml이 돌린다.
#
#   bash scripts/check-spec-numbers.sh [specs 폴더]   # 기본 specs
set -euo pipefail

SPECS_DIR="${1:-specs}"
[ -d "$SPECS_DIR" ] || { echo "스펙 폴더가 없다: $SPECS_DIR" >&2; exit 1; }

declare -A seen=()
duplicates=""
for path in "$SPECS_DIR"/*/; do
	name="$(basename "$path")"
	[[ "$name" =~ ^([0-9]+)- ]] || continue
	# 앞의 0은 무시하고 비교한다 (8-x와 008-x도 겹친다)
	number=$((10#${BASH_REMATCH[1]}))
	if [ -n "${seen[$number]:-}" ]; then
		duplicates+="$(printf '%03d' "$number"): ${seen[$number]} ${name}"$'\n'
		seen[$number]="${seen[$number]} ${name}"
	else
		seen[$number]="$name"
	fi
done

if [ -n "$duplicates" ]; then
	echo "스펙 번호가 겹친다 (나중에 만든 쪽의 번호를 바꾼다. specs/README.md \"스펙 번호\"):" >&2
	printf '%s' "$duplicates" | sort -u >&2
	exit 1
fi
echo "스펙 번호 겹침 없음 (${#seen[@]}개)"
