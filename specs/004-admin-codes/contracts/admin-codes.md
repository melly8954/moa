# Contract: 공통코드

처음에는 새 API가 없었다(아래 "관리 화면 정리"에서 더했다). 키트 API를 moa의 AC에 맞춰 어떻게 확인하는지만 적는다. 요청·응답 전체는 local 프로필의 `/v3/api-docs`가 기준이다.

## 코드 관리 목록

`GET /api/v1/admin/code-groups`

| AC | 호출자 | 기대 |
|---|---|---|
| US1-AC1 | ADMIN | 200. 그룹 코드 목록이 spec.md FR-001 표와 같다. 지금은 빈 목록 |

## 관리 화면 정리 (2026-11-15, research R5~R8)

그룹은 코드로 등록한다. 그룹 만들기 API(`POST /api/v1/admin/code-groups`)는 없앤다.

| AC | 호출 | 기대 |
|---|---|---|
| US1-AC1 | `GET /api/v1/admin/code-groups/tree` (시험 그룹 둘) | 200. 그룹마다 `groupCode`, `name`, `description`, `usedBy`, 항목. `POST /api/v1/admin/code-groups`는 405 |
| US1-AC2 | `POST /api/v1/admin/code-groups/{g}/codes` (`name`만) | 201, `code` = `001`. 지운 뒤 다시 만들면 `002` |
| US1-AC3 | `PUT /api/v1/admin/code-groups/{g}/codes/{code}` (`name`, `enabled`) | 200. 코드는 그대로 |
| US1-AC4 | `PUT /api/v1/admin/code-groups/{g}/codes/enabled` (`codes` 둘, `enabled` false) | 204. `GET /api/v1/codes/{g}`에서 빠짐 |
| US1-AC5 | `PUT /api/v1/admin/code-groups/{g}/codes/order` | 204. 화면용 목록이 그 순서. 다른 그룹의 코드가 섞이면 404 `R001`, 아무것도 안 바뀜 |
| US1-AC6 | 등록 그룹 없음 `GET .../tree` | 200, 빈 목록. 테스트에는 시험 그룹이 언제나 등록되어 `CodeGroupRegistryTest`(단위)로 본다 |
