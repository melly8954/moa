---
name: psw-implement
description: harness-psw 구현 흐름을 진행한다. 승인된 FR을 하나씩 검토자(착수 점검) 뒤 dev에서 만든 기능 브랜치(feat/<FR-ID>-<요약>)에서 구현자(계약) → 검증자(AC 테스트 선작성) → 구현자 → 도구 검사 → 검토자 → 검증자(판정) → 커밋 검사 → 사용자 확인 → dev 병합 순서로 하위 에이전트를 조율한다. 설계가 승인된 도메인의 구현을 시작할 때 사용한다.
---

# psw-implement

메인 세션이 오케스트레이터로 FR 단위 구현을 조율한다. 오케스트레이터는 직접 구현하지 않는다.

## 전제 (구현 착수 조건)

- 대상 FR 파일과 관련 설계 문서가 `approved`다
- 용어집이 비어 있지 않다 (`.claude/scripts/psw/loop-status.sh`에 용어집 오류가 없다)
- 프로젝트별 결정이 채워져 있다 (`psw-design` 1·3·7단계)
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
- 같은 검토 지적(같은 근거·위치)이 2회 다시 나오거나 코드 검토가 3회차에도 CHANGES면 멈추고 사용자에게 보고한다
- 이어 쓰기: 한 FR 안에서는 같은 `implementer`·`verifier`·`reviewer`를 SendMessage로 이어 쓴다. 후속 지시(검토 지적, 검증 실패)는 묶어서 한 번에 보낸다. FR이 바뀌면 새로 부른다
  - 이어 쓸 수 없는 환경이면 새로 부르고, 지난 회차의 지적·실패를 함께 넘긴다
- 병렬: 3단계(테스트 작성 ∥ 계약 검토), 6단계(코드 검토 ∥ 테스트 실행)는 두 에이전트를 함께 돌린다. 둘 다 그 단계에서 같은 파일을 고치지 않는다
- 구현자·검증자가 "상위 결함"을 보고하면 흐름을 멈추고 `psw-loop`로 해당 루프를 호출한다
  - 설계 결함(코드 규칙, 테마·UI 컴포넌트 변경 포함) → 설계 루프 (`psw-design`), 요구사항 결함 → 기획 루프
  - 문서 수정과 승인은 문서 커밋 위치(직접 방식 `dev`, PR 방식 `docs/<주제>`)에서 한다. 브랜치를 옮기고 돌아오는 절차는 `psw-loop` G를 따른다
- 원격(`origin`)이 있으면 (harness-psw 5.1)
  - 기능 브랜치는 첫 커밋 뒤 `git push -u origin feat/<FR-ID>-<요약>`로 올리고, 하위 에이전트가 커밋을 보고할 때마다 오케스트레이터가 push한다. 하위 에이전트는 push하지 않는다
  - 병합 방식(`CLAUDE.md` 브랜치 줄, harness-psw 5.1)에 따라 9단계에서 묻는다. 직접 방식은 병합과 `dev` push를, PR 방식은 PR 열기를 묻는다
  - 기능 브랜치는 `psw-loop` G의 rebase 뒤에만 `git push --force-with-lease`로 덮어쓴다. `main`·`dev`에는 force push하지 않는다
- 검사 범위 (harness-psw 9.2)
  - 반복 중에는 바꾼 부분과 관련된 검사·테스트만 돌린다 (이 FR의 AC 테스트, 바뀐 모듈의 lint). `CLAUDE.md` 명령 표의 "하나만 실행" 명령을 쓴다
  - 전체 검사(빌드, 전체 테스트)는 구현자의 완료 보고 직전, 검증자의 판정 직전에 한 번씩 돌린다. 5단계 도구 검사는 한 번 돌린다
  - 하위 에이전트를 호출할 때 이 범위를 함께 알린다
- MUST: `dev`로 합치기(또는 PR을 열기) 전에 FR마다 사용자 확인을 받는다
- NEVER: PR 방식에서 PR을 병합하지 않는다. 병합은 사용자(팀)가 GitHub에서 한다
- 병합은 개별 커밋을 유지한다. squash하지 않는다
- NEVER: `main`에 커밋하거나 합치지 않는다. 배포는 사용자가 한다

## 준비 (프로젝트에서 처음 한 번)

1. 커밋 검사 도구가 harness 스크립트로 정해졌으면
   - `templates/githooks/commit-msg`를 `.githooks/commit-msg`로 복사한다
   - `git config core.hooksPath .githooks`
   - 다른 도구(commitlint 등)로 정했으면 그 도구 설정에서 `check-commit-msg.sh`와 같은 규칙을 적용한다
2. 시크릿 스캔 도구가 gitleaks로 정해졌으면
   - `gitleaks version`으로 설치를 확인한다. 없으면 멈추고 사용자에게 설치를 요청한다. 설치를 대신하지 않는다
   - `templates/githooks/pre-commit`을 `.githooks/pre-commit`으로 복사한다 (위의 `core.hooksPath`를 함께 쓴다)
   - 확인: 가짜 키를 넣은 파일을 스테이징하고 커밋이 막히는지 본 뒤, 그 파일은 스테이징을 풀고 지운다
   - 다른 도구로 정했으면 그 도구의 pre-commit 연결 방법을 따른다
3. GitHub를 쓰면 `templates/github-workflow-psw.yml`을 `.github/workflows/psw.yml`로 복사할지 사용자에게 묻는다

## FR 흐름

### 1. 대상 선택

- `approved` FR 중 의존 순서가 앞선 것을 고른다 (`.claude/scripts/psw/rtm.sh`로 현황 확인)

### 1-B. 착수 점검 (검토자)

기능 브랜치를 만들기 전에 한다. 문서를 고쳐도 기능 브랜치를 옮길 일이 없다. 답은 문서 커밋 위치에서 반영한다.

- `reviewer`를 "착수 점검"으로 호출한다. 넘길 것: FR 경로, 관련 `_policy.md`·화면 경로(`find-refs.sh <FR-ID>`), FR `refs`의 영역 문서
- 질문이 있으면 모아서 한 번에 사용자에게 묻는다. 선택지와 추천안을 함께 보인다
  - 답은 `psw-loop`로 원천 문서에 반영하고 승인받는다
  - 정하지 않고 진행하기로 하면 미결 보류로 등록한다 (`psw-loop` C)
- 크기 신호가 있으면 분리 조건(harness-psw 3.4)에 맞는지 보이고, 나눌지 사용자에게 묻는다. 나누면 기획 루프(`psw-req` 갱신)로 넘기고 이 FR은 멈춘다
- 점검이 끝나면(질문이 없거나 답을 반영해 승인받아 `dev`에 들어가면) 최신 `dev`에서 기능 브랜치를 만든다. 원격이 있으면 첫 커밋(계약 또는 테스트) 뒤 원격에 올린다

### 2. 계약 (구현자)

- 서버 쪽 변경이 없는 FR(화면만 바뀌는 기능)이면 건너뛴다
- `implementer`를 "계약" 단계로 호출한다
- 넘길 것: 기능 브랜치, FR 경로, 관련 `_policy.md` 경로
- 구현자는 `docs/design/conventions.md` REST·DB 절을 따라 컨트롤러 시그니처, 요청·응답 형식, 새 오류 코드, DB 마이그레이션을 쓰고 동작은 비워 둔다

### 3. AC 테스트 작성 (검증자) ∥ 계약 검토 (검토자)

- `verifier`를 "테스트 작성" 단계로 호출한다
- 서버 쪽 계약이 있으면 같은 때 `reviewer`를 "계약 검토"로 호출한다. 넘길 것: 계약 커밋의 diff, FR 경로, 관련 `_policy.md` 경로
  - 지적이 있으면 4단계에서 구현자에게 함께 넘긴다. 계약이 바뀌면 검증자에게 알려 테스트를 맞춘다
- 넘길 것: 기능 브랜치, FR 경로, 관련 `_policy.md` 경로, 관련 화면 경로 (`find-refs.sh <FR-ID>`로 찾는다), 계약 커밋
- 검증자는 AC 테스트 작성법을 `docs/design/conventions.md` AC 테스트 절에서, 테스트 위치를 `architecture.md` 백엔드 절에서 읽는다

### 4. 구현 (구현자)

- `implementer`를 "구현" 단계로 호출한다 (계약을 쓴 구현자를 이어 쓴다)
- 넘길 것: 기능 브랜치, FR 경로, 관련 `_policy.md`·화면 경로, 이전 반복의 지적·실패 내용(묶어서 한 번에)
- "중단" 보고면 사유에 따라 규칙대로 처리한다

### 5. 도구 검사

- lint·타입 검사를 실행한다 (`CLAUDE.md` 명령). 전체를 한 번 돌린다
- 실패하면 4단계로 돌아간다

### 6. 코드 검토 (검토자) ∥ 테스트 실행 (검증자)

- diff를 뽑는다: `git diff dev...feat/<FR-ID>-<요약>`
- `reviewer`를 "코드 검토"로 호출한다. 넘길 것: diff 전문, FR 경로, 관련 `_policy.md`·화면 경로
- 같은 때 `verifier`에게 이 FR의 AC 테스트만 실행해 결과를 알려 달라고 한다. 판정이 아니다
- CHANGES이거나 실패한 테스트가 있으면 지적과 실패를 묶어 구현자에게 넘겨 4단계로 돌아간다
- 다시 검토할 때는 diff 전문과 지난 회차 지적 표를 함께 넘긴다. 지적 번호(N1, N2, …)는 회차를 넘어 이어 쓴다

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
→ dev에 합치고 origin에 올릴까요?
```

- 마지막 줄은 병합 방식에 따라 다르다
  - 직접 방식: `→ dev에 합치고 origin에 올릴까요?` (원격이 없으면 `→ dev에 합칠까요?`)
  - PR 방식: `→ dev로 PR을 열까요?`

### 10. 병합과 정리

직접 방식

1. `dev`로 합친다: `git switch dev` 후 `git merge feat/<FR-ID>-<요약>`
2. 원격이 있으면 `git push origin dev`. 그 사이 쌓인 문서 커밋도 함께 올라가고, CI가 돈다
3. 기능 브랜치를 지운다
   - 원격: `git push origin --delete feat/<FR-ID>-<요약>` (`dev` push가 끝난 뒤)
   - 로컬: `git branch -d feat/<FR-ID>-<요약>`

PR 방식

1. PR을 연다: `gh pr create --base dev --head feat/<FR-ID>-<요약>`. 본문에 9단계 요약을 넣는다
   - `gh`가 없거나 인증이 안 돼 있으면 GitHub에서 PR을 여는 링크를 안내한다
2. 사용자(팀)가 리뷰하고 CI가 통과하면 GitHub에서 병합한다 (squash 금지, harness-psw 5.1)
3. 병합 뒤 로컬 `dev`를 받아 오고(`git switch dev`, `git pull --ff-only`), 로컬 기능 브랜치를 지운다. 원격 기능 브랜치는 GitHub 자동 삭제가 없으면 지운다

공통

- 병합으로 바뀐 코드가 다른 FR에 영향을 주는지 `find-refs.sh`로 찾고, 영향받은 FR의 직접 확인 AC를 다시 확인한다
  → AC 테스트 코드가 있는 AC는 테스트 실행·CI가 다시 확인한다

## 보고

- FR마다: 결과, 반복 횟수, 남은 SHOULD 미충족
- 도메인이 끝나면: `rtm.sh <도메인>` 결과
