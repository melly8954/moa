# Data Model: 이용약관·개인정보처리방침 공개 페이지 (terms-privacy)

DB 테이블은 없다. 문안은 백엔드 리소스 파일로 버전마다 둔다 (research R1). 백엔드가 시작할 때 한 번 읽어 공개 API로 내려 준다 (R4).

## 공개 문서 (`LegalDocument`)

| enum | 주소 키 | 문서 | 화면 | 010 연결 |
|---|---|---|---|---|
| `TERMS` | `terms` | 이용약관 | `/terms` | 010 `SERVICE`와 같은 원문 (research R2) |
| `PRIVACY` | `privacy` | 개인정보처리방침 | `/privacy` | 없음. 010의 `PRIVACY`(개인정보 수집·이용 동의)는 다른 문서다 |

## 문서 버전 파일 (`src/main/resources/legal/<주소 키>/<버전>.json`)

| 칸 | 형식 | 규칙 |
|---|---|---|
| `version` | 문자열 | 필수. 파일 이름과 같다. 한 문서 안에서 겹치지 않는다. 주소에 쓰므로 영문 소문자·숫자·`-`·`.`만 (예: `draft`, `2026-12-01`) |
| `effectiveDate` | `YYYY-MM-DD` | 필수. 한국 날짜의 시행일. 한 문서 안에서 겹치지 않는다 |
| `title` | 문자열 | 필수. 화면 제목 (예: "moa 개인정보처리방침") |
| `sections` | `{ id, heading, html }[]` | 1개 이상. `id`는 장 바로 가기 주소(`#id`)라 겹치지 않는다. `heading`은 장 제목(목차). `html`은 장 본문(문단, `h3` 조 제목, 목록, 표, 링크). 스크립트·이벤트 속성·`javascript:` 주소는 넣지 않는다 |

- 지우지 않는다. 문안이 바뀌면 새 파일을 더한다 (FR-007)
- 위치는 `app.legal.location`(기본 `classpath:legal/`). 시작 때 검사(FR-008, research R6)

## 버전 상태 (칸이 아니라 오늘 날짜로 정한다, research R5)

```text
effectiveDate > 오늘                                   → UPCOMING (예정)
effectiveDate ≤ 오늘 중 effectiveDate가 가장 늦은 것    → CURRENT (지금 시행 중)
그 밖의 effectiveDate ≤ 오늘                            → PAST (예전)
```

- 오늘은 요청 때의 한국 날짜다(`app.legal.zone`, 기본 `Asia/Seoul`). 시행일 0시(한국 시간)부터 그 버전이 지금 시행 중이다
- 예정 안내(US3-AC3)는 예정 버전 중 시행일이 가장 이른 것 하나만 보인다

## 설정 (`app.legal`)

| 키 | 기본 | 뜻 |
|---|---|---|
| `location` | `classpath:legal/` | 문안 파일 위치. 인수 테스트는 `classpath:legal-test/` |
| `zone` | `Asia/Seoul` | "오늘"을 정하는 시간대 |

## 처음 넣을 문안

| 문서 | 버전 | 시행일 | 내용 |
|---|---|---|---|
| 이용약관 | `draft` | `2026-11-01` (개발용) | 010 자리표시 문안을 옮긴다. `app.terms.service-version`(`draft`)과 같은 버전이다. 실서비스 전에 팀 문안으로 바꾼다 |
| 개인정보처리방침 | `draft` | `2026-11-01` (개발용) | 2026-11-21 초안 + 018 탈퇴(30일 복구·파기). 보관 기간 표는 `docs/privacy.md` "보관 기간 표"와 같다. 운영 주체·보호책임자·호스팅 업체는 빈칸(`[운영 주체]` 등)으로 둔다 |

- 빈칸이 남은 문안은 실서비스에 배포하지 않는다 (spec Assumptions)
