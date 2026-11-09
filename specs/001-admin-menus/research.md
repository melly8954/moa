# Research: 관리 메뉴와 접근 (admin-menus)

Technical Context에 미결은 없다. 아래는 코드를 확인해 정한 것이다.

## R1. 만들 것의 범위

- **Decision**: 운영 코드는 바꾸지 않고 AC 테스트만 더한다
- **Rationale**: 시드(`V1_7__menus.sql`, `V1_7_1__admin_menus.sql`)의 순서와 권한이 FR-001 표와 같다. 메뉴 거르기(FR-002)와 접근 차단(FR-003)도 키트에 있다
- **Alternatives considered**: moa 전용 시드를 따로 두기. 키트 시드와 같은 내용이라 두 곳이 어긋날 수 있어 버렸다

## R2. US1-AC1에서 처음 구성을 확인하는 방법

- **Decision**: 테스트 전에 `menus`를 비우고, 실제 마이그레이션 파일의 INSERT 문만 다시 실행한 뒤 내 메뉴를 조회한다
- **Rationale**: `DatabaseCleaner`가 테스트마다 모든 테이블을 비우고 Flyway는 다시 돌지 않아, 앞선 테스트가 있으면 시드가 사라진다. 파일을 그대로 읽으면 시드가 스펙과 어긋날 때 테스트가 실패한다
- **Alternatives considered**:
  - 테스트 안에 시드 값을 다시 적기: 시드가 바뀌어도 통과하므로 처음 구성을 검증하지 못한다
  - `DatabaseCleaner`에서 `menus`를 빼기: 키트 공용 지원 코드를 고쳐야 하고 다른 테스트에 영향을 준다
  - 테스트 순서를 첫 번째로 고정: 다른 클래스와의 순서에 기대어 깨지기 쉽다

## R3. "화면이 보인다"와 "안내"를 어디서 확인하나

- **Decision**: AC 테스트는 API 수준에서 확인한다 (원칙 II 기본). US1-AC2는 다섯 화면이 쓰는 목록 API의 200, US2-AC2는 403 `A005`, US2-AC3는 401 `A001`로 본다
- **Rationale**: 화면 쪽 동작은 키트가 맡는다. `A005`는 목록 자리에 "이 화면을 볼 권한이 없어요."로 보이고(`DataTable`, `QueryState`), 로그인하지 않으면 `AuthGate`가 `/sign-in?next=`로 보낸다 (키트 `e2e/auth.spec.ts`)
- **해석**: US2-AC3의 "로그인하라는 안내"는 로그인 화면으로 보내는 것으로 본다. 별도 안내 문구는 없다. US2-AC2에서 머리글과 버튼은 그대로 보이고 목록 자리에 안내가 나온다
- **Alternatives considered**: AC마다 e2e 추가. 키트 e2e는 백엔드를 가로채므로 실제 권한을 검증하지 못하고, 원칙 II는 e2e를 핵심 흐름에만 둔다

## R4. 테스트 이름과 위치

- **Decision**: `AdminMenusAcceptanceTest` 한 클래스에 `@DisplayName("US1-AC1 ...")` 형식으로 둔다
- **Rationale**: 원칙 II의 형식이다. 다른 스펙도 `US1-AC1`을 쓰므로 클래스 이름으로 스펙을 구분한다
- **Alternatives considered**: 키트 `MenuAcceptanceTest`에 더하기. 키트 테스트는 그대로 두어야 다음 키트 패치를 옮기기 쉽다
