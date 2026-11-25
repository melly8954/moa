# Implementation Plan: 배너·팝업 관리 (banner-popup)

**Branch**: `feat/016-banner-popup` | **Date**: 2026-11-24 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/016-banner-popup/spec.md`

## Summary

관리 메뉴에 "마케팅" 상위 메뉴와 배너 관리·팝업 관리 화면을 더한다. 백엔드에 배너·팝업 엔터티와 관리 API(한 건 CRUD, 일괄 켜기·끄기·삭제)를, 프론트에 목록 + 드로어 + 체크박스 일괄 처리 화면을 만든다.
배너 이미지와 팝업 본문 이미지는 공지에서 쓰는 키트 rich-text 헬퍼의 이미지 기능을 그대로 쓴다. 새 헬퍼는 얹지 않는다 ([research.md](./research.md) R1).
상태(예정·노출 중·끝남·꺼짐)는 저장하지 않고 켜짐과 노출 기간으로 읽을 때 정한다 (R3). 사용자 화면 노출과 사용자용 API는 만들지 않는다 (FR-011).

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.2), TypeScript 5 (Next 16.3)

**Primary Dependencies**: 기존 키트 헬퍼(admin 프리셋 menu·rbac, rich-text). 새 의존성 없음

**Storage**: MariaDB. 새 테이블 `tb_banner`, `tb_popup`과 메뉴 행을 마이그레이션 한 파일로 넣는다(번호는 PR 직전 마지막+1, 지금은 `V11`). 이미지는 키트 `tb_rich_text_image`와 `app.rich-text.image-dir`

**Testing**: 인수 테스트 `BannerPopupAcceptanceTest` (MockMvc + Testcontainers MariaDB, PR CI의 `dbTest`). 메뉴가 늘어 `AdminMenusAcceptanceTest`를 고친다. 프론트는 `npm run check`와 핵심 흐름 e2e 둘

**Target Platform**: 웹 (관리 셸)

**Project Type**: 웹 서비스 (모노레포 `backend/`, `frontend/`)

**Performance Goals**: 목록은 페이지 단위(기본 20건). 상태 거르기와 정렬은 인덱스로 (data-model)

**Constraints**: 키트 코드는 고치지 않는다 (원칙 III). 컨트롤러 쪽 일시 칸은 `OffsetDateTime` (`ArchitectureTest`). 메뉴 행은 넣기만 한다 (`docs/db.md`). 로컬에서는 `build`·`check`만 돌린다

**Scale/Scope**: 백엔드 API 14개 (배너 7, 팝업 7), 프론트 화면 2개(각각 목록 + 드로어), AC 22개 (US1 14, US2 8)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 스펙이 먼저다 | 통과 | spec.md에 `[NEEDS CLARIFICATION]`이 없다. 배너 형태(이미지), 노출 안 함(관리만), 순서 숫자, 일괄 켜기·끄기, 기본값, 검색은 2026-11-24 사용자 답. 사용자 화면 노출은 FR-011로 만들지 않는다 |
| II. AC마다 테스트 하나 | 통과 | AC 22개에 API 통합 테스트를 하나씩 둔다. 화면에서만 보이는 동작(US1-AC10 미리보기, 일괄 처리 뒤 고른 것 풀림, 칸 아래 이유)은 e2e로 본다 (R11) |
| III. 키트를 따른다 | 통과 | rich-text 헬퍼를 고치지 않고 `attach`·`detach`를 부른다(R1). 계층·권한·연결 화면·감사 필드는 골격 규칙을 따른다 |
| IV. 기존 기능은 제자리에서 고친다 | 통과 | 마케팅 메뉴는 `001-admin-menus` spec(FR-001 표, US1·US1-AC1)·data-model과 `003-admin-roles` US3-AC4(권한 트리의 상위 메뉴)를 이 문서 PR에서 고친다. 새 폴더를 만들지 않는다 |
| V. 테스트를 고쳐서 통과시키지 않는다 | 통과 | `AdminMenusAcceptanceTest`, `AdminRolesAcceptanceTest`, e2e 가짜 메뉴·권한 트리는 001·003 스펙을 먼저 고친 만큼만 고친다 |
| VI. 시크릿을 다루지 않는다 | 통과 | 인수 테스트는 토큰을 직접 만든다. 화면 직접 확인은 사용자에게 남긴다 |
| 기술 제약: 목록 | 통과 | 두 목록 모두 업무 목록이다(spec FR-008). 끝난 것이 쌓이고 상태·이름으로 찾는다 |
| 기술 제약: DB | 통과 | `docs/db.md` 이름·열·인덱스 규칙을 따른다 (data-model) |

Phase 1 설계 뒤 다시 확인했다. 위반이 없다.

## Project Structure

### Documentation (this feature)

```text
specs/016-banner-popup/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── admin-api.md       # 배너·팝업 관리 API와 AC 확인 방법
│   └── admin-screens.md   # 관리 화면과 드로어
└── tasks.md               # /speckit-tasks가 만든다
```

### Source Code (repository root)

```text
backend/
├── src/main/resources/db/migration/V11__banner_popup.sql   # tb_banner, tb_popup, 마케팅 메뉴 (번호는 PR 직전에)
├── src/main/java/com/moa/api/
│   ├── controller/v1/admin/banner/        # ApiAdminBannerController (+ request, response)
│   ├── controller/v1/admin/popup/         # ApiAdminPopupController (+ request, response)
│   ├── application/v1/banner/             # BannerApplication (트랜잭션, 이미지 attach·detach)
│   ├── application/v1/popup/              # PopupApplication
│   ├── service/{banner,popup}/  repository/{jpa,reader,writer}/{banner,popup}/
│   ├── entity/{banner,popup}/             # Banner, Popup
│   ├── dto/{banner,popup}/  dto/marketing/ExposureStatus.java
│   ├── permission/{BannerPermissions,PopupPermissions}.java
│   └── screen/MarketingScreens.java       # ADMIN_BANNERS, ADMIN_POPUPS (@Order(70))
└── src/test/java/com/moa/api/
    ├── BannerPopupAcceptanceTest.java     # 이 기능의 AC 테스트
    └── AdminMenusAcceptanceTest.java      # 001: 마케팅 메뉴

frontend/
├── src/api/{banner,popup}.ts  src/types/{banner,popup}.ts
├── src/app/(admin)/admin/banners/         # page.tsx(목록), banner-drawer.tsx, banner-preview.tsx
├── src/app/(admin)/admin/popups/          # page.tsx(목록), popup-drawer.tsx
├── src/components/marketing/              # 노출 기간 입력(DatePicker+TimeInput), 상태 표시 (둘이 같이 씀)
├── messages/ko.json                       # banner, popup
├── src/types/schema.d.ts                  # npm run gen:api
└── e2e/{banners,popups}.spec.ts  e2e/support/api.ts (가짜 메뉴)
```

**Structure Decision**: 백엔드는 공지와 같은 골격 계층(controller → application → service → reader·writer)이다. 배너와 팝업은 칸이 달라 엔터티·API를 나누고, 상태 판정(`ExposureStatus`)과 노출 기간 입력만 같이 쓴다.
프론트는 사용자 관리의 목록 + 드로어 화면과 메뉴 관리의 일괄 처리 줄을 본보기로 삼는다 (R7, R9).

## Complexity Tracking

위반이 없어 비워 둔다.
