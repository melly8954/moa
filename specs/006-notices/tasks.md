---

description: "공지사항 (notices) 작업 목록"
---

# Tasks: 공지사항 (notices)

**Input**: `specs/006-notices/`의 설계 문서

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/notices-api.md, contracts/notices-screens.md

**Tests**: 원칙 II에 따라 AC마다 API 통합 테스트를 하나 둔다 (`NoticesAcceptanceTest`, `@DisplayName("US1-AC1 ...")`). 프론트는 단위 테스트와 핵심 흐름 e2e만 둔다 (research.md R10). US4는 `007-notifications` 뒤에 한다 (research.md R9).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 가능 (다른 파일, 의존 없음)
- **[Story]**: 작업이 속한 사용자 스토리
- 경로는 저장소 루트 기준이다. 키트 파일은 `C:\psw\github\psw-dev-kit`의 태그에서 `git show <태그>:<경로>`로 꺼낸다

## Phase 1: Setup (rich-text 헬퍼 얹기)

**Purpose**: 키트 rich-text 헬퍼를 백엔드(`springboot/v0.3.1`)와 프론트(`web-next/v0.4.0`)에 손으로 얹는다 (research.md R1). 키트 파일 내용은 바꾸지 않는다

- [ ] T001 [P] 키트 `springboot/v0.3.1`의 `backend/springboot/helpers/rich-text/files/` 아래 파일을 backend/에 같은 경로로 옮기고, 경로와 내용의 `com.example.psw` · `com/example/psw`를 `com.moa` · `com/moa`로 바꾼다 (main 코드 `common/helper/richtext/` 13개, `api/application/v1/richtext/`, `api/controller/v1/richtext/`, 마이그레이션 `V1_9__rich_text_images.sql`, 테스트 `RichTextImageAcceptanceTest`, `HtmlSanitizerTest`)
- [ ] T002 [P] 키트 `helper.gradle`을 backend/gradle/helpers/rich-text.gradle로 옮기고 (`org.jsoup:jsoup:1.23.1`), backend/build.gradle이 `gradle/helpers/*.gradle`을 읽는지 확인한다 (다른 헬퍼와 같은 방식)
- [ ] T003 [P] 키트 `application.yaml` 조각(`app.rich-text.*`, `spring.servlet.multipart`)을 backend/src/main/resources/application.yaml 끝에 다른 헬퍼 조각과 같은 모양의 문서로 덧붙인다. 값은 그대로 둔다
- [ ] T004 backend/에서 `./gradlew spotlessApply`, `./gradlew build`, `./gradlew dbTest --tests "*RichTextImageAcceptanceTest"`로 헬퍼가 키트 테스트대로 도는지 확인한다
- [ ] T005 [P] 키트 `web-next/v0.4.0`의 `frontend/web/next/helpers/rich-text/files/` 아래 파일을 frontend/에 같은 경로로 옮긴다 (`src/api/rich-text.ts`, `src/types/rich-text.ts`, `src/lib/rich-text.ts`, `src/components/rich-text/` 5개, `src/app/(dev)/dev/rich-text/`, `tests/unit/rich-text.test.ts`, `e2e/rich-text.spec.ts`). 줄끝은 LF로 둔다
- [ ] T006 [P] 키트 `helper.json`의 의존성 6개(`@tiptap/react`·`@tiptap/pm`·`@tiptap/starter-kit`·`@tiptap/extension-image`·`@tiptap/extensions` `^3.32.2`, `dompurify` `^3.4.17`)를 frontend/에서 `npm install`로 더한다
- [ ] T007 frontend/src/api/index.ts의 `// @helpers:imports` 위에 `import { richTextApi } from "./rich-text"`, `// @helpers:api` 위에 `richText: richTextApi,`를 끼운다 (`helper.json` inserts)
- [ ] T008 키트 `messages.ko.json`의 `richText`를 frontend/messages/ko.json에 더한다
- [ ] T009 frontend/src/components/shell/sidebar-shell.tsx를 키트 `web-next/v0.4.0`의 `skeleton/src/components/shell/sidebar-shell.tsx`와 같게 맞춘다 (차이는 `<nav>` 위 주석 한 줄)
- [ ] T010 backend/.psw-dev-kit의 `helpers`에 `rich-text`를 더하고, frontend/.psw-dev-kit의 `tag`를 `web-next/v0.4.0`으로, `helpers`에 `rich-text`를 더한다. .specify/memory/constitution.md "기술 제약"의 프론트 버전을 `web-next/v0.4.0`으로 고치고 버전을 1.1.3(PATCH), Last Amended를 고친 날로 바꾼다. CLAUDE.md의 키트 사용법 줄도 `web-next/v0.4.0`으로 고친다
- [ ] T011 frontend/에서 `npm run check`, `npm run test:e2e -- rich-text`로 헬퍼가 키트 검사대로 도는지 확인한다

**Checkpoint**: `/dev/rich-text` 본보기와 `POST /api/v1/rich-text/images`가 moa에서 돈다

---

## Phase 2: Foundational (공지 데이터·권한·메뉴·공개 조회)

**Purpose**: 모든 스토리가 쓰는 테이블, 엔터티, 권한, 메뉴, 공개 조회 API, 테스트 틀

**⚠️ CRITICAL**: US1~US3 모두 이 단계에 기댄다

- [ ] T012 backend/src/main/resources/db/migration/V2_1__notices.sql을 만든다. `notices`: `id` BIGINT 자동 증가, `title` VARCHAR(200) NOT NULL, `content` MEDIUMTEXT NOT NULL, `status` VARCHAR(20) NOT NULL (`DRAFT`·`PUBLISHED`), `pinned` BOOLEAN NOT NULL 기본 FALSE, `published_at` DATETIME(6) NULL, 감사 필드(`created_at`·`updated_at`·`created_by`·`updated_by`), `deleted_at`·`deleted_by`, 인덱스 `(status, deleted_at, pinned, published_at)`. `notice_reads`: `user_id` BIGINT PK, `last_seen_at` DATETIME(6) NOT NULL. 모양은 V1_4·V1_8과 같게 (InnoDB, utf8mb4_unicode_ci) (data-model.md)
- [ ] T013 backend/src/main/resources/db/migration/V2_2__notice_admin_menu.sql을 만든다. `menus`에 `id` 7, `parent_id` NULL, 이름 '공지 관리', `path` '/admin/notices', `icon` 'clipboard', `sort_order` 50, `enabled` TRUE, `permission` 'NOTICE_MANAGE'를 INSERT한다 (V1_7_1과 같은 칸 순서, 감사 값 1) (research.md R5)
- [ ] T014 [P] backend/src/main/java/com/moa/api/permission/NoticePermissions.java를 만든다. `MANAGE = "NOTICE_MANAGE"`, `PermissionDefinition(MANAGE, "공지 관리", "공지")` (CodePermissions와 같은 모양)
- [ ] T015 [P] backend/src/main/java/com/moa/api/entity/notice/에 `NoticeStatus`(DRAFT, PUBLISHED), `Notice`(`SoftDeleteEntity` 상속, `@SQLRestriction(SoftDeleteEntity.NOT_DELETED)`, 칸은 T012와 같다), `NoticeRead`(`user_id` PK, `last_seen_at`)를 만든다. `Notice`에 `publish(now)`(DRAFT일 때만, 아니면 B001 `INVALID_STATE`), `modify(title, content)`, `pin(boolean)` 메서드를 둔다. `published_at`은 `publish`에서만 넣는다 (data-model.md 상태 전이)
- [ ] T016 backend/src/main/java/com/moa/api/repository/{jpa,reader,writer}/notice/에 `NoticeRepository`, `NoticeReadRepository`, `NoticeReader`, `NoticeWriter`를 code 헬퍼와 같은 계층으로 만든다. 공개 목록 조회는 `status = PUBLISHED`, 정렬 `pinned DESC, published_at DESC, id DESC`, 페이지 (FR-005)
- [ ] T017 backend/src/main/java/com/moa/api/dto/notice/와 backend/src/main/java/com/moa/api/application/v1/notice/NoticeApplication.java에 공개 조회(`getPublishedPage`, `getPublished(id)`: 게시하지 않았거나 없으면 R001)를 둔다
- [ ] T018 backend/src/main/java/com/moa/api/controller/v1/notice/ApiNoticeController.java를 만든다. `@PermitAll`, `GET /api/v1/notices`(PageResponse {id, title, pinned, publishedAt}), `GET /api/v1/notices/{id}`({id, title, content, pinned, publishedAt}) (contracts/notices-api.md 공개 API)
- [ ] T019 backend/src/test/java/com/moa/support/MenuSeed.java의 `SCRIPTS`에 `db/migration/V2_2__notice_admin_menu.sql`을 더한다
- [ ] T020 backend/src/test/java/com/moa/api/AdminMenusAcceptanceTest.java를 `001-admin-menus` spec의 바뀐 FR-001(메뉴 6개)대로 고친다: US1-AC1 기대 이름 끝에 "공지 관리", US1-AC2·US2-AC2·US2-AC3의 목록 API에 `/api/v1/admin/notices`를 더하고 DisplayName의 5개를 6개로 (원칙 V: 스펙이 먼저 바뀌었다)
- [ ] T021 backend/src/test/java/com/moa/api/NoticesAcceptanceTest.java를 `AcceptanceTest`를 상속해 만든다. 클래스 주석에 스펙 `006-notices`를 적는다. 도우미: ADMIN `bearer(2L, "ADMIN")`, 역할 없는 운영자 `bearer(3L, "USER")`, 공지 만들기(`POST /api/v1/admin/notices`)와 게시
- [ ] T022 [P] frontend/src/types/notice.ts와 frontend/src/api/notice.ts를 만들고 frontend/src/api/index.ts의 `api.v1`에 `notice`(공개 목록·상세, 새 공지·확인)와 `noticeAdmin`(관리 7개)을 더한다 (contracts/notices-api.md). 백엔드가 떠 있으면 `npm run gen:api`로 타입을 맞춘다
- [ ] T023 [P] frontend/messages/ko.json에 `notice` 문구 묶음과 `menu.notices`("공지")를 더한다
- [ ] T024 backend/에서 `./gradlew spotlessApply`, `./gradlew build`, `./gradlew dbTest --tests "*AdminMenusAcceptanceTest"`를 돌린다

**Checkpoint**: 공지 테이블·권한·메뉴가 있고, 공개 목록·상세 API가 돈다. 001 테스트가 메뉴 6개로 통과한다

---

## Phase 3: User Story 1 - 관리자가 공지를 쓰고 게시하고 관리한다 (Priority: P1) 🎯 MVP

**Goal**: 공지 관리 권한을 가진 사용자가 공지를 쓰고, 임시 저장하고, 게시하고, 고치고, 고정하고, 지운다

**Independent Test**: `./gradlew dbTest --tests "*NoticesAcceptanceTest"`에서 US1-AC1~AC8이 통과하고, 관리 화면에서 공지를 써서 게시할 수 있다

### 백엔드

- [ ] T025 [US1] backend/src/main/java/com/moa/api/controller/v1/admin/notice/request/에 `NoticeCreateRequest`(`@NotBlank @Size(max = 200) title`, `@SafeHtml @NotBlank @Size(max = 20000) content`, `pinned`, `publish`), `NoticeModifyRequest`(title, content 같은 규칙), `NoticePinRequest`(pinned)를, response/에 관리 목록·상세 응답을 만든다 (contracts/notices-api.md 관리 API)
- [ ] T026 [US1] backend/src/main/java/com/moa/api/service/notice/NoticeService.java와 NoticeApplication.java에 관리 기능을 더한다: 목록(제목 포함·상태 조건, `updated_at DESC, id DESC`, 페이지), 상세, 만들기(`publish=true`면 바로 게시), 고치기(상태·`published_at` 그대로), 게시(`published_at` = 지금), 고정·풀기, 지우기(소프트 삭제). 만들기·고치기 뒤 `richTextImageService.attach("NOTICE", id, content)`, 지우기 뒤 `detach("NOTICE", id)`를 같은 트랜잭션에서 부른다. 임시 저장에도 attach한다. 게시는 `publish` 한 메서드로만 한다 (research.md R2~R4, R9)
- [ ] T027 [US1] backend/src/main/java/com/moa/api/controller/v1/admin/notice/ApiAdminNoticeController.java를 만든다. `@RequirePermission(NoticePermissions.MANAGE)`, `/api/v1/admin/notices`의 7개 요청과 상태 코드는 contracts/notices-api.md 관리 API 표대로
- [ ] T028 [US1] `@DisplayName("US1-AC1 ...")`~`("US1-AC8 ...")` 테스트 8개를 backend/src/test/java/com/moa/api/NoticesAcceptanceTest.java에 더한다. 호출과 기대는 contracts/notices-api.md "AC와 확인 방법" 표대로. US1-AC8 본문은 `<p>안녕</p><script>alert(1)</script><img src="x" onerror="alert(1)">`

### 프론트

- [ ] T029 [P] [US1] frontend/src/app/(main)/admin/notices/page.tsx 관리 목록을 만든다. 키트 목록 화면 모양(`PageHeader`, `SearchPanel`, `DataTable`, 페이지): 검색은 제목·상태, 표는 제목·상태·고정·게시 시각·고친 시각, 행을 누르면 고치기 화면, 머리에 "공지 쓰기" (contracts/notices-screens.md)
- [ ] T030 [US1] frontend/src/components/notice/notice-form.tsx를 만든다. react-hook-form + Zod(`title` 1~200자, `content: z.string().min(1, "notice.contentRequired").max(20000)`), 본문은 `Controller` + `RichTextEditor`(이름표는 `id` + `aria-labelledby`), 고정 여부, `useUnsavedChanges(form.formState.isDirty)` (키트 rich-text HELPER.md "쓰는 법")
- [ ] T031 [US1] frontend/src/app/(main)/admin/notices/new/page.tsx(버튼 "임시 저장"·"게시")와 frontend/src/app/(main)/admin/notices/[id]/page.tsx(임시 저장 공지는 "저장"·"게시", 게시한 공지는 "저장", 고정 켜고 끄기, "지우기"는 `confirm`)를 만든다. 불러온 공지는 `form.reset`으로 넣는다
- [ ] T032 [P] [US1] frontend/tests/unit/notice.test.ts에 공지 폼 검사(제목·본문 필수, 길이)를 둔다

**Checkpoint**: US1이 API와 관리 화면에서 단독으로 된다

---

## Phase 4: User Story 2 - 사용자가 공지를 본다 (Priority: P1)

**Goal**: 로그인하지 않은 사람을 포함해 누구나 게시한 공지의 목록과 상세를 본다

**Independent Test**: `./gradlew dbTest --tests "*NoticesAcceptanceTest"`에서 US2-AC1~AC3가 통과하고, 로그아웃한 채 `/notices`에서 목록과 상세가 보인다

- [ ] T033 [US2] `@DisplayName("US2-AC1 ...")`~`("US2-AC3 ...")` 테스트 3개를 backend/src/test/java/com/moa/api/NoticesAcceptanceTest.java에 더한다 (contracts/notices-api.md 표). 공개 API는 T018에 있다
- [ ] T034 [US2] frontend/src/app/(site)/layout.tsx를 만든다. 로그인을 요구하지 않는 간단한 머리(서비스 이름, 로그인했으면 첫 화면, 아니면 로그인 링크). 처음 열 때 인증 상태가 `unknown`이면 `api.v1.auth.refresh()`를 한 번 부르고, 실패해도 로그인 화면으로 보내지 않는다 (`AuthGate`를 쓰지 않는다, research.md R7)
- [ ] T035 [US2] frontend/src/app/(site)/notices/page.tsx 사용자 목록(고정 표시, 제목, 게시 날짜, 빈 결과 안내, 페이지)과 frontend/src/app/(site)/notices/[id]/page.tsx 상세(제목, 게시 날짜, `RichTextView`, R001이면 없는 공지 안내와 목록 링크)를 만든다 (contracts/notices-screens.md)

**Checkpoint**: US1과 US2가 모두 된다

---

## Phase 5: User Story 3 - 읽지 않은 새 공지를 알아챈다 (Priority: P2)

**Goal**: 로그인한 사용자가 목록을 마지막으로 본 뒤 게시된 공지가 있으면 표시로 알아챈다

**Independent Test**: `./gradlew dbTest --tests "*NoticesAcceptanceTest"`에서 US3-AC1~AC3가 통과하고, 새 공지가 있으면 첫 화면에 안내가 보인다

- [ ] T036 [US3] backend/src/main/java/com/moa/api/service/notice/와 NoticeApplication.java에 확인 기록(`markSeen(userId)`: `notice_reads`에 서버 지금 시각을 넣거나 바꾼다)과 새 공지 여부(`hasNew(userId)`: `published_at > last_seen_at`인 게시·미삭제 공지가 있는가, 기록이 없으면 게시·미삭제 공지가 하나라도 있는가)를 더한다 (data-model.md, research.md R6)
- [ ] T037 [US3] backend/src/main/java/com/moa/api/controller/v1/notice/ApiNoticeController.java에 `GET /api/v1/notices/new`(200 {hasNew})와 `PUT /api/v1/notices/seen`(204)을 더한다. 이 둘은 `@PermitAll`이 아니다 (로그인 필요, 권한은 묻지 않는다). `@CurrentUser`로 사용자를 받는다
- [ ] T038 [US3] `@DisplayName("US3-AC1 ...")`~`("US3-AC3 ...")` 테스트 3개를 backend/src/test/java/com/moa/api/NoticesAcceptanceTest.java에 더한다 (contracts/notices-api.md 표)
- [ ] T039 [US3] frontend/src/app/(site)/notices/page.tsx가 로그인 상태면 열 때 `PUT /api/v1/notices/seen`을 부르고 새 공지 쿼리를 무효화하게 한다
- [ ] T040 [P] [US3] frontend/src/components/shell/menu.ts의 `MENU`에 `{ label: "notices", href: "/notices", icon: Megaphone }`(권한 없음, 홈 다음)를 더하고, frontend/src/components/notice/new-notice-alert.tsx(`GET /api/v1/notices/new`가 hasNew면 "새 공지가 있어요"와 목록 링크)를 만들어 frontend/src/app/(main)/page.tsx에 둔다 (contracts/notices-screens.md 새 공지 표시)

**Checkpoint**: US1~US3이 모두 된다

---

## Phase 6: User Story 4 - 공지가 게시되면 바로 알린다 (Priority: P3) ⏸ 007 뒤

**Goal**: 공지를 게시하면 화면을 보고 있는 로그인 사용자에게 실시간으로 알린다 (FR-009)

**선행 조건**: `007-notifications`가 키트 websocket 헬퍼와 전체 알림 경로를 만든 뒤 시작한다. 그 전까지 이 단계는 체크하지 않는다 (research.md R9)

- [ ] T041 [US4] `007-notifications`의 전체 알림 경로로 backend/src/main/java/com/moa/api/application/v1/notice/NoticeApplication.java의 `publish`에서 공지 제목을 보낸다 (자세한 방법은 007 plan을 따른다)
- [ ] T042 [US4] `@DisplayName("US4-AC1 ...")` 테스트를 backend/src/test/java/com/moa/api/NoticesAcceptanceTest.java에 더한다 (방법은 007의 알림 테스트를 따른다)
- [ ] T043 [US4] frontend/에서 전체 알림을 받으면 공지 제목 알림을 띄우고 새 공지 쿼리를 무효화한다

---

## Phase 7: Polish & Cross-Cutting

**Purpose**: 핵심 흐름 e2e, 전체 검사, 문서 정합성

- [ ] T044 [P] frontend/e2e/notices.spec.ts를 만든다 (백엔드는 키트 e2e처럼 가로챈다): 관리자가 공지를 써서 게시하는 흐름, 사용자 목록·상세, 3폭 가로 넘침·접근성(밝은·어두운)
- [ ] T045 backend/에서 `./gradlew spotlessApply`, `./gradlew build dbTest`를 돌려 통과를 확인한다. 실패하면 테스트를 고치지 말고 spec.md의 AC부터 확인한다 (원칙 V)
- [ ] T046 frontend/에서 `npm run format`, `npm run check`, `npm run test:e2e`를 돌려 통과를 확인한다
- [ ] T047 specs/README.md 기능 지도에서 `006-notices`의 상태를 `기획 중`에서 `구현됨 (US4는 007 뒤)`으로 바꾼다
- [ ] T048 specs/006-notices/quickstart.md의 "화면 확인" 절차를 사람이 직접 수행한다. 시험 계정 비밀번호가 필요하므로 에이전트는 하지 않는다 (원칙 VI)
- [ ] T049 팀원에게 알린다: rich-text 헬퍼 적용(사용자 쪽도 쓸 수 있다), 사용자 공지 화면의 임시 위치(`/notices`)와 새 공지 표시 자리를 함께 정하자는 것 (spec FR-010, specs/README.md 맞춰야 할 경계). 사람이 한다

---

## Dependencies & Execution Order

- Setup: 백엔드(T001~T003 → T004)와 프론트(T005·T006 → T007·T008·T009 → T011)는 서로 기대지 않는다. T010은 둘 다 끝난 뒤
- Foundational: T012 → T015 → T016 → T017 → T018. T013 → T019 → T020. T014는 아무 때나. T021은 T018 뒤. T022·T023은 프론트 Setup 뒤. T024는 백엔드 Foundational 마지막
- US1(T025~T032), US2(T033~T035), US3(T036~T040)는 모두 Foundational에 기댄다
  - US2의 테스트(T033)는 공지를 만드는 관리 API(T027)를 쓰므로 T027 뒤에 돌린다. 화면(T034·T035)은 US1과 따로 된다
  - US3의 테스트(T038)도 T027 뒤. T039는 T035 뒤
- US4(T041~T043)는 `007-notifications` 뒤
- Polish는 US1~US3 뒤

## Parallel Example

```text
Setup:        T001 · T002 · T003 (백엔드)   ∥   T005 · T006 (프론트)
Foundational: T014 · T015 (백엔드)          ∥   T022 · T023 (프론트)
US1:          T025~T028 (백엔드)            ∥   T029 · T032 (프론트)
Polish:       T044 ∥ T045
```

`NoticesAcceptanceTest.java`는 한 파일이라 T028·T033·T038은 차례로 한다.

## Implementation Strategy

1. Setup(T001~T011)으로 rich-text를 얹고 키트 검사를 통과시킨다
2. Foundational(T012~T024)로 데이터·권한·메뉴·공개 조회를 만들고 001 테스트를 6개로 맞춘다
3. US1(T025~T032)로 관리 기능을 끝낸다 (MVP: 관리자가 공지를 쓰고 게시한다)
4. US2(T033~T035)로 사용자 화면을 붙인다. 여기까지가 첫 출시에 쓸 수 있는 범위다
5. US3(T036~T040)으로 새 공지 표시를 더한다
6. Polish(T044~T049) 뒤 구현 PR을 연다. US4(T041~T043)는 007 뒤에 이 tasks에 이어서 한다
