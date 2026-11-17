# 스펙 번호

psw-dev-kit `git` 템플릿(`--spec-kit`)에서 온 파일이다. 여기서 고치지 않는다.
바꿀 것은 키트의 `git/`을 고치고 다시 적용한다.

Spec Kit은 새 스펙 번호를 로컬 `specs/` 폴더의 가장 큰 번호 + 1로 정한다.
여럿이 각자 `/speckit-specify`를 돌리면 같은 번호(예: `008-party`, `008-feed`)를 받고, 폴더 이름이 달라 git 충돌 없이 함께 병합될 수 있다.
그래서 번호를 먼저 예약하고, CI가 겹침을 검사한다.

## 예약

1. 스펙 목록 표(`specs/README.md`)에 예약 줄을 넣는다: 스펙 칸에 다음 번호(`` `008` ``), 상태 칸에 `예약 (담당: <이름>)`. 이 변경만 `dev`에 먼저 병합한다 (`docs/` 브랜치)
2. 스펙을 만들 때 예약한 번호를 명시한다: `/speckit-specify SPECIFY_FEATURE_DIRECTORY=specs/008-<이름> <기능 설명>`
   - 명시하지 않으면 로컬 폴더로 번호가 정해져, 아직 폴더가 없는 남의 예약 번호를 받을 수 있다
   - Spec Kit 스크립트(`.specify/scripts/bash/create-new-feature.sh`)를 직접 쓸 때는 `--number 8`. 로컬에 같은 번호 폴더가 있으면 다음 빈 번호로 바뀌니 출력의 번호를 확인한다
3. 스펙이 생기면 표의 스펙 칸을 폴더 이름(`` `008-<이름>` ``)으로 바꾼다

- 겹치면 나중에 만든 쪽이 번호를 바꾼다. 문서 PR을 병합한 뒤면 폴더·구현 브랜치(`feat/<번호>-…`)·문서 참조를 모두 바꿔야 하니, 가능하면 그 전에 바꾼다

## 검사

- `scripts/check-spec-numbers.sh`: 같은 번호를 서로 다른 스펙이 쓰면 실패한다
  - 폴더끼리 (`specs/008-party`와 `specs/008-feed`, 앞의 0은 무시)
  - 표의 `` `008-<이름>` ``과 다른 이름의 폴더
  - 예약 줄(`` `008` ``)이 둘
  - 예약 줄과 그 번호의 스펙 하나가 함께 있는 것은 괜찮다
  - 표는 `specs/README.md`에서 `|`로 시작하는 줄의 백틱 안 번호만 본다. 파일이 없으면 폴더만 본다. `specs/`가 없으면 통과한다
- CI: `.github/workflows/spec-numbers.yml`이 `dev`·`main`으로 가는 PR과 `dev` push에서 돌린다 (job 이름 `spec-numbers`, ruleset 필수 검사)
- 필수 검사로 건다: 저장소 Settings → Branches(또는 Rules)의 `dev`·`main` 규칙에서 "Require status checks to pass before merging"에 `spec-numbers`를 더한다
  - 걸지 않으면 검사가 실패해도 병합 단추가 열려 있다
  - GitHub Free 비공개 저장소는 규칙을 걸 수 없다. 그때는 PR에서 검사 결과를 보고 병합한다
