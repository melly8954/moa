# API: 고객센터 (customer-center)

모든 응답·오류 모양은 골격 규칙(`ErrorResponse`, 칸 오류)을 따른다. 일시는 `OffsetDateTime`이다. 칸 규칙은 [data-model.md](../data-model.md), 결정의 이유는 [research.md](../research.md)를 본다.

## 사용자 FAQ (로그인 없이)

| 요청 | 응답 | AC |
|---|---|---|
| `GET /api/v1/faqs?categoryCode=&keyword=` | 200 `[{id, categoryCode, question, answer}]`. 켜져 있고 분류가 사용 중인 FAQ만, FR-006 순서 (R9) | US2-AC1·AC2·AC4·AC5 |
| `GET /api/v1/faqs/categories` | 200 `[{code, name}]`. 지금 고를 수 있는 분류만 코드 관리의 순서대로. FAQ 분류 단추와 문의·FAQ 쓰기의 분류 고르기에 쓴다. 공통코드 화면용 목록(`/api/v1/codes/...`)은 로그인이 필요해 따로 둔다 (2026-11-25 구현 때 더함) | US2-AC1·AC5 |

## 회원 문의 (로그인 필요, 관리자 계정은 403 `A005`)

| 요청 | 응답 | AC |
|---|---|---|
| `GET /api/v1/inquiries/me?page=&size=` | 200 쪽 `{id, categoryCode, categoryName, title, status, createdAt}`, 쓴 최신순 | US3-AC1 |
| `POST /api/v1/inquiries` `{categoryCode, title, content, imageUrls[0..5]}` | 201 내 문의 상세와 같은 모양. 칸 오류 400 `V001` (여섯 장 이상, 없는 이미지, 사용 중이 아닌 분류 포함) | US3-AC1·AC3·AC8 |
| `GET /api/v1/inquiries/{id}` | 200 `{id, categoryCode, categoryName, title, content, imageUrls, status, createdAt, answer, answeredAt, deletable}`. 남의 것·없는 것 404 `R001`. 답변한 관리자는 넣지 않는다 | US3-AC1·AC4·AC6·AC7 |
| `DELETE /api/v1/inquiries/{id}` | 204. 답변 완료면 409 `B001`, 남의 것·없는 것 404 `R001` | US3-AC5·AC6 |

- 로그인하지 않으면 401 `A001` (골격). 화면은 `AuthGate`가 로그인으로 보낸다 (US3-AC2)

## 관리 FAQ (`FAQ_MANAGE`)

| 요청 | 응답 | AC |
|---|---|---|
| `GET /api/v1/admin/faqs?categoryCode=&enabled=&keyword=` | 200 전체 `[{id, categoryCode, categoryName, question, sortOrder, enabled, updatedAt}]` (쪽 없음, 상한 200) | US1-AC1~AC3 |
| `GET /api/v1/admin/faqs/{id}` | 200 `{… , answer}` | US1-AC4 |
| `POST /api/v1/admin/faqs` `{categoryCode, question, answer, sortOrder, enabled}` | 201 FAQ 하나와 같은 모양. 칸 오류 400 `V001`, 200개면 409 `B301` | US1-AC1·AC8 |
| `PUT /api/v1/admin/faqs/{id}` | 200 FAQ 하나와 같은 모양. 분류를 바꿀 때 고를 수 없는 분류면 400 `V001` | US1-AC2~AC4 |
| `DELETE /api/v1/admin/faqs/{id}` | 204 | US1-AC5 |
| `POST /api/v1/admin/faqs/bulk-delete` `{ids[1..200]}` | 204. 하나라도 없으면 404 `R001`, 아무것도 지우지 않는다 | US1-AC5 |
| `PUT /api/v1/admin/faqs/enabled` `{ids[1..200], enabled}` | 204. 하나라도 없으면 404 `R001`. 이미 그 값이면 그대로 | US1-AC6 |

- 권한이 없으면 403 `A005` (US1-AC7)

## 관리 문의 (`INQUIRY_MANAGE`)

| 요청 | 응답 | AC |
|---|---|---|
| `GET /api/v1/admin/inquiries?status=&categoryCode=&from=&to=&keyword=&page=&size=` | 200 쪽 `{id, status, categoryCode, categoryName, title, author{userId, loginId, name}, createdAt, answeredAt}`. `status` 없으면 `WAITING` (R6) | US4-AC1·AC7 |
| `GET /api/v1/admin/inquiries/{id}` | 200 `{… , content, imageUrls, answer, answeredByName, answerable}` | US4-AC2 |
| `PUT /api/v1/admin/inquiries/{id}/answer` `{content}` | 204. 처음이면 답변 완료 + 알림 `INQUIRY_ANSWERED`, 이미 답변했으면 내용만 바꾸고 알림 없음. 없으면 404 `R001`, 작성자 탈퇴면 409 `B001`(US5) | US4-AC3~AC5, US5-AC1 |

- 권한이 없으면 403 `A005` (US4-AC6)

## 알림 (기존 API, `007-notifications`)

- 답변 뒤 회원의 `GET /api/v1/notifications`에 `{type: "INQUIRY_ANSWERED", title: "1:1 문의에 답변이 등록되었습니다", link: "/support/inquiries/{id}"}`가 있다 (US4-AC4)

## 새 오류 코드

| 코드 | HTTP | 문구 |
|---|---|---|
| `B301` `FAQ_LIMIT_EXCEEDED` | 409 | FAQ는 200개까지 둘 수 있습니다. |

- 구현 PR 직전에 다른 브랜치와 번호가 겹치지 않는지 본다 (research R9)
