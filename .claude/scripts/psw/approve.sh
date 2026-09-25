#!/usr/bin/env bash
# 문서를 approved로 바꾼다 (harness-psw 1.1, 10.5).
# 사용자가 직접 실행한다. 에이전트의 실행은 guard-paths.sh hook이 막는다.
#   Claude Code 입력창에서: ! bash .claude/scripts/psw/approve.sh <경로...>
#
# 사용법: approve.sh <파일 또는 폴더...>
#   - status: draft 인 md 파일(frontmatter)과 목업 html(메타 주석)을 approved로 바꾼다
#   - 미정 또는 끊긴 [OPEN-NNN] 자리표시가 남은 파일은 건너뛴다 (보류 항목은 허용, docs/open-question.md)
#   - 커밋은 하지 않는다
set -euo pipefail

if [[ $# -eq 0 ]]; then
  echo "사용법: approve.sh <파일 또는 폴더...>" >&2
  exit 1
fi

root="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "git 저장소 안에서 실행해야 합니다" >&2; exit 1; }

files=()
for target in "$@"; do
  if [[ -d "$target" ]]; then
    while IFS= read -r f; do files+=("$f"); done < <(find "$target" -type f \( -name '*.md' -o -name '*.html' \) | sort)
  elif [[ -f "$target" ]]; then
    files+=("$target")
  else
    echo "없는 경로: $target" >&2
    exit 1
  fi
done

# open-question.md에서 항목의 상태를 읽는다: 미정, 보류, 없음
open_status() {
  local oq="$root/docs/open-question.md"
  [[ -f "$oq" ]] || { echo "없음"; return; }
  awk -v id="$1" '
    /^## / { if (cur) exit; cur = ($2 == id); if (cur) found = 1; next }
    cur && /^- 상태:/ { sub(/^- 상태:[[:space:]]*/, ""); sub(/[[:space:]]*$/, ""); st = $0; exit }
    END { if (!found) print "없음"; else print (st == "" ? "미정" : st) }
  ' "$oq"
}

open_blockers() { # 파일에 남은, 승인을 막는 자리표시
  local f="$1" id st
  for id in $(grep -oE '\[OPEN-[0-9]+\]' "$f" | grep -oE 'OPEN-[0-9]+' | sort -u); do
    st="$(open_status "$id")"
    case "$st" in
      없음) echo "$id(끊김)" ;;
      보류) ;;
      *) echo "$id" ;;
    esac
  done
}

approved=()
skipped=()
for f in "${files[@]}"; do
  case "$f" in
    *.md)
      # 첫 frontmatter 안의 status만 본다
      current="$(awk 'NR==1 && $0!="---"{exit} NR>1 && $0=="---"{exit} /^status:/{sub(/^status:[[:space:]]*/,""); print; exit}' "$f")"
      ;;
    *.html)
      current="$(grep -m1 -oE '<!--[[:space:]]*psw .*status:[[:space:]]*[a-z]+' "$f" | sed -E 's/.*status:[[:space:]]*//' || true)"
      ;;
  esac
  [[ "$current" == "draft" ]] || continue

  blockers="$(open_blockers "$f" | tr '\n' ' ')"
  if [[ -n "$blockers" ]]; then
    skipped+=("$f (미결: $blockers)")
    continue
  fi

  case "$f" in
    *.md)
      awk 'NR==1{infm=($0=="---")} infm && NR>1 && $0=="---"{infm=0} infm && !done && /^status:[[:space:]]*draft[[:space:]]*$/{sub(/draft/,"approved"); done=1} {print}' "$f" > "$f.tmp" && mv "$f.tmp" "$f"
      ;;
    *.html)
      sed -i -E '0,/<!--[[:space:]]*psw /{s/(status:[[:space:]]*)draft/\1approved/}' "$f"
      ;;
  esac
  approved+=("$f")
done

echo "승인: ${#approved[@]}개"
for f in "${approved[@]}"; do echo "  $f"; done
if [[ ${#skipped[@]} -gt 0 ]]; then
  echo "건너뜀: ${#skipped[@]}개"
  for s in "${skipped[@]}"; do echo "  $s"; done
fi
if [[ ${#approved[@]} -gt 0 ]]; then
  echo "다음: 변경을 확인하고 커밋한다 (예: git commit -m \"docs: <범위> 승인\")"
fi
