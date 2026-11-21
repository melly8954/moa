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

## R16. 최고 관리자 역할을 무엇으로 두나 (FR-010, US2, 2026-11-21)

- **Decision**:
  - `tb_access_role`에 코드 `SUPER_ADMIN`, 이름 "최고 관리자"인 행 하나를 새 마이그레이션(V7, R20)으로 넣는다. 권한 행(`tb_access_role_permission`)은 두지 않는다. 고정 역할인지는 열을 더하지 않고 코드 상수(`AccessRoles.SUPER_ADMIN_CODE`)로 가린다
  - `PermissionGuard.permissions(principal)`: 시스템 역할 `ADMIN`이고 최고 관리자 역할의 구성원이면 `registry.keys()`(모든 권한, 나중에 더해지는 것 포함), `ADMIN`이면 받은 역할의 합집합(지금 `PermissionChecker`), `USER`면 빈 집합. 지금의 "`ADMIN`이면 모두"를 바꾼다. 구성원인지는 권한처럼 요청마다 한 번 조회한다(`AccessRolePermissionChecker`에 `isSuperAdmin(userId)`)
  - `Role` enum 주석, `PermissionChecker` 주석, `ApiMenuController` 설명 문구의 "ADMIN = 모든 권한"을 고친다. `SecurityConfig.roleHierarchy()`(ADMIN이 USER를 품음)는 그대로 둔다. 계정 화면(알림·설정·비밀번호)이 `USER` 권한으로 열려 있기 때문이다
  - 최고 관리자 역할은 `AccessRoleService.modify`·`delete`·`deleteAll`에서 막는다(`B001`, "최고 관리자 역할은 바꾸거나 지울 수 없어요"). 목록·상세 응답에 `fixed`(true)를 붙이고 목록은 이 역할을 맨 앞에 둔다. 권한 수는 등록된 권한 전체 개수로 보인다
- **Rationale**: 권한을 행으로 담으면 새 화면이 생길 때마다 마이그레이션으로 행을 더해야 하고, 빠뜨리면 최고 관리자가 새 화면을 못 쓴다. 코드로 가리면 열을 더하지 않아도 되고, 인수 테스트가 표를 비운 뒤에도 코드로 다시 만들 수 있다(R21)
- **Alternatives considered**: `tb_access_role`에 `fixed` 열 더하기(최고 관리자 하나뿐이라 열의 값이 늘 한 행만 다르다), 권한 키 `*`를 행으로 담기(키 검사·트리·권한 수가 모두 `*`를 따로 다뤄야 한다), 사용자에 `super_admin` 열 두기(최고 관리자도 역할이라는 스펙 FR-010과 다르고, 구성원 목록·드로어 체크를 따로 만들어야 한다)
- **키트로 옮길 것**: `PermissionGuard`·`PermissionChecker`의 판정, 최고 관리자 역할

## R17. 역할을 주고 다루는 규칙 (FR-002·FR-006~FR-008·FR-010, US2·US5, 2026-11-21)

- **Decision**: 규칙은 `RoleGrantPolicy`(지금 `PermissionCeilingGrantPolicy`)와 `RoleUserService`에 모은다. 권한 관리 구성원과 사용자 관리 드로어(`PUT /api/v1/admin/users/{userId}/roles`)가 같은 코드를 지난다
  - 행위자: 지금의 "ADMIN이면 무엇이든"을 "최고 관리자면 무엇이든"으로 바꾼다
  - 역할: 최고 관리자 역할, 사용자 관리 권한(`USER_MANAGE`)이 든 역할은 최고 관리자만 만들고(`create`)·고치고·지우고·구성원을 더하고 뺀다. 그 밖은 지금처럼 역할의 권한이 내 권한 안에 있어야 한다. 어기면 403 `A005`
  - 대상: `requireNotAdmin`(ADMIN이면 B001)을 `requireAdminAccount`(USER면 409 `B001`, "회원에게는 역할을 줄 수 없어요")로 뒤집는다. 최고 관리자 역할이 아닌 역할에 최고 관리자 역할을 가진 계정을 더하면 `B001`이다
  - 최고 관리자 역할에 더하면 그 계정의 다른 역할은 같은 트랜잭션에서 뺀다
  - 마지막 구성원: 최고 관리자 역할에서 빼서 0명이 되면 `B001`("최고 관리자는 한 명은 있어야 해요"). 사용자 관리의 회원으로 바꾸기·잠그기·지우기도 같은 검사를 부른다(`002` research R9)
  - 일괄 더하기·빼기는 지금처럼 하나라도 어기면 아무것도 바꾸지 않는다
- **Rationale**: 두 화면이 같은 서버 규칙을 쓰면 한쪽만 막히는 일이 없다. 사용자 관리 권한은 계정을 만들고 비밀번호를 초기화할 수 있어, 넘기는 일을 최고 관리자에게만 두지 않으면 권한 상승의 길이 된다(스펙 가정)
- **Alternatives considered**: 권한마다 "최고 관리자만 줄 수 있음" 표시를 `PermissionRegistry`에 두기(지금 해당하는 것이 `USER_MANAGE` 하나뿐이다. 늘면 그때 옮긴다), 최고 관리자에게 다른 역할을 함께 두기(모든 권한이라 뜻이 없고, 최고 관리자에서 뺐을 때 남는 역할이 헷갈린다)

## R18. 응답에 더하는 칸 (FR-005·FR-006, US4·US5, 2026-11-21)

- **Decision**:
  - `GET /api/v1/admin/roles` 항목: `fixed`(최고 관리자 역할), `memberCount`(지금), `permissionNames`(역할에 담긴 권한의 이름. 사용자 관리 드로어가 역할마다 쓰는 메뉴를 작게 보이는 데 쓴다, `002` FR-007), `grantable`(이 행위자가 이 역할을 주고 뺄 수 있나), `blockReason`(`SUPER_ADMIN_ONLY`·`WIDER_PERMISSIONS`·null). 최고 관리자 역할의 `permissionNames`는 비우고 화면이 "모든 권한"으로 보인다
  - `GET /api/v1/admin/roles/{id}/members` 항목: `role`을 빼고 `superAdmin`(최고 관리자 역할을 가졌나)을 더한다. 검색은 `ADMIN` 계정만 찾는다(`RoleUserReader.findSearchPage`에 `role = ADMIN` 조건). 기본 목록(그 역할의 구성원)은 이미 `ADMIN`뿐이다
- **Rationale**: 체크할 수 있는지는 서버만 정확히 안다(행위자의 권한, 역할의 권한, 최고 관리자 여부). 화면이 같은 규칙을 다시 계산하면 두 곳이 어긋난다
- **Alternatives considered**: 화면이 내 권한(`/me/permissions`)과 역할의 권한을 비교하기(최고 관리자만 다루는 규칙까지 화면에 다시 적어야 한다)

## R19. 두 칸 화면 (FR-005·FR-011, US4, 2026-11-21)

- **Decision**:
  - 틀은 메뉴 관리(`001` research R25)와 같다. `lg` 이상은 그리드 `lg:grid-cols-[minmax(16rem,1fr)_minmax(0,2fr)]`(왼쪽 목록이 좁다), 칸마다 안에서 스크롤, `components/kit/detail-panel.tsx`의 `DetailPanel`·`usePanelGuard`·`ResponsivePanel`을 쓴다. `lg` 아래는 목록만 보이고 상세를 드로어로 연다
  - `roles/role-drawer.tsx`를 `role-panel.tsx`로 바꾼다. 탭(`basic`·`members`)을 없애고, 머리(이름·코드·쓸 메뉴 수·구성원 수), 이름·설명 칸, 아래에 "쓸 메뉴"(`PermissionTreePicker`)와 "구성원"(`RoleMembers`)을 `grid-cols-2`로 나란히 둔다. 저장 줄은 칸 아래에 붙는다(이름·설명·쓸 메뉴 저장). 구성원 더하기·빼기는 지금처럼 바로 저장된다
  - 왼쪽 목록 열: 체크박스, 이름, 구성원 수. 코드와 권한 수는 오른쪽 머리로 옮긴다. 최고 관리자 행은 맨 위, 자물쇠 아이콘, 체크 칸 꺼짐. 이 역할을 고르면 쓸 메뉴 트리가 모두 체크·꺼짐이고 이름·설명 칸이 읽기 전용이다
  - 새 역할은 목록 위 "역할 추가"로 오른쪽 칸에 빈 폼을 연다. 저장 전 구성원 칸에는 "저장하면 구성원을 더할 수 있어요"를 보인다
  - 이름: `RoleScreens`의 연결 화면 이름, `RolePermissions`의 권한 이름을 "권한 관리"로 바꾼다(권한 키 `ROLE_MANAGE`, 경로 `/admin/roles`는 그대로). `messages/ko.json` `role.title`·`pageDescription`(FR-009 문구)·`guide`, `menuAdmin.guide`와 `userAdmin.description`의 링크 이름을 바꾼다. 쓰지 않는 `menu.roles`도 같이 바꾼다
- **Rationale**: 드로어는 목록을 가리고, 탭은 쓸 메뉴와 구성원을 함께 보지 못하게 한다. 메뉴 관리·코드 관리와 같은 부품을 쓰면 동작(저장하지 않은 변경 묻기, 좁은 폭 드로어)이 같다
- **Alternatives considered**: 오른쪽 칸 안을 다시 탭으로 나누기(스펙 FR-005가 탭을 두지 않는다), 왼쪽과 오른쪽을 반씩(목록에 이름·구성원 수뿐이라 넓을 필요가 없다)
- **키트로 옮길 것**: 권한 관리 두 칸 화면, 최고 관리자 행 표시

## R20. 마이그레이션 V7 (FR-003·FR-010·FR-011, 2026-11-21)

- **Decision**: `V7__super_admin_role.sql` 하나에 둔다 (`docs/db.md` "마이그레이션", 지금 마지막은 V6)
  1. `tb_access_role`에 `SUPER_ADMIN`·"최고 관리자"·설명("모든 관리 메뉴와 기능을 써요")을 넣는다. 만든 사람은 시스템 계정(1)
  2. 업무 역할을 가진 `USER`(`tb_user_access_role`에 행이 있는 사용자)를 `ADMIN`으로 바꾼다
  3. 지금 `ADMIN`이던 계정(2에서 바뀐 계정은 빼고)을 최고 관리자 역할의 구성원으로 넣는다
  4. 메뉴 `ADMIN_ROLES`의 이름이 "역할 관리"면 "권한 관리"로 바꾼다(V3와 같은 방식, 관리자가 바꾼 이름은 그대로)
  - local 시험 계정: `db/localdata/R__local_accounts.sql`은 버전 마이그레이션 뒤에 돌아 V7이 `admin`을 보지 못한다. 그래서 이 파일에 `admin`(id 2)을 최고 관리자 구성원으로 넣는 줄을 더한다(`INSERT IGNORE`, 역할은 코드로 찾는다)
  - 첫 관리자 부트스트랩(`UserService.bootstrapAdmin`)은 계정을 만든 뒤 최고 관리자 역할에 넣는다. 건너뛰는 조건 "`ADMIN`이 있으면"을 "최고 관리자 역할에 구성원이 있으면"으로 바꾼다
- **Rationale**: 운영·dev DB에 이미 있는 운영자(`USER` + 역할)와 관리자가 배포 직후에도 지금처럼 쓴다
- **Alternatives considered**: 2·3을 앱 시작 때 코드로 옮기기(한 번만 돌아야 하는 데이터 변경이라 Flyway가 맞다)
- **같이 볼 것**: 마이그레이션을 바꾸므로 `bootRun`으로 한 번 띄워 Flyway 적용과 `validate`를 본다(`docs/db.md`)

## R21. 무엇을 테스트하나 (US1-AC4, US2, US4, US5, 2026-11-21)

- **Decision**:
  - 테스트 지원: `DatabaseCleaner`가 테스트마다 모든 표를 비우므로 V7의 역할 행도 사라진다. `AcceptanceTest`에 `superAdmin()`(최고 관리자 역할이 없으면 만들고, `ADMIN` 계정을 만들어 구성원으로 넣고 토큰을 준다)과 `operator(permissions...)`(그 권한을 묶은 역할과 `ADMIN` 계정)을 둔다. 지금 `bearer(2L, "ADMIN")`(DB에 없는 계정의 토큰)으로 모든 권한을 얻던 테스트는 이제 권한이 없으므로 `superAdmin()`으로 바꾼다
  - 키트 수용 테스트의 기대를 바꾼다: `PermissionAcceptanceTest`(ADMIN이 모든 권한 → 최고 관리자가 모든 권한, 역할 없는 ADMIN은 빈 권한), `RbacAcceptanceTest`(역할을 `USER`에게 주던 것 → `ADMIN` 계정, `USER`에게 주면 B001), `MenuAcceptanceTest.myMenusByPermission`, `AdminBootstrapAcceptanceTest`(만든 계정이 최고 관리자 구성원). 스펙이 바뀐 만큼만 고친다(원칙 V). 바꾼 테스트 목록을 PR 설명에 적는다
  - `AdminRolesAcceptanceTest`: US1-AC4(역할 없는 ADMIN → `/menus/me` 빈 목록, 관리 API 403), US2-AC1(부트스트랩 뒤 목록 맨 앞 `fixed`), US2-AC2(최고 관리자 역할 수정·삭제 B001), US2-AC3(테스트 연결 화면(`TestScreens`)의 권한도 최고 관리자 200), US2-AC4(마지막 구성원 빼기 B001), US2-AC5(권한 관리 운영자가 최고 관리자 구성원 더하기 A005), US4-AC4(최고 관리자 역할 bulk-delete 섞으면 아무것도 안 지워짐), US5-AC4(검색에 회원 없음, `superAdmin` 표시, 최고 관리자 계정을 다른 역할에 더하기 B001), 사용자 관리 권한이 든 역할을 운영자가 만들기 A005. 지금 `adminGetsNoAccessRole`·`addingAdminFailsWholly`(ADMIN이면 B001)는 USER면 B001로 뒤집는다(원칙 V)
  - e2e(`role.spec.ts`): 두 칸(왼쪽 목록, 오른쪽 안내), 행을 누르면 쓸 메뉴와 구성원이 한 칸에 함께(탭 없음), 최고 관리자 행 맨 위·체크 꺼짐·트리 모두 체크 꺼짐, 좁은 폭 드로어, 머리 안내 문구, 메뉴명 "권한 관리". 지금 탭·드로어를 찾던 기대는 두 칸에 맞춘다(원칙 V)

## R22. 마이그레이션 V8: 한 명으로 줄이고 DB가 지킨다 (FR-010, Clarifications 2026-11-22)

- **Decision**: `V8__single_super_admin.sql` 하나에 둔다 (`docs/db.md` "마이그레이션", 지금 마지막은 V7)
  1. 이 DB의 최고 관리자 역할 ID를 읽는다: `SET @super := (SELECT id FROM tb_access_role WHERE code = 'SUPER_ADMIN' AND deleted_at IS NULL)`
  2. 최고 관리자 줄이 둘 이상이면 `user_id`가 가장 작은 줄만 남기고 지운다. 같은 표를 하위 쿼리로 읽으므로 MariaDB 제약을 피해 파생 표(`SELECT m FROM (SELECT MIN(user_id) m ...) t`)로 감싼다. 지운 계정은 역할 없는 관리자 계정이 된다 (최고 관리자는 다른 역할을 함께 갖지 않았다)
  3. `tb_user_access_role`에 생성 열 `super_slot TINYINT AS (IF(access_role_id = <최고 관리자 역할 ID>, 1, NULL)) VIRTUAL COMMENT '최고 관리자 자리 (1: 최고 관리자 줄, NULL: 그 밖)'`과 유일 제약 `uk_user_access_role_super_slot (super_slot)`을 더한다
  - 생성 열 식은 다른 표를 볼 수 없어 역할 ID를 숫자로 박는다. DB마다 ID가 다를 수 있으므로 1에서 읽은 값으로 `ALTER` 문을 만들어 `PREPARE`·`EXECUTE`한다. 최고 관리자 역할은 지우거나 다시 만들 수 없어(FR-010) 박은 숫자가 바뀌지 않는다
  - 역할 행이 없는 DB(있을 수 없지만)는 `@super`가 NULL이라 식이 늘 NULL이 된다. 그런 DB는 마이그레이션을 실패시켜 드러낸다(`SIGNAL`)
  - 엔터티(`UserAccessRole`)에는 이 열을 매핑하지 않는다. `ddl-auto: validate`는 매핑한 열만 본다 (`active_key`와 같다)
- **Rationale**: 앱 검사만으로는 DB 절차(`002-admin-users` FR-009)를 잘못 실행했을 때 둘이 될 수 있다. 생성 열 + 유일 제약은 moa가 `active_key`로 이미 쓰는 방식이고, NULL은 유일 제약에서 여러 개 허용된다
- **Alternatives considered**: 트리거(moa는 DB 로직을 두지 않는다. FK도 두지 않는다), 최고 관리자를 따로 담는 한 행짜리 표(역할 모델과 두 곳이 된다), 서버 시작 때 수 세기만(들어가는 것은 못 막는다)
- **같이 볼 것**: `bootRun`으로 한 번 띄워 V8 적용과 `validate`를 본다 (`docs/db.md`)

## R23. 최고 관리자 역할의 구성원을 바꾸지 못하게 (FR-006·FR-007·FR-010, US2-AC4·AC5)

- **Decision**: R17의 규칙을 바꾼다
  - `AccessRoleService.addMembers`·`removeMembers`: 대상 역할이 최고 관리자 역할이면 누구든 409 `B001`("최고 관리자는 한 명으로 고정이에요. 바꾸려면 개발자에게 요청해요."). 지금의 "최고 관리자에 더하면 다른 역할 빼기"와 "마지막 구성원 검사"(`requireSuperAdminRemains`)는 없앤다
  - `replaceRolesOfUser`: 바뀌는 역할(더해지거나 빠지는 것)에 최고 관리자 역할이 있으면 B001. 최고 관리자 계정의 역할 목록 자체를 바꾸려 해도 B001(최고 관리자는 다른 역할을 받지 않는다)
  - `RoleGrantPolicy.blockReason`: 최고 관리자 역할 행은 누구에게나 `FIXED`(새 값)를 준다. `grantable`은 늘 false. 최고 관리자도 이 역할을 주거나 뺄 수 없기 때문이다. 사용자 관리 권한이 든 역할은 지금처럼 `SUPER_ADMIN_ONLY`
  - `addSuperAdmin`(첫 관리자 부트스트랩)은 그대로 둔다. 화면·API가 아닌 서버 안의 길이고, 최고 관리자가 없을 때만 돈다
  - DB 유일 제약(R22)에 걸리는 일은 앱 검사가 먼저 막아 생기지 않는다. 생기면 500이 아니라 409 `B001`로 바꿔 알린다(`DataIntegrityViolationException`을 그 자리에서 잡는다)
- **Rationale**: 스펙 2026-11-21 한 명 고정. 넘기는 길을 화면과 API에서 모두 없앤다

## R24. 화면: 최고 관리자 구성원은 보기만 (FR-006, US2-AC4)

- **Decision**:
  - `role-members.tsx`: `role.fixed`면 검색 칸·체크박스·일괄 줄을 그리지 않고 구성원 한 명과 안내("최고 관리자는 한 명으로 고정이에요. 바꾸려면 개발자에게 요청해요.")만 보인다
  - `role-panel.tsx`의 막힌 이유 문구에 `FIXED`를 더한다. 최고 관리자 역할은 지금도 "고정 역할" 안내(`fixedHint`)를 보이므로 그 문구를 "구성원도 바꿀 수 없다"로 고친다
  - 타입 `GrantBlock`에 `FIXED`를 더한다 (`types/role.ts`)
- **Rationale**: 보이는 단추가 있는데 누르면 막히는 것보다 처음부터 없는 편이 헷갈리지 않는다

## R25. 무엇을 테스트하나 (US2-AC4·AC5, FR-010, SC-006, 2026-11-22)

- **Decision**:
  - 테스트 지원: `TestUsers.superAdminRole()`은 역할을 다시 만들 때 **마이그레이션 직후의 최고 관리자 역할 ID를 지정해** 넣는다(`MenuSeed`처럼 처음 한 번 읽어 둔다). V8의 생성 열이 그 ID를 박았으므로, 다른 ID로 다시 만들면 엉뚱한 역할이 한 명만 받게 된다
  - `AdminRolesAcceptanceTest`: US2-AC4(최고 관리자 역할 구성원 더하기·빼기 B001, 최고 관리자 자신도), US2-AC5(`PUT /users/{id}/roles`로 최고 관리자 역할을 주거나 최고 관리자에게서 빼기 B001, 목록의 최고 관리자 역할 `blockReason` = `FIXED`). 지금의 `lastSuperAdminStays`·`superAdminDropsOtherRoles`(FR-010 옛 규칙)는 새 규칙으로 바꾼다(원칙 V)
  - SC-006: JdbcTemplate으로 최고 관리자 줄을 하나 더 넣으면 DB가 거부한다(`DuplicateKeyException`)
  - e2e(`role.spec.ts`): 최고 관리자 역할을 고르면 구성원에 검색 칸·체크박스가 없고 안내가 보인다
