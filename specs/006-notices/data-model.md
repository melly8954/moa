# Data Model: 공지사항 (notices)

moa 마이그레이션 `V2_1__notices.sql`(테이블), `V2_2__notice_admin_menu.sql`(메뉴)을 더한다. 본문 이미지 테이블 `rich_text_images`는 키트 `V1_9`가 기준이다.

## 공지 (`notices`)

| 칸 | 형식 | 규칙 |
|---|---|---|
| `id` | BIGINT, 자동 증가 | |
| `title` | VARCHAR(200) | 필수, 1~200자 |
| `content` | MEDIUMTEXT | 필수. 저장 때 걸러진 HTML (rich-text `@SafeHtml`), 거른 뒤 20,000자 이하 |
| `status` | VARCHAR(20) | `DRAFT`(임시 저장) · `PUBLISHED`(게시) |
| `pinned` | BOOLEAN | 상단 고정. 기본 `FALSE` |
| `published_at` | DATETIME(6) NULL | 게시한 시각. `DRAFT`면 비어 있다 |
| `created_at`·`updated_at`·`created_by`·`updated_by` | | 골격 감사 필드 (쓴 사람, 고친 사람과 시각) |
| `deleted_at`·`deleted_by` | | 소프트 삭제 (FR-007) |

- 인덱스: 공개 목록용 `(status, deleted_at, pinned, published_at)`
- 공개 목록 정렬: `pinned DESC, published_at DESC, id DESC` (FR-005, 고정끼리도 게시 최신순)
- 관리 목록 정렬: `updated_at DESC, id DESC`. 조건은 제목 글자(포함), 상태

### 상태 전이

```text
(만들기, publish=false) → DRAFT ──게시──→ PUBLISHED
(만들기, publish=true)  ─────────────────→ PUBLISHED
PUBLISHED → DRAFT: 막는다 (B001)      아무 상태 → 삭제(deleted_at)
```

- 게시할 때만 `published_at`을 넣는다. 고치기·고정·풀기는 `published_at`을 바꾸지 않는다 (Edge Cases)
- 이미 게시한 공지를 다시 게시하면 B001

## 공지 확인 기록 (`notice_reads`)

| 칸 | 형식 | 규칙 |
|---|---|---|
| `user_id` | BIGINT, PK | 사용자마다 한 행 |
| `last_seen_at` | DATETIME(6) | 사용자 공지 목록을 마지막으로 연 서버 시각 |

- 새 공지 있음: `published_at > last_seen_at`인 게시·미삭제 공지가 있다. 행이 없으면 게시·미삭제 공지가 하나라도 있다 (FR-008, Edge Cases)
- 감사 필드를 두지 않는다. 사용자가 자기 기록만 바꾼다

## 공지 이미지 (키트 `rich_text_images`)

- `owner_type` = `NOTICE`, `owner_id` = 공지 `id`
- 만들기·고치기 뒤 attach, 지우기 뒤 detach (research R3, R4). 정리는 키트 동작

## 공지 관리 권한과 메뉴

| 대상 | 값 |
|---|---|
| 권한 키 | `NOTICE_MANAGE` ("공지 관리", 묶음 "공지") |
| 메뉴 (`menus`) | `id` 7, 최상위(`parent_id` 없음), 이름 "공지 관리", `path` `/admin/notices`, `icon` `clipboard`, `sort_order` 50, `permission` `NOTICE_MANAGE` |

- 관리자(ADMIN)는 모든 권한을 가진다. 운영자는 역할에 이 권한을 묶어 받는다 (`003-admin-roles`)
- `001-admin-menus`의 data-model 표에 6번째 줄로 더한다
