---
name: psw-init
description: harness-psw 프로젝트 골격(CLAUDE.md, docs/, open-question.md, glossary.md, .env.example, .gitignore, .gitattributes)과 승인 보호 hook을 만든다. 새 프로젝트에 harness-psw를 처음 적용할 때 사용한다.
---

# psw-init

프로젝트에 harness-psw 문서 골격을 만든다. 문서 내용은 쓰지 않는다.

## 전제

- `install.sh`로 이 스킬이 설치된 프로젝트 루트에서 실행한다
- 템플릿은 이 스킬의 `templates/`에 있다

## 절차

### 1. 상태 확인

- git 저장소가 아니면 사용자에게 `git init` 여부를 묻는다
- 아래 경로 중 이미 있는 것을 확인한다
  - `CLAUDE.md`, `docs/`, `.env.example`, `.gitignore`, `.gitattributes`, `.claude/settings.json`
- MUST: 이미 있는 파일은 덮어쓰지 않는다. 목록을 보고하고 진행 여부를 묻는다
  - 예외: `.gitignore`, `.gitattributes`는 블록만 추가하고, `.claude/settings.json`은 hook 항목만 추가한다

### 2. 사용자에게 묻기

- 프로젝트명
- 한 줄 요약
  - 아직 정하지 못했으면 `(인터뷰 후 작성)`으로 두고, `psw-interview`가 `docs/req/README.md` 개요를 쓴 뒤 갱신한다

### 3. 디렉터리 생성

아래 디렉터리를 만들고, 빈 디렉터리에는 `.gitkeep`을 둔다.

```text
docs/req/
docs/design/
```

- `docs/req/`, `docs/design/` 아래 하위 폴더와 파일은 `psw-interview`, `psw-req`, `psw-design`이 필요할 때 만든다

### 4. 템플릿 복사

| 템플릿 | 대상 | 처리 |
|---|---|---|
| `templates/CLAUDE.md.tmpl` | `CLAUDE.md` | `{{PROJECT_NAME}}`, `{{SUMMARY}}`를 2단계 답으로 바꾼다 |
| `templates/glossary.md` | `docs/glossary.md` | 그대로 복사 |
| `templates/open-question.md` | `docs/open-question.md` | 그대로 복사 |
| `templates/env.example` | `.env.example` | 그대로 복사 |

### 5. `.gitignore`, `.gitattributes`

- 없으면 `templates/gitignore`를 `.gitignore`로, `templates/gitattributes`를 `.gitattributes`로 복사한다
- 있으면 `# harness-psw` 블록이 없을 때만 템플릿 내용을 끝에 추가한다

### 6. 승인 보호 hook (`.claude/settings.json`)

- 설정 파일을 바꾸는 작업이므로 사용자에게 내용을 보여주고 동의를 받는다
  - 하는 일: 모든 에이전트(메인 세션 포함)가 문서를 `approved`로 바꾸거나 `approve.sh`를 실행하는 것을 막고, 하위 에이전트의 편집 경로를 역할별로 제한한다
  - 승인은 사용자가 입력창에서 직접 실행한다: `! bash .claude/scripts/psw/approve.sh <경로>`
- `.claude/settings.json`이 없으면 `templates/settings.json`을 복사한다
- 있으면 `hooks.PreToolUse`에 템플릿의 항목을 추가한다. 같은 command가 이미 있으면 추가하지 않는다
- hook은 워크스페이스 신뢰를 수락한 뒤에 동작한다

### 7. 확인

- 3~6단계의 결과 경로가 모두 있는지 확인한다
- `CLAUDE.md`에 `{{`가 남아 있지 않은지 확인한다
- hook 동작 확인: `printf '{"tool_name":"Bash","tool_input":{"command":"approve.sh"}}' | bash .claude/scripts/psw/guard-paths.sh hook` 가 exit 2로 끝나야 한다

### 8. 보고

- 생성한 파일과 디렉터리 목록
- 건너뛴 파일과 이유
- 다음 단계: `psw-interview`로 기획 인터뷰를 시작한다
- 커밋은 사용자 승인 후에 한다. 메시지 예: `chore: harness-psw 골격 생성`

## 하지 않는 것

- 커밋 검사 hook·CI 설정
  → 커밋 검사 도구가 프로젝트별 결정 항목(harness-psw 1.5)이므로 구현 착수 전에 설정한다 (`psw-implement`)
- REQ, 설계 문서 작성
- 기존 파일 덮어쓰기
