#!/usr/bin/env bash
# 스펙 번호 중복 검사. 같은 번호를 서로 다른 스펙이 쓰면 겹친 것을 출력하고 실패한다.
#   - 폴더: specs/ 아래 <숫자>-<이름> 폴더
#   - 예약: specs/README.md 표의 `008`(예약) 또는 `008-<이름>`
# 같은 번호에 이름이 둘 이상이거나(폴더·표), 예약 줄이 둘 이상이면 겹친 것이다.
# 예약 줄(`008`)과 그 번호의 스펙 하나(`008-<이름>`)가 함께 있는 것은 같은 스펙이라 괜찮다.
# 팀원이 각자 /speckit-specify를 돌리면 같은 번호(예: 008-party, 008-feed)가 git 충돌 없이 함께 병합될 수 있다.
# 번호 예약 규칙은 specs/README.md "스펙 번호" 절. CI는 .github/workflows/specs.yml이 돌린다.
#
#   bash scripts/check-spec-numbers.sh [specs 폴더]   # 기본 specs
set -euo pipefail

SPECS_DIR="${1:-specs}"
README="$SPECS_DIR/README.md"
[ -d "$SPECS_DIR" ] || { echo "스펙 폴더가 없다: $SPECS_DIR" >&2; exit 1; }

declare -A names=()        # 번호 → 서로 다른 이름 목록 (공백으로 구분)
declare -A reservations=() # 번호 → 예약 줄 수
declare -A where=()        # 번호 → 어디서 봤는지 (출력용)

add_name() { # 번호 이름 출처
	local number="$1" name="$2" source="$3"
	case " ${names[$number]:-} " in
		*" $name "*) ;;
		*) names[$number]="${names[$number]:-} $name" ;;
	esac
	where[$number]="${where[$number]:-} $source:$name"
}

# 앞의 0은 무시하고 비교한다 (8-x와 008-x도 겹친다)
for path in "$SPECS_DIR"/*/; do
	[ -d "$path" ] || continue
	name="$(basename "$path")"
	[[ "$name" =~ ^([0-9]+)-(.+)$ ]] || continue
	add_name "$((10#${BASH_REMATCH[1]}))" "$name" "폴더"
done

if [ -f "$README" ]; then
	# 표 줄(|로 시작)의 `숫자` 또는 `숫자-이름`만 본다. 다른 표의 참조(예: `002-admin-users` FR-004)도 이름이 같으면 같은 스펙이다
	while IFS= read -r line; do
		[[ "$line" == \|* ]] || continue
		rest="$line"
		while [[ "$rest" =~ \`([0-9]{3,})(-[a-z0-9-]+)?\` ]]; do
			number=$((10#${BASH_REMATCH[1]}))
			if [ -n "${BASH_REMATCH[2]}" ]; then
				add_name "$number" "${BASH_REMATCH[1]}${BASH_REMATCH[2]}" "README"
			else
				reservations[$number]=$(( ${reservations[$number]:-0} + 1 ))
				where[$number]="${where[$number]:-} README:예약"
			fi
			rest="${rest#*"${BASH_REMATCH[0]}"}"
		done
	done < <(tr -d '\r' < "$README")
fi

problems=""
for number in $(printf '%s\n' "${!names[@]}" "${!reservations[@]}" | sort -nu); do
	name_count=$(wc -w <<< "${names[$number]:-}")
	if [ "$name_count" -gt 1 ] || [ "${reservations[$number]:-0}" -gt 1 ]; then
		problems+="$(printf '%03d' "$number"):${where[$number]}"$'\n'
	fi
done

if [ -n "$problems" ]; then
	echo "스펙 번호가 겹친다 (나중에 만든 쪽의 번호를 바꾼다. specs/README.md \"스펙 번호\"):" >&2
	printf '%s' "$problems" >&2
	exit 1
fi
echo "스펙 번호 겹침 없음 (번호 $(printf '%s\n' "${!names[@]}" "${!reservations[@]}" | sort -u | grep -c .)개)"
