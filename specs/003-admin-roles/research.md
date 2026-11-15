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

## R6. 역할 창의 메뉴 트리를 어디서 받나 (FR-004, 2026-11-15)

- **Decision**: `GET /api/v1/admin/roles/permission-tree`(역할 관리 권한)를 새로 둔다. 응답은 두 부분이다
  - `menus`: 관리 메뉴 트리. 메뉴 관리와 같은 순서·묶음이고 꺼 둔 메뉴도 넣는다. 노드마다 `menuId`, `name`, `enabled`, `permission`(`key`, `name`. 묶음 메뉴는 없음), `children`. 없는 프로그램을 가리키는 메뉴는 뺀다
  - `unlisted`: 어느 관리 메뉴도 가리키지 않는 관리 프로그램. `programKey`, `name`, `permission`(`key`, `name`)
  - 역할 저장은 지금 API(`POST`·`PUT /api/v1/admin/roles`, 권한 키 목록) 그대로다. 화면이 체크한 노드의 권한 키를 모아 보낸다
  - rbac 쪽 application이 menu 헬퍼의 메뉴 트리와 `ProgramRegistry`(`001-admin-menus` research R13)를 읽어 만든다
- **Rationale**: 역할 관리 권한만 받은 운영자는 메뉴 관리 API(`GET /api/v1/admin/menus`, 메뉴 관리 권한)를 부를 수 없다(FR-008). 역할에는 지금처럼 권한이 담기므로 저장 API를 바꿀 까닭이 없다
- **Alternatives considered**: 역할에 메뉴 id를 담기. 메뉴를 지우거나 옮기면 역할의 권한이 바뀌어 버린다(스펙 Assumptions). 화면이 메뉴 API와 권한 API를 둘 다 불러 합치기: 권한이 하나만 있는 운영자는 한쪽을 못 부른다
- **키트로 옮길 것**: rbac는 menu 없이도 쓰일 수 있다. 키트에서는 menu 헬퍼가 트리를 주는 인터페이스를 구현하고, 없으면 rbac가 지금처럼 권한 목록을 묶음별로 준다

## R7. 사용자별 역할 탭 (FR-005)

- **Decision**: `GET /api/v1/admin/roles/users?keyword=&page=&size=`(역할 관리 권한, 페이지)를 새로 둔다
  - `keyword`가 없으면 업무 역할이 하나라도 있는 사용자와 시스템 역할 `ADMIN`인 사용자, 있으면 아이디·이름에 그 글자가 든 모든 사용자다. 시스템 계정은 빼고, 아이디 순서다
  - 응답 항목: `id`, `loginId`, `name`, `role`(시스템 역할), `accessRoles`(`id`, `code`, `name`)
  - 업무 역할 바꾸기는 지금 `PUT /api/v1/admin/users/{userId}/roles`(역할 관리 권한)를 쓴다. 대상이 `ADMIN`이면 `B001`로 막는다(스펙 Edge Cases)
- **Rationale**: 사용자 관리 API(`GET /api/v1/admin/users`)는 사용자 관리 권한이라 FR-008을 어긴다. 응답을 아이디·이름·역할로 줄여 역할 관리에 필요한 것만 준다
- **Alternatives considered**: 사용자 관리 API에 업무 역할 열을 더하기. 권한이 다르고, 사용자 관리에서 역할을 다루지 않기로 했다(FR-007)

## R8. 역할별 구성원 (FR-006)

- **Decision**: 역할 관리 권한으로 세 API를 새로 둔다
  - `GET /api/v1/admin/roles/{id}/members`: 그 역할을 가진 사용자(`id`, `loginId`, `name`), 아이디 순서
  - `POST /api/v1/admin/roles/{id}/members` (`userId`): 더한다. 이미 있으면 그대로 둔다
  - `DELETE /api/v1/admin/roles/{id}/members/{userId}`: 뺀다
  - 다룰 수 없는 역할이면 `A005`(키트 권한 상승 방지, `RoleGrantPolicy.canManage`), 대상이 `ADMIN`이면 `B001`, 없는 역할·사용자는 `R001`. 더할 사용자는 R7 API의 `keyword`로 찾는다
- **Rationale**: 구성원 화면은 한 역할에 한 사람씩 더하고 뺀다. 사용자의 역할 전체를 통째로 바꾸는 API(`PUT .../users/{userId}/roles`)를 화면에서 조합하면, 그 사이 다른 관리자가 바꾼 역할을 덮어쓴다
- **Alternatives considered**: 역할의 구성원 전체를 통째로 바꾸는 API. 같은 덮어쓰기 문제가 있다

## R9. 사용자 관리의 역할 주기 빼기 (FR-007, `002-admin-users` FR-005)

- **Decision**: frontend/src/app/(admin)/admin/users/user-extras.tsx에서 업무 역할 메뉴(`useUserRolesAction`)와 대화상자를 빼고, frontend/src/components/role/user-roles.tsx의 역할 체크 폼은 사용자별 역할 탭으로 옮긴다
- **Rationale**: 역할을 주는 곳을 한 곳으로 모은다. 체크 폼은 그대로 쓸 수 있다

## R10. 역할 관리 화면 (FR-004~FR-006, SC-004·SC-005)

- **Decision**:
  - 화면 위에 "역할" / "사용자별 역할" 탭을 둔다
  - 역할 탭: 지금 표에 "구성원" 단추를 더한다. 구성원 대화상자는 목록(아이디, 이름, 빼기)과 사용자 찾아 더하기다
  - 사용자별 역할 탭: 검색창(아이디·이름), 표(아이디, 이름, 시스템 역할, 업무 역할, "역할 변경" 단추). `ADMIN` 행은 업무 역할 칸이 "모든 권한"이고 단추가 없다
  - 역할 대화상자의 권한 칸: R6 트리를 체크 목록으로 그린다. 묶음 메뉴를 체크하면 하위 메뉴가 모두 체크되고, 같은 권한의 노드는 함께 체크된다. 꺼 둔 메뉴는 "꺼짐"을 붙인다. 권한 키는 작은 글자로 옆에 둔다. 맨 아래에 "메뉴에 없는 프로그램" 묶음을 둔다
  - 문구는 frontend/messages/ko.json `role`에 둔다
- **키트로 옮길 것**: rbac 헬퍼 화면(탭, 구성원, 트리 체크)과 API(R6~R8)

## R11. 무엇을 테스트하나 (US2~US4)

- **Decision**:
  - API 인수 테스트 `AdminRolesAcceptanceTest`: US2-AC1(`permission-tree`의 `menus`가 관리 메뉴 6개를 처음 구성 순서와 권한으로, `unlisted`는 비어 있음), US2-AC2(접속 이력 권한만 담은 역할을 다시 읽으면 권한이 그것 하나, 목록의 권한 수 1), US2-AC3(코드 관리 메뉴를 지우면 `unlisted`에 코드 관리), US3-AC1~AC3(R7 API와 역할 바꾸기 뒤 운영자의 `GET /api/v1/admin/login-histories`), US4-AC1~AC3(R8 API와 운영자의 접속 이력 접근)
  - 화면은 e2e: 역할 대화상자가 메뉴 트리로 체크하고 키 입력이 없다, 두 탭, 구성원 대화상자, 사용자 관리 "관리" 메뉴에 역할 항목이 없다
  - 키트 rbac 테스트 중 `ADMIN`에게 업무 역할을 주는 테스트가 있으면 스펙이 바뀐 만큼만 고친다 (원칙 V)

## R12. 역할 목록과 드로어 (FR-005, 2026-11-15 관리 화면 정리)

- **Decision**: 역할 목록은 지금의 `GET /api/v1/admin/roles`(전체)를 쓰고, 이름·코드 검색은 화면에서 거른다(구성 목록, 100개 이하). 열: 체크박스, 이름(누르면 드로어), 코드, 권한 수, 구성원 수. 드로어는 `001` R21의 공용 드로어(`side-drawer`)에 탭 "기본·메뉴"(이름·설명·권한 트리, 지금 `role-dialog.tsx`의 내용)와 "구성원"(R13)을 둔다. 새 역할은 "기본·메뉴"만 있고 저장하면 "구성원"이 열린다
- 한꺼번에 지우기: `POST /api/v1/admin/roles/bulk-delete` (`ids`). 한 트랜잭션, 없는 역할 R001, 다룰 수 없는 역할 A005가 하나라도 있으면 아무것도 지우지 않는다
- "사용자별 역할" 탭(`user-roles-tab.tsx`)과 `GET /api/v1/admin/roles/users`, 구성원 창(`members-dialog.tsx`)은 지운다

## R13. 구성원 탭 (FR-006, US4)

- **Decision**:
  - `GET /api/v1/admin/roles/{id}/members?keyword=&page=&size=` (쪽 나눔): `keyword`가 없으면 그 역할의 구성원만, 있으면 아이디·이름에 그 글자가 든 모든 사용자. 아이디 순. 항목 `id`, `loginId`, `name`, `role`, `member`(구성원인가). 시스템 계정·지운 사용자 제외
  - `POST /api/v1/admin/roles/{id}/members` (`userIds`): 한꺼번에 더하기. 이미 구성원이면 그대로
  - `POST /api/v1/admin/roles/{id}/members/remove` (`userIds`): 한꺼번에 빼기. 구성원이 아니면 그대로
  - 둘 다 한 트랜잭션. 없는 역할·사용자 R001, 다룰 수 없는 역할 A005, `ADMIN`이 하나라도 있으면 B001이고 아무것도 바꾸지 않는다
  - 지금의 한 사람 API(`POST .../members` `{userId}`, `DELETE .../members/{userId}`)는 위 둘로 바꾼다
- 화면: 드로어 안에 검색 칸, 표(체크박스·아이디·이름·구성원 표시), 쪽 번호, 일괄 줄("더하기"·"빼기"). `ADMIN` 행은 "모든 권한"이고 체크 칸이 꺼져 있다. 쪽은 드로어 안 상태로 두고 주소에 두지 않는다(고르기 목록)
- **Rationale**: 고르기 목록이라 검색과 서버 쪽 나눔이 필요하다(원칙). 일괄 API는 덮어쓰기 없이 고른 사람만 바꾼다
- **Alternatives considered**: 구성원만 보이는 목록과 더할 사람 검색을 나누기. 한 표에 구성원 표시를 두면 조작이 하나로 끝난다

## R14. 사용자 관리의 업무 역할 열을 위한 조회 (002 FR-006)

- **Decision**: `RoleUserService`에 사용자 ID 목록 → 업무 역할 목록을 주는 공개 메서드를 둔다. 002의 사용자 관리 목록이 이를 써서 응답에 `accessRoles`를 더한다 (002 research R6)

## R15. 무엇을 테스트하나 (US2~US4, 2026-11-15)

- **Decision**:
  - API 인수 테스트(`AdminRolesAcceptanceTest`): US3-AC3(`bulk-delete` 둘, 하나라도 다룰 수 없으면 A005로 아무것도 안 지워짐), US4-AC1(검색 없는 구성원 쪽 나눔: 21명 넣고 `size`=20 첫 쪽), US4-AC2(검색 → `member` 표시, `userIds` 둘 더하기 → 접속 이력 200), US4-AC3(둘 빼기 → 403), US4-AC4(`ADMIN` 섞어 더하기 → B001, 아무도 안 더해짐). 없어진 "사용자별 역할" 테스트(옛 US3)는 지운다 (원칙 V, 스펙이 바뀐 만큼)
  - e2e(`role.spec.ts`): US3-AC1·AC2(탭 없음, 화면 거르기), 드로어 기본·메뉴(트리 체크), 새 역할 저장 뒤 구성원 탭, US4-AC2·AC4(체크 일괄 더하기, `ADMIN` 체크 꺼짐), 머리 안내
