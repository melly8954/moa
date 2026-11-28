# Contract: 공개 문서 API

요청·응답 전체는 local 프로필의 `/v3/api-docs`가 기준이다. 오류 응답 형식과 코드는 골격 `ErrorCode`를 따른다.
칸의 규칙은 [data-model.md](../data-model.md). 일시가 아니라 날짜(`LocalDate`, `YYYY-MM-DD`)를 쓴다.

`/api/v1/legal`. `@PermitAll` (로그인 없이 연다). 토큰이 있어도 같은 응답이다 (FR-001).

| 요청 | 설명 | 응답 |
|---|---|---|
| `GET /api/v1/legal/{document}` | 지금 시행 중인 버전. `document`는 `terms`·`privacy` | 200 {document, current: {version, effectiveDate, title, sections[{id, heading, html}]}, upcoming: {version, effectiveDate} 또는 null, versions: [{version, effectiveDate, state}]}. `versions`는 시행일 최근순이고 예정 버전도 든다 |
| `GET /api/v1/legal/{document}/versions/{version}` | 버전 하나 (예전·지금·예정) | 200 {document, version, effectiveDate, title, sections[…], state, currentVersion} |

- `state`: `CURRENT`, `PAST`, `UPCOMING` (data-model "버전 상태")
- 없는 `document`, 없는 `version`이면 404 `R001` (주소 값을 문자열로 받아 서비스가 판단한다. 400이 되지 않는다, research R4)
- 010 가입 화면은 이용약관 칸의 전문을 `GET /api/v1/legal/terms/versions/{가입 동의 버전}`으로 받는다 (research R2). 가입 동의 버전은 지금처럼 `GET /api/v1/terms`

## AC와 확인 방법

인수 테스트 `LegalAcceptanceTest`는 테스트용 문안(`classpath:legal-test/`, 문서마다 `old` 2000-01-01, `draft` 2020-01-01, `next` 2099-01-01)으로 본다.

| AC | 호출 | 기대 |
|---|---|---|
| US1-AC1 | 토큰 없이 `GET /api/v1/legal/privacy` | 200, `current.version`=`draft`, 제목·시행일·장 |
| US1-AC2 | 토큰 없이 `GET /api/v1/legal/terms` | 200, 같은 규칙 |
| US1-AC3 | 회원 토큰, 관리자 계정 토큰으로 같은 요청 | 둘 다 200, 토큰 없을 때와 같은 본문 |
| US3-AC1 | `GET /api/v1/legal/privacy` | `versions`가 `next`(UPCOMING), `draft`(CURRENT), `old`(PAST) 순 |
| US3-AC2 | `GET …/versions/old` | 200, `state`=`PAST`, `currentVersion`=`draft` |
| US3-AC3 | `GET /api/v1/legal/privacy`, `GET …/versions/next` | `upcoming`={next, 2099-01-01}, 예정 버전 `state`=`UPCOMING` |
| US3-AC4 | 단위 테스트 `LegalServiceTest` | 시행일 전날은 예정, 당일(한국 날짜)부터 지금 버전 |
| (404) | `GET /api/v1/legal/cookies`, `GET …/versions/none` | 404 `R001` |

US1-AC4~AC6, US2 전부, US3의 화면 쪽(안내 띠, 목록, 예전 버전 안내)은 e2e가 본다 (research R8).
