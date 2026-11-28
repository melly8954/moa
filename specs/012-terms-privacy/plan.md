# Implementation Plan: 이용약관·개인정보처리방침 공개 페이지 (terms-privacy)

**Branch**: `feat/012-terms-privacy` | **Date**: 2026-11-28 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/012-terms-privacy/spec.md`

## Summary

누구나 여는 이용약관·개인정보처리방침 화면(지금 버전, 예전 버전, 시행 예정 안내)을 만든다. 다른 화면처럼 백엔드가 공개 API로 문안을 내려 주고, 프론트가 그 API를 받아 브라우저에서 그린다 (2026-11-28 결정).
문안은 DB가 아니라 백엔드 리소스 파일로 문서·버전마다 두고(장마다 HTML 조각), 010 가입 화면의 이용약관도 같은 원문을 API로 받게 한다 ([research.md](./research.md) R1·R2).
"지금 시행 중"은 요청 때 한국 날짜로 고르므로 시행일이 되면 배포 없이 바뀐다(R5). 문안이 잘못되면 백엔드가 시작하지 않는다(R6).
화면은 셸 없는 새 경로 묶음 `(legal)`에 두어 관리자 계정도 연다(R7). 링크는 로그인·가입 화면, 사용자 셸 본문, 문서 화면 맨 아래의 푸터 줄에 두고, 가입 화면의 약관 칸은 전문을 상자에 바로 보이고 "더보기"로 문서 화면을 새 탭에서 연다(R2·R7).

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.2), TypeScript 5 (Next 16.3)

**Primary Dependencies**: 기존 의존성(백엔드 Jackson, 프론트 react-query·DOMPurify·next-intl). 새 의존성 없음

**Storage**: DB 테이블 없음. 문안은 백엔드 `src/main/resources/legal/<문서>/<버전>.json`, 시작 때 한 번 읽는다. 설정 `app.legal.location`·`app.legal.zone`

**Testing**: 인수 테스트 `LegalAcceptanceTest`(PR CI `dbTest`, 테스트용 문안 `classpath:legal-test/`), 단위 테스트 `LegalServiceTest`·`LegalCatalogTest`(`./gradlew build`), 프론트 `npm run check`, e2e `legal.spec.ts`(API 가로채기)와 고친 `social-login.spec.ts`

**Target Platform**: 웹 (사용자 화면, 공개)

**Project Type**: 웹 서비스 (모노레포 `backend/`, `frontend/`)

**Performance Goals**: 문안은 시작 때 메모리에 올려 요청마다 파일을 읽지 않는다. 버전 고르기는 버전 수만큼의 비교 한 번

**Constraints**: 문안은 `messages/ko.json`에 넣지 않는다(spec 배경). 키트 `RichTextView`(표 없음)를 고치지 않는다(R3). 관리자 계정을 막는 사용자 셸에 두지 않는다(R7). 로컬에서는 `build`·`check`만 돌린다

**Scale/Scope**: 공개 API 2개, 화면 4개(문서 2 × 지금·버전), 푸터 줄 자리 5곳(로그인 2, 가입 1, 사용자 셸, 문서 화면), 문안 2개(각 첫 버전), AC 15개(US1 6, US2 5, US3 4)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 스펙이 먼저다 | 통과 | spec.md에 `[NEEDS CLARIFICATION]`이 없다. 링크 자리(푸터), 예전 버전, 이용약관 함께, 가입 화면 약관 칸, 공개 API로 내려 주기는 2026-11-27·28 사용자 답. "서버 API를 두지 않는다" 가정은 spec에서 먼저 고쳤다 |
| II. AC마다 테스트 하나 | 통과 | API로 볼 수 있는 AC는 인수 테스트, 날짜 전환(US3-AC4)은 단위 테스트, 화면에서만 보이는 AC는 e2e (R8, contracts/api.md "AC와 확인 방법") |
| III. 키트를 따른다 | 통과 | 계층·`@PermitAll`·오류 코드는 골격 규칙. 키트 `RichTextView`는 고치지 않고 문서 보기 부품을 따로 둔다(R3). `sidebar-shell.tsx` 본문 영역에 푸터 자리 하나를 더하는 것은 moa가 이미 바꿔 온 셸 파일이고, 키트로 옮길 목록에 더한다(R7) |
| IV. 기존 기능은 제자리에서 고친다 | 통과 | 사용자 셸 푸터 줄은 `001-admin-menus` FR-005, 가입 화면의 약관 칸·원문은 `010-social-login` FR-013·US1-AC7에 반영했다(spec 커밋) |
| V. 테스트를 고쳐서 통과시키지 않는다 | 통과 | 010 가입 e2e(`social-login.spec.ts`)는 010 FR-013·US1-AC7을 먼저 고친 만큼(전문 상자, 더보기 새 탭, 이용약관 전문 API)만 고친다 |
| VI. 시크릿을 다루지 않는다 | 통과 | 다룰 시크릿이 없다 |
| 기술 제약: DB·목록 | 해당 없음 | 테이블이 없다. 버전 목록은 한 문서의 버전 수만큼이라 쪽 나눔 없이 모두 보인다(해마다 몇 개) |

Phase 1 설계 뒤 다시 확인했다. 위반이 없다.

## Project Structure

### Documentation (this feature)

```text
specs/012-terms-privacy/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── api.md            # 공개 문서 API와 AC 확인 방법
│   └── screens.md        # 화면 경로·구성·푸터 줄·가입 화면 약관 칸
└── tasks.md              # /speckit-tasks가 만든다
```

### Source Code (repository root)

```text
backend/
├── src/main/resources/application.yaml               # app.legal.location, app.legal.zone
├── src/main/resources/legal/
│   ├── terms/draft.json                              # 010 자리표시 이용약관을 옮김
│   └── privacy/draft.json                            # 처리방침 첫 버전 (11-21 초안 + 018)
├── src/main/java/com/moa/
│   ├── common/legal/                                 # LegalDocument, LegalVersion, LegalProperties, LegalCatalog(시작 때 읽기·검사)
│   └── api/
│       ├── controller/v1/legal/ApiLegalController.java   # @PermitAll, response/
│       ├── application/v1/legal/LegalApplication.java    # 오늘(한국 날짜)을 구해 넘김
│       ├── service/legal/LegalService.java               # 지금·예정·예전 고르기, 없으면 R001
│       ├── repository/reader/legal/LegalReader.java      # LegalCatalog 읽기
│       └── dto/legal/                                    # LegalVersionDto, VersionState
└── src/test/
    ├── resources/legal-test/{terms,privacy}/{old,draft,next}.json
    └── java/com/moa/
        ├── api/LegalAcceptanceTest.java
        ├── api/service/legal/LegalServiceTest.java   # 단위
        └── common/legal/LegalCatalogTest.java        # 단위 (실제 legal/ 리소스)

frontend/
├── src/api/legal.ts  src/types/legal.ts              # api.v1.legal.get(document), version(document, version)
├── src/content/terms/{index.ts,draft.json}           # 010: SERVICE를 빼고 PRIVACY(동의서)만 남김
├── src/components/legal/
│   ├── legal-document.tsx                            # 문서 보기 (목차, 장, 표 상자, 작은 모양, DOMPurify 표 허용)
│   └── legal-footer.tsx                              # 푸터 줄 (newTab)
├── src/app/(legal)/
│   ├── layout.tsx                                    # 머리 줄, 푸터 줄
│   ├── terms/page.tsx  terms/versions/[version]/page.tsx
│   └── privacy/page.tsx  privacy/versions/[version]/page.tsx
├── src/app/(public)/sign-in/page.tsx  sign-in/recover/page.tsx  sign-up/nickname/page.tsx   # 푸터 줄, 가입 화면 약관 칸(전문 상자·더보기)
├── src/components/shell/{sidebar-shell.tsx,user-shell.tsx}   # 본문 맨 아래 푸터 자리
├── messages/ko.json                                  # legal
├── src/types/schema.d.ts                             # npm run gen:api
└── e2e/legal.spec.ts  e2e/social-login.spec.ts
```

**Structure Decision**: 백엔드는 골격 계층(controller → application → service → reader)을 따르되 저장소가 DB가 아니라 시작 때 읽은 리소스(`LegalCatalog`)다. 트랜잭션이 필요 없다. 프론트는 다른 화면처럼 클라이언트 컴포넌트가 react-query로 API를 부른다.

## Complexity Tracking

위반이 없어 비워 둔다.
