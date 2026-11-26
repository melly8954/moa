# Research: 회원 탈퇴 (member-withdrawal)

spec에 미결 표시가 없다. 아래는 spec이 plan에 맡긴 것을 코드를 확인해 정한 것이다 (2026-11-25, dev 94a5a52).

## R1. 탈퇴 상태를 어디에 두는가 — `withdrawn_at`, `purged_at` (FR-009·FR-012·FR-018)

- **Decision**: `tb_user`에 `withdrawn_at`(탈퇴한 시각)과 `purged_at`(파기한 시각)을 더한다. **유예 기간에는 `deleted_at`을 쓰지 않는다.** 파기할 때 `deleted_at`을 채운다

  | 상태 | `withdrawn_at` | `purged_at` | `deleted_at` |
  |---|---|---|---|
  | 활동 중 | NULL | NULL | NULL |
  | 탈퇴, 유예 중 | 값 | NULL | NULL |
  | 탈퇴, 유예 끝·파기 전 | 값 (30일 지남) | NULL | NULL |
  | 파기됨 (스스로 탈퇴) | 값 | 값 | 값 |
  | 관리자가 지움 | NULL | NULL | 값 |
  | 파기됨 (관리자가 지움) | NULL | 값 | 값 |

- **Rationale**
  - 로그인 아이디 붙잡기가 저절로 된다. `uk_user_login_id_active_key`의 `active_key`는 `deleted_at`이 NULL이면 1이라, 유예 중인 회원의 아이디는 유니크에 남는다. 관리자의 계정 만들기(`UserAdminReader.existsLoginId`)는 `UserRepository.findByLoginId`를 쓰므로, 이 조회가 유예 중인 회원도 보면(R8) **관리자 코드를 고치지 않고** 지금 문구 그대로 R002로 막힌다 (US2-AC12, FR-019)
  - 이메일과 소셜 계정은 `tb_user_social_account` 행이 남아 막힌다(`existsByEmail`, `(provider, subject)` 유니크) (US2-AC13, FR-012)
  - 복구는 `withdrawn_at`을 비우는 것뿐이라 아무것도 되살릴 필요가 없다 (US2-AC4)
  - 파기하면 `deleted_at`이 채워져 `active_key`가 NULL이 되고, 아이디는 고정값으로 덮이고 소셜 행은 지워져 모두 풀린다 (US3-AC10·AC12·AC13)
  - `purged_at`을 따로 두는 이유: 관리자가 지운 회원은 `deleted_at`만 있어, 파기했는지를 다른 칸으로 알 수 없다 (FR-019)
- **Alternatives considered**
  - 탈퇴할 때 `deleted_at`을 채우기: `active_key`가 NULL이 되어 아이디가 풀린다. `active_key` 식, 관리자 조회, 소셜 아이디 만들기, 복구 조회(`@SQLRestriction` 우회)를 모두 고쳐야 한다
  - 별도 테이블 `tb_user_withdrawal`: 요청마다 하는 확인(R2)에 조인이 들고, 상태가 두 곳에 나뉜다
  - 상태 enum 칸: 시각이 따로 필요해(30일, `017` FR-017) 칸이 줄지 않는다
- **결과로 생기는 것**: `deleted_at`만 거르면 유예 중인 회원이 살아 있는 회원으로 보인다. R8이 기본 조회에서 숨긴다

## R2. "다음 요청부터 인증 거부" (FR-006, US1-AC6, SC-002)

- **배경**: 액세스 토큰은 상태 없는 JWT이고 15분(`app.jwt.access-token-ttl`) 유효하다. `JwtAuthenticationFilter`는 서명만 보고, 회원 상태는 재발급 때(`UserPrincipalLoader`)만 읽는다. 리프레시 토큰만 끊으면 최대 15분 동안 요청이 통과한다
- **Decision**: 인증된 요청마다 **PK 조회 한 번**으로 탈퇴를 확인하는 필터를 JWT 필터 뒤에 둔다
  - `common/security/access/AccountAccessChecker`(인터페이스, `isBlocked(userId)`)와 `AccountAccessFilter`. 구현은 `api/service/user/WithdrawnAccountChecker`
  - 키트가 둔 확장점 `HttpSecurityCustomizer` 빈으로 `addFilterAfter(…, JwtAuthenticationFilter.class)`. `SecurityConfig`·`JwtAuthenticationFilter`(키트)는 고치지 않는다
  - 조회는 `select … from tb_user where id = ? and withdrawn_at is not null`(native). `User`의 기본 조건(R8)이 유예 중인 회원을 숨기므로 JPQL로는 찾지 못한다
  - 막을 때: `SecurityContext`를 비우고 오류 코드 속성을 **A002(`ACCESS_TOKEN_EXPIRED`)**로 둔다. 로그인 없이 되는 API는 로그인하지 않은 것으로 진행된다
- **왜 A002인가**: 프론트 `api/client.ts`는 A002일 때만 재발급을 시도하고, 재발급이 A003으로 실패하면 세션을 비워 로그인 화면으로 보낸다. A001을 주면 다른 브라우저는 오류만 보이고 로그인 화면으로 가지 않는다. 응답으로 탈퇴 여부도 드러나지 않는다
- **재발급**: 탈퇴할 때 `AuthTokenService.revokeAll`로 모두 폐기하고, `UserPrincipalLoader`는 R8의 조건 때문에 유예 중인 회원을 찾지 못해 A003이다(코드는 고치지 않는다). 폐기와 겹친 재발급이 새 토큰을 남겨도 다음 재발급에서 막힌다. 복구할 때 한 번 더 `revokeAll`한 뒤 새로 발급한다
- **Rationale**: 여러 서버에서도 바로 맞고, 새 저장소(Redis)가 필요 없다. PK 조회 한 번이라 비용이 작다
- **Alternatives considered**
  - 서버 메모리 차단 목록: 서버가 여럿이면 다른 서버가 모른다. 재시작하면 사라진다
  - 액세스 토큰 수명 줄이기: 줄여도 "다음 요청부터"가 아니다. 재발급이 늘어난다
  - Redis 차단 목록: 이 프로젝트에 Redis가 없다. 이 기능만으로 들이지 않는다
  - 토큰에 버전을 넣고 비교: 어차피 요청마다 조회가 들고, 키트의 토큰 만들기를 고쳐야 한다
- **알려진 한계**: 탈퇴하고 15분 안에 복구하면, 탈퇴 전에 받은 액세스 토큰이 남은 수명 동안 다시 통과한다. 탈퇴 상태에서는 통과하지 않으므로 FR-006은 지킨다. 막으려면 토큰 발급 시각 비교가 필요하고 키트 토큰을 고쳐야 해서 하지 않는다

## R3. 실시간 연결 끊기 (FR-006)

- **Decision**
  - 새 연결: inbound 채널에 moa 인터셉터(`BlockedAccountStompInterceptor`)를 더한다. 키트 `StompAuthInterceptor`가 정한 `CONNECT`의 사용자가 막힌 계정(`AccountAccessChecker`)이면 거절한다(HTTP와 같이 A002). 키트 `WebSocketConfig`·`StompAuthInterceptor`는 고치지 않는다
    - **순서**: moa 인터셉터는 키트 인증 인터셉터 다음에 돌아야 한다(사용자가 정해진 뒤). 설정(`WebSocketMessageBrokerConfigurer`)의 인터셉터는 설정의 `@Order` 순으로 붙는데, 키트 `WebSocketConfig`는 순서가 없어 `LOWEST_PRECEDENCE`로 정렬되므로 어떤 `@Order`로도 그 뒤에 설 수 없다(같은 값이면 빈 등록 순서라 정해지지 않는다). 그래서 moa 인터셉터는 설정으로 붙이지 않고, `SmartInitializingSingleton`(`BlockedAccountInterceptorRegistrar`)이 모든 설정이 채널을 만든 뒤 `clientInboundChannel`의 맨 끝에 덧붙인다. 서버가 연결을 받기 전이다. 순서는 인수 테스트(`WithdrawalRealtimeAcceptanceTest`)가 확인한다
  - 열린 연결: 회원과 세션의 연결은 Spring `SimpUserRegistry`가 안다(`SessionConnectedEvent`·`SessionDisconnectEvent`로 채운다). moa는 `WebSocketHandlerDecoratorFactory`로 세션 ID → `WebSocketSession`만 기억한다(`RealtimeSessions`). 탈퇴가 커밋된 뒤 `closeAll(userId)`가 레지스트리에서 그 회원의 세션 ID를 찾아 닫는다. 레지스트리는 메시지 브로커 설정을 거쳐 만들어지므로 순환을 피해 `ObjectProvider`로 늦게 찾는다
  - 닫기는 Application이 커밋 뒤 직접 부른다. "탈퇴됨" 이벤트에 붙는 처리는 없다 (FR-017)
- **서버가 여럿일 때**: 다른 서버의 세션은 닫지 못한다. 키트 브로커가 서버 메모리라 그 세션에는 개인 메시지도 닿지 않는다(키트 `WebSocketConfig` 주의와 같은 한계). 다시 연결하면 막힌다

## R4. 탈퇴 API (FR-003~FR-005·FR-009)

- **Decision**: `POST /api/v1/users/me/withdrawal` `{nickname}` → 200 `{purgeAt}`, 리프레시 쿠키를 지운다
  - 순서: 관리자 계정이면 A201(`MemberActivityPolicy.requireMypageAllowed`) → 회원 행 잠금(`SELECT … FOR UPDATE`) → 닉네임 확인(V001, 칸 `nickname`) → `withdrawn_at` = 지금 → `revokeAll` → 푸시 구독 모두 삭제(`NotificationService.deleteSubscriptionsOf`, FR-021) → "탈퇴됨" 이벤트 → 커밋 → 실시간 연결 닫기
  - 닉네임 비교: 입력과 `tb_user.name`을 `NicknamePolicy.normalize`(NFC, 앞뒤 공백 제거)로 맞춘 뒤 `equals`. 대소문자를 구별한다
  - 이미 탈퇴했으면(동시에 두 번) 시각을 바꾸지 않고 같은 `purgeAt`으로 200 (spec Edge Case)
- **Rationale**: 되돌릴 수 있는 일이라 `DELETE`로 두지 않는다. 확인 값을 본문으로 받는다

## R5. 로그인했을 때의 안내와 복구 (FR-007·FR-010·FR-011)

- **Decision**: 본인 확인에 성공한 자리에서만 **서명한 복구 쿠키**를 주고, 복구 화면이 그 쿠키로 복구한다

  | 로그인 방법 | 유예 중 | 유예 끝, 파기 전 |
  |---|---|---|
  | 아이디·비밀번호 | 401 A401 + 복구 쿠키 → 화면이 `/sign-in/recover`로 | 401 A402 |
  | 소셜 콜백 | 복구 쿠키 + `/sign-in/recover`로 이동 | `/sign-in?oauthError=purgePending` |

  - 확인 순서: 비밀번호 틀림 A006 → 잠김 A007 → 탈퇴 A401·A402. **탈퇴 확인(A401·A402)은 비밀번호가 맞은 다음에만 한다.** 비밀번호가 틀리면 탈퇴 여부와 관계없이 지금과 같은 A006이고 쿠키도 없다(2026-11-25 사용자 조건, 계정 존재·탈퇴 여부가 새지 않게). 틀린 비밀번호로는 탈퇴를 알 수 없고(US2-AC3), 잠긴 회원은 복구 안내를 보지 못한다(US2-AC8)
  - 복구 쿠키 `withdrawal_recovery`: 회원 ID와 탈퇴 시각을 담아 JWT 키로 서명, 발급자 `…:recovery`, 10분, HttpOnly, `SameSite=Strict`, Path `/api/v1/auth/recovery`. `OAuthCookies`와 같은 방식이다. 탈퇴 시각이 지금 값과 다르면 쓸 수 없다(다시 탈퇴한 뒤 옛 쿠키 막기)
  - `GET /api/v1/auth/recovery` → 200 `{purgeAt}`. 쿠키가 없거나 맞지 않으면 400 A403
  - `POST /api/v1/auth/recovery` → 회원 행 잠금 → 잠김 A007, 유예 지남 A402, 상태 다름 A403 → `withdrawn_at` 비움 → `revokeAll` → "복구됨" 이벤트 → 커밋 → 토큰 발급(로그인과 같은 응답), 쿠키 지움
  - 접속 이력: A401·A402는 실패, 복구는 성공으로 남는다 (spec 가정)
- **A401을 쿠키와 함께 내는 방법**: 서비스가 `ServiceException`의 하위 `WithdrawnAccountException`(회원 ID, 탈퇴 시각)을 던지고, `common/security/recovery`의 `@RestControllerAdvice`(높은 순서)가 쿠키를 쓰고 같은 오류 응답을 만든다. 골격 `ApiAuthTokenController`와 `GlobalExceptionHandler`는 고치지 않는다. `api`의 컨트롤러·Application은 `ServiceException`을 다루지 못하므로(`ArchitectureTest`) `common`에 둔다
- **Alternatives considered**
  - 토큰을 먼저 주고 화면이 복구를 물음: 탈퇴한 회원에게 로그인 토큰이 생긴다 (FR-007 위반)
  - 복구 표를 응답 본문으로: 브라우저 저장소에 남아 훔칠 수 있다. 소셜 콜백(302)은 본문이 없다
  - 서버 테이블에 복구 표 저장: 10분짜리 상태를 위해 테이블이 는다. 쿠키로 충분하다
- **앱 클라이언트**(`X-Client-Type: app`): 쿠키를 쓰지 않으면 복구할 수 없다. 소셜 로그인도 지금 쿠키를 쓴다. 앱이 생기면 함께 정한다

## R6. 30일 뒤 파기 (FR-013·FR-014)

- **Decision**: `api/scheduler/MemberPurgeCleaner`가 매일 `app.withdrawal.cleanup-cron`(기본 `0 20 5 * * *`, 다른 정리 04:30~05:10 뒤)에 **Asia/Seoul 시각으로** 돈다. 저장소에 서버 시간대 설정이 없어 `@Scheduled`는 JVM 기본 시간대를 쓰므로 `zone`(`app.withdrawal.cleanup-zone`, 기본 `Asia/Seoul`)을 명시한다. 다른 정리기는 zone이 없어 서버 시간대를 따른다(고치지 않는다). 30일 비교는 저장된 시각과 같은 서버 시간대의 `LocalDateTime`이라 cron의 시간대와 상관없다
  1. 대상 ID를 native로 읽는다(`User`의 기본 조건을 타지 않게). 둘을 합친다
     - 스스로 탈퇴: `purged_at is null and withdrawn_at <= 지금 − 30일`
     - 관리자가 지움: `purged_at is null and withdrawn_at is null and deleted_at <= 지금 − 30일 and role = 'USER'` (시스템 계정 제외, FR-019). 이 기능 전에 지운 회원도 `purged_at`이 비어 있어 처음 돌 때 들어간다 (US3-AC15)
  2. 회원마다 **트랜잭션 하나**(`TransactionTemplate`): 행 잠금(native `FOR UPDATE`) → 조건 다시 확인(복구됐거나 이미 파기됐으면 건너뜀) → 딸린 행 삭제 → 회원 행의 개인정보 열 덮기 → `PersonalDataPurgedEvent` → "파기됨" 이벤트 → 커밋
  3. 한 회원이 실패하면 로그를 남기고 다음 회원으로 간다. 다음 날 다시 찾는다 (US3-AC7). 파기한 수를 로그에 남긴다
- **지우는 행**: `tb_user_profile_play_time`, `tb_user_profile`, `tb_user_game_profile`, `tb_user_social_account`, `tb_user_terms_agreement`, `tb_notification`, `tb_push_subscription`, `tb_notice_read`, `tb_refresh_token`
- **덮는 열**: `login_id` = `withdrawn_<id>`, `password` = NULL, `name` = `탈퇴한 회원`, `deleted_at`·`deleted_by`(비었을 때만 지금·시스템 계정), `purged_at` = 지금, `updated_at`·`updated_by`(시스템 계정). `role`·`locked_at`·`password_change_required`·`withdrawn_at`은 개인정보가 아니라 둔다
- **계층**: 트랜잭션은 `MemberPurgeApplication`. Service는 자기 도메인만 다루므로(`ArchitectureTest`) 알림·공지 확인은 그 도메인 Service에 `deleteAllOf(userId)`를 더해 Application이 조합한다. 리프레시 토큰은 `common/security/token/RefreshTokenEraser`(새 moa 클래스)가 지운다. 키트 `RefreshTokenRepository`·`AuthTokenService`는 고치지 않는다
- **파기 이력**: `new PersonalDataPurgedEvent(PurgeTarget.MEMBER, PurgeMethod.COLUMN_ERASE, 1, null, userId)`. 동기 `@EventListener`(`PurgeLogRecorder`)라 저장이 실패하면 그 회원의 파기가 되돌아간다 (US3-AC6, 013 FR-004). `PurgeTarget`에 `MEMBER`("회원")를 더하고 파기 이력 화면의 종류와 문구를 더한다
- **여러 서버**: 행 잠금 뒤 `purged_at`을 다시 보므로 회원마다 한 번만 파기되고 이력도 하나다
- **복구와 겹침**: 복구도 같은 행을 잠근다. 먼저 잠근 쪽만 된다 (FR-013)
- **5일**: 매일 돌므로 실패해도 다섯 번 다시 한다. 유예가 끝난 뒤 4일이 지나도 남은 회원이 있으면 ERROR 로그를 남긴다
- **Alternatives considered**: 한 트랜잭션으로 모두 — 한 명이 실패하면 모두 되돌아간다. DB 이벤트 스케줄러 — 파기 이력과 이벤트를 낼 수 없다. ShedLock — 행 잠금으로 충분하고 의존성이 는다

## R7. 이벤트 (FR-017)

- **Decision**: `common/event`가 아니라 회원 도메인의 record `MemberWithdrawnEvent`, `MemberRestoredEvent`, `MemberPurgedEvent`(회원 ID, 일어난 때). 트랜잭션 안에서 낸다. 받는 쪽은 `@TransactionalEventListener(AFTER_COMMIT)`로 붙인다 (007·006과 같은 방식). 이번에 받는 쪽은 만들지 않는다
- **테스트**: US4-AC3~AC6은 시험용 리스너로 확인한다. 실패하는 리스너를 붙여도 탈퇴·복구·파기가 그대로인지 본다

## R8. 유예 중인 회원을 기본 조회에서 숨기기 (FR-008, US1-AC7·AC13)

- **배경**: dev 코드에서 회원을 거르는 기준은 `User`의 `@SQLRestriction("deleted_at IS NULL")` 하나이고, native 쿼리로 회원을 읽는 곳은 없다. 다른 회원을 보여 주는 화면(파티·친구·숏폼·문의)은 아직 구현되지 않았다
- **Decision** (2026-11-25 사용자 결정): `User`의 조건을 `deleted_at IS NULL AND withdrawn_at IS NULL`로 한다. 로그인·복구·파기만 예외로 읽는다
  - `UserRepository.findByLoginId`: native(`deleted_at is null`만 본다). 유예 중인 회원도 나온다. 쓰는 곳은 로그인 확인, 첫 관리자, 소셜 아이디 만들기, 관리자의 `existsLoginId`뿐이고 모두 "아이디가 쓰이는 중인가"를 묻는다
  - `UserRepository.findIncludingWithdrawn(id)`, `findForUpdateIncludingWithdrawn(id)`: native. 소셜 로그인의 연결된 회원, 탈퇴·복구의 행 잠금
  - 탈퇴 확인(R2)과 파기(R6)도 native다
- **저절로 되는 것**: 토큰 재발급 A003, 내 정보·마이페이지 R001(그 전에 인증에서 막힘), 관리자 회원 목록·상세·권한 관리에서 빠짐. 이 코드들은 고치지 않는다
- **이름**: 조회에 없는 회원은 `MemberDisplay`가 "탈퇴한 회원"으로 바꾼다. 파기된 회원도 조회에 없으므로 모든 기능이 어차피 해야 하는 일이다. 회원 상태는 `MemberStatusService.find(userId)`로 준다(활동, 탈퇴, 지움, 파기와 그 시각. FR-018)
- **이 조건으로 막히지 않는 넷** (담당자에게 알린다. [plan.md](./plan.md) "담당자 알림 목록"): 프로필 표(`tb_user_profile`)를 `User` 없이 읽기, 숏폼 자체, 개인 알림, 문의 답변
  - `tb_user_profile`은 숨길 수 없다. 닉네임 태그를 파기 전까지 붙잡아야 한다
- **확인한 것 (T002, 2026-11-25, Hibernate 7.4.13, `UserRestrictionAcceptanceTest` 7개 통과)**: 지금 있는 `deleted_at` 조건으로 시험했다. 조건은 `findById`, 메서드 이름 쿼리, QueryDSL `selectFrom`, 왼쪽 조인, 안쪽 조인, 서브쿼리에 모두 붙는다. 왼쪽 조인은 행이 남고 회원 값이 NULL이며, 안쪽 조인은 행이 빠진다. native 쿼리로 읽은 엔터티는 조건을 타지 않고, 고친 값이 저장된다
  - 그래서 다른 기능은 왼쪽 조인이나 따로 읽기로 하고 NULL이면 "탈퇴한 회원"을 쓴다. 안쪽 조인이면 행이 빠진다 (plan "담당자 알림 목록")
- **Alternatives considered**
  - 각 기능이 거르게 알리기만: 빠뜨리면 이름이 새고 `docs/privacy.md` 원칙을 어긴다. 남의 기능 여러 곳을 고쳐야 한다
  - 표시 정책 클래스만: 부르지 않으면 샌다

## R9. 로그 (FR-020)

- **Decision**: 탈퇴·복구·파기 로그에는 회원 내부 ID, 건수, 오류 코드, 예외 **클래스 이름**만 적는다. 예외 메시지와 스택은 적지 않는다. DB 예외 메시지는 값을 담을 수 있다(`Duplicate entry 'a@b.com' …`)
- **확인**: 파기는 삭제와 고정값 덮기뿐이라 유니크 충돌이 없다. 테스트는 로그를 모아 이메일·비밀번호·토큰·소셜 식별자가 없는지 본다 (US3-AC14)

## R10. 화면 (FR-001~FR-003·FR-010·FR-016)

- **Decision**
  - 탭은 주소로 나눈다: `/account/profile`(프로필), `/account/profile/manage`(계정 관리). 탭은 링크다. 저장하지 않은 프로필이 있으면 `useUnsavedChanges`가 링크를 잡아 묻는다(Edge Case). 새로 만들 것이 없다
  - 머리·탭·권한 확인은 `components/profile/mypage-frame.tsx`에 모아 두 화면이 쓴다. 지금 탭은 prop으로 받는다
  - 계정 관리 항목: `components/account-management/items.ts`의 배열(키, 순서, 컴포넌트). 지금은 비어 있다. "탈퇴하기"는 배열 밖에서 늘 맨 아래 오른쪽에 그린다 (US4-AC1·AC2)
  - 확인 창은 입력이 있어 `confirm()`이 아니라 `AlertDialog`로 만든다. 닫으면 입력을 비운다
  - 탈퇴 뒤: 서버는 탈퇴 때 리프레시 쿠키와 그 회원의 푸시 구독을 모두 지운다(FR-021). 화면은 브라우저 쪽 구독만 해제하고(`unsubscribeBrowserOnly`) `/sign-in?withdrawnUntil=<시각>`을 새로 연다(`window.location.replace`). 새로 열면 메모리의 액세스 토큰과 캐시도 사라진다. 탈퇴 뒤에는 서버 API를 부르지 않는다: 부르면 A002 → 재발급 A003으로 `AuthGate`가 "로그인이 만료됐어요" 안내를 띄우고 `/sign-in`으로 덮어쓴다. 처음 적은 `disablePush` → 탈퇴 → `signOut` 순서는 탈퇴가 실패해도 알림이 꺼지고, `signOut`이 `AuthGate`를 거쳐 쿼리 없는 `/sign-in`으로 보내 안내가 사라져서 바꿨다
  - 복구 화면 `/sign-in/recover`: 완전 삭제 예정 시각, "복구하기", "취소"
- **[공용] 파일**: `sign-in/page.tsx`, `next-path.ts`, `ko.json`, `api/index.ts`, `oauth-error-alert.tsx`, 파기 이력 화면. 바꿀 내용을 먼저 보고하고 승인받는다

## R11. 오류 코드

- `A401 ACCOUNT_WITHDRAWN`(401), `A402 ACCOUNT_PURGE_PENDING`(401), `A403 RECOVERY_NOT_AVAILABLE`(400). 기능마다 백 단위를 쓰는 방식(008 `1xx`, 009 `2xx`, 017 `3xx`)에 이어 `4xx`다. HTTP 상태 번호와 관계없다. 구현 때 번호가 비었는지 다시 본다

## R12. 테스트 (원칙 II)

- AC 48개: 인수 테스트 `MemberWithdrawalAcceptanceTest`(탈퇴·복구), `MemberPurgeAcceptanceTest`(파기), 프론트 단위, e2e는 핵심 흐름 둘(탈퇴, 복구). 대응은 [quickstart.md](./quickstart.md)
- 시간은 `withdrawn_at`·`deleted_at`을 DB에서 앞당겨 만든다
- `UserRestrictionAcceptanceTest`: R8의 조회 종류마다 조건이 붙는지 (구현 첫 작업)
- `User` 조건을 바꾼 뒤와 마지막에 백엔드 테스트 전체(`build`, `dbTest`)를 로컬에서 돌린다 (사용자 지시)
- 공통 시드는 `CodeSeed`(009)를 쓴다
- [공용] 프론트 파일을 바꾸므로 push 전에 e2e 전체를 돌린다

## R13. 유예 중인 회원에게 알림을 만들지 않기 (FR-021·FR-022)

- **Decision**: `NotificationApplication.notify` 맨 앞에서 `MemberStatusService.isWithdrawn(userId)`면 저장하지 않고 null을 돌려준다(한 줄). 이벤트가 나지 않아 실시간과 푸시도 가지 않는다. 007 코드(haily)이고 2026-11-25 사용자 승인이다. 이유는 `docs/privacy.md` 원칙 "그 목적 말고는 쓰지 않는다"
- 탈퇴할 때 그 회원의 `tb_push_subscription` 행을 모두 지운다. 파기할 때 남은 것을 다시 지운다
- **Rationale**: 알림을 보내는 길이 이 메서드 하나다. 보내는 기능마다 거르게 하면 빠뜨린다
- **전체 공지**(`/topic/notices`)는 저장하지 않고 열린 연결에만 간다. 유예 중인 회원의 연결은 닫힌다(R3)
