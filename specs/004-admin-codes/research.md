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

## R5. 그룹을 코드로 등록하는 방법 (2026-11-15, 2026-11-18 R9로 바뀜)

> 2026-11-18에 그룹을 다시 DB에 두고 관리자가 화면에서 다루기로 했다(spec.md Assumptions). 아래는 그 전 결정의 기록이고, 지금 기준은 R9다.


- **Decision**: 메뉴의 프로그램(`001` research R13)과 같은 모양으로 `CodeGroupSource` 빈이 `CodeGroupDefinition(groupCode, name, description, usedBy)`를 내고 `CodeGroupRegistry`가 모은다. 시작 검사(틀리면 시작 실패): 그룹 코드 형식 `[A-Z][A-Z0-9_]{0,49}`, 겹침, 이름·쓰는 곳 필수
  - 등록이 그룹의 기준이다. `code_groups` 표는 쓰지 않으므로 moa 마이그레이션 `V2_7__code_groups_from_code.sql`이 지운다 (moa에는 그룹이 하나도 없다). 항목(`codes`)은 그대로 `group_code`로 그룹을 가리킨다
  - 관리 API의 그룹 만들기(`POST /code-groups`)를 없앤다. 그룹 목록·화면용 목록(`GET /api/v1/codes/{groupCode}`)은 등록된 그룹만 다룬다(없는 그룹 R001)
  - 등록에서 빠진 그룹의 항목은 남아 있지만 어디에도 보이지 않는다 (스펙 Edge Cases)
- **Rationale**: 그룹은 그것을 읽는 기능의 코드와 함께 있어야 쓸모가 있다. 화면·DB에 두면 코드와 어긋날 수 있다
- **Alternatives considered**: 시작할 때 등록을 `code_groups`에 맞춰 넣기(동기화). 두 곳이 기준이 되어 어긋날 때 무엇이 맞는지 흐려진다

## R6. 항목 코드 자동 순번 (FR-006)

- **Decision**: 코드를 비우고 만들면, 그 그룹의 지운 것까지 포함한 항목 중 숫자로만 된 코드의 가장 큰 값 + 1을 세 자리 이상으로 채운다(`001`). 지운 항목은 `@SQLRestriction` 밖이라 네이티브 쿼리로 센다. 같은 코드가 있으면 저장 전에 R002로 막는다
- **Rationale**: 지운 순번을 다시 쓰면 예전에 저장된 값이 새 항목을 가리키게 된다
- **구현 때 바꿈 (2026-11-17)**: 같은 순간에 두 번 만들어 겹칠 때 다시 시도하던 안은 두지 않았다. 저장이 바로 INSERT되어 유일 키에 걸리면 그 트랜잭션은 되살릴 수 없어 새 트랜잭션이 필요하다. 한 그룹에 두 관리자가 같은 순간 항목을 만들 때만 생기므로, 그때는 유일 키가 겹침을 막고 서버 오류가 난다(다시 저장하면 된다)

## R7. 트리 표와 일괄 처리 (FR-005)

- **Decision**:
  - `GET /api/v1/admin/code-groups/tree`: 등록된 그룹마다 `groupCode`, `name`, `description`, `usedBy`, 항목(`code`, `name`, `sortOrder`, `enabled`, 사용 안 함 포함)
  - `PUT /api/v1/admin/code-groups/{groupCode}/codes/order` (`[{code, sortOrder}]`): 그룹 안 순서 한꺼번에
  - `PUT /api/v1/admin/code-groups/{groupCode}/codes/enabled` (`codes`, `enabled`), `POST /api/v1/admin/code-groups/{groupCode}/codes/bulk-delete` (`codes`): 한 트랜잭션, 하나라도 틀리면(없는 코드 R001) 아무것도 바꾸지 않는다
  - 화면은 `001` R22의 메뉴 표와 같은 부품(트리 표, 손잡이, 일괄 줄, 드로어)을 쓴다. 그룹 행은 체크 칸이 없고 접고 편다. 항목 드로어: 이름, 사용, 코드(만들 때만 적을 수 있음, 비우면 자동). 그룹마다 "항목 추가" 줄
  - 화면은 여러 그룹을 한 표에 두지만 일괄 처리는 한 그룹 안에서만 한다 (다른 그룹의 항목을 함께 고르면 일괄 줄이 "한 그룹씩 처리해요"로 안내)
- **Rationale**: 메뉴 관리와 조작을 같게 한다(스펙 Assumptions). 코드는 그룹 안에서만 뜻이 있어 API도 그룹 단위다

## R8. 무엇을 테스트하나 (US1)

- **2026-11-18 R10으로 바뀜**. 아래는 그 전 기록이다
- **Decision (그 전)**: 테스트에서만 등록하는 그룹 둘(`TestCodeGroups`)로 API 인수 테스트(`AdminCodesAcceptanceTest`): US1-AC1(트리 응답, 그룹 만들기 API 없음 405), US1-AC2(이름만 → `001`, 지운 뒤 다시 만들면 `002`), US1-AC3(이름·사용 바꾸기, 코드 바꾸기 없음), US1-AC4(`enabled` 둘 끄기 → 화면용 목록에서 빠짐), US1-AC5(`order`, 다른 그룹 코드는 R001), US1-AC6(등록 그룹 없음 → 빈 목록. 테스트 실행에는 시험 그룹이 언제나 등록되므로 `CodeGroupRegistryTest` 단위 테스트로 본다). 키트 `CodeAcceptanceTest`의 그룹 만들기 테스트는 없어진 동작이라 지운다 (원칙 V). e2e(`code.spec.ts`): 트리, 그룹 추가 단추 없음, 드로어(이름만), 일괄, 빈 안내, 머리 안내

## R9. 그룹을 DB에 두고 화면에서 다룬다 (FR-002·FR-004·FR-008, 2026-11-18)

> 2026-11-20 R11로 바뀌었다: `system`·`used_by`와 삭제를 걷어내고 사용 여부를 둔다. `code_groups`를 DB에 두는 것은 그대로다. 아래는 그 전 기록이다

- **Decision**:
  - 새 마이그레이션(`V2_8__code_groups_on_screen.sql`, 구현할 때 dev의 마지막 번호를 다시 본다)이 `code_groups`를 다시 만든다. 열은 키트 `V1_4__codes.sql`의 `code_groups`와 같고 `used_by VARCHAR(200) NULL`, `system BOOLEAN NOT NULL DEFAULT FALSE`를 더한다. 소프트 삭제와 `uk_code_groups_group_code_active`도 키트와 같다. moa에는 아직 그룹이 없어 옮길 행이 없다
  - `CodeGroupDefinition`·`CodeGroupSource`·`CodeGroupRegistry`와 local 시험 그룹 `LocalCodeGroups`를 지운다. 그룹의 기준은 `code_groups` 표 하나다
  - 관리 API
    - `POST /api/v1/admin/code-groups` (`groupCode`, `name`, `description`, `usedBy`): 일반 그룹(`system` = false)으로 만든다. 그룹 코드 형식 `[A-Z][A-Z0-9_]{0,49}`가 틀리면 400 `V001`, 지우지 않은 그룹과 겹치면 409 `R002`. 요청에 `system`은 없다
    - `PUT /api/v1/admin/code-groups/{groupCode}` (`name`, `description`, `usedBy`): 그룹 코드와 `system`은 바꾸지 않는다. 시스템 그룹도 고칠 수 있다
    - `DELETE /api/v1/admin/code-groups/{groupCode}`: 그룹과 그 항목을 한 트랜잭션에서 소프트 삭제한다. 시스템 그룹은 409 `B001`, 아무것도 지우지 않는다
    - `GET .../tree`, `GET /api/v1/admin/code-groups` 응답에 `system`을 더한다. 그룹은 만든 순서(`id`)다
  - 항목 API와 화면용 목록(`GET /api/v1/codes/{groupCode}`)은 그룹이 `code_groups`에 없으면(지운 그룹 포함) 404 `R001`이다
  - 시스템 그룹은 기능이 마이그레이션으로 넣는다(`system` = true). 그 기능의 스펙에 그룹과 초기 코드를 적는다 (FR-002, 예: `008-party` FR-002 표)
  - 화면: 머리에 "그룹 추가", 그룹 이름을 누르면 그룹 드로어(그룹 코드는 만들 때만 적음, 이름, 설명, 쓰는 곳). 일반 그룹 드로어에 "그룹 삭제"(확인 창에 함께 지울 항목 수), 시스템 그룹은 그룹 행에 잠금 표시, 드로어에 삭제 대신 "기능이 쓰는 그룹이라 지울 수 없어요". 빈 안내는 spec.md US1-AC6 문구
  - local 프로필 시험 그룹은 `db/localdata`의 SQL로 넣는다(`R__local_accounts.sql`처럼 `INSERT IGNORE`): 시스템 그룹 `LOCAL_GAME_GENRE`(게임 장르 (시험)), 일반 그룹 `LOCAL_REPORT_REASON`(신고 사유 (시험))
- **Rationale**: 그룹과 항목을 모두 관리자가 화면에서 다루는 것이 목표다. 기능이 읽는 그룹만 지울 수 없게 막으면 기능이 깨지지 않는다. 그룹을 DB 한곳에 두면 기준이 하나다
- **Alternatives considered**
  - 코드 등록 유지 + DB 표: 시스템 그룹의 오타·누락을 서버 시작 때 잡지만 그룹이 두 곳에 나뉜다 (spec.md Assumptions 버린 안)
  - 일반 그룹의 그룹 코드 바꾸기 허용: 항목의 `group_code`를 함께 옮겨야 하고, 나중에 그 그룹을 읽는 기능이 생기면 어긋난다
  - 항목이 있는 그룹은 지우지 못하게 막기: 항목을 하나씩 지운 뒤에야 그룹을 지울 수 있어 번거롭다. 확인 창에 항목 수를 보이는 것으로 갈음한다

## R10. 무엇을 테스트하나 (US1, 2026-11-18)

> 2026-11-20 R13으로 바뀌었다. 아래는 그 전 기록이다

- **Decision**: API 인수 테스트(`AdminCodesAcceptanceTest`). `TestCodeGroups`를 지우고, 각 테스트가 시스템 그룹 하나를 JDBC로 넣고(API로는 시스템 그룹을 만들 수 없다) 일반 그룹 하나를 API로 만든다
  - US1-AC1: 트리 응답에 두 그룹과 `system`
  - US1-AC2~AC5: R8과 같다
  - US1-AC6: 그룹이 없으면 트리가 빈 목록 (DB가 비어 있으므로 인수 테스트로 본다. `CodeGroupRegistryTest`는 지운다)
  - US1-AC7: 그룹 만들기 201, 같은 그룹 코드(시스템 그룹 코드 포함) 409 `R002`, 형식 틀림 400 `V001`
  - US1-AC8: 그룹 고치기 200, 그룹 코드·`system`은 그대로
  - US1-AC9: 항목 둘 있는 일반 그룹 지우기 204 → 트리에서 빠지고 `GET /api/v1/codes/{g}` 404 `R001`
  - US1-AC10: 시스템 그룹 지우기 409 `B001`, 그룹과 항목이 남는다
  - 키트 `CodeAcceptanceTest`는 R8에서 지운 그룹 만들기 테스트를 되살리지 않는다. 그룹 만들기는 US1-AC7이 본다
  - e2e(`code.spec.ts`): 트리와 잠금 표시, 그룹 추가 드로어, 그룹 고치기, 일반 그룹 삭제 확인 창(항목 수), 시스템 그룹 드로어의 안내, 빈 안내, 머리 안내(FR-007)

## R11. 지우지 않고 사용 여부로 끈다 (FR-002·FR-004·FR-008, 2026-11-20)

- **Decision**:
  - 새 마이그레이션(`V2_9__code_groups_enabled.sql`, 구현할 때 dev의 마지막 번호를 다시 본다)이 `code_groups`에서 `used_by`·`system`을 지우고 `enabled BOOLEAN NOT NULL DEFAULT TRUE`를 더한다. 소프트 삭제 열(`deleted_at` 등)은 키트 표 그대로 두지만 쓰지 않는다. 이미 지운 행은 moa에 없다(시험 데이터만)
  - `CodeGroup` 엔티티: 그룹 코드, 이름, 설명, 사용 여부. `modify(name, description, enabled)`
  - 관리 API
    - `POST /api/v1/admin/code-groups` (`groupCode`, `name`, `description`): 사용 중으로 만든다. 형식 V001, 겹치면 R002
    - `PUT /api/v1/admin/code-groups/{groupCode}` (`name`, `description`, `enabled`): 그룹 코드는 바꾸지 않는다
    - 지우는 API를 모두 없앤다: `DELETE /api/v1/admin/code-groups/{groupCode}`, `DELETE /api/v1/admin/code-groups/{groupCode}/codes/{code}`, `POST /api/v1/admin/code-groups/{groupCode}/codes/bulk-delete`
    - `GET .../tree`, `GET /api/v1/admin/code-groups` 응답에서 `usedBy`·`system`을 빼고 `enabled`를 더한다
  - 화면용 목록 `GET /api/v1/codes/{groupCode}`: 그룹이 없으면 R001(그대로). 그룹이 꺼져 있으면 빈 목록(200). `includeDisabled=true`(예전 데이터의 이름을 보일 때)면 그룹이 꺼져 있어도 모든 항목을 준다
  - 항목 자동 순번(R6)은 그대로다. 지운 항목이 더 생기지 않을 뿐이다
  - 키트 `CodeAcceptanceTest`의 코드 지우기 부분은 없어진 동작이라 지운다 (원칙 V)
- **Rationale**: 지우지 않으면 기능이 쓰는 그룹이나 이미 저장된 값이 가리키는 항목이 사라지지 않는다. 시스템 표시를 개발자가 빠뜨려도 사고가 나지 않는다. 그룹웨어·SI 공통코드에서 흔한 방식이다
- **Alternatives considered**
  - 시스템 그룹만 잠그기(R9): 표시를 빠뜨리면 지워진다. 관리자가 잠금의 뜻을 알기 어렵다
  - 쓰지 않은(어디에도 저장되지 않은) 항목만 지우기 허용: 어디에 저장됐는지 공통코드가 알 수 없다(기능마다 표가 다르다)

## R12. 두 칸 화면 (FR-005·FR-009, 2026-11-20)

- **Decision**:
  - 넓은 폭(`lg` 이상): 두 칸 그리드. 머리 아래 남은 높이를 채우도록 `flex-1 basis-0 min-h-0`으로 두고 행을 `minmax(0,1fr)`로 고정한다. 왼쪽은 일괄 처리 줄 + 트리(칸 안 스크롤), 오른쪽은 상세 패널(머리·스크롤 본문·아래 저장 줄)
  - 상세 패널은 지금 드로어의 폼(항목·그룹)을 그대로 쓰고 감싸는 틀만 바꾼다(`DetailPanel`, `SideDrawer`와 같은 props). 좁은 폭(`useIsMobile`, 사이드바가 서랍이 되는 폭)에서는 `SideDrawer`로 연다
  - 저장하지 않은 변경: 패널이 dirty를 화면에 알리고, 화면은 다른 행을 고르기 전에 `001` FR-012와 같은 문구로 묻는다
  - 일괄 처리 줄(`BulkToolbar`)은 늘 그린다. 높이를 고정한다(`min-h-11`, 키트 부품을 고친다). 단추: 사용, 사용 안 함
  - 그룹 행: 첫 칸 체크(항목이 있을 때만, 일부 선택 표시), 둘째 칸 접기, 이름 칸에 이름·설명(흐리게: 꺼진 그룹), 코드 칸에 그룹 코드, 사용 칸에 상태
  - 트리 맨 아래 "그룹 추가" 줄. 그룹이 없으면 빈 안내 아래 "그룹 추가" 단추
  - 상태 색: 테마에 `--success`(밝은·어두운 테마)와 `--color-success`를 더하고 `StatusText` positive를 `bg-success`로. 사용 안 함은 negative(`bg-destructive`). 글자색은 그대로 둔다(초록 글자는 흰 바탕 대비가 낮다)
  - 코드 입력 칸: `onChange`에서 대문자로, `-`·빈칸은 `_`로, 그 밖의 글자는 지운다. 그룹 코드는 맨 앞의 숫자·밑줄도 지운다. `maxLength=50`
  - 일부 선택 체크: 공통 `Checkbox`(shadcn 생성 코드)는 일부 선택에도 ✓를 그려 이 화면에서 클래스로 "−"를 입힌다
- **Rationale**: 드로어는 트리를 가린다. 일괄 처리 줄이 생겼다 사라지면 목록이 밀린다. 그룹 추가를 트리 안에 두면 항목 추가와 같은 자리 규칙이 된다
- **Alternatives considered**: 3단 칸·표 안 바로 편집(스펙 Assumptions), 패널 너비 고정(사용자가 반반을 골랐다), 공통 `Checkbox`를 고치기(생성 코드라 손대지 않는다)

## R13. 무엇을 테스트하나 (US1, 2026-11-20)

- **Decision**: API 인수 테스트(`AdminCodesAcceptanceTest`). 각 테스트가 그룹 둘을 API로 만들고 시작한다(시스템 그룹이 없어져 JDBC로 넣을 것이 없다)
  - US1-AC1: 트리 응답에 두 그룹, `enabled`, 항목. `usedBy`·`system`이 없다
  - US1-AC2·AC3·AC5: R8과 같다
  - US1-AC4: 한 그룹 항목 둘 `enabled` false → 화면용 목록에서 빠진다. 하나라도 없으면 R001, 아무것도 안 바뀐다
  - US1-AC6: 그룹이 없으면 빈 목록
  - US1-AC7: 그룹 만들기 201 `enabled` true, 겹침 R002, 형식 V001
  - US1-AC8: 그룹 고치기(이름·설명·사용) 200, 그룹 코드 그대로. 그룹·항목 지우기 API는 없다(405)
  - US1-AC9: 그룹 끄기 → 화면용 목록 빈 목록, `includeDisabled=true`면 항목이 그대로, 다시 켜면 켜진 항목이 보인다
  - US1-AC10은 화면 동작이라 e2e로만 본다
  - e2e(`code.spec.ts`): 두 칸과 안내, 일괄 처리 줄이 고르기 전후에 같은 자리, 그룹 체크(모두·일부), 그룹 추가 줄과 입력 거르기, 그룹 패널(사용 스위치, 삭제 없음), 꺼진 그룹 흐림, 저장하지 않은 변경 묻기, 좁은 폭 드로어
