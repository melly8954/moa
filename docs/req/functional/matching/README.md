# 매칭 (MAT)

<!-- 이 도메인 기능 목록의 정본이다. 우선순위는 여기에만 적는다 (harness-psw 3.2) -->

- 도메인 정책·상태 전이: `_policy.md`

<!-- ck-fun-2 -->

| ID | 기능 | 우선순위 | 파일 |
|---|---|---|---|
| FR-MAT-001 | 플레이 성향 설정 | should | `001-play-style.md` |
| FR-MAT-002 | AI 친구 추천 | should | `002-recommend-friend.md` |
| FR-MAT-003 | AI 파티 추천 | should | `003-recommend-party.md` |
| FR-MAT-004 | AI 게시물 추천 | should | `004-recommend-post.md` |

## 정책 후보

<!-- 인터뷰에서 나온 규칙·수치 후보다. psw-req가 _policy.md로 옮기고 이 절을 지운다 -->

- 매칭 기능은 범위 안이지만 MVP 이후에 구현한다
- 추천 근거: 플레이하는 게임, 티어, 플레이 성향, 평판. 추천 방식(AI 모델, 외부 AI API 여부)은 설계에서 정한다
- 차단 관계인 회원은 서로 추천하지 않는다
