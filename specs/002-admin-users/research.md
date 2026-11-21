# Research: 관리자·운영자 계정 (admin-users)

Technical Context에 미결은 없다. 아래는 코드를 확인해 정한 것이다.

## R1. 만들 것의 범위

- **Decision**: 운영 코드는 바꾸지 않고 AC 테스트만 더한다
- **Rationale**: 부트스트랩(FR-001), 관리자가 만드는 계정(FR-002), 목록 항목(FR-004)이 키트에 있다. 운영자가 스스로 가입하는 API도 없다 (`/api/v1/auth`는 로그인, 재발급, 로그아웃뿐이다)
- **Alternatives considered**: moa 전용 계정 만들기 API. 키트와 같은 동작이라 만들 이유가 없다

## R2. 운영자를 어떻게 나타내나

- **Decision**: 운영자는 시스템 역할 `USER`로 만들고, 업무 역할은 `003-admin-roles`에서 준다
- **Rationale**: 키트의 시스템 역할은 `USER`와 `ADMIN`뿐이고, 업무 역할을 enum에 더하지 말라고 정한다 (`Role`)
- **영향**: 운영자와 일반 회원은 시스템 역할로 구분되지 않는다. FR-003을 넘겨받는 회원 기능이 운영자를 가려낼 방법을 정해야 한다 (`specs/README.md` 맞춰야 할 경계)
- **Alternatives considered**: `OPERATOR` 역할 추가. 키트 규칙에 어긋나고 키트 코드를 고쳐야 한다

## R3. US1-AC1에서 첫 관리자를 만드는 방법

- **Decision**: 테스트 시작에 DB를 비우고, `AdminBootstrapRunner`가 부르는 `UserApplication.bootstrapAdmin`을 시스템 행위자로 직접 부른다
- **Rationale**: 환경 변수를 준 채 애플리케이션을 띄우는 것은 테스트 컨텍스트를 따로 만들어야 해 무겁다. 설정 이름과 시작 실패 조건은 키트 `AdminBootstrapAcceptanceTest`가 맡는다
- **Alternatives considered**: `@SpringBootTest(properties=...)`로 별도 컨텍스트 띄우기. 러너를 거치는 것 말고는 얻는 것이 없고, DB가 비었는지 맞추기도 어렵다

## R4. "비밀번호를 바꾸라는 안내"와 "한 번 보인다"를 어디서 확인하나

- **Decision**: AC 테스트는 API 수준에서 확인한다. 안내는 `GET /api/v1/users/me`의 `passwordChangeRequired`가 `true`인 것으로, "한 번"은 만들기 응답에만 `temporaryPassword`가 있고 조회 응답에는 없는 것으로 본다
- **Rationale**: 화면은 키트가 맡는다. `AuthGate`가 비밀번호 변경 화면으로 보내고 안내를 보이며(`e2e/auth.spec.ts`), 임시 비밀번호 창은 `e2e/user-admin.spec.ts`가 확인한다
- **알려진 동작**: 변경 요구는 화면만 막는다. 백엔드는 비밀번호를 바꾸기 전에도 임시 비밀번호로 받은 토큰의 API 호출을 막지 않는다. 키트 동작이고 AC도 안내와 변경 후 진입만 요구하므로 바꾸지 않는다. 막아야 한다면 키트를 고치고 이 스펙에 요구사항을 더한다
- **Alternatives considered**: AC마다 e2e 추가. 키트 e2e는 백엔드를 가로채므로 실제 흐름을 검증하지 못한다

## R5. 테스트 이름과 위치

- **Decision**: `AdminUsersAcceptanceTest` 한 클래스에 `@DisplayName("US1-AC1 ...")` 형식으로 둔다
- **Rationale**: 원칙 II의 형식이다. 스펙은 클래스 이름으로 구분한다 (`001-admin-menus`와 같다)

## R6. 업무 역할 열 (FR-006, 2026-11-15 관리 화면 정리)

- **Decision**: 사용자 관리 목록 응답(`UserAdminResponse`)에 `accessRoles`(`id`, `name`)를 더한다. `UserAdminApplication.search`가 그 쪽의 사용자 ID로 `003` `RoleUserService`의 업무 역할 조회(003 research R14)를 한 번 불러 붙인다 (쿼리 하나 더). 화면은 `ADMIN`이면 "모든 권한", 없으면 빈 칸
- **Rationale**: 목록 한 쪽(최대 100명)마다 사용자별로 부르면 N+1이 된다
- **키트로 옮길 것**: user-admin 헬퍼가 rbac 없이도 쓰이므로, 키트에서는 rbac가 업무 역할 조회를 주는 확장점(없으면 열을 숨김)으로 둔다

## R7. 사용자 드로어 (FR-007)

- **Decision**: 행을 누르면 `001` R21의 공용 드로어가 열린다. 위에 아이디·이름·시스템 역할·상태·업무 역할(이름만, "역할 관리에서 바꿔요" 링크), 아래에 지금 "관리" 메뉴의 일을 단추로 둔다(이름 바꾸기, 시스템 역할 바꾸기, 잠그기·풀기, 비밀번호 초기화, 삭제). 막는 규칙(내 행, 관리자가 아니면 관리자 행, 비밀번호 없는 계정)은 단추를 끄는 지금 규칙 그대로. 확인이 필요한 일(잠그기, 초기화, 삭제)은 지금처럼 확인 창
  - "사용자 만들기"도 같은 드로어에서 하고, 저장 직후 임시 비밀번호를 드로어 안에 한 번 보인다(복사 단추). 드로어를 닫으면 다시 볼 수 없다
  - 행의 "관리" 메뉴 단추와 user-extras(다른 헬퍼가 끼우는 메뉴 자리)는 드로어 단추 자리로 옮긴다
- **Rationale**: 관리 화면을 같은 틀(목록 + 드로어)로 맞춘다. 사용자 관리는 업무 목록이라 쪽 나눔은 그대로다

## R8. 무엇을 테스트하나 (US3)

- **Decision**: API 인수 테스트(`AdminUsersAcceptanceTest`): US3-AC1(목록 응답의 `accessRoles`: 역할 받은 운영자는 "보안 담당", 없으면 빈 목록). e2e(`user-admin.spec.ts`): US3-AC1(열, 관리자 "모든 권한"), US3-AC2(행 → 드로어, 행 "관리" 단추 없음, 막는 규칙), US3-AC3(업무 역할 보기만, 역할 관리 링크), 만들기 뒤 임시 비밀번호가 드로어에 한 번. 지금 "관리" 메뉴를 쓰던 e2e는 드로어로 고친다 (원칙 V)

## R9. 계정을 다루는 규칙 (FR-005·FR-007, US2·US4, 2026-11-21)

- **Decision**: `UserAdminService`(키트 user-admin)의 규칙을 바꾼다. 모두 사용자 관리 권한(`USER_MANAGE`) API 안의 규칙이다
  - 만들기(`create`): 계정 종류 `ADMIN`도 사용자 관리 권한으로 만든다. 지금의 "ADMIN은 ADMIN만 만든다"(:67)를 없앤다. 역할 없는 `ADMIN`은 권한이 없기 때문이다
  - 계정 종류 바꾸기(`changeRole`): "ADMIN만"(:94)을 없애고 사용자 관리 권한으로 한다. `ADMIN` → `USER`면 같은 트랜잭션에서 그 계정의 역할을 모두 빼고(`003` `RoleUserService`), 그 계정의 토큰을 끊는다(잠그기와 같다). 내 행은 지금처럼 B001
  - 다룰 수 있나(`requireManageable`, 이름 바꾸기·계정 종류·잠그기·풀기·비밀번호 초기화·삭제 모두): 대상이 최고 관리자면 행위자도 최고 관리자여야 한다. 대상이 `ADMIN`이고 행위자가 최고 관리자가 아니면 대상의 권한(역할 합집합)이 모두 행위자의 권한 안에 있어야 한다. 어기면 403 `A005`. 대상이 `USER`면 지금처럼 다룬다. 지금의 "행위자가 ADMIN이 아니면 ADMIN 행은 못 다룬다"(:154-160)를 이것으로 바꾼다
  - 마지막 최고 관리자: 회원으로 바꾸기·잠그기·지우기가 최고 관리자를 0명으로 만들면 409 `B001` (`003` research R17의 같은 검사를 부른다)
- **Rationale**: 사용자 관리 권한이 있는 운영자가 자기보다 권한이 많은 운영자의 비밀번호를 초기화하면 그 계정으로 로그인해 권한을 넓힐 수 있다. 키트가 역할에 쓰는 권한 상승 방지(내 권한 안쪽만)를 계정에도 같은 식으로 둔다(스펙 가정, 사용자 결정)
- **Alternatives considered**: 관리자 계정 행은 최고 관리자만 다루기(단순하지만 운영자 계정 관리를 모두 최고 관리자가 해야 한다. 사용자가 버렸다), 비밀번호 초기화만 막기(이름 바꾸기·잠그기는 권한을 넓히지 않지만, 규칙이 둘로 나뉘면 화면 안내가 복잡해진다)
- **키트로 옮길 것**: user-admin 헬퍼의 대상 범위 규칙과 확장점(rbac가 있을 때 권한을 비교)

## R10. 응답에 더하는 칸 (FR-006·FR-007, US3·US4, 2026-11-21)

- **Decision**: `UserAdminResponse`(목록·한 사람)에 `superAdmin`(최고 관리자 역할을 가졌나)과 `protection`(이 행위자가 다룰 수 없는 이유: `SUPER_ADMIN_ONLY`·`WIDER_PERMISSIONS`·null)을 더한다. 목록 한 쪽의 대상 권한은 `accessRoles`처럼 한 번에 모아 계산한다(쿼리 하나 더). 화면의 `protectedRow`(`page.tsx:66`, "내가 ADMIN이 아니면 ADMIN 행")와 `iAmAdmin`(`page.tsx:42`)은 이 칸으로 바꾼다
- **Rationale**: 대상과 행위자의 권한 비교는 서버만 정확히 안다. 화면은 이유만 받아 단추를 끄고 안내한다 (`003` research R18과 같은 생각)
- **Alternatives considered**: 화면이 대상 권한을 받아 비교하기(최고 관리자 규칙까지 화면에 두 벌 적는다)

## R11. 사용자 드로어와 목록 (FR-005~FR-008, US3·US4, 2026-11-21)

- **Decision**:
  - 문구(`messages/ko.json` `userAdmin`): `role` "시스템 역할" → "계정 종류", `roles.USER` "사용자" → "회원", `roles.ADMIN` "관리자" 그대로, `accessRoles` "업무 역할" → "역할", `noAccessRoles` → "역할 없음", 최고 관리자 표시 "최고 관리자", `description`은 FR-008 문구. `allPermissions`·`adminOnly`·`accessRolesHint`(역할 관리 링크)는 쓰지 않게 되면 지운다
  - 목록 역할 열: `superAdmin`이면 "최고 관리자", `ADMIN`이고 역할 없으면 "역할 없음", 있으면 이름들, `USER`면 빈 칸
  - 드로어 위 칸: 아이디·이름·계정 종류·상태·만든 시각. 그 아래 역할 칸(관리자 계정만):
    - 권한 관리 권한(`ROLE_MANAGE`, `useHasPermission`)이 있으면 체크 목록. 역할은 `GET /api/v1/admin/roles`(`003` R18의 `grantable`·`blockReason`·`permissionNames`), 이 계정의 역할은 `GET /api/v1/admin/users/{userId}/roles`, 저장은 `PUT` 같은 주소(지금 API, `003` 권한). 체크할 수 없는 역할은 꺼진 채 이유를 작게 보인다. 역할 이름 아래 `permissionNames`를 작게 보인다. 최고 관리자 역할 칸은 최고 관리자만 켜진다. "역할 저장" 단추는 이 칸 안에 두고 바꾼 것이 있을 때만 켠다
    - 권한 관리 권한이 없으면 `accessRoles` 이름만 보이고 체크 칸을 끈다
    - 회원이면 역할 칸 대신 "계정 종류를 관리자로 바꾸면 역할을 줄 수 있어요"
  - 아래 단추: `protection`이 있으면 모두 끄고 이유를 보인다. 내 행·비밀번호 없는 계정 규칙은 그대로
  - 계정 종류 바꾸기 대화상자: `ADMIN` → `USER`면 빠질 역할 이름을 보이고 확인을 받는다
  - 만들기 드로어의 계정 종류는 회원·관리자 둘 다 고를 수 있다(`canGrantAdmin` 없앰)
- **Rationale**: 미리보기로 고른 모양(체크 칸 드로어)이다. 역할을 주는 API는 `003` 것을 그대로 써서 규칙이 한 곳에 있다
- **Alternatives considered**: 사용자 관리 API에 역할 저장을 더하기(같은 규칙의 API가 둘이 된다)
- **키트로 옮길 것**: 드로어 역할 칸(rbac가 있을 때만 보이는 확장점)

## R12. 무엇을 테스트하나 (US1~US4, 2026-11-21)

- **Decision**:
  - API 인수 테스트(`AdminUsersAcceptanceTest`): US1-AC1(부트스트랩 계정이 최고 관리자, 모든 관리 메뉴), US2-AC1(`role` = `ADMIN`으로 만들기, 역할 전에는 `/menus/me` 빈 목록), US3-AC1(`accessRoles`·`superAdmin`), US3-AC3(드로어가 쓰는 `PUT /users/{id}/roles` → 공지 관리 200), US4-AC1(사용자 관리 운영자가 회원 다루기·`ADMIN`으로 바꾸기), US4-AC2(권한이 더 많은 운영자 비밀번호 초기화 A005, `protection` = `WIDER_PERMISSIONS`), US4-AC3(최고 관리자 행 A005, `SUPER_ADMIN_ONLY`), US4-AC5(`ADMIN` → `USER`면 역할이 비고 토큰이 끊김), 마지막 최고 관리자 잠그기·지우기 B001. 지금 운영자를 `role` = `USER`로 만들던 테스트는 `ADMIN`으로 바꾼다(원칙 V)
  - 키트 수용 테스트 `UserAdminAcceptanceTest`: `adminProtection`(ADMIN이 아니면 ADMIN 행 A005)을 R9 규칙으로 바꾼다. `search`의 `role=ADMIN` 거르기는 그대로다. 바꾼 것을 PR 설명에 적는다
  - e2e(`user-admin.spec.ts`): US3-AC1(역할 열 세 가지), US3-AC2·AC3(체크 칸, 저장), US3-AC4(회원 드로어 안내), US4-AC2·AC3(`protection`이면 단추 꺼짐과 이유), US4-AC4(권한 관리 권한 없으면 체크 꺼짐), US4-AC5(되돌리기 확인 창에 역할 이름), "계정 종류"·"회원" 문구. "내가 관리자가 아니면 관리자 행은 모두 꺼져 있다"(:107)는 `protection` 기대로 바꾼다(원칙 V)

## R13. 최고 관리자 계정 행 (FR-007, US4-AC3, 2026-11-21 한 명 고정)

- **Decision**:
  - `UserAdminApplication`: 대상이 최고 관리자면 계정 종류 바꾸기·잠그기·풀기·비밀번호 초기화·삭제를 누구든 409 `B001`("최고 관리자 계정은 바꿀 수 없어요. 개발자에게 요청해요."). 이름 바꾸기는 최고 관리자 본인만 된다(다른 사람은 지금처럼 `protection` = `SUPER_ADMIN_ONLY`로 403 `A005`). 막는 것은 `UserAdminService.requireNotSuperAdmin(boolean)`이 던진다(ServiceException은 Service에서만, ArchUnit)
  - 지금의 `requireSuperAdminRemains` 호출(회원으로 바꾸기·잠그기·지우기)은 위 규칙이 먼저 막으므로 없앤다
  - 화면(`user-drawer.tsx`): `user.superAdmin`이면 이름 바꾸기 말고 모두 끈다. 본인이면 이름 바꾸기만 켜고 "최고 관리자 계정은 바꿀 수 없어요. 비밀번호는 내 비밀번호 바꾸기에서 바꿔요." 안내, 남이면 지금처럼 모두 끄고 이유
  - 사용자 드로어 역할 칸(`user-roles.tsx`): 최고 관리자 역할 체크 칸은 늘 끄고 이유("최고 관리자는 한 명으로 고정이에요"), 최고 관리자 계정의 드로어는 체크 목록 대신 "최고 관리자 (모든 권한)"만 보인다
- **Rationale**: 스펙 FR-007. 최고 관리자 계정을 바꾸는 길은 개발자 DB 절차뿐이다

## R14. 개발자 DB 절차 (FR-009, US1-AC2·AC3, Clarifications 2026-11-22)

- **Decision**: SQL 파일 두 개를 `backend/src/main/resources/db/ops/`에 둔다. Flyway 위치(`db/migration`, local `db/localdata`)가 아니라 서버가 실행하지 않는다. quickstart "운영: 최고 관리자 되돌리기"가 이 파일과 순서를 안내한다
  - `super-admin-reset-password.sql`: 맨 위 `SET @login := '<아이디>'; SET @hash := '{bcrypt}<bcrypt 값>';` 두 줄만 고쳐 실행. 한 트랜잭션에서 그 계정이 최고 관리자인지 확인(아니면 `SIGNAL`로 중단) → `password = @hash`, `password_change_required = TRUE`, `locked_at = NULL` → `tb_refresh_token.revoked_at = NOW(6)`(그 계정의 살아 있는 토큰) → 끝에 최고 관리자 수 확인 쿼리
  - `super-admin-transfer.sql`: 맨 위 `SET @to := '<새 최고 관리자 아이디>'`만 고쳐 실행. 그 계정이 지운 적 없는 `ADMIN`인지 확인(아니면 SIGNAL) → 그 계정의 다른 역할 줄 지우기 → 예전 최고 관리자 줄 **지우기 먼저** → 새 줄 넣기(`super_slot` 유일 제약 때문에 순서가 중요하다, 003 R22) → 예전 최고 관리자의 토큰 폐기 → 끝에 최고 관리자가 정확히 한 명인지 확인. 새 최고 관리자의 비밀번호는 바꾸지 않는다(본인 비밀번호로 로그인한다)
  - bcrypt 값 만들기: `htpasswd -bnBC 10 "" '<임시 비밀번호>' | tr -d ':\n'`(Apache 도구, `$2y$`도 Spring `BCryptPasswordEncoder`가 읽는다) 또는 `python -c "import bcrypt; print(bcrypt.hashpw(b'<pw>', bcrypt.gensalt(10)).decode())"`. 저장 값 앞에 `{bcrypt}`를 붙인다(`DelegatingPasswordEncoder`)
  - 감사 열(`updated_at`, `updated_by`)은 `NOW(6)`, 시스템 계정(1)으로 둔다
- **Rationale**: 스펙 Clarifications(서버 설정값·코드 없이 DB 절차). 절차를 파일로 두면 문서와 테스트가 같은 SQL을 쓴다
- **Alternatives considered**: quickstart에 SQL을 글로만 적기(테스트가 같은 SQL을 돌릴 수 없어 문서가 낡아도 모른다)

## R15. 무엇을 테스트하나 (US1-AC2·AC3, US4-AC3, FR-007, 2026-11-22)

- **Decision**:
  - `AdminUsersAcceptanceTest`: US1-AC2·AC3는 R14의 SQL 파일을 읽어 맨 위 값 줄만 바꿔 `ScriptUtils`로 실행한 뒤 로그인·`/menus/me`·리프레시로 확인한다. US4-AC3는 운영자와 최고 관리자 본인이 최고 관리자 행의 계정 종류·잠그기·초기화·삭제를 하면 A005·B001, 본인 이름 바꾸기는 200. 지금의 `lastSuperAdminIsProtected`는 이 테스트로 바꾼다(원칙 V)
  - SQL 파일이 아닌 계정(회원, 없는 아이디)으로 절차를 돌리면 SIGNAL로 멈추고 아무것도 바뀌지 않는다
  - e2e(`user-admin.spec.ts`): 최고 관리자 본인 행은 이름 바꾸기만 켜짐과 안내, 최고 관리자 역할 체크 칸은 늘 꺼짐
