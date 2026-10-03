---
status: approved
refs: [NFR-ENV-001]
---

# 공통 컴포넌트

<!--
- 첫 열(컴포넌트)이 목업의 data-component 값이다. crosscheck.sh가 이 열을 읽는다
- 분류: 누르기, 입력하기, 고르기, 보여주기, 알려주기, 나누기, 셸. 새 컴포넌트는 이 중 하나에 넣는다
- 셸 분류는 화면 본문이 아니라 셸 틀(web/mockup/shells/)이 그린다. 목업 본문에 직접 넣지 않는다
- 목업은 이 표에 있는 컴포넌트만 쓴다. 새 컴포넌트는 여기에 먼저 추가한다
- 코드 경로는 Next 기준이다. 추가 명령: npx shadcn@latest add <shadcn 이름>
- 모양은 web/mockup/snippets.md의 조각을 쓴다. 조각이 없는 변형은 키트의 playground/README.md 절차로 뽑아 추가한다
-->

| 컴포넌트 | 분류 | shadcn 이름 | 코드 경로 | 용도 | 변형 | 상태 | 쓰지 말아야 할 경우 |
|---|---|---|---|---|---|---|---|
| Button | 누르기 | button | `components/ui/button.tsx` | 행동 실행 | default, destructive, outline, secondary, ghost, link / xs, sm, lg, icon, icon-xs, icon-sm, icon-lg | 기본, 비활성 | 페이지 이동만 할 때 (링크) |
| Input | 입력하기 | input | `components/ui/input.tsx` | 한 줄 입력 | | 기본, 비활성, 오류 | |
| Textarea | 입력하기 | textarea | `components/ui/textarea.tsx` | 여러 줄 입력 | | 기본, 비활성, 오류 | |
| Label | 입력하기 | label | `components/ui/label.tsx` | 입력 이름표 | | | |
| Select | 고르기 | select | `components/ui/select.tsx` | 목록에서 하나 선택 | | 기본, 비활성, 오류 | 선택지가 3개 이하일 때 (RadioGroup) |
| RadioGroup | 고르기 | radio-group | `components/ui/radio-group.tsx` | 적은 선택지 중 하나 | | 기본, 비활성 | |
| Checkbox | 고르기 | checkbox | `components/ui/checkbox.tsx` | 켜기·끄기, 여러 개 선택 | | 기본, 비활성 | |
| Card | 보여주기 | card | `components/ui/card.tsx` | 내용 묶음 | default, sm (크기) | | |
| Badge | 보여주기 | badge | `components/ui/badge.tsx` | 상태·분류 표시 | default, secondary, destructive, outline, ghost, link | | 누를 수 있는 요소 (Button) |
| Alert | 알려주기 | alert | `components/ui/alert.tsx` | 안내·오류 메시지 | default, destructive | | 잠깐 떴다 사라지는 알림 (Sonner) |
| Table | 보여주기 | table | `components/ui/table.tsx` | 목록 데이터 | | | |
| Tabs | 나누기 | tabs | `components/ui/tabs.tsx` | 같은 화면 안의 보기 전환 | default, line | 선택, 비선택 | 다른 화면으로 이동 (링크) |
| Dialog | 알려주기 | dialog | `components/ui/dialog.tsx` | 확인, 짧은 입력 | | 열림 | 긴 입력 (별도 화면) |
| Skeleton | 알려주기 | skeleton | `components/ui/skeleton.tsx` | 로딩 자리 표시 | | | |
| Separator | 나누기 | separator | `components/ui/separator.tsx` | 구역 나눔 | | | |
| Sonner | 알려주기 | sonner | `components/ui/sonner.tsx` | 잠깐 떴다 사라지는 알림 | | | 사용자가 읽어야 하는 중요한 오류 (Alert) |
| Avatar | 보여주기 | avatar | `components/ui/avatar.tsx` | 회원 프로필 사진 (없으면 닉네임 첫 글자) | 크기 sm, default, lg | 사진, 대체 글자 | 회원이 아닌 대상 (게임 등) |
| MobileTabBar | 셸 | (없음. 앱 컴포넌트) | `components/mobile-tab-bar.tsx` | 모바일 하단 탭 바와 헤더 아이콘 | | 지금 화면 탭 강조 | 데스크톱 폭 (사이드바가 메뉴를 맡는다) |
| Sidebar | 셸 | sidebar | `components/ui/sidebar.tsx` | 셸 사이드바형의 메뉴 틀 | 위치 left, right / 접기 icon, offcanvas, none / 생김새 sidebar, floating, inset | 펼침, 접힘 | 화면 안에서 보기를 바꿀 때 (Tabs). 사이드바를 쓰지 않는 셸(상단 메뉴형) |

- Avatar 〔2026-10-01〕 SNS라 프로필 사진이 거의 모든 목록에 나온다. 키트에 조각이 없어 `frontend/components/ui/avatar.tsx`의 클래스로 그렸다. 키트에 조각 추가를 요청한다
- MobileTabBar 〔2026-10-02〕 하단 탭 바 사용자 결정. shadcn에 해당 컴포넌트가 없어 키트 playground `shells.tsx`의 `MobileTabBar`·`MobileActions`를 따라 테마 변수 클래스로 만든다. 셸이 그리므로 목업 본문에 넣지 않는다
