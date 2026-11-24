---

description: "배너·팝업 관리 (banner-popup) 작업 목록"
---

# Tasks: 배너·팝업 관리 (banner-popup)

**Input**: `specs/016-banner-popup/`의 설계 문서

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/admin-api.md, contracts/admin-screens.md, quickstart.md

**Tests**: 원칙 II에 따라 AC마다 테스트를 하나 둔다. CLAUDE.md에 따라 기능 코드(백엔드·프론트)를 먼저 끝내고 테스트 작업은 맨 끝(Phase 6)에 둔다. 로컬에서는 빠른 검사(`.\gradlew.bat build`, `npm run check`)만 돌리고, DB 테스트와 e2e는 PR CI로 본다.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: 병렬 가능 (다른 파일, 의존 없음)
- **[Story]**: 작업이 속한 사용자 스토리 (US1 배너, US2 팝업)
- 경로는 저장소 루트 기준이다. 백엔드 패키지 `backend/src/main/java/com/moa/`는 `B/`, 프론트 `frontend/src/`는 `F/`로 줄여 쓴다

---

## Phase 1: Setup (번호 확인)

**Purpose**: 구현 직전에 겹칠 수 있는 번호를 다시 확인한다

- [ ] T001 구현 브랜치 `feat/016-banner-popup`을 최신 `dev`에서 딴 뒤 확인한다: backend/src/main/resources/db/migration의 마지막 버전(계획 때 `V10` → 이 기능 `V11`), B/api/screen의 `@Order` 값(계획 때 공지 60 → 이 기능 70), B/api/permission의 권한 키에 `BANNER_MANAGE`·`POPUP_MANAGE`가 없는지. 다르면 data-model.md, plan.md, 이 tasks.md의 번호를 먼저 고친다. 마이그레이션 번호는 PR 직전에 다시 본다(T031)

**Checkpoint**: 번호가 정해졌다

---

## Phase 2: Foundational (테이블·메뉴, 상태, 권한·연결 화면, 같이 쓰는 화면 부품)

**Purpose**: 배너(US1)와 팝업(US2)이 함께 쓰는 것. 이것이 끝나야 두 스토리를 시작한다

- [ ] T002 backend/src/main/resources/db/migration/V11__banner_popup.sql: 머리 주석 `-- moa: 배너·팝업 관리 테이블과 마케팅 메뉴. 스펙 specs/016-banner-popup (FR-001~FR-004)`. `docs/db.md`를 따른다(모든 열 COMMENT, `DEFAULT` 없음)
  - `tb_banner`: `id BIGINT NOT NULL AUTO_INCREMENT`, `name VARCHAR(100) NOT NULL`('배너 이름 (관리용)'), `wide_image_url VARCHAR(100) NOT NULL`('넓은 화면 이미지 주소'), `narrow_image_url VARCHAR(100) NULL`('좁은 화면 이미지 주소 (비면 넓은 화면 이미지)'), `alt_text VARCHAR(200) NOT NULL`('대체 문구'), `link_url VARCHAR(500) NULL`('연결 주소'), `starts_at DATETIME(6) NOT NULL`, `ends_at DATETIME(6) NULL`, `sort_order INT NOT NULL`('순서 (작을수록 먼저)'), `enabled BOOLEAN NOT NULL`, 감사 열 6개(`created_at`·`updated_at`·`created_by`·`updated_by`·`deleted_at`·`deleted_by`), 인덱스 `idx_banner_deleted_at_sort_order_starts_at`, `idx_banner_enabled_starts_at_ends_at`
  - `tb_popup`: `title VARCHAR(200) NOT NULL`, `content MEDIUMTEXT NOT NULL`('팝업 내용 (걸러진 HTML)'), `link_url VARCHAR(500) NULL`, `starts_at`·`ends_at`·`sort_order`·`enabled`와 감사 열은 배너와 같다. 인덱스 `idx_popup_deleted_at_sort_order_starts_at`, `idx_popup_enabled_starts_at_ends_at`
  - 메뉴(넣기만, 기존 행은 고치지 않는다): 상위 메뉴 "마케팅"(`parent_id` NULL, `screen_key` NULL, `icon` `megaphone`, `sort_order` 15, `enabled` TRUE, `audience` `ADMIN`, `visibility` NULL)을 넣고 `SET @marketing_id = LAST_INSERT_ID()`로 하위 "배너 관리"(`ADMIN_BANNERS`, `folder`, 10), "팝업 관리"(`ADMIN_POPUPS`, `file`, 20)를 넣는다. 감사 열은 `V6__purge_log.sql`의 메뉴 넣기와 같은 모양
- [ ] T003 [P] B/api/dto/marketing/ExposureStatus.java: `SCHEDULED`·`ACTIVE`·`ENDED`·`DISABLED`와 `static ExposureStatus of(boolean enabled, LocalDateTime startsAt, @Nullable LocalDateTime endsAt, LocalDateTime now)` (꺼짐 → `DISABLED`, 지금 < 시작 → `SCHEDULED`, 끝 ≤ 지금 → `ENDED`, 그 밖 `ACTIVE`, data-model "상태"). 같은 패키지에 목록 거르기 값 `ExposureStatusFilter`(`SCHEDULED`·`ACTIVE`·`ENDED`·`DISABLED`·`ALL`, 없으면 끝남 빼고, research R4)
- [ ] T004 [P] B/api/permission/BannerPermissions.java(`MANAGE = "BANNER_MANAGE"`, `PermissionDefinition(MANAGE, "배너 관리", "마케팅")`)와 B/api/permission/PopupPermissions.java(`POPUP_MANAGE`, "팝업 관리", "마케팅"). `NoticePermissions`와 같은 모양
- [ ] T005 B/api/screen/MarketingScreens.java: `@Component @Order(70)`, `ScreenDefinition.admin("ADMIN_BANNERS", "배너 관리", "/admin/banners", BannerPermissions.MANAGE)`, `ScreenDefinition.admin("ADMIN_POPUPS", "팝업 관리", "/admin/popups", PopupPermissions.MANAGE)` (T004 뒤)
- [ ] T006 [P] F/components/marketing/exposure-period-fields.tsx: 시작 날짜·시각(필수), 끝 날짜·시각(선택)을 키트 `DatePicker` + `TimeInput`으로 받아 `2026-12-01T10:00:00+09:00` 꼴의 문자열로 내고, 받은 `OffsetDateTime`을 `SITE.timeZone`으로 풀어 채운다 (research R5). 같은 곳에 F/components/marketing/exposure-status.tsx(`StatusText`로 노출 중은 긍정, 꺼짐·끝남은 흐림, 예정은 보통)
- [ ] T007 [P] F/lib/marketing.ts: `isAllowedLinkUrl`(비거나, `/`로 시작하되 `//`로 시작하지 않거나, `http://`·`https://`로 시작, 500자 이하, research R10), `toOffsetDateTime`·`fromOffsetDateTime`(T006이 쓴다), 상태 거르기 선택지(끝남 빼고 `""`, `SCHEDULED`, `ACTIVE`, `ENDED`, `DISABLED`, `ALL`)
- [ ] T008 [P] frontend/messages/ko.json `marketing` 묶음: 상태 이름 `statuses.<값>`, 상태 거르기 이름(끝남 빼고, 전체), 노출 기간 칸 이름과 오류(끝이 시작보다 앞), 연결 주소 칸 이름과 오류, 순서·켜짐 칸 이름. `npm run check`의 문구 검사를 통과하게 둔다

**Checkpoint**: 테이블·메뉴·권한·연결 화면과 같이 쓰는 부품이 준비됐다

---

## Phase 3: User Story 1 - 관리자가 배너를 등록하고 기간과 순서를 관리한다 (Priority: P1) 🎯 MVP

**Goal**: 마케팅 > 배너 관리에서 이미지 배너를 등록·수정·끄기·삭제하고, 미리 보고, 여러 개를 한꺼번에 켜고 끄고 지운다

**Independent Test**: 관리자로 배너를 등록·수정·끄기·삭제하고, 목록의 상태가 기간과 켜짐에 맞게 바뀌는지 본다 (spec US1)

### 백엔드

- [ ] T009 [US1] B/api/entity/banner/Banner.java: `@Table(name = "tb_banner")`, `@SQLRestriction(SoftDeleteEntity.NOT_DELETED)`, `SoftDeleteEntity` 상속, `public static final String OWNER_TYPE = "BANNER"`. 칸은 data-model `tb_banner`대로. `modify(...)`(모든 칸), `changeEnabled(boolean)`
- [ ] T010 [P] [US1] B/api/repository/jpa/banner/BannerRepository.java(`SoftDeleteRepository<Banner, Long>`), B/api/repository/reader/banner/BannerReader.java(QueryDSL `findAdminPage(keyword, ExposureStatusFilter, now, pageable)`: 이름 포함, 상태 조건은 T003과 같은 식, 값이 없으면 `ENDED`만 빼기, 정렬 `sort_order ASC, starts_at DESC, id DESC`, 따로 센 수로 `PageImpl`. `findAllByIdIn`), B/api/repository/writer/banner/BannerWriter.java (`NoticeReader`·`NoticeWriter`와 같은 모양)
- [ ] T011 [US1] B/api/dto/banner/BannerDto.java(`from(Banner, now)`로 상태 포함)와 B/api/service/banner/BannerService.java: `page`, `get`(없으면 `NOT_FOUND` R001), `create`, `modify`, `delete`, `setEnabled(ids, enabled)`, `deleteAll(ids)`. 일괄은 `requireAll(ids)`로 모든 `id`가 있는지 먼저 보고(하나라도 없으면 R001) 바꾼다(`MenuService` line 133·143·197과 같은 모양). 이미지 확인: 주소가 `/api/v1/rich-text/images/`로 시작해야 하고, 그 뒤 `publicId`로 `RichTextImageService.load`를 불러 `R001`이면 `V001`로 바꾸고, `contentType`이 `image/gif`면 `V001`("JPG, PNG, WebP 이미지만 쓸 수 있습니다.") (research R1, R10). 이미지의 주인은 확인하지 않는다
- [ ] T012 [US1] B/api/application/v1/banner/BannerApplication.java: 메서드마다 `@Transactional`. 만들기·고치기 뒤 `richTextImageService.attach(Banner.OWNER_TYPE, id, html)`(html = 넓은 화면·좁은 화면 주소의 `<img src="…">` 조각), 지우기·일괄 지우기 뒤 배너마다 `detach`. 지금 시각은 한 곳에서 얻어 서비스에 넘긴다 (research R1, R3, R7)
- [ ] T013 [US1] B/api/controller/v1/admin/banner/ApiAdminBannerController.java(`/api/v1/admin/banners`, 클래스에 `@RequirePermission(BannerPermissions.MANAGE)`, `@Tag`·`@Operation`): contracts/admin-api.md "배너 관리 API" 7개. 요청 B/api/controller/v1/admin/banner/request/: `BannerSaveRequest`(`name` `@NotBlank @Size(max = 100)`, `wideImageUrl` `@NotBlank @Size(max = 100)`, `narrowImageUrl` `@Nullable @Size(max = 100)`, `altText` `@NotBlank @Size(max = 200)`, `linkUrl` `@Nullable @Size(max = 500)` + 허용 주소 `@Pattern`(research R10), `startsAt` `@NotNull OffsetDateTime`, `endsAt` `@Nullable OffsetDateTime`(있으면 `startsAt`보다 뒤, 어기면 칸 `endsAt`의 V001), `sortOrder` `@NotNull @Min(0) @Max(9999)`, `enabled` `@NotNull`), `BannerSearchRequest`(`keyword` `@Size(max = 200)`, `status` `ExposureStatusFilter`), `BannerIdsRequest`(`@NotEmpty @Size(max = 200) List<@NotNull Long> ids`), `BannerEnabledRequest`(같은 `ids` + `@NotNull Boolean enabled`). 응답 response/: `BannerAdminListResponse`, `BannerAdminResponse`(일시는 `ApiTimes.of`). 목록 `page` 기본 0, `size` 기본 20 `@Min(1) @Max(100)`. POST 201, PUT 200, DELETE·일괄 204. 교차 칸 검증은 기존 요청에 같은 방식이 있으면 그것을 따른다

### 프론트

- [ ] T014 [P] [US1] F/types/banner.ts(목록 항목, 상세, 저장 요청, 상태 거르기), F/api/banner.ts(`bannerAdminApi`: `list`, `get`, `create`, `modify`, `remove`, `setEnabled(ids, enabled)`, `bulkDelete(ids)`). 이미지 올리기는 `richTextApi.uploadImage(file)`을 쓴다 (`noticeAdminApi`와 같은 모양)
- [ ] T015 [P] [US1] F/app/(admin)/admin/banners/banner-preview.tsx: 넓은 화면 틀(가로 4:1)과 좁은 화면 틀(휴대폰 폭, 2:1)에 이미지를 `object-cover`로 나란히. 좁은 화면 이미지가 없으면 좁은 틀에 넓은 화면 이미지와 "좁은 화면 이미지가 없어 넓은 화면 이미지를 써요" 안내. 이미지가 하나도 없으면 빈 틀과 안내 (US1-AC10, research R2)
- [ ] T016 [US1] F/app/(admin)/admin/banners/banner-drawer.tsx: `SideDrawer` + react-hook-form. 칸은 contracts/admin-screens.md "배너 드로어" 표대로(이름, 넓은·좁은 화면 이미지 파일 고르기 `accept="image/jpeg,image/png,image/webp"`와 5MB 앞 검사·권장 크기 1200×300·720×360 안내, 미리보기 T015, 대체 문구, 연결 주소 T007 검사, 노출 기간 T006, 순서 기본 0, 켜짐 기본 켜짐). 저장 실패의 V001 칸 오류를 그 칸 아래에 보인다. 고칠 때 아래 줄에 삭제(확인 창)와 저장, 등록할 때 취소와 등록. `useDrawerClose`로 바꾼 채 닫으면 묻는다
- [ ] T017 [US1] F/app/(admin)/admin/banners/page.tsx: `ListPage`·`PageHeader`(제목 "배너 관리", 설명은 `t.rich`의 `<link>`로 `/admin/notices`, 오른쪽 "배너 등록" 단추가 빈 드로어를 연다), `SearchPanel`(상태 `ChoiceFilter` 기본 `""` 끝남 빼고, 이름 `KeywordFilter`, `useListSearch`), `BulkToolbar`(늘 그린다. 고른 것이 없으면 안내와 꺼진 단추, 고르면 "N개 선택"과 켜기·끄기·삭제. 삭제만 `confirm({ destructive: true })`), `DataTable`(`selection`의 `rowLabel` = 이름, 열: 작은 넓은 화면 이미지, 이름, 기간, 순서, 상태(T006), `onRowClick`으로 드로어). 고른 것은 `search.params`를 열쇠로 두어 쪽·검색이 바뀌면 풀리고, 일괄 처리에 성공해도 푼다(사용자 관리 일괄 삭제와 같은 방식, research R7). 처리 뒤 토스트와 목록 다시 읽기
- [ ] T018 [US1] frontend/messages/ko.json `banner` 묶음: 화면 제목·머리 설명(FR-010 문구 그대로), 등록 단추, 칸 이름·안내(권장 크기, 형식·5MB), 미리보기 안내, 일괄 처리 문구(`bulkToolbar`, `bulkHint`, `bulkSelected`, `bulkEnabled`, `bulkDisabled`, `bulkRemoved`, `bulkRemoveTitle`, `bulkRemoveDescription`), 저장·삭제 토스트, 빈 목록 안내

**Checkpoint**: 배너 관리만으로 쓸 수 있다 (MVP)

---

## Phase 4: User Story 2 - 관리자가 팝업을 등록하고 관리한다 (Priority: P2)

**Goal**: 마케팅 > 팝업 관리에서 서식 있는 글 팝업을 등록·수정·끄기·삭제하고, 여러 개를 한꺼번에 켜고 끄고 지운다

**Independent Test**: 관리자로 팝업을 등록·수정·끄기·삭제하고, 목록의 상태가 기간과 켜짐에 맞게 바뀌는지 본다 (spec US2)

### 백엔드

- [ ] T019 [US2] B/api/entity/popup/Popup.java: `@Table(name = "tb_popup")`, `OWNER_TYPE = "POPUP"`, 칸은 data-model `tb_popup`대로. `modify(...)`, `changeEnabled(boolean)` (T009와 같은 모양)
- [ ] T020 [P] [US2] B/api/repository/jpa/popup/PopupRepository.java, B/api/repository/reader/popup/PopupReader.java(제목 포함 검색, 나머지는 T010과 같다), B/api/repository/writer/popup/PopupWriter.java
- [ ] T021 [US2] B/api/dto/popup/PopupDto.java와 B/api/service/popup/PopupService.java: T011과 같은 메서드와 `requireAll`. 이미지 확인은 없다(본문 이미지는 rich-text가 맡는다)
- [ ] T022 [US2] B/api/application/v1/popup/PopupApplication.java: 만들기·고치기 뒤 `attach(Popup.OWNER_TYPE, id, content)`, 지우기·일괄 지우기 뒤 `detach` (research R8)
- [ ] T023 [US2] B/api/controller/v1/admin/popup/ApiAdminPopupController.java(`/api/v1/admin/popups`, `@RequirePermission(PopupPermissions.MANAGE)`): contracts/admin-api.md "팝업 관리 API" 7개. 요청 `PopupSaveRequest`(`title` `@NotBlank @Size(max = 200)`, `content` `@SafeHtml @NotBlank @Size(max = 20000)`, `linkUrl`·`startsAt`·`endsAt`·`sortOrder`·`enabled`는 T013과 같다), `PopupSearchRequest`, `PopupIdsRequest`, `PopupEnabledRequest`, 응답 `PopupAdminListResponse`, `PopupAdminResponse`

### 프론트

- [ ] T024 [P] [US2] F/types/popup.ts, F/api/popup.ts(`popupAdminApi`, T014와 같은 메서드)
- [ ] T025 [US2] F/app/(admin)/admin/popups/popup-drawer.tsx: 넓은 `SideDrawer`(`className`으로 폭을 넓힌다, research R9). 칸은 contracts/admin-screens.md "팝업 드로어" 표대로. 내용은 `RichTextEditor`를 react-hook-form `Controller`에 잇는다(`notice-form.tsx`와 같다). 편집기의 작은 창(링크 넣기 등)이 드로어 안에서 제대로 열리는지 본다. 안 되면 멈추고 보고한다(spec FR-008을 먼저 고쳐야 한다)
- [ ] T026 [US2] F/app/(admin)/admin/popups/page.tsx: T017과 같은 목록(검색은 제목, 열은 제목·기간·순서·상태, `rowLabel` = 제목, 머리 설명은 FR-010 팝업 문구)
- [ ] T027 [US2] frontend/messages/ko.json `popup` 묶음: T018과 같은 항목(팝업 문구)

**Checkpoint**: 배너와 팝업 관리가 함께 동작한다

---

## Phase 5: Polish & Cross-Cutting

- [ ] T028 frontend/에서 local 백엔드를 띄운 채 `npm run gen:api`로 F/types/schema.d.ts를 다시 만들고, T014·T024의 손으로 쓴 타입 중 생성 타입으로 바꿀 수 있는 것을 바꾼다
- [ ] T029 backend/에서 `.\gradlew.bat spotlessApply`, `.\gradlew.bat build`(`ArchitectureTest` 포함), 한 번 `bootRun`으로 V11 적용과 `validate`를 확인한다(공유 local DB 주의: 이 브랜치를 쓰는 작업 사본에서만). frontend/에서 `npm run format`, `npm run check`
- [ ] T030 quickstart.md "화면 확인" 1~7을 사람이 직접 한다. 시험 계정 비밀번호가 필요하므로 에이전트는 하지 않는다 (원칙 VI)
- [ ] T031 PR 직전에 `git fetch` 뒤 `origin/dev`의 마지막 마이그레이션 번호를 다시 본다. 다른 기능이 `V11`을 썼으면 파일 이름과 data-model.md·plan.md·이 tasks.md의 번호를 다음 번호로 바꾼다 (`docs/db.md`, CI `migration-numbers`). 같은 때 specs/README.md의 016 줄 상태를 "구현됨. 사용자 화면 노출은 자리가 정해진 뒤"로 바꾼다

---

## Phase 6: Tests (맨 끝, CLAUDE.md)

**Purpose**: AC마다 테스트 하나 (원칙 II), 이름에 AC 번호. 어느 AC를 API로, 어느 것을 e2e로 보는지는 research R11과 contracts/admin-api.md "AC와 확인 방법"

- [ ] T032 backend/src/test/java/com/moa/api/BannerPopupAcceptanceTest.java(`AcceptanceTest` 상속): US1-AC1~AC9, AC11~AC14(API 쪽), US2-AC1~AC8을 `@DisplayName("US1-AC1 …")`으로 하나씩 둔다. 이미지는 `MockMultipartFile`로 rich-text 올리기를 거쳐 만든다(PNG, GIF, 상한을 넘는 파일). 상태는 시작·끝 일시를 과거·미래로 넣어 만든다. 일괄 처리에 없는 `id`를 섞으면 R001이고 아무것도 바뀌지 않는 경우도 하나 둔다. 역할 없는 운영자 토큰은 기존 테스트 도우미를 쓴다
- [ ] T033 backend/src/test/java/com/moa/api/AdminMenusAcceptanceTest.java: `001-admin-menus` FR-001 표와 US1-AC1을 먼저 고친 만큼만 고친다(상위 메뉴 운영·마케팅·설정, 마케팅 아래 배너 관리·팝업 관리, 연결 화면 목록에 `ADMIN_BANNERS`·`ADMIN_POPUPS`). `support/MenuSeed`가 새 마이그레이션을 읽는지 본다 (원칙 V)
- [ ] T034 [P] frontend/e2e/support/api.ts의 가짜 메뉴에 마케팅 묶음을 더하고, 메뉴 목록을 확인하는 e2e(`menu.spec.ts` 등)가 001 스펙과 맞는지 본다
- [ ] T035 [P] frontend/e2e/banners.spec.ts: (1) 배너를 이미지와 함께 등록한다 — 순서 0·켜짐이 기본값(US1-AC14), 넓은 화면 이미지만 올렸을 때 좁은 틀에 넓은 이미지와 안내(US1-AC10), 끝 일시를 시작보다 앞으로 넣으면 그 칸 아래 이유(US1-AC7). (2) 목록에서 둘을 골라 끄고, 다시 골라 지운다 — 처리 뒤 고른 것이 풀린다(US1-AC11·AC12). 3폭, 가로 넘침·접근성, 밝은·어두운 화면은 다른 관리 화면 e2e와 같이
- [ ] T036 [P] frontend/e2e/popups.spec.ts: 팝업을 편집기로 써서 등록하고, 목록에서 둘을 골라 지운다 — 고른 것이 풀린다(US2-AC5). 넓은 드로어가 3폭에서 넘치지 않는다
- [ ] T037 [P] frontend/tests/unit/marketing.test.ts: `isAllowedLinkUrl`(`/a` 통과, `//evil.example`·`javascript:alert(1)`·`ftp://x` 막힘, 501자 막힘), `toOffsetDateTime`·`fromOffsetDateTime`(한국 시간 왕복)
- [ ] T038 backend/src/test/java/com/moa/api/AdminRolesAcceptanceTest.java: 상위 메뉴를 `containsExactly("운영", "설정")`로 보는 곳(계획 때 77·103·265줄)과 권한 트리의 하위 메뉴·권한 키 목록(105·107줄)을 `001-admin-menus` FR-001 표와 `003-admin-roles` US3-AC4(마케팅 더함)만큼 고친다. 하위 메뉴 순서는 운영(사용자 관리, 공지 관리, 접속 이력, 파기 이력), 마케팅(배너 관리, 팝업 관리), 설정(권한 관리, 메뉴 관리, 코드 관리), 권한 키는 `BANNER_MANAGE`·`POPUP_MANAGE`가 파기 이력 다음에 온다 (원칙 V)
- [ ] T039 [P] frontend/e2e/role.spec.ts: 가짜 권한 트리(계획 때 37줄 근처)에 마케팅 묶음과 배너 관리·팝업 관리를 더하고, US3-AC4 흐름(마케팅·설정 접힘, "0/2"·"0/3")이 스펙과 맞는지 본다
- [ ] T040 로컬에서는 `.\gradlew.bat build`, `npm run check`만 돌린다. `dbTest`와 e2e는 PR CI로 확인하고, 사용자가 요청할 때만 로컬에서 `.\gradlew.bat dbTest --tests "*BannerPopupAcceptanceTest"`를 돌린다

---

## Dependencies & Execution Order

- Phase 1 → Phase 2 → 스토리. T001은 모든 작업보다 먼저
- Phase 2 안: T004 → T005. T002·T003·T006·T007·T008은 서로 기대지 않는다
- 스토리 작업이 기대는 공통 작업: 저장소·서비스(T010·T011·T020·T021)는 T003(상태), 컨트롤러(T013·T023)는 T004(권한), 화면(T016·T017·T025·T026)은 T006~T008
- US1(Phase 3)이 MVP. 백엔드 T009 → T010 → T011 → T012 → T013, 프론트 T014·T015 → T016 → T017, T018은 화면 작업과 같이
- US2(Phase 4)는 Phase 2만 끝나면 US1과 따로 할 수 있다. 다만 같은 모양이라 US1 뒤에 하면 빠르다. T019 → T020 → T021 → T022 → T023, T024 → T025 → T026
- Phase 5(T028은 두 스토리의 API가 다 생긴 뒤) → Phase 6(테스트) → PR. T031은 PR 바로 전

## Parallel Example

```text
Phase 2 안: T002, T003, T004, T006, T007, T008 (서로 다른 파일)
US1 안: T010 (백엔드 저장소)와 T014, T015 (프론트 타입·미리보기)
US2 안: T020 (백엔드 저장소)와 T024 (프론트 타입)
Phase 6 안: T034, T035, T036, T037, T039
```

## Implementation Strategy

1. **MVP**: Phase 1 → 2 → 3(US1). 배너 관리만으로 배포를 검토할 수 있다(사용자 화면 노출은 없다, FR-011)
2. US2(팝업 관리)
3. Polish → 테스트(맨 끝) → PR. 사용자 화면 노출은 노출 자리가 정해질 때 spec에 사용자 스토리를 더하고 이 tasks에 이어 붙인다
