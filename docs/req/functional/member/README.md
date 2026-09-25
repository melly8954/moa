# 회원 (MEM)

<!-- 이 도메인 기능 목록의 정본이다. 우선순위는 여기에만 적는다 (harness-psw 3.2) -->

- 도메인 정책·상태 전이: `_policy.md`

<!-- ck-fun-2 -->

| ID | 기능 | 우선순위 | 파일 |
|---|---|---|---|
| FR-MEM-001 | 회원가입 | must | `001-sign-up.md` |
| FR-MEM-002 | 로그인 | must | `002-sign-in.md` |
| FR-MEM-003 | 로그아웃 | must | `003-sign-out.md` |
| FR-MEM-004 | 프로필 편집 | must | `004-edit-profile.md` |
| FR-MEM-005 | 프로필 보기 | must | `005-view-profile.md` |
| FR-MEM-006 | 회원 검색 | must | `006-search-member.md` |
| FR-MEM-007 | 계정 공개 범위 설정 | must | `007-account-visibility.md` |
| FR-MEM-008 | 회원 탈퇴 | must | `008-withdraw.md` |

## 정책 후보

<!-- 인터뷰에서 나온 규칙·수치 후보다. psw-req가 _policy.md로 옮기고 이 절을 지운다 -->

- 관계 모델: 팔로우만 둔다. 서로 팔로우하면 친구다 (README MVP 절 결정)
- 비공개 계정의 글은 승인된 팔로워만 본다. 게시물별 공개 범위는 두지 않는다
- 게임 계정·티어는 회원이 직접 입력하고 검증하지 않는다
