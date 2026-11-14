# git 규칙

psw-dev-kit `git` 템플릿에서 온 파일이다. 여기서 고치지 않는다.
바꿀 것은 키트의 `git/`을 고치고 다시 적용한다. 저장소마다 다른 내용은 `CLAUDE.md`에 둔다.

## 흐름

- `main`(배포) ← `dev`(개발) ← 작업 브랜치. 작업 브랜치는 dev에서 딴다
- dev·main에 직접 푸시하지 않는다. PR로만 병합한다 (승인 0, 작성자 병합 가능)
  - 예외: 저장소를 시작할 때의 첫 커밋과 dev 브랜치 생성
- 병합은 머지 커밋만 쓴다. squash·rebase 병합은 쓰지 않는다
- PR 전에 자잘한 커밋(오타 수정, 다시 시도)은 브랜치 안에서 합친다
- push와 PR 병합은 사용자가 요청할 때만 한다

## 태그

- 형식: `vMAJOR.MINOR.PATCH` (예: `v1.2.0`). 저장소마다 다르면 `.githooks/branch-rules`의 `tag-pattern`으로 정한다
- 배포 태그: `release/vMAJOR.MINOR.PATCH` (예: `release/v1.2.0`)
- PR이 병합되면 CI(`.github/workflows/tag.yml`)가 그 머지 커밋에 자동으로 단다
  - 작업 브랜치 → `dev`: `vX.Y.Z`. `feat/*`는 MINOR, `fix/*`·`refactor/*`·`design/*`는 PATCH를 올린다. 그 밖의 종류는 태그를 달지 않는다. MAJOR는 PR에 `tag:major` 라벨을 붙인다
  - `dev` → `main`: 함께 들어간 가장 높은 `vX.Y.Z`에 `release/`를 붙인다. 그 태그가 이미 있으면(새 번호 없이 문서만 바뀐 병합 등) 달지 않는다
  - PR 검사가 실패했거나 끝나지 않았으면 달지 않고 그 job이 실패한다
  - 무엇이 배포됐는지는 `release/` 태그로 본다
- 주석 태그만 쓴다: `git tag -a v1.2.0 -m "v1.2.0: <무엇이 바뀌었나>"`
- 이미 올린 태그는 옮기거나 다시 쓰지 않는다. 고칠 것이 있으면 다음 번호로 단다
- 태그는 검사(빌드·테스트)를 통과한 커밋에만 단다. 손으로 달아 올리는 것은 사용자가 요청할 때만: `git push origin refs/tags/<태그>`

## 종류

커밋은 `<종류>: <한국어 설명>`, 브랜치는 `<종류>/<이름>`으로 종류를 맞춘다.
브랜치의 `<이름>`은 소문자·숫자·`-`로 쓴다 (예: `feat/001-admin-login`, `fix/login-redirect`).

| 종류 | 브랜치 | 쓰는 경우 |
|---|---|---|
| `feat` | `feat/<스펙 번호>-<이름>` | 기능 추가·변경 |
| `fix` | `fix/<이름>` | 버그 수정 |
| `design` | `design/<이름>` | 동작은 그대로, 화면만 바뀜 |
| `style` | `style/<이름>` | 동작은 그대로, 코드 포맷·공백·줄바꿈만 바뀜 |
| `refactor` | `refactor/<이름>` | 동작은 그대로, 코드 구조만 바뀜 |
| `test` | `test/<이름>` | 테스트만 추가·수정 |
| `docs` | `docs/<이름>` | 문서·기획·스펙 |
| `chore` | `chore/<이름>` | 설정, 도구, 의존성, CI만 바뀜 |

## 커밋 메시지

```text
feat: 목록 화면에 검색 추가

이름과 기간으로 목록을 거를 수 있게 한다.

Co-Authored-By: ...
```

- scope(`feat(web):`)는 쓰지 않는다
- 제목은 무엇을 했는지 한국어로. 50자 이내 권장, 72자를 넘으면 막힌다
- 제목 다음 줄은 비우고, 본문에 무엇을·왜를 적는다
- 트레일러(`Co-Authored-By` 등)는 맨 끝에 빈 줄 없이 한 덩어리로 둔다

## 검사

| 무엇을 | 로컬 (hook) | 원격 (CI, `.github/workflows/commit-msg.yml`) |
|---|---|---|
| 커밋 메시지 (`scripts/check-commit-msg.sh`) | 커밋할 때 `commit-msg` | `main`·`dev` push와 PR |
| 브랜치 이름, dev·main 직접 push (`scripts/check-branch.sh`) | push할 때 `pre-push` | PR의 브랜치 이름과 병합 방향 (작업 → `dev` → `main`) |
| 태그 이름, 주석 태그, 옮기지 않기 (`scripts/check-branch.sh`) | push할 때 `pre-push` | 태그 push |

- hook 켜기 (클론마다 한 번): `git config core.hooksPath .githooks`
  - 키트의 앱 템플릿으로 만든 프로젝트는 `npm install`이나 Gradle 빌드 때 자동으로 켜진다 (`core.hooksPath`가 비어 있을 때만)
- 저장소마다 다른 값은 `.githooks/` 아래 설정 파일에 둔다. 키트에서 복사되지 않으므로 다시 적용해도 남는다
  - `commit-types`: 종류 목록 (예: `feat|fix|build`). 커밋과 브랜치가 함께 쓴다
  - `branch-rules`: 기준 브랜치, 직접 push를 막는 브랜치, 태그 이름 (기본 `base=main dev`, `protected=main dev`, `tag-pattern=^(release/)?v[0-9]+\.[0-9]+\.[0-9]+$`)

### 빌드·테스트 (앱 CI)

앱 템플릿의 CI(백엔드 `backend-ci`, 프론트 `frontend-ci`)가 돈다. 빠른 검사는 매번, 무거운 검사는 한 번만 돈다.

| 검사 | 작업 브랜치 → `dev` PR | `dev` push, `dev` → `main` PR, `main` push |
|---|---|---|
| 빠른 검사: 포맷·lint·타입·단위 테스트 (백엔드 `build`, 프론트 `check`) | 돈다 | 돈다 |
| 무거운 검사: DB 테스트 (백엔드 `db-test`), 화면 검사 (프론트 `e2e`) | 돈다 | 건너뛴다 (같은 내용이다) |

- 무거운 job은 `if: github.base_ref == 'dev'`로 건다. 새로 더하는 무거운 job도 같은 조건을 건다
- PR 전에 로컬에서 무거운 검사 전체를 돌리지 않는다. PR CI가 같은 검사를 돈다. 사용자가 요청할 때만 돌린다
  - 로컬에서는 빠른 검사와 바꾼 기능의 테스트 한 묶음만 한다 (예: 백엔드 `./gradlew build`와 `dbTest --tests "*<기능>AcceptanceTest"`, 프론트 `npm run check`)
  - 무거운 검사는 PR CI 결과로 확인하고, 실패하면 그때 고친다
- moa: 이 절은 moa에서 먼저 더했다. 키트 `git` 템플릿으로 옮긴 뒤 다시 적용하면 이 줄은 빠진다
