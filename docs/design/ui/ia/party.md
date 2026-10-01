---
status: draft
refs: [FR-PTY-001, FR-PTY-002, FR-PTY-003, FR-PTY-004, FR-PTY-005, FR-PTY-006, FR-PTY-007, FR-PTY-008, FR-PTY-009, FR-PTY-010]
---

# 파티 화면

<!-- 이 도메인의 화면 목록 정본이다. 색인·메뉴는 README.md -->

## 화면 목록

| ID | 이름 | 경로 | 접근 역할 | refs |
|---|---|---|---|---|
| SCR-PTY-001 | 파티 모집 목록 | /parties | 비회원, 회원, 관리자 | FR-PTY-002 |
| SCR-PTY-002 | 모집 상세 | /recruits/[id] | 비회원, 회원, 관리자 | FR-PTY-003, FR-REP-002 |
| SCR-PTY-003 | 파티 만들기 | /parties/new | 회원 | FR-PTY-001 |
| SCR-PTY-004 | 파티 | /parties/[id] | 회원 | FR-PTY-005, FR-PTY-006, FR-PTY-008, FR-PTY-009 |
| SCR-PTY-005 | 모집 열기 | /parties/[id]/recruit | 회원 | FR-PTY-010 |
| SCR-PTY-006 | 참여 신청 목록 | /parties/[id]/requests | 회원 | FR-PTY-004, FR-REP-002 |
| SCR-PTY-007 | 친구 초대 (모달) | (모달) | 회원 | FR-PTY-007 |
| SCR-PTY-008 | 파티원 조치 확인 (모달) | (모달) | 회원 | FR-PTY-005, FR-PTY-008, FR-PTY-009 |

- 파티(SCR-PTY-004)는 파티원만 연다. 방장 전용 버튼(내보내기, 마감·취소, 방장 넘기기, 모집 열기, 신청 목록)은 방장에게만 보인다
- 내 파티는 대화 목록(SCR-MSG-001)의 파티 채팅방에서 들어간다
- 파티 초대 수락·거절은 알림 목록(SCR-NTF-001)의 초대 알림에서 한다
