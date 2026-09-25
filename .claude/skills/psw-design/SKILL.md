---
name: psw-design
description: 승인된 harness-psw REQ를 근거로 설계 문서를 작성한다. 아키텍처·보안 설계(외부 연동 포함), UI 기초(프론트 키트의 테마·컴포넌트 적용, IA, 셸), 백엔드 기초(백엔드 키트 골격 적용, 문서 조각 반영), 코드 규칙(conventions.md), 도메인별 화면 목록과 HTML 목업, 구현 착수 전 프로젝트별 결정(도구·경로)을 채운다. REQ가 승인된 도메인의 설계를 시작하거나 설계를 고칠 때 사용한다.
---

# psw-design

`docs/design/`, 프론트 테마·컴포넌트, 백엔드 골격을 쓴다.

UI는 디자인을 새로 만들지 않는다. 프론트 키트(`harness-psw-frontend`)의 테마와 컴포넌트를 가져와 쓴다.
백엔드도 골격을 새로 짜지 않는다. 백엔드 키트(`harness-psw-backend`)의 골격을 적용해 시작한다.
테이블과 도메인 API는 문서로 쓰지 않는다. 코드가 정본이고, 공통 규칙만 `conventions.md`에 둔다 (harness-psw 4.2).

## 입력과 출력

| 입력 | 출력 |
|---|---|
| `approved` REQ (도메인 단위) | `docs/design/architecture.md`, `security.md` |
| `docs/req/README.md` 제약 절 (기술 스택) | `docs/design/conventions.md` |
| 프론트 키트 (`harness-psw-frontend`, 태그) | 프론트 테마와 컴포넌트, `docs/design/ui/` (ia, kit, screens, ui-rules, components) |
| 백엔드 키트 (`harness-psw-backend`, 태그) | 백엔드 골격 코드, 문서 조각(REQ 초안은 기획 루프로) |
| 사용자 결정 | `CLAUDE.md` 명령, `.claude/psw.conf` (구현 준비) |

## 규칙

- 전제: 설계할 도메인의 REQ가 `approved`다
- 템플릿: `references/*.md`
- 프론트 키트
  - 저장소: `https://github.com/melly8954/harness-psw-frontend.git` (비공개). 로컬 사본 `C:\psw\github\harness-psw-frontend`가 있으면 그 경로를 써도 된다
  - 목업 템플릿, 컴포넌트 조각, 기본 컴포넌트 목록, 프레임워크별 적용 절차는 키트가 소유한다. 하네스에 복사해 두지 않는다
- 백엔드 키트
  - 저장소: `https://github.com/melly8954/harness-psw-backend.git` (비공개). 로컬 사본 `C:\psw\github\harness-psw-backend`가 있으면 그 경로를 써도 된다
  - 골격 구조, 라이브러리, 헬퍼, 적용 스크립트와 절차(`<프레임워크>/APPLY.md`)는 키트가 소유한다. 하네스에 복사해 두지 않는다
  - 키트의 문서 조각 중 REQ 초안은 설계에서 REQ로 만들지 않는다. `psw-change`로 기획 루프를 호출한다
- 순서: 아키텍처 → UI 기초 · 백엔드 기초 → 코드 규칙 → 화면
- 문서
  - 전역: `architecture.md`, `security.md`, `conventions.md`, `ui/ia/README.md`, `ui/ui-rules.md`, `ui/components.md`, `ui/kit/shell.js`
  - 도메인별: `ui/ia/<domain>.md`, `ui/screens/<domain>/`
- 쓰지 않는 문서: ERD, 도메인별 API 명세, 상태 전이 문서, 연동 문서
  - 테이블·API는 코드가 정본이다. 규칙은 `conventions.md` DB·REST 절에 둔다
  - 상태 전이는 도메인 `_policy.md`가 소유한다. 설계 중 빠진 전이를 발견하면 기획 루프를 호출한다
  - 외부 연동 설계는 `architecture.md` 외부 연동 절에 둔다
- 모든 설계 문서의 frontmatter에 `status`와 `refs`(근거 요구사항 ID)를 적는다
- 다른 곳이 정본인 값은 옮겨 적지 않고 참조한다
  - 상태값: `_policy.md` / 테마·컴포넌트: 프론트 코드 (경로는 `architecture.md` UI 절) / 정책 수치: `_policy.md` / 역할별 권한: `docs/req/actors.md` / 에러 코드 목록: 백엔드 코드
- 교차 검증 스크립트가 읽는 형식을 지킨다 (각 템플릿의 주석 참고)
  - `ui/ia/<domain>.md` 화면 목록 행, 목업 첫 줄 메타 주석, 목업의 `data-component`와 `components.md` 첫 열
- 화면 ID는 `.claude/scripts/psw/next-id.sh SCR-<도메인>`으로 발급한다
- 요구사항에 결함이 있으면 설계에서 고치지 않는다. `psw-change`로 기획 루프를 호출한다
- 모르는 것은 `psw-change`로 미결을 등록한다
- 프로젝트별 결정(도구, 경로)은 사용자가 정하고, 기록 위치에 `〔YYYY-MM-DD〕 <이유>`를 붙여 적는다 (harness-psw 8.1)
- `approved`로는 바꾸지 않는다

## 절차

### 1. 아키텍처 (전역, 첫 도메인에서 만들고 이후 갱신)

1. `docs/req/README.md` 제약 절의 기술 스택과 적용 NFR·SEC·INT를 읽는다
2. `architecture.md`, `security.md`를 쓴다
   - 연동 요구사항이 있으면 `architecture.md` 외부 연동 절에 호출·실패 처리·테스트 환경을 쓴다
3. 프로젝트별 결정: 프론트 키트의 프레임워크, 태그, 테마를 사용자에게 정하게 하고 `architecture.md` UI 절에 적는다
   - 키트에 없는 프레임워크면 멈추고 알린다. 키트에 프레임워크를 먼저 추가한다
4. 프로젝트별 결정: 백엔드 키트의 프레임워크, 태그, 헬퍼, 패키지 이름을 사용자에게 정하게 하고 `architecture.md` 백엔드 절에 적는다
   - 태그 조회: `git ls-remote --tags --sort=-v:refname <키트 저장소> 'v*'`. 권장은 가장 최근 태그
   - 헬퍼 목록과 용도는 키트의 `<프레임워크>/helpers/*/HELPER.md`
   - 키트에 없는 프레임워크면 멈추고 알린다. 키트에 프레임워크를 먼저 추가한다

### 2. UI 기초 (전역, 첫 도메인에서 만들고 이후 갱신)

1. 프론트 키트를 고른 태그로 임시 폴더에 가져온다
   - 권장: 가장 최근 태그. 키트는 검증을 마친 커밋에만 태그를 단다
   - 조회: `git ls-remote --tags --sort=-v:refname <키트 저장소> 'v*'`. 태그마다 바뀐 점은 태그 메시지에 있다
   - `git clone --depth 1 --branch <태그> <키트 저장소> <임시 폴더>`
2. 키트의 `web/frameworks/<프레임워크>/README.md` 절차를 따른다. 프론트 코드는 `frontend/`에 둔다 (설계서 2절)
   - 프레임워크 초기화, UI 라이브러리 초기화, 테마 적용, 컴포넌트 추가
   - 절차가 정한 테마 파일과 컴포넌트 코드 경로를 `architecture.md` UI 절에 적는다. 구현자는 이 경로를 공유 파일로 보고 고치지 않는다
   - 키트의 `web/components.md`를 `docs/design/ui/components.md`로 복사하고, 쓰지 않을 컴포넌트는 뺀다
3. `ui/ui-rules.md`를 쓴다
   - 커스터마이즈는 테마 변수로만 한다. 컴포넌트 코드의 스타일을 직접 바꾸지 않는다
4. `ui/ia/README.md`에 도메인 색인, 셸 없는 화면, 메뉴 계층, 화면 흐름을 쓴다 (`references/ia-readme.md`)
   - 메뉴를 관리 기능으로 바꾸는 요구사항이 있으면, 메뉴 계층이 초기 메뉴라는 것과 근거 FR을 적는다
5. 셸을 만든다
   - 형태와 설정(키트 셸 틀의 설정 구역)을 사용자에게 정하게 하고 `architecture.md` UI 절에 적는다
   - 키트의 셸 틀을 `ui/kit/shell.js`로 복사하고, 설정과 메뉴만 고친다. 방법은 키트의 `web/mockup/README.md`
   - 메뉴는 `ia/README.md` 메뉴 계층을 그대로 옮긴다. 메뉴가 바뀌면 `ia/README.md`를 먼저 고친다

### 2-B. 백엔드 기초 (전역, 첫 도메인에서 한 번)

1. 백엔드 키트를 고른 태그로 임시 폴더에 가져온다: `git clone --depth 1 --branch <태그> <키트 저장소> <임시 폴더>`
2. 키트의 `<프레임워크>/APPLY.md` 절차대로 `backend/`에 적용한다 (`apply.sh`, 헬퍼·패키지 지정)
3. 문서 조각을 반영한다 (조각마다 `docs-fragments/<조각>/README.md`)
   - REQ 초안: `psw-change`로 기획 루프를 호출해 넘긴다. 기획 루프가 범위를 확인하고 REQ로 만든다
   - 조각의 역할 이름(예: ADMIN)이 `docs/req/actors.md`와 다르면 코드를 actors.md에 맞춘다
   - 다 넣었으면 `docs-fragments/`를 지운다
4. `architecture.md`를 채운다
   - 백엔드 절: 키트 태그·헬퍼, 코드 루트, DB 마이그레이션 경로, 테스트 위치, 사용자 도메인을 구현할 때 고칠 골격 테스트
   - 모듈 경계 절: 키트가 정한 레이어와 의존 방향. 강제 수단(키트의 레이어 검사 테스트)도 적는다

### 3. 코드 규칙 (전역, 첫 도메인에서 만들고 이후 갱신)

1. `conventions.md`를 템플릿에서 시작한다 (`references/conventions.md`)
2. 키트를 적용했으면 키트가 따르는 규칙을 옮긴다
   - 백엔드·DB·REST 절: 백엔드 키트 `APPLY.md`와 골격 코드
   - REST 오류 응답 절의 형식과 기본 에러 코드는 백엔드 키트 골격과 같으므로 바꾸지 않는다. 바꿔야 하면 코드도 함께 바꾼다
   - 프론트 절: 프론트 키트의 프레임워크 절차
3. 키트가 정하지 않은 규칙은 사용자에게 선택지와 추천안을 제시해 정한다. 정하지 못한 것은 미결로 등록한다
4. 규칙마다 강도(`[MUST]`, `[SHOULD]`, `NEVER`)를 붙인다

### 4. 도메인 화면

1. `ui/ia/<domain>.md`에 화면 목록을 쓴다 (`references/ia-domain.md`)
   - 화면 ID는 사용자가 보는 화면 단위다. 모달과 단계형 폼의 각 단계도 화면이다
   - 접근 역할은 `docs/req/actors.md`와 같게 쓴다
2. 화면마다 `ui/screens/<domain>/<screen>.html` 목업을 만든다
   - 만드는 방법은 키트의 `web/mockup/README.md`를 따른다. 키트는 `architecture.md`에 적은 태그로 가져온다
   - 첫 줄은 메타 주석이다. `crosscheck.sh`가 읽는 형식이라 바꾸지 않는다
     `<!-- psw id: SCR-ORD-001 | refs: FR-ORD-001, FR-ORD-003 | status: draft -->`
   - 셸은 `ui/kit/shell.js`를 불러온다. 셸 없는 화면 목록에 있는 화면은 불러오지 않는다
   - 테마는 프론트 코드의 테마 파일을 상대 경로로 연결한다. 복사하지 않는다
   - 컴포넌트는 키트 조각을 그대로 쓰고, 컴포넌트를 쓴 요소에 `data-component`를 단다
   - 기본, 빈 상태, 로딩, 오류 네 가지 상태를 채운다
   - `components.md`에 있는 컴포넌트만 쓴다. 새 컴포넌트가 필요하면 `components.md`에 먼저 추가하고 프로젝트에 컴포넌트를 추가한다
3. `ia/README.md` 도메인 색인의 화면 수를 고친다

### 5. 교차 검증

- `psw-crosscheck`를 실행한다. 불일치가 없어질 때까지 4단계와 반복한다

### 6. 승인 요청 (도메인 단위)

1. `.claude/scripts/psw/loop-status.sh docs/design`으로 종료 조건을 확인한다
2. 사용자에게 도메인 설계 승인을 요청한다
   - 요약: 화면 수, 새로 정하거나 바꾼 코드 규칙, 남은 미결, 교차 검증 결과
   - 목업은 파일 경로를 알려 사용자가 브라우저로 열어보게 한다. 여는 데 필요한 조건(인터넷 연결 등)은 키트 `web/mockup/README.md`를 보고 함께 알린다
3. 승인은 사용자가 입력창에서 직접 실행한다: `! bash .claude/scripts/psw/approve.sh <설계 문서·목업 경로>`
   - 에이전트의 승인 시도는 hook이 막는다
4. 키트의 조각과 설치한 컴포넌트의 클래스가 다르면 키트를 고치도록 사용자에게 알린다

### 7. 구현 준비 (구현 착수 전 한 번)

1. 프로젝트별 결정을 사용자와 정하고, 기록 위치에 날짜·이유와 함께 적는다
   - 시크릿 저장소 도구 → `architecture.md` 배포 단위
   - lint, 포맷, 커밋 검사, 시크릿 스캔, AC 테스트 도구 → `conventions.md` 도구 절
     - 백엔드 키트의 기본 도구(포맷·lint·AC 테스트·CI)를 그대로 쓰면 근거에 키트 태그를 적는다
     - 커밋 검사 도구의 기본안은 하네스 스크립트(`check-commit-msg.sh`)다. 스택과 상관없이 동작한다
   - AC 테스트 작성법 → `conventions.md` AC 테스트 절 (백엔드는 키트 `APPLY.md`의 AC 테스트 절에서 옮긴다)
   - 개발 서버, lint, AC 테스트 명령 → `CLAUDE.md` 명령 표 (백엔드는 키트 `APPLY.md`의 명령 표에서 가져온다)
   - AC 테스트 파일 경로 → `.claude/psw.conf`의 `PSW_TEST_GLOBS` (`templates/psw.conf`에서 시작)
     → 역할별 경로 검사가 이 패턴으로 테스트 파일을 가린다
     - 백엔드 테스트 위치(`architecture.md` 백엔드 절)를 반드시 넣는다. 기본 패턴은 `src/test/java/` 같은 경로를 잡지 못한다
2. 커밋 검사 hook과 CI는 `psw-implement` 준비 단계에서 설정한다 (`templates/githooks/`, `templates/github-workflow-psw.yml`)
3. 다음 단계: `psw-implement`

## 커밋

- 트레일러: `Refs:`에 근거 요구사항 ID, 미결을 해결했으면 `Closes:`
- 메시지 예: `docs: 주문 도메인 화면 설계`
