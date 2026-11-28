# moa 디자인 시스템

`spec.md`(020-ui-foundation)의 구체 값이다. 색 코드, 글꼴, 크기, 배치, 컴포넌트 규칙은 이 문서가 정본이다.
화면을 만드는 사람과 AI 도구는 이 문서를 처음부터 끝까지 따른다. 이 문서에 없는 값이 필요하면 값을 지어내지 않고 이 문서에 먼저 더한다.

- 단어: MUST는 반드시, MUST NOT은 절대 하지 않음, SHOULD는 이유가 없으면 따름
- 코드 예시는 지금 프론트 구조(Next, Tailwind v4, 테마 변수 클래스, lucide, base-ui 기반 `components/ui`)에 맞춘 것이다
- 맨 아래 "AI 도구에게 주는 규칙"은 팀원이 자기 AI 도구에 그대로 붙여 넣는 요약이다

## 1. 성격

moa는 "게임 대기실의 전광판"이다. 어두운 흑연색 바탕 위에 선으로 나뉜 촘촘한 줄이 있고, 지금 중요한 것 하나만 형광 라임으로 켜진다. 숫자는 각진 표시 글꼴로 크게, 나머지는 단정한 한글 고딕으로 조용하게 둔다.

- 촘촘하다: 한 화면에 많이 보인다. 여백으로 고급스러움을 내지 않는다. 줄 높이 32~40px의 목록이 기본이다
- 선으로 나눈다: 덩어리는 1px 선과 바탕색 단계로 나눈다. 그림자와 둥근 카드로 띄우지 않는다
- 강조는 하나다: 시그널(라임)은 한 영역에 하나. 나머지는 흑백이다
- 숫자가 말한다: 팔로워, 인원, 티어, 조회수 같은 수는 표시 글꼴로 눈에 띄게 둔다
- 꾸밈이 없다: 그라디언트, 유리 효과, 일러스트, 이모지, 반짝이 아이콘이 없다

## 2. 글꼴

세 벌만 쓴다(MUST). 모두 SIL Open Font License라 상업 서비스에서 무료로 쓰고 함께 내려 줄 수 있다. 셋 다 Google Fonts에 있어 `next/font/google`로 빌드 때 받아 우리 서버가 내려 준다(브라우저가 Google에 따로 요청하지 않는다).

| 토큰 (클래스) | 글꼴 | 굵기 | 쓰는 곳 | 대체 글꼴 |
|---|---|---|---|---|
| `--font-sans` (`font-sans`), `--font-heading` (`font-heading`) | IBM Plex Sans KR | 400, 500, 700 | 본문, 제목, 단추, 입력, 메뉴. 한글이 들어가는 모든 곳 | `"Apple SD Gothic Neo", "Malgun Gothic", "Noto Sans KR", system-ui, sans-serif` |
| `--font-display` (`font-display`, 새 토큰) | Chakra Petch | 600, 700 | 워드마크 `moa`, 프로필 수 줄의 숫자, 티어·점수처럼 크게 보이는 수, 로그인 화면의 큰 영문 표어. 영문·숫자만 | `var(--font-sans)` |
| `--font-mono` (`font-mono`) | IBM Plex Mono | 400, 500 | 닉네임 태그 `#0421`, 초대 코드, 문의 번호, 코드 블록 | `ui-monospace, "SF Mono", Menlo, monospace` |

고른 이유:

- IBM Plex Sans KR: Pretendard·Noto Sans KR보다 획 끝이 각지고 공학적인 인상이라 "흔한 한국 서비스 글꼴" 느낌이 덜하다. 한글 자형을 따로 설계한 글꼴이라 15px 촘촘한 본문에서도 또렷하고, 같은 집안의 Plex Mono와 숫자·영문 모양이 맞는다
- Chakra Petch: 모서리를 깎은 사각 글자라 e스포츠 점수판·HUD 느낌을 준다. 흔한 "게임 글꼴"처럼 장식이 과하지 않아 숫자와 짧은 영문에만 쓰면 성격이 산다. 한글 글자가 없으므로 한글 문장에 쓰지 않는다(MUST NOT)
- IBM Plex Mono: 태그·코드처럼 한 글자씩 읽어야 하는 값을 본문과 구별한다

규칙:

- 화면 코드에 글꼴 이름을 적지 않는다(MUST NOT). `font-sans`, `font-heading`, `font-display`, `font-mono` 클래스만 쓴다
- 굵기는 표의 것만 쓴다. Plex Sans KR은 `font-normal`(400), `font-medium`(500), `font-bold`(700). `font-semibold`(600)는 쓰지 않는다(받지 않는 굵기라 브라우저가 흉내 낸다)
- 바뀌는 수(실시간 인원, 안 읽은 수)는 `font-sans tabular-nums`로 쓴다. Chakra Petch는 큰 고정 수(프로필 수 줄)에만 쓴다
- 한글 글꼴은 preload하지 않고 `display: "swap"`으로 기기 글꼴부터 보인다. 영문 표시 글꼴(Chakra Petch)만 preload한다

`app/layout.tsx`에서 지금의 Geist 두 벌을 바꾼다 (구현 메모. 정확한 옵션, 특히 한글 글꼴의 `subsets`·`preload`는 plan에서 `node_modules/next/dist/docs/`로 확인한다):

```tsx
import { Chakra_Petch, IBM_Plex_Mono, IBM_Plex_Sans_KR } from "next/font/google"

const sans = IBM_Plex_Sans_KR({ variable: "--font-sans", weight: ["400", "500", "700"], display: "swap", preload: false })
const display = Chakra_Petch({ variable: "--font-chakra", weight: ["600", "700"], subsets: ["latin"], display: "swap" })
const mono = IBM_Plex_Mono({ variable: "--font-plex-mono", weight: ["400", "500"], subsets: ["latin"], display: "swap" })
```

`globals.css` `@theme inline`에서는 `--font-display: var(--font-chakra), var(--font-sans)`, `--font-mono: var(--font-plex-mono)`로 잇는다(지금 `--font-mono: var(--font-geist-mono)`를 바꾼다). 대체 글꼴 목록은 `next/font`가 만드는 대체 글꼴 뒤에 위 표의 것을 붙인다.

## 3. 글자 크기

아래 단계만 쓴다(MUST). 기준은 `rem`이라 브라우저 글자 크기를 따라 커진다. 새 단계(`text-meta`, `text-body`, `text-title`, `text-display`)는 `globals.css`의 `@theme`에 더한다.

| 클래스 | 크기/줄 높이 | 굵기 | 쓰는 곳 |
|---|---|---|---|
| `text-xs` | 12/16 | 400·500 | 배지, 보조 설명, 아래 탭 줄 글자. 이보다 작게 쓰지 않는다 |
| `text-meta` | 13/18 | 400 | 시각, 수, 태그, 패널 묶음 이름 |
| `text-sm` | 14/20 | 400·500 | 단추, 입력, 메뉴, 표, 곁줄 목록 |
| `text-body` | 15/22 | 400 | 피드 글, 채팅, 자기소개. 기본 읽기 글자 |
| `text-base` | 16/26 | 400 | 긴 글(공지 본문, 약관) |
| `text-title-sm` | 17/24 | 700 | 본문 열 머리 제목, 곁줄 묶음 제목, 창 제목 |
| `text-xl` | 20/28 | 700 | 화면 제목(h1) |
| `text-2xl` | 24/32 | 700 | 프로필 닉네임 |
| `text-display` | 32/36 | 700, `font-display` | 프로필 수, 큰 점수 |
| `text-hero` | 72/72 | 700, `font-display` | 로그인 화면 워드마크만 |

- 한글 줄바꿈: 제목·단추·짧은 글은 `break-keep`(낱말 단위), 닉네임·주소·긴 영문은 `wrap-anywhere`(칸 안에서 끊음)를 함께 둔다
- 말줄임: 한 줄은 `truncate`, 여러 줄은 `line-clamp-2`·`line-clamp-3`. 말줄임한 전체 값은 `title` 대신 툴팁이나 펼치기로 준다
- 수: 1만 이상은 한국어 줄임(`Intl.NumberFormat("ko", { notation: "compact" })`, 예 1.2만). 정확한 수는 툴팁과 `aria-label`로 준다. 수 포맷은 `lib/format.ts`에 함수로 더해 쓴다
- 시각: 목록은 상대 시각(`formatRelative`), 마우스를 올리면 절대 시각(`formatDateTime`, 서비스 시간대). `<time dateTime>`으로 감싼다
- 자간은 기본값 그대로 둔다. 한글 제목에 음수 자간(`tracking-tight`)을 넣지 않는다

## 4. 색

### 4.1 토큰 값

`app/theme.css`의 `:root`(밝게)와 `.dark`(어둡게)에 적는다. 테마 파일은 색 검사가 보지 않으므로 hex로 적는다. 화면 코드는 `bg-*`, `text-*`, `border-*` 테마 변수 클래스로만 쓴다(MUST).

이름에 "새"가 붙은 줄은 지금 없는 토큰이라 `theme.css`에 값을, `globals.css`의 `@theme inline`에 `--color-<이름>: var(--<이름>)`을 더한다.

중립·면

| 토큰 | 밝게 | 어둡게 | 쓰는 곳 |
|---|---|---|---|
| `background` | `#FFFFFF` | `#1A1A1E` | 본문 열, 화면 바탕 |
| `foreground` | `#17171A` | `#ECECEF` | 기본 글자 |
| `card` / `card-foreground` | `#FFFFFF` / `#17171A` | `#202025` / `#ECECEF` | 곁줄 묶음, 모집 카드, 입력 바탕 |
| `popover` / `popover-foreground` | `#FFFFFF` / `#17171A` | `#24242A` / `#ECECEF` | 메뉴, 팝오버, 창, 토스트 |
| `muted` / `muted-foreground` | `#F0F0F3` / `#5E5E6A` | `#26262C` / `#A1A1AB` | 자리 표시, 흐린 바탕 / 보조 글자 |
| `secondary` / `secondary-foreground` | `#F0F0F3` / `#17171A` | `#26262C` / `#ECECEF` | 보조 단추 |
| `accent` / `accent-foreground` | `#EBEBEF` / `#17171A` | `#2C2C33` / `#ECECEF` | 마우스 올림·눌림 바탕 (강조 색이 아니다) |
| `border` | `#E2E2E8` | `#2E2E35` | 나눔 선, 카드 테두리 (꾸밈 선) |
| `input` | `#8A8A96` | `#6E6E7A` | 입력칸·체크 상자 테두리 (3:1 이상) |
| `rail` (새) / `rail-foreground` (새) | `#E8E8ED` / `#5E5E6A` | `#0D0D0F` / `#A1A1AB` | 레일 바탕과 아이콘 |
| `sidebar` / `sidebar-foreground` | `#F4F4F6` / `#17171A` | `#151518` / `#ECECEF` | 패널 바탕 |
| `sidebar-accent` / `sidebar-accent-foreground` | `#E8E8ED` / `#17171A` | `#24242A` / `#ECECEF` | 패널의 고른 줄 |
| `sidebar-primary` / `sidebar-primary-foreground` | `#17171A` / `#FFFFFF` | `#ECECEF` / `#17171A` | 패널 안 주 단추 |
| `sidebar-border` / `sidebar-ring` | `#E2E2E8` / `#4D6B00` | `#2E2E35` / `#C6F135` | 패널 선, 패널 포커스 |

주 색·강조

| 토큰 | 밝게 | 어둡게 | 쓰는 곳 |
|---|---|---|---|
| `primary` / `primary-foreground` | `#17171A` / `#FFFFFF` | `#ECECEF` / `#17171A` | 주 단추(잉크). 대부분의 "저장", "만들기" |
| `signal` (새) / `signal-foreground` (새) | `#C6F135` / `#17171A` | `#C6F135` / `#17171A` | 시그널 채움: 영역의 가장 중요한 단추 하나, 안 읽은 수 배지, 라이브 표시 |
| `signal-strong` (새) | `#4D6B00` | `#C6F135` | 시그널을 글자·선으로 쓸 때: 링크, 고른 탭 밑줄, 레일 선택 막대, 좋아요 켜짐 |
| `ring` | `#4D6B00` | `#C6F135` | 포커스 표시 |

의미 색 (기본 색은 아이콘·점·테두리, `-strong`은 글자. 의미 색 글자는 늘 `-strong`을 쓴다)

| 토큰 | 밝게 | 어둡게 | 뜻 |
|---|---|---|---|
| `success` / `success-strong` (새) | `#079455` / `#067647` | `#32D583` / `#32D583` | 완료, 연결됨, 모집 중 |
| `warning` (새) / `warning-strong` (새) | `#DC6803` / `#B54708` | `#FDB022` / `#FDB022` | 주의, 곧 마감, 처리 중 |
| `destructive` / `destructive-strong` | `#D92D20` / `#B42318` | `#F97066` / `#FDA29B` | 오류, 되돌릴 수 없는 행동 |
| `info` (새) / `info-strong` (새) | `#2E6FE0` / `#1D5FD1` | `#5BA3FF` / `#5BA3FF` | 안내, 도움말 |

연한 바탕은 `bg-<의미>/10`(밝게)·`bg-<의미>/20`(어둡게) 위에 `text-<의미>-strong`이다. 지금 위험 단추가 쓰는 방식과 같다.

차트

| 토큰 | 밝게 | 어둡게 |
|---|---|---|
| `chart-1` | `#4D6B00` | `#C6F135` |
| `chart-2` | `#2E6FE0` | `#5BA3FF` |
| `chart-3` | `#DC6803` | `#FDB022` |
| `chart-4` | `#079455` | `#32D583` |
| `chart-5` | `#5E5E6A` | `#A1A1AB` |

제공자 단추 (각 제공자 브랜드 지침 값, 2026-11-28 기준)

| 토큰 | 밝게 | 어둡게 | 근거 |
|---|---|---|---|
| `discord` (새) / `discord-foreground` (새) | `#5865F2` / `#FFFFFF` | `#5865F2` / `#FFFFFF` | Discord 브랜드 Blurple, 흰 로고 |
| `google` (새) / `google-foreground` (새) / `google-border` (새) | `#FFFFFF` / `#1F1F1F` / `#747775` | `#131314` / `#E3E3E3` / `#8E918F` | Google Identity 로그인 단추 Light·Dark 테마 |

- 지금 `.dark`의 `--sidebar-primary: oklch(0.488 0.243 264.376)`(보라빛 파랑)은 위 값으로 바꾼다
- 지금 `--radius: 0.625rem`은 5절 값으로 바꾼다

### 4.2 대비 (계산값, WCAG 2.x 상대 휘도)

글자는 4.5:1, 큰 글자·아이콘·테두리·포커스는 3:1 이상이어야 한다. 새 짝을 쓰면 이 표에 더한다.

| 짝 | 밝게 | 어둡게 |
|---|---|---|
| `foreground` / `background` | 17.89 | 14.71 |
| `muted-foreground` / `background` | 6.39 | 6.78 |
| `muted-foreground` / `muted` | 5.62 | 5.87 |
| `muted-foreground` / `accent` | 5.38 | 5.41 |
| `foreground` / `card` | 17.89 | 13.76 |
| `muted-foreground` / `card` | 6.39 | 6.33 |
| `muted-foreground` / `popover` | 6.39 | 6.03 |
| `sidebar-foreground` / `sidebar` | 16.29 | 15.46 |
| `muted-foreground` / `sidebar` | 5.82 | 7.12 |
| `rail-foreground` / `rail` | 5.23 | 7.58 |
| `primary-foreground` / `primary` | 17.89 | 15.17 |
| `signal-foreground` / `signal` | 13.66 | 13.66 |
| `signal-strong` / `background` | 6.14 | 13.25 |
| `signal-strong` / `sidebar` | 5.59 | 13.91 |
| `signal-strong` / `rail` | 5.03 | 14.83 |
| `ring` / `background` (3:1) | 6.14 | 13.25 |
| `input` / `background` (3:1) | 3.41 | 3.45 |
| `input` / `card` (3:1) | 3.41 | 3.22 |
| `destructive-strong` / `background` | 6.57 | 8.94 |
| `destructive-strong` / `destructive` 연한 바탕 | 5.65 | 7.04 |
| `success-strong` / `background` | 5.69 | 9.07 |
| `success-strong` / 연한 바탕 | 5.02 | 6.44 |
| `warning-strong` / `background` | 5.43 | 9.43 |
| `warning-strong` / 연한 바탕 | 4.84 | 6.61 |
| `info-strong` / `background` | 5.82 | 6.71 |
| `info-strong` / 연한 바탕 | 5.12 | 5.10 |
| `success`·`warning` 아이콘 / `background` (3:1) | 3.91 · 3.49 | 9.07 · 9.43 |
| `discord-foreground` / `discord` | 4.61 | 4.61 |
| `google-foreground` / `google` | 16.48 | 14.47 |
| `google-border` / `google` (3:1) | 4.53 | 5.83 |

`border`(꾸밈 선, 밝게 1.29 · 어둡게 1.29)는 3:1이 아니다. 그래서 입력칸·체크 상자·선택 상자처럼 테두리로 모양을 알아보는 것에는 `border`가 아니라 `input`을 쓴다(MUST).

### 4.3 색 쓰는 규칙

- 시그널 채움(`bg-signal`)은 한 영역(본문 열, 곁줄, 창 하나)에 단추 하나까지다. 나머지 주 행동은 `primary`(잉크)다
- 시그널을 밝게 모드에서 글자로 쓰지 않는다(`#C6F135`는 흰 바탕과 1.31:1). 글자·선은 늘 `signal-strong`이다
- 빨강(`destructive`)은 오류와 지우기·나가기·끊기에만 쓴다. 안 읽은 수, 알림 점, 좋아요에 빨강을 쓰지 않는다(시그널을 쓴다)
- 링크: 본문 안 링크는 `text-signal-strong underline underline-offset-2`. 메뉴·목록처럼 링크인 것이 뻔한 곳은 밑줄 없이 `hover:underline`
- 선택됨: 탭은 `signal-strong` 밑줄 3px, 레일은 왼쪽 막대, 패널 줄은 `bg-sidebar-accent` + `font-medium`
- 투명도 섞기는 테마 변수에만 한다(`bg-signal/15`, `bg-destructive/10`). 팔레트 색에 섞지 않는다

## 5. 간격·모서리·그림자·움직임

### 간격

4px 눈금이다. Tailwind 기본 간격(`1` = 4px)을 그대로 쓰고 아래 값만 쓴다(SHOULD).

| 쓰는 곳 | 값 |
|---|---|
| 아이콘과 글자 사이 | `gap-1.5`(6px) ~ `gap-2`(8px) |
| 목록 줄 안쪽 | `px-3 py-2`(패널), `px-4 py-3`(피드 항목) |
| 덩어리 사이 | `gap-4`(16px). 화면 안쪽 여백은 `p-(--page-padding)` |
| 곁줄 묶음 사이 | `gap-4` |
| 폼 칸 사이 | `FieldGroup` 기본값 |

### 모서리

`--radius`를 `0.375rem`(6px)으로 바꾼다. 지금 `globals.css`의 배율이 그대로 따라 바뀐다.

| 클래스 | 값 | 쓰는 곳 |
|---|---|---|
| `rounded-none` | 0 | 피드 항목, 본문 열 머리, 띠 |
| `rounded-md` | 약 5px | 배지(사각), 작은 단추 |
| `rounded-lg` | 6px | 단추, 입력칸, 메뉴 항목, 패널 줄 |
| `rounded-xl` | 약 8px | 카드, 곁줄 묶음, 피드 안 미디어 |
| `rounded-2xl` | 약 11px | 창, 서랍 윗모서리 |
| `rounded-group` (새, `--radius-group: 30%`) | 30% | 모임(파티·크루) 아바타, 레일 아이콘 단추 |
| `rounded-full` | 원 | 사람 아바타, 수 배지, 상태 점 |

`rounded-3xl`, `rounded-4xl`은 쓰지 않는다(MUST NOT).

### 그림자

- 카드, 피드 항목, 곁줄 묶음, 단추에는 그림자가 없다(MUST NOT). 테두리와 바탕색 단계로 나눈다
- 그림자는 떠 있는 층(메뉴, 팝오버, 툴팁, 창, 서랍, 토스트)에만 쓰고, 그 컴포넌트(`components/ui`) 안에 이미 있는 것만 쓴다. 화면 코드에서 `shadow-*`를 더하지 않는다
- 어둡게에서는 그림자 대신 `ring-1 ring-foreground/10`(지금 팝오버 방식)으로 층을 나눈다

### 움직임

| 토큰 (새, `layout-tokens.css`) | 값 | 쓰는 곳 |
|---|---|---|
| `--duration-fast` | 120ms | 마우스 올림, 눌림, 토글 |
| `--duration-base` | 180ms | 메뉴·팝오버·툴팁 열기 |
| `--duration-slow` | 240ms | 서랍, 창, 패널 겹쳐 열기 |
| `--ease-out` | `cubic-bezier(0.2, 0, 0, 1)` | 나타남 |
| `--ease-in` | `cubic-bezier(0.4, 0, 1, 1)` | 사라짐 |

- 240ms를 넘는 움직임은 쓰지 않는다. 튕김(bounce, spring), 흔들림, 반복 장식 움직임은 쓰지 않는다
- `motion-reduce:` 아래에서는 `transform` 움직임(미끄러짐·커짐)을 끄고 `opacity`만 120ms 이하로 남긴다. `tw-animate-css`의 `slide-in-*`, `zoom-in-*`에는 `motion-reduce:animate-none`이나 같은 효과를 둔다
- 눌림은 지금 단추의 `active:translate-y-px` 하나다. 커지기(`scale`)는 쓰지 않는다

## 6. 배치

### 6.1 폭 기준과 배치 상수

폭 기준은 Tailwind 기본값을 쓴다: `md` 768px, `lg` 1024px, `xl` 1280px. 좁은 화면 기준(767px 이하)은 지금 `use-mobile.ts`(768 미만)와 같다.

`layout-tokens.css`의 값을 바꾸거나 더한다.

| 상수 | 값 | 지금 값 | 쓰는 곳 |
|---|---|---|---|
| `--rail-width` | `4.5rem` (72px) | 3.5rem | 레일 |
| `--panel-width` | `15rem` (240px) | 14rem | 패널 |
| `--aside-width` (새) | `20rem` (320px) | - | 곁줄 |
| `--feed-max-width` (새) | `37.5rem` (600px) | - | 본문 열 최대 폭 (피드·채팅·상세) |
| `--wide-max-width` (새) | `58.5rem` (936px) | - | 프로필, 탐색 격자, 모집 카드 격자 (본문 열 + 곁줄 자리) |
| `--header-height` | `3rem` (48px) | 3.5rem | 본문 열 머리 줄, 패널 머리 줄, 좁은 화면 머리 줄 |
| `--tabbar-height` (새) | `3.5rem` (56px) | - | 좁은 화면 아래 탭 줄 (기기 아래 안전 여백은 더한다) |
| `--page-padding` | `1rem` | 1rem | 그대로 |

관리 셸은 이 배치를 쓰지 않고 지금 사이드바 한 겹을 그대로 쓴다. `--rail-width`·`--panel-width`를 관리 셸이 쓰고 있으면 관리 셸용 상수를 따로 둔다(plan에서 확인).

### 6.2 메인 셸 (사용자 셸, 디스코드 × X)

1280px 이상

```text
┌──────┬────────────────┬────────────────────────────────┬──────────────────────┐
│ RAIL │ PANEL          │ MAIN (본문 열, 최대 600px)       │ ASIDE (320px)        │
│ 72px │ 240px          │                                │                      │
│      │┌──────────────┐│┌──────────────────────────────┐│┌────────────────────┐│
│ [m]  ││ 홈        48 ││││ 홈            [추천|팔로잉] 48 ││││ [검색 칸]           ││
│ ──── │└──────────────┘│└──────────────────────────────┘│└────────────────────┘│
│▌[⌂]  │ 묶음 이름       │ ┌──┐ 닉네임 #0421 · 3분 전   ⋯ │┌ 인기 숏폼 ─────────┐│
│ [◎]  │  # 추천        │ │av│ 본문 15/22 ...             ││ 1  썸네일 제목  1.2만 ││
│ [⚑]  │ ▌# 팔로잉  ●   │ └──┘ [미디어 rounded-xl]       ││ 2  ...              ││
│ ──── │  # 게임별      │      ○ 12   ♡ 340   ↗          ││ 더 보기              ││
│ [P1] │                │ ────────────────────────────── │└────────────────────┘│
│ [P2] │                │ ┌──┐ 다음 항목 ...              │┌ 모집 중 파티 ───────┐│
│ [+]  │                │                                ││ ...                 ││
│      │┌──────────────┐│                                │└────────────────────┘│
│      ││av 닉네임 🔔⚙⏻││                                │ 이용약관 · 개인정보처리방침│
└──────┴┴──────────────┴┴────────────────────────────────┴──────────────────────┘
  rail     sidebar         background                        background
```

(그림의 기호는 자리 표시다. 실제 화면에는 lucide 아이콘을 쓰고 이모지를 쓰지 않는다)

- 레일(`<nav aria-label>` "영역"): 바탕 `bg-rail`
  - 맨 위 moa 표지(48px, `rounded-group`, `bg-primary text-primary-foreground`, `font-display` "m") → 홈 `/`
  - 구분선(`h-px w-8 bg-border`)
  - 사용자 메뉴 1단계: 48px 아이콘 단추(`rounded-group`, 아이콘 22px, `text-rail-foreground`, 올리면 `bg-accent text-foreground`), 툴팁은 오른쪽
  - 구분선 아래 모임 자리(파티·크루 아바타 48px `rounded-group`). 무엇을 보일지는 모임 스펙이 정한다
  - 선택 표시: 단추 왼쪽 끝에 붙은 막대 `w-1 rounded-r-full bg-signal-strong`, 고름 40px · 올림 20px · 안 읽음 8px 점. `aria-current="page"`
  - 넘치면 레일 안에서 세로 스크롤(스크롤 막대 숨김), 표지는 위에 고정
- 패널(`<nav aria-label>` 고른 1단계 이름): 바탕 `bg-sidebar`, 오른쪽 `border-r border-sidebar-border`
  - 머리 줄 48px: 1단계 메뉴 이름 `text-title-sm`, 아래 선
  - 2단계 메뉴 줄: 높이 32px, `rounded-lg px-2 gap-2`, 아이콘 18px + `text-sm font-medium text-muted-foreground`. 고름은 `bg-sidebar-accent text-sidebar-accent-foreground`, 안 읽음은 `text-foreground font-bold` + 오른쪽 시그널 수 배지
  - 묶음 이름: `text-meta font-medium text-muted-foreground px-2 pt-4 pb-1`
  - 맨 아래 계정 줄(`001-admin-menus` FR-005 그대로): 높이 52px, 위 선, 아바타 32px + 닉네임 + 벨·톱니·로그아웃 아이콘 단추
- 본문 열(`<main id="main">`): 바탕 `bg-background`, 폭 `max-w-(--feed-max-width)`, 좌우 `border-x border-border`(1280px 이상)
  - 머리 줄 48px, `sticky top-0 z-10 bg-background border-b`. 제목은 화면 h1(`text-title-sm`, 화면 제목은 이 줄에 둔다). 탭이 있으면 오른쪽 또는 아래 줄에 X식 같은 폭 탭
  - 머리 줄은 불투명이다. 흐림(`backdrop-blur`)을 쓰지 않는다
- 곁줄(`<aside aria-label>` "추천"): 폭 `w-(--aside-width)`, `sticky top-0 h-svh overflow-y-auto py-3 px-4 gap-4`
  - 맨 위 검색 칸(검색 기능이 생기면). 그 아래 기능 묶음들, 맨 아래 이용약관·개인정보처리방침 줄(`text-xs text-muted-foreground`, 개인정보처리방침 굵게)
  - 묶음: `rounded-xl border bg-card`, 제목 `text-title-sm px-4 pt-3 pb-2`, 줄 `px-4 py-2.5 hover:bg-accent`(최대 5줄), 아래 "더 보기" `text-sm text-signal-strong px-4 py-3`
- 넓은 화면(`--wide-max-width`)을 쓰는 화면(프로필, 탐색, 모집)은 본문 열과 곁줄 자리를 함께 쓰고 곁줄을 두지 않는다

1024~1279px: 곁줄이 없다. 본문 열은 남는 폭 가운데. 푸터 줄은 본문 맨 아래.

768~1023px

```text
┌──────┬──────────────────────────────────────┐
│ RAIL │ [≡] 홈             [추천|팔로잉]  48 │  ← [≡] 패널 열기 (aria-expanded)
│      │──────────────────────────────────────│
│ [m]  │ 본문 열 (가운데, 최대 600px)         │
│ [⌂]  │                                      │
│ [◎]  │    ┌ PANEL 겹침 240px ┐              │
│ ...  │    │ (열면 본문 위,    │              │
│      │    │  바깥 어둡게)     │              │
└──────┴────┴──────────────────┴──────────────┘
```

- 패널은 Sheet(왼쪽, 레일 오른쪽에서 나옴)로 연다. 바깥 누름·Esc로 닫고 포커스를 [≡]로 돌린다. 계정 줄은 패널 안에 있다

767px 이하

```text
┌──────────────────────────────┐
│ [m]   홈             [벨]  48│  ← 머리 줄 (sticky, 불투명)
│──────────────────────────────│
│ 본문 열 (가득, 좌우 여백 없음)│
│ ┌──┐ 닉네임 #0421 · 3분 전   │
│ └──┘ 본문 ...                │
│──────────────────────────────│
│                              │
│──────────────────────────────│
│ [⌂]  [◎]  [⚑]  [✉]  [⋯]   56│  ← 아래 탭 줄: 레일 앞 넷 + 더보기
└──────────────────────────────┘
  더보기 → 왼쪽 서랍: [레일 | 패널] + 맨 아래 계정 줄
```

- 아래 탭 줄: `fixed bottom-0 h-(--tabbar-height) pb-[env(safe-area-inset-bottom)] border-t bg-background`, 칸마다 아이콘 24px + `text-xs` 글자, 고름은 `text-foreground` + 위쪽 2px `bg-signal-strong`, 안 고름은 `text-muted-foreground`. `<nav aria-label>` 안의 링크들이다
- 본문 아래 여백은 탭 줄 높이만큼 둔다(마지막 항목이 가려지지 않게)
- 서랍은 지금 셸의 Sheet 방식을 쓴다. 서랍 안은 넓은 화면의 레일+패널과 같은 구성이다
- 토스트는 탭 줄 위에 뜬다

### 6.3 프로필 (인스타그램식)

넓은 화면 (폭 `max-w-(--wide-max-width)`, 곁줄 없음)

```text
┌──────────────────────────────────────────────────────────────┐
│ [←] 닉네임                                              48  │ ← 본문 머리 줄
├──────────────────────────────────────────────────────────────┤
│                                                              │
│   ┌────────┐   닉네임 #0421 [자물쇠]  [친구 추가] [팔로우] [⋯]│
│   │ 아바타  │                                                │
│   │ 128px  │   128      2.4만     310                        │
│   │ (원)   │   친구     팔로워    팔로잉                      │
│   └────────┘                                                 │
│               자기소개 3줄까지, 더보기                         │
│               [리그 오브 레전드 · 미드 · 다이아 2] [저녁 9~12시]│ ← 009 공개 항목 (테두리 칩)
│                                                              │
├──────────────────────────────────────────────────────────────┤
│        [▦ 숏폼]          [다른 탭]          [다른 탭]          │ ← 탭: 아이콘 16 + 글자, 고름 = 아래 3px signal-strong
├──────────────────────────────────────────────────────────────┤
│ ┌────────┐┌────────┐┌────────┐                               │
│ │  3:4   ││  3:4   ││  3:4   │   3열, 간격 2px (gap-0.5)      │
│ │▷ 1.2만 ││▷ 980   ││▷ 45    │   조회수: 왼쪽 아래, 흰 글자 + │
│ └────────┘└────────┘└────────┘   아래쪽 어둡게 깔기(검정 40%)  │
└──────────────────────────────────────────────────────────────┘
```

- 머리: `grid grid-cols-[auto_1fr] gap-x-10 px-6 py-8`. 아바타 `size-32 rounded-full`
- 이름 줄: 닉네임 `text-2xl font-bold`, 태그 `font-mono text-meta text-muted-foreground`, 비공개면 lucide `Lock` 16px + 화면 읽기 글자
- 수 줄: 숫자 `font-display text-display`, 이름 `text-meta text-muted-foreground`. 셋 다 링크(볼 수 없으면 링크가 아닌 글자, `014` FR-018)
- 관계 단추: `014` "화면"의 표대로. 주 행동 하나(친구 추가 또는 팔로우)만 `primary`, 나머지 `secondary`. 시그널은 쓰지 않는다(프로필에서 가장 중요한 행동이 사람마다 다르다)
- 009 공개 항목: 테두리 칩(`rounded-md border px-2 py-0.5 text-meta`)
- 탭: `role="tablist"`, 탭 값은 주소 검색 값(`?tab=shortform`). 고른 탭 `aria-selected="true"`, 아래 3px `bg-signal-strong`(본문 머리 줄 탭과 같은 밑줄), 글자 `text-foreground font-medium`. 안 고른 탭 `text-muted-foreground`
- 격자: `grid grid-cols-3 gap-0.5`, 칸 `aspect-[3/4] overflow-hidden bg-muted`, 모서리 없음. 칸은 링크(세로 뷰어 주소)이고 이름은 "올린 사람의 숏폼, 조회수 N"
- 조회수 깔기: 그림 위 아래쪽 1/3에 `bg-black/40`은 색 검사에 걸리므로 새 토큰 `--scrim: oklch(0 0 0 / 0.6)`(두 모드 같음. 흰 그림 위에서도 흰 글자가 4.5:1을 넘는 진하기)를 더해 `bg-scrim`으로 쓴다. 글자는 `text-scrim-foreground`(`#FFFFFF`)
- 비공개로 볼 수 없으면 탭과 격자 자리에 `Lock` 아이콘 + "비공개 계정입니다" + 한 줄 설명(가운데, `py-16`)

좁은 화면

```text
┌──────────────────────────────┐
│ [←] 닉네임              [⋯] │
├──────────────────────────────┤
│ ┌─────┐   128   2.4만   310 │ ← 아바타 80px + 수 줄 한 줄
│ │ 80  │   친구  팔로워 팔로잉│
│ └─────┘                     │
│ 닉네임 #0421 [자물쇠]         │
│ 자기소개 ...                  │
│ [칩] [칩]                     │
│ [  친구 추가  ][  팔로우   ]  │ ← 폭을 나눠 가득, 높이 40px
├──────────────────────────────┤
│   [▦]        [ ]        [ ]  │ ← 탭은 아이콘만 + aria-label
├──────────────────────────────┤
│ 3열 격자 (gap 2px)            │
└──────────────────────────────┘
```

### 6.4 로그인 화면 (소셜 우선)

(public) 레이아웃이다. 사용자 셸 밖이다.

1024px 이상

```text
┌─────────────────────────────────┬────────────────────────────────┐
│ bg-rail                         │ bg-background                  │
│                                 │                                │
│                                 │      moa 로그인          (h1)  │
│   moa                (text-hero)│                                │
│   같이 할 사람, 여기 다 모여요   │   ┌──────────────────────────┐ │
│   (text-xl, foreground)         │   │[D]   Discord로 계속하기   │ │ 40px, bg-discord
│                                 │   └──────────────────────────┘ │
│                                 │   ┌──────────────────────────┐ │
│                                 │   │[G]   Google로 계속하기    │ │ 40px, bg-google + border
│                                 │   └──────────────────────────┘ │
│                                 │   ──────── 또는 ────────        │
│                                 │   아이디 [               ]     │
│                                 │   비밀번호 [             ]     │
│                                 │   [        로그인         ]    │ primary
│                                 │   (019 가입·찾기 링크 자리)     │
│                                 │                                │
│                                 │   이용약관 · 개인정보처리방침   │
└─────────────────────────────────┴────────────────────────────────┘
          1fr                          폼 열 폭 22.5rem, 가운데
```

- 왼쪽 판은 단색 `bg-rail`이다. 그림·그라디언트·사진·움직임을 두지 않는다. 표어 문구는 문구 파일에 둔다(위 문구는 예시)
- 1023px 이하는 왼쪽 판이 없고, 폼 위에 `font-display` 워드마크(`text-display`)만 둔다
- 카드 상자로 감싸지 않는다(지금 `Card`를 걷어 낸다). 폼 열이 곧 화면이다
- 실패 안내(`OAuthErrorAlert`, 탈퇴 안내 등)는 h1 아래, 소셜 단추 위에 `Alert`로 둔다

소셜 단추 규칙 (MUST)

- 순서: Discord, Google. 설정된 제공자만 보인다(`010` FR-001). 하나만 있으면 그 하나만, 빈자리 없음
- 모양: 높이 40px(`h-10`), 폭 가득, `rounded-lg`, 로고 20px을 왼쪽 12px 자리에, 글자 `text-sm font-medium` 가운데. 두 단추의 로고 자리·글자 자리가 같다. 제공자 지침이 로고 크기·여백·모서리를 다르게 정하면 지침이 먼저이고, 두 단추를 그 값에 함께 맞춘다(plan에서 두 지침 원문을 확인한다)
- Discord: `bg-discord text-discord-foreground`, 흰 Discord 로고(Clyde). 올림 `hover:bg-discord/90`
- Google: `bg-google text-google-foreground border border-google-border`, 여러 색 "G" 로고. 밝게·어둡게는 Google의 Light·Dark 단추를 따른다
- 로고는 제공자가 낸 공식 SVG를 `public/brands/discord.svg`, `public/brands/google.svg`로 두고 `<img alt="">`(글자가 이름을 말하므로 꾸밈)로 쓴다. 색을 바꾸거나 다시 그리거나 lucide 아이콘으로 대신하지 않는다. 화면 코드에 SVG의 색 값을 적으면 색 검사에 걸리므로 파일로 둔다
- 글자: "Discord로 계속하기", "Google로 계속하기"(지금 `signIn.continueWith.google`과 같은 꼴). 제공자 이름은 영문 그대로
- 누르면 `aria-busy`와 `disabled`, 로고 자리에 `Spinner`. 제공자 화면으로 넘어가는 동안 다시 눌리지 않는다
- 포커스 표시는 다른 단추와 같은 `ring`이다

### 6.5 계정 설정의 소셜 계정 칸

```text
소셜 계정
┌────────────────────────────────────────────────────────────┐
│ [D]  Discord        ● 연결됨                       [해제]   │
│ [G]  Google           연결 안 됨                    [연결]   │
│      유일한 로그인 수단이라 해제할 수 없어요.                 │ ← 해제할 수 없을 때만
└────────────────────────────────────────────────────────────┘
```

- 줄: `flex items-center gap-3 px-4 py-3`, 줄 사이 `divide-y`, 바깥 `rounded-xl border`
- 로고 20px(로그인 단추와 같은 파일), 제공자 이름 `text-sm font-medium`
- 연결됨: `size-2 rounded-full bg-success` 점 + `text-meta text-success-strong` "연결됨". 연결 안 됨: `text-meta text-muted-foreground` "연결 안 됨"
- 단추: 연결은 `outline`, 해제는 `ghost`(누르면 `ConfirmDialog`로 한 번 묻는다. 되돌릴 수 있으므로 위험 색은 쓰지 않는다)
- 해제할 수 없는 줄은 해제 단추를 그대로 두고, 줄 아래 `text-meta text-muted-foreground`로 이유를 미리 보인다. 눌렀을 때의 안내는 `010` US5-AC6대로다

## 7. 컴포넌트 규칙

기준 컴포넌트는 `components/ui`(키트 골격)와 `components/kit`이다. 같은 일을 하는 것을 새로 만들지 않는다(MUST NOT). 모자라면 그 컴포넌트에 종류(variant)를 더한다.

### 단추

| 종류 | 모양 | 쓰는 곳 |
|---|---|---|
| `default` | `bg-primary`(잉크) | 대부분의 주 행동: 저장, 만들기, 보내기 |
| `signal` (새 종류) | `bg-signal text-signal-foreground hover:bg-signal/85` | 영역마다 하나: 파티 참가, 올리기 같은 그 화면의 목적 |
| `outline` | 테두리 `border-input` | 보조 행동, 연결 |
| `secondary` | `bg-secondary` | 관계 단추의 두 번째, 거르기 |
| `ghost` | 바탕 없음 | 아이콘 단추, 줄 안 행동, 취소 |
| `destructive` | 연한 빨강 바탕 + `destructive-strong` 글자 | 지우기, 나가기, 끊기 |
| `link` | `text-signal-strong` 밑줄 | 글 안 행동 |

- 크기: 기본 `h-8`(32px). 좁은 화면의 주 행동과 폼 보내기, 소셜 단추는 `lg` 또는 `h-10`(40px). 아이콘 단추는 `icon`(32px), 좁은 화면 머리 줄은 `icon-lg`(36px)
- 단추 글자는 하는 일이다(9절). 아이콘과 함께면 아이콘은 왼쪽, 16px
- 진행 중: `disabled` + `aria-busy="true"` + 아이콘 자리에 `Spinner`. 글자는 바꾸지 않는다(폭이 흔들리지 않게)
- 한 줄에 단추가 여럿이면 주 행동이 오른쪽 끝(창, 폼 아래 줄), 프로필 관계 단추는 왼쪽부터

### 입력

- `Field` + `FieldLabel` + `Input` + `FieldError`(지금 로그인 화면 방식). 칸 이름은 늘 보인다. 자리 표시 글자(placeholder)는 예시에만 쓰고 이름 대신 쓰지 않는다
- 테두리 `border-input`, 높이 32px(폼은 36px SHOULD), `rounded-lg`, 바탕 `bg-background`(어둡게 `bg-card`)
- 오류: 칸에 `aria-invalid`, 칸 아래 `FieldError`(`text-destructive-strong`, `id`를 `aria-describedby`로 잇는다)
- 선택 칸 이름 끝에 "(선택)". 필수 칸에 별표를 쓰지 않는다
- 글자 수 제한이 있으면 칸 오른쪽 아래 `text-meta text-muted-foreground tabular-nums` "12/100"

### 피드 항목 (X식)

```text
┌──┐ 닉네임 #0421 · 3분 전                          [⋯]
│av│ 본문 text-body, 4줄 넘으면 "더 보기"
└──┘ ┌──────────────────────────────┐
     │ 미디어 rounded-xl border      │ 최대 높이 32rem, 비율 유지
     └──────────────────────────────┘
     [○ 12]   [♡ 340]   [↗]                         ← ghost 아이콘 단추, 18px, 수 text-meta tabular-nums
```

- `<article aria-labelledby>`, 줄 사이 `border-b`, 안쪽 `px-4 py-3`, `grid grid-cols-[2.5rem_1fr] gap-3`. 카드·그림자·둥근 상자로 감싸지 않는다
- 아바타 40px 원. 닉네임 `text-body font-bold`, 태그 `font-mono text-meta text-muted-foreground`, 시각 `<time>` `text-meta text-muted-foreground`
- 항목 전체를 누르면 상세로 간다면, 제목 링크를 늘린 덮개(`after:absolute after:inset-0`)로 만들고 안의 단추는 그 위(`relative z-10`)에 둔다. `div onClick`으로 만들지 않는다
- 켜짐 상태(좋아요 등)는 `aria-pressed`, 켜지면 아이콘 채움 + `text-signal-strong`
- 무슨 행동이 있는지는 각 기능 스펙이 정한다. 이 모양은 자리만 정한다

### 모집 카드·격자 카드

- `rounded-xl border bg-card p-4`, 그림자 없음. 올림 `hover:border-input`
- 위: 게임 이름(`text-meta text-muted-foreground`) + 상태 배지, 가운데: 제목 `text-title-sm line-clamp-2`, 아래: 인원 `font-display` "3/5" + 빈 포지션 칩 + 방장 아바타 24px
- 카드 전체가 링크(늘린 덮개 방식)

### 패널·메뉴 목록

- 6.2절의 패널 줄 규칙. 메뉴는 `DropdownMenu`, 오른쪽 누름 메뉴는 `ContextMenu`, 명령 찾기는 `Command`

### 창·서랍·시트

- 확인(되돌릴 수 없는 것): `AlertDialog`(`kit/confirm-dialog`). 제목은 질문 "파티에서 나갈까요?", 설명 한 줄, 단추는 "취소"와 그 행동("나가기", 위험이면 `destructive`)
- 입력이 있는 창: 넓은 화면 `Dialog`(최대 폭 28rem, 폼은 `--form-max-width`), 좁은 화면 `Drawer`(아래에서 올라옴)
- 옆 서랍: `Sheet`(지금 `kit/side-drawer`)
- 창 바깥 어둡게는 컴포넌트 기본값을 쓴다. 흐림을 더하지 않는다

### 토스트·알림

- 토스트(`sonner`): 끝난 일 알림 한 줄("저장했어요"), 4초. 넓은 화면 오른쪽 아래, 좁은 화면 탭 줄 위 가운데. 되돌리기가 있으면 "되돌리기" 단추 하나
- 화면 안 알림(`Alert`): 화면을 떠나도 남아야 하는 안내, 폼 전체 오류. 의미 색 연한 바탕 + 아이콘 + 글자
- 오류를 토스트로만 알리지 않는다. 폼 오류는 칸 아래·폼 위, 불러오기 오류는 그 자리에

### 배지·칩

| 종류 | 모양 |
|---|---|
| 수 배지 (안 읽음) | `min-w-5 h-5 rounded-full bg-signal text-signal-foreground text-xs font-bold tabular-nums px-1.5`. 100 이상은 "99+" |
| 상태 배지 | `rounded-md px-1.5 py-0.5 text-xs font-medium bg-<의미>/10 text-<의미>-strong` + 앞에 점이나 아이콘. 글자로 상태를 말한다 |
| 칩 (게임·포지션·티어) | `rounded-md border px-2 py-0.5 text-meta`. 고를 수 있으면 `Toggle`, 고르면 `bg-primary text-primary-foreground` |
| 라이브·새것 | `bg-signal text-signal-foreground text-xs font-bold` 영문 대문자 "LIVE", "NEW"는 쓰지 않고 한글 "진행 중", "새 글"로 쓴다 |

### 아바타

| 크기 | 쓰는 곳 |
|---|---|
| 24px | 카드 안 작은 사람, 겹친 멤버 |
| 32px | 계정 줄, 패널 줄, 곁줄 줄 |
| 40px | 피드 항목, 채팅 |
| 48px | 레일 모임 아이콘 |
| 80px / 128px | 프로필 머리 (좁은 / 넓은) |

- 사람은 `rounded-full`, 모임(파티·크루)은 `rounded-group`(MUST). 모양으로 사람과 모임을 구별한다
- 그림은 `avatar-image.tsx`(기본 아바타 키 → `/avatars/<키>.svg`)를 쓴다. `alt`는 닉네임, 옆에 닉네임 글자가 있으면 `alt=""`
- 테두리·빛나는 고리·그라디언트 고리를 두르지 않는다

### 탭

- 본문 머리 줄 탭(X식): 같은 폭, 높이 48px, 고름 `font-bold text-foreground` + 글자 폭만큼 아래 3px `bg-signal-strong rounded-full`
- 프로필 탭(인스타그램식): 6.3절
- 탭 값은 주소에 둔다(10절). `Tabs` 컴포넌트를 쓴다

### 표

관리 화면과 업무 목록은 `kit/data-table`과 `011-list-screen` 규칙 그대로다. 사용자 화면에서는 표보다 목록 줄을 먼저 쓴다(SHOULD).

## 8. 상태

| 상태 | 모양 | 컴포넌트 |
|---|---|---|
| 처음 불러오기 | 내용과 같은 모양·크기의 자리 표시(`Skeleton`, `bg-muted`). 피드는 항목 3개, 격자는 칸 9개, 곁줄 묶음은 줄 3개 | `Skeleton`, `kit/query-state` |
| 다시 불러오기 | 있던 내용 그대로 + 머리 줄 아래 2px 진행 막대 또는 단추 `Spinner` | React Query `placeholderData: keepPreviousData` |
| 다음 묶음 (피드) | 목록 끝에 항목 1개 자리 표시. 끝이면 "끝까지 봤어요" 한 줄 `text-meta text-muted-foreground` | |
| 빈 결과 | 가운데 `py-16`: lucide 아이콘 32px `text-muted-foreground`, 한 줄 설명 `text-sm`, 할 일 단추 하나(있으면) | `Empty` |
| 오류 | 그 자리에: 오류 코드 문구(`errors.*`), "다시 시도" `outline` 단추, 문의 번호 `font-mono text-meta`(`common.traceId`) | `kit/error-view` |
| 화면 전체 오류·없음 | `error.tsx`, `not-found.tsx` 지금 것 | |

- 회전 표시(`Spinner`)만 화면 가운데에 두지 않는다. 단추 안과 300ms 안에 끝나는 짧은 일에만 쓴다
- 빈 결과에 일러스트, 이모지, 큰 장식 그림을 두지 않는다
- 오류 문구는 `role="alert"`(폼 전체 오류, 실패), 완료·진행 안내는 `role="status"`로 읽힌다. 토스트는 `sonner`가 읽힌다

## 9. 문구

모든 글자는 `messages/ko.json`에 키로 둔다(MUST). 화면 읽기 도구 이름(`aria-label`), 툴팁, `alt`, 화면 제목(metadata)도 같다. 키는 기능 이름 묶음 아래(`party.create.submit`), 함께 쓰는 것은 `common.*`.

- 해요체다: "저장했어요", "다시 시도해 주세요". "~습니다", "~함", "~하세요!"를 섞지 않는다
- 느낌표, 이모지, 물결표(~), "앗!", "짠!", "야호" 같은 감탄을 쓰지 않는다(MUST NOT)
- 단추·메뉴·탭·칸 이름은 마침표 없이 짧게(8자 안팎). 단추는 하는 일: "파티 만들기", "참가", "저장", "나가기". "확인"은 정보만 보이는 창의 닫기에만 쓴다
- 확인 창: 제목은 질문("이 숏폼을 지울까요?"), 설명은 결과("지우면 되돌릴 수 없어요."), 단추는 행동("지우기")
- 오류: 무슨 일인지 + 무엇을 하면 되는지 한두 문장. 사용자를 탓하지 않는다("잘못 입력했습니다" 대신 "아이디를 입력해 주세요")
- 빈 결과: 상황 한 문장 + 할 일("아직 파티가 없어요. 첫 파티를 만들어 보세요." 단추 "파티 만들기")
- 숫자와 단위는 붙인다: "3명", "5분 전", "1.2만". 날짜는 `formatDate`·`formatRelative`로만
- 브랜드 이름은 원래 표기: Discord, Google, moa(늘 소문자)
- 용어는 기존 스펙을 따른다: 회원, 닉네임, 태그, 파티, 방장, 숏폼, 팔로우, 친구, 탈퇴, 완전 삭제

## 10. 주소·메타·성능

주소

- 탭, 거르기, 검색어, 쪽은 주소 검색 값이다(`lib/list-search.ts` 방식). 다시 열거나 나누면 같은 화면이다
- 주소가 있는 겹침 화면(세로 뷰어, 상세 패널)은 열면 주소가 바뀌고 뒤로 가기로 닫힌다. 확인 창·메뉴·토스트는 주소를 바꾸지 않는다
- 이동은 `next/link`의 `<Link>`, 동작은 `<button>`. `router.push`는 폼을 보낸 뒤처럼 동작 끝의 이동에만
- 피드는 React Query 캐시로 뒤로 가기 때 다시 불러오지 않고 스크롤 자리를 되살린다
- 로그인이 필요하면 `signInPath(현재 주소)`로 보내고 로그인 뒤 돌아온다(지금 규칙)
- 프로필 주소는 회원 순번을 드러내지 않는다(`014` FR-003)

메타

- 화면마다 `metadata.title`을 준다. 바깥 틀 `"%s | moa"`는 지금 `layout.tsx`에 있다. 제목 글자도 문구 파일에서 온다(`generateMetadata` + `getTranslations`)
- 누구나 볼 수 있고 나눌 만한 화면(공지 상세, 공개·승인 파티, 공개 숏폼, 탐색, 모집)은 `description`과 `openGraph`(제목, 설명, 대표 그림 1200×630 또는 숏폼 썸네일)를 준다
- 로그인해야 보는 화면, 계정·설정·마이페이지, 로그인·가입 화면은 `robots: { index: false }`
- `<html lang>`은 지금처럼 언어 설정에서 온다

성능

- 글꼴은 2절대로 쓰는 굵기만 받는다. 새 굵기·새 글꼴을 더하지 않는다
- 그림은 `next/image`. `sizes`를 실제 보이는 폭으로 준다(격자 칸 `"(max-width: 767px) 33vw, 312px"`, 피드 미디어 `"(max-width: 767px) 100vw, 568px"`, 아바타는 그 px). 첫 화면의 큰 그림 하나만 `priority`, 나머지는 기본 지연 받기
- 그림·영상 자리는 비율(`aspect-*`)이나 크기로 미리 잡는다. 자리 표시도 같은 비율이다
- 목록은 커서 무한 스크롤(피드 목록, 원칙 문서 "목록")이고, 화면 밖으로 많이 내려가면 지난 항목을 가상화할지는 그 기능 plan에서 정한다
- 실시간 갱신(파티 채팅 등)은 바뀐 항목만 다시 그린다

## 11. 아이콘

- lucide(`lucide-react`)만 쓴다(MUST). 메뉴 아이콘은 `components/menu/icons.ts` 목록에서 고르고, 없으면 거기에 더한다
- 크기: 줄 안·단추 16px, 패널 줄 18px, 피드 행동 18px, 레일 22px, 아래 탭 줄 24px, 빈 결과 32px. `strokeWidth`는 기본(2)을 쓰고 레일·탭 줄처럼 큰 곳만 1.75
- 색은 글자를 따른다(`currentColor`). 아이콘에만 다른 색을 칠하지 않는다(의미 색 아이콘은 예외)
- 아이콘만 있는 단추는 `aria-label`(문구 파일) + `Tooltip`. 글자 옆 아이콘은 `aria-hidden`(lucide 기본)
- 쓰지 않는 아이콘: `Sparkles`, `Wand`·`WandSparkles`, `Bot`, `Stars`, `BrainCircuit`, `Rocket`(꾸밈용), `PartyPopper`. 기능이 실제로 그 뜻일 때만 예외로 리뷰에서 묻는다
- 브랜드 로고(Discord, Google 등)는 lucide에 없고 그려 넣지도 않는다. 6.4절대로 공식 파일을 쓴다

## 12. 접근성

- 영역: 레일·패널·아래 탭 줄 `<nav aria-label>`, 본문 `<main id="main">`, 곁줄 `<aside aria-label>`, 머리 줄 `<header>`. 레일과 패널의 이름은 다르게
- 화면마다 `<h1>` 하나(본문 열 머리 줄 제목). 그 아래 제목 순서를 건너뛰지 않는다. 곁줄 묶음 제목은 `<h2>`
- 첫 Tab에 "본문으로 건너뛰기"(`sr-only focus:not-sr-only`, `#main`으로)
- 포커스: `focus-visible:ring-3 ring-ring/50`(지금 단추 방식)보다 또렷하게 `focus-visible:ring-2 ring-ring ring-offset-2 ring-offset-background`를 기준으로 한다. `outline-none`만 두고 대체 표시를 빼지 않는다(MUST NOT)
- 키보드: 모든 행동은 Tab·Enter·Space·Esc로 된다. 메뉴·탭·라디오 같은 묶음 안은 방향키(`components/ui`가 해 준다). 세로 뷰어의 키는 `015` "세로 뷰어"대로
- ARIA는 HTML로 안 될 때만: 토글 `aria-pressed`, 펼침 `aria-expanded`, 선택된 메뉴 `aria-current="page"`, 진행 중 `aria-busy`. `div role="button"`을 만들지 않는다
- 색만으로 뜻을 말하지 않는다: 상태 배지에 글자, 연결됨에 글자, 오류 칸에 글자
- 누르는 자리: 24×24px 이상. 작은 아이콘(16px)도 단추 자리는 32px. 좁은 화면 주 행동은 40px 이상
- 확대 200%, 폭 320px에서 가로 스크롤·잘림이 없다
- 움직임 줄이기: 5절
- 숏폼·영상: 소리 처음 꺼짐(`015`), 멈춤·이어짐을 키보드로. 설명 글은 영상 위 글자이면서 화면 읽기 도구가 읽는다
- 자동 접근성 검사: e2e에서 `@axe-core/playwright`로 주요 화면을 두 모드로 돌린다(심각·치명 0건)

## 13. AI 느낌 금지 목록

AI가 만든 듯한 흔한 화면을 만드는 것들이다. 아래는 하지 않는다(MUST NOT). 기계로 찾을 수 있는 것(표시 ◆)은 프론트 검사가 막는다(spec FR-020).

- ◆ 그라디언트: `bg-gradient-*`, `bg-linear-*`, `bg-radial-*`, `bg-conic-*`, `from-*`/`via-*`/`to-*`, 글자 그라디언트(`bg-clip-text text-transparent`)
- ◆ 유리 효과: `backdrop-blur-*`, `backdrop-*`, 반투명 흰 판 위 흐림
- ◆ 큰 흐린 그림자: 화면 코드의 `shadow-md` 이상, `shadow-[...]`, 색 그림자, 빛 번짐(glow). 떠 있는 층의 그림자는 `components/ui` 안에만 있다
- ◆ 이모지: 문구 파일과 화면 코드의 이모지(제목·단추·빈 결과·토스트 어디든)
- ◆ 정해진 세 벌 밖의 글꼴 이름(`Inter`, `Pretendard`, `Geist`, `Poppins` 등)
- 보라·파랑 위주의 색, 보라→분홍·파랑→보라 섞임. moa의 강조는 라임 하나다
- 모서리가 큰(16px 넘는) 둥근 카드에 흐린 그림자를 두고 넉넉한 여백으로 띄운 "카드 더미" 화면
- 반짝이(`Sparkles`) 같은 마법 아이콘, 로봇 아이콘, 꾸밈용 3D 그림·일러스트, 흐릿한 빛 덩어리 배경
- 가운데 큰 제목 + 작은 설명 + 단추 둘의 "랜딩 페이지 히어로"를 앱 화면에 두는 것
- 모든 덩어리를 같은 카드 틀로 찍어 낸 화면(목록은 줄, 덩어리는 선으로 나눈다)
- 숫자·아이콘·제목을 3개씩 나란히 둔 "기능 소개 칸"
- 장식 움직임: 떠다니기, 반짝임, 계속 도는 테두리, 튕기며 나타나기, 글자 한 자씩 나타나기
- "~해 보세요!", "멋진", "강력한", "손쉽게" 같은 광고 말투, 느낌표
- 회색 위 회색(대비 모자람)으로 "세련돼 보이게" 하는 흐린 글자
- 의미 없는 배지("NEW", "BETA", "AI")와 영문 대문자 눈썹 제목

## 14. 구현 메모 (plan에서 할 것)

토큰 방식은 그대로이고, 값과 토큰을 더한다. 이 절은 무엇을 바꾸는지만 적는다.

- `app/theme.css`: 4절 값으로 `:root`·`.dark`를 바꾸고 새 토큰(`signal`, `signal-foreground`, `signal-strong`, `rail`, `rail-foreground`, `success-strong`, `warning`, `warning-strong`, `info`, `info-strong`, `scrim`, `scrim-foreground`, `discord`, `discord-foreground`, `google`, `google-foreground`, `google-border`)을 더한다. 파일 머리 주석의 "shadcn/ui neutral 기본값"을 "moa 테마(020)"로 고친다
- `app/globals.css` `@theme inline`: 새 색 토큰의 `--color-*`, `--font-display`, `--font-mono`를 새 변수로, `--radius-group`, 글자 크기 `--text-meta`·`--text-body`·`--text-title-sm`·`--text-display`·`--text-hero`(각 `--line-height`와 함께)
- `app/layout-tokens.css`: 6.1절 배치 상수, 5절 움직임 토큰
- `app/layout.tsx`: 2절 글꼴
- `components/ui/button.tsx`: `signal` 종류를 더한다. 키트로 옮길 것으로 주석을 단다(지금 moa 주석 방식)
- 사용자 셸: `components/shell/user-shell.tsx`가 지금 `SidebarShell`을 쓴다. 레일·패널·곁줄·아래 탭 줄을 가진 사용자 셸을 따로 만들고 관리 셸은 `SidebarShell`을 그대로 쓴다. 계정 줄(`account-bar.tsx`)은 그대로 패널 맨 아래에 둔다
- `(public)/layout.tsx`, 로그인 화면: 6.4절 배치. 카드 걷어 내기, 소셜 단추를 폼 위로, Discord 단추 추가(동작은 `010`)
- 포커스 표시: 지금 `components/ui`의 `focus-visible:ring-3 ring-ring/50`은 반투명이라 밝게에서 3:1에 모자랄 수 있다. 12절 기준(`ring-2 ring-ring ring-offset-2`)으로 바꾼다
- 검사(`scripts/`): `check-colors.mjs`에 13절 ◆ 항목을 더하거나 `check-style.mjs`를 새로 둔다. `components/ui`는 그림자 검사에서 뺀다. `check-i18n.mjs`에 이모지 검사(`\p{Extended_Pictographic}`)를 더한다
- `public/brands/`: 제공자 공식 로고 파일
- 견본 파일(spec FR-021): 셸, 프로필, 로그인, 피드 항목, 상태 다섯. 어디에 둘지(예: `(dev)` 화면)는 plan에서 정한다

## 15. AI 도구에게 주는 규칙

아래를 팀원의 AI 도구(시스템 지시, 규칙 파일)에 그대로 넣는다. 이 문서와 다르면 이 문서가 맞고, 이 절을 고친다.

```text
너는 moa 프론트 화면을 만든다. specs/020-ui-foundation/design-system.md가 정본이다. 값을 지어내지 마라.

색
- 색은 테마 변수 클래스로만 쓴다: bg-background, text-foreground, bg-card, text-muted-foreground, border-border,
  border-input, bg-primary, bg-signal, text-signal-strong, bg-rail, bg-sidebar, text-destructive-strong,
  bg-success/10 + text-success-strong 등. 팔레트 색(red-500, zinc-100), bg-white/black, hex, rgb()를 쓰지 마라.
- 시그널(bg-signal, 라임)은 한 영역에 단추 하나와 안 읽은 수·선택 표시에만. 주 행동은 bg-primary(잉크).
- 의미 색 글자는 늘 -strong. 빨강은 오류와 지우기·나가기에만. 안 읽은 수·좋아요는 시그널.
- 입력칸·체크 상자 테두리는 border-input. border-border는 꾸밈 선만.

글꼴·글자
- font-sans(IBM Plex Sans KR), font-display(Chakra Petch, 영문·숫자만), font-mono(IBM Plex Mono)만. 글꼴 이름을 코드에 쓰지 마라.
- 굵기는 font-normal, font-medium, font-bold만. font-semibold 금지.
- 크기는 text-xs, text-meta, text-sm, text-body, text-base, text-title-sm, text-xl, text-2xl, text-display만.
  12px보다 작게 쓰지 마라. 피드 글은 text-body.
- 한글 제목·단추는 break-keep, 닉네임·주소는 wrap-anywhere. 바뀌는 수는 tabular-nums. 큰 수는 1.2만 꼴.

모양
- 모서리: 단추·입력 rounded-lg, 카드 rounded-xl, 창 rounded-2xl, 사람 아바타 rounded-full, 모임 아바타 rounded-group.
  rounded-3xl 이상 금지. 피드 항목은 모서리 없음.
- 그림자 금지(카드, 피드, 단추). 떠 있는 층은 components/ui의 것을 그대로 쓴다. shadow-* 를 화면 코드에 쓰지 마라.
- 그라디언트, backdrop-blur, glow, 이모지, Sparkles·Wand·Bot 아이콘, 일러스트, 장식 움직임 금지.
- 덩어리는 1px 선(border-b, divide-y)과 바탕 단계(rail < sidebar < background < card < popover)로 나눈다.

배치
- 사용자 화면은 사용자 셸 안: 레일 72px | 패널 240px | 본문 열(max-w-(--feed-max-width)) | 곁줄 320px.
  1280 이상 넷, 1024~1279 곁줄 없음, 768~1023 레일+본문(패널은 겹쳐 열기), 767 이하 머리 줄+아래 탭 줄+서랍.
- 프로필·탐색·모집 격자는 max-w-(--wide-max-width), 곁줄 없음. 격자는 grid-cols-3 gap-0.5, 칸 aspect-[3/4].
- 본문 열 머리 줄: h-(--header-height) sticky top-0 bg-background border-b, 제목이 h1. 흐림 금지.
- 피드 항목: article, border-b, px-4 py-3, 아바타 40px 원, 카드로 감싸지 마라.
- 가로 스크롤 금지. 크기는 배치 상수(--page-padding 등)를 쓴다.

컴포넌트
- components/ui, components/kit에 있는 것을 쓴다. 같은 것을 새로 만들지 마라.
- 단추 종류: default, signal, outline, secondary, ghost, destructive, link. 진행 중은 disabled + aria-busy + Spinner.
- 폼: zod 스키마 → react-hook-form → Field/FieldLabel/Input/FieldError. 이름은 늘 보이게, 선택 칸은 "(선택)",
  오류는 칸 아래 aria-describedby로, 오류에도 값을 지우지 마라. 떠날 때 저장 안 한 변경은 use-unsaved-changes.
- 확인 창: kit/confirm-dialog. 제목은 질문, 단추는 그 행동("나가기"). "확인" 단추 금지.
- 상태: 처음 불러오기 = 같은 모양 Skeleton, 다시 불러오기 = 있던 내용 유지, 빈 결과 = 아이콘+한 줄+단추 하나(그림·이모지 금지),
  오류 = errors.<코드> 문구 + 다시 시도 + 문의 번호(kit/error-view). 가운데 Spinner만 두지 마라.
- 소셜 단추: Discord 다음 Google, h-10 폭 가득, 로고 20px 왼쪽 같은 자리, bg-discord / bg-google + border-google-border,
  로고는 public/brands/의 공식 파일을 그대로. 제공자 이름은 영문.

접근성
- nav(레일·패널·탭 줄, 이름 다르게), main#main, aside, header. 화면마다 h1 하나. 첫 Tab에 "본문으로 건너뛰기".
- 이동은 Link, 동작은 button. div onClick, div role="button" 금지.
- 포커스 표시를 지우지 마라. 아이콘만 있는 단추는 aria-label + Tooltip.
- 토글 aria-pressed, 펼침 aria-expanded, 현재 메뉴 aria-current="page". 색만으로 상태를 말하지 마라.
- 누르는 자리 24px 이상, 좁은 화면 주 행동 40px. motion-reduce에서 미끄러짐·커짐을 꺼라. 240ms 넘는 움직임 금지.

문구·주소·메타
- 화면에 한글을 쓰지 마라. 모든 글자(aria-label, alt, 툴팁, 제목 포함)는 messages/ko.json 키 + t("키").
- 해요체, 짧게, 느낌표·이모지·물결표 금지. 단추는 하는 일. 브랜드는 Discord, Google, moa.
- 탭·거르기·검색어·쪽은 주소 검색 값. 주소가 있는 겹침 화면은 뒤로 가기로 닫힌다.
- 화면마다 metadata title. 공개 화면은 description·openGraph, 로그인해야 보는 화면은 robots index false.
- 그림은 next/image + 실제 폭 sizes + 비율 자리. 첫 큰 그림만 priority.

끝나면 npm run check를 통과해야 한다(포맷, lint, 타입, 색 검사, 문구 검사, 단위 테스트).
```
