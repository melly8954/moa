---
status: approved
refs: [FR-MSG-001, FR-MSG-003, FR-MSG-004, FR-MSG-006]
---

# 메신저 화면

<!-- 이 도메인의 화면 목록 정본이다. 색인·메뉴는 README.md -->

## 화면 목록

| ID | 이름 | 경로 | 접근 역할 | refs |
|---|---|---|---|---|
| SCR-MSG-001 | 대화 목록 | /messages | 회원 | FR-MSG-004 |
| SCR-MSG-002 | 대화방 | /messages/[id] | 회원 | FR-MSG-001, FR-MSG-003, FR-MSG-004, INT-STORAGE-001 |
| SCR-MSG-003 | 대화방 나가기 확인 (모달) | (모달) | 회원 | FR-MSG-006 |

- 대화방은 1:1과 파티 채팅이 같은 화면이다. 파티 채팅방 머리에는 파티(SCR-PTY-004)로 가는 링크를 둔다
- PC 폭에서는 대화 목록과 대화방을 나란히, 모바일 폭에서는 따로 보여 준다
