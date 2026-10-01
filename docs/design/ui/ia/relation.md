---
status: draft
refs: [FR-REL-001, FR-REL-002, FR-REL-003, FR-REL-004, FR-REL-005]
---

# 관계 화면

<!-- 이 도메인의 화면 목록 정본이다. 색인·메뉴는 README.md -->

## 화면 목록

| ID | 이름 | 경로 | 접근 역할 | refs |
|---|---|---|---|---|
| SCR-REL-001 | 팔로워·팔로잉 목록 | /u/[nickname]/follows | 비회원, 회원, 관리자 | FR-REL-003, FR-REL-001 |
| SCR-REL-002 | 팔로우 요청 | /follow-requests | 회원 | FR-REL-002 |
| SCR-REL-003 | 친구 목록 | /friends | 회원 | FR-REL-004, FR-MSG-001, FR-PTY-007 |
| SCR-REL-004 | 차단 확인 (모달) | (모달) | 회원 | FR-REL-005 |
| SCR-REL-005 | 차단 목록 | /settings/blocks | 회원 | FR-REL-005 |

- 팔로우·언팔로우 버튼은 프로필(SCR-MEM-009), 게시물(SCR-FEED-003), 이 도메인 목록에 있다
- 팔로우 요청(SCR-REL-002)은 비공개 계정 회원에게만 메뉴에 보인다
