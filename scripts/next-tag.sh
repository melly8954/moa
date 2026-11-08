#!/usr/bin/env bash
# 병합 때 달 다음 태그를 출력한다 (.github/workflows/tag.yml이 쓴다). 규칙은 docs/git.md. 태그가 없으면 아무것도 출력하지 않는다.
#
#   next-tag.sh <작업 브랜치> [--major]   작업 브랜치 → dev
#     feat/*                       → MINOR
#     fix/*, refactor/*, design/*  → PATCH
#     그 밖의 종류, 기준 브랜치    → 태그 없음
#     --major (PR 라벨 tag:major)  → 작업 브랜치면 종류와 관계없이 MAJOR
#     vX.Y.Z 태그가 하나도 없으면  → v0.1.0 (--major면 v1.0.0)
#
#   next-tag.sh --release                 dev → main. HEAD에 들어 있는 가장 높은 vX.Y.Z에 release/를 붙인다
#     vX.Y.Z 태그가 없거나, 그 release 태그가 이미 있으면 → 태그 없음
set -uo pipefail

if [[ "${1:-}" == "--release" ]]; then
  last="$(git tag --merged HEAD --list 'v[0-9]*' --sort=-v:refname | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | head -n 1)"
  [[ -n "$last" ]] || exit 0
  git rev-parse -q --verify "refs/tags/release/$last" >/dev/null && exit 0
  echo "release/$last"
  exit 0
fi

branch="${1:?작업 브랜치를 지정한다}"
major="${2:-}"

[[ "$branch" == */* ]] || exit 0

level=""
case "${branch%%/*}" in
  feat) level=minor ;;
  fix | refactor | design) level=patch ;;
esac
[[ "$major" == "--major" ]] && level=major
[[ -n "$level" ]] || exit 0

last="$(git tag --list 'v[0-9]*' --sort=-v:refname | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | head -n 1)"
if [[ -z "$last" ]]; then
  [[ "$level" == major ]] && echo "v1.0.0" || echo "v0.1.0"
  exit 0
fi

IFS=. read -r x y z <<<"${last#v}"
case "$level" in
  major) echo "v$((x + 1)).0.0" ;;
  minor) echo "v$x.$((y + 1)).0" ;;
  patch) echo "v$x.$y.$((z + 1))" ;;
esac
