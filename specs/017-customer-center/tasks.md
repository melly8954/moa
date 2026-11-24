---

description: "고객센터 (customer-center) 작업 목록"
---

# Tasks: 고객센터 (customer-center)

**Input**: `specs/017-customer-center/`의 설계 문서

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api.md, contracts/screens.md, quickstart.md

**Tests**: 원칙 II에 따라 AC마다 테스트를 하나 둔다. CLAUDE.md에 따라 기능 코드(백엔드·프론트)를 먼저 끝내고 테스트 작업은 맨 끝(Phase 9)에 둔다. 로컬에서는 빠른 검사(`.\gradlew.bat build`, `npm run check`)만 돌리고, DB 테스트와 e2e는 PR CI로 본다.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: 병렬 가능 (다른 파일, 의존 없음)
- **[Story]**: 작업이 속한 사용자 스토리 (US1 FAQ 관리, US2 사용자 FAQ, US3 회원 문의, US4 관리자 답변, US5 탈퇴 회원 문의 보관)
- 경로는 저장소 루트 기준이다. 백엔드 패키지 `backend/src/main/java/com/moa/`는 `B/`, 프론트 `frontend/src/`는 `F/`로 줄여 쓴다

---

## Phase 1: Setup (번호 확인)

**Purpose**: 구현 직전에 겹칠 수 있는 번호와 먼저 생겼을 수 있는 공용 코드를 다시 확인한다

- [ ] T001 구현 브랜치 `feat/017-customer-center`를 최신 `dev`에서 딴 뒤 확인한다: backend/src/main/resources/db/migration의 마지막 버전(계획 때 dev `V10`, 016이 `V11` 예정 → 이 기능 `V12`), B/api/screen의 `@Order` 값(계획 때 공지 60, 016 70 → 이 기능 65가 비었는지), B/api/permission에 `FAQ_MANAGE`·`INQUIRY_MANAGE`가 없는지, B/common/exception/ErrorCode.java에 `B301`이 없는지, B/api/service/user/MemberActivityPolicy.java와 backend/src/test/java/com/moa/support/CodeSeed.java가 이미 있는지(008·009가 먼저 만들었으면 그것을 쓴다). 다르면 data-model.md, plan.md, contracts/api.md, 이 tasks.md의 번호를 먼저 고친다. 마이그레이션 번호는 PR 직전에 다시 본다(T045)

**Checkpoint**: 번호와 공용 코드 사정이 정해졌다

---

## Phase 2: Foundational (테이블·분류·메뉴, 권한·연결 화면, 알림 종류, 같이 쓰는 부품)

**Purpose**: 네 스토리가 함께 쓰는 것. 이것이 끝나야 스토리를 시작한다

- [ ] T002 backend/src/main/resources/db/migration/V12__customer_center.sql: 머리 주석 `-- moa: 고객센터(FAQ·1:1 문의) 테이블, 분류 코드, 메뉴. 스펙 specs/017-customer-center (FR-001~FR-004, FR-007, FR-008, FR-014)`. `docs/db.md`를 따른다(모든 열 COMMENT, `DEFAULT` 없음, 이름 있는 인덱스)
  - `tb_faq`: `id BIGINT NOT NULL AUTO_INCREMENT`, `category_code VARCHAR(50) NOT NULL`('분류 (SUPPORT_CATEGORY 항목 코드)'), `question VARCHAR(200) NOT NULL`('질문'), `answer MEDIUMTEXT NOT NULL`('답 (걸러진 HTML)'), `sort_order INT NOT NULL`('순서 (작을수록 먼저)'), `enabled BOOLEAN NOT NULL`('켜짐 (사용자에게 보임)'), 감사 열 6개(`created_at`·`updated_at`·`created_by`·`updated_by`·`deleted_at`·`deleted_by`), 인덱스 `idx_faq_deleted_at_sort_order_created_at`, `idx_faq_category_code`
  - `tb_inquiry`: `id`, `user_id BIGINT NOT NULL`('쓴 회원'), `category_code VARCHAR(50) NOT NULL`, `title VARCHAR(100) NOT NULL`('제목'), `content VARCHAR(2000) NOT NULL`('내용 (서식 없는 글)'), `answer MEDIUMTEXT NULL`('답변 (걸러진 HTML)'), `answered_at DATETIME(6) NULL`('처음 답변한 시각 (NULL: 답변 대기)'), `answered_by BIGINT NULL`('처음 답변한 관리자'), 감사 열 4개(`created_at`·`updated_at`·`created_by`·`updated_by`, 하드 삭제). 테이블 COMMENT `'1:1 문의 (개인정보: 탈퇴한 날부터 1년 보관, docs/privacy.md)'`. 인덱스 `idx_inquiry_user_id_created_at`, `idx_inquiry_answered_at_created_at`, `idx_inquiry_category_code`
  - `tb_inquiry_image`: `inquiry_id BIGINT NOT NULL`('주인 문의'), `sort_order INT NOT NULL`('붙인 순서 (0~4)'), `image_url VARCHAR(100) NOT NULL`('이미지 주소'), PK (`inquiry_id`, `sort_order`). 테이블 COMMENT `'문의 첨부 이미지 (PK: 문의별 순서; 감사 열 없음: 컬렉션 테이블)'`
  - 분류(넣기만): `tb_code_group`에 `SUPPORT_CATEGORY`("고객센터 분류", "FAQ와 1:1 문의의 분류", 사용 중)와 `tb_code`에 spec FR-003 표의 다섯 항목. 같은 `group_code`가 이미 있으면 넣지 않는다(`INSERT … SELECT … WHERE NOT EXISTS`). `sort_order` 열이 있으면 표의 순서를 넣는다(V5 확인)
  - 메뉴(넣기만, 기존 행은 고치지 않는다): 부모 "운영"(`parent_id` 10) 아래 "FAQ 관리"(`ADMIN_FAQS`, `list`, 23)와 "문의 관리"(`ADMIN_INQUIRIES`, `mail`, 26), 사용자 메뉴 "고객센터"(`parent_id` NULL, `SUPPORT`, `mail`, 90, `audience` `USER`, `visibility` `PUBLIC`). 감사 열은 `V6__purge_log.sql`의 메뉴 넣기와 같은 모양
- [ ] T003 [P] B/api/permission/FaqPermissions.java(`MANAGE = "FAQ_MANAGE"`, `PermissionDefinition(MANAGE, "FAQ 관리", "고객센터")`)와 B/api/permission/InquiryPermissions.java(`INQUIRY_MANAGE`, "문의 관리", "고객센터"). `NoticePermissions`와 같은 모양
- [ ] T004 B/api/screen/SupportScreens.java: `@Component @Order(65)`, `ScreenDefinition.admin("ADMIN_FAQS", "FAQ 관리", "/admin/faqs", FaqPermissions.MANAGE)`, `ScreenDefinition.admin("ADMIN_INQUIRIES", "문의 관리", "/admin/inquiries", InquiryPermissions.MANAGE)`, `ScreenDefinition.user("SUPPORT", "고객센터", "/support", false)` (T003 뒤, `NoticeScreens`와 같은 모양)
- [ ] T005 [P] B/api/dto/notification/NotificationType.java에 `INQUIRY_ANSWERED`(주석 "1:1 문의에 처음 답변했다 (specs/017-customer-center FR-012)")를 더한다. 프론트에서 알림 종류로 아이콘·문구를 고르는 곳이 있으면 함께 더한다
- [ ] T006 [P] B/common/exception/ErrorCode.java에 `FAQ_LIMIT_EXCEEDED("B301", HttpStatus.CONFLICT, "FAQ는 200개까지 둘 수 있습니다.")` (research R9)
- [ ] T007 [P] B/api/service/user/MemberActivityPolicy.java: T001에서 없으면 `009-mypage` research R2의 설계대로 만든다. `isManagerAccount(userId)`: 시스템 역할이 `ADMIN`이면 참 (research R4). 이미 있으면 건너뛴다
- [ ] T008 [P] B/api/service/support/SupportCategoryValidator.java(또는 같은 일을 하는 서비스 메서드): 분류 코드가 `SUPPORT_CATEGORY`의 사용 중 항목인지 본다. 아니면 `INVALID_INPUT` V001 칸 오류 `categoryCode` (research R8). `CodeApplication.getScreenCodes`가 캐시를 쓰므로 코드 관리에서 바꾼 뒤 캐시가 비워지는지 확인하고 같은 값을 쓴다
- [ ] T009 [P] F/components/support/: `category-name.ts`(분류 코드 → 이름, `GET /api/v1/codes/SUPPORT_CATEGORY`를 react-query로 한 번 읽는 `useSupportCategories`), `inquiry-status-badge.tsx`(답변 대기는 보통, 답변 완료는 긍정. 기존 배지 부품을 쓴다), `image-thumbnails.tsx`(작은 그림 줄, 누르면 크게 보는 창, 관리 드로어·회원 상세·쓰기 화면이 같이 쓴다. 쓰기 화면용으로 빼기 단추를 켤 수 있다)
- [ ] T010 [P] frontend/messages/ko.json `support` 묶음: 분류·상태 이름, 이미지 크게 보기·빼기 문구. `npm run check`의 문구 검사를 통과하게 둔다

**Checkpoint**: 테이블·분류·메뉴·권한·연결 화면과 같이 쓰는 부품이 준비됐다

---

## Phase 3: User Story 1 - 관리자가 FAQ를 쓰고 관리한다 (Priority: P1) 🎯 MVP

**Goal**: 운영 > FAQ 관리에서 FAQ를 등록·수정·끄기·삭제하고, 순서를 정하고, 여러 개를 한꺼번에 켜고 끄고 지운다

**Independent Test**: 관리자로 FAQ를 등록·끄기·수정·삭제·일괄 처리하고, 관리 목록에 반영되는지 본다 (spec US1. 사용자 화면 반영은 US2 뒤)

### 백엔드

- [ ] T011 [US1] B/api/entity/faq/Faq.java: `@Table(name = "tb_faq")`, `@SQLRestriction(SoftDeleteEntity.NOT_DELETED)`, `SoftDeleteEntity` 상속, `public static final String OWNER_TYPE = "FAQ"`. 칸은 data-model `tb_faq`대로. `modify(categoryCode, question, answer, sortOrder, enabled)`, `changeEnabled(boolean)`
- [ ] T012 [P] [US1] B/api/repository/jpa/faq/FaqRepository.java(`SoftDeleteRepository<Faq, Long>`, `countBy…`로 지우지 않은 수), B/api/repository/reader/faq/FaqReader.java(QueryDSL `findAdminList(categoryCode, enabled, keyword)`: 질문 포함, 정렬 `sort_order ASC, created_at DESC, id DESC`, 쪽 없음. `findAllByIdIn`), B/api/repository/writer/faq/FaqWriter.java (`BannerReader`·`BannerWriter`와 같은 모양)
- [ ] T013 [US1] B/api/dto/faq/FaqDto.java와 B/api/service/faq/FaqService.java: `listAdmin`, `get`(없으면 `NOT_FOUND` R001), `create`(지우지 않은 수가 200이면 `FAQ_LIMIT_EXCEEDED` B301, 분류 확인 T008), `modify`(분류 확인), `delete`, `setEnabled(ids, enabled)`, `deleteAll(ids)`. 일괄은 `requireAll(ids)`로 모든 `id`가 있는지 먼저 보고(하나라도 없으면 R001) 바꾼다 (research R9, `016-banner-popup`과 같은 모양)
- [ ] T014 [US1] B/api/application/v1/faq/FaqApplication.java: 메서드마다 `@Transactional`. 만들기·고치기 뒤 `richTextImageService.attach(Faq.OWNER_TYPE, id, answer)`, 지우기·일괄 지우기 뒤 FAQ마다 `detach` (research R2). 관리 목록의 분류 이름은 `CodeApplication.getAllCodes("SUPPORT_CATEGORY")`로 채운다(꺼진 항목도 이름이 보이게, research R8)
- [ ] T015 [US1] B/api/controller/v1/admin/faq/ApiAdminFaqController.java(`/api/v1/admin/faqs`, 클래스에 `@RequirePermission(FaqPermissions.MANAGE)`, `@Tag`·`@Operation`): contracts/api.md "관리 FAQ" 7개. 요청 request/: `FaqSaveRequest`(`categoryCode` `@NotBlank @Size(max = 50)`, `question` `@NotBlank @Size(max = 200)`, `answer` `@SafeHtml @NotBlank @Size(max = 20000)`, `sortOrder` `@NotNull @Min(0) @Max(9999)`, `enabled` `@NotNull`), `FaqSearchRequest`(`categoryCode`, `enabled` `@Nullable Boolean`, `keyword` `@Size(max = 200)`), `FaqIdsRequest`(`@NotEmpty @Size(max = 200) List<@NotNull Long> ids`), `FaqEnabledRequest`(같은 `ids` + `@NotNull Boolean enabled`). 응답 response/: `FaqAdminListResponse`, `FaqAdminResponse`(일시는 `ApiTimes.of`). POST 201 `{id}`, PUT 204, DELETE·일괄 204

### 프론트

- [ ] T016 [P] [US1] F/types/faq.ts(관리 목록 항목, 상세, 저장 요청, 사용자 FAQ 항목), F/api/faq.ts(`faqAdminApi`: `list`, `get`, `create`, `modify`, `remove`, `setEnabled(ids, enabled)`, `bulkDelete(ids)`. `bannerAdminApi`와 같은 모양)
- [ ] T017 [US1] F/app/(admin)/admin/faqs/faq-drawer.tsx: 넓은 `SideDrawer`(팝업 드로어와 같은 폭) + react-hook-form. 칸은 contracts/screens.md "FAQ 관리" 드로어대로(분류 선택 T009, 질문 200자, 답 `RichTextEditor`를 `Controller`에 잇는다, 순서 기본 0, 켜짐 기본 켜짐). V001 칸 오류는 그 칸 아래에. 고칠 때 삭제(확인)와 저장, 등록할 때 취소와 등록. `useDrawerClose`로 바꾼 채 닫으면 묻는다. B301이면 상한 안내 토스트
- [ ] T018 [US1] F/app/(admin)/admin/faqs/page.tsx: 구성 목록. `ListPage`·`PageHeader`(제목 "FAQ 관리", 설명은 FR-016 문구를 `t.rich`의 `<link>`로 `/admin/inquiries`에 잇는다, 오른쪽 "FAQ 등록"), `SearchPanel`(분류 `ChoiceFilter`, 켜짐 `ChoiceFilter`, 질문 `KeywordFilter`, `useListSearch`), `BulkToolbar`(켜기·끄기·삭제, 삭제만 `confirm({ destructive: true })`), `DataTable`(쪽 없이 표 안 스크롤, `selection`의 `rowLabel` = 질문, 열: 분류, 질문, 순서, 켜짐, `onRowClick`으로 드로어). 고른 것은 `search.params`를 열쇠로 두어 검색이 바뀌면 풀리고, 일괄 처리에 성공해도 푼다 (`016-banner-popup` 배너 목록과 같은 방식)
- [ ] T019 [US1] frontend/messages/ko.json `faq` 묶음: 화면 제목·머리 설명(FR-016 문구 그대로), 등록 단추, 칸 이름·안내, 켜짐 거르기, 일괄 처리 문구, 저장·삭제 토스트, 상한 안내, 빈 목록 안내

**Checkpoint**: FAQ 관리만으로 쓸 수 있다 (MVP)

---

## Phase 4: User Story 2 - 사용자가 FAQ에서 답을 찾는다 (Priority: P1)

**Goal**: 로그인하지 않은 사람도 `/support`에서 분류와 검색어로 FAQ를 찾고 답을 펼친다

**Independent Test**: 분류가 다른 FAQ 여럿을 켜 두고 로그인한 사용자와 로그인하지 않은 사람으로 분류 고르기와 검색을 해 본다 (spec US2)

### 백엔드

- [ ] T020 [US2] B/api/repository/reader/faq/FaqReader.java에 `findPublicList(categoryCodes, categoryCode, keyword)`: 켜져 있고 분류가 사용 중 항목(`categoryCodes`)인 것만, 질문 또는 답 포함 검색, 정렬은 T012와 같다
- [ ] T021 [US2] B/api/application/v1/faq/FaqApplication.java에 `listPublic(categoryCode, keyword)`(사용 중 분류는 `CodeApplication.getScreenCodes("SUPPORT_CATEGORY")`)와 B/api/controller/v1/faq/ApiFaqController.java(`GET /api/v1/faqs`, `@PermitAll`, 응답 `FaqPublicResponse` `{id, categoryCode, question, answer}`, contracts/api.md "사용자 FAQ")

### 프론트

- [ ] T022 [P] [US2] F/api/faq.ts에 `faqApi.list({ categoryCode, keyword })`
- [ ] T023 [US2] F/app/(user)/support/page.tsx: contracts/screens.md "고객센터 FAQ". 머리(제목 "고객센터", "1:1 문의하기" → `/support/inquiries/new`, 로그인했으면 "내 문의" → `/support/inquiries`), 분류 단추 줄("전체" + 사용 중 분류, T009), 검색어 칸, 둘 다 주소(`?category=&keyword=`)에 둔다. 질문 목록은 누르면 그 자리에서 답을 펼치고 다시 누르면 접는다(여러 개를 함께 펼칠 수 있다, `aria-expanded`). 답은 공지 상세와 같은 서식 글 표시 부품으로 보인다. 결과가 없으면 "찾는 답이 없나요?"와 "1:1 문의하기"
- [ ] T024 [US2] frontend/messages/ko.json `support` 묶음에 FAQ 화면 문구(제목, 단추, 분류 "전체", 검색어 칸, 빈 결과)

**Checkpoint**: 관리자가 켠 FAQ를 사용자가 찾아 읽는다

---

## Phase 5: User Story 3 - 회원이 1:1 문의를 남기고 답변을 본다 (Priority: P1)

**Goal**: 로그인한 회원이 이미지를 붙여 문의를 남기고, 내 문의에서 상태를 보고, 답변 대기 문의를 지운다

**Independent Test**: 회원으로 이미지를 붙인 문의를 남기고 목록·상세를 본 뒤 지운다. 다른 회원으로 그 문의를 열어 본다 (spec US3)

### 백엔드

- [ ] T025 [US3] B/api/entity/inquiry/Inquiry.java: `@Table(name = "tb_inquiry")`, `BaseEntity` 상속(하드 삭제), `OWNER_TYPE = "INQUIRY"`, `ANSWER_OWNER_TYPE = "INQUIRY_ANSWER"`. 칸은 data-model `tb_inquiry`대로. 첨부는 `@ElementCollection @CollectionTable(name = "tb_inquiry_image", joinColumns = @JoinColumn(name = "inquiry_id")) @OrderColumn(name = "sort_order") @Column(name = "image_url") List<String> imageUrls` (`AccessRole`의 `@ElementCollection` 참고). `status()`(`answered_at`으로 `InquiryStatus`), `isOwnedBy(userId)`
- [ ] T026 [P] [US3] B/api/dto/inquiry/InquiryStatus.java(`WAITING`, `ANSWERED`)와 목록 거르기 값 `InquiryStatusFilter`(`WAITING`·`ANSWERED`·`ALL`, 없으면 `WAITING`, research R6)
- [ ] T027 [P] [US3] B/api/repository/jpa/inquiry/InquiryRepository.java, B/api/repository/reader/inquiry/InquiryReader.java(`findMyPage(userId, pageable)`: 쓴 최신순 `created_at DESC, id DESC`), B/api/repository/writer/inquiry/InquiryWriter.java(만들기, 하드 삭제)
- [ ] T028 [US3] B/api/dto/inquiry/InquiryDto.java와 B/api/service/inquiry/InquiryService.java: `getMyPage`, `getMine(userId, id)`(남의 것·없는 것 모두 `NOT_FOUND` R001, research R5), `create`(분류 확인 T008, 이미지 주소마다 `/api/v1/rich-text/images/`로 시작하는지와 `RichTextImageService.load`로 있는지 확인, 없으면 V001 칸 오류 `imageUrls`. `016-banner-popup` T011의 이미지 확인과 같다), `deleteMine(userId, id)`(답변 완료면 `INVALID_STATE` B001)
- [ ] T029 [US3] B/api/application/v1/inquiry/InquiryApplication.java: 메서드마다 `@Transactional`. 쓰기·지우기·내 문의 앞에서 `MemberActivityPolicy.isManagerAccount(userId)`면 `FORBIDDEN` A005 (research R4). 만들기 뒤 `attach(Inquiry.OWNER_TYPE, id, html)`(html = 주소마다 `<img src="…">`), 지우기 뒤 `detach(OWNER_TYPE)`·`detach(ANSWER_OWNER_TYPE)` (research R1·R2·R5)
- [ ] T030 [US3] B/api/controller/v1/inquiry/ApiInquiryController.java(`/api/v1/inquiries`, 로그인 필요, `@AuthenticationPrincipal` 사용자는 `ApiNotificationController`와 같은 방식): contracts/api.md "회원 문의" 4개. 요청 `InquiryCreateRequest`(`categoryCode` `@NotBlank @Size(max = 50)`, `title` `@NotBlank @Size(max = 100)`, `content` `@NotBlank @Size(max = 2000)`, `imageUrls` `@NotNull @Size(max = 5) List<@NotBlank @Size(max = 100) String>`), 응답 `MyInquiryListResponse`, `MyInquiryResponse`(답변한 관리자는 넣지 않는다, `deletable` = 답변 대기). 목록 `page` 기본 0, `size` 기본 20 `@Min(1) @Max(100)`. POST 201 `{id}`, DELETE 204

### 프론트

- [ ] T031 [P] [US3] F/types/inquiry.ts(내 문의 목록 항목, 상세, 쓰기 요청, 관리 목록 항목·상세·검색), F/api/inquiry.ts(`inquiryApi`: `myList`, `get`, `create`, `remove`)
- [ ] T032 [US3] F/app/(user)/(signed-in)/support/inquiries/page.tsx: 업무 목록. 분류, 제목, 상태 배지(T009), 쓴 날짜. 좁은 폭은 줄 목록. 쪽 이동(`011-list-screen`). 비었으면 안내와 "1:1 문의하기"
- [ ] T033 [US3] F/app/(user)/(signed-in)/support/inquiries/new/page.tsx: 분류 선택, 제목(100자), 내용(2000자, 남은 글자 수), 이미지 붙이기(`accept="image/png,image/jpeg,image/gif,image/webp"`, 고를 때 5MB 앞 검사, `richTextApi.uploadImage`로 한 장씩 올리고 T009 작은 그림과 빼기. 다섯 장이면 붙이기 단추를 끄고 "이미지는 다섯 장까지 붙일 수 있어요."). 보내면 상세로 간다. V001 칸 오류는 그 칸 아래에. 바꾼 채 떠나면 묻는다
- [ ] T034 [US3] F/app/(user)/(signed-in)/support/inquiries/[id]/page.tsx: 분류·제목·상태·쓴 시각, 내용(`white-space: pre-wrap`), 첨부(T009, 누르면 크게), 답변이 있으면 "moa 운영팀"과 답변 시각·답변(서식 글 표시), `deletable`이면 "지우기"(확인 뒤 내 문의로). R001이면 "없는 문의예요."와 내 문의로 가는 단추
- [ ] T035 [US3] frontend/messages/ko.json `inquiry` 묶음의 회원 화면 문구(목록·쓰기·상세 제목, 칸 이름·안내, 장수 안내, 지우기 확인, "moa 운영팀", 없는 문의, 빈 목록)

**Checkpoint**: 회원이 문의를 남기고 자기 문의만 본다

---

## Phase 6: User Story 4 - 관리자가 문의에 답한다 (Priority: P1)

**Goal**: 운영 > 문의 관리에서 답변 대기 문의를 열어 답변하고, 처음 답변 때 회원에게 알림이 간다

**Independent Test**: 회원이 남긴 문의를 관리자로 열어 답변하고, 회원 쪽 상태·상세·알림을 본다 (spec US4. US3의 엔터티를 쓴다)

### 백엔드

- [ ] T036 [US4] B/api/repository/reader/inquiry/InquiryReader.java에 `findAdminPage(InquiryStatusFilter, categoryCode, from, to, keyword, pageable)`: 상태는 `answered_at IS NULL`/`IS NOT NULL`, 기간은 쓴 날짜(`011-list-screen`의 기간 꼴), `keyword`는 제목 포함 또는 작성자(`tb_user`) 아이디·이름 포함, 정렬 `created_at DESC, id DESC`, 따로 센 수로 `PageImpl`. 작성자 아이디·이름을 함께 읽는다. B/api/repository/jpa/inquiry/InquiryRepository.java에 잠금 읽기 `findByIdForUpdate`(`@Lock(PESSIMISTIC_WRITE)`, research R3)
- [ ] T037 [US4] B/api/service/inquiry/InquiryService.java에 `getAdminPage`, `getForAdmin(id)`(없으면 R001, 작성자·답변한 관리자 이름), `answer(id, content, adminUserId)`: 잠금 읽기 뒤 `answered_at`이 비었으면 답변·`answered_at`·`answered_by`를 넣고 "처음"을 돌려준다, 이미 있으면 답변만 바꾼다 (research R3)
- [ ] T038 [US4] B/api/application/v1/inquiry/InquiryApplication.java에 관리 메서드: `answer`는 한 트랜잭션에서 서비스 `answer` 뒤 `attach(ANSWER_OWNER_TYPE, id, content)`, "처음"이면 `notificationApplication.notify(문의 쓴 회원, NotificationType.INQUIRY_ANSWERED, "1:1 문의에 답변이 등록되었습니다", "/support/inquiries/" + id)` (spec FR-012)
- [ ] T039 [US4] B/api/controller/v1/admin/inquiry/ApiAdminInquiryController.java(`/api/v1/admin/inquiries`, `@RequirePermission(InquiryPermissions.MANAGE)`): contracts/api.md "관리 문의" 3개. 요청 `InquirySearchRequest`(`status` `InquiryStatusFilter`, `categoryCode`, `from`·`to` 날짜, `keyword` `@Size(max = 200)`), `InquiryAnswerRequest`(`content` `@SafeHtml @NotBlank @Size(max = 20000)`). 응답 `InquiryAdminListResponse`, `InquiryAdminResponse`(`answerable`은 지금 늘 참, US5에서 탈퇴를 본다). PUT 204

### 프론트

- [ ] T040 [P] [US4] F/api/inquiry.ts에 `inquiryAdminApi`: `list`, `get`, `answer(id, content)`
- [ ] T041 [US4] F/app/(admin)/admin/inquiries/inquiry-drawer.tsx: 넓은 `SideDrawer`. 분류·제목·작성자·쓴 시각, 내용(`pre-wrap`), 첨부(T009), 답변 `RichTextEditor`와 저장(답변 완료면 답변 시각·답변한 관리자와 "답변 고치기"). `useDrawerClose`. 저장 뒤 토스트와 목록 다시 읽기. R001이면 "없는 문의예요." 뒤 목록 다시 읽기 (spec Edge Cases)
- [ ] T042 [US4] F/app/(admin)/admin/inquiries/page.tsx: 업무 목록. `PageHeader`(제목 "문의 관리", 설명은 FR-016 문구, `<link>`로 `/admin/faqs`), `SearchPanel`(상태 `ChoiceFilter` 기본 `""` 답변 대기와 답변 완료·전체 `ALL`, 분류, 기간 빠른 단추, 검색어), `DataTable`(쪽, 열: 상태 배지, 분류, 제목, 작성자(이름·아이디), 쓴 시각, 답변 시각, `onRowClick`으로 드로어). 일괄 처리 줄은 두지 않는다
- [ ] T043 [US4] frontend/messages/ko.json `inquiry` 묶음의 관리 화면 문구(제목·머리 설명 FR-016 그대로, 거르기, 열 이름, 드로어 칸, 저장 토스트, 빈 목록). 알림 목록에서 `INQUIRY_ANSWERED` 종류를 보이는 문구가 필요하면 더한다

**Checkpoint**: 문의 → 답변 → 알림 흐름이 끝난다

---

## Phase 7: User Story 5 - 탈퇴한 회원의 문의를 1년 뒤 지운다 (Priority: P3) ⏸ 회원 탈퇴 스펙 뒤

**Goal**: 탈퇴 회원의 문의를 1년 보관하는 동안 답변을 막고, 1년이 지나면 지우고 파기 이력에 남긴다

**Independent Test**: 문의를 남긴 회원을 탈퇴시킨 뒤 관리 화면을 보고, 탈퇴 일시를 1년 넘게 앞당겨 정리기를 돌린다 (spec US5)

이 단계는 회원 탈퇴 스펙(`009-mypage`·`010-social-login`)이 탈퇴 상태·탈퇴 일시를 만든 뒤 한다. 그 전에는 건너뛰고 Phase 8로 간다. 시작할 때 research R10의 칸 이름을 그 스펙에 맞춘다.

- [ ] T044 [US5] 탈퇴 회원 문의: B/api/service/inquiry/InquiryService.java `answer`에서 작성자가 탈퇴 상태면 `INVALID_STATE` B001, `getForAdmin`의 `answerable`을 거짓으로. B/common/purgelog/PurgeTarget.java에 `INQUIRY`("1:1 문의"), B/api/scheduler/InquiryCleaner.java(`NotificationCleaner` 모양, 매일 새벽 `@Scheduled`, `app.inquiry.withdrawn-retention-days` 기본 365, 지난 문의마다 `detach` 두 번 뒤 행 삭제, `PersonalDataPurgedEvent.byScheduler(PurgeTarget.INQUIRY, count)`), backend/src/main/resources/application.yml 설정값, 파기 이력 화면의 대상 이름 문구, F/app/(admin)/admin/inquiries/inquiry-drawer.tsx의 "탈퇴한 회원의 문의라 답변할 수 없어요." 안내. `docs/privacy.md` 보관 기간 표의 상태 칸을 "구현됨 (`InquiryCleaner`)"로 바꾼다

---

## Phase 8: Polish & Cross-Cutting

- [ ] T045 PR 직전에 `git fetch` 뒤 `origin/dev`의 마지막 마이그레이션 번호와 `ErrorCode`의 `B3xx`를 다시 본다. 다른 기능이 `V12`나 `B301`을 썼으면 파일 이름·코드와 data-model.md·plan.md·contracts/api.md·research.md·이 tasks.md를 다음 번호로 바꾼다 (`docs/db.md`, CI `migration-numbers`). 같은 때 specs/README.md의 017 줄 상태를 "구현됨. 탈퇴 회원 문의 1년 보관(US5)은 회원 탈퇴 스펙 뒤"로 바꾼다
- [ ] T046 frontend/에서 local 백엔드를 띄운 채 `npm run gen:api`로 F/types/schema.d.ts를 다시 만들고, T016·T031의 손으로 쓴 타입 중 생성 타입으로 바꿀 수 있는 것을 바꾼다
- [ ] T047 backend/에서 `.\gradlew.bat spotlessApply`, `.\gradlew.bat build`(`ArchitectureTest` 포함), 한 번 `bootRun`으로 V12 적용과 `validate`를 확인한다(공유 local DB 주의: 이 브랜치를 쓰는 작업 사본에서만). frontend/에서 `npm run format`, `npm run check`
- [ ] T048 quickstart.md "화면 확인" 1~8을 사람이 직접 한다. 시험 계정 비밀번호가 필요하므로 에이전트는 하지 않는다 (원칙 VI)

---

## Phase 9: Tests (맨 끝, CLAUDE.md)

**Purpose**: AC마다 테스트 하나 (원칙 II), 이름에 AC 번호. 어느 AC를 API로, 어느 것을 e2e로 보는지는 research R13과 contracts/api.md의 AC 칸

- [ ] T049 backend/src/test/java/com/moa/api/CustomerCenterAcceptanceTest.java(`AcceptanceTest` 상속): US1-AC1~AC8, US2-AC1·AC2·AC4·AC5, US3-AC1·AC3(API는 `imageUrls` 여섯 개면 400)·AC4~AC8, US4-AC1·AC3~AC7을 `@DisplayName("US1-AC1 …")`으로 하나씩 둔다. 이미지는 `MockMultipartFile`로 rich-text 올리기를 거쳐 만든다. 일괄 처리에 없는 `id`를 섞으면 R001이고 아무것도 바뀌지 않는 경우, FAQ 200개 상한(B301), 사용 중이 아닌 분류(V001)도 하나씩 둔다. US4-AC4는 회원의 알림 목록 API로 본다. 역할 없는 운영자 토큰은 기존 테스트 도우미를 쓴다
- [ ] T050 backend/src/test/java/com/moa/api/AdminMenusAcceptanceTest.java: `001-admin-menus` FR-001·FR-007·FR-008 표를 고친 만큼만 고친다(운영 아래 FAQ 관리·문의 관리, 사용자 메뉴 고객센터, 연결 화면 `ADMIN_FAQS`·`ADMIN_INQUIRIES`·`SUPPORT`). `support/MenuSeed`가 새 마이그레이션을 읽는지 본다 (원칙 V)
- [ ] T051 backend/src/test/java/com/moa/api/AdminRolesAcceptanceTest.java: 권한 트리의 운영 하위 메뉴(사용자 관리, 공지 관리, FAQ 관리, 문의 관리, 접속 이력, 파기 이력)와 권한 키 목록에 `FAQ_MANAGE`·`INQUIRY_MANAGE`를 `001-admin-menus` FR-001 표만큼 더한다 (원칙 V)
- [ ] T052 backend/src/test/java/com/moa/api/AdminCodesAcceptanceTest.java의 그룹 기대 목록에 `SUPPORT_CATEGORY`를 `004-admin-codes` FR-001 표만큼 더한다. 테스트 정리(`DatabaseCleaner`)가 시드를 지우면 backend/src/test/java/com/moa/support/CodeSeed.java로 되돌린다(없으면 `MenuSeed`처럼 만든다, research R8)
- [ ] T053 [P] frontend/e2e/support/api.ts의 가짜 메뉴에 운영의 FAQ 관리·문의 관리와 사용자 메뉴 고객센터를 더하고, 메뉴 목록을 보는 e2e(`menu.spec.ts`, `role.spec.ts` 등)가 001·003 스펙과 맞는지 본다
- [ ] T054 [P] frontend/e2e/support.spec.ts: (1) 로그아웃한 채 `/support`에서 "1:1 문의하기" → 로그인 → 쓰기 화면으로 돌아온다(US3-AC2). 이미지 다섯 장을 붙이면 붙이기 단추가 꺼지고 안내(US3-AC3), 두 장으로 보낸다. 관리자가 문의 관리 드로어에서 이미지를 크게 보고(US4-AC2) 답변한다. 회원이 벨의 알림을 눌러 상세에서 "moa 운영팀" 답변을 본다. (2) 관리자가 FAQ를 등록하고 사용자 FAQ에서 분류·검색으로 찾고 펼치고 접는다(US2-AC3). 3폭, 가로 넘침·접근성, 밝은·어두운 화면은 다른 e2e와 같이
- [ ] T055 로컬에서는 `.\gradlew.bat build`, `npm run check`만 돌린다. `dbTest`와 e2e는 PR CI로 확인하고, 사용자가 요청할 때만 로컬에서 `.\gradlew.bat dbTest --tests "*CustomerCenterAcceptanceTest"`를 돌린다
- US5의 AC 세 개(US5-AC1~AC3)는 Phase 7을 할 때 T049의 클래스에 더한다

---

## Dependencies & Execution Order

- Phase 1 → Phase 2 → 스토리. T001은 모든 작업보다 먼저
- Phase 2 안: T003 → T004. T002·T005~T010은 서로 기대지 않는다
- US1(Phase 3)이 MVP. T011 → T012 → T013 → T014 → T015, 프론트 T016 → T017 → T018, T019는 화면 작업과 같이
- US2(Phase 4)는 US1의 엔터티·reader(T011·T012)에 기댄다. T020 → T021, T022 → T023
- US3(Phase 5)는 Phase 2만 끝나면 US1·US2와 따로 할 수 있다. T025 → T027 → T028 → T029 → T030, T026은 T028 앞. 프론트 T031 → T032·T033·T034
- US4(Phase 6)는 US3의 엔터티·서비스(T025~T028)에 기댄다. T036 → T037 → T038 → T039, T040 → T041 → T042
- US5(Phase 7)는 회원 탈퇴 스펙 뒤. 그 전에는 건너뛴다
- Phase 8(T046은 API가 다 생긴 뒤) → Phase 9(테스트) → PR. T045는 PR 바로 전

## Parallel Example

```text
Phase 2 안: T002, T003, T005, T006, T007, T008, T009, T010 (서로 다른 파일)
US1과 US3을 나란히: T011~T015 (FAQ 백엔드) 와 T025~T030 (문의 백엔드)
US1 안: T012 (백엔드 저장소)와 T016 (프론트 타입·API)
US3 안: T026, T027 (상태 값, 저장소)와 T031 (프론트 타입·API)
Phase 9 안: T053, T054
```

## Implementation Strategy

1. **MVP**: Phase 1 → 2 → 3(US1) → 4(US2). FAQ만으로 고객센터를 열 수 있다
2. US3(회원 문의) → US4(관리자 답변). 둘이 함께 있어야 문의가 쓸모 있으므로 함께 배포한다
3. Polish → 테스트(맨 끝) → PR. US5는 회원 탈퇴 스펙이 생기면 이 tasks의 Phase 7과 T049에 이어 붙인다
