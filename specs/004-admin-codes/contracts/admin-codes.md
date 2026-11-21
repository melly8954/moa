# Contract: 공통코드

처음에는 새 API가 없었다(아래 "관리 화면 정리"에서 더했다). 키트 API를 moa의 AC에 맞춰 어떻게 확인하는지만 적는다. 요청·응답 전체는 local 프로필의 `/v3/api-docs`가 기준이다.

## 코드 관리 목록

`GET /api/v1/admin/code-groups`

| AC | 호출자 | 기대 |
|---|---|---|
| US1-AC1 | ADMIN | 200. 그룹 코드 목록이 spec.md FR-001 표와 같다. 지금은 빈 목록 |

## 관리 화면 정리 (2026-11-15, research R5~R8)

> 2026-11-18에 아래 "그룹 관리"로 바뀌었다. 그룹 만들기 API는 다시 둔다. 이 표는 그 전 기록이다

그룹은 코드로 등록한다. 그룹 만들기 API(`POST /api/v1/admin/code-groups`)는 없앤다.

| AC | 호출 | 기대 |
|---|---|---|
| US1-AC1 | `GET /api/v1/admin/code-groups/tree` (시험 그룹 둘) | 200. 그룹마다 `groupCode`, `name`, `description`, `usedBy`, 항목. `POST /api/v1/admin/code-groups`는 405 |
| US1-AC2 | `POST /api/v1/admin/code-groups/{g}/codes` (`name`만) | 201, `code` = `001`. 지운 뒤 다시 만들면 `002` |
| US1-AC3 | `PUT /api/v1/admin/code-groups/{g}/codes/{code}` (`name`, `enabled`) | 200. 코드는 그대로 |
| US1-AC4 | `PUT /api/v1/admin/code-groups/{g}/codes/enabled` (`codes` 둘, `enabled` false) | 204. `GET /api/v1/codes/{g}`에서 빠짐 |
| US1-AC5 | `PUT /api/v1/admin/code-groups/{g}/codes/order` | 204. 화면용 목록이 그 순서. 다른 그룹의 코드가 섞이면 404 `R001`, 아무것도 안 바뀜 |
| US1-AC6 | 등록 그룹 없음 `GET .../tree` | 200, 빈 목록. 테스트에는 시험 그룹이 언제나 등록되어 `CodeGroupRegistryTest`(단위)로 본다 |

## 그룹 관리 (2026-11-18, research R9·R10)

> 2026-11-20에 아래 "사용 여부로 끄기"로 바뀌었다. 그룹 지우기와 `system`은 없어졌다. 이 표는 그 전 기록이다

그룹은 `tb_code_group`에 두고 관리자가 다룬다. 시스템 그룹(`system` = true)은 마이그레이션으로만 생긴다.
테스트는 시스템 그룹 하나(JDBC)와 일반 그룹 하나(API)로 시작한다.

| AC | 호출 | 기대 |
|---|---|---|
| US1-AC1 | `GET /api/v1/admin/code-groups/tree` | 200. 그룹마다 `groupCode`, `name`, `description`, `usedBy`, `system`, 항목. 만든 순서 |
| US1-AC2~AC5 | 위 표와 같다 | 위 표와 같다 |
| US1-AC6 | 그룹 없음 `GET .../tree` | 200, 빈 목록 |
| US1-AC7 | `POST /api/v1/admin/code-groups` (`groupCode`, `name`, `description`, `usedBy`) | 201, `system` = false. 지우지 않은 그룹(시스템 그룹 포함)과 겹치면 409 `R002`. 형식이 틀리면 400 `V001` |
| US1-AC8 | `PUT /api/v1/admin/code-groups/{g}` (`name`, `description`, `usedBy`) | 200. `groupCode`·`system`은 그대로. 시스템 그룹도 200 |
| US1-AC9 | `DELETE /api/v1/admin/code-groups/{g}` (일반 그룹, 항목 둘) | 204. 트리에서 빠지고 `GET /api/v1/codes/{g}`는 404 `R001` |
| US1-AC10 | `DELETE /api/v1/admin/code-groups/{g}` (시스템 그룹) | 409 `B001`. 그룹과 항목이 그대로 |

## 사용 여부로 끄기 (2026-11-20, research R11·R13)

그룹과 항목은 지우지 않는다. 지우는 API(그룹 지우기, 항목 지우기, 항목 일괄 지우기)는 없다.
테스트는 그룹 둘을 API로 만들고 시작한다.

| AC | 호출 | 기대 |
|---|---|---|
| US1-AC1 | `GET /api/v1/admin/code-groups/tree` | 200. 그룹마다 `groupCode`, `name`, `description`, `enabled`, 항목. 만든 순서. `usedBy`·`system`은 없다 |
| US1-AC2·AC3·AC5 | 위 "관리 화면 정리" 표와 같다 | 같다 |
| US1-AC4 | `PUT /api/v1/admin/code-groups/{g}/codes/enabled` (`codes` 둘, `enabled` false) | 204. `GET /api/v1/codes/{g}`에서 빠짐. 없는 코드가 섞이면 404 `R001`, 아무것도 안 바뀜 |
| US1-AC6 | 그룹 없음 `GET .../tree` | 200, 빈 목록 |
| US1-AC7 | `POST /api/v1/admin/code-groups` (`groupCode`, `name`, `description`) | 201, `enabled` = true. 겹치면 409 `R002`. 형식이 틀리면 400 `V001` |
| US1-AC8 | `PUT /api/v1/admin/code-groups/{g}` (`name`, `description`, `enabled`) | 200. `groupCode`는 그대로. `DELETE /api/v1/admin/code-groups/{g}`·`DELETE .../codes/{code}`는 405 |
| US1-AC9 | `PUT /api/v1/admin/code-groups/{g}` (`enabled` false) | 200. `GET /api/v1/codes/{g}`는 200 빈 목록, `?includeDisabled=true`면 항목이 모두 온다. 다시 켜면 켜진 항목이 온다 |

## 메뉴 관리와 같은 조작 (2026-11-21)

그룹 행 체크는 그룹 자체를 고르고, 일괄 사용 여부는 그룹과 항목을 섞어 여러 그룹에 걸쳐 바꾼다. 그룹도 끌어서 순서를 바꾼다.

| AC | 요청 | 기대 |
|---|---|---|
| US1-AC1 | `GET /api/v1/admin/code-groups/tree` | 200. 그룹은 순서(`sort_order`)대로, 같으면 만든 순서 |
| US1-AC4 | `PUT /api/v1/admin/code-groups/enabled` (`groupCodes`: 한 그룹, `codes`: 다른 그룹의 `{groupCode, code}` 하나, `enabled` false) | 204. 그 그룹의 `GET /api/v1/codes/{g}`는 빈 목록, 그 항목은 자기 그룹의 화면용 목록에서 빠짐. 없는 그룹·코드가 섞이면 404 `R001`, 아무것도 안 바뀜. `groupCodes`·`codes`가 둘 다 비면 400 `V001` |
| US1-AC5 | `PUT /api/v1/admin/code-groups/order` (`[{groupCode, sortOrder}]`) | 204. 트리가 그 순서. 없는 그룹이 섞이면 404 `R001`, 아무것도 안 바뀜 |
| US1-AC7 | `POST /api/v1/admin/code-groups` | 201. 트리 맨 뒤 |

- 한 그룹 안 항목만 바꾸던 `PUT /api/v1/admin/code-groups/{g}/codes/enabled`는 그대로 둔다(화면은 위 API를 쓴다)

## 그룹 목록 + 항목 표, 코드 직접 입력 (2026-11-21)

화면이 그룹 목록 → 고른 그룹의 항목 표로 바뀐다(spec.md FR-005). 위 "메뉴 관리와 같은 조작"의 섞은 일괄 처리 API를 걷어낸다.

| 시나리오 | 요청 | 기대 |
|---|---|---|
| US1-AC2 | `POST /api/v1/admin/code-groups/{g}/codes` (`code`, `name`) | 201, 그 코드로 그룹 맨 뒤. 그룹 안에서 겹치면 409 `R002` |
| US1-AC2 | `POST /api/v1/admin/code-groups/{g}/codes` (`code` 없음·빈 값) | 400 `V001`. 자동 순번은 없다 |
| US1-AC3 | `PUT /api/v1/admin/code-groups/{g}/codes/{code}` (`name`, `enabled`) | 200. 코드는 그대로 |
| US1-AC4 | `PUT /api/v1/admin/code-groups/{g}/codes/enabled` (`codes` 둘, `enabled` false) | 204. `GET /api/v1/codes/{g}`에서 빠짐. 없는 코드가 섞이면 404 `R001`, 아무것도 안 바뀜 |
| US1-AC5 | `PUT /api/v1/admin/code-groups/order`, `PUT /api/v1/admin/code-groups/{g}/codes/order` | 204. 목록·표가 그 순서 |

- `PUT /api/v1/admin/code-groups/enabled`(그룹과 항목을 섞은 일괄 사용 여부)는 없앤다. 그룹은 `PUT /api/v1/admin/code-groups/{g}`의 `enabled`로 하나씩 끈다
