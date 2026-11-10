# Contract: 공통코드

새 API는 없다. 키트 API를 moa의 AC에 맞춰 어떻게 확인하는지만 적는다. 요청·응답 전체는 local 프로필의 `/v3/api-docs`가 기준이다.

## 코드 관리 목록

`GET /api/v1/admin/code-groups`

| AC | 호출자 | 기대 |
|---|---|---|
| US1-AC1 | ADMIN | 200. 그룹 코드 목록이 spec.md FR-001 표와 같다. 지금은 빈 목록 |
