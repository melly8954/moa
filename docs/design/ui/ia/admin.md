---
status: draft
refs: [FR-ADM-001, FR-ADM-002, FR-ADM-003, FR-ADM-004, FR-ADM-005]
---

# 관리 화면

<!-- 이 도메인의 화면 목록 정본이다. 색인·메뉴는 README.md -->

## 화면 목록

| ID | 이름 | 경로 | 접근 역할 | refs |
|---|---|---|---|---|
| SCR-ADM-001 | 신고 목록 | /admin/reports | 관리자 | FR-ADM-001 |
| SCR-ADM-002 | 신고 상세 | /admin/reports/[id] | 관리자 | FR-ADM-001 |
| SCR-ADM-003 | 회원 목록 | /admin/members | 관리자 | FR-ADM-002, FR-ADM-003, FR-ADM-005, SEC-PRIV-002 |
| SCR-ADM-004 | 회원 상세 | /admin/members/[id] | 관리자 | FR-ADM-002, FR-ADM-003, FR-ADM-005, SEC-PRIV-002 |
| SCR-ADM-005 | 제재 입력 (모달) | (모달) | 관리자 | FR-ADM-002 |
| SCR-ADM-006 | 게임 목록 | /admin/games | 관리자 | FR-ADM-004 |
| SCR-ADM-007 | 게임 편집 | /admin/games/[id] | 관리자 | FR-ADM-004 |

- 회원 상세의 휴대폰 번호는 가운데 4자리를 가린다 (SEC-PRIV-002)
