---
status: draft
refs: []
---

# UI 규칙

- 테마와 컴포넌트의 값은 프론트 코드가 정본이다. 여기에는 값을 적지 않는다
- 테마·컴포넌트 경로와 키트 버전: `docs/design/architecture.md` UI 절
- 커스터마이즈는 테마 변수로만 한다. 컴포넌트 코드의 스타일을 직접 바꾸지 않는다
- 여기에는 화면을 그릴 때의 규칙만 둔다. 프론트 코드 규칙은 `docs/design/conventions.md` 프론트 절이 소유한다

## 테마 사용 규칙

- <예: 색은 테마 변수(primary, destructive 등)의 Tailwind 클래스만 쓰고 원색 값을 직접 쓰지 않는다>

## 레이아웃 규칙

- <예: 페이지 본문 폭은 max-w-5xl, 좌우 여백은 px-4. 모바일 기준점은 md>

## 상태 표시 규칙

- 빈 상태: <점선 테두리 안내 + 다음 행동 Button>
- 로딩: <Skeleton, 300ms 이상일 때만 표시>
- 오류: <Alert destructive, 다시 시도 Button 포함>

## 문구 규칙

- <예: 버튼은 동사로 끝낸다. 용어는 glossary.md를 따른다>

## 예외

| 화면 | 예외 | 이유 |
|---|---|---|
