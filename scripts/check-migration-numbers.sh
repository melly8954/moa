#!/usr/bin/env bash
# Flyway 마이그레이션 번호 검사. 규칙은 docs/db.md "마이그레이션". CI는 .github/workflows/migration-numbers.yml이 돌린다.
#   - backend/src/main/resources/db/migration: V<자연수>__<설명>.sql만 둔다. 번호는 1부터 빈 번호 없이, 겹치지 않는다
#   - backend/src/test/resources/db/testmigration: V<900 이상>__<설명>.sql, 겹치지 않는다
#   - backend/src/main/resources/db/localdata: R__<설명>.sql만 둔다
#   <설명>은 소문자·숫자·밑줄이다.
# 여럿이 각자 "dev 마지막 번호 + 1"을 고르면 같은 번호가 git 충돌 없이 함께 병합될 수 있다.
# 인수 테스트(db-test)는 PR마다 그때의 dev 기준으로만 돌아 이것을 막지 못하고, dev push에서는 Flyway가 돌지 않는다.
# 그래서 이 검사를 PR과 dev push 모두에서 돌린다. dev에서 겹침이 잡히면 늦게 병합된 쪽이 바로 번호를 옮기는 PR을 낸다
# (배포 DB가 없을 때만 허용되는 예외다. 개발 서버가 생기면 dev에 들어간 파일은 옮기지 않고 새 번호로 고친다).
#
#   bash scripts/check-migration-numbers.sh [backend 폴더]   # 기본 backend
set -euo pipefail

BACKEND="${1:-backend}"
MAIN="$BACKEND/src/main/resources/db/migration"
TEST="$BACKEND/src/test/resources/db/testmigration"
LOCAL="$BACKEND/src/main/resources/db/localdata"
errors=0

fail() {
	echo "실패: $1"
	errors=$((errors + 1))
}

# 폴더 하나를 본다. 인자: 폴더, 가장 작은 번호, 빈 번호 검사 여부(yes/no)
check_versions() {
	local dir="$1" min="$2" contiguous="$3"
	[ -d "$dir" ] || return 0
	declare -A seen=()
	local numbers=() file name number
	for file in "$dir"/*; do
		[ -e "$file" ] || continue
		name="$(basename "$file")"
		if [[ ! "$name" =~ ^V([1-9][0-9]*)__[a-z0-9_]+\.sql$ ]]; then
			fail "$dir/$name: 이름은 V<자연수>__<소문자·숫자·밑줄>.sql이어야 한다"
			continue
		fi
		number="${BASH_REMATCH[1]}"
		if [ "$number" -lt "$min" ]; then
			fail "$dir/$name: 번호는 $min 이상이어야 한다"
		fi
		if [ -n "${seen[$number]:-}" ]; then
			fail "$dir: V$number 겹침 (${seen[$number]}, $name). 늦게 병합된 쪽의 번호를 dev 마지막 번호 + 1로 옮긴다"
		else
			seen[$number]="$name"
			numbers+=("$number")
		fi
	done
	if [ "$contiguous" = yes ] && [ "${#numbers[@]}" -gt 0 ]; then
		local expected="$min" sorted
		sorted="$(printf '%s\n' "${numbers[@]}" | sort -n)"
		for number in $sorted; do
			if [ "$number" -ne "$expected" ]; then
				fail "$dir: V$expected 이 없다 (빈 번호). 다음 번호는 dev 마지막 번호 + 1이다"
				break
			fi
			expected=$((expected + 1))
		done
	fi
}

check_versions "$MAIN" 1 yes
check_versions "$TEST" 900 no

if [ -d "$LOCAL" ]; then
	for file in "$LOCAL"/*; do
		[ -e "$file" ] || continue
		name="$(basename "$file")"
		[[ "$name" =~ ^R__[a-z0-9_]+\.sql$ ]] || fail "$LOCAL/$name: 로컬 데이터는 R__<소문자·숫자·밑줄>.sql이어야 한다"
	done
fi

if [ "$errors" -gt 0 ]; then
	echo "마이그레이션 번호 검사 실패 $errors건 (규칙: docs/db.md \"마이그레이션\")"
	exit 1
fi
echo "마이그레이션 번호 검사 통과"
