# Research: 배너·팝업 관리 (banner-popup)

Technical Context에 미결은 없다. 아래는 moa 코드와 키트 헬퍼를 확인해 정한 것이다.

## R1. 배너 이미지 올리기·저장

- **Decision**: 공지에서 쓰는 키트 rich-text 헬퍼의 이미지 기능을 그대로 쓴다. 키트 코드는 고치지 않는다
  - 올리기: 화면이 `POST /api/v1/rich-text/images`(multipart `file`)로 올리고 받은 `url`(`/api/v1/rich-text/images/{publicId}`)을 배너 요청에 넣는다
  - 저장: `tb_banner`에 이미지 주소(`wide_image_url`, `narrow_image_url`)를 둔다. 공지 본문 HTML이 이미지 주소를 품는 것과 같다
  - 잇기·풀기: `BannerApplication`이 두 주소로 `<img src="…">` 조각을 만들어 `richTextImageService.attach("BANNER", id, html)`을 부른다. 이미지를 바꾸면 빠진 이미지는 attach가 임시로 돌리고, 지우면 `detach("BANNER", id)`한다. 임시 이미지는 키트 정리기가 24시간 뒤 파일과 함께 지운다 (spec Edge Cases)
  - 형식: rich-text 올리기는 PNG·JPEG·GIF·WEBP를 받는다. 배너는 저장할 때 이미지의 형식을 확인해 GIF면 400 `V001`이다(FR-003 JPG·PNG·WebP). 화면은 파일 고르기에서 `image/jpeg,image/png,image/webp`만 보인다
  - 크기: rich-text 상한(`app.rich-text.image-max-size`, 기본 5MB)을 그대로 쓴다. 넘으면 올리기에서 400 `V001`이다
- **Rationale**: 이미지 저장, 공개 주소, 임시 정리, 파기 이력(`PurgeTarget.RICH_TEXT_IMAGE`)이 이미 있다. 새 헬퍼를 얹지 않으니 팀원에게 따로 알릴 것도 없다 (`specs/README.md` 추가로 쓸 키트 헬퍼). `attach`가 HTML만 받으므로 주소 두 개로 짧은 HTML을 만든다
- **Alternatives considered**:
  - 키트 file 헬퍼를 새로 얹기: 저장·정리·파기 기록을 다시 잇고 사용자 쪽에도 영향이 간다. 이미지 두 장을 위해 들이기엔 크다
  - rich-text 헬퍼에 `attachIds` 같은 메서드를 더하기: 키트 코드를 고치게 된다 (원칙 III). 필요하면 키트에 먼저 넣고 옮긴다
  - 이미지 `public_id`나 `tb_rich_text_image.id`를 칸에 두기: 응답마다 주소를 다시 만들어야 하고, 공지와 저장 방식이 달라진다

## R2. 권장 크기와 미리보기 (FR-003, US1-AC10)

- **Decision**: 권장 크기는 넓은 화면 1200×300(4:1), 좁은 화면 720×360(2:1)이다. 크기를 강제하지 않고 등록 화면에 안내만 한다. 드로어의 미리보기는 화면에서만 그린다: 넓은 화면 틀(가로 4:1)과 좁은 화면 틀(휴대폰 폭, 2:1)에 이미지를 `object-cover`로 채워 나란히 보인다. 좁은 화면용이 없으면 좁은 틀에 넓은 화면용을 넣는다
- **Rationale**: 노출 자리가 아직 없어(FR-011) 정확한 틀을 정할 수 없다. 흔한 홍보 배너 비율로 안내하고, 자리가 정해지면 그 스펙이 권장 크기와 미리보기 틀을 고친다. 서버가 크기를 검사하면 자리를 정한 뒤 다시 바꿔야 한다
- **Alternatives considered**: 서버에서 가로세로를 검사. 위 이유로 버렸다

## R3. 상태 (FR-006)

- **Decision**: 상태는 칸에 저장하지 않고 읽을 때 정한다. `ExposureStatus`(`SCHEDULED` 예정, `ACTIVE` 노출 중, `ENDED` 끝남, `DISABLED` 꺼짐)를 `dto`에 두고 배너·팝업이 같이 쓴다
  - `enabled = false` → `DISABLED`. 그 밖에 지금 < `starts_at` → `SCHEDULED`, `ends_at`이 있고 `ends_at` ≤ 지금 → `ENDED`, 나머지 → `ACTIVE`
  - 목록 거르기도 같은 조건을 질의에 쓴다(QueryDSL, 지금 시각을 인자로 받는다)
- **Rationale**: 기간이 지나면 손대지 않아도 상태가 바뀌어야 한다(SC-002). 저장하면 정해진 시각마다 바꾸는 작업이 필요하다
- **Alternatives considered**: `status` 칸과 스케줄러. 위 이유로 버렸다

## R4. 목록 검색과 기본 거르기 (FR-008)

- **Decision**: `GET /api/v1/admin/banners?keyword=&status=&page=&size=`. `keyword`는 이름(팝업은 제목) 포함, `status`는 `SCHEDULED`·`ACTIVE`·`ENDED`·`DISABLED`·`ALL`. `status`가 없으면 "끝남 빼고"(켜져 있고 끝나지 않은 것과 꺼진 것)다. 화면의 상태 거르기 기본값도 "끝남 빼고"이고 "전체"는 `ALL`이다
- 정렬은 `sort_order ASC, starts_at DESC, id DESC` (FR-005)
- **Rationale**: spec FR-008(기본 거르기는 "끝남"을 뺀 상태). `useListSearch`의 빈 값이 "거르지 않음"이라 빈 값을 "끝남 빼고"로 두고 전체를 따로 둔다
- **Alternatives considered**: 상태 여러 개를 고르는 거르기. 키트 `ChoiceFilter`가 하나만 고른다

## R5. 노출 기간 입력과 시간대

- **Decision**: 화면은 키트 `DatePicker` + `TimeInput`(시:분)을 짝지어 시작·끝 일시를 받는다. 요청은 `OffsetDateTime`(예: `2026-12-01T10:00:00+09:00`)이고, 백엔드가 `LocalDateTime`(서버 기본 시간대)으로 바꿔 저장한다. 응답은 `ApiTimes.of`로 다시 `OffsetDateTime`이다
  - 끝 일시는 비울 수 있다. 있으면 시작보다 뒤여야 한다(같거나 앞이면 400 `V001`, 칸 오류 `endsAt`)
  - 시작 일시가 지난 시각이어도 받는다 (US1-AC1의 "노출 중")
- **Rationale**: `ArchitectureTest`가 컨트롤러 쪽 `LocalDateTime` 칸을 막는다. 화면 표시는 `SITE.timeZone`(Asia/Seoul)이다. 서버 시간대는 공지 게시 시각과 같은 가정(서버가 한국 시간)을 따른다
- **Alternatives considered**: 날짜·시각을 합친 새 입력 부품. 키트에 없고 두 부품을 짝짓는 것으로 충분하다

## R6. 권한, 연결 화면, 메뉴

- **Decision**:
  - 권한: `BannerPermissions` `BANNER_MANAGE`("배너 관리", 묶음 "마케팅"), `PopupPermissions` `POPUP_MANAGE`("팝업 관리", 묶음 "마케팅")
  - 연결 화면: `MarketingScreens`(`@Order(70)`)가 `ADMIN_BANNERS`(`/admin/banners`)와 `ADMIN_POPUPS`(`/admin/popups`)를 낸다
  - 메뉴: 마이그레이션 한 파일(번호는 PR 직전 마지막+1, 지금은 `V11`)이 상위 메뉴 "마케팅"(`icon` `megaphone`, `sort_order` 15, `audience` `ADMIN`)을 넣고, `LAST_INSERT_ID()`로 그 아래 "배너 관리"(`ADMIN_BANNERS`, `folder`, 10)와 "팝업 관리"(`ADMIN_POPUPS`, `file`, 20)를 넣는다. 기존 메뉴 행은 고치지 않는다
- **Rationale**: 운영(10)과 설정(20) 사이에 두려면 순서 15면 된다. 기존 행을 고치지 않아 관리자가 바꾼 메뉴를 덮지 않는다 (`docs/db.md` 메뉴 행 규칙). 상위 메뉴는 `screen_key`가 없어 같은 파일에서 방금 넣은 `id`로 잇는다. 키트 아이콘 표에 이미지 아이콘이 없어 있는 것 중에서 고른다
- **Alternatives considered**:
  - 설정의 순서를 30으로 밀기: 기존 메뉴 행을 고치게 된다
  - 권한 하나("마케팅 관리"): spec FR-002(연결 화면마다 권한 하나)
- `001-admin-menus`의 spec(FR-001 표, US1-AC1)·data-model과 `003-admin-roles` US3-AC4(권한 트리에 마케팅이 끼어 "마케팅"·"설정"이 접힘)는 이 문서 PR에서 고친다. `AdminMenusAcceptanceTest`(상위 메뉴, 하위 메뉴, 연결 화면 목록), `AdminRolesAcceptanceTest`(권한 트리의 상위 메뉴·하위 메뉴·권한 키), 화면 검사의 가짜 메뉴(`frontend/e2e/support/api.ts`)와 가짜 권한 트리(`frontend/e2e/role.spec.ts`)는 구현 PR에서 그 스펙만큼 고친다 (원칙 IV·V)

## R7. 일괄 처리 (FR-008)

- **Decision**: 메뉴·공통코드·공지와 같은 모양으로 둔다
  - `POST /api/v1/admin/banners/bulk-delete` `{ids}` (1~200개) → 204. 하나라도 없으면 404 `R001`이고 아무것도 지우지 않는다. 지울 때 배너마다 `detach("BANNER", id)`
  - `PUT /api/v1/admin/banners/enabled` `{ids, enabled}` (1~200개) → 204. 없으면 404 `R001`, 아무것도 바꾸지 않는다. 이미 그 값이면 그대로 둔다
  - 팝업은 `/api/v1/admin/popups/...`로 같다(지울 때 `detach("POPUP", id)`)
  - 한 Application 메서드 = 한 트랜잭션. 서비스는 모든 `id`를 먼저 확인한 뒤(`requireAll`) 바꾼다
  - 화면: `DataTable`의 `selection`(`rowLabel` = 이름·제목) + `BulkToolbar`. 단추는 켜기·끄기·삭제 순서이고 삭제만 `confirm`을 받는다. 고른 것은 목록 검색 값(`params`)을 열쇠로 두어 쪽·검색이 바뀌면 저절로 풀린다(사용자 관리 일괄 삭제와 같은 방식). 처리에 성공해도 푼다
- **Rationale**: `MenuApplication.setEnabled`·`deleteAll`, `CodeApplication.setEnabled`, `006-notices` R13, `002-admin-users` R18과 같다. 처리할 수 없는 경우는 그 사이 지워진 것뿐이다 (spec Edge Cases)
- **Alternatives considered**: 하나씩 여러 번 부르기. 중간에 실패하면 일부만 바뀐다 (FR-008 MUST)

## R8. 팝업 내용 (FR-004)

- **Decision**: `content`에 `@SafeHtml @NotBlank @Size(max = 20000)`. 만들기·고치기 뒤 `attach("POPUP", id, content)`, 지울 때 `detach`. 편집기는 공지와 같은 `RichTextEditor`
- **Rationale**: `006-notices` R4와 같다. US2-AC7은 `<script>`가 든 내용을 저장한 뒤 상세 응답에 없는지로 본다

## R9. 관리 화면 모양

- **Decision**: 두 화면 모두 목록(`ListPage`, `SearchPanel`(상태 `ChoiceFilter`, `KeywordFilter`), `BulkToolbar`, `DataTable`) + 행을 누르면 여는 `SideDrawer`다. 등록 단추도 같은 드로어를 빈 채로 연다. 저장하지 않은 채 닫으면 묻는다(`useDrawerClose`)
  - 배너 목록 열: 작은 이미지, 이름, 기간, 순서, 상태
  - 팝업 목록 열: 제목, 기간, 순서, 상태
  - 팝업 드로어는 편집기가 들어가므로 넓은 드로어(`className`으로 폭을 넓힘)를 쓴다
  - 머리 설명은 `t.rich`의 `<link>`로 공지 관리에 잇는다 (FR-010)
- **Rationale**: spec FR-008("목록 + 드로어 + 체크박스 일괄 처리"). 공지는 편집기 때문에 화면을 나눴지만(006 R8), 팝업은 짧은 안내라 드로어 안 편집기로 충분하다
- **Alternatives considered**: 공지처럼 쓰기·고치기 화면을 따로 두기. 001의 관리 화면 틀과 달라진다

## R10. 입력 규칙

| 칸 | 규칙 |
|---|---|
| 이름(배너) | 1~100자 |
| 대체 문구(배너) | 1~200자 |
| 제목(팝업) | 1~200자 |
| 이미지 주소 | `/api/v1/rich-text/images/`로 시작하고 저장된 이미지여야 한다(키트 `load`로 확인, 없으면 `R001` 대신 `V001`). 넓은 화면은 필수 |
| 연결 주소 | 비우거나, `/`로 시작하되 `//`로 시작하지 않거나, `http://`·`https://`로 시작. 500자 이하 |
| 순서 | 0~9999 정수. 화면 기본값 0 |
| 켜짐 | 화면 기본값 켜짐 |

- 어기면 400 `V001`과 칸 오류. 화면은 그 칸 아래에 이유를 보인다 (US1-AC7, US2-AC4)
- `//`로 시작하는 주소는 다른 사이트로 가는 주소라 내부 주소로 보지 않는다
- 이미지의 주인은 확인하지 않는다 (2026-11-24 결정). 키트에 주인을 알려 주는 공개 메서드가 없고, `attach`는 다른 글이 쓰는 이미지를 건너뛴다. 화면은 늘 새로 올린 이미지를 넣으므로, 관리자가 API로 다른 공지·배너의 이미지 주소를 넣을 때만 생긴다. 그때는 그쪽이 지워지면 이 배너의 이미지도 정리되어 깨진다
  - 버린 안: moa reader가 키트 `RichTextImageRepository`를 읽어 주인을 확인. 키트 안쪽 구조에 기댄다

## R11. 테스트

- **Decision**: `BannerPopupAcceptanceTest` 한 클래스에 US1·US2 AC를 `@DisplayName("US1-AC1 …")`로 둔다. 화면에서만 보이는 동작은 e2e로 본다
  - API로 보는 AC: US1-AC1~AC9, AC11~AC14(API 쪽), US2-AC1~AC8
  - e2e로 보는 AC: US1-AC10(미리보기), AC11·AC12의 "고른 것이 풀린다", AC7의 "그 칸에 이유"
  - e2e 흐름 둘: 배너를 이미지와 함께 등록하고 미리보기를 본다(3폭, 가로 넘침·접근성), 목록에서 둘을 골라 끄고 지운다
- 상태는 시작·끝 일시를 과거·미래로 넣어 만든다. 시계를 바꾸지 않는다
- **Rationale**: 원칙 II(API 통합 테스트 기본, e2e는 핵심 흐름), `006-notices` R10과 같다
