---
id: FR-ADM-003
status: draft
refs: []
---

# FR-ADM-003 제재 해제

<!-- refs: 이 기능에만 적용되는 NFR·SEC·INT·DAT ID. 전체 적용 항목은 적지 않는다 -->

## 행위자

관리자

## 선행조건

- 관리자로 로그인한 상태다

## 주 흐름

<!-- ck-fun-3 -->

1. 관리자가 SUSPENDED 회원을 골라 해제한다
2. 시스템이 ACTIVE로 바꾸고 회원에게 알린다

## 예외 흐름

- E1. 정지 기간이 끝나면 → 관리자가 누르지 않아도 ACTIVE가 된다

## 수용 기준

- AC-1 [MUST] API: 해제하면 회원이 ACTIVE가 되고 로그인할 수 있다
- AC-2 [MUST] API: 끝나는 날이 지난 SUSPENDED 회원은 ACTIVE가 된다. 영구 정지는 바뀌지 않는다

## 정책 참조

- `_policy.md` 상태 전이: 회원 계정 (회원 정책)
