---
status: draft
refs: [CON-001, FR-REL-003, NFR-I18N-001, SEC-AUTH-004, SEC-AUTH-005, SEC-PRIV-002]
---

# 코드 규칙

<!-- 이 프로젝트 코드 규칙의 정본이다. 백엔드·프론트·DB·REST와 둘 사이의 연관 규칙을 모두 담는다 (harness-psw 4.6) -->
<!-- 도구로 강제하는 규칙은 설정 파일이 정본이다. 여기에는 도구, 이유, 예외만 적는다 (harness-psw 5.1) -->

## 적용 범위

- 대상: `frontend/`, `backend/`, DB 마이그레이션
- 규칙과 코드가 다르면 규칙이 맞다. 코드를 고친다. "고치지 않는다"고 적은 예외는 뺀다
- 모호하면 추측하지 않고 `docs/open-question.md`에 등록한다
- 백엔드 규칙의 출처: 백엔드 키트 v0.2.0 `springboot/APPLY.md`와 골격 코드. 프론트 규칙의 출처: 프론트 키트 v0.8.0 `web/frameworks/next/README.md`. 출처 표시가 없는 규칙은 이 프로젝트에서 정했다 〔2026-10-01〕

## 강도 표기

- `[MUST]` 지킨다. 어기면 결함이다
- `[SHOULD]` 지킨다. 어기면 이유를 커밋 본문이나 이 문서 예외 표에 적는다
- `NEVER` 하지 않는다

## 1. 백엔드

### 1.1 패키지 구조와 레이어 [MUST]

```text
com.moa
  api/
    controller/v1/{request,response}   Api<리소스>Controller      → Application만 부른다
    application/v1/{command,query}     <리소스>Application         → Service를 조합한다
    service/                           업무 규칙, 트랜잭션 경계    → Reader·Writer만 부른다
    repository/{reader,writer}         조회·변경 창구              → Repository만 부른다
    repository/jpa/                    Spring Data JPA, QueryDSL
    entity/  dto/
  common/                              응답·예외·보안·감사·설정·외부 연동 (어느 레이어든 쓸 수 있다)
```

- 의존 방향: `docs/design/architecture.md` 모듈 경계 절. 검사: `ArchitectureTest`(ArchUnit) (키트)
- 외부 연동은 `common/<연동>/`에 포트(인터페이스)와 어댑터를 둔다. Service는 포트만 부른다 (예: `common/mail/MailSender`, `SmtpMailSender`, `LogMailSender`)
- 실시간 전달은 `common/realtime/`의 포트(예: `RealtimePublisher`)로 한다. Service는 STOMP 템플릿을 직접 쓰지 않는다
- 예약 작업은 `api/application/v1/`의 `<업무>Scheduler`가 Application을 부르고, 저장은 `SystemActor.run(...)` 안에서 한다 (키트)

### 1.2 이름 [MUST]

- 코드 이름은 `docs/glossary.md`의 코드 이름을 따른다
- 클래스 (키트)
  - 컨트롤러 `Api<리소스>Controller`, 응용 `<리소스>Application`, 업무 `<리소스>Service`, 조회 `<리소스>Reader`, 변경 `<리소스>Writer`
  - 요청 `<리소스><동작>Request`, 커맨드 `<동작><리소스>Command`, 조회 조건 `<리소스>Query`, 계층 간 `<리소스>Dto`, 응답 `<리소스>GetResponse`(단건)·`<리소스>ListResponse`(목록 항목)
  - DTO는 record
- 메서드 동사: 조회 `get`(없으면 예외)·`find`(Optional), 목록 `list`, 생성 `create`, 수정 `update`, 삭제 `delete`, 상태 바꾸기는 업무 동사(`suspend`, `close`, `accept`)

### 1.3 트랜잭션 [MUST]

- 트랜잭션은 Service 메서드에서 연다 (키트). Application은 여러 Service를 조합하되 트랜잭션을 열지 않는다
- 외부 호출(메일, 문자, 저장소)은 트랜잭션 밖에서 한다. 트랜잭션 안에서 하면 실패 때 DB만 되돌아가고 발송은 남는다
- 실시간 전달은 커밋 뒤에 보낸다 (`@TransactionalEventListener(phase = AFTER_COMMIT)`)

### 1.4 응답과 예외 [MUST]

- 응답·오류 형식은 5절 REST를 따른다
- 업무 오류는 `throw new ServiceException(ErrorCode.X)`. 필드별 사유가 있으면 `errors`를 함께 준다 (키트)
- 도메인 오류 코드는 `ErrorCode`에 추가한다. 코드 목록의 정본은 `ErrorCode`다

### 1.5 검증 [MUST]

- 형식 검증(길이, 패턴, 필수)은 요청 DTO의 Bean Validation, 업무 규칙(중복, 상태, 권한 조건)은 Service
- 정책 수치(닉네임 길이, 첨부 용량 등)는 `_policy.md`가 정본이다. 코드에서는 상수 한 곳(`<도메인>Policy`)에 두고 화면 검증과 같은 값을 쓴다

### 1.6 설정과 환경변수 [MUST]

- 환경변수는 `application.yml`에서만 읽고, `@ConfigurationProperties` + `@Validated`로 시작할 때 검증한다 (키트, harness-psw 5.3)
- 설정 접두사는 `psw.*` (키트). 설정 클래스는 그 설정을 쓰는 `common/<영역>/` 패키지에 둔다
- 목록은 `backend/.env.example`. 새 환경변수는 같은 커밋에서 추가한다

### 1.7 보안 [MUST]

- 역할별 권한의 정본은 `docs/req/actors.md`다
- 기본은 모든 API가 인증을 요구한다. 로그인 없이 부를 API는 `@PermitAll` (키트)
- 역할 검사는 컨트롤러의 `@PreAuthorize("hasRole('MEMBER')")`. 조건부 권한은 Service
- 로그인 사용자: `@AuthenticationPrincipal AuthPrincipal principal` (키트)
- 설계: `docs/design/security.md`
- NEVER: 다른 회원에게 가는 응답 DTO에 이메일·생년월일·휴대폰 번호 필드를 두지 않는다 (SEC-PRIV-002)

### 1.8 API 문서 [SHOULD]

- API 명세는 코드에서 만든다: springdoc(Swagger UI), local 프로필에서만 켠다 (키트)
- 컨트롤러와 DTO에 `@Operation`, `@Schema`로 요약을 단다. 정본은 코드다 (키트 APPLY.md의 "`docs/design/api/*.md`가 정본"은 쓰지 않는다. 하네스는 API 문서를 두지 않는다)

### 1.9 하지 않는 것

- NEVER: 컨트롤러에서 Service·Repository를 직접 부르지 않는다
- NEVER: 엔터티를 컨트롤러 응답으로 내보내지 않는다
- NEVER: Service에서 외부 SDK를 직접 부르지 않는다

## 2. DB

### 2.1 이름 [MUST]

- 테이블: 복수형 snake_case (`members`, `follows`, `party_members`)
- 컬럼: snake_case, FK는 `<테이블 단수>_id` (`member_id`)
- 제약·인덱스: 유니크 `uk_<테이블>_<컬럼>`, 인덱스 `idx_<테이블>_<컬럼>`, FK `fk_<테이블>_<참조 테이블>`

### 2.2 표준 컬럼과 삭제 [MUST]

- 감사 컬럼: `created_at`, `created_by`, `updated_at`, `updated_by` (키트 `BaseEntity`)
- 삭제는 소프트 삭제가 기본이다: `deleted_at`, `deleted_by` (키트 `SoftDeleteEntity`, harness-psw 4.7)
- 행위자 개념이 없는 테이블(로그, 토큰)은 시각 컬럼만 둔다 (`BaseTimeEntity`)
- 소프트 삭제 테이블의 유니크는 가상 컬럼 `active_key`로 활성 행에만 건다 (키트 APPLY.md 엔터티·감사)

| 하드 삭제 테이블 | 이유 |
|---|---|
| `refresh_tokens` | 토큰. 키트 골격 |
| 로그인 세션 | 끊기거나 리프레시 수명(14일)이 지난 세션은 예약 작업이 지운다 (`security.md` 로그인 세션) |
| 인증 링크·인증 번호 | 한 번 쓰면 끝나는 값. 만료 뒤 정리 |
| 알림 | 보관 기간(30일)이 지나면 지운다 (`notification/_policy.md`) |
| 좋아요, 팔로우, 차단 | 관계 행. 끊으면 이력 없이 없앤다 |
| 붙지 않은 업로드 | 하루 뒤 정리 (`architecture.md` 파일 저장소) |
| 파기된 회원 개인정보 | 탈퇴 유예가 지나면 파기한다 (DAT-RET-001). 회원 행은 남기고 개인정보 컬럼을 지운다 |

### 2.3 타입 [MUST]

- ID `bigint` 자동 증가, 시각 `datetime(6)` UTC, 상태 `varchar(30)`, 불리언 `tinyint(1)`, 짧은 문자열 `varchar(n)`, 본문 `text`
- 문자셋 `utf8mb4`, 정렬 `utf8mb4_unicode_ci`. 닉네임 대소문자 무시 중복 검사는 이 정렬로 한다
- 상태 컬럼의 값은 도메인 `_policy.md` 상태 전이 절이 정본이다

### 2.4 인덱스와 제약 [SHOULD]

- FK마다 인덱스. 목록 조회의 정렬 키(`created_at desc, id desc`)에 복합 인덱스
- FK 제약은 건다. 소프트 삭제 행을 참조하는 FK는 그대로 둔다

### 2.5 마이그레이션 [MUST]

- 위치: `docs/design/architecture.md` 백엔드 절
- 이미 적용된 마이그레이션 파일은 고치지 않는다. 바꿀 것은 새 파일로 추가한다 (harness-psw 9.3)
- 이름: `V<번호>__<설명>.sql`, 번호는 `V2`부터 (골격 `V1`) (키트)
- 스키마는 `ddl-auto: validate`로 엔터티와 맞는지 시작할 때 검사한다 (키트)

## 3. 프론트

### 3.1 디렉터리 [MUST]

```text
frontend/
  app/                    라우트. 경로는 IA 화면 목록과 같다
  components/ui/          키트 컴포넌트 (공유 파일, 고치지 않는다)
  components/<도메인>/     앱 컴포넌트 (member, feed, party ...)
  lib/api/                API 호출과 응답 타입
  lib/realtime/           STOMP 연결
  messages/{ko,en}.json   화면 문구
```

- 공유 파일(테마, UI 컴포넌트 코드, 셸)은 `docs/design/architecture.md` UI 절

### 3.2 컴포넌트 [MUST]

- 공통 컴포넌트 목록: `docs/design/ui/components.md`. 새 컴포넌트는 목록에 먼저 추가한다
- 컴포넌트 코드의 스타일을 직접 바꾸지 않는다. 테마 변수로 바꾼다 (프론트 키트)
- 서버 컴포넌트가 기본이다. 상태·이벤트가 필요한 컴포넌트만 `"use client"`

### 3.3 API 호출 [MUST]

- API 호출은 `lib/api/` 한 곳에서만 한다. 화면은 TanStack Query 훅(`use<리소스>`)으로 부른다
- 액세스 토큰은 메모리에만 둔다. A002를 받으면 재발급을 한 번만 요청하고(동시 요청을 하나로 모은다), 실패하면 로그인으로 보낸다 (키트 알려진 제약)
- 요청은 `credentials: "include"`로 보낸다 (리프레시 쿠키)
- 응답 타입은 백엔드 OpenAPI 문서에서 `openapi-typescript`로 만든다 (`lib/api/schema.ts`, 손으로 고치지 않는다)

### 3.4 상태 관리 [SHOULD]

- 서버 상태는 TanStack Query, 화면 상태는 컴포넌트 안. 전역 상태 라이브러리는 두지 않는다
- 실시간으로 받은 메시지·알림은 해당 쿼리 캐시를 고쳐 반영한다

### 3.5 스타일 [MUST]

- 화면 규칙은 `docs/design/ui/ui-rules.md`
- 색은 테마 변수의 클래스만 쓴다

### 3.6 라우팅·다국어·환경변수 [MUST]

- 라우트는 `docs/design/ui/ia/`의 화면 목록 경로와 같다. 앞에 언어(`/ko`, `/en`)를 붙이지 않고 쿠키로 언어를 고른다 (next-intl, 기본 `ko`)
- 화면 문구는 `messages/ko.json`, `messages/en.json`에만 둔다. 컴포넌트에 한국어 문자열을 직접 쓰지 않는다 (NFR-I18N-001)
- 서버 오류는 `code`로 문구를 고른다 (`errors.<코드>`). 서버 `message`를 그대로 보여 주지 않는다
- 시각은 `Asia/Seoul`로 보여 준다
- 브라우저에 노출할 환경변수만 `NEXT_PUBLIC_` 접두사 (`NEXT_PUBLIC_API_URL`, `NEXT_PUBLIC_WS_URL`)

## 4. 백엔드 ↔ 프론트 연관 [MUST]

- API 경로를 바꾸면 프론트 호출 경로를 같은 커밋에서 바꾼다
- 요청·응답 DTO를 바꾸면 `lib/api/schema.ts`를 다시 만들어 같은 커밋에 넣는다
- 에러 코드를 추가하면 `messages/{ko,en}.json`의 `errors.<코드>`를 같은 커밋에서 추가한다
- 정책 수치(`<도메인>Policy`)를 바꾸면 화면의 zod 스키마도 같은 커밋에서 바꾼다
- STOMP 목적지·메시지 형식을 바꾸면 `lib/realtime/`을 같은 커밋에서 바꾼다

## 5. REST

### 5.1 기본 [MUST]

- 기본 경로: `/api/v1` (키트)
- 형식: JSON, UTF-8. 시각은 ISO-8601 UTC (`2026-10-01T03:00:00Z`)
- 경로: kebab-case 복수형 명사 (`/api/v1/parties/{id}/members`). 동사를 넣지 않고, 상태를 바꾸는 행동은 하위 자원으로 표현한다 (예: 방장 넘기기 `POST /api/v1/parties/{id}/leader-transfers`)
- 필드: camelCase

### 5.2 인증 [MUST]

- `Authorization: Bearer <액세스 토큰>`. 리프레시 토큰은 HttpOnly 쿠키 (키트)
- WebSocket: `/ws` STOMP 엔드포인트, `CONNECT` 프레임의 `Authorization` 헤더. 구독은 `/user/queue/messages`, `/user/queue/notifications`
- 상세: `docs/design/security.md`

### 5.3 오류 응답 [MUST]

<!-- 이 절의 형식과 기본 표는 백엔드 키트 골격과 같다 (harness-psw 4.7). 한쪽만 바꾸지 않는다 -->

```json
{
  "code": "V001",
  "message": "<사용자에게 보여줄 문장>",
  "timestamp": "<발생 시각, ISO-8601>",
  "path": "<요청 경로>",
  "traceId": "<요청 추적 ID. 서버 로그와 같은 값>",
  "errors": [{ "field": "<필드>", "message": "<사유>" }]
}
```

- `errors`는 입력 검증 실패(V001)일 때만 넣는다
- 코드는 접두사 + 세 자리 번호다
  - V: 입력 검증, A: 인증·인가, R: 리소스, B: 업무 규칙, S: 시스템
  - 도메인 오류는 해당 접두사의 다음 번호를 쓴다. 번호는 다시 쓰지 않는다
- 에러 코드 목록의 정본은 백엔드 코드의 에러 코드 정의다. 아래는 골격이 처음 가진 기본 코드다

| HTTP | 코드 | 의미 |
|---|---|---|
| 400 | V001 | 입력 검증 실패 |
| 405 | V002 | 지원하지 않는 요청 방식 |
| 415 | V003 | 지원하지 않는 요청 형식 |
| 401 | A001 | 인증 필요 (토큰 없음 또는 무효) |
| 401 | A002 | 액세스 토큰 만료 |
| 401 | A003 | 리프레시 토큰 무효 또는 만료 |
| 401 | A004 | 리프레시 토큰 재사용 감지 (해당 사용자 토큰 전부 폐기) |
| 403 | A005 | 권한 없음 |
| 404 | R001 | 대상 없음 |
| 409 | R002 | 이미 존재함 |
| 409 | B001 | 현재 상태에서 할 수 없는 요청 |
| 500 | S001 | 서버 오류 |

- 로그인 세션이 끊겼거나 회원이 ACTIVE가 아닌 요청도 A001이다 (`security.md` 로그인 세션)

### 5.4 페이징 [MUST]

- 쪽 번호 목록(관리 화면, 검색): `page`(0부터), `size`(기본 20, 최대 100) → `PageResponse` `{content, page, size, totalElements, totalPages}` (키트)
- 무한 스크롤 목록(피드, 메시지, 알림, 댓글, 팔로워): 커서 페이징 `cursor`, `size` → `CursorResponse` `{content, nextCursor}`. `nextCursor`가 null이면 끝 〔2026-10-01〕 새 글이 계속 쌓이는 목록에서 쪽 번호는 중복·누락이 생긴다. `CursorResponse`는 `common/response/`에 추가한다
- 페이징 없는 짧은 목록은 배열 그대로 (키트)

### 5.5 버전 [SHOULD]

- 경로에 버전을 둔다. 호환되지 않는 변경은 새 버전 경로로 낸다

### 5.6 하지 않는 것

- NEVER: 성공 응답에 HTTP 200과 오류 코드를 함께 담지 않는다
- NEVER: 존재를 숨겨야 하는 대상(나를 차단한 회원, 찾을 수 없는 회원과 그 게시물·모집)에 A005를 주지 않는다. R001로 답한다
- 비공개 계정의 게시물·팔로워·팔로잉 목록은 오류가 아니라 항목 없는 결과와 비공개 표시로 답한다 (FR-REL-003 AC-1)

## 6. 도구와 검증 명령

| 용도 | 도구 | 설정 파일 | 근거 |
|---|---|---|---|
| lint (백엔드) | Checkstyle (네이버 규칙, error) | `backend/config/checkstyle/` | 백엔드 키트 v0.2.0 |
| 포맷 (백엔드) | Spotless (네이버 Eclipse 포맷터) | `backend/config/` , `build.gradle` | 백엔드 키트 v0.2.0 |
| lint (프론트) | ESLint (eslint-config-next) | `frontend/eslint.config.mjs` | 프론트 키트 v0.8.0 |
| 포맷 (프론트) | (구현 준비에서 정한다) | | |
| 커밋 검사 | (구현 준비에서 정한다) | | |
| 시크릿 스캔 | (구현 준비에서 정한다) | | |
| AC 테스트 | JUnit 5 + MockMvcTester + Testcontainers(MariaDB) (백엔드) / 프론트는 구현 준비에서 정한다 | `backend/src/test/` | 백엔드 키트 v0.2.0 |

- 명령은 `CLAUDE.md` 명령 표에 둔다

### lint 예외

| 규칙 | 예외 범위 | 이유 |
|---|---|---|
| ESLint 전체 | `frontend/hooks/use-mobile.ts` | shadcn이 만든 코드. 고치지 않는다 (프론트 키트) |

## 7. AC 테스트 작성

- 기반: `AcceptanceTest`를 상속한다. 요청마다 트랜잭션이 따로 돌고(운영과 같다), 테스트가 끝나면 테이블을 비운다 (키트)
- 인증된 요청: `bearer(userId, roles...)` (키트). 로그인 세션 확인을 넣은 뒤에는 회원 행과 로그인 세션을 먼저 만든다
- 데이터 정리: `DatabaseCleaner`가 테스트마다 모든 테이블을 비운다. 시드 행이 필요하면 테스트 안에서 만든다 (키트)
- 외부 연동: 메일·문자는 `log` 어댑터를 대신하는 테스트 대역으로 보낸 내용을 확인한다. 저장소는 `local` 어댑터
- 이름: 표시 이름에 AC ID를 단다 (예: `@DisplayName("FR-MEM-002 AC-1: 이메일로 로그인")`)
- 계약: 구현자가 먼저 쓴 컨트롤러·DTO로 경로와 형식을 확인한다 (harness-psw 9.2)

## 8. 주석 예외

| 위치 | 예외 | 이유 |
|---|---|---|
| (없음) | | |
