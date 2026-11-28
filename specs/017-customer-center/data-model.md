# Data Model: 고객센터 (customer-center)

마이그레이션 한 파일(`V<다음 번호>__customer_center.sql`, PR 직전에 번호를 정한다. 2026-11-28에 dev가 `V13`(`018-member-withdrawal`)까지라 `V14`다)이 테이블, 분류 코드, 메뉴를 넣는다. 테이블·열·인덱스 규칙은 `docs/db.md`를 따른다.
이미지 파일과 그 기록은 키트 `tb_rich_text_image`가 맡는다 (research R1·R2).

## FAQ (`tb_faq`)

| 칸 | 형식 | 규칙 |
|---|---|---|
| `id` | BIGINT, 자동 증가 | |
| `category_code` | VARCHAR(50) | 필수. `SUPPORT_CATEGORY`의 항목 코드 (research R8) |
| `question` | VARCHAR(200) | 필수, 1~200자 |
| `answer` | MEDIUMTEXT | 필수. 저장 때 걸러진 HTML (rich-text `@SafeHtml`), 거른 뒤 20,000자 이하 |
| `sort_order` | INT | 필수, 0~9999. 작을수록 먼저. 화면 기본값 0 |
| `enabled` | BOOLEAN | 켜짐. 화면 기본값 켜짐 |
| 감사 열, `deleted_at`·`deleted_by` | | `SoftDeleteEntity` (FAQ에는 개인정보가 없다) |

- 엔터티 `Faq`, `OWNER_TYPE = "FAQ"`
- 인덱스: 목록 정렬용 `idx_faq_deleted_at_sort_order_created_at` (`deleted_at`, `sort_order`, `created_at`), 분류 거르기용 `idx_faq_category_code`. 글자 포함 검색은 공지 제목처럼 인덱스를 쓰지 않는다(200개 상한)
- 상한: 지우지 않은 행 200개 (research R9)

## 문의 (`tb_inquiry`)

| 칸 | 형식 | 규칙 |
|---|---|---|
| `id` | BIGINT, 자동 증가 | |
| `user_id` | BIGINT | 필수. 쓴 회원 (`tb_user.id`, 외래 키 없음) |
| `category_code` | VARCHAR(50) | 필수. `SUPPORT_CATEGORY`의 항목 코드 |
| `title` | VARCHAR(100) | 필수, 1~100자 |
| `content` | VARCHAR(2000) | 필수, 1~2000자. 서식 없는 글 (research R2) |
| `answer` | MEDIUMTEXT NULL | 답변. 걸러진 HTML, 20,000자 이하 |
| `answered_at` | DATETIME(6) NULL | 처음 답변한 시각. 비면 답변 대기 (research R3) |
| `answered_by` | BIGINT NULL | 처음 답변한 관리자 |
| `created_at`·`updated_at`·`created_by`·`updated_by` | | `BaseEntity` (하드 삭제). 회원은 문의를 고치지 않으므로 `updated_*`는 답변을 고친 관리자와 시각이다 |

- 엔터티 `Inquiry`, `OWNER_TYPE = "INQUIRY"`(첨부), `ANSWER_OWNER_TYPE = "INQUIRY_ANSWER"`(답변 본문 이미지)
- 하드 삭제: 회원이 지울 때(답변 대기만), 탈퇴 뒤 1년 정리 때 행을 `DELETE`한다 (`docs/privacy.md`, research R5·R10)
- 인덱스
  - `idx_inquiry_user_id_created_at` (`user_id`, `created_at`): 내 문의 목록, 탈퇴 회원의 문의 찾기
  - `idx_inquiry_answered_at_created_at` (`answered_at`, `created_at`): 관리 목록의 상태 거르기와 정렬
  - `idx_inquiry_category_code`: 분류 거르기
  - 제목·작성자 글자 포함 검색은 인덱스를 쓰지 않는다
- 테이블 `COMMENT`: `'1:1 문의 (개인정보: 탈퇴한 날부터 1년 보관, docs/privacy.md)'`

### 상태 (칸이 아니라 읽을 때 정한다, FR-008)

```text
answered_at IS NULL      → WAITING  (답변 대기)
answered_at IS NOT NULL  → ANSWERED (답변 완료)
```

- 답변 대기 → 답변 완료만 있다. 되돌리지 않는다
- 처음 답변 저장은 행을 잠그고 `answered_at`이 비었을 때만 알림을 보낸다 (research R3)

## 문의 첨부 이미지 (`tb_inquiry_image`)

| 칸 | 형식 | 규칙 |
|---|---|---|
| `inquiry_id` | BIGINT | 필수. 주인 문의 |
| `sort_order` | INT | 0~4. 붙인 순서 |
| `image_url` | VARCHAR(100) | `/api/v1/rich-text/images/{publicId}`. 저장된 이미지여야 한다(키트 `load`로 확인, 없으면 400 `V001`) |

- 컬렉션 테이블(`@ElementCollection`, `@OrderColumn(name = "sort_order")`)이라 감사 열이 없다. PK는 (`inquiry_id`, `sort_order`). `COMMENT`: `'문의 첨부 이미지 (PK: 문의별 순서; 감사 열 없음: 컬렉션 테이블)'`
- 문의마다 0~5개. 문의와 함께 지운다

## 이미지 (키트 `tb_rich_text_image`)

| 소유 | `owner_type` | 잇는 때 | 푸는 때 |
|---|---|---|---|
| FAQ 답 본문 이미지 | `FAQ` | 만들기·고치기 뒤 답으로 attach | 지우기(한 건·일괄) 뒤 detach |
| 문의 첨부 | `INQUIRY` | 만들기 뒤 주소들로 attach | 문의 지우기·정리 뒤 detach |
| 답변 본문 이미지 | `INQUIRY_ANSWER` | 답변 저장 뒤 답변으로 attach | 문의 지우기·정리 뒤 detach |

- 빠진 이미지는 임시가 되어 키트 정리기가 24시간 뒤 파일과 함께 지운다

## 고객센터 분류 (`tb_code_group`, `tb_code`)

| 그룹 | 이름 | 설명 | 항목 |
|---|---|---|---|
| `SUPPORT_CATEGORY` | 고객센터 분류 | FAQ와 1:1 문의의 분류 | spec FR-003 표 (`ACCOUNT`, `USAGE`, `ERROR`, `SUGGESTION`, `ETC`) |

- 같은 `group_code`가 이미 있으면 넣지 않는다. 넣기만 한다 (research R8)

## 권한, 연결 화면, 메뉴 (FR-001, FR-002, FR-014, research R7)

| 대상 | 값 |
|---|---|
| 권한 | `FAQ_MANAGE` ("FAQ 관리", 묶음 "고객센터"), `INQUIRY_MANAGE` ("문의 관리", 묶음 "고객센터") |
| 연결 화면 | `ADMIN_FAQS` FAQ 관리 `/admin/faqs`, `ADMIN_INQUIRIES` 문의 관리 `/admin/inquiries`, `SUPPORT` 고객센터 `/support` (사용자, 로그인 필요 없음). `SupportScreens`, `@Order(65)` |
| 관리 메뉴 (`tb_menu`) | 부모 "운영"(`id` 10) 아래 "FAQ 관리" `ADMIN_FAQS` `list` 23, "문의 관리" `ADMIN_INQUIRIES` `mail` 26 |
| 사용자 메뉴 | "고객센터" `SUPPORT` `mail` 90, `audience` `USER`, `visibility` `PUBLIC` |

- 메뉴 행은 넣기만 한다. 기존 행은 고치지 않는다 (`docs/db.md`)
- `001-admin-menus` data-model의 묶음 표와 연결 화면 표에 더했다

## 알림 (FR-012)

| 값 | 내용 |
|---|---|
| `NotificationType.INQUIRY_ANSWERED` | 답변 대기 문의에 처음 답변했을 때. 받는 사람 문의를 쓴 회원, 제목 "1:1 문의에 답변이 등록되었습니다", 연결 `/support/inquiries/{id}` |

## 파기 (FR-017, research R10, 회원 탈퇴 스펙 뒤)

| 값 | 내용 |
|---|---|
| `PurgeTarget.INQUIRY` | 1:1 문의. 탈퇴한 날부터 `app.inquiry.withdrawn-retention-days`(기본 365)일이 지난 회원의 문의를 행 삭제 |
