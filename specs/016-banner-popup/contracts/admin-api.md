# Contract: 배너·팝업 관리 API

요청·응답 전체는 local 프로필의 `/v3/api-docs`가 기준이다. 오류 응답 형식과 코드는 골격 `ErrorCode`를 따른다.
칸의 규칙은 [data-model.md](../data-model.md)와 [research.md](../research.md) R10, 이미지 올리기는 키트 rich-text API(`POST /api/v1/rich-text/images`)를 그대로 쓴다 (research R1).
일시는 `OffsetDateTime`(예: `2026-12-01T10:00:00+09:00`)이다 (research R5). 사용자용 API는 두지 않는다 (FR-011).

## 배너 관리 API

`/api/v1/admin/banners`. 모두 `BANNER_MANAGE` 권한이 필요하다 (없으면 403 `A005`, 토큰 없으면 401 `A001`).

| 요청 | 설명 | 응답 |
|---|---|---|
| `GET /api/v1/admin/banners?keyword=&status=&page=&size=` | 관리 목록. `keyword`는 이름 포함, `status`는 `SCHEDULED`·`ACTIVE`·`ENDED`·`DISABLED`·`ALL`, 없으면 끝남 빼고. 정렬은 순서, 시작 늦은 것 | 200 `PageResponse` {id, name, wideImageUrl, startsAt, endsAt, sortOrder, enabled, status} |
| `GET /api/v1/admin/banners/{id}` | 드로어에서 불러오기 | 200 {id, name, wideImageUrl, narrowImageUrl, altText, linkUrl, startsAt, endsAt, sortOrder, enabled, status, createdAt, updatedAt}. 없으면 404 `R001` |
| `POST /api/v1/admin/banners` | 등록. 본문 {name, wideImageUrl, narrowImageUrl?, altText, linkUrl?, startsAt, endsAt?, sortOrder, enabled} | 201 상세와 같은 모양. 입력 오류 400 `V001` |
| `PUT /api/v1/admin/banners/{id}` | 고치기. 본문은 등록과 같다 | 200 상세 |
| `DELETE /api/v1/admin/banners/{id}` | 소프트 삭제, 이미지 detach | 204 |
| `PUT /api/v1/admin/banners/enabled` | 일괄 켜기·끄기 {ids(1~200), enabled} | 204. 하나라도 없으면 404 `R001`, 아무것도 바꾸지 않는다 |
| `POST /api/v1/admin/banners/bulk-delete` | 일괄 삭제 {ids(1~200)} | 204. 하나라도 없으면 404 `R001`, 아무것도 지우지 않는다 |

- 이미지 주소가 rich-text 이미지 주소가 아니거나, 없는 이미지거나, GIF면 400 `V001` (research R1, R10). 이미지의 주인은 확인하지 않는다
- `endsAt`이 `startsAt`과 같거나 앞이면 400 `V001` (칸 `endsAt`)
- `linkUrl` 규칙을 어기면 400 `V001` (칸 `linkUrl`)

## 팝업 관리 API

`/api/v1/admin/popups`. 모두 `POPUP_MANAGE` 권한이 필요하다.

| 요청 | 설명 | 응답 |
|---|---|---|
| `GET /api/v1/admin/popups?keyword=&status=&page=&size=` | 관리 목록. `keyword`는 제목 포함, 나머지는 배너와 같다 | 200 `PageResponse` {id, title, startsAt, endsAt, sortOrder, enabled, status} |
| `GET /api/v1/admin/popups/{id}` | 드로어에서 불러오기 | 200 {id, title, content, linkUrl, startsAt, endsAt, sortOrder, enabled, status, createdAt, updatedAt}. 없으면 404 `R001` |
| `POST /api/v1/admin/popups` | 등록 {title, content, linkUrl?, startsAt, endsAt?, sortOrder, enabled} | 201 상세 |
| `PUT /api/v1/admin/popups/{id}` | 고치기. 본문은 등록과 같다 | 200 상세 |
| `DELETE /api/v1/admin/popups/{id}` | 소프트 삭제, 본문 이미지 detach | 204 |
| `PUT /api/v1/admin/popups/enabled` | 일괄 켜기·끄기 {ids, enabled} | 204, 배너와 같다 |
| `POST /api/v1/admin/popups/bulk-delete` | 일괄 삭제 {ids} | 204, 배너와 같다 |

- `content`는 `@SafeHtml`로 걸러 저장한다. 거른 뒤 비었거나 20,000자를 넘으면 400 `V001`

## AC와 확인 방법

| AC | 호출 | 기대 |
|---|---|---|
| US1-AC1 | 이미지 올리기 → ADMIN `POST` (시작 과거) / (시작 미래) → 목록 | 201, 목록에 `wideImageUrl`이 있고 `status` `ACTIVE` / `SCHEDULED` |
| US1-AC2 | 끝 일시가 과거인 배너 → 목록 `status=ALL` | `status` `ENDED`. 기본 목록(`status` 없음)에는 없다 |
| US1-AC3 | `ACTIVE` 배너를 `PUT {id}` enabled=false → 목록, 다시 true | `DISABLED`, 다시 `ACTIVE` |
| US1-AC4 | 순서 0·0·5인 배너(시작 다름) 중 하나를 `PUT`으로 순서 1 → 목록 | 순서 작은 것부터, 같으면 시작 늦은 것 먼저 |
| US1-AC5 | `DELETE {id}` → 목록, 상세 | 204, 목록에 없음, 상세 404 `R001` |
| US1-AC6 | 역할 없는 운영자 `POST`·`PUT` | 403 `A005` |
| US1-AC7 | `endsAt` ≤ `startsAt`, `linkUrl` `javascript:alert(1)`·`//evil.example`·`ftp://x` | 400 `V001`, 칸 오류 `endsAt`·`linkUrl` |
| US1-AC8 | GIF를 올려 그 주소로 `POST`, 상한을 넘는 파일 올리기 | 400 `V001` / 올리기 400 `V001` |
| US1-AC9 | 넓은 화면 이미지를 새로 올려 `PUT` → 상세, 이전 이미지 기록 | 상세에 새 주소, 이전 이미지는 임시 |
| US1-AC10 | e2e | 드로어 미리보기 두 틀, 좁은 화면용이 없으면 넓은 화면용 |
| US1-AC11 | 셋 중 둘 `bulk-delete` → 목록 | 204, 둘 없음, 하나 남음. 없는 `id`를 섞으면 404 `R001`이고 셋 다 남음 |
| US1-AC12 | `ACTIVE`·`DISABLED` 둘 `PUT enabled` false → 목록, 다시 true | 둘 다 `DISABLED`, 다시 기간에 맞는 상태 |
| US1-AC13 | 이름 "윈터컵"·"점검" → `keyword=윈터`, `keyword=윈터&status=ACTIVE` | "윈터컵"만 |
| US1-AC14 | 화면 기본값으로 등록 (e2e) / API는 `sortOrder` 0, `enabled` true로 등록 | 순서 0, 켜짐 |
| US2-AC1 | ADMIN `POST` 팝업 (시작 과거·미래) → 목록 | 201, `ACTIVE`·`SCHEDULED` |
| US2-AC2 | `PUT {id}` enabled=false, `DELETE {id}` | `DISABLED` / 204, 목록에 없음 |
| US2-AC3 | 역할 없는 운영자 `POST`·`PUT` | 403 `A005` |
| US2-AC4 | `endsAt` ≤ `startsAt`, 잘못된 `linkUrl` | 400 `V001` |
| US2-AC5 | 셋 중 둘 `bulk-delete` | 204, 둘 없음 |
| US2-AC6 | 켜진 둘 `PUT enabled` false | 둘 다 `DISABLED` |
| US2-AC7 | 본문 `<p>안녕</p><script>alert(1)</script>` → 상세 | `script` 없음, `<p>안녕</p>` 남음 |
| US2-AC8 | 순서를 바꿔 `PUT` → 목록 | 배너와 같은 순서 규칙 |
