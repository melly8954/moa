# 백엔드 구조

`backend/`의 구조와 코드 규칙이다. 도구로 강제하는 규칙(Checkstyle, Spotless, ArchUnit)은 설정 파일과 테스트가 정본이고, 여기에는 이유와 예외만 적는다.

## 기술 스택

| 영역 | 선택 |
|---|---|
| 언어·프레임워크 | Java 21, Spring Boot 3.5 |
| 데이터 | JPA + QueryDSL, Flyway, MariaDB 11.4 (운영 RDS, 로컬 Docker) |
| 실시간 | Spring WebSocket + STOMP 내장 브로커 (서버 한 대 전제) |
| 예약 작업 | Spring `@Scheduled` |
| 테스트 | JUnit 5 + MockMvcTester + Testcontainers(MariaDB) |
| lint·포맷 | Checkstyle(네이버 규칙), Spotless(네이버 Eclipse 포맷터). 설정은 `backend/config/` |

## 배포 구성

```text
브라우저 ─HTTPS→ 웹 프론트 (Vercel, www.<도메인>)
브라우저 ─REST /api/v1, STOMP /ws→ API 서버 (EC2 한 대, Docker, api.<도메인>)
브라우저 ─Presigned PUT·GET→ S3 (비공개 버킷)
API 서버 → RDS MariaDB, S3, SMTP(→ SES), 솔라피(문자), 구글·카카오 OAuth
```

- 프론트와 API가 같은 등록 도메인이라 리프레시 쿠키(`SameSite=Strict`)와 WebSocket이 그대로 동작한다
- 시크릿: 운영은 AWS SSM Parameter Store(EC2 인스턴스 역할로 읽음), 로컬은 커밋하지 않는 `.env`. 목록은 `backend/.env.example`
- 서버를 늘리려면 STOMP 외부 브로커와 예약 작업 잠금이 먼저 필요하다

## 패키지 구조와 레이어

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

- 의존 방향은 `ArchitectureTest`(ArchUnit)가 강제한다
- 외부 연동은 `common/<연동>/`에 포트(인터페이스)와 어댑터를 둔다. Service는 포트만 부른다 (예: `MailSender` ← `SmtpMailSender`, `LogMailSender`). 어댑터는 설정으로 고른다
- 실시간 전달은 `common/realtime/`의 포트(예: `RealtimePublisher`)로 한다. Service는 STOMP 템플릿을 직접 쓰지 않는다
- 예약 작업은 `api/application/v1/`의 `<업무>Scheduler`가 Application을 부르고, 저장은 `SystemActor.run(...)` 안에서 한다

### 하지 않는 것

- 컨트롤러에서 Service·Repository를 직접 부르지 않는다
- 엔터티를 컨트롤러 응답으로 내보내지 않는다
- Service에서 외부 SDK(S3, 솔라피, SMTP)를 직접 부르지 않는다

## 이름

- 클래스: 컨트롤러 `Api<리소스>Controller`, 응용 `<리소스>Application`, 업무 `<리소스>Service`, 조회 `<리소스>Reader`, 변경 `<리소스>Writer`
- DTO(record): 요청 `<리소스><동작>Request`, 커맨드 `<동작><리소스>Command`, 조회 조건 `<리소스>Query`, 계층 간 `<리소스>Dto`, 응답 `<리소스>GetResponse`(단건)·`<리소스>ListResponse`(목록 항목)
- 메서드 동사: 조회 `get`(없으면 예외)·`find`(Optional), 목록 `list`, 생성 `create`, 수정 `update`, 삭제 `delete`, 상태 바꾸기는 업무 동사(`suspend`, `close`, `accept`)

## 트랜잭션

- 트랜잭션은 Service 메서드에서 연다. Application은 여러 Service를 조합하되 트랜잭션을 열지 않는다
- 외부 호출(메일, 문자, 저장소)은 트랜잭션 밖에서 한다. 안에서 하면 실패 때 DB만 되돌아가고 발송은 남는다
- 실시간 전달은 커밋 뒤에 보낸다 (`@TransactionalEventListener(phase = AFTER_COMMIT)`)

## 검증과 예외

- 형식 검증(길이, 패턴, 필수)은 요청 DTO의 Bean Validation, 업무 규칙(중복, 상태, 권한 조건)은 Service
- 정책 수치(닉네임 길이, 첨부 용량 등)는 `<도메인>Policy` 상수 한 곳에 두고 프론트 검증과 같은 값을 쓴다
- 업무 오류는 `throw new ServiceException(ErrorCode.X)`. 필드별 사유가 있으면 `errors`를 함께 준다
- 에러 코드 목록의 정본은 `ErrorCode`다

## 설정과 환경변수

- 환경변수는 `application.yml`에서만 읽고, `@ConfigurationProperties` + `@Validated`로 시작할 때 검증한다
- 설정 접두사는 `psw.*`. 설정 클래스는 그 설정을 쓰는 `common/<영역>/`에 둔다
- 새 환경변수는 같은 커밋에서 `backend/.env.example`에 추가한다

## REST

- 기본 경로 `/api/v1`. JSON, UTF-8, 시각은 ISO-8601 UTC
- 경로: kebab-case 복수형 명사. 동사를 넣지 않고 상태를 바꾸는 행동은 하위 자원으로 표현한다 (예: `POST /api/v1/parties/{id}/leader-transfers`)
- 필드: camelCase
- 호환되지 않는 변경은 새 버전 경로로 낸다
- API 명세는 코드에서 만든다: springdoc(Swagger UI, local 프로필에서만). 컨트롤러와 DTO에 `@Operation`, `@Schema`

### 오류 응답

```json
{
  "code": "V001",
  "message": "<사용자에게 보여줄 문장>",
  "timestamp": "<ISO-8601>",
  "path": "<요청 경로>",
  "traceId": "<서버 로그와 같은 값>",
  "errors": [{ "field": "<필드>", "message": "<사유>" }]
}
```

- `errors`는 입력 검증 실패(V001)일 때만 넣는다
- 코드는 접두사 + 세 자리 번호. V 입력 검증, A 인증·인가, R 리소스, B 업무 규칙, S 시스템. 번호는 다시 쓰지 않는다

| HTTP | 코드 | 의미 |
|---|---|---|
| 400 | V001 | 입력 검증 실패 |
| 405 | V002 | 지원하지 않는 요청 방식 |
| 415 | V003 | 지원하지 않는 요청 형식 |
| 401 | A001 | 인증 필요 (토큰 없음·무효, 세션 끊김, 회원이 ACTIVE 아님) |
| 401 | A002 | 액세스 토큰 만료 |
| 401 | A003 | 리프레시 토큰 무효 또는 만료 |
| 401 | A004 | 리프레시 토큰 재사용 감지 (해당 사용자 토큰 전부 폐기) |
| 403 | A005 | 권한 없음 |
| 404 | R001 | 대상 없음 |
| 409 | R002 | 이미 존재함 |
| 409 | B001 | 현재 상태에서 할 수 없는 요청 |
| 500 | S001 | 서버 오류 |

- 성공 응답에 HTTP 200과 오류 코드를 함께 담지 않는다
- 존재를 숨겨야 하는 대상(나를 차단한 회원, 탈퇴·정지 회원과 그 게시물·모집)에는 A005가 아니라 R001로 답한다
- 비공개 계정의 게시물·팔로워·팔로잉 목록은 오류가 아니라 항목 없는 결과와 비공개 표시로 답한다

### 페이징

- 쪽 번호 목록(관리 화면, 검색): `page`(0부터), `size`(기본 20, 최대 100) → `PageResponse` `{content, page, size, totalElements, totalPages}`
- 무한 스크롤 목록(피드, 메시지, 알림, 댓글, 팔로워): `cursor`, `size` → `CursorResponse` `{content, nextCursor}`. `nextCursor`가 null이면 끝
- 페이징 없는 짧은 목록은 배열 그대로

## 인증·인가

- 액세스 토큰: JWT(HS256) 15분, `Authorization: Bearer`. 프론트는 메모리에만 둔다
- 리프레시 토큰: HttpOnly·Secure·`SameSite=Strict` 쿠키(경로 `/api/v1/auth`) 14일. DB에는 해시만, 쓸 때마다 회전, 재사용하면 그 회원 토큰 전부 폐기
- 로그인 세션: 로그인마다 세션을 하나 만들고 액세스 토큰의 `sid` 클레임에 넣는다. 인증 필터가 요청마다 세션과 회원 상태를 확인해 즉시 끊기를 지원한다
  - 로그아웃 → 그 기기, 비밀번호 변경 → 지금 기기 뺀 전부, 비밀번호 재설정·제재·탈퇴 → 전부. 리프레시 토큰과 STOMP 연결도 함께 끊는다
- 역할: `MEMBER`, `ADMIN`. 기본은 모든 API가 인증을 요구하고, 로그인 없이 부를 API는 `@PermitAll`
- 역할 검사는 컨트롤러의 `@PreAuthorize`, 조건부 권한(작성자만, 방장만, 차단 관계 아님 등)은 Service
- 로그인 사용자: `@AuthenticationPrincipal AuthPrincipal principal`
- WebSocket: `/ws` STOMP, `CONNECT` 프레임의 `Authorization` 헤더를 REST와 같게 검사. 구독은 `/user/queue/messages`, `/user/queue/notifications` 등 자기 대상만
- 소셜 로그인(구글·카카오)은 `state`로 CSRF를 막고, 제공자가 인증된 이메일로 알려 준 경우만 같은 이메일 계정에 합친다

## 개인정보와 로그

| 대상 | 보호 방식 |
|---|---|
| 비밀번호 | BCrypt |
| 휴대폰 번호 | AES-GCM 암호화 저장, 중복 검사는 HMAC-SHA256. 키는 `PII_ENCRYPTION_KEY`, `PII_HMAC_KEY` |
| 인증 링크·인증 번호·리프레시 토큰 | SHA-256 해시 |
| 이메일, 생년월일 | 평문. 본인·관리자 응답에만 |
| 첨부 파일 | 비공개 버킷 + 서버가 서명한 짧은 GET URL |

- 다른 회원에게 가는 응답 DTO에는 이메일·생년월일·휴대폰 번호 필드를 두지 않는다. 본인용·관리자용 DTO를 따로 둔다
- 로그에 비밀번호, 토큰, 인증 번호, 휴대폰 번호 원문을 남기지 않는다
- 로그인 성공·실패, 세션 끊기, 리프레시 토큰 재사용, 관리자 조치는 서버 로그(traceId 포함)로 남긴다

## 외부 연동

공통: `common`의 포트 뒤에 두고, 어댑터는 `application.yml` 설정으로 고른다. 키 값은 환경변수로만 받는다.

| 연동 | 어댑터 설정 | 비고 |
|---|---|---|
| 구글·카카오 로그인 | - | 코드 교환 타임아웃 5초, 재시도 없음 |
| 메일 | `psw.mail.provider` = `smtp` / `ses` / `log` | 로컬은 Docker Compose의 Mailpit(`http://localhost:8025`). `log`는 본문을 남기지 않는다 |
| 문자 (솔라피) | `psw.sms.provider` = `solapi` / `log` | 재시도 없음 (중복 발송 방지). 운영에서 `log` 금지 |
| 파일 저장소 | `psw.storage.provider` = `s3` / `local` | Presigned URL 직접 업로드. 운영 S3는 EC2 인스턴스 역할로 접근(접근 키 없음). 운영에서 `local` 금지 |

- 파일 업로드: 서버가 형식·용량 검사 후 PUT URL(10분) 발급 → 브라우저가 직접 업로드 → 저장 때 업로드 키를 보내면 서버가 실제 크기·형식을 확인하고 붙인다. 붙지 않은 업로드는 하루 뒤 정리
- S3 버킷을 공개로 열지 않는다

## DB

- 이름: 테이블 복수형 snake_case, FK 컬럼 `<테이블 단수>_id`, 유니크 `uk_<테이블>_<컬럼>`, 인덱스 `idx_<테이블>_<컬럼>`, FK `fk_<테이블>_<참조 테이블>`
- 타입: ID `bigint` 자동 증가, 시각 `datetime(6)` UTC, 상태 `varchar(30)`, 불리언 `tinyint(1)`, 본문 `text`. 문자셋 `utf8mb4`, 정렬 `utf8mb4_unicode_ci`
- 감사 컬럼 `created_at`, `created_by`, `updated_at`, `updated_by` (`BaseEntity`)
- 삭제는 소프트 삭제가 기본이다: `deleted_at`, `deleted_by` (`SoftDeleteEntity`). 유니크는 가상 컬럼 `active_key`로 활성 행에만 건다
- 하드 삭제는 `@HardDelete(reason)`로 이유를 남긴다 (토큰, 로그인 세션, 인증 링크·번호, 가입 진행, 알림, 좋아요·팔로우·차단 같은 관계 행, 붙지 않은 업로드)
- 행위자 개념이 없는 테이블(로그, 토큰)은 시각 컬럼만 둔다 (`BaseTimeEntity`)
- FK마다 인덱스, 목록 정렬 키(`created_at desc, id desc`)에 복합 인덱스
- 마이그레이션: `backend/src/main/resources/db/migration/V<번호>__<설명>.sql`. 이미 적용된 파일은 고치지 않고 새 파일로 추가한다. 스키마는 `ddl-auto: validate`로 시작할 때 검사한다

## 테스트

- 위치: `backend/src/test/`. API 수준 인수 테스트는 `AcceptanceTest`를 상속한다
- 요청마다 트랜잭션이 따로 돌고(운영과 같다), `DatabaseCleaner`가 테스트마다 모든 테이블을 비운다. 시드 행이 필요하면 테스트 안에서 만든다
- 인증된 요청: `bearer(userId, roles...)`. 로그인 세션 확인이 있으므로 회원 행과 로그인 세션을 먼저 만든다
- 외부 연동: 메일·문자는 테스트 대역으로 보낸 내용을 확인한다. 저장소는 `local` 어댑터
- 표시 이름에 스펙의 수용 기준 번호를 단다 (예: `@DisplayName("member/sign-in AC-1: 이메일로 로그인")`)

## 프론트와 맞춰 바꾸는 것

같은 커밋에서 함께 바꾼다.

- API 경로 → 프론트 호출 경로
- 요청·응답 DTO → `frontend/lib/api/schema.ts` 재생성
- 에러 코드 추가 → `frontend/messages/{ko,en}.json`의 `errors.<코드>`
- 정책 수치(`<도메인>Policy`) → 프론트 zod 스키마
- STOMP 목적지·메시지 형식 → `frontend/lib/realtime/`
