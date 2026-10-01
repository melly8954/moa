---
status: draft
refs: [FR-FEED-001, FR-FEED-002, FR-FEED-003, FR-FEED-004, FR-FEED-005, FR-FEED-006, FR-FEED-007, FR-FEED-008]
---

# 피드 화면

<!-- 이 도메인의 화면 목록 정본이다. 색인·메뉴는 README.md -->

## 화면 목록

| ID | 이름 | 경로 | 접근 역할 | refs |
|---|---|---|---|---|
| SCR-FEED-001 | 홈 피드 | / | 회원 | FR-FEED-003, FR-FEED-005 |
| SCR-FEED-002 | 게임별 피드 | /games/[game] | 비회원, 회원, 관리자 | FR-FEED-004, FR-FEED-005 |
| SCR-FEED-003 | 게시물 상세 | /posts/[id] | 비회원, 회원, 관리자 | FR-FEED-005, FR-FEED-006, FR-REL-001 |
| SCR-FEED-004 | 게시물 작성 | /posts/new | 회원 | FR-FEED-001, INT-STORAGE-001 |
| SCR-FEED-005 | 게시물 수정 | /posts/[id]/edit | 회원 | FR-FEED-002, INT-STORAGE-001 |
| SCR-FEED-006 | 게시물 삭제 확인 (모달) | (모달) | 회원 | FR-FEED-002 |
| SCR-FEED-007 | 해시태그 게시물 | /tags/[tag] | 비회원, 회원, 관리자 | FR-FEED-007 |
| SCR-FEED-008 | 신고 (모달) | (모달) | 회원 | FR-FEED-008 |

- 비회원이 `/`로 오면 게임별 피드(SCR-FEED-002)로 보낸다
- 좋아요·댓글·신고 버튼은 비회원에게 로그인 안내로 바뀐다
