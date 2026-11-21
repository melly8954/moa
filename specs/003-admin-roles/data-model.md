# Data Model: 운영자 역할 (admin-roles)

> 2026-11-21: 지금 스키마는 `V1__schema.sql`, 처음 데이터는 `V2__seed.sql`에 합쳐져 있다 (`docs/db.md`). 아래 마이그레이션 파일 이름은 합치기 전 기록이다.

새 테이블과 컬럼은 없다. 키트 `tb_access_role`, `tb_user_access_role`(`V1_8__access_roles.sql`)가 기준이다.

## 스펙의 개념과 키트의 대응

| 스펙 | 키트 | 비고 |
|---|---|---|
| 역할 | `tb_access_role` (코드, 이름, 설명, 권한 목록) | 소프트 삭제. 처음에는 없다 |
| 운영자에게 역할 주기 | `tb_user_access_role` | 한 사용자가 여러 역할을 받는다 |
| 운영자가 쓸 수 있는 범위 | 받은 역할들의 권한 합집합 | 요청마다 조회한다 |

## 권한 키

역할에 묶는 권한은 `001-admin-menus` FR-008 표의 관리 연결 화면 권한이다 (키는 [`001` data-model.md](../001-admin-menus/data-model.md) 연결 화면 표).

## 메뉴 트리와 사용자 화면 (2026-11-15)

새 테이블과 컬럼은 없다. 화면이 보이는 것은 지금 표에서 읽는다.

| 스펙 | 읽는 곳 | 비고 |
|---|---|---|
| 역할 창의 메뉴 트리 (FR-004) | `tb_menu`(대상 `ADMIN`, 꺼진 것 포함)와 연결 화면의 권한 | 메뉴 id가 아니라 권한 키를 `tb_access_role`에 담는다 |
| 메뉴에 없는 연결 화면 | 관리 연결 화면 중 어느 메뉴의 `screen_key`도 아닌 것 | |
| 사용자별 역할 기본 목록 (FR-005) | `tb_user_access_role`가 있는 사용자 + `tb_user.role` = `ADMIN` | 시스템 계정은 뺀다. 지운 역할은 빠진다 |
| 구성원 (FR-006) | 그 역할의 `tb_user_access_role` | |

규칙: `ADMIN`에게는 업무 역할을 주거나 빼지 않는다(`B001`). 이미 가진 역할을 다시 더하면 그대로 둔다. (2026-11-21에 뒤집었다: `USER`에게 주면 `B001`, 아래)

## 검증 규칙 (키트)

- 코드: 영문 대문자로 시작하고 영문 대문자·숫자·`_` 50자 이하, 겹치면 안 된다. 만든 뒤 바꾸지 않는다
- 이름: 필수, 50자 이하
- 설명: 200자 이하
- 권한: 등록된 키만, 관리자가 아니면 자기가 가진 권한까지만

## 관리자 계정과 최고 관리자 역할 (2026-11-21)

새 테이블과 컬럼은 없다. `V7__super_admin_role.sql`이 행을 넣고 옮긴다 (research R20).

| 스펙 | 데이터 | 비고 |
|---|---|---|
| 최고 관리자 역할 (FR-010) | `tb_access_role` 행 하나, 코드 `SUPER_ADMIN`, 이름 "최고 관리자" | `tb_access_role_permission` 행은 없다. 모든 권한은 코드(`PermissionGuard`)가 준다. 코드 상수로 가리고 지우지 않는다 |
| 최고 관리자 | `tb_user_access_role`에 그 역할 행이 있는 `ADMIN` 계정 | 늘 한 명 이상. 다른 역할 행은 없다 |
| 운영자 | 그 밖의 `ADMIN` 계정과 그 `tb_user_access_role` | 역할이 없을 수 있다(권한 없음) |
| 회원 | `tb_user.role` = `USER` | `tb_user_access_role` 행이 없다 |

옮기기 (V7): 역할을 가진 `USER`는 `ADMIN`으로, 그 전부터 `ADMIN`이던 계정은 최고 관리자 구성원으로. 메뉴 `ADMIN_ROLES`의 이름이 "역할 관리"면 "권한 관리"로.

## 최고 관리자 한 명 고정 (2026-11-22)

`V8__single_super_admin.sql` (research R22).

| 열 | 타입 | 뜻 |
|---|---|---|
| `tb_user_access_role.super_slot` | `TINYINT` 생성 열(VIRTUAL) | 최고 관리자 역할 줄이면 1, 그 밖은 NULL. 식에 최고 관리자 역할 ID를 숫자로 박는다 |
| `uk_user_access_role_super_slot` | 유일 제약 | `super_slot`이 1인 줄은 표 전체에서 하나. NULL은 여러 개 허용 |

- 이미 최고 관리자가 여럿이면 제약을 걸기 전에 `user_id`가 가장 작은 줄만 남긴다
- 엔터티에는 매핑하지 않는다 (`active_key`와 같다)
