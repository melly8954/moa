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

그룹은 `code_groups`에 두고 관리자가 다룬다. 시스템 그룹(`system` = true)은 마이그레이션으로만 생긴다.
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
