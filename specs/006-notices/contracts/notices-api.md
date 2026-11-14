# Contract: 공지 API

요청·응답 전체는 local 프로필의 `/v3/api-docs`가 기준이다. 오류 응답 형식과 코드는 골격 `ErrorCode`를 따른다.
칸의 규칙은 [data-model.md](../data-model.md), 본문 이미지 올리기는 키트 rich-text API(`POST /api/v1/rich-text/images`)를 그대로 쓴다.

## 관리 API

`/api/v1/admin/notices`. 모두 `NOTICE_MANAGE` 권한이 필요하다 (없으면 403 `A005`, 토큰 없으면 401 `A001`).

| 요청 | 설명 | 응답 |
|---|---|---|
| `GET /api/v1/admin/notices?keyword=&status=&page=&size=` | 관리 목록. 임시 저장 포함, 고친 최신순. `keyword`는 제목 포함, `status`는 `DRAFT`·`PUBLISHED` | 200 `PageResponse` {id, title, status, pinned, publishedAt, updatedAt} |
| `GET /api/v1/admin/notices/{id}` | 고칠 때 불러오기 | 200 {id, title, content, status, pinned, publishedAt, createdAt, updatedAt}. 없으면 404 `R001` |
| `POST /api/v1/admin/notices` | 만들기. 본문 {title, content, pinned, publish}. `publish=true`면 바로 게시 | 201 상세와 같은 모양. 입력 오류 400 `V001` |
| `PUT /api/v1/admin/notices/{id}` | 제목·본문 고치기 {title, content}. 상태와 게시 시각은 그대로 | 200 상세 |
| `POST /api/v1/admin/notices/{id}/publish` | 임시 저장 공지를 게시. `published_at` = 지금 | 200 상세. 이미 게시했으면 409 `B001` |
| `PUT /api/v1/admin/notices/{id}/pin` | 고정·풀기 {pinned} | 200 상세 |
| `DELETE /api/v1/admin/notices/{id}` | 소프트 삭제, 본문 이미지 detach | 204 |

- `content`는 `@SafeHtml`로 걸러 저장한다. 거른 뒤 비었거나 20,000자를 넘으면 400 `V001`
- `title`은 1~200자

## 공개 API

`/api/v1/notices`. `@PermitAll` (로그인 없이 연다).

| 요청 | 설명 | 응답 |
|---|---|---|
| `GET /api/v1/notices?page=&size=` | 게시한 공지만. 고정 먼저, 게시 최신순 (FR-005) | 200 `PageResponse` {id, number, title, pinned, publishedAt}. `number`는 게시 순번 (research R11) |
| `GET /api/v1/notices/{id}` | 게시한 공지 상세 | 200 {id, title, content, pinned, publishedAt}. 임시 저장·지운 공지·없는 공지는 404 `R001` |

## 새 공지 (로그인한 사용자)

토큰이 없으면 401 `A001`. 역할·권한은 묻지 않는다 (관리자·운영자도 쓴다).

| 요청 | 설명 | 응답 |
|---|---|---|
| `GET /api/v1/notices/new` | 마지막 확인 뒤 게시된 공지가 있나 | 200 {hasNew} |
| `PUT /api/v1/notices/seen` | 지금(서버 시각)을 마지막 확인 시각으로 기록 | 204 |

## AC와 확인 방법

| AC | 호출 | 기대 |
|---|---|---|
| US1-AC1 | ADMIN `POST` (publish=true) → 공개 목록 | 201, 공개 목록에서 고정 공지 다음 첫 항목 |
| US1-AC2 | ADMIN `POST` (publish=false) → 관리 목록, 공개 목록·상세 | 관리 목록 `status`=`DRAFT`, 공개 목록에 없음, 공개 상세 404 `R001` |
| US1-AC3 | 임시 저장 공지 `POST .../publish` → 공개 목록 | 200 `publishedAt` 있음, 공개 목록 첫 항목(고정 다음) |
| US1-AC4 | `PUT .../pin` true → 공개 목록, 다시 false | 고정 공지가 더 최근 일반 공지보다 위. 풀면 게시 순서 |
| US1-AC5 | `PUT .../{id}` → 공개 상세 | 고친 제목·본문 |
| US1-AC6 | `DELETE` → 공개 목록·상세 | 204, 목록에 없음, 상세 404 `R001` |
| US1-AC7 | 역할 없는 운영자(USER) `POST`·`PUT` | 403 `A005` |
| US1-AC8 | ADMIN `POST` 본문 `<p>안녕</p><script>alert(1)</script><img src="x" onerror="alert(1)">` → 공개 상세 | `content`에 `script`·`onerror`·외부 `img`가 없고 `<p>안녕</p>`은 남음 |
| US2-AC1 | 고정·일반 공지 여러 개(지운 공지 하나 포함) → 공개 목록 | 고정 먼저, 각 무리 안에서 게시 최신순. 칸 {number, title, publishedAt, pinned}. `number`는 게시 순번이고 지운 공지는 세지 않는다 |
| US2-AC2 | 공개 상세 (본문에 서식·본문 이미지 주소) | 제목·게시 시각·본문 HTML 그대로 |
| US2-AC3 | 토큰 없이 공개 목록·상세 / 토큰 있이 같은 요청 | 둘 다 200, 같은 내용 |
| US3-AC1 | 사용자 `PUT seen` → ADMIN 게시 → `GET new` | `hasNew`=true |
| US3-AC2 | 이어서 `PUT seen` → `GET new` | `hasNew`=false |
| US3-AC3 | `PUT seen` → ADMIN 임시 저장만 → `GET new` | `hasNew`=false |
| US4-AC1 | 007 뒤에 정한다 (research R9) | |
