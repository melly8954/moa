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
- 브랜치 정리
  - 작업 브랜치 → `dev` PR을 병합하면 그 작업 브랜치를 원격과 로컬에서 함께 지운다
    - 원격: `git push origin --delete <브랜치>`
    - 로컬: `git checkout dev && git pull --ff-only && git branch -d <브랜치>`
  - `dev` → `main` 병합에서는 아무 브랜치도 지우지 않는다
  - 저장소 설정 "Automatically delete head branches"는 끈다. 켜 두면 `dev` → `main` 병합 때 head인 `dev`가 지워질 수 있다

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
브랜치의 `<이름>`은 소문자·숫자 덩어리를 `-`(버전은 `.`도) 하나로 잇는다 (예: `feat/001-admin-login`, `fix/login-redirect`, `chore/git-template-v0.4.0`).
`_`·대문자·한글, `--`·`..`처럼 겹친 구분, 앞뒤의 `-`·`.`은 막힌다 (`scripts/check-branch.sh`).
`feat`의 스펙 번호는 어느 스펙의 구현인지 알아보려고 붙이는 약속이고, 검사하지 않는다.

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

| 무엇을 | 로컬 (hook) | 원격 (CI) |
|---|---|---|
| 커밋 메시지 (`scripts/check-commit-msg.sh`) | 커밋할 때 `commit-msg` | `main`·`dev`로 가는 PR (`.github/workflows/git.yml`, job `commit`) |
| 브랜치 이름, dev·main 직접 push (`scripts/check-branch.sh`) | push할 때 `pre-push` | PR의 브랜치 이름과 병합 방향 (작업 → `dev` → `main`) (`git.yml`, job `branch`) |
| 태그 이름, 주석 태그, 옮기지 않기 (`scripts/check-branch.sh`) | push할 때 `pre-push` | 자동 태그를 달기 전 (`.github/workflows/tag.yml`). 손으로 올린 태그는 CI가 보지 않는다 |

- 원격은 PR에서만 본다. `main`·`dev`는 직접 push를 막아 PR로만 커밋이 들어오기 때문이다

- hook 켜기 (클론마다 한 번): `git config core.hooksPath .githooks`
  - 키트의 앱 템플릿으로 만든 프로젝트는 `npm install`이나 Gradle 빌드 때 자동으로 켜진다 (`core.hooksPath`가 비어 있을 때만)
- 저장소마다 다른 값은 `.githooks/` 아래 설정 파일에 둔다. 키트에서 복사되지 않으므로 다시 적용해도 남는다
  - `commit-types`: 종류 목록 (예: `feat|fix|build`). 커밋과 브랜치가 함께 쓴다
  - `branch-rules`: 기준 브랜치, 직접 push를 막는 브랜치, 태그 이름 (기본 `base=main dev`, `protected=main dev`, `tag-pattern=^(release/)?v[0-9]+\.[0-9]+\.[0-9]+$`)
- Spec Kit을 쓰는 저장소(키트 `--spec-kit`로 적용)는 스펙 번호 겹침도 검사한다: `docs/spec-numbers.md`

### 빌드·테스트 (앱 CI)

키트 앱 템플릿의 CI(백엔드 `backend-ci`, 프론트 `frontend-ci`)가 돈다. 빠른 검사는 매번, 무거운 검사는 한 번만 돈다.

| 검사 | 작업 브랜치 → `dev` PR | `dev` push, `dev` → `main` PR |
|---|---|---|
| 빠른 검사: 포맷·lint·타입·단위 테스트 (백엔드 `build`, 프론트 `check`) | 돈다 | 돈다 |
| 무거운 검사: 인수 테스트 (백엔드 `db-test`), 화면 검사 (프론트 `e2e`) | 돈다 | 건너뛴다 (같은 내용이다) |

- `dev` push에서 빠른 검사를 다시 도는 것은, 따로 통과한 PR 둘이 합쳐져 깨진 것을 병합 직후에 잡으려는 것이다 (PR 검사는 그때의 `dev` 기준이다)
- `main` push에서는 돌지 않는다. `main`에는 `dev` → `main` PR만 들어오고 그 PR 검사가 곧 병합 결과다
- 같은 PR에 새 커밋을 올리면 앞선 실행은 취소된다 (`concurrency`). 모든 job에 시간 제한(`timeout-minutes`)을 건다
- 무거운 job은 `if: github.base_ref == 'dev'`로 건다. 새로 더하는 무거운 job도 같은 조건을 건다
- 로컬에서 무엇을 돌리는지는 저장소의 `CLAUDE.md`가 정한다. CI는 위 표대로 돈다
