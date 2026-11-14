# moa

게임 커뮤니티 + 피드형 SNS + 메신저. 모노레포로 백엔드는 `backend/`(Spring Boot), 프론트는 `frontend/`(Next)에 둔다.
스펙 주도 개발(Spec Kit)로 진행한다.

## 문서

| 위치 | 내용 |
|---|---|
| `.specify/memory/constitution.md` | 프로젝트 원칙. 모든 작업에 적용된다. 이 문서와 충돌하면 원칙을 따른다 |
| `specs/README.md` | 도메인·기능 지도. 무엇이 키트에서 왔고 무엇을 moa가 만드는지 |
| `docs/git.md` | git 규칙 (흐름, 브랜치·커밋·태그, 검사). psw-dev-kit `git` 템플릿 |
| `specs/<번호>-<이름>/` | 기능 스펙 (spec·plan·tasks 등). 첫 `/speckit-specify` 때 생긴다 |

- 작업 전에 해당 기능의 스펙과 원칙 문서만 읽는다. `specs/` 전체를 한꺼번에 읽지 않는다
- 원칙 내용을 이 파일에 다시 적지 않는다
- 결정 기록 문서를 따로 두지 않는다. 결정과 그 배경은 해당 기능의 스펙에 적고, 바뀌면 그 자리에서 고친다

## 작업 흐름

```text
/speckit-specify → (/speckit-clarify) → /speckit-plan → /speckit-tasks
→ (/speckit-analyze) → 문서 PR 병합
→ /speckit-implement → (/speckit-converge) → 구현 PR
```

- 새 스펙은 번호를 먼저 예약하고, `/speckit-specify`에 예약한 번호를 명시한다 (`specs/README.md` "스펙 번호")
- 기존 기능을 바꿀 때는 새 스펙 폴더를 만들지 않고 그 기능의 spec.md를 고친다
- 키트에서 온 기능도 도입할 때 스펙을 만들고, 머리에 출처(태그·헬퍼)를 적는다
- 스펙 작업 브랜치는 `docs/<이름>`, 구현 브랜치는 `feat/<스펙 번호>-<이름>`이다. spec·plan·tasks를 먼저 PR로 병합한 뒤 구현 브랜치를 딴다

## git

@docs/git.md

- 브랜치 정리 (psw-gongbu와 같다, 키트 `git` 템플릿으로 옮길 것)
  - 작업 브랜치 → `dev` PR을 병합하면 그 작업 브랜치를 원격과 로컬에서 함께 지운다
    - 원격: `git push origin --delete <브랜치>`
    - 로컬: `git checkout dev && git pull --ff-only && git branch -d <브랜치>`
  - `dev` → `main` 병합에서는 아무 브랜치도 지우지 않는다
  - 저장소 설정 "Automatically delete head branches"는 끈다. 켜 두면 `dev` → `main` 병합 때 head인 `dev`가 지워질 수 있다

## 작업 기록

- 답변 끝에 정해진 양식을 붙이지 않는다
- 기능 완료는 PR 설명에, 버그의 원인과 해결 과정은 GitHub 이슈에 남긴다

## 명령

백엔드는 `backend/`에서 실행한다. Windows는 `./gradlew` 대신 `.\gradlew.bat`.

| 용도 | 명령 |
|---|---|
| 로컬 실행 | `./gradlew bootRun` (local 프로필, Docker로 DB를 함께 띄운다. 시험 계정 `admin`·`user`) |
| 전체 검사 | `./gradlew build` (포맷·lint·테스트. DB 테스트 제외) |
| DB 테스트 (AC) | `./gradlew dbTest` (Docker 필요) |
| 포맷 적용 | `./gradlew spotlessApply` |

프론트는 `frontend/`에서 실행한다.

| 용도 | 명령 |
|---|---|
| 로컬 실행 | `npm run dev` (http://localhost:3000. 백엔드는 따로 띄운다) |
| 전체 검사 | `npm run check` (포맷·lint·타입·색 검사·문구 검사·단위 테스트) |
| 화면 검사 | `npm run test:e2e` (처음 한 번 `npx playwright install chromium`) |
| API 타입 만들기 | `npm run gen:api` (local 프로필 백엔드가 떠 있어야 한다) |
| 포맷 적용 | `npm run format` |

- 화면 검사와 DB 테스트를 언제 돌리는지는 `docs/git.md` "빌드·테스트 (앱 CI)"를 따른다 (PR 전 로컬에서는 빠른 검사만)

- 키트 사용법은 psw-dev-kit `springboot/v0.5.0`의 `backend/springboot/README.md`, `web-next/v0.5.0`의 `frontend/web/next/README.md`를 본다
