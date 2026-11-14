# Research: 공지사항 (notices)

Technical Context에 미결은 없다. 아래는 키트와 moa 코드를 확인해 정한 것이다.

## R1. rich-text 헬퍼를 얹는 방법

- **Decision**: 키트 태그의 헬퍼 파일을 손으로 옮긴다. 백엔드는 `springboot/v0.3.1`, 프론트는 `web-next/v0.4.0`의 `helpers/rich-text`를 쓴다. 얹은 뒤 `backend/.psw-dev-kit`·`frontend/.psw-dev-kit`의 `helpers`에 `rich-text`를 더하고, 프론트 기록의 `tag`를 `web-next/v0.4.0`으로 올린다. 헌법 "기술 제약"의 프론트 버전도 함께 고친다
  - 백엔드: `files/`를 `com.example.psw` → `com.moa`로 바꿔 옮긴다. `helper.gradle` → `gradle/helpers/rich-text.gradle`, `application.yaml` 조각은 `src/main/resources/application.yaml` 끝에 문서로 덧붙인다 (키트 `apply.sh`가 하는 일과 같다)
  - 프론트: `files/`를 옮기고, `helper.json`의 의존성 6개를 `package.json`에, `inserts`를 `src/api/index.ts`에, `messages.ko.json`을 `messages/ko.json`의 `richText`로 넣는다
  - 프론트 기록을 `web-next/v0.4.0`으로 올리면 골격도 그 태그와 맞춘다. v0.3.0 → v0.4.0의 골격 변경은 sidebar 셸 `<nav>` 하나이고 moa에 이미 들어 있다 (키트 쪽 주석 한 줄만 다르다)
- **Rationale**: 키트에는 적용 뒤 헬퍼를 더하는 스크립트가 없다 (`backend/springboot/README.md` 7절). 백엔드 rich-text 계약은 v0.3.1 이후 바뀌지 않았다
- **Alternatives considered**:
  - file 헬퍼도 함께 얹기: rich-text가 본문 이미지 올리기·저장·정리를 모두 해서 필요 없다 (spec Assumptions)
  - moa에서 편집기를 따로 만들기: 원칙 III. 키트에 먼저 넣었다 (`web-next/v0.4.0`)
- **주의**: rich-text 설정 조각의 `spring.servlet.multipart`는 file 헬퍼와 같은 환경 변수(`FILE_MAX_SIZE`)를 쓴다. moa에 file 헬퍼가 없어도 그대로 둔다

## R2. 공지 상태와 게시 시각

- **Decision**: `status`는 `DRAFT`·`PUBLISHED` 둘이다. 게시하면 `published_at`에 그 시각을 넣고 다시 바꾸지 않는다. `PUBLISHED` → `DRAFT`는 막는다 (B001)
- **Rationale**: FR-004와 Edge Cases(게시한 공지는 임시 저장으로 되돌리지 않는다, 고쳐도 새 공지가 다시 생기지 않는다). 새 공지 판정(R6)이 `published_at` 하나로 된다
- **Alternatives considered**: 고칠 때 `published_at`을 갱신. 고칠 때마다 새 공지 표시가 다시 생겨 Edge Cases에 어긋난다

## R3. 삭제

- **Decision**: 골격 `SoftDeleteEntity`로 지운다 (`deleted_at`). 지운 공지는 관리·공개 조회 모두에서 빠지고 상세는 R001이다. 지울 때 `richTextImageService.detach("NOTICE", id)`를 같은 트랜잭션에서 부른다
- **Rationale**: FR-007. 키트 다른 엔터티와 같다. detach하면 이미지가 임시가 되어 24시간 뒤 정리된다 (키트 동작)
- **Alternatives considered**: 행을 바로 지우기. 쓴 사람·시각 기록이 사라지고 다른 엔터티와 다르다

## R4. 본문 거르기와 이미지 잇기

- **Decision**: 요청 DTO의 `content`에 `@SafeHtml @NotBlank @Size(max = 20000)`을 붙인다. 만들기·고치기 뒤 `richTextImageService.attach("NOTICE", id, content)`를 같은 트랜잭션에서 부른다. 임시 저장 공지에도 attach한다
- **Rationale**: 키트 rich-text 계약(저장한 HTML은 이미 걸러져 있다). 임시 저장 공지의 이미지가 24시간 뒤 지워지면 게시할 때 깨진다. US1-AC8은 `<script>`가 든 본문을 저장한 뒤 상세 응답에 없는지로 확인한다
- **Alternatives considered**: 게시할 때만 attach. 위 이유로 버렸다

## R5. 권한과 메뉴

- **Decision**: 권한 키 `NOTICE_MANAGE`("공지 관리", 묶음 "공지")를 `NoticePermissions`(`PermissionSource`)로 등록한다. 관리 API 컨트롤러에 `@RequirePermission(NoticePermissions.MANAGE)`를 붙인다. 메뉴는 moa 마이그레이션 `V2_2__notice_admin_menu.sql`로 최상위에 더한다 (`id` 7, `sort_order` 50, 경로 `/admin/notices`, 아이콘 `clipboard`)
- **Rationale**: 키트 헬퍼들과 같은 방식이다. `sort_order` 50은 역할 관리(40) 다음이다. 아이콘은 키트 아이콘 표(`components/menu/icons.ts`)에 있는 것 중에서 고른다 (`bell`은 007 알림에 남긴다). `MenuSeed`가 INSERT·UPDATE만 다시 실행하므로 테이블 파일(`V2_1`)과 메뉴 파일(`V2_2`)을 나눈다
- **Alternatives considered**: 메뉴를 `V2_1`에 함께 넣기. `MenuSeed`가 같은 파일을 읽어도 되지만 메뉴 시드 목록에 테이블 파일이 끼어 읽기 어렵다

## R6. 새 공지 표시

- **Decision**: `notice_reads`에 사용자마다 한 행(`user_id`, `last_seen_at`)을 둔다. 로그인한 사용자가 사용자 공지 목록을 열면 프론트가 `PUT /api/v1/notices/seen`을 부른다. `GET /api/v1/notices/new`는 `last_seen_at` 뒤에 게시된(지우지 않은) 공지가 있는지 준다. 기록이 없으면 게시된 공지가 하나라도 있으면 있다
- **Rationale**: FR-008과 Edge Cases(한 번도 보지 않은 사용자, 지운 공지만 있는 경우, 임시 저장은 무시). 공지마다 읽음을 쌓지 않는다 (spec 배경)
- **Alternatives considered**:
  - 공개 목록 API(`GET /api/v1/notices`)가 로그인 사용자면 기록: 로그인 없이 여는 API(`@PermitAll`)에 쓰기를 섞고, 페이지를 넘길 때마다 기록된다
  - 확인 시각을 브라우저에만 저장: 기기마다 달라지고 서버 시각과 어긋난다
- **확인 시각**: 서버 시각으로 기록한다. 기록과 게시가 같은 순간이면 게시가 뒤로 보지 않는다(`published_at > last_seen_at`)

## R7. 사용자 공지 화면의 위치 (FR-010)

- **Decision**: 같은 Next 앱에 로그인 없이 여는 경로 묶음 `(site)`를 만들고 `/notices`, `/notices/[id]`를 둔다. 레이아웃은 간단한 머리(서비스 이름, 로그인 또는 첫 화면으로 가는 링크)만 둔다. 처음 열 때 리프레시 토큰으로 조용히 로그인 상태를 확인하고, 실패해도 로그인 화면으로 보내지 않는다. 로그인 상태면 목록을 열 때 확인을 기록한다
- 새 공지 표시: 로그인 뒤 셸 메뉴(코드 메뉴 `menu.ts`)에 "공지"를 모두에게 보이게 더하고, 첫 화면(`(main)/page.tsx`)에 새 공지가 있으면 안내와 목록으로 가는 링크를 보인다
- **Rationale**: 2026-11-14 사용자 결정(같은 앱, 임시 위치). moa 프론트는 지금 백오피스 앱 하나라 사용자 쪽 앱이 없다. 셸 메뉴 항목에 점을 다는 것은 키트 셸 파일을 고쳐야 해서 이번에는 하지 않는다 (원칙 III)
- **Alternatives considered**:
  - `(public)` 묶음에 넣기: 로그인 화면용으로 가운데 정렬만 하는 레이아웃이라 목록 화면에 맞지 않는다
  - 셸 메뉴에 새 공지 점 달기: 키트 셸 변경이 필요하다. 팀원과 위치를 정한 뒤 필요하면 키트에 넣는다
- **팀원과 맞출 것**: 사용자 화면의 메뉴 위치, 새 공지 표시 자리, 목록·상세 모양 (`specs/README.md` 맞춰야 할 경계). 정해지면 이 결정과 화면을 고친다

## R8. 관리 화면 모양

- **Decision**: 관리 목록은 키트 목록 화면 모양(`PageHeader`, 검색, `DataTable`, 페이지)을 따른다. 검색은 제목 글자와 상태. 쓰기·고치기는 대화상자가 아니라 화면(`/admin/notices/new`, `/admin/notices/[id]`)이다. 폼은 `RichTextEditor`를 react-hook-form `Controller`에 잇고, 저장하지 않고 떠나면 되묻는다 (`useUnsavedChanges`)
- **Rationale**: spec Assumptions(키트 목록 화면 모양). 본문 편집기는 대화상자에 넣기엔 크다
- **Alternatives considered**: 공통코드처럼 대화상자. 위 이유로 버렸다

## R9. US4(실시간 알림) 미루기

- **Decision**: 이번 구현에서 US4-AC1, FR-009는 만들지 않는다. tasks에 "007 뒤"로 표시하고, 007이 전체 알림 경로를 만든 뒤 이 스펙의 tasks에 이어서 붙인다. 게시 처리에 알림을 붙일 자리만 `NoticeApplication.publish` 한 곳으로 모은다
- **Rationale**: spec Assumptions(007 전에도 US1~US3은 단독으로 쓴다). 키트 websocket 헬퍼는 007에서 적용한다
- **Alternatives considered**: 게시 이벤트(`NoticePublishedEvent`)를 지금 만들어 두기. 받는 쪽이 없어 원칙 I(스펙에 없는 동작)에 가깝다. 007에서 정한다

## R10. 테스트 방식

- **Decision**: `NoticesAcceptanceTest` 한 클래스에 US1~US3 AC 14개를 `@DisplayName("US1-AC1 ...")`로 둔다. 공개 API는 토큰 없이, 관리 API는 ADMIN과 역할 없는 운영자 토큰으로 부른다. 프론트 e2e는 핵심 흐름 둘만: 관리자가 공지를 써서 게시하는 화면, 사용자 목록·상세(가로 넘침·접근성 포함)
- **Rationale**: 원칙 II(API 통합 테스트 기본, e2e는 핵심 흐름). 001~005와 같은 모양이다
- **Alternatives considered**: AC마다 e2e. 키트 e2e는 백엔드를 가로채 실제 권한·거르기를 검증하지 못한다
