---
name: psw-implement
description: harness-psw 구현 흐름을 진행한다. 승인된 FR을 하나씩 dev에서 만든 기능 브랜치(feat/<FR-ID>-<요약>)에서 구현자(계약) → 검증자(AC 테스트 선작성) → 구현자 → 도구 검사 → 검토자 → 검증자(판정) → 커밋 검사 → 사용자 확인 → dev 병합 순서로 하위 에이전트를 조율한다. 설계가 승인된 도메인의 구현을 시작할 때 사용한다.
---

# psw-implement

메인 세션이 오케스트레이터로 FR 단위 구현을 조율한다. 오케스트레이터는 직접 구현하지 않는다.

## 전제 (구현 착수 조건)

- 대상 FR 파일과 관련 설계 문서가 `approved`다
- 프로젝트별 결정이 채워져 있다 (`psw-design` 7단계)
  - `docs/design/conventions.md` 도구 절과 AC 테스트 절, `CLAUDE.md` 명령 표
  - `.claude/psw.conf`의 테스트 경로 패턴
  - 커밋 검사 hook 설정
- `dev` 브랜치가 있다. 없으면 사용자에게 `main`에서 만들지 묻는다
- 하위 에이전트 `implementer`, `reviewer`, `verifier`가 `.claude/agents/`에 있다
- `.claude/settings.json`에 `guard-paths.sh` hook이 있다 (`psw-init`)

하나라도 없으면 멈추고 사용자에게 알린다.

## 규칙

- FR은 한 번에 하나씩 진행한다
- FR 1개 = 기능 브랜치 1개: `dev`에서 `git switch -c feat/<FR-ID>-<요약> dev`
  - git worktree는 규칙이 아니다. 사용자가 원하면 써도 된다
- 테이블·API 문서는 없다. 계약(API 경로, 요청·응답 형식, 오류 코드, DB 마이그레이션)은 구현자가 코드로 먼저 쓰고, 검증자는 그 코드로 테스트를 쓴다 (harness-psw 4.2, 9.2)
- 검토자·검증자에게 구현자의 설명을 넘기지 않는다. 산출물과 원천 문서만 넘긴다
- 지적하는 쪽은 고치지 않는다. 수정은 항상 구현자가 한다
- 같은 AC가 3회 FAIL하면 멈추고 사용자에게 보고한다 (설계·요구사항 문제로 본다)
- 구현자·검증자가 "상위 결함"을 보고하면 흐름을 멈추고 `psw-loop`로 해당 루프를 호출한다
  - 설계 결함(코드 규칙, 테마·UI 컴포넌트 변경 포함) → 설계 루프 (`psw-design`), 요구사항 결함 → 기획 루프
- MUST: `dev`로 합치기 전에 FR마다 사용자 확인을 받는다
- 병합은 개별 커밋을 유지한다. squash하지 않는다
- NEVER: `main`에 커밋하거나 합치지 않는다. 배포는 사용자가 한다

## 준비 (프로젝트에서 처음 한 번)

1. 커밋 검사 도구가 harness 스크립트로 정해졌으면
   - `psw-design`의 `templates/githooks/commit-msg`를 `.githooks/commit-msg`로 복사한다
   - `git config core.hooksPath .githooks`
   - 다른 도구(commitlint 등)로 정했으면 그 도구 설정에서 `check-commit-msg.sh`와 같은 규칙을 적용한다
2. GitHub를 쓰면 `psw-design`의 `templates/github-workflow-psw.yml`을 `.github/workflows/psw.yml`로 복사할지 사용자에게 묻는다

## FR 흐름

### 1. 대상 선택

- `approved` FR 중 의존 순서가 앞선 것을 고른다 (`.claude/scripts/psw/rtm.sh`로 현황 확인)
- `dev`에서 기능 브랜치를 만든다

### 2. 계약 (구현자)

- 서버 쪽 변경이 없는 FR(화면만 바뀌는 기능)이면 건너뛴다
- `implementer`를 "계약" 단계로 호출한다
- 넘길 것: 기능 브랜치, FR 경로, 관련 `_policy.md` 경로
- 구현자는 `docs/design/conventions.md` REST·DB 절을 따라 컨트롤러 시그니처, 요청·응답 형식, 새 오류 코드, DB 마이그레이션을 쓰고 동작은 비워 둔다

### 3. AC 테스트 작성 (검증자)

- `verifier`를 "테스트 작성" 단계로 호출한다
- 넘길 것: 기능 브랜치, FR 경로, 관련 `_policy.md` 경로, 관련 화면 경로 (`find-refs.sh <FR-ID>`로 찾는다), 계약 커밋
- 검증자는 AC 테스트 작성법을 `docs/design/conventions.md` AC 테스트 절에서, 테스트 위치를 `architecture.md` 백엔드 절에서 읽는다

### 4. 구현 (구현자)

- `implementer`를 "구현" 단계로 호출한다
- 넘길 것: 기능 브랜치, FR 경로, 관련 `_policy.md`·화면 경로, 이전 반복의 지적·실패 내용
- "중단" 보고면 사유에 따라 규칙대로 처리한다

### 5. 도구 검사

- lint·타입 검사를 실행한다 (`CLAUDE.md` 명령)
- 실패하면 4단계로 돌아간다

### 6. 코드 검토 (검토자)

- diff를 뽑는다: `git diff dev...feat/<FR-ID>-<요약>`
- `reviewer`를 "코드 검토"로 호출한다. 넘길 것: diff 전문, FR 경로, 관련 `_policy.md`·화면 경로
- CHANGES면 지적을 구현자에게 넘겨 4단계로 돌아간다

### 7. 검증 (검증자)

- `verifier`를 "검증" 단계로 호출한다
- FAIL이면 실패 AC를 구현자에게 넘겨 4단계로 돌아간다. AC별 FAIL 횟수를 센다

### 8. 커밋 검사

- 브랜치의 각 커밋: `.claude/scripts/psw/check-commit-msg.sh --commit <커밋>` (`git rev-list dev..feat/<FR-ID>-<요약>`)
- 실패하면 원인을 고친다

### 9. 사용자 확인

아래 형식으로 요청한다. AC 결과는 검증자 보고를 그대로 옮긴다.

```text
[병합 확인] <FR-ID> <제목>
- 변경 파일: N개 (코드 n, 테스트 n)
- AC 결과: AC-1 PASS(테스트) / AC-2 PASS(직접 확인) / AC-3 FAIL [SHOULD] 사유: ...
- 검토 지적: N건 → 처리 결과
- 남은 미결: <없음 또는 목록>
→ dev에 합칠까요?
```

### 10. 병합과 정리

1. `dev`로 합친다: `git switch dev` 후 `git merge feat/<FR-ID>-<요약>`
2. 기능 브랜치를 지운다: `git branch -d feat/<FR-ID>-<요약>`
3. 병합으로 바뀐 코드가 다른 FR에 영향을 주는지 `find-refs.sh`로 찾고, 영향받은 FR의 직접 확인 AC를 다시 확인한다
   → AC 테스트 코드가 있는 AC는 테스트 실행·CI가 다시 확인한다

## 보고

- FR마다: 결과, 반복 횟수, 남은 SHOULD 미충족
- 도메인이 끝나면: `rtm.sh <도메인>` 결과
