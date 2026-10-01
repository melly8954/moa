// psw 셸 틀: 사이드바형
// 목업 템플릿(screen.html)이 <script src="…/shell.js" defer>로 불러와, 화면 본문(<main>)을 셸 안에 넣는다
// 프로젝트는 이 파일을 docs/design/ui/kit/shell.js로 복사하고 아래 설정 구역(SHELL, MENU)만 고친다
// 설정 구역 밖은 shadcn/ui Sidebar가 실제로 그린 HTML이다. 고치지 않는다 (키트 playground /shells에서 검증)

// @psw-shell-config-start
const SHELL = {
  name: "moa", // 사이드바 위쪽에 보일 이름
  user: "롤하는곰", // 사이드바 아래쪽 사용자 이름
  side: "left", // left | right
  collapsible: "icon", // icon(아이콘만 남김) | offcanvas(완전히 숨김) | none(접지 않음)
  variant: "sidebar", // sidebar(벽에 붙음) | floating(떠 있는 카드) | inset(본문을 안쪽으로 감쌈)
  open: true, // 처음 상태: true 펼침, false 접힘
}

// 메뉴: docs/design/ui/ia/README.md의 메뉴 계층과 같게 적는다
//   label: 메뉴 이름 / screen: 화면 ID (ia/<도메인>.md 화면 목록) / icon: 아래 ICONS의 이름 / children: 하위 메뉴 (1단까지)
const MENU = [
  { label: "홈", icon: "house", screen: "SCR-FEED-001" },
  { label: "게임별 피드", icon: "gamepad-2", screen: "SCR-FEED-002" },
  { label: "검색", icon: "search", screen: "SCR-MEM-011" },
  { label: "메시지", icon: "message-circle", screen: "SCR-MSG-001" },
  { label: "파티", icon: "users", screen: "SCR-PTY-001" },
  { label: "알림", icon: "bell", screen: "SCR-NTF-001" },
  {
    label: "친구",
    icon: "user-plus",
    children: [
      { label: "친구 목록", screen: "SCR-REL-003" },
      { label: "팔로우 요청", screen: "SCR-REL-002" },
    ],
  },
  { label: "프로필", icon: "user", screen: "SCR-MEM-009" },
  { label: "설정", icon: "settings", screen: "SCR-MEM-012" },
  {
    label: "관리",
    icon: "shield",
    children: [
      { label: "신고", screen: "SCR-ADM-001" },
      { label: "회원", screen: "SCR-ADM-003" },
      { label: "게임", screen: "SCR-ADM-006" },
    ],
  },
]
// @psw-shell-config-end

// lucide 아이콘. 더 필요하면 키트 playground에서 node scripts/icons.mjs <이름>으로 뽑아 더한다
const ICONS = {
  "house": '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-house lucide-home" aria-hidden="true"><path d="M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"></path><path d="M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path></svg>',
  "layout-dashboard": '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-layout-dashboard" aria-hidden="true"><rect width="7" height="9" x="3" y="3" rx="1"></rect><rect width="7" height="5" x="14" y="3" rx="1"></rect><rect width="7" height="9" x="14" y="12" rx="1"></rect><rect width="7" height="5" x="3" y="16" rx="1"></rect></svg>',
  "list": '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-list" aria-hidden="true"><path d="M3 5h.01"></path><path d="M3 12h.01"></path><path d="M3 19h.01"></path><path d="M8 5h13"></path><path d="M8 12h13"></path><path d="M8 19h13"></path></svg>',
  "file-text": '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-file-text" aria-hidden="true"><path d="M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z"></path><path d="M14 2v5a1 1 0 0 0 1 1h5"></path><path d="M10 9H8"></path><path d="M16 13H8"></path><path d="M16 17H8"></path></svg>',
  "folder": '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-folder" aria-hidden="true"><path d="M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z"></path></svg>',
  "shopping-cart": '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-shopping-cart" aria-hidden="true"><path d="m2.05 2.05 1.099-.028a1 1 0 0 1 1.008.815l2.69 14.347A1 1 0 0 0 7.83 18H18"></path><path d="M4.563 5h16.435a1 1 0 0 1 .981 1.204l-1.026 6.226A2 2 0 0 1 18.962 14H6.25"></path><circle cx="18" cy="20" r="2"></circle><circle cx="8" cy="20" r="2"></circle></svg>',
  "package": '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-package" aria-hidden="true"><path d="M11 21.73a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73z"></path><path d="M12 22V12"></path><polyline points="3.29 7 12 12 20.71 7"></polyline><path d="m7.5 4.27 9 5.15"></path></svg>',
  "users": '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-users" aria-hidden="true"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"></path><path d="M16 3.128a4 4 0 0 1 0 7.744"></path><path d="M22 21v-2a4 4 0 0 0-3-3.87"></path><circle cx="9" cy="7" r="4"></circle></svg>',
  "user": '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-user" aria-hidden="true"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>',
  "calendar": '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-calendar" aria-hidden="true"><path d="M8 2v3"></path><path d="M16 2v3"></path><rect x="3" y="3" width="18" height="18" rx="2"></rect><path d="M3 9h18"></path></svg>',
  "mail": '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-mail" aria-hidden="true"><path d="m22 7-8.991 5.727a2 2 0 0 1-2.009 0L2 7"></path><rect x="2" y="4" width="20" height="16" rx="2"></rect></svg>',
  "bell": '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-bell" aria-hidden="true"><path d="M10.268 21a2 2 0 0 0 3.464 0"></path><path d="M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326"></path></svg>',
  "chart-column": '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-chart-column lucide-bar-chart-3" aria-hidden="true"><path d="M3 3v16a2 2 0 0 0 2 2h16"></path><path d="M18 17V9"></path><path d="M13 17V5"></path><path d="M8 17v-3"></path></svg>',
  "shield": '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-shield" aria-hidden="true"><path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z"></path></svg>',
  "settings": '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-settings" aria-hidden="true"><path d="M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915"></path><circle cx="12" cy="12" r="3"></circle></svg>',
  "circle": '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-circle" aria-hidden="true"><circle cx="12" cy="12" r="10"></circle></svg>',
  "panel-left": '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-panel-left lucide-sidebar" aria-hidden="true"><rect width="18" height="18" x="3" y="3" rx="2"></rect><path d="M9 3v18"></path></svg>',
  "gamepad-2": '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-gamepad-2" aria-hidden="true"><line x1="6" x2="10" y1="11" y2="11"></line><line x1="8" x2="8" y1="9" y2="13"></line><line x1="15" x2="15.01" y1="12" y2="12"></line><line x1="18" x2="18.01" y1="10" y2="10"></line><path d="M17.32 5H6.68a4 4 0 0 0-3.978 3.59c-.006.052-.01.101-.017.152C2.604 9.416 2 14.456 2 16a3 3 0 0 0 3 3c1 0 1.5-.5 2-1l1.414-1.414A2 2 0 0 1 9.828 16h4.344a2 2 0 0 1 1.414.586L17 18c.5.5 1 1 2 1a3 3 0 0 0 3-3c0-1.545-.604-6.584-.685-7.258-.007-.05-.011-.1-.017-.151A4 4 0 0 0 17.32 5z"></path></svg>',
  "search": '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-search" aria-hidden="true"><path d="m21 21-4.34-4.34"></path><circle cx="11" cy="11" r="8"></circle></svg>',
  "message-circle": '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-message-circle" aria-hidden="true"><path d="M2.992 16.342a2 2 0 0 1 .094 1.167l-1.065 3.29a1 1 0 0 0 1.236 1.168l3.413-.998a2 2 0 0 1 1.099.092 10 10 0 1 0-4.777-4.719"></path></svg>',
  "user-plus": '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-user-plus" aria-hidden="true"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><line x1="19" x2="19" y1="8" y2="14"></line><line x1="22" x2="16" y1="11" y2="11"></line></svg>',
}

;(() => {
  // shadcn/ui Sidebar(base-nova)가 그린 클래스
  const C = {
    wrapper: "group/sidebar-wrapper flex min-h-svh w-full has-data-[variant=inset]:bg-sidebar",
    sidebar: "group peer hidden text-sidebar-foreground md:block",
    sidebarNone: "flex h-full w-(--sidebar-width) flex-col bg-sidebar text-sidebar-foreground",
    gap: "relative w-(--sidebar-width) bg-transparent transition-[width] duration-200 ease-linear group-data-[collapsible=offcanvas]:w-0 group-data-[side=right]:rotate-180",
    gapSidebar: "group-data-[collapsible=icon]:w-(--sidebar-width-icon)",
    gapFloating: "group-data-[collapsible=icon]:w-[calc(var(--sidebar-width-icon)+(--spacing(4)))]",
    container: "fixed inset-y-0 z-10 hidden h-svh w-(--sidebar-width) transition-[left,right,width] duration-200 ease-linear data-[side=left]:left-0 data-[side=left]:group-data-[collapsible=offcanvas]:left-[calc(var(--sidebar-width)*-1)] data-[side=right]:right-0 data-[side=right]:group-data-[collapsible=offcanvas]:right-[calc(var(--sidebar-width)*-1)] md:flex",
    containerSidebar: "group-data-[collapsible=icon]:w-(--sidebar-width-icon) group-data-[side=left]:border-r group-data-[side=right]:border-l",
    containerFloating: "p-2 group-data-[collapsible=icon]:w-[calc(var(--sidebar-width-icon)+(--spacing(4))+2px)]",
    inner: "flex size-full flex-col bg-sidebar group-data-[variant=floating]:rounded-lg group-data-[variant=floating]:shadow-sm group-data-[variant=floating]:ring-1 group-data-[variant=floating]:ring-sidebar-border",
    section: "flex flex-col gap-2 p-2",
    content: "no-scrollbar flex min-h-0 flex-1 flex-col gap-0 overflow-auto group-data-[collapsible=icon]:overflow-hidden",
    group: "relative flex w-full min-w-0 flex-col p-2",
    groupLabel: "flex h-8 shrink-0 items-center rounded-md px-2 text-xs font-medium text-sidebar-foreground/70 ring-sidebar-ring outline-hidden transition-[margin,opacity] duration-200 ease-linear group-data-[collapsible=icon]:-mt-8 group-data-[collapsible=icon]:opacity-0 focus-visible:ring-2 [&>svg]:size-4 [&>svg]:shrink-0",
    menu: "flex w-full min-w-0 flex-col gap-0",
    menuItem: "group/menu-item relative",
    menuButton: "peer/menu-button group/menu-button flex w-full items-center gap-2 overflow-hidden rounded-md p-2 text-left ring-sidebar-ring outline-hidden transition-[width,height,padding] group-has-data-[sidebar=menu-action]/menu-item:pr-8 group-data-[collapsible=icon]:size-8! group-data-[collapsible=icon]:p-2! focus-visible:ring-2 active:bg-sidebar-accent active:text-sidebar-accent-foreground disabled:pointer-events-none disabled:opacity-50 aria-disabled:pointer-events-none aria-disabled:opacity-50 data-open:hover:bg-sidebar-accent data-open:hover:text-sidebar-accent-foreground data-active:bg-sidebar-accent data-active:font-medium data-active:text-sidebar-accent-foreground [&_svg]:size-4 [&_svg]:shrink-0 [&>span:last-child]:truncate hover:bg-sidebar-accent hover:text-sidebar-accent-foreground h-8 text-sm",
    menuButtonLg: "peer/menu-button group/menu-button flex w-full items-center gap-2 overflow-hidden rounded-md p-2 text-left ring-sidebar-ring outline-hidden transition-[width,height,padding] group-has-data-[sidebar=menu-action]/menu-item:pr-8 group-data-[collapsible=icon]:size-8! focus-visible:ring-2 active:bg-sidebar-accent active:text-sidebar-accent-foreground disabled:pointer-events-none disabled:opacity-50 aria-disabled:pointer-events-none aria-disabled:opacity-50 data-open:hover:bg-sidebar-accent data-open:hover:text-sidebar-accent-foreground data-active:bg-sidebar-accent data-active:font-medium data-active:text-sidebar-accent-foreground [&_svg]:size-4 [&_svg]:shrink-0 [&>span:last-child]:truncate hover:bg-sidebar-accent hover:text-sidebar-accent-foreground h-12 text-sm group-data-[collapsible=icon]:p-0!",
    logo: "flex aspect-square size-8 items-center justify-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground",
    menuSub: "mx-3.5 flex min-w-0 translate-x-px flex-col gap-1 border-l border-sidebar-border px-2.5 py-0.5 group-data-[collapsible=icon]:hidden",
    menuSubItem: "group/menu-sub-item relative",
    menuSubButton: "flex h-7 min-w-0 -translate-x-px items-center gap-2 overflow-hidden rounded-md px-2 text-sidebar-foreground ring-sidebar-ring outline-hidden group-data-[collapsible=icon]:hidden hover:bg-sidebar-accent hover:text-sidebar-accent-foreground focus-visible:ring-2 active:bg-sidebar-accent active:text-sidebar-accent-foreground disabled:pointer-events-none disabled:opacity-50 aria-disabled:pointer-events-none aria-disabled:opacity-50 data-[size=md]:text-sm data-[size=sm]:text-xs data-active:bg-sidebar-accent data-active:text-sidebar-accent-foreground [&>span:last-child]:truncate [&>svg]:size-4 [&>svg]:shrink-0 [&>svg]:text-sidebar-accent-foreground",
    rail: "absolute inset-y-0 z-20 hidden w-4 transition-all ease-linear group-data-[side=left]:-right-4 group-data-[side=right]:left-0 after:absolute after:inset-y-0 after:start-1/2 after:w-[2px] hover:after:bg-sidebar-border sm:flex ltr:-translate-x-1/2 rtl:-translate-x-1/2 in-data-[side=left]:cursor-w-resize in-data-[side=right]:cursor-e-resize [[data-side=left][data-state=collapsed]_&]:cursor-e-resize [[data-side=right][data-state=collapsed]_&]:cursor-w-resize group-data-[collapsible=offcanvas]:translate-x-0 group-data-[collapsible=offcanvas]:after:left-full hover:group-data-[collapsible=offcanvas]:bg-sidebar [[data-side=left][data-collapsible=offcanvas]_&]:-right-2 [[data-side=right][data-collapsible=offcanvas]_&]:-left-2",
    inset: "relative flex w-full flex-1 flex-col bg-background md:peer-data-[variant=inset]:m-2 md:peer-data-[variant=inset]:ml-0 md:peer-data-[variant=inset]:rounded-xl md:peer-data-[variant=inset]:shadow-sm md:peer-data-[variant=inset]:peer-data-[state=collapsed]:ml-2",
    header: "flex h-12 shrink-0 items-center gap-2 border-b px-4",
    trigger: "group/button inline-flex shrink-0 items-center justify-center border border-transparent bg-clip-padding text-sm font-medium whitespace-nowrap transition-all outline-none select-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 active:not-aria-[haspopup]:translate-y-px disabled:pointer-events-none disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-3 aria-invalid:ring-destructive/20 dark:aria-invalid:border-destructive/50 dark:aria-invalid:ring-destructive/40 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4 hover:bg-muted hover:text-foreground aria-expanded:bg-muted aria-expanded:text-foreground dark:hover:bg-muted/50 size-7 rounded-[min(var(--radius-md),12px)] in-data-[slot=button-group]:rounded-lg -ml-1",
    separator: "shrink-0 bg-border data-horizontal:h-px data-horizontal:w-full data-vertical:w-px data-vertical:self-stretch mr-2 h-4",
    page: "flex flex-1 flex-col gap-4 p-4",
  }

  const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c])
  const icon = (name) => (name && ICONS[name]) || ""

  // 지금 화면: 문서 첫 줄 메타 주석의 id
  const screen = (() => {
    for (const n of document.childNodes) {
      const m = n.nodeType === Node.COMMENT_NODE && n.data.match(/id:\s*(SCR-[A-Za-z0-9]+-\d+)/)
      if (m) return m[1]
    }
    return ""
  })()
  const title = (() => {
    for (const item of MENU) {
      if (item.screen === screen) return item.label
      const child = (item.children || []).find((c) => c.screen === screen)
      if (child) return `${item.label} / ${child.label}`
    }
    return ""
  })()
  const active = (item) => (item.screen && item.screen === screen ? ' data-active=""' : "")

  const menu = (items) => `<ul data-slot="sidebar-menu" data-sidebar="menu" class="${C.menu}">${items}</ul>`
  const subMenu = (items) =>
    `<ul data-slot="sidebar-menu-sub" data-sidebar="menu-sub" class="${C.menuSub}">${items
      .map(
        (c) =>
          `<li data-slot="sidebar-menu-sub-item" data-sidebar="menu-sub-item" class="${C.menuSubItem}"><a data-slot="sidebar-menu-sub-button" data-sidebar="menu-sub-button" data-size="md"${active(c)} class="${C.menuSubButton}"><span>${esc(c.label)}</span></a></li>`,
      )
      .join("")}</ul>`
  const menuItem = (item) =>
    `<li data-slot="sidebar-menu-item" data-sidebar="menu-item" class="${C.menuItem}"><button type="button" data-slot="sidebar-menu-button" data-sidebar="menu-button" data-size="default"${active(item)} title="${esc(item.label)}" class="${C.menuButton}">${icon(item.icon)}<span>${esc(item.label)}</span></button>${item.children ? subMenu(item.children) : ""}</li>`

  const inner =
    `<div data-slot="sidebar-header" data-sidebar="header" class="${C.section}">${menu(
      `<li data-slot="sidebar-menu-item" data-sidebar="menu-item" class="${C.menuItem}"><button type="button" data-slot="sidebar-menu-button" data-sidebar="menu-button" data-size="lg" class="${C.menuButtonLg}"><div class="${C.logo}">${esc(SHELL.name.slice(0, 1))}</div><span class="truncate font-medium">${esc(SHELL.name)}</span></button></li>`,
    )}</div>` +
    `<div data-slot="sidebar-content" data-sidebar="content" class="${C.content}"><div data-slot="sidebar-group" data-sidebar="group" class="${C.group}"><div data-slot="sidebar-group-label" data-sidebar="group-label" class="${C.groupLabel}">메뉴</div>${menu(MENU.map(menuItem).join(""))}</div></div>` +
    `<div data-slot="sidebar-footer" data-sidebar="footer" class="${C.section}">${menu(menuItem({ label: SHELL.user, icon: "user" }))}</div>` +
    `<button data-sidebar="rail" data-slot="sidebar-rail" aria-label="Toggle Sidebar" tabindex="-1" title="Toggle Sidebar" class="${C.rail}"></button>`

  const boxed = SHELL.variant === "floating" || SHELL.variant === "inset"
  const collapsed = !SHELL.open && SHELL.collapsible !== "none"
  const sidebar =
    SHELL.collapsible === "none"
      ? `<div data-slot="sidebar" data-component="Sidebar" class="${C.sidebarNone}">${inner}</div>`
      : `<div class="${C.sidebar}" data-state="${collapsed ? "collapsed" : "expanded"}" data-collapsible="${collapsed ? SHELL.collapsible : ""}" data-variant="${SHELL.variant}" data-side="${SHELL.side}" data-slot="sidebar" data-component="Sidebar">` +
        `<div data-slot="sidebar-gap" class="${C.gap} ${boxed ? C.gapFloating : C.gapSidebar}"></div>` +
        `<div data-slot="sidebar-container" data-side="${SHELL.side}" class="${C.container} ${boxed ? C.containerFloating : C.containerSidebar}"><div data-sidebar="sidebar" data-slot="sidebar-inner" class="${C.inner}">${inner}</div></div></div>`

  const inset =
    `<main data-slot="sidebar-inset" class="${C.inset}"><header data-shell="header" class="${C.header}">` +
    `<button type="button" data-slot="sidebar-trigger" data-sidebar="trigger" class="${C.trigger}">${icon("panel-left")}<span class="sr-only">Toggle Sidebar</span></button>` +
    `<div data-orientation="vertical" role="separator" data-slot="separator" class="${C.separator}"></div>` +
    `<span class="text-sm font-medium">${esc(title)}</span></header><div data-shell="page" class="${C.page}"></div></main>`

  // 화면 본문(<body> 바로 아래 <main>)을 셸의 본문 자리로 옮긴다
  const page = document.querySelector("body > main")
  const holder = document.createElement("div")
  holder.innerHTML = `<div data-slot="sidebar-wrapper" style="--sidebar-width:16rem;--sidebar-width-icon:3rem" class="${C.wrapper}">${sidebar}${inset}</div>`
  const shell = holder.firstElementChild
  if (page) {
    shell.querySelector('[data-shell="page"]').append(...page.childNodes)
    page.replaceWith(shell)
  } else {
    document.body.prepend(shell)
  }

  // 접기·펼치기: 트리거, 레일, 목업 확인 막대의 버튼
  const el = shell.querySelector('[data-slot="sidebar"]')
  let barButton = null
  const setCollapsed = (on) => {
    if (SHELL.collapsible === "none") return
    el.dataset.state = on ? "collapsed" : "expanded"
    el.dataset.collapsible = on ? SHELL.collapsible : ""
    if (barButton) barButton.setAttribute("aria-pressed", String(on))
  }
  const toggle = () => setCollapsed(el.dataset.state !== "collapsed")
  shell.querySelectorAll('[data-sidebar="trigger"], [data-sidebar="rail"]').forEach((b) => b.addEventListener("click", toggle))
  const bar = document.querySelector(".mock-bar")
  if (bar && SHELL.collapsible !== "none") {
    barButton = document.createElement("button")
    barButton.type = "button"
    barButton.textContent = "사이드바 접기"
    barButton.setAttribute("aria-pressed", String(collapsed))
    barButton.addEventListener("click", toggle)
    bar.append(barButton)
  }
})()
