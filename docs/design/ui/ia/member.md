---
status: draft
refs: [FR-MEM-001, FR-MEM-002, FR-MEM-003, FR-MEM-004, FR-MEM-005, FR-MEM-006, FR-MEM-007, FR-MEM-008, FR-MEM-009, FR-MEM-010, FR-MEM-011]
---

# 회원 화면

<!-- 이 도메인의 화면 목록 정본이다. 색인·메뉴는 README.md -->

## 화면 목록

| ID | 이름 | 경로 | 접근 역할 | refs |
|---|---|---|---|---|
| SCR-MEM-001 | 로그인 | /login | 비회원, 관리자 | FR-MEM-002, SEC-AUTH-001, INT-GOOGLE-001, INT-KAKAO-001 |
| SCR-MEM-002 | 가입 수단 선택 | /signup | 비회원 | FR-MEM-001, SEC-AUTH-001 |
| SCR-MEM-003 | 가입: 이메일·비밀번호 | /signup/email | 비회원 | FR-MEM-001, SEC-AUTH-002, INT-KAKAO-003 |
| SCR-MEM-004 | 가입: 인증 메일 안내 | /signup/email/sent | 비회원 | FR-MEM-001, SEC-AUTH-003, INT-MAIL-001, INT-MAIL-002 |
| SCR-MEM-005 | 가입: 생년월일·휴대폰 인증 | /signup/phone | 비회원 | FR-MEM-001, SEC-AUTH-003, INT-SMS-001, INT-SMS-002, DAT-RET-002 |
| SCR-MEM-006 | 가입: 닉네임·동의 | /signup/profile | 비회원 | FR-MEM-001, SEC-PRIV-001 |
| SCR-MEM-007 | 비밀번호 찾기 | /password/forgot | 비회원 | FR-MEM-009, INT-MAIL-001, INT-MAIL-002 |
| SCR-MEM-008 | 비밀번호 재설정 | /password/reset | 비회원 | FR-MEM-009, SEC-AUTH-002, SEC-AUTH-003 |
| SCR-MEM-009 | 프로필 | /u/[nickname] | 비회원, 회원, 관리자 | FR-MEM-005, FR-REP-002, FR-REL-001, FR-REL-005, FR-MSG-001 |
| SCR-MEM-010 | 프로필 편집 | /settings/profile | 회원 | FR-MEM-004 |
| SCR-MEM-011 | 검색 | /search | 비회원, 회원, 관리자 | FR-MEM-006, FR-FEED-007 |
| SCR-MEM-012 | 설정 | /settings | 회원, 관리자 | FR-MEM-007, FR-MEM-003, NFR-I18N-001 |
| SCR-MEM-013 | 비밀번호 변경 | /settings/password | 회원, 관리자 | FR-MEM-010, SEC-AUTH-002, SEC-AUTH-004 |
| SCR-MEM-014 | 로그인 수단 관리 | /settings/login-methods | 회원 | FR-MEM-011 |
| SCR-MEM-015 | 본인 확인 (모달) | (모달) | 회원 | FR-MEM-011, FR-MEM-008 |
| SCR-MEM-016 | 회원 탈퇴 | /settings/withdraw | 회원 | FR-MEM-008, DAT-RET-001 |

- ID는 라우트가 아니라 사용자가 보는 화면 단위다. 모달과 단계형 폼의 각 단계도 화면이다
- 접근 역할은 `docs/req/actors.md`와 같아야 한다
- 소셜 인증 콜백은 화면이 아니다. 서버가 처리한 뒤 홈, 가입 다음 단계(SCR-MEM-003·005), 로그인(실패 안내)으로 보낸다
- 이메일 인증 링크는 SCR-MEM-005로, 비밀번호 재설정 링크는 SCR-MEM-008로 연다
- 로그아웃(FR-MEM-003)은 설정 화면과 셸 사용자 메뉴의 버튼이다. 끝나면 게임별 피드(SCR-FEED-002)로 간다
- 관리자는 설정에서 언어, 비밀번호 변경, 로그아웃만 본다 〔2026-10-02〕 관리자 언어 전환 사용자 결정 (NFR-I18N-001)
- 비밀번호 변경(SCR-MEM-013)의 회원은 비밀번호가 있는 회원만 쓴다 (FR-MEM-010 △). 없는 회원에게는 로그인 수단 관리로 안내한다
- 관리자가 로그인하면 신고 목록(SCR-ADM-001)으로 간다 (FR-MEM-002)
