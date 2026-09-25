#!/usr/bin/env bash
# 추적 매트릭스(RTM)를 만든다 (harness-psw 1.2, 10.5).
# 하위 → 상위 참조를 모아 기능 요구사항별로 설계·테스트·커밋 연결을 보여준다.
# 손으로 관리하지 않는다. 필요할 때 다시 만든다.
# 사용법: rtm.sh [도메인 코드]   (결과는 표준 출력)
set -uo pipefail

root="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "git 저장소 안에서 실행해야 합니다" >&2; exit 1; }
cd "$root"
domain="${1:-}"

echo "# 추적 매트릭스"
echo
echo "- 생성: $(date '+%Y-%m-%d %H:%M'), 커밋 $(git rev-parse --short HEAD 2>/dev/null || echo 없음)"
echo "- 테스트: 코드에서 'FR-xxx-NNN AC-n'을 참조하는 AC 수 / 전체 AC 수"
echo
echo "| FR | 제목 | 상태 | 우선순위 | 설계 참조 | AC (MUST) | 테스트 | 커밋 |"
echo "|---|---|---|---|---|---|---|---|"

# FR 파일을 "ID 파일" 줄로 만들어 ID 순으로 정렬한다
fr_list() {
  grep -rE '^id:[[:space:]]*FR-' docs/req/functional 2>/dev/null \
    | awk -F':id:' '{ split($2, a, " "); print a[1], $1 }' | sort
}

while read -r id f; do
  [[ -z "$id" ]] && continue
  [[ -n "$domain" && "$id" != "FR-${domain}-"* ]] && continue
  title="$(grep -m1 -E '^# ' "$f" | sed -E "s/^# ${id}[[:space:]]*//")"
  st="$(grep -m1 -E '^status:' "$f" | sed -E 's/^status:[[:space:]]*//')"
  # 우선순위: 도메인 README.md 기능 목록의 세 번째 열 (| ID | 기능 | 우선순위 | 파일 |)
  prio="$(grep -hE "^\|[[:space:]]*${id}[[:space:]]*\|" "$(dirname "$f")/README.md" 2>/dev/null | head -1 | awk -F'|' '{gsub(/ /,"",$4); print $4}')"
  design="$(grep -rlF "$id" docs/design 2>/dev/null | wc -l | tr -d ' ')"
  ac_total="$(grep -cE '^- AC-[0-9]+' "$f")"
  ac_must="$(grep -cE '^- AC-[0-9]+ \[MUST\]' "$f")"
  tested="$(git grep --untracked -h -oE "${id} AC-[0-9]+" -- . ':!docs' ':!.claude' 2>/dev/null | sort -u | wc -l | tr -d ' ')"
  commits="$(git log --all --format=%H --grep="$id" 2>/dev/null | wc -l | tr -d ' ')"
  echo "| ${id} | ${title} | ${st} | ${prio:--} | ${design} | ${ac_total} (${ac_must}) | ${tested}/${ac_total} | ${commits} |"
done < <(fr_list)
