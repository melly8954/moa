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
- 태그는 배포 브랜치(`main`)에 병합할 때만, 그 머지 커밋에 단다. 새 기능이 들어가면 MINOR, 수정만 있으면 PATCH를 올린다
  - `dev` 병합에는 달지 않는다. PR 번호와 머지 커밋이 이력이 된다. 작업 브랜치는 태그와 관계없다
  - 이유: 태그는 저장소 전체에 하나뿐인 이름이라 브랜치별 버전을 둘 수 없고, dev 병합마다 올리면 번호가 "무엇이 배포됐나"를 말하지 못한다
- 주석 태그만 쓴다: `git tag -a v1.2.0 -m "v1.2.0: <무엇이 바뀌었나>"`
- 이미 올린 태그는 옮기거나 다시 쓰지 않는다. 고칠 것이 있으면 다음 번호로 단다
- 태그는 검사(빌드·테스트)를 통과한 커밋에만 단다. 올리기는 사용자가 요청할 때만: `git push origin refs/tags/<태그>`

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
- 저장소마다 다른 값은 `.githooks/` 아래 설정 파일에 둔다. 키트에서 복사되지 않으므로 다시 적용해도 남는다
  - `commit-types`: 종류 목록 (예: `feat|fix|build`). 커밋과 브랜치가 함께 쓴다
  - `branch-rules`: 기준 브랜치, 직접 push를 막는 브랜치, 태그 이름 (기본 `base=main dev`, `protected=main dev`, `tag-pattern=^v[0-9]+\.[0-9]+\.[0-9]+$`)
