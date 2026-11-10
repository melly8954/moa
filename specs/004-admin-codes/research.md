# Research: 공통코드 (admin-codes)

Technical Context에 미결은 없다. 아래는 코드를 확인해 정한 것이다.

## R1. 만들 것의 범위

- **Decision**: 운영 코드는 바꾸지 않고 AC 테스트만 더한다
- **Rationale**: 코드 그룹·코드 관리와 화면용 목록이 키트에 있다. 마이그레이션(`V1_4__codes.sql`)과 로컬 데이터에 코드 그룹 INSERT가 없어 처음에는 비어 있다 (FR-001 표와 같다)
- **Alternatives considered**: 지금 moa 코드 그룹을 미리 만들기. 쓰는 기능이 없고, FR-002는 그 기능의 스펙이 그룹을 정하게 한다

## R2. 캐시 (FR-003)

- **Decision**: 지금 그대로 둔다. 확인할 것은 없다
- **Rationale**: `CodeApplication`에 `@Cacheable`·`@CacheEvict`가 있지만, 캐시를 켜는 `@EnableCaching`은 redis 헬퍼가 주는 것이라 moa에는 없다. 그래서 어노테이션이 아무 일도 하지 않고, 바꾼 값이 다음 조회에 바로 보인다 (SC-001)
- **주의**: 나중에 redis 헬퍼를 넣으면 캐시가 켜진다. 그때는 FR-003을 이 스펙에서 고친다
- **Alternatives considered**: 캐시가 꺼져 있음을 테스트로 고정. AC가 아니고, 키트 `CodeAcceptanceTest`가 바꾼 값의 반영을 이미 본다

## R3. US1-AC1을 확인하는 방법

- **Decision**: 관리자로 `GET /api/v1/admin/code-groups`를 불러 그룹 코드 목록이 FR-001 표와 같은지 본다. 지금 표는 비어 있으므로 빈 목록이다
- **Rationale**: 화면은 이 API를 그대로 보인다 (`frontend/src/api/code.ts`)
- **다음 기능이 할 일**: 기능이 코드 그룹을 시드로 넣으면(FR-002), 이 테스트의 기대 목록에 그룹을 더한다. 그때 `DatabaseCleaner`가 시드를 지우므로 `MenuSeed`처럼 시드를 되돌리는 지원 코드가 필요하다 (`003-admin-roles` research.md R3)
- **Alternatives considered**: 빈 목록이 아니라 "200이면 된다"만 확인. 표와 실제가 어긋나도 통과해 FR-001을 지키지 못한다

## R4. 테스트 이름과 위치

- **Decision**: `AdminCodesAcceptanceTest` 한 클래스에 `@DisplayName("US1-AC1 ...")` 형식으로 둔다
- **Rationale**: 원칙 II의 형식이다. 스펙은 클래스 이름으로 구분한다
