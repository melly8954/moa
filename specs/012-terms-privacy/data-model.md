# Data Model: 이용약관·개인정보처리방침 공개 페이지 (terms-privacy)

DB 테이블은 없다. 문안은 프론트 코드에 버전마다 둔다 (research R1). 서버 API도 없다.

## 공개 문서 (`LegalDocumentKey`)

| 값 | 문서 | 경로 | 010 연결 |
|---|---|---|---|
| `terms` | 이용약관 | `/terms` | 010 `SERVICE`와 같은 원문 (research R2) |
| `privacy` | 개인정보처리방침 | `/privacy` | 없음. 010의 `PRIVACY`(개인정보 수집·이용 동의)는 다른 문서다 |

## 문서 버전 (`LegalVersion`, `src/content/legal/<문서>/<버전>.ts`)

| 칸 | 형식 | 규칙 |
|---|---|---|
| `version` | 문자열 | 필수. 한 문서 안에서 겹치지 않는다. 주소에 쓰므로 영문 소문자·숫자·`-`·`.`만 (예: `draft`, `2026-12-01`) |
| `effectiveDate` | `YYYY-MM-DD` | 필수. 한국 날짜의 시행일. 한 문서 안에서 겹치지 않는다 |
| `title` | 문자열 | 필수. 화면 제목 (예: "moa 개인정보처리방침") |
| `sections` | `{ id, heading, html }[]` | 1개 이상. `id`는 장 바로 가기 주소(`#id`)라 겹치지 않는다. `heading`은 장 제목(목차). `html`은 장 본문(문단, `h3` 조 제목, 목록, 표, 링크) |

- 문서마다 버전 목록은 `src/content/legal/index.ts`가 모은다. 지우지 않는다 (FR-007)
- 배포 전 검사(FR-008)는 이 목록을 읽는 단위 테스트다 (research R6)

## 버전 상태 (칸이 아니라 오늘 날짜로 정한다, research R5)

```text
effectiveDate > 오늘                                   → 예정 (upcoming)
effectiveDate ≤ 오늘 중 effectiveDate가 가장 늦은 것    → 지금 시행 중 (current)
그 밖의 effectiveDate ≤ 오늘                            → 예전 (past)
```

- 오늘은 요청 때의 한국 날짜다. 시행일 0시(한국 시간)부터 그 버전이 지금 시행 중이다
- 예정 안내(US3-AC3)는 예정 버전 중 시행일이 가장 이른 것 하나만 보인다

## 처음 넣을 문안

| 문서 | 버전 | 시행일 | 내용 |
|---|---|---|---|
| 이용약관 | `draft` | `2026-11-01` (개발용) | 010 자리표시 문안을 옮긴다. 실서비스 전에 팀 문안으로 바꾼다 |
| 개인정보처리방침 | `draft` | `2026-11-01` (개발용) | 2026-11-21 초안 + 018 탈퇴(30일 복구·파기). 보관 기간 표는 `docs/privacy.md`와 같다. 운영 주체·보호책임자·호스팅 업체는 빈칸(`[운영 주체]` 등)으로 둔다 |

- 빈칸이 남은 문안은 실서비스에 배포하지 않는다 (spec Assumptions)
