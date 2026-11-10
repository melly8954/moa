# Research: 운영자 역할 (admin-roles)

Technical Context에 미결은 없다. 아래는 코드를 확인해 정한 것이다.

## R1. 만들 것의 범위

- **Decision**: 운영 코드는 바꾸지 않고 AC 테스트만 더한다
- **Rationale**: 역할에 권한 묶기(FR-001), 권한 합집합 판정(FR-002)이 키트에 있다. 마이그레이션에 역할 시드가 없어 처음에는 역할이 하나도 없다(FR-003)
- **Alternatives considered**: moa 기본 역할 시드. FR-003이 두지 않기로 정했다

## R2. 역할을 만들고 주는 방법

- **Decision**: 관리자가 API로 역할을 만들고(`POST /api/v1/admin/roles`) 운영자에게 준다(`PUT /api/v1/admin/users/{userId}/roles`). DB에 직접 넣지 않는다
- **Rationale**: 스토리의 흐름 그대로다. 역할 주기는 실제 사용자가 있어야 하므로(없으면 R001) 운영자 계정은 `TestUsers`로 만든다
- **Alternatives considered**: `user_access_roles`에 SQL로 넣기. 빠르지만 관리자가 역할을 주는 흐름을 건너뛴다

## R3. US1-AC1의 메뉴 처음 구성

- **Decision**: 001 테스트의 시드 복원 코드를 `com.moa.support.MenuSeed`로 옮겨 두 테스트가 함께 쓴다
- **Rationale**: `DatabaseCleaner`가 시드를 지우므로 US1-AC1도 처음 구성이 필요하다 (`001-admin-menus` research.md R2). 같은 코드를 두 곳에 두면 한쪽만 고칠 수 있다
- **원칙 V**: 001 테스트는 복원 코드의 위치만 바뀌고 확인 내용은 그대로다. 옮긴 뒤에도 001 AC 5개가 통과해야 한다
- **Alternatives considered**: 003 테스트에 복원 코드를 복사. 두 벌이 되어 버렸다

## R4. "권한이 없다는 안내"를 어디서 확인하나

- **Decision**: API 수준에서 403 `A005`로 확인한다
- **Rationale**: 화면 안내는 `001-admin-menus`와 같은 키트 동작이다 (그 research.md R3)

## R5. 테스트 이름과 위치

- **Decision**: `AdminRolesAcceptanceTest` 한 클래스에 `@DisplayName("US1-AC1 ...")` 형식으로 둔다
- **Rationale**: 원칙 II의 형식이다. 스펙은 클래스 이름으로 구분한다
