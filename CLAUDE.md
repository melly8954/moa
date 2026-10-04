# moa

(인터뷰 후 작성). 상세는 docs/req/README.md

## 작업별 진입 경로

| 작업 | 먼저 읽을 문서 |
|---|---|
| 기획 (인터뷰, REQ) | docs/req/README.md → 해당 도메인 README.md |
| 설계 | 해당 FR 파일 → docs/design/architecture.md → docs/design/conventions.md → 해당 화면(docs/design/ui/ia/) |
| 구현 | 해당 FR 파일 → docs/design/conventions.md → FR ID로 검색한 화면·목업 |
| 검증 | 해당 FR 파일의 수용 기준 → docs/design/conventions.md AC 테스트 절 |
| 미결·결정 | docs/open-question.md, 결정은 각 문서의 〔날짜〕 표기 |

## 로딩 원칙

- 인덱스(README.md)를 먼저 읽고, 필요한 파일만 읽는다
- docs/ 전체를 한꺼번에 읽지 않는다
- 테이블·API 문서는 없다. 테이블은 DB 마이그레이션, API는 백엔드 코드가 정본이다
- 코드는 frontend/와 backend/에 있다. 프레임워크 안내는 그 폴더의 CLAUDE.md가 불러온다

## 핵심 규칙

- MUST: 문서를 approved로 바꾸지 않는다. 승인은 사용자만 한다
- MUST: 하위 단계에서 상위 문서를 고치지 않는다. 해당 단계의 루프를 호출한다
- MUST: 승인된 내용을 바꿔야 하면 먼저 사용자에게 변경안을 보이고 수락을 받는다
- MUST: 모르는 것은 추측하지 않고 docs/open-question.md에 등록한다
- MUST: 테스트를 통과시키려고 테스트를 고치지 않는다
- NEVER: 시크릿 값을 읽거나 출력하거나 커밋하지 않는다
- MUST: 커밋에 트레일러(Refs, Closes)를 단다
- 브랜치: main(배포, 사용자만) ← dev(개발, 문서 커밋) ← feat/<FR-ID>-<요약>(FR 구현)

## 명령

| 용도 | 명령 |
|---|---|
| 개발 서버 (백엔드) | `cd backend && ./gradlew bootRun` (local 프로필, Docker로 DB 자동 기동) |
| 개발 서버 (프론트) | `cd frontend && npm run dev` |
| lint·포맷·타입 검사 (백엔드) | `cd backend && ./gradlew spotlessCheck checkstyleMain checkstyleTest` (포맷 적용: `./gradlew spotlessApply`) |
| lint·포맷·타입 검사 (프론트) | `cd frontend && npm run lint && npm run format:check && npm run typecheck` (포맷 적용: `npm run format`) |
| AC 테스트 (API) | `cd backend && ./gradlew test` (Docker 필요). 전체 검사: `./gradlew build` |
| AC 테스트 (화면) | `cd frontend && npm run test:e2e` (처음 한 번 `npx playwright install chromium`) |
| 테스트 하나만 실행 (구현 반복 중) | API: `cd backend && ./gradlew test --tests '<클래스>'`, 화면: `cd frontend && npx playwright test <파일>`. 전체 검사는 보고 직전에 한 번 |
| 커밋 hook 설정 (처음 한 번) | `git config core.hooksPath .githooks` (gitleaks 필요) |
| 설계 교차 검증 | `bash .claude/scripts/psw/crosscheck.sh [도메인]` |
| 루프 종료 확인 | `bash .claude/scripts/psw/loop-status.sh [경로]` |
