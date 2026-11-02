# moa

게임 커뮤니티 + 피드형 SNS + 메신저 (피드, 파티 모집, 팔로우, 메시지, 알림, 평판). 백엔드 Spring Boot(`backend/`), 프론트 Next(`frontend/`).

## 문서

| 문서 | 내용 |
|---|---|
| `docs/specs/README.md` | 스펙 쓰는 법과 흐름. 기능별 스펙은 `docs/specs/<도메인>/<기능>.md` |
| `docs/backend.md` | 백엔드 구조와 코드 규칙 (레이어, REST, 인증, DB, 테스트) |
| `docs/frontend.md` | 프론트 구조와 코드 규칙 (디렉터리, API 호출, 다국어, 화면 규칙, 테스트) |

- 작업 전에 해당 스펙과, 건드리는 쪽의 구조 문서만 읽는다
- 테이블은 DB 마이그레이션, API는 백엔드 코드(springdoc)가 정본이다

## 규칙

- 스펙에 없는 동작을 추측으로 정하지 않는다. 스펙의 "미결" 절에 적고 묻는다
- 테스트를 통과시키려고 테스트를 고치지 않는다
- 시크릿 값을 읽거나 출력하거나 커밋하지 않는다
- 브랜치: main(배포) ← dev(개발) ← feat/<도메인>-<기능>

## 명령

| 용도 | 명령 |
|---|---|
| 개발 서버 (백엔드) | `cd backend && ./gradlew bootRun` (local 프로필, Docker로 DB 자동 기동) |
| 개발 서버 (프론트) | `cd frontend && npm run dev` |
| lint·포맷 (백엔드) | `cd backend && ./gradlew spotlessCheck checkstyleMain checkstyleTest` (포맷 적용: `./gradlew spotlessApply`) |
| lint·포맷·타입 (프론트) | `cd frontend && npm run lint && npm run format:check && npm run typecheck` (포맷 적용: `npm run format`) |
| 테스트 (백엔드) | `cd backend && ./gradlew test` (Docker 필요). 하나만: `--tests '<클래스>'` |
| 테스트 (화면) | `cd frontend && npm run test:e2e` (처음 한 번 `npx playwright install chromium`). 하나만: `npx playwright test <파일>` |
| 커밋 hook 설정 (처음 한 번) | `git config core.hooksPath .githooks` (gitleaks 필요) |
