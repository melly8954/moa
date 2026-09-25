---
id: FR-ADM-005
status: approved
refs: [DAT-RET-001]
---

# FR-ADM-005 탈퇴 계정 복구

<!-- refs: 이 기능에만 적용되는 NFR·SEC·INT·DAT ID. 전체 적용 항목은 적지 않는다 -->

## 행위자

관리자

## 선행조건

- 관리자로 로그인한 상태다
- 회원이 전화나 개인 문의로 계정 복구를 요청했다

## 주 흐름

<!-- ck-fun-3 -->

1. 관리자가 WITHDRAWN 계정을 찾아 연다
2. 관리자가 요청자가 본인인지 확인하고 복구를 누른다
3. 시스템이 계정을 ACTIVE로 바꾸고 프로필과 작성자 표시를 되살린다

## 예외 흐름

- E1. 탈퇴 유예가 지나 PURGED된 계정이면 → 복구할 수 없다

## 수용 기준

- AC-1 [MUST] API: WITHDRAWN 계정을 복구하면 ACTIVE가 되고 글 작성자 표시가 원래 닉네임으로 돌아온다
- AC-2 [MUST] API: PURGED 계정의 복구 요청은 거부된다

## 정책 참조

- `_policy.md` 계정 복구
- `_policy.md` 상태 전이: 회원 계정 (회원 정책)
