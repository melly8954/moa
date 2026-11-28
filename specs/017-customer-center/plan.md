# Implementation Plan: 고객센터 (customer-center)

**Branch**: `feat/017-customer-center` | **Date**: 2026-11-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/017-customer-center/spec.md`

## Summary

관리 메뉴 "운영" 아래 FAQ 관리·문의 관리와 사용자 메뉴 "고객센터"를 더한다. 백엔드에 FAQ·문의 엔터티와 사용자 FAQ, 회원 문의, 관리 FAQ(한 건·일괄), 관리 문의(목록·상세·답변) API를, 프론트에 관리 화면 둘(목록 + 드로어)과 사용자 화면 넷(FAQ, 내 문의, 쓰기, 상세)을 만든다.
문의 첨부와 FAQ 답·답변 본문 이미지는 공지에서 쓰는 키트 rich-text 헬퍼의 이미지 기능을 그대로 쓴다. 새 헬퍼는 얹지 않는다 ([research.md](./research.md) R1·R2).
처음 답변 때만 회원에게 개인 알림(`INQUIRY_ANSWERED`)을 보낸다 (R3). 탈퇴 회원 문의의 1년 보관과 정리(US5)는 `018-member-withdrawal`의 탈퇴 상태와 탈퇴 시각으로 한다 (R10).

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.2), TypeScript 5 (Next 16.3)

**Primary Dependencies**: 기존 키트 헬퍼(admin 프리셋 menu·rbac·code, rich-text, websocket 알림). 새 의존성 없음

**Storage**: MariaDB. 새 테이블 `tb_faq`, `tb_inquiry`, `tb_inquiry_image`와 분류 코드·메뉴 행을 마이그레이션 한 파일로 넣는다(번호는 PR 직전 마지막+1, 지금은 `V14`). 이미지는 키트 `tb_rich_text_image`

**Testing**: 인수 테스트 `CustomerCenterAcceptanceTest` (MockMvc + Testcontainers MariaDB, PR CI의 `dbTest`). 메뉴·권한·코드 그룹이 늘어 `AdminMenusAcceptanceTest`, `AdminRolesAcceptanceTest`, `AdminCodesAcceptanceTest`를 고친다. 프론트는 `npm run check`와 핵심 흐름 e2e 둘

**Target Platform**: 웹 (관리 셸, 사용자 셸)

**Project Type**: 웹 서비스 (모노레포 `backend/`, `frontend/`)

**Performance Goals**: FAQ는 200개 상한이라 한 번에 준다. 문의 목록은 페이지 단위(기본 20건). 거르기와 정렬은 인덱스로 (data-model)

**Constraints**: 키트 코드는 고치지 않는다 (원칙 III). 컨트롤러 쪽 일시 칸은 `OffsetDateTime` (`ArchitectureTest`). 메뉴·코드 행은 넣기만 한다 (`docs/db.md`). 문의는 개인정보가 든 행이라 하드 삭제다 (`docs/privacy.md`). 로컬에서는 `build`·`check`만 돌린다

**Scale/Scope**: 백엔드 API 15개 (사용자 FAQ 1, 회원 문의 4, 관리 FAQ 7, 관리 문의 3), 프론트 화면 6개(관리 2, 사용자 4), AC 31개 (US1 8, US2 5, US3 8, US4 7, US5 3)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 스펙이 먼저다 | 통과 | spec.md에 `[NEEDS CLARIFICATION]`이 없다. 메뉴 자리, 이미지 첨부, 답변 하나, 탈퇴 뒤 1년 보관은 2026-11-25 사용자 답. 첨부 방법은 spec이 plan에 맡겼고 R1이 정한다 |
| II. AC마다 테스트 하나 | 통과 | AC 31개에 API 통합 테스트를 하나씩 둔다. 화면에서만 보이는 동작(펼치기, 로그인 뒤 돌아오기, 여섯 장째 안내, 드로어 이미지)은 e2e로 본다 (R13). US5 3개는 탈퇴 상태를 `tb_user.withdrawn_at`으로 만들어 본다 |
| III. 키트를 따른다 | 통과 | rich-text 헬퍼를 고치지 않고 `upload`·`load`·`attach`·`detach`를 부른다(R1·R2). 계층·권한·연결 화면·감사 필드·공통코드는 골격 규칙을 따른다 |
| IV. 기존 기능은 제자리에서 고친다 | 통과 | 메뉴는 `001-admin-menus` spec(FR-001·FR-007·FR-008 표, US1)·data-model, 분류 그룹은 `004-admin-codes` FR-001 표, 보관 기간은 `docs/privacy.md`를 이 문서 PR에서 고친다. 새 폴더를 만들지 않는다 |
| V. 테스트를 고쳐서 통과시키지 않는다 | 통과 | `AdminMenusAcceptanceTest`·`AdminRolesAcceptanceTest`·`AdminCodesAcceptanceTest`와 e2e 가짜 메뉴는 001·003·004 스펙이 바뀐 만큼만 고친다 |
| VI. 시크릿을 다루지 않는다 | 통과 | 인수 테스트는 토큰을 직접 만든다. 화면 직접 확인은 사용자에게 남긴다 |
| 기술 제약: 목록 | 통과 | FAQ 관리·사용자 FAQ는 구성 목록(상한 200), 문의 관리·내 문의는 업무 목록이다 (spec FR-015) |
| 기술 제약: DB | 통과 | `docs/db.md` 이름·열·인덱스·감사 열 규칙을 따른다. 컬렉션 테이블과 하드 삭제 테이블은 이유를 `COMMENT`에 적는다 (data-model) |

Phase 1 설계 뒤 다시 확인했다. 위반이 없다.

## Project Structure

### Documentation (this feature)

```text
specs/017-customer-center/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── api.md         # 사용자 FAQ, 회원 문의, 관리 FAQ·문의 API와 AC
│   └── screens.md     # 관리 화면 2개, 사용자 화면 4개
└── tasks.md           # /speckit-tasks가 만든다
```

### Source Code (repository root)

```text
backend/
├── src/main/resources/db/migration/V14__customer_center.sql  # tb_faq, tb_inquiry, tb_inquiry_image, 분류, 메뉴 (번호는 PR 직전에)
├── src/main/java/com/moa/api/
│   ├── controller/v1/faq/                 # ApiFaqController (사용자, 로그인 없이)
│   ├── controller/v1/inquiry/             # ApiInquiryController (회원)
│   ├── controller/v1/admin/faq/           # ApiAdminFaqController
│   ├── controller/v1/admin/inquiry/       # ApiAdminInquiryController
│   ├── application/v1/{faq,inquiry}/      # FaqApplication, InquiryApplication (트랜잭션, attach·detach, notify)
│   ├── service/{faq,inquiry}/  repository/{jpa,reader,writer}/{faq,inquiry}/
│   ├── service/user/MemberActivityPolicy.java   # 009 설계대로. 먼저 생긴 쪽이 만든다 (R4)
│   ├── entity/{faq,inquiry}/              # Faq, Inquiry (+ 첨부 컬렉션)
│   ├── dto/{faq,inquiry}/                 # InquiryStatus
│   ├── dto/notification/NotificationType.java   # INQUIRY_ANSWERED
│   ├── permission/{FaqPermissions,InquiryPermissions}.java
│   └── screen/SupportScreens.java         # ADMIN_FAQS, ADMIN_INQUIRIES, SUPPORT (@Order(65))
├── src/main/java/com/moa/common/exception/ErrorCode.java  # B301
└── src/test/java/com/moa/
    ├── api/CustomerCenterAcceptanceTest.java   # 이 기능의 AC 테스트
    ├── api/{AdminMenusAcceptanceTest,AdminRolesAcceptanceTest,AdminCodesAcceptanceTest}.java
    └── support/CodeSeed.java              # 없으면 만든다 (R8)

frontend/
├── src/api/{faq,inquiry}.ts  src/types/{faq,inquiry}.ts
├── src/app/(admin)/admin/faqs/            # page.tsx(목록), faq-drawer.tsx
├── src/app/(admin)/admin/inquiries/       # page.tsx(목록), inquiry-drawer.tsx
├── src/app/(user)/support/page.tsx        # FAQ (로그인 없이)
├── src/app/(user)/(signed-in)/support/inquiries/   # page.tsx(내 문의), new/page.tsx, [id]/page.tsx
├── src/components/support/                # 분류 이름, 상태 배지, 첨부 그림·크게 보기 (관리·사용자가 같이 씀)
├── messages/ko.json                       # faq, inquiry, support
├── src/types/schema.d.ts                  # npm run gen:api
└── e2e/support.spec.ts  e2e/support/api.ts (가짜 메뉴)
```

**Structure Decision**: 백엔드는 공지·배너와 같은 골격 계층(controller → application → service → reader·writer)이다. FAQ와 문의는 칸과 수명이 달라 엔터티·API를 나누고, 분류(`SUPPORT_CATEGORY`)만 같이 쓴다.
프론트 관리 화면은 배너·팝업의 목록 + 넓은 드로어 + 일괄 처리 줄을, 사용자 화면은 공지 사용자 화면과 `(signed-in)` 묶음을 본보기로 삼는다 (R11, R12).

## Complexity Tracking

위반이 없어 비워 둔다.
