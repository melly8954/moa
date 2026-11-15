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

## R5. 그룹을 코드로 등록하는 방법 (FR-004, 2026-11-15 관리 화면 정리)

- **Decision**: 메뉴의 프로그램(`001` research R13)과 같은 모양으로 `CodeGroupSource` 빈이 `CodeGroupDefinition(groupCode, name, description, usedBy)`를 내고 `CodeGroupRegistry`가 모은다. 시작 검사(틀리면 시작 실패): 그룹 코드 형식 `[A-Z][A-Z0-9_]{0,49}`, 겹침, 이름·쓰는 곳 필수
  - 등록이 그룹의 기준이다. `code_groups` 표는 쓰지 않으므로 moa 마이그레이션 `V2_7__code_groups_from_code.sql`이 지운다 (moa에는 그룹이 하나도 없다). 항목(`codes`)은 그대로 `group_code`로 그룹을 가리킨다
  - 관리 API의 그룹 만들기(`POST /code-groups`)를 없앤다. 그룹 목록·화면용 목록(`GET /api/v1/codes/{groupCode}`)은 등록된 그룹만 다룬다(없는 그룹 R001)
  - 등록에서 빠진 그룹의 항목은 남아 있지만 어디에도 보이지 않는다 (스펙 Edge Cases)
- **Rationale**: 그룹은 그것을 읽는 기능의 코드와 함께 있어야 쓸모가 있다. 화면·DB에 두면 코드와 어긋날 수 있다
- **Alternatives considered**: 시작할 때 등록을 `code_groups`에 맞춰 넣기(동기화). 두 곳이 기준이 되어 어긋날 때 무엇이 맞는지 흐려진다

## R6. 항목 코드 자동 순번 (FR-006)

- **Decision**: 코드를 비우고 만들면, 그 그룹의 지운 것까지 포함한 항목 중 숫자로만 된 코드의 가장 큰 값 + 1을 세 자리 이상으로 채운다(`001`). 지운 항목은 `@SQLRestriction` 밖이라 네이티브 쿼리로 센다. 같은 순간에 두 번 만들어 겹치면(유일 키) 한 번 다시 시도하고, 또 겹치면 R002
- **Rationale**: 지운 순번을 다시 쓰면 예전에 저장된 값이 새 항목을 가리키게 된다

## R7. 트리 표와 일괄 처리 (FR-005)

- **Decision**:
  - `GET /api/v1/admin/code-groups/tree`: 등록된 그룹마다 `groupCode`, `name`, `description`, `usedBy`, 항목(`code`, `name`, `sortOrder`, `enabled`, 사용 안 함 포함)
  - `PUT /api/v1/admin/code-groups/{groupCode}/codes/order` (`[{code, sortOrder}]`): 그룹 안 순서 한꺼번에
  - `PUT /api/v1/admin/code-groups/{groupCode}/codes/enabled` (`codes`, `enabled`), `POST /api/v1/admin/code-groups/{groupCode}/codes/bulk-delete` (`codes`): 한 트랜잭션, 하나라도 틀리면(없는 코드 R001) 아무것도 바꾸지 않는다
  - 화면은 `001` R22의 메뉴 표와 같은 부품(트리 표, 손잡이, 일괄 줄, 드로어)을 쓴다. 그룹 행은 체크 칸이 없고 접고 편다. 항목 드로어: 이름, 사용, 코드(만들 때만 적을 수 있음, 비우면 자동). 그룹마다 "항목 추가" 줄
  - 화면은 여러 그룹을 한 표에 두지만 일괄 처리는 한 그룹 안에서만 한다 (다른 그룹의 항목을 함께 고르면 일괄 줄이 "한 그룹씩 처리해요"로 안내)
- **Rationale**: 메뉴 관리와 조작을 같게 한다(스펙 Assumptions). 코드는 그룹 안에서만 뜻이 있어 API도 그룹 단위다

## R8. 무엇을 테스트하나 (US1)

- **Decision**: 테스트에서만 등록하는 그룹 둘(`TestCodeGroups`)로 API 인수 테스트(`AdminCodesAcceptanceTest`): US1-AC1(트리 응답, 그룹 만들기 API 없음 405), US1-AC2(이름만 → `001`, 지운 뒤 다시 만들면 `002`), US1-AC3(이름·사용 바꾸기, 코드 바꾸기 없음), US1-AC4(`enabled` 둘 끄기 → 화면용 목록에서 빠짐), US1-AC5(`order`, 다른 그룹 코드는 R001), US1-AC6(등록 그룹 없음 → 빈 트리). 키트 `CodeAcceptanceTest`의 그룹 만들기 테스트는 없어진 동작이라 지운다 (원칙 V). e2e(`code.spec.ts`): 트리, 그룹 추가 단추 없음, 드로어(이름만), 일괄, 빈 안내, 머리 안내
