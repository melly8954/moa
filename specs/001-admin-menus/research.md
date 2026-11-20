# Research: 관리 메뉴와 접근 (admin-menus)

Technical Context에 미결은 없다. 아래는 코드를 확인해 정한 것이다.

## R1. 만들 것의 범위

- **Decision**: 운영 코드는 바꾸지 않는다. 키트 시드를 그대로 두고, 메뉴를 최상위로 옮기는 moa 마이그레이션 `V2__flat_admin_menus.sql` 하나와 AC 테스트를 더한다
- **Rationale**: 시드(`V1_7__menus.sql`, `V1_7_1__admin_menus.sql`)의 순서와 권한은 FR-001 표와 같고 위치("관리" 그룹)만 다르다. 메뉴 거르기(FR-002)와 접근 차단(FR-003)은 키트에 있다
- **Alternatives considered**:
  - moa 전용 시드를 따로 두기: 키트 시드와 같은 내용이라 두 곳이 어긋날 수 있어 버렸다
  - 키트 시드 `V1_7`·`V1_7_1`을 직접 고치기: 이미 적용된 DB에서 Flyway 체크섬이 깨지고 다음 키트 패치를 옮기기 어렵다
  - 키트 admin 프리셋을 먼저 바꾸기: 키트에서 따로 진행한다. 키트가 바뀌어도 이미 배포된 DB는 옮기는 마이그레이션이 있어야 한다

## R2. US1-AC1에서 처음 구성을 확인하는 방법

- **Decision**: 테스트 전에 `menus`를 비우고, 실제 마이그레이션 파일(`V1_7`, `V1_7_1`, `V2`)의 INSERT·UPDATE 문만 차례로 다시 실행한 뒤 내 메뉴를 조회한다
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

## R5. 셸 두 벌을 어떻게 만드나 (FR-005, 2026-11-15)

- **Decision**: 키트 `SidebarShell`이 메뉴와 로고 주소를 인자로 받게 하고, `AdminShell`(서버 메뉴 `/menus/me`만)과 `UserShell`(코드 메뉴 `menu.ts`: 홈, 공지)이 이를 감싼다. 경로 묶음은 `(admin)`, `(user)`, `(account)` 셋이다. 키트 `(main)` 묶음과 006의 `(site)` 묶음은 없앤다
- **Rationale**: 두 셸이 같은 모양(사이드바, 계정 줄, 서랍, 벨)이어야 하므로 셸 코드를 둘로 복사하지 않는다. 관리 메뉴는 이미 서버 메뉴로만 오고(001 R1) 코드 메뉴에는 홈·공지만 있어, 둘을 섞던 `useMenu`를 나누면 된다
- **Alternatives considered**: 한 셸에서 경로(`/admin`)를 보고 메뉴를 바꾸기. 계정 화면(R7)처럼 경로로 정할 수 없는 화면이 있어 셸을 부품으로 나누는 편이 단순하다. 키트 상단 메뉴형 셸 쓰기: 사용자가 사이드바로 맞추기로 했다

## R6. 로그인 뒤 첫 화면 (FR-006)

- **Decision**: 로그인이 끝나면 `?next=`가 있으면 그 주소, 없으면 `/menus/me`를 읽어 항목이 있으면 `/admin`, 없으면 `/`로 보낸다. `/admin`은 볼 수 있는 첫 관리 메뉴(위에서부터 처음 만나는 주소 있는 메뉴)로 보낸다. 관리 셸 로고도 `/admin`이다
- **Rationale**: 첫 관리 메뉴는 권한과 메뉴 관리 설정으로 바뀌므로 고정 주소로 두지 않는다. `/admin` 한 곳에서 정하면 로그인과 로고가 같은 규칙을 쓴다
- **해석**: "관리 메뉴가 있는 사용자"는 `/menus/me`가 하나 이상을 주는 사용자다. 메뉴 관리에서 메뉴를 모두 끄면 관리자도 사용자 셸로 간다. 관리 화면 주소로 직접 들어가는 것은 막지 않는다(권한은 API가 막는다)
- **Alternatives considered**: 시스템 역할(`ADMIN`)로 정하기. 관리 권한을 받은 운영자는 역할이 `USER`라 가려내지 못한다 (`specs/README.md` 맞춰야 할 경계)

## R7. 계정 화면은 어느 셸에 보이나 (FR-005)

- **Decision**: 알림·설정·비밀번호 변경은 `(account)` 묶음에 두고, 레이아웃이 `/menus/me`를 읽어 항목이 있으면 관리 셸, 없으면 사용자 셸로 감싼다. 읽는 동안은 키트 `AuthGate`의 대기 화면을 쓴다
- **Rationale**: 관리자가 계정 메뉴를 누를 때마다 사용자 셸로 넘어가지 않게 한다. 같은 주소를 두 벌(`/admin/settings`, `/settings`) 두지 않는다

## R8. 무엇을 테스트하나

- **Decision**: 셸 나누기 AC(US1-AC1의 첫 화면·사이드바, US2-AC1, US2-AC4)는 프론트 e2e로 본다(백엔드는 가로챈다). 관리 메뉴 구성(서버 메뉴)은 지금의 API AC 테스트가 그대로 지킨다
- **Rationale**: 바뀌는 것이 화면뿐이다. 서버 메뉴의 내용과 권한은 API 테스트(T003~T007)가 이미 고정한다
- **메뉴 대상 (2026-11-15)**: US3-AC1~AC4는 API 인수 테스트(`AdminMenusAcceptanceTest`)로 본다. 사용자 셸이 `/menus/user`를 그리는지와 메뉴 관리 탭은 e2e로 본다. 권한 칸이 빈 관리 메뉴의 보임이 바뀌므로(R10) 키트 `MenuAcceptanceTest` 중 그 동작을 보는 테스트는 스펙이 바뀐 만큼 고친다 (원칙 V)

## R9. 메뉴 대상을 어떻게 저장하나 (FR-007, 2026-11-15)

- **Decision**: `menus`에 `audience`(`ADMIN`·`USER`, 기본 `ADMIN`) 열을 moa 마이그레이션 `V2_4__menu_audience.sql`로 더한다. 기존 메뉴는 모두 `ADMIN`이 되고, 같은 파일이 사용자 메뉴 홈(`/`, 아이콘 `home`)·공지(`/notices`, 아이콘 `megaphone`)를 `USER`로 넣는다. 하위 메뉴는 부모의 대상을 따른다: 만들 때 부모가 있으면 부모 대상으로 정하고, 부모의 대상을 바꾸면 하위 메뉴도 함께 바꾼다
- **Rationale**: 대상은 메뉴마다 하나다. 별도 표나 그룹 메뉴("사용자" 묶음)로 나누면 2단계 깊이 제한과 "그룹 없이 최상위" 결정(FR-001)과 부딪힌다
- **Alternatives considered**: 사용자 메뉴를 "사용자" 그룹 아래에 두기. 깊이를 하나 써 버리고 관리 셸의 최상위 구성이 깨진다. 권한 키로 구분하기(`USER_MENU` 같은 가짜 권한): 권한의 뜻이 섞인다

## R10. 메뉴 API (FR-002, FR-007)

- **Decision**:
  - `GET /api/v1/menus/me`(키트): 대상 `ADMIN` 메뉴만 준다. 권한 칸이 빈 관리 메뉴는 `ADMIN`에게만 보인다(키트는 로그인한 모두). 관리 셸, 로그인 뒤 첫 화면(R6), 계정 화면 셸(R7)은 지금처럼 이 API를 쓴다
  - `GET /api/v1/menus/user`(새로, 로그인 없이 부른다): 대상 `USER` 메뉴 중 볼 수 있는 것. 권한 칸이 빈 메뉴는 모두, 권한을 건 메뉴는 그 권한을 가진 로그인 사용자에게만. 응답 모양은 `/menus/me`와 같다
  - 메뉴 관리 API: 응답(`MenuTreeResponse`)과 만들기·고치기 요청에 `audience`를 더한다. 하위 메뉴의 `audience`는 보내도 부모를 따른다
  - 순서 바꾸기는 지금처럼 형제끼리다. 최상위는 대상별로 따로 순서를 가진다
- **Rationale**: `/menus/me`의 뜻(관리 메뉴가 있나)을 바꾸지 않으면 셸 나누기(R6·R7) 코드가 그대로 맞다. 사용자 메뉴는 로그인하지 않은 사용자 셸(공지)에서도 보여야 해 로그인 없는 API가 따로 필요하다
- **Alternatives considered**: `/menus/me?audience=USER` 한 API로. 로그인 없이 불러야 하는 쪽과 로그인해야 하는 쪽이 한 경로에 섞여 보안 설정이 흐려진다

## R12. 머리 줄과 접기 단추 (FR-005, 2026-11-15)

- **Decision**: `SidebarShell`의 본문 머리 줄은 좁은 폭(서랍)에서만 그린다. 넓은 폭에서는 사이드바 머리(로고 줄)에 접기 단추를 둔다. 아이콘만 남게 접으면 로고 자리 하나가 펼치기 단추가 된다. 평소에는 로고 글자를 보이고, 마우스를 올리거나 키보드로 닿으면 펼치기 아이콘을 보인다. 이 단추는 첫 화면으로 가지 않고 펼치기만 한다. 좁은 폭 머리 줄은 서랍 단추, 사이트 이름(로고 주소로 가는 링크), 오른쪽 벨이나 로그인 단추다. 머리 줄의 화면 이름(`screenTitle`)은 쓰지 않는다
- **Rationale**: 넓은 폭 머리 줄의 메뉴 이름은 화면 제목(`PageHeader` h1)과 같은 말이라 줄 하나를 쓸 까닭이 없다. 접기 단추가 사이드바 안에 있으면 사이드바를 다루는 단추가 사이드바에 모인다. 접힌 상태에서 로고와 펼치기를 한 단추로 하면 아이콘 폭에 단추가 둘 쌓이지 않는다. 사이드바 가장자리(Rail)와 `Ctrl+B`는 그대로 둔다
- **Alternatives considered**: 머리 줄에 위치 표시(상위 메뉴 > 메뉴)를 두기. 관리 메뉴는 묶음 없이 최상위라(FR-001) 보일 것이 메뉴 이름 하나다. 접힌 상태에 로고와 펼치기 단추를 따로 두기: 아이콘 폭에 단추가 둘 쌓여 메뉴와 헷갈린다
- **키트로 옮길 것**: `SidebarShell` 머리 줄과 로고 줄 접기 단추

## R11. 메뉴 관리 화면과 사용자 셸 (FR-007)

- **Decision**: 메뉴 관리 화면 위에 대상 탭(관리자 메뉴 / 사용자 메뉴)을 두고 탭마다 그 대상의 트리와 순서를 보인다. 만들기 대화상자는 지금 탭의 대상으로 만들고, 하위 메뉴는 부모 대상을 보여 주기만 한다. 사용자 셸(`useUserMenu`)은 코드 메뉴(`menu.ts`) 대신 `/menus/user`를 쓰고, 새 공지 점은 주소가 `/notices`인 항목에 붙인다(006 FR-010). `menu.ts`의 코드 메뉴 항목은 비운다
- **Rationale**: 한 표에 두 대상을 섞으면 순서 바꾸기가 대상 사이를 넘나든다. 탭이면 지금 화면 모양을 그대로 두 번 쓴다
- **키트로 옮길 것**: menu 헬퍼 백엔드(열, API 두 곳, 요청·응답)와 메뉴 관리 화면 탭, 셸의 사용자 메뉴

## R13. 프로그램을 어디에 어떻게 등록하나 (FR-008, 2026-11-15)

- **Decision**: 백엔드에 `ProgramSource` 빈을 두고, 빈마다 `ProgramDefinition(key, name, path, audience, permission, signInRequired)` 목록을 낸다. `ProgramRegistry`가 모은다. 권한의 `PermissionSource`·`PermissionRegistry`와 같은 모양이다
  - 키는 영문 대문자·숫자·`_`로 쓴다 (예: `ADMIN_USERS`, `NOTICES`)
  - 시작할 때 검사하고 어기면 시작이 실패한다: 키·경로가 겹치지 않는다, 관리 프로그램은 등록된 권한이 하나 있고 로그인 필요 여부가 없다, 사용자 프로그램은 권한이 없다
  - 프로그램은 그 화면을 가진 쪽이 낸다: menu(메뉴 관리), user-admin(사용자 관리), code(코드 관리), login-history(접속 이력), rbac(역할 관리), 공지(공지 관리, 공지), moa(홈)
- **Rationale**: 권한을 등록하는 방식과 같아 개발자가 따로 배울 것이 없다. 시작 검사로 오타와 등록 누락을 배포 전에 잡는다. 헬퍼마다 자기 프로그램을 내면, 키트에서 헬퍼를 빼도 프로그램 목록이 맞다
- **Alternatives considered**: 프로그램을 DB 시드(마이그레이션)로 두기. 권한은 코드에 있는데 프로그램만 DB에 있으면 시작 검사를 할 수 없다. 프론트 라우트에서 뽑기: 메뉴를 거르는 것이 백엔드라 백엔드가 알아야 한다
- **한계**: 프론트에 그 경로의 화면이 실제로 있는지는 백엔드가 모른다. e2e에서 프로그램 경로를 모두 열어 확인한다 (R17)

## R14. 메뉴가 프로그램과 공개 범위를 어떻게 갖나 (FR-007, FR-009)

- **Decision**: `menus`에 `program_key`(`VARCHAR(50) NULL`)와 `visibility`(`VARCHAR(10) NULL`, `PUBLIC`·`MEMBER`)를 더하고 `path`·`permission` 열을 지운다. moa 마이그레이션 `V2_5__menu_programs.sql`이 한다
  - 기존 메뉴는 경로로 프로그램을 찾아 `program_key`를 채운다. 사용자 메뉴는 `visibility` = `PUBLIC`이다
  - 경로가 어느 프로그램과도 맞지 않는 메뉴는 `program_key` = `MISSING`으로 두어 "없는 프로그램"으로 보이게 한다 (지금 시드에는 없다)
  - 규칙: `program_key`가 없으면 묶음 메뉴이고 1단계에만 둔다(하위 메뉴는 화면 메뉴다). 관리 메뉴는 `visibility`가 없고, 사용자 메뉴는 반드시 있다. 로그인이 필요한 프로그램이면 `MEMBER`만 된다. 프로그램의 대상과 메뉴의 대상이 같아야 한다. 하위 메뉴는 부모 대상을 따르므로(R9), 부모의 대상을 바꿀 때 하위 메뉴 중 대상이 다른 프로그램이 있으면 막는다
- **Rationale**: 경로와 권한을 메뉴에 복사해 두면 프로그램이 바뀔 때 두 곳이 어긋난다. 키로 가리키면 코드가 언제나 기준이다
- **Alternatives considered**: `path`·`permission` 열을 남기고 프로그램 값을 복사해 두기. 두 곳이 어긋날 수 있다. 공개 범위를 권한 키처럼 쓰기(`MEMBER` 같은 가짜 권한): 권한의 뜻이 섞인다(R9와 같은 이유)

## R15. 메뉴 API (FR-002, FR-007~FR-009)

- **Decision**:
  - `GET /api/v1/menus/me`, `GET /api/v1/menus/user`: 응답 모양은 그대로이고 `path`는 프로그램의 경로다. 관리 메뉴는 프로그램의 권한을 가졌을 때, 사용자 메뉴는 `PUBLIC`이면 모두에게, `MEMBER`면 로그인한 사용자에게 보인다. 묶음 메뉴는 보이는 하위 메뉴가 있을 때만, 없는 프로그램을 가리키는 메뉴는 보이지 않는다
  - `GET /api/v1/admin/programs` (새로, 메뉴 관리 권한): 프로그램마다 `key`, `name`, `path`, `audience`, `permission`(`key`, `name`. 사용자 프로그램은 없음), `signInRequired`
  - `GET /api/v1/admin/menus` 응답: `path`·`permission` 대신 `programKey`, `program`(`name`, `path`, `permissionName`, `signInRequired`. 없는 프로그램이면 비고 `missingProgram` = true), `visibility`
  - `POST`·`PUT /api/v1/admin/menus` 요청: `path`·`permission` 대신 `programKey`(묶음 메뉴면 비운다), `visibility`(사용자 메뉴). 등록되지 않은 프로그램, 대상이 다른 프로그램, 로그인이 필요한 프로그램에 `PUBLIC`, 사용자 메뉴에 공개 범위가 없음, 하위 메뉴에 프로그램이 없음은 `V001`. 대상을 바꿀 때 하위 메뉴와 부딪히면 `B001`
- **Rationale**: 셸이 쓰는 두 API의 모양을 그대로 두면 셸 코드(R6·R7·R11)는 바뀌지 않는다. 프로그램 API가 권한 이름까지 주므로, 메뉴 관리 권한만 받은 운영자도 권한 키 없이 화면을 쓴다(FR-009)
- **Alternatives considered**: 권한 목록 API(`GET /api/v1/admin/permissions`, 역할 관리 권한)를 메뉴 관리 권한에도 열기. `@RequirePermission`은 키 하나만 받고, 프로그램 API가 권한 이름을 주면 따로 필요 없다

## R16. 메뉴 관리 화면 (FR-008, FR-009, SC-006)

- **Decision**:
  - 탭: 관리자 메뉴 / 사용자 메뉴 / 프로그램. 프로그램 탭은 조회 전용 표다(이름, 경로, 대상, 필요한 권한 이름 또는 로그인 필요)
  - 메뉴 목록의 "필요한 권한" 열을 "프로그램"(없는 프로그램이면 경고 표시)과 "보이는 사람"(관리 메뉴는 권한 이름, 사용자 메뉴는 비회원 포함·회원만, 묶음 메뉴는 "하위 메뉴를 따름")으로 바꾼다
  - 대화상자: 종류(화면 메뉴 / 묶음 메뉴. 하위 메뉴는 화면 메뉴만), 프로그램 선택 상자(지금 탭 대상의 프로그램만, 이름과 경로), 고른 뒤 경로와 필요한 권한을 읽기 전용 글로 보인다. 사용자 메뉴면 공개 범위를 고르고, 로그인이 필요한 프로그램이면 "비회원 포함"을 끄고 이유를 보인다. 공개 범위 옆에 "사이드바에 보일지만 정한다"는 설명을 둔다. 경로·권한 입력 칸은 없앤다
  - 문구는 frontend/messages/ko.json `menuAdmin`에 둔다
- **Rationale**: 관리자가 고르는 것만 화면에 남기고, 코드가 정하는 값은 보여 주기만 한다
- **키트로 옮길 것**: menu 헬퍼 백엔드(프로그램 등록, 열, API)와 메뉴 관리 화면, admin 프리셋 시드

## R17. 무엇을 테스트하나 (US3, US4)

- **Decision**:
  - API 인수 테스트 `AdminMenusAcceptanceTest`: US3-AC2(`PUBLIC`), US3-AC4(코드 관리 프로그램을 가리키는 관리 메뉴를 더해도 역할 없는 `USER`의 `/menus/me`는 빈 목록), US3-AC5(`MEMBER`), US4-AC1(`/admin/programs`), US4-AC2(대상이 다른 프로그램은 `V001`, 응답에 프로그램 경로·권한 이름), US4-AC3(로그인이 필요한 프로그램에 `PUBLIC`은 `V001`), US4-AC4(묶음 아래 코드 관리 메뉴, 코드 관리 권한만 받은 운영자의 `/menus/me`), US4-AC5(하위 메뉴 없는 묶음은 `/menus/me`에 없음)
  - 처음 등록하는 사용자 프로그램(홈, 공지)은 모두 로그인이 필요 없다. US3-AC5·US4-AC3은 테스트에서만 등록하는 로그인 필요 프로그램(테스트 설정의 `ProgramSource`)을 쓴다
  - 화면은 e2e(`menu.spec.ts`): 대화상자에 경로·권한 입력이 없고 프로그램을 고른다, 로그인 필요 프로그램이면 "비회원 포함"이 꺼진다, 프로그램 탭. `screens.spec.ts`가 처음 등록한 프로그램 경로를 모두 연다
  - 기존 US3-AC2(권한 없이 더한 사용자 메뉴)와 US3-AC4(권한 없이 더한 관리 메뉴), 키트 `MenuAcceptanceTest`의 요청 모양(`path`, `permission`)은 스펙이 바뀐 만큼만 고친다 (원칙 V)

## R18. 운영·설정 묶음을 어떻게 넣나 (FR-001, 2026-11-15 관리 화면 정리)

- **Decision**: moa 마이그레이션 `V2_6__admin_menu_groups.sql`이 관리 메뉴 묶음 "운영"(아이콘 `briefcase`, 순서 10)과 "설정"(`settings`, 20)을 넣고, 처음 구성의 관리 메뉴를 `program_key`로 찾아 그 아래로 옮긴다(운영: `ADMIN_USERS` 10, `ADMIN_NOTICES` 20, `ADMIN_LOGIN_HISTORIES` 30 / 설정: `ADMIN_ROLES` 10, `ADMIN_MENUS` 20, `ADMIN_CODES` 30). 같은 파일이 메뉴 이름 "코드 관리"를 "공통코드"로 바꾼다. 최상위가 아닌 메뉴(관리자가 이미 옮긴 것)는 건드리지 않는다
- 프로그램 `ADMIN_CODES`의 이름도 "공통코드"로 바꾼다 (`CodePrograms`)
- **Rationale**: 시드를 지우고 다시 넣으면 관리자가 바꾼 메뉴가 사라진다. 키로 찾아 옮기면 처음 구성만 바뀐다
- **영향**: 내 메뉴(`/menus/me`)가 1단계 묶음 2개와 하위 메뉴로 바뀐다. 로그인 뒤 첫 화면(R6 `landing.ts`)은 하위 메뉴까지 내려가 첫 화면 메뉴를 찾는다. 003 US1-AC1(운영자의 내 메뉴), 003 US2-AC1(권한 트리)의 기대도 묶음 모양으로 바뀐다 (원칙 V, 스펙이 바뀐 만큼)

## R19. 일괄 처리 API (FR-010)

- **Decision**:
  - 순서·부모 바꾸기(끌기, "묶음으로 옮기기")는 지금의 `PUT /api/v1/admin/menus/order`를 그대로 쓴다. 화면이 옮긴 결과의 순서 목록을 만들어 보낸다 (옮긴 메뉴는 대상 묶음의 맨 뒤)
  - `PUT /api/v1/admin/menus/enabled` (`ids`, `enabled`): 사용·사용 안 함 한꺼번에
  - `POST /api/v1/admin/menus/bulk-delete` (`ids`): 한꺼번에 지우기
  - 둘 다 한 트랜잭션이고 하나라도 틀리면(없는 메뉴 R001, 하위 메뉴가 있는 묶음 B001) 아무것도 바꾸지 않는다. 고른 메뉴끼리 부모·자식이면(묶음과 그 하위를 함께 지우기) 하위를 먼저 지운 것으로 보고 허용한다
- **Rationale**: 메뉴마다 요청을 보내면 중간에 실패할 때 일부만 바뀐다. 순서 바꾸기는 이미 일괄 API다
- **Alternatives considered**: `POST /menus/bulk` 하나에 동작 종류를 담기. 요청 모양이 동작마다 달라 검사가 흐려진다. `DELETE` 본문에 ID 목록: 본문 있는 DELETE를 막는 프록시가 있다

## R20. 끌어서 순서 바꾸기 (FR-010, FR-012)

- **Decision**: `@dnd-kit/core`·`@dnd-kit/sortable`을 들인다. 키보드 센서(스페이스로 집고 화살표로 옮기고 스페이스로 놓기)와 화면 낭독기 안내를 기본으로 쓴다. 손잡이는 버튼(`aria-label` "{이름} 옮기기")이다
  - 묶음 메뉴는 1단계에서만, 화면 메뉴는 1단계·묶음 안 어디든 놓을 수 있다. 규칙에 어긋나는 자리(묶음을 묶음 안에)는 놓을 곳으로 표시하지 않는다
  - 놓으면 바로 `PUT /menus/order`를 보내고, 실패하면 원래 자리로 되돌리고 이유를 보인다
  - 좁은 폭에서는 끌기 대신 행의 "옮기기" 메뉴(위로·아래로·묶음 고르기)를 보인다 (FR-012)
- **Rationale**: 직접 만들면 키보드·낭독기 지원을 다시 짜야 한다. dnd-kit은 React용으로 널리 쓰이고 접근성 지원이 있다
- **Alternatives considered**: 체크한 행에만 위·아래 단추. 묶음 사이 이동이 여전히 번거롭다

## R21. 공용 드로어와 머리 안내 (FR-011, FR-012)

- **Decision**:
  - `components/kit/side-drawer.tsx`: UI `sheet`(오른쪽)를 감싼 관리 화면 드로어. 넓은 폭은 오른쪽 폭 `--drawer-width`(32rem), 좁은 폭은 화면 전체. 제목·탭 자리·하단 단추 줄을 둔다. 저장하지 않은 변경이 있으면 닫기 전에 묻는다 (키트 `use-unsaved-changes`)
  - 머리 안내: `PageHeader`의 `description`을 문구 + 링크로 쓴다 (next-intl `t.rich`의 `<link>` 태그 → `Link`). 문구는 각 화면 메시지에 둔다
  - 메뉴·역할·공통코드·사용자 관리가 함께 쓴다 (002·003·004)
- **키트로 옮길 것**: side-drawer, 머리 안내 방식

## R22. 메뉴 표 (FR-010, US5)

- **Decision**: 쪽 나눔이 없는 트리 표를 메뉴 관리 화면 안에 둔다(키트 `DataTable`은 페이지 응답 전용이라 쓰지 않는다). 열: 체크박스, 손잡이, 이름(누르면 드로어), 보이는 사람, 사용. 체크하면 표 위에 일괄 처리 줄(사용·사용 안 함·묶음으로 옮기기·삭제)
  - 메뉴 드로어: 이름, 아이콘, 종류(화면/묶음), 프로그램(선택 상자, 고르면 경로·권한 이름), 공개 범위(사용자 메뉴), 사용. 지금의 `menu-dialog.tsx`를 드로어로 옮긴다. 상위 메뉴 칸은 없앤다(끌기·옮기기로 정한다). 새 메뉴는 "메뉴 추가"로 열고, 어느 묶음 아래 둘지만 고른다
  - 프로그램 탭은 없앤다(C). `GET /admin/programs`는 선택 상자가 계속 쓴다
- **Rationale**: 메뉴는 구성 목록이라(원칙) 전체를 보이고 끌어야 한다

## R23. 무엇을 테스트하나 (US1, US5)

- **Decision**:
  - API 인수 테스트(`AdminMenusAcceptanceTest`): US1-AC1(내 메뉴가 운영·설정 묶음과 하위 순서), US5-AC4(`PUT /menus/enabled` 둘 끄기 → 내 메뉴에서 빠짐, 묶음으로 옮기기는 `/order`), US5-AC5(하위 있는 묶음 + 다른 메뉴 `bulk-delete` → B001, 아무것도 안 지워짐). US5-AC3의 서버 쪽(다른 묶음으로 `/order`)
  - 키트 `MenuAcceptanceTest`에 일괄 API 규칙(없는 ID R001, 부모·자식 함께 지우기 허용)
  - e2e(`menu.spec.ts`): US5-AC1(행 단추 없음, 트리), US5-AC2(드로어), US5-AC3(키보드로 옮기기 → `/order` 요청), US5-AC4·AC5(일괄 줄), 프로그램 탭 없음, 머리 안내 링크, 좁은 폭 드로어가 화면 전체
  - 처음 구성이 바뀌므로 `AdminRolesAcceptanceTest` US1-AC1·US2-AC1, e2e `SIX_ADMIN_MENUS`를 묶음 모양으로 고친다 (원칙 V)

## R24. 사이드바 묶음 펴기·접기 (FR-005, US1-AC4~AC8)

- **Decision**: `components/shell/sidebar-shell.tsx`에서 묶음 메뉴(`children`이 있는 메뉴)의 하위 목록을 펴고 접는다
  - 상태: `localStorage` 키 `moa:sidebar-open-menus`에 `{ 메뉴key: boolean }`. 메뉴 key는 서버 메뉴 id(`menu-<id>`)라 이름을 바꿔도 유지된다
  - 펴짐 = `state[key] ?? isActive(묶음, pathname)`. 직접 펴거나 접은 값이 자동 펴기보다 먼저다
  - 자동으로 펴진 묶음 기억: `pathname`이 바뀔 때(effect) 지금 화면이 있는 묶음에 저장값이 없으면 `true`를 저장소에 쓰고 구독자에게 알린다. React 상태를 effect에서 바꾸지 않으므로 lint에 걸리지 않는다. 다른 묶음 화면으로 옮겨도 지나온 묶음이 접히지 않는다 (US1-AC8)
  - 읽기는 `useSyncExternalStore(subscribe, read, () => "{}")`. 서버 렌더는 저장값 없이(자동 펴기만) 그리고 브라우저에서 맞춘다. effect 안 setState는 lint에 걸려 쓰지 않는다. 읽기·쓰기는 try/catch로 감싸 저장소를 못 쓰면 기억만 하지 않는다. 같은 탭 구독자에게는 리스너 집합으로, 다른 탭은 `storage` 이벤트로 알린다
  - 묶음 행(`SidebarMenuButton`, 링크 없음)이 토글이다: `aria-expanded`, `aria-controls`(하위 목록 id), 오른쪽 `ChevronRight`(펴지면 `rotate-90`). 접힌 하위 목록은 그리지 않는다(DOM에 없다)
  - 아이콘만 남게 접혔을 때(`state === "collapsed"`, 넓은 폭) 묶음을 누르면 `setOpen(true)`와 그 묶음 펴기를 함께 한다
  - 경계: `SidebarHeader`에 `border-b border-sidebar-border`, `SidebarFooter`에 `border-t border-sidebar-border`, 앱 이름 `font-semibold`. 하위 목록은 UI `SidebarMenuSub`의 왼쪽 선을 그대로 쓴다. 색은 테마 변수 클래스만
  - 새 문구 없음: 묶음 이름이 단추 이름이고 펴짐은 `aria-expanded`가 알린다
- **Rationale**: 묶음 행은 링크가 아니라 할 일이 없었다. 행 전체를 토글로 쓰면 화살표 단추를 따로 두지 않아도 되고 누를 자리가 넓다
- **Alternatives considered**: UI `Collapsible`로 감싸기(접힌 내용을 DOM에 남겨 상태 계산과 테스트 기대가 gongbu와 달라진다), 서버에 저장하기(기기마다 달라도 되는 화면 상태다)
- **테스트**: e2e(`menu.spec.ts`) US1-AC4~AC8. 하위 링크 수를 세던 US1-AC1·US1-AC3, `notifications.spec.ts`의 사이드바 링크 기대는 펴짐 규칙대로 고친다 (원칙 V). 접힌 하위 링크는 0개를 기대한다
- **키트로 옮길 것**: 묶음 펴기·접기와 경계선

## R25. 메뉴 관리 두 칸 화면과 쉬운 말 (FR-009·FR-010·FR-012, US5, 2026-11-20)

- **Decision**:
  - 화면 틀은 공통코드(`004-admin-codes` research R12)와 같다. 넓은 폭(`lg` 이상)은 두 칸 그리드(`lg:grid-cols-2`, `lg:grid-rows-[minmax(0,1fr)]`, `flex-1 basis-0 min-h-0`). 왼쪽은 대상 탭 + 일괄 처리 줄 + 트리(칸 안 스크롤), 오른쪽은 상세 패널. 좁은 폭은 트리만 보이고 상세를 `SideDrawer`로 연다
  - 공통코드의 `app/(admin)/admin/codes/detail-panel.tsx`(`DetailPanel`, `usePanelGuard`, `PanelGuardContext`)를 `components/kit/detail-panel.tsx`로 옮겨 두 화면이 함께 쓴다. 동작은 바꾸지 않는다
  - `menu-drawer.tsx`는 공통코드의 드로어처럼 `SideDrawer` 대신 `ResponsivePanel`(넓은 폭은 패널, 좁은 폭은 드로어)로 감싼다. 폼을 따로 떼지 않는다. 패널은 dirty를 `PanelGuardContext`로 알리고, 화면은 다른 행을 고르거나 대상 탭을 바꾸기 전에 `confirmLeave()`를 부른다
  - 폼 칸: 이름, 아이콘, 상위 메뉴, 연결 화면, 공개 범위(사용자 메뉴), 사용. "종류" 라디오를 없앤다
    - 상위 메뉴: `NativeSelect`. "없음(맨 위)" + 같은 대상의 맨 위 메뉴 중 연결 화면이 없는 것(자기 자신 빼고). 고치기에도 보인다(예전에는 새 메뉴에만 "넣을 곳"이 있었다). 하위 메뉴가 있는 메뉴는 "없음(맨 위)"만 고를 수 있고 안내를 붙인다
    - 연결 화면: 상위 메뉴를 고르면 필수. 맨 위면 "없음(상위 메뉴로 써요)"을 첫 항목으로 두고 비울 수 있다. 하위 메뉴가 있는 메뉴는 "없음"으로 고정하고 안내를 붙인다
    - 새 메뉴의 상위 메뉴 처음 값: 지금 고른 메뉴가 상위 메뉴면 그 메뉴, 하위 메뉴면 그 부모, 맨 위 화면 메뉴거나 고른 것이 없으면 "없음(맨 위)"
    - 상위 메뉴를 바꿔 저장하면 `PUT /api/v1/admin/menus/{id}`에 새 `parentId`와 새 부모의 맨 뒤 `sortOrder`를 보낸다. 서버의 2단계·대상 검사(`MenuService.modify`의 `checkParent`·`checkProgram`)를 그대로 쓴다. API는 바꾸지 않는다
  - 일괄 처리 줄(`BulkToolbar`)은 늘 그린다(공통코드와 같은 고정 높이). 단추: 사용, 사용 안 함, 상위 메뉴로 옮기기, 삭제
  - 트리 행은 체크박스·손잡이·이름·보이는 사람·사용이다. 반 폭에 맞게 예전의 프로그램 열은 뺀다(상세 칸에 보인다). "없는 화면"은 이름 옆에 보인다. 패널에 열린 행은 배경과 `aria-current`로 강조한다. 행에 아이콘으로 상위 메뉴(폴더)와 화면 메뉴(문서)를 구분한다
  - 일괄 처리 안내 문구는 넓은 폭에서 단추 넷과 한 줄에 들어가게 짧게 둔다("체크해서 한꺼번에 처리해요."). 줄 높이가 고르기 전후에 같아야 한다
  - 문구(`messages/ko.json` `menuAdmin`): "묶음"이 든 문구를 "상위 메뉴"로, "프로그램"을 "연결 화면"으로 바꾼다. "없는 프로그램"은 "없는 화면"으로. 머리 안내는 FR-011 문구대로. 역할 관리(`role`)의 `unlistedPrograms`("메뉴에 없는 프로그램")는 "메뉴에 없는 화면"으로, `permissionTreeHint`의 "묶음 메뉴"는 "상위 메뉴"로 바꾼다(`003-admin-roles` US2-AC3)
- **Rationale**: 드로어는 트리를 가리고 메뉴마다 열고 닫아야 한다. 연결 화면이 있고 없음이 곧 종류라 "종류" 칸은 같은 것을 두 번 묻는다. 공통코드와 같은 틀이면 관리자가 한 번 익혀 두 화면을 쓴다
- **Alternatives considered**: 추가 단추를 "상위 메뉴 추가"·"하위 메뉴 추가"로 나누기(맨 위 화면 메뉴인 홈·공지를 부를 말이 없다), 패널 부품을 메뉴 화면에 복사하기(같은 동작을 두 벌 고쳐야 한다), 상위 메뉴를 끌기로만 바꾸기(R22. 폼에서 자리를 볼 수 없어 처음 쓰는 관리자가 헷갈린다)
- **키트로 옮길 것**: `detail-panel.tsx`, 메뉴 관리 두 칸 화면

## R26. 무엇을 테스트하나 (US5, 2026-11-20)

- **Decision**: 화면만 바뀌어 e2e(`menu.spec.ts`)로 본다. API 인수 테스트는 그대로다
  - US5-AC1: 두 칸, 오른쪽 고르라는 안내, 행 단추 없음
  - US5-AC2: 이름을 누르면 오른쪽 칸에 상위 메뉴·연결 화면 칸, "종류" 없음, 열린 행 강조, 저장하면 트리에 반영
  - US5-AC3~AC5: 지금 테스트를 두 칸 화면에 맞춘다(원칙 V). 일괄 처리 줄이 고르기 전후에 같은 자리
  - US5-AC6: 메뉴 추가 → 맨 위 + 연결 화면 비움 → 상위 메뉴 생성. "운영"을 고르면 연결 화면 필수 안내
  - US5-AC7: 이름을 고치고 다른 행을 누르면 묻고, 그만두면 고치던 값이 남는다
  - US5-AC8: 좁은 폭에서 드로어로 열린다
  - "묶음"·"프로그램" 문구를 찾던 기대(`menu.spec.ts`, 역할 관리 `role.spec.ts`)를 새 문구로 고친다(원칙 V)
