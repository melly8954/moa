# 스펙

기능 하나를 스펙 하나로 쓰고, 스펙 → 구현 → 테스트 순서로 진행한다.

## 위치와 이름

```text
docs/specs/<도메인>/<기능>.md      예: docs/specs/member/sign-up.md
```

- 도메인: member, feed, party, relation, message, notification, reputation, matching, admin ...
- 기능 이름은 영어 kebab-case 동사구로 쓴다

## 흐름

1. `_template.md`를 복사해 스펙을 쓴다 (상태 `draft`)
2. 모르는 것은 스펙의 "미결" 절에 적고, 정해지면 지운다
3. 미결이 없고 수용 기준이 확정되면 상태를 `ready`로 바꾼다
4. 구현한다. 수용 기준마다 테스트를 하나 이상 쓴다 (백엔드 `backend/src/test/`, 화면 `frontend/e2e/`)
5. 테스트가 통과하면 상태를 `done`으로 바꾼다

## 규칙

- 구조와 코드 규칙은 스펙에 다시 적지 않는다. `docs/backend.md`, `docs/frontend.md`를 따른다
- 정책 수치(길이, 개수, 기간)는 스펙에 적고, 코드에서는 `<도메인>Policy` 한 곳에 둔다
- 테스트 이름에 `<도메인>/<기능> AC-<번호>`를 단다
