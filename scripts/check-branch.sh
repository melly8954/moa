#!/usr/bin/env bash
# 브랜치·태그 이름과 흐름을 검사한다. 규칙은 docs/git.md (없는 저장소는 CLAUDE.md).
#
# 사용법
#   pre-push hook: check-branch.sh < <git이 주는 push 목록>
#   CI(PR):        check-branch.sh --pr <작업 브랜치> <대상 브랜치>
#   CI(태그):      check-branch.sh --tag <태그>
#   이름만:        check-branch.sh --name <브랜치>
# 통과하면 0, 아니면 1.
#
# 저장소 설정 (.githooks/branch-rules, 없으면 기본값)
#   base=main dev          기준 브랜치. 앞이 배포 쪽이다. 이름 검사를 받지 않는다
#   protected=main dev     직접 push를 막는 브랜치. 원격에 처음 만들 때만 허용한다
#   tag-pattern=<정규식>   태그 이름 (기본 ^(release/)?v[0-9]+\.[0-9]+\.[0-9]+$). 태그는 주석 태그(git tag -a)만, 이미 올린 태그는 옮기지 않는다
# 종류는 커밋 검사와 같은 목록(.githooks/commit-types)을 쓴다.
set -uo pipefail

top="$(git rev-parse --show-toplevel)"

types='feat|fix|design|style|refactor|test|docs|chore'
[[ -f "$top/.githooks/commit-types" ]] && types="$(tr -d '[:space:]' <"$top/.githooks/commit-types")"

base="main dev"
protected="main dev"
tag_pattern='^(release/)?v[0-9]+\.[0-9]+\.[0-9]+$'
if [[ -f "$top/.githooks/branch-rules" ]]; then
  while IFS='=' read -r key value; do
    value="${value%$'\r'}"
    case "$key" in
      base) base="$value" ;;
      protected) protected="$value" ;;
      tag-pattern) tag_pattern="$value" ;;
    esac
  done <"$top/.githooks/branch-rules"
fi
read -ra bases <<<"$base"

errors=0
err() { echo "브랜치 검사 오류: $*" >&2; errors=$((errors + 1)); }
in_list() { [[ " $2 " == *" $1 "* ]]; }   # in_list <값> <공백 구분 목록>

# 작업 브랜치 이름: <종류>/<소문자·숫자·-·.>
check_name() {
  local name="$1"
  in_list "$name" "$base" && return 0
  if ! [[ "$name" =~ ^($types)/[a-z0-9][a-z0-9._-]*$ ]]; then
    err "브랜치 이름은 '<종류>/<이름>'이다 (종류: ${types//|/, }, 이름: 소문자·숫자·-): $name"
  fi
}

# 태그: 이름은 tag-pattern, 주석 태그만 (로컬에 그 태그가 있을 때). 대상은 sha (pre-push) 또는 태그 ref (CI)
check_tag() {
  local name="$1" object="${2:-refs/tags/$1}"
  if ! [[ "$name" =~ $tag_pattern ]]; then
    err "태그 이름이 규칙($tag_pattern)과 다르다: $name"
  fi
  if git cat-file -e "$object" 2>/dev/null && [[ "$(git cat-file -t "$object")" != tag ]]; then
    err "주석 태그만 올린다 (git tag -a <태그> -m <설명>): $name"
  fi
}

# PR: 기준 브랜치 i에는 i+1만, 마지막 기준 브랜치에는 작업 브랜치만 들어온다 (예: main ← dev ← 작업)
check_pr() {
  local head="$1" target="$2" i
  for i in "${!bases[@]}"; do
    [[ "${bases[$i]}" == "$target" ]] || continue
    if (( i + 1 < ${#bases[@]} )); then
      [[ "$head" == "${bases[$((i + 1))]}" ]] || err "${target}에는 ${bases[$((i + 1))]}만 병합한다: $head → $target"
    else
      in_list "$head" "$base" && err "${target}에는 작업 브랜치만 병합한다: $head → $target"
      check_name "$head"
    fi
    return
  done
}

case "${1:-}" in
  --name) check_name "${2:?브랜치를 지정한다}" ;;
  --pr) check_pr "${2:?작업 브랜치를 지정한다}" "${3:?대상 브랜치를 지정한다}" ;;
  --tag) check_tag "${2:?태그를 지정한다}" ;;
  *)
    # pre-push: 줄마다 <로컬 ref> <로컬 sha> <원격 ref> <원격 sha>
    zero='^0+$'
    while read -r _ local_sha remote_ref remote_sha; do
      [[ "$local_sha" =~ $zero ]] && continue           # 원격 브랜치·태그 삭제
      if [[ "$remote_ref" == refs/tags/* ]]; then
        [[ "$remote_sha" =~ $zero ]] || err "이미 올린 태그는 옮기지 않는다. 새 번호로 단다: ${remote_ref#refs/tags/}"
        check_tag "${remote_ref#refs/tags/}" "$local_sha"
        continue
      fi
      [[ "$remote_ref" == refs/heads/* ]] || continue   # 그 밖의 ref는 보지 않는다
      name="${remote_ref#refs/heads/}"
      if in_list "$name" "$protected"; then
        [[ "$remote_sha" =~ $zero ]] || err "${name}에 직접 push하지 않는다. 작업 브랜치를 올리고 PR로 병합한다"
      else
        check_name "$name"
      fi
    done
    ;;
esac

if (( errors > 0 )); then
  rules=CLAUDE.md
  [[ -f "$top/docs/git.md" ]] && rules=docs/git.md
  echo "브랜치·태그 규칙: $rules" >&2
  exit 1
fi
exit 0
