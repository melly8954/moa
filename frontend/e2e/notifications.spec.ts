import { expect, type Page, test } from "@playwright/test"
import { signedIn, signedOut } from "./support/api"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 알림 (specs/007-notifications) 화면 쪽 AC. 백엔드 대신 API를 가로채고, 실시간 연결(STOMP over WebSocket)은
// page.routeWebSocket으로 흉내 낸다. 권한·전달 대상은 백엔드 NotificationsAcceptanceTest가 확인한다

type Stomp = {
  connected: () => boolean
  // 구독 경로에 메시지를 보낸다 (구독할 때까지 기다린다)
  send: (destination: string, body: unknown) => Promise<void>
}

async function mockStomp(page: Page): Promise<Stomp> {
  let isConnected = false
  const subscriptions = new Map<string, string>()
  let socket: { send: (message: string) => void } | undefined
  let messageId = 0
  await page.routeWebSocket("**/ws", (ws) => {
    socket = ws
    ws.onMessage((raw) => {
      const frame = String(raw)
      const command = frame.split("\n")[0]
      if (command === "CONNECT" || command === "STOMP") {
        isConnected = true
        ws.send("CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\0")
      } else if (command === "SUBSCRIBE") {
        const header = (name: string) =>
          frame
            .split("\n")
            .find((line) => line.startsWith(`${name}:`))
            ?.slice(name.length + 1) ?? ""
        subscriptions.set(header("destination"), header("id"))
      }
    })
  })
  return {
    connected: () => isConnected,
    send: async (destination, body) => {
      await expect.poll(() => subscriptions.has(destination)).toBe(true)
      messageId += 1
      socket?.send(
        `MESSAGE\ndestination:${destination}\nsubscription:${subscriptions.get(destination)}\nmessage-id:${messageId}\ncontent-type:application/json\n\n${JSON.stringify(body)}\0`,
      )
    },
  }
}

// 로그인한 화면이 부르는 알림·공지 API
async function mockSignedInApis(
  page: Page,
  { unread = () => 0, hasNew = () => false }: { unread?: () => number; hasNew?: () => boolean } = {},
) {
  await signedIn(page)
  await page.route("**/api/v1/menus/me", (route) => route.fulfill({ json: [] }))
  await page.route("**/api/v1/notices/new", (route) => route.fulfill({ json: { hasNew: hasNew() } }))
  await page.route("**/api/v1/notifications/unread-count", (route) => route.fulfill({ json: { count: unread() } }))
  await page.route("**/api/v1/notifications?*", (route) =>
    route.fulfill({ json: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 } }),
  )
  await page.route("**/api/v1/notifications/push/public-key", (route) =>
    route.fulfill({
      json: { publicKey: "BEl62iUYgUivxIkv69yViEuiBIa-Ib9-SkvMeAtA3LFgDzkrxZJjSgSnfckjBJuBkr3qBUYIHBQFLXYp5Nksh8U" },
    }),
  )
}

test("US1-AC1·AC4 공지가 게시되면 토스트와 공지 점이 생기고, 토스트의 보기를 누르면 공지 상세로 간다", async ({
  page,
  viewport,
}) => {
  let published = false
  const stomp = await mockStomp(page)
  await mockSignedInApis(page, { hasNew: () => published })
  await page.route("**/api/v1/notices/5", (route) =>
    route.fulfill({
      json: {
        id: 5,
        title: "서버 점검 안내",
        content: "<p>본문</p>",
        pinned: false,
        publishedAt: "2026-11-15T09:00:00+09:00",
      },
    }),
  )
  await page.goto("/")

  published = true
  await stomp.send("/topic/notices", { type: "NOTICE_PUBLISHED", id: 5, title: "서버 점검 안내", link: "/notices/5" })

  await expect(page.getByText("새 공지: 서버 점검 안내")).toBeVisible()
  if ((viewport?.width ?? 0) >= 768) {
    await expect(page.getByRole("link", { name: /^공지\s*,\s*새 공지$/ })).toBeVisible()
  }
  await page.getByRole("button", { name: "보기" }).click()
  await expect(page).toHaveURL(/\/notices\/5$/)
  await expect(page.getByRole("heading", { level: 1, name: "서버 점검 안내" })).toBeVisible()
})

test("US1-AC3 로그인하지 않은 화면은 실시간 연결을 하지 않는다", async ({ page }) => {
  const stomp = await mockStomp(page)
  await signedOut(page)
  await page.route("**/api/v1/notices?*", (route) =>
    route.fulfill({ json: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 } }),
  )
  await page.goto("/notices")
  await expect(page.getByRole("link", { name: "로그인" })).toBeVisible()
  await page.waitForTimeout(500)
  expect(stomp.connected()).toBe(false)
})

test("US3-AC1 새 개인 알림이 오면 토스트가 뜨고 벨 숫자가 하나 는다", async ({ page }) => {
  let count = 0
  const stomp = await mockStomp(page)
  await mockSignedInApis(page, { unread: () => count })
  await page.goto("/")
  await expect(page.getByRole("button", { name: "알림", exact: true }).first()).toBeVisible()

  count = 1
  await stomp.send("/user/queue/notifications", {
    type: "NOTIFICATION_CREATED",
    id: 9,
    notificationType: "SYSTEM",
    title: "새 알림이 왔어요",
    link: "/notifications",
    createdAt: "2026-11-15T09:00:00+09:00",
  })

  await expect(page.getByText("새 알림이 왔어요")).toBeVisible()
  await expect(page.getByRole("button", { name: "알림, 안 읽은 알림 1개" }).first()).toBeVisible()
})

test("US4-AC4 브라우저 알림 권한을 거부했으면 꺼져 있고 브라우저 설정 안내가 보인다 (알림 화면 가로 넘침·접근성)", async ({
  page,
}) => {
  await page.addInitScript(() => {
    Object.defineProperty(window, "Notification", {
      configurable: true,
      value: Object.assign(function Notification() {}, {
        permission: "denied",
        requestPermission: async () => "denied",
      }),
    })
    if (!("PushManager" in window))
      Object.defineProperty(window, "PushManager", { configurable: true, value: function PushManager() {} })
  })
  await mockSignedInApis(page)
  await page.goto("/notifications")

  await expect(page.getByRole("heading", { level: 1, name: "알림" })).toBeVisible()
  await expect(page.getByText("브라우저 알림이 막혀 있어요. 브라우저 설정에서 알림을 허용해 주세요.")).toBeVisible()
  await expect(page.getByRole("switch", { name: "브라우저 알림" })).not.toBeChecked()
  await expect(page.getByRole("switch", { name: "브라우저 알림" })).toBeDisabled()
  await expect(page.getByText("알림이 없어요.")).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)
  // 화면 모드를 바꾸면 색이 전환(transition)되므로 끝난 뒤에 대비를 본다
  await page.evaluate(() => document.documentElement.classList.add("dark"))
  await page.evaluate(() => Promise.all(document.getAnimations().map((a) => a.finished)))
  await expectNoSeriousA11yViolations(page)
})

test("벨: 좁은 폭에서는 화면 머리에 벨이 있고, 펼친 목록의 가로 넘침·접근성을 지킨다 (FR-005)", async ({ page }) => {
  await mockSignedInApis(page, { unread: () => 3 })
  await page.goto("/")

  const bell = page.getByRole("button", { name: "알림, 안 읽은 알림 3개" }).first()
  await expect(bell).toBeVisible()
  await bell.click()
  await expect(page.getByText("최근 알림")).toBeVisible()
  await expect(page.getByRole("link", { name: "모두 보기" })).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)
})
