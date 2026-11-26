# Research: 고객센터 (customer-center)

Technical Context에 미결은 없다. 아래는 moa 코드(2026-11-25 dev `b90b2f0`)와 키트 헬퍼를 확인해 정한 것이다.

## R1. 문의 첨부 이미지 올리기·저장 (FR-007)

- **Decision**: 공지·배너가 쓰는 키트 rich-text 헬퍼의 이미지 기능을 그대로 쓴다. 키트 코드는 고치지 않는다 (`016-banner-popup` R1과 같은 방식)
  - 올리기: 문의 쓰기 화면이 `POST /api/v1/rich-text/images`(multipart `file`)로 한 장씩 올리고 받은 `url`을 문의 요청의 `imageUrls`에 넣는다. 이 API는 로그인한 사용자면 부를 수 있다(`@PermitAll`이 없다)
  - 저장: 컬렉션 테이블 `tb_inquiry_image`에 주소와 순서를 둔다 (data-model)
  - 잇기·풀기: `InquiryApplication`이 주소들로 `<img src="…">` 조각을 만들어 `richTextImageService.attach("INQUIRY", id, html)`을 부른다. 문의를 지우면 `detach("INQUIRY", id)`. 풀린 이미지는 키트 정리기가 24시간 뒤 파일과 함께 지운다
  - 형식·크기: rich-text 올리기 그대로 PNG·JPEG·GIF·WEBP, 한 장 5MB(`app.rich-text.image-max-size`). 넘으면 올리기에서 400 `V001`
  - 장수: 요청의 `imageUrls`는 0~5개(`@Size(max = 5)`). 화면은 다섯 장을 붙이면 고르기를 막고 안내한다 (US3-AC3)
- **Rationale**: 이미지 저장, 공개 주소, 임시 정리, 파기 이력(`PurgeTarget.RICH_TEXT_IMAGE`)이 이미 있다. 새 헬퍼를 얹지 않으니 팀원에게 따로 알릴 것이 없다 (`specs/README.md` 추가로 쓸 키트 헬퍼)
- **주의 (SC-004)**: rich-text 이미지 보기(`GET /api/v1/rich-text/images/{publicId}`)는 로그인 없이 열린다. 주소의 `publicId`는 추측할 수 없는 UUID라, 화면으로는 다른 회원의 첨부를 볼 길이 없다. 다만 주소를 알면 누구나 열 수 있다(주소가 곧 열쇠). 문의 화면은 주소를 그 문의의 주인과 문의 관리 권한자에게만 보낸다. 그래서 SC-004는 "화면이나 API로" 보는 일로 정하고, 주소의 공개 범위는 spec Assumptions에 적었다 (2026-11-25, analyze S1)
  - 주소만으로 열리는 것이 문제가 되면(민감한 스크린샷이 많아지는 경우) 키트 file 헬퍼로 권한을 확인하는 내려받기로 바꾼다. 그때는 이 결정과 data-model을 고친다
- **Alternatives considered**:
  - 키트 file 헬퍼를 새로 얹기: 권한을 확인하는 내려받기를 만들 수 있지만, 저장·정리·파기 기록을 다시 잇고 사용자 쪽에도 영향이 간다. 첫 출시에는 크다
  - 문의 내용을 rich-text로 받고 본문에 이미지를 넣게 하기: 회원 화면에 서식 편집기를 두게 되고, 장수 제한(다섯 장)을 본문에서 세야 한다

## R2. 답변과 FAQ 답의 서식 (FR-004, FR-008)

- **Decision**: FAQ 답과 문의 답변은 공지 본문과 같다. `@SafeHtml @NotBlank @Size(max = 20000)`, 편집기는 `RichTextEditor`. 저장 뒤 FAQ는 `attach("FAQ", id, answer)`, 답변은 `attach("INQUIRY_ANSWER", inquiryId, content)`
  - 문의 첨부와 답변 본문 이미지는 같은 문의 `id`에 붙지만 `owner_type`이 달라(`INQUIRY`, `INQUIRY_ANSWER`) 서로 덮지 않는다. 문의를 지우면 둘 다 `detach`
- **Rationale**: `006-notices` R4, `016-banner-popup` R8과 같다. US1-AC8은 `<script>`가 든 답을 저장한 뒤 응답에 없는지로 본다
- 문의 내용(회원이 쓴 글)은 서식 없는 글이다. 저장 그대로 두고 화면이 글자로 보인다(React가 이스케이프, 줄바꿈은 `white-space: pre-wrap`)

## R3. 문의 상태와 답변 한 번 알림 (FR-008, FR-012, Edge Cases)

- **Decision**: 상태 칸을 두지 않고 `answered_at`으로 읽는다. 비면 `WAITING`(답변 대기), 있으면 `ANSWERED`(답변 완료). `InquiryStatus`는 `dto`에 둔다
  - 답변 저장(`PUT /api/v1/admin/inquiries/{id}/answer`): 문의 행을 잠그고(`SELECT … FOR UPDATE`) 읽는다. `answered_at`이 비었으면 답변 내용, `answered_at`, `answered_by`를 넣고 같은 트랜잭션에서 `notify`한다. 이미 있으면 답변 내용만 바꾸고 알림을 보내지 않는다
  - 두 관리자가 함께 처음 답변해도 잠금 때문에 하나만 "처음"이 되어 알림은 한 번이다. 나중 저장이 내용을 덮는다 (Edge Cases)
- **Rationale**: 상태가 답변 시각과 어긋날 수 없다. 거르기는 `answered_at IS NULL`로 한다
- **Alternatives considered**: `status` 칸(답변 시각과 두 곳에 같은 사실), 낙관적 잠금 `@Version`(나중 저장이 실패해 관리자가 다시 써야 한다)

## R4. 관리자 계정 막기 (FR-011)

- **Decision**: 문의 쓰기·지우기·내 문의 API 앞에서 `MemberActivityPolicy.isManagerAccount(userId)`가 참이면 403 `A005`(`FORBIDDEN`)다
  - `MemberActivityPolicy`는 `009-mypage`가 만들기로 한 공용 클래스다(009 research R2, `role == ADMIN`). 017이 먼저 구현되면 017이 009의 설계대로 만들고, 009·008은 그것을 쓴다 (`008-party` research R14와 같은 약속)
  - 새 오류 코드를 만들지 않는다. 관리자 계정은 사용자 셸을 열 수 없어(`001-admin-menus` FR-005) 이 오류를 화면에서 볼 일이 없다
- **Alternatives considered**: 문의 전용 오류 코드(`008-party`의 `A103`처럼). 화면에 보일 일이 없어 이름만 늘어난다

## R5. 회원 쪽 문의 API와 남의 문의 (FR-009, FR-010)

- **Decision**:
  - `GET /api/v1/inquiries/me?page=&size=`: 내 문의 목록, 쓴 최신순
  - `POST /api/v1/inquiries`, `GET /api/v1/inquiries/{id}`, `DELETE /api/v1/inquiries/{id}`
  - 남의 문의나 없는 문의는 조회·지우기 모두 404 `R001`이다. 남의 것이 있다는 것도 알리지 않는다 (US3-AC7)
  - 답변 완료 문의를 지우면 409 `B001`(`INVALID_STATE`) (FR-009)
  - 회원이 지우는 것은 하드 삭제다(행 `DELETE`, 첨부·답변 이미지 `detach`). 개인정보가 든 행이라 소프트 삭제로 남기지 않는다 (`docs/privacy.md` 원칙). 회원이 자기 글을 지운 것이라 파기 이력에는 남기지 않는다 (`013-purge-log`는 보관 기간 파기와 탈퇴 파기를 남긴다)
- 회원 응답에는 답변한 관리자를 넣지 않는다. 화면이 "moa 운영팀"으로 보인다 (FR-008)

## R6. 관리 쪽 문의 API (FR-010, FR-015, US4)

- **Decision**:
  - `GET /api/v1/admin/inquiries?status=&categoryCode=&from=&to=&keyword=&page=&size=`. `status`는 `WAITING`·`ANSWERED`·`ALL`, 없으면 `WAITING`(기본 거르기). 기간은 쓴 날짜(`011-list-screen` 기간 빠른 단추). `keyword`는 제목 포함 또는 작성자 아이디·이름 포함
  - 정렬은 `created_at DESC, id DESC`
  - `GET /api/v1/admin/inquiries/{id}`: 문의, 첨부 주소, 작성자, 답변(있으면 답변한 관리자 이름)
  - `PUT /api/v1/admin/inquiries/{id}/answer` `{content}` (R3)
  - 문의 지우기, 일괄 처리는 두지 않는다 (spec FR-010, Assumptions)
  - 작성자: 회원 아이디와 이름(`tb_user`). 탈퇴 회원 파기 뒤에는 "탈퇴한 회원"(US5, 회원 탈퇴 스펙이 정하는 표시를 따른다)
- **Rationale**: `useListSearch`의 빈 값을 기본 거르기 "답변 대기"로 두고 "전체"를 `ALL`로 둔다 (`016-banner-popup` R4와 같은 방식)

## R7. 권한, 연결 화면, 메뉴 (FR-001, FR-002, FR-014)

- **Decision**:
  - 권한: `FaqPermissions` `FAQ_MANAGE`("FAQ 관리", 묶음 "고객센터"), `InquiryPermissions` `INQUIRY_MANAGE`("문의 관리", 묶음 "고객센터")
  - 연결 화면: `SupportScreens`(`@Order(65)`, 공지 60 다음)가 `ADMIN_FAQS`(`/admin/faqs`), `ADMIN_INQUIRIES`(`/admin/inquiries`), `SUPPORT`(`/support`, 사용자, 로그인 필요 없음)를 낸다
  - 메뉴: 마이그레이션이 "운영"(`id` 10) 아래에 "FAQ 관리"(`ADMIN_FAQS`, `list`, 23)와 "문의 관리"(`ADMIN_INQUIRIES`, `mail`, 26)를 넣고, 사용자 메뉴 "고객센터"(`SUPPORT`, `mail`, 90, `PUBLIC`)를 넣는다. 기존 메뉴 행은 고치지 않는다
- **Rationale**: 운영의 공지 관리(20)와 접속 이력(30) 사이에 두려면 23·26이면 되고, 기존 행을 고치지 않아 관리자가 바꾼 메뉴를 덮지 않는다 (`docs/db.md`). 사용자 메뉴 90은 지금 메뉴(홈 10, 공지 20, `008-party`의 모집·내 파티) 뒤에 오고, 담당이 자리를 정하면 메뉴 관리에서 옮긴다. 키트 아이콘 표에 도움말 아이콘이 없어 있는 것 중에서 고른다
- `001-admin-menus`의 spec(FR-001·FR-007·FR-008 표, US1 메뉴 수)과 data-model은 이 문서 PR에서 고쳤다. `003-admin-roles` US3-AC4는 "운영"이 펼쳐진 채라 바뀌지 않는다. `AdminMenusAcceptanceTest`(운영 하위 메뉴, 사용자 메뉴, 연결 화면 목록), `AdminRolesAcceptanceTest`(권한 트리), e2e 가짜 메뉴(`frontend/e2e/support/api.ts`)는 구현 PR에서 그만큼 고친다 (원칙 IV·V)

## R8. 고객센터 분류 (FR-003)

- **Decision**: 마이그레이션이 `tb_code_group`에 `SUPPORT_CATEGORY`, 항목 다섯을 넣는다. 같은 `group_code`가 이미 있으면 넣지 않는다 (`docs/db.md` 마이그레이션, `008-party` research의 시드 넣는 법)
  - 저장: FAQ·문의의 `category_code`에 항목 코드를 둔다
  - 쓰기 확인: 새 FAQ·문의, 그리고 FAQ를 고치며 분류를 바꿀 때 분류가 그룹의 사용 중 항목이 아니면 400 `V001`(칸 오류 `categoryCode`). 분류를 그대로 두고 고치면 확인하지 않는다. 꺼진 분류의 FAQ도 질문·답을 고칠 수 있어야 한다 (analyze U1)
  - 화면의 분류 고르기: `GET /api/v1/faqs/categories`(로그인 없이, 사용 중 항목만, 순서대로). 공통코드 화면용 목록은 로그인이 필요해 로그인하지 않은 FAQ 화면이 쓸 수 없어 구현 때 따로 두었다(2026-11-25). 저장된 분류의 이름은 서버 응답의 `categoryName`(`getAllCodes`로 채움, 꺼진 항목도 이름이 있다)으로 보인다. 관리 응답과 회원 응답 모두 같다 (analyze I1)
  - 사용자 FAQ는 분류가 사용 중인 FAQ만 보인다. 관리 목록은 분류가 꺼져도 보이고, 이름은 전체 항목(`getAllCodes`)으로 찾는다 (spec Edge Cases)
- `004-admin-codes` FR-001 표에 줄을 더했다. `AdminCodesAcceptanceTest`의 그룹 기대 목록과 테스트 정리 뒤 시드 되돌리기(`CodeSeed`, `008-party`가 만들기로 한 지원 코드. 없으면 017이 만든다)는 구현 PR에서 다룬다

## R9. FAQ 목록 (FR-005, FR-006, FR-015)

- **Decision**:
  - 관리: `GET /api/v1/admin/faqs?categoryCode=&enabled=&keyword=` 전체(쪽 없음, 상한 200). `keyword`는 질문 포함
  - 사용자: `GET /api/v1/faqs?categoryCode=&keyword=` 로그인 없이(`@PermitAll`). 켜져 있고 분류가 사용 중인 것만, 답까지 담아 한 번에 준다(펼치기에 따로 부르지 않는다). `keyword`는 질문 또는 답의 글자 포함. 답의 태그 안 글자(`img`의 주소 등)도 걸릴 수 있으나 FAQ 수가 적어 받아들인다
  - 정렬: `sort_order ASC, created_at DESC, id DESC` (FR-006)
  - 상한: 지우지 않은 FAQ가 200개면 등록을 409 `B301`(`FAQ_LIMIT_EXCEEDED`, "FAQ는 200개까지 둘 수 있습니다.")로 막는다
  - 일괄 처리: `POST /api/v1/admin/faqs/bulk-delete` `{ids}`, `PUT /api/v1/admin/faqs/enabled` `{ids, enabled}` (1~200개). 하나라도 없으면 404 `R001`이고 아무것도 바꾸지 않는다 (`016-banner-popup` R7과 같다)
- **Rationale**: 구성 목록은 전체를 보이고 화면 안에서 스크롤한다(원칙 "목록"). 사용자 FAQ 화면은 펼치기만 하므로 한 번에 받는다
- 오류 코드 `B301`은 지금 dev에 없는 번호다. 구현 PR 직전에 `ErrorCode`에서 겹치지 않는지 다시 본다(다른 기능 브랜치가 B3xx를 쓸 수 있다)

## R10. 탈퇴 회원 문의 보관과 정리 (FR-017, US5)

- **Decision** (회원 탈퇴 스펙 뒤에 붙인다):
  - 답변 막기: 작성자가 탈퇴 상태면 답변 저장을 409 `B001`로 막고, 상세 응답의 `answerable`을 `false`로 준다 (US5-AC1)
  - 정리기 `InquiryCleaner`: 매일 새벽 `@Scheduled`. 작성자의 탈퇴 일시가 `app.inquiry.withdrawn-retention-days`(기본 365)보다 오래된 문의를 찾아, 문의마다 `detach("INQUIRY")`·`detach("INQUIRY_ANSWER")` 뒤 행과 첨부 행을 지운다. 지운 건수로 `PersonalDataPurgedEvent.byScheduler(PurgeTarget.INQUIRY, count)`를 낸다 (`013-purge-log`). 여러 서버에서 같이 돌아도 결과가 같다(이미 지운 행은 다시 찾지 않는다)
  - `PurgeTarget`에 `INQUIRY`("1:1 문의")를 더한다
  - 탈퇴 상태·일시의 칸 이름과 복구 때의 동작은 회원 탈퇴 스펙의 data-model을 따른다. 그 스펙이 생기면 이 R10의 칸 이름을 맞춘다
- **Rationale**: `docs/privacy.md` "실행"(설정값 기간, 매일 새벽 정리기, 지운 건수만 로그)
- **Alternatives considered**: 탈퇴 회원 파기(유예 30일 끝) 처리 안에서 1년 뒤 예약을 거는 방식. 탈퇴 처리와 문의가 얽혀 둘 중 하나를 고칠 때 함께 고쳐야 한다

## R11. 사용자 화면 (FR-013)

- **Decision**:
  - `app/(user)/support/page.tsx`: FAQ(로그인 없이). 분류 단추 줄("전체" + 사용 중인 분류), 검색어 칸, 질문 목록(누르면 그 자리에서 답 펼침, 여러 개를 함께 펼칠 수 있다). 분류와 검색어는 주소(`?category=&keyword=`)에 둔다
  - `app/(user)/(signed-in)/support/inquiries/page.tsx`(내 문의), `…/inquiries/new/page.tsx`(쓰기), `…/inquiries/[id]/page.tsx`(상세): `AuthGate`가 로그인으로 보내고 `?next=`로 돌아온다 (US3-AC2)
  - 쓰기 화면: 분류 선택, 제목, 내용(남은 글자 수), 이미지 붙이기(작은 그림, 빼기). 다 쓰면 상세로 간다
  - 상세: 문의 내용과 첨부(누르면 크게), 답변이 있으면 "moa 운영팀"과 답변 시각, 답변 대기면 지우기 단추
  - 화면 모양은 사용자 셸의 공지 화면을 본보기로 하고, 사용자 쪽 담당이 정한 것이 있으면 따른다
- **Rationale**: 공지 사용자 화면(`app/(user)/notices`)과 로그인이 필요한 사용자 화면 묶음(`(signed-in)`)이 이미 있다

## R12. 관리 화면 (FR-005, FR-015, FR-016)

- **Decision**:
  - FAQ 관리(`/admin/faqs`): 구성 목록. `SearchPanel`(분류 `ChoiceFilter`, 켜짐 `ChoiceFilter`, `KeywordFilter`) + `BulkToolbar`(켜기·끄기·삭제, 삭제만 확인) + `DataTable`(쪽 없음, 표 안 스크롤). 열: 분류, 질문, 순서, 켜짐. 행을 누르면 넓은 드로어(분류, 질문, 답 `RichTextEditor`, 순서, 켜짐)
  - 문의 관리(`/admin/inquiries`): 업무 목록. `SearchPanel`(상태 기본 "답변 대기", 분류, 기간 빠른 단추, 검색어) + `DataTable`(쪽). 열: 상태, 분류, 제목, 작성자, 쓴 시각, 답변 시각. 행을 누르면 넓은 드로어(문의 내용, 첨부 작은 그림 → 누르면 크게, 작성자, 답변 `RichTextEditor`와 저장)
  - 드로어는 저장하지 않은 채 닫으면 묻는다(`useDrawerClose`). 머리 설명은 `t.rich`의 `<link>`로 서로 잇는다 (FR-016)
- **Rationale**: `016-banner-popup` R9(팝업 드로어 안 편집기)와 같은 틀이다

## R13. 테스트

- **Decision**: `CustomerCenterAcceptanceTest` 한 클래스에 US1~US4 AC를 `@DisplayName("US1-AC1 …")`로 둔다. US5는 회원 탈퇴 스펙 뒤에 같은 클래스에 더한다
  - API로 보는 AC: US1-AC1~AC8, US2-AC1·AC2·AC4·AC5, US3-AC1·AC3(API는 `imageUrls` 여섯 개면 400)·AC4~AC8, US4-AC1·AC3~AC7
  - e2e로 보는 AC: US2-AC3(펼치고 접기), US3-AC2(로그인 뒤 돌아오기), US3-AC3(여섯 장째 막는 안내), US4-AC2(드로어와 이미지 크게 보기)
  - e2e 흐름 둘: 회원이 이미지를 붙여 문의 → 관리자가 답변 → 회원이 알림을 눌러 상세에서 답변을 본다. 관리자가 FAQ를 등록하고 사용자 FAQ에서 분류·검색으로 찾는다
  - US4-AC4의 알림은 `NotificationsAcceptanceTest`처럼 알림 목록 API로 본다(실시간 전달은 007이 확인한다)
- **Rationale**: 원칙 II(API 통합 테스트 기본, e2e는 핵심 흐름), `006-notices`·`016-banner-popup`과 같다
