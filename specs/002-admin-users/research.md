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
