# Implementation Plan: 공지사항 (notices)

**Branch**: `feat/006-notices` | **Date**: 2026-11-14 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/006-notices/spec.md`

## Summary

moa가 처음 직접 만드는 기능이다. 백엔드에 공지 엔터티와 관리·공개 API, 새 공지 확인 기록을 더하고, 프론트에 관리자 공지 관리 화면과 사용자 공지 목록·상세 화면을 만든다.
본문의 서식·이미지는 키트 rich-text 헬퍼(백엔드 `springboot/v0.5.0`, 프론트 `web-next/v0.5.0`)를 손으로 얹어 쓴다 ([research.md](./research.md) R1).
관리 메뉴에 "공지 관리"를 6번째로 더하고 `001-admin-menus`의 스펙과 테스트를 함께 고친다.
실시간 알림(US4, FR-009)은 `007-notifications` 뒤로 미룬다 (R9).

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.2), TypeScript 5 (Next 16.3)

**Primary Dependencies**: 키트 rich-text 헬퍼 (백엔드 jsoup, 프론트 Tiptap·DOMPurify). 기존 admin 프리셋 헬퍼(menu·rbac 등)

**Storage**: MariaDB. `notices`, `notice_reads` (moa `V2_1`), `rich_text_images` (키트 `V1_9`). 본문 이미지 파일은 `app.rich-text.image-dir` (기본 `./data/rich-text-images`)

**Testing**: 인수 테스트 `NoticesAcceptanceTest` (MockMvc + Testcontainers MariaDB, `./gradlew dbTest`). 키트 `RichTextImageAcceptanceTest`·`HtmlSanitizerTest`를 함께 얹는다. 프론트는 `npm run check`(단위)와 핵심 흐름 e2e(Playwright)

**Target Platform**: 웹 (백오피스와 사용자 공지 화면이 같은 Next 앱)

**Project Type**: 웹 서비스 (모노레포 `backend/`, `frontend/`)

**Performance Goals**: 목록은 페이지 단위(기본 20건). 새 공지 여부는 인덱스 한 번 조회 (R6)

**Constraints**: 키트 코드는 고치지 않는다 (원칙 III). 마이그레이션 번호는 `V2_1` 이후 (`V2`를 001이 썼다). 본문은 저장 때 거른다 (rich-text 계약)

**Scale/Scope**: 백엔드 API 11개 (관리 7, 공개 2, 새 공지 2), 프론트 화면 5개 (관리 목록·쓰기·고치기, 사용자 목록·상세), AC 15개 (US4 제외 14개를 이번에 테스트)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 스펙이 먼저다 | 통과 | spec.md에 `[NEEDS CLARIFICATION]`이 없다. 사용자 화면의 위치는 FR-010대로 임시 위치를 두고 팀원과 맞춘다 (2026-11-14 사용자 결정, R7) |
| II. AC마다 테스트 하나 | 통과 | US1~US3의 AC 14개에 API 통합 테스트를 하나씩 둔다. US4-AC1은 007 뒤에 더한다 (R9). 핵심 흐름만 e2e |
| III. 키트를 따른다 | 통과 | rich-text 헬퍼를 키트 그대로 얹고 `.psw-dev-kit`에 적는다. 계층·권한·감사 필드는 골격 규칙을 따른다. 키트 파일은 고치지 않는다 (R1, R8) |
| IV. 기존 기능은 제자리에서 고친다 | 통과 | 메뉴 6번째는 `001-admin-menus` spec·data-model·contracts·테스트를 고친다. 새 폴더를 만들지 않는다 |
| V. 테스트를 고쳐서 통과시키지 않는다 | 통과 | `AdminMenusAcceptanceTest`는 001 스펙(FR-001 표)을 먼저 고친 만큼만 고친다 |
| VI. 시크릿을 다루지 않는다 | 통과 | 인수 테스트는 토큰을 직접 만든다. 화면 직접 확인은 사용자에게 남긴다 |

Phase 1 설계 뒤 다시 확인했다. 위반이 없다.

## Project Structure

### Documentation (this feature)

```text
specs/006-notices/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── notices-api.md        # 관리·공개·새 공지 API
│   └── notices-screens.md    # 화면 경로와 동작
└── tasks.md                  # /speckit-tasks가 만든다
```

### Source Code (repository root)

```text
backend/
├── gradle/helpers/rich-text.gradle                 # 키트 helper.gradle (jsoup)
├── src/main/resources/application.yaml             # 끝에 키트 rich-text 설정 문서를 덧붙인다
├── src/main/resources/db/migration/
│   ├── V1_9__rich_text_images.sql                  # 키트 그대로
│   ├── V2_1__notices.sql                           # notices, notice_reads
│   └── V2_2__notice_admin_menu.sql                 # 공지 관리 메뉴 (최상위 6번째)
├── src/main/java/com/moa/
│   ├── common/helper/richtext/                     # 키트 그대로 (패키지만 com.moa)
│   └── api/
│       ├── controller/v1/richtext/                 # 키트 그대로
│       ├── controller/v1/admin/notice/             # ApiAdminNoticeController (+ request, response)
│       ├── controller/v1/notice/                   # ApiNoticeController (공개 + 새 공지)
│       ├── application/v1/notice/                  # NoticeApplication (트랜잭션, rich-text attach·detach)
│       ├── service/notice/  repository/{jpa,reader,writer}/notice/
│       ├── entity/notice/                          # Notice, NoticeStatus, NoticeRead
│       ├── dto/notice/
│       └── permission/NoticePermissions.java       # NOTICE_MANAGE
└── src/test/java/com/moa/
    ├── api/NoticesAcceptanceTest.java              # 이 기능의 AC 테스트
    ├── api/AdminMenusAcceptanceTest.java           # 001: 메뉴 6개로
    ├── api/RichTextImageAcceptanceTest.java        # 키트 그대로
    ├── common/helper/richtext/HtmlSanitizerTest.java  # 키트 그대로
    └── support/MenuSeed.java                       # V2_2를 더한다

frontend/
├── src/api/{rich-text,notice}.ts  src/types/{rich-text,notice}.ts
├── src/lib/rich-text.ts  src/components/rich-text/      # 키트 그대로
├── src/components/notice/                          # 공지 폼, 새 공지 안내, 관리 목록 표
├── src/app/(main)/admin/notices/                   # 관리 목록, new, [id]
├── src/app/(main)/page.tsx                         # 첫 화면에 새 공지 안내
├── src/app/(site)/                                 # 로그인 없이 보는 화면 묶음 (layout)
│   └── notices/                                    # 사용자 목록, [id] 상세
├── src/app/(dev)/dev/rich-text/                    # 키트 본보기 그대로
├── src/components/shell/menu.ts                    # 코드 메뉴에 "공지" (모두에게)
├── messages/ko.json                                # richText(키트), notice, menu.notices
├── tests/unit/{rich-text,notice}.test.ts
└── e2e/{rich-text,notices}.spec.ts
```

**Structure Decision**: 백엔드는 골격 계층(controller → application → service → reader·writer)과 code 헬퍼의 모양을 따른다. 헬퍼 파일은 키트 태그에서 그대로 옮기고 패키지만 바꾼다.
프론트의 사용자 공지 화면은 새 경로 묶음 `(site)`에 둔다. 로그인을 요구하지 않고, 로그인 상태면 새 공지 확인을 기록한다 (R7).

## Complexity Tracking

위반이 없어 비워 둔다.
