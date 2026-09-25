---
name: psw-req
description: harness-psw 요구사항(REQ)의 상세를 채운다. 인터뷰가 만든 REQ 초안에 기능별 수용 기준, 도메인 정책·상태 전이(_policy.md), 비기능·보안·연동·데이터 기준, 역할·권한 매트릭스, 전체 적용 목록을 채우고 누락 검사와 승인 요청을 한다. 인터뷰가 끝난 뒤, 또는 요구사항이 바뀌어 REQ를 갱신할 때 사용한다.
---

# psw-req

`docs/req/`를 완성한다. 요구가 존재한다는 사실(기능 목록, 영역 항목)은 `psw-interview`가 적고, 이 스킬은 상세(흐름, 예외, 수용 기준, 기준 수치, 권한)를 채운다.

## 입력과 출력

| 입력 | 출력 |
|---|---|
| `docs/req/` 초안 (`psw-interview`) | `docs/req/functional/<domain>/` (FR 파일 상세, `_policy.md`) |
| `docs/open-question.md` | 영역 파일 기준 (`non-functional/`, `security/`, `integration/`, `data/`) |
| 바뀐 ID 목록 (갱신 시) | `docs/req/actors.md` 권한 매트릭스 |
| | `docs/req/README.md` 전체 적용 목록 |

## 규칙

- 템플릿: `references/*.md`
- 기능 목록(도메인 `README.md`)과 영역 파일에 없는 요구를 만들지 않는다. 필요해 보이면 사용자에게 묻고, 답이 없으면 `psw-loop`로 미결을 등록한다
  → 범위는 인터뷰에서 사용자가 정한다
- 상세를 모르면 추측하지 않는다. 사용자에게 묻거나 `[OPEN-NNN]` 자리표시를 둔다 (`psw-loop` 절차 A)
- 파일 구성
  - 기능 목록의 한 줄 = FR 파일 1개. 파일 이름은 `<ID 번호>-<행위>.md`다. 번호는 FR ID의 세 자리 번호, 행위는 영어 kebab-case (예: `FR-ORD-010` → `010-guest-checkout.md`)
  - FR 파일 제목은 기능 목록의 한 줄 요구와 같게 쓴다
  - 우선순위는 FR 파일에 적지 않는다 (도메인 `README.md`가 소유)
  - 둘 이상의 기능이 같이 쓰는 규칙·수치는 `_policy.md`에만 적고, FR은 항목 이름으로 참조한다
  - 상태가 3개 이상이거나 시간·외부 이벤트로 상태가 바뀌는 엔터티는 `_policy.md`에 상태 전이 절을 쓴다 (harness-psw 3.4)
  - 비기능·보안·연동·데이터는 영역별 파일 1개에 표로 담는다. 파일 이름은 `req/README.md` 코드표를 따른다
- 수용 기준
  - 기능마다 1개 이상, 확인할 수 있는 한 문장으로 쓴다
  - 강도는 `[MUST]` 또는 `[SHOULD]`만 쓴다. MAY는 쓰지 않는다
  - 화면으로만 확인할 수 있는 기준과 API·데이터로 확인할 수 있는 기준을 섞지 않고 나눠 쓴다
    → 검증 방법이 달라진다 (harness-psw 5.2)
- 영역 항목의 기준은 측정할 수 있는 수치로 쓴다
- 적용 범위
  - `전체`: 영역 파일에만 둔다. `req/README.md`의 전체 적용 목록은 영역 파일에서 다시 만든다
  - `개별`: 해당 FR 파일의 `refs`에만 적는다
- 역할별 권한은 `actors.md`에만 적는다. FR의 행위자에는 주 흐름을 수행하는 주체만 적는다
- 사용자가 선택지 중에서 고른 값에는 결정 기록을 붙인다: `<값> 〔YYYY-MM-DD〕 <이유>` (harness-psw 8.1)
- `approved`로는 바꾸지 않는다. 승인은 사용자가 한다
- `approved` 문서의 내용을 바꿔야 하면 `psw-loop` 절차 D를 따른다

## 절차

### 1. 상세 채우기 (처음)

1. `req/README.md`, `actors.md`, 도메인 `README.md`를 읽는다
2. 영역 파일의 기준, 확인 방법, 적용 범위를 채운다 (`references/area.md`)
3. 도메인마다
   1. 공유 규칙이나 상태 전이가 있으면 `_policy.md`를 쓴다 (`references/policy.md`)
   2. FR 파일마다 선행조건, 예외 흐름, 수용 기준, 정책 참조, `refs`를 채운다 (`references/fr.md`)
   3. 모르는 것은 한 번에 3~5개씩 묶어 사용자에게 묻는다. 선택지와 추천안을 함께 제시한다
4. `actors.md`에 도메인별 권한 표를 쓴다 (`references/actors.md`)
5. `req/README.md`의 전체 적용 목록을 영역 파일에서 다시 만든다

### 2. 갱신 (요구사항이 바뀐 뒤)

1. `psw-loop`나 `psw-interview`가 넘긴 바뀐 ID만 다룬다
2. 추가된 ID: 1단계와 같이 만든다. 기능 목록에 행이 없으면 먼저 넣는다
3. 바뀐 ID: 해당 파일을 고친다. `approved`였으면 `draft`로 되돌린다
4. 삭제된 ID: 기능 목록의 행과 FR 파일(또는 영역 표의 행)을 지운다
   - 지우기 전에 `.claude/scripts/psw/find-refs.sh <ID>`로 설계·테스트 참조를 확인하고, 참조가 있으면 보고한다
5. `actors.md`, `req/README.md`의 전체 적용 목록을 다시 만든다

### 3. 점검

1. `.claude/scripts/psw/checklist-coverage.sh`를 실행한다
   - 누락이 있으면 `psw-interview`로 다시 묻는다 (피드백 루프 트리거: 누락)
2. `.claude/scripts/psw/loop-status.sh docs/req`를 실행한다
3. 스스로 점검한다
   - 기능 목록의 모든 행에 FR 파일이 있고, 제목이 같은가
   - FR마다 수용 기준이 있고, `[MUST]`가 하나 이상 있는가
   - `refs`의 ID가 영역 파일에 있는가
   - `_policy.md`의 값이 FR 본문에 다시 적혀 있지 않은가
   - `actors.md`에 모든 FR과 모든 역할이 있는가
   - 측정할 수 없는 비기능 표현(빠르게, 적절히 등)이 없는가. 있으면 미결로 등록한다

### 4. 승인 요청

1. 도메인 단위로 승인을 요청할 수 있다
   - 요약: 도메인별 FR 수와 우선순위 분포, 남은 미결(보류 포함), 정책 항목·상태 전이 수, 범위 밖 항목
2. 사용자가 승인한 파일만 `status: approved`가 된다
   - 승인은 사용자가 입력창에서 직접 실행한다: `! bash .claude/scripts/psw/approve.sh docs/req/functional/<domain>`
   - 영역 파일과 `actors.md`도 같은 방법으로 승인한다
   - 에이전트의 승인 시도는 hook이 막는다
   - 승인 커밋 트레일러: `Refs: <도메인 접두사>`
3. 다음 단계: `psw-design`

## 커밋

- 최초 작성: `Refs:`에 도메인·영역 접두사 (예: `Refs: FR-ORD, NFR-PERF`)
- 갱신: `Refs:`에 바뀐 ID, 미결을 해결했으면 `Closes:`
- 메시지 예: `docs: 주문 도메인 REQ 상세 작성`
