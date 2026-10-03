---
status: approved
refs: [FR-NTF-001, FR-PTY-007]
---

# 알림 화면

<!-- 이 도메인의 화면 목록 정본이다. 색인·메뉴는 README.md -->

## 화면 목록

| ID | 이름 | 경로 | 접근 역할 | refs |
|---|---|---|---|---|
| SCR-NTF-001 | 알림 목록 | /notifications | 회원 | FR-NTF-001, FR-PTY-007 |

- 안 읽은 수 배지는 셸의 알림 메뉴에 붙는다. WebSocket으로 바로 오른다
- 파티 초대 알림에는 수락·거절 버튼을 둔다. 초대받은 본인만, 수락할 때 자리가 남아 있어야 한다 (FR-PTY-007, `actors.md`)
