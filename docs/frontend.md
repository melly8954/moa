# 프론트 구조

`frontend/`의 구조와 코드 규칙이다. 도구로 강제하는 규칙(ESLint, Prettier, TypeScript)은 설정 파일이 정본이다.

## 기술 스택

| 영역 | 선택 |
|---|---|
| 프레임워크 | Next 16 (App Router), TypeScript |
| UI | Tailwind 4, shadcn/ui (base-nova, Base UI), 테마 rose |
| 서버 상태 | TanStack Query |
| 폼 | react-hook-form + zod |
| 다국어 | next-intl (`ko`, `en`, 기본 `ko`) |
| 실시간 | `@stomp/stompjs` |
| 테스트 | Playwright (`e2e/`) |
| lint·포맷 | ESLint(eslint-config-next), Prettier + prettier-plugin-tailwindcss |
| 호스팅 | Vercel (`www.<도메인>`) |

> 이 Next 버전은 학습 데이터와 API가 다를 수 있다. 코드를 쓰기 전에 `node_modules/next/dist/docs/`의 해당 안내를 읽는다 (`frontend/AGENTS.md`).

## 디렉터리

```text
frontend/
  app/                    라우트
  components/ui/          shadcn 컴포넌트 (공유 파일, 직접 고치지 않는다)
  components/<도메인>/     앱 컴포넌트 (member, feed, party ...)
  hooks/                  공용 훅
  lib/api/                API 호출과 응답 타입
  lib/realtime/           STOMP 연결
  lib/validation/         정책 수치와 zod 스키마
  i18n/                   next-intl 설정
  messages/{ko,en}.json   화면 문구
  e2e/<도메인>/            Playwright 테스트
```

- 호출 방향: 화면(`app/`) → 앱 컴포넌트(`components/`) → API 모듈(`lib/api/`)
- 공유 파일: `app/theme.css`, `components/ui/`, `hooks/use-mobile.ts`. 포맷도 하지 않는다 (`.prettierignore`)

## 컴포넌트

- 서버 컴포넌트가 기본이다. 상태·이벤트가 필요한 컴포넌트만 `"use client"`
- 새 공통 컴포넌트는 `npx shadcn@latest add <이름>`으로 추가한다
- 컴포넌트 코드의 스타일을 직접 바꾸지 않는다. 테마 변수로 바꾼다
- 쓰는 기준
  - Select는 선택지가 4개 이상일 때, 3개 이하면 RadioGroup
  - 잠깐 알리는 결과(저장, 복사, 신고 접수)는 Sonner, 사용자가 읽어야 하는 실패는 Alert
  - Dialog는 확인과 짧은 입력만. 긴 입력은 별도 화면
  - Table은 관리 화면에서만. 회원 화면 목록은 카드나 줄 목록
  - 같은 화면 안 보기 전환은 Tabs, 다른 화면 이동은 링크

## API 호출

- API 호출은 `lib/api/` 한 곳에서만 한다. 화면은 TanStack Query 훅(`use<리소스>`)으로 부른다
- 응답 타입은 백엔드 OpenAPI 문서에서 `openapi-typescript`로 만든다 (`lib/api/schema.ts`, 손으로 고치지 않는다)
- 요청은 `credentials: "include"`로 보낸다 (리프레시 쿠키)
- 액세스 토큰은 메모리에만 둔다 (localStorage·sessionStorage·쿠키 금지). A002를 받으면 재발급을 한 번만 요청하고(동시 요청을 하나로 모은다), 실패하면 로그인으로 보낸다
- 서버 오류는 `code`로 문구를 고른다 (`errors.<코드>`). 서버 `message`를 그대로 보여 주지 않는다

## 상태 관리

- 서버 상태는 TanStack Query, 화면 상태는 컴포넌트 안. 전역 상태 라이브러리는 두지 않는다
- 실시간으로 받은 메시지·알림은 해당 쿼리 캐시를 고쳐 반영한다

## 라우팅·다국어·환경변수

- 주소에 언어(`/ko`, `/en`)를 붙이지 않고 쿠키(`NEXT_LOCALE`)로 고른다. 회원의 언어는 서버에도 저장한다
- 화면 문구는 `messages/ko.json`, `messages/en.json`에만 둔다. 컴포넌트에 한국어 문자열을 직접 쓰지 않는다
- 시각은 서버가 UTC로 주고 화면은 `Asia/Seoul`로 보여 준다. 24시간 안은 상대 시각("3분 전"), 그 밖은 날짜
- 브라우저에 노출할 환경변수만 `NEXT_PUBLIC_` 접두사 (`NEXT_PUBLIC_API_URL`, `NEXT_PUBLIC_WS_URL`)

## 셸

- 데스크톱: 사이드바형 (왼쪽, 접으면 아이콘만, 벽에 붙음, 기본 펼침)
- 모바일(`md` 미만): 하단 탭 바와 헤더 아이콘
- 로그인·가입 화면은 셸 없이 가운데 카드

## 화면 규칙

### 테마

- 색은 테마 변수의 Tailwind 클래스(`bg-primary`, `text-muted-foreground`, `text-destructive` 등)만 쓴다. 원색 값(`#fff`, `red-500`)을 쓰지 않는다
- 강조색(primary)은 화면에서 가장 중요한 행동 하나에만. 나머지는 outline·ghost·secondary
- 라이트·다크 모두 테마 변수로 그린다. 다크 전용 색을 따로 쓰지 않는다
- 게임은 색이 아니라 이름(Badge outline)으로 구분한다

### 레이아웃

- 모바일 우선. 기준점 `md`(768px). 360px 폭에서 가로 스크롤이 없어야 한다
- 본문 폭: 회원 화면 `max-w-2xl`, 관리 화면·대화(목록과 방 나란히) `max-w-5xl`, 셸 없는 화면 `max-w-sm`. 좌우 여백 `px-4`
- 본문 폭은 셸 본문 칸 안의 페이지 상자에 건다
- 모바일에서 화면 아래 고정 요소(예: 대화 입력칸)는 탭 바 위에 둔다
- 회원을 보여 주는 줄은 Avatar + 닉네임 + 부가 정보(레벨, 티어) 순서

| 간격 쓰는 곳 | 값 |
|---|---|
| 페이지 구역 사이 | `gap-6` |
| 카드·대화상자 안 구역, 폼 입력칸 사이 | `gap-4` |
| 읽기 카드(게시물, 모집, 평가) 안 요소 사이 | `gap-3` |
| 이름표와 입력, 버튼·배지 묶음 | `gap-2` |
| 텍스트 줄 묶음 | `gap-1` |
| 목록 한 줄 | `py-3`, 아바타와 글 사이 `gap-3`, `divide-y` |
| 사진 격자 | `gap-1` |

- 컴포넌트 안쪽 여백은 컴포넌트가 정한다. 바깥에서 덮어쓰지 않는다

### 상태 표시

- 빈 상태: 점선 테두리 안내 + 다음 행동 Button
- 로딩: 실제 배치 모양의 Skeleton. 300ms 안에 끝나면 보이지 않게 한다
- 오류: Alert destructive + 다시 시도 Button. 입력 오류는 입력칸 아래 `text-destructive`
- 무한 스크롤: 아래에 이어 불러오는 Skeleton, 끝이면 "모두 봤습니다"
- 찾을 수 없는 대상(R001)은 오류가 아니라 빈 상태 모양으로 "찾을 수 없습니다"

### 문구와 개인정보

- 존댓말 "~합니다/~하세요". 버튼은 동사로 끝낸다
- 개인정보(이메일, 생년월일, 휴대폰 번호)는 본인 설정·관리자 화면에만 그린다. 관리자 화면의 휴대폰 번호는 가운데 4자리를 가린다
- 관리자가 원문 보기로 연 사용자 화면에는 회원 행동 버튼(팔로우, 메시지, 좋아요, 댓글, 신고, 참여)을 그리지 않는다
- 비회원에게는 회원 행동 버튼을 그리고, 누르면 로그인 안내로 보낸다

## 테스트

- 위치: `e2e/<도메인>/<스펙 이름>.spec.ts`. 설정: `playwright.config.ts`
- 세 폭(mobile 360, tablet 768, desktop 1280)으로 모두 돈다. 폭에 따라 다른 기준은 `test.skip`으로 고른다
- API 응답은 `page.route`로 대신해 화면 동작만 본다. API 동작은 백엔드 테스트가 본다
- 요소는 역할·이름(`getByRole`, `getByLabel`)으로 찾는다. CSS 클래스로 찾지 않는다
- 이름에 스펙의 수용 기준 번호를 단다 (예: `test("member/sign-up AC-2: ...", ...)`)

## 백엔드와 맞춰 바꾸는 것

`docs/backend.md`의 같은 절을 따른다. 경로, DTO(`schema.ts` 재생성), 에러 코드 문구, 정책 수치, STOMP 형식은 같은 커밋에서 함께 바꾼다.
