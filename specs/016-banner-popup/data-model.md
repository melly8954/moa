# Data Model: 배너·팝업 관리 (banner-popup)

마이그레이션 한 파일(`V<다음 번호>__banner_popup.sql`, PR 직전에 번호를 정한다. 지금은 `V11`)이 두 테이블과 메뉴를 넣는다. 테이블·열·인덱스 규칙은 `docs/db.md`를 따른다.
이미지 파일과 그 기록은 키트 `tb_rich_text_image`가 맡는다 (research R1).

## 배너 (`tb_banner`)

| 칸 | 형식 | 규칙 |
|---|---|---|
| `id` | BIGINT, 자동 증가 | |
| `name` | VARCHAR(100) | 필수, 1~100자. 관리 목록에서 알아보는 이름 (사용자에게 안 보임) |
| `wide_image_url` | VARCHAR(100) | 필수. 넓은 화면 이미지 주소 (`/api/v1/rich-text/images/{publicId}`) |
| `narrow_image_url` | VARCHAR(100) NULL | 좁은 화면 이미지 주소. 비면 넓은 화면 이미지를 쓴다 |
| `alt_text` | VARCHAR(200) | 필수, 1~200자. 대체 문구 |
| `link_url` | VARCHAR(500) NULL | 연결 주소 (research R10) |
| `starts_at` | DATETIME(6) | 필수. 노출 시작 일시 |
| `ends_at` | DATETIME(6) NULL | 노출 끝 일시. 있으면 `starts_at`보다 뒤 |
| `sort_order` | INT | 필수, 0~9999. 작을수록 먼저 |
| `enabled` | BOOLEAN | 켜짐 |
| `created_at`·`updated_at`·`created_by`·`updated_by` | | 감사 필드 (등록한 사람, 고친 사람과 시각) |
| `deleted_at`·`deleted_by` | | 소프트 삭제 (FR-009) |

- 엔터티 `Banner`는 `SoftDeleteEntity`, `@SQLRestriction`, 저장소는 `SoftDeleteRepository`다. `OWNER_TYPE = "BANNER"`
- 인덱스 (`docs/db.md`: `where`·`order by` 열마다): 목록 정렬용 `idx_banner_deleted_at_sort_order_starts_at` (`deleted_at`, `sort_order`, `starts_at`), 상태 거르기용 `idx_banner_enabled_starts_at_ends_at` (`enabled`, `starts_at`, `ends_at`). 이름 포함 검색은 공지 제목처럼 인덱스를 쓰지 않는다
- 관리 목록 정렬: `sort_order ASC, starts_at DESC, id DESC` (FR-005)
- 관리 목록 조건: 이름 글자(포함), 상태 (research R4)

## 팝업 (`tb_popup`)

| 칸 | 형식 | 규칙 |
|---|---|---|
| `id` | BIGINT, 자동 증가 | |
| `title` | VARCHAR(200) | 필수, 1~200자 |
| `content` | MEDIUMTEXT | 필수. 저장 때 걸러진 HTML (rich-text `@SafeHtml`), 거른 뒤 20,000자 이하 |
| `link_url` | VARCHAR(500) NULL | 연결 주소 |
| `starts_at`·`ends_at`·`sort_order`·`enabled` | | 배너와 같다 |
| 감사 필드, `deleted_at`·`deleted_by` | | 배너와 같다 |

- 엔터티 `Popup`, `OWNER_TYPE = "POPUP"`
- 인덱스: `idx_popup_deleted_at_sort_order_starts_at`, `idx_popup_enabled_starts_at_ends_at`
- 관리 목록 조건: 제목 글자(포함), 상태. 정렬은 배너와 같다

## 상태 (칸이 아니라 읽을 때 정한다, FR-006, research R3)

```text
enabled = false                         → DISABLED (꺼짐)
enabled = true, 지금 < starts_at          → SCHEDULED (예정)
enabled = true, ends_at ≤ 지금            → ENDED (끝남)
enabled = true, 그 밖                     → ACTIVE (노출 중)
```

- `ExposureStatus`는 `dto`에 두고 배너·팝업이 같이 쓴다
- 목록의 상태 거르기는 같은 조건을 질의로 쓴다. 값이 없으면 `ENDED`만 뺀다 (research R4)

## 이미지 (키트 `tb_rich_text_image`)

| 소유 | `owner_type` | 잇는 때 | 푸는 때 |
|---|---|---|---|
| 배너 이미지 | `BANNER` | 만들기·고치기 뒤 두 주소로 attach | 지우기(한 건·일괄) 뒤 detach |
| 팝업 본문 이미지 | `POPUP` | 만들기·고치기 뒤 본문으로 attach | 지우기(한 건·일괄) 뒤 detach |

- 빠진 이미지는 임시가 되어 키트 정리기가 24시간 뒤 지운다 (spec Edge Cases)

## 권한과 메뉴 (FR-001, FR-002, research R6)

| 대상 | 값 |
|---|---|
| 권한 | `BANNER_MANAGE` ("배너 관리", 묶음 "마케팅"), `POPUP_MANAGE` ("팝업 관리", 묶음 "마케팅") |
| 연결 화면 | `ADMIN_BANNERS` 배너 관리 `/admin/banners`, `ADMIN_POPUPS` 팝업 관리 `/admin/popups` (`MarketingScreens`, `@Order(70)`) |
| 상위 메뉴 (`tb_menu`) | 이름 "마케팅", `screen_key` 없음, `icon` `megaphone`, `sort_order` 15, `audience` `ADMIN` |
| 하위 메뉴 | "배너 관리" `ADMIN_BANNERS` `folder` 10, "팝업 관리" `ADMIN_POPUPS` `file` 20. 부모는 같은 파일에서 넣은 "마케팅" |

- 메뉴 행은 넣기만 한다. 기존 행은 고치지 않는다 (`docs/db.md`)
- `001-admin-menus` data-model의 묶음 표와 연결 화면 표에 더했다
