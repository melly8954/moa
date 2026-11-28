import { type BrowserContext, expect, type Locator, type Page, type Route, test } from "@playwright/test"
import { signedIn, signedOut } from "./support/api"
import { expectNoSeriousA11yViolations } from "./support/layout"

// 파티 (specs/008-party spec.md "e2e로 확인할 흐름", T056)
//   백엔드 대신 한 테스트 안에서 함께 쓰는 가짜 백엔드(Backend)를 두고, 회원마다 브라우저 문맥을 따로 연다
//   REST는 page.route로, 실시간 연결(STOMP over WebSocket)은 page.routeWebSocket으로 흉내 낸다 (007 notifications.spec.ts와 같다)
//   권한·동시성·오류 코드는 백엔드 인수 테스트(backend/src/test/java/com/moa/api/party)가 본다

type Relation = "NONE" | "MEMBER" | "HOST"
type User = { id: number; loginId: string; name: string }
type Member = {
  userId: number
  displayName: string
  role: "HOST" | "MEMBER"
  positionCode: string | null
  tierCode: string | null
  joinedAt: string
}
type Position = { positionCode: string; slots: number; filled: number }
type Party = {
  id: number
  gameCode: string
  modeCode: string | null
  title: string
  description: string | null
  joinPolicy: "OPEN" | "APPROVAL" | "PRIVATE"
  capacity: number
  status: "RECRUITING" | "CLOSED"
  tierMinCode: string | null
  tierMaxCode: string | null
  positionEnabled: boolean
  positions: Position[]
  members: Member[]
  hostUserId: number
  inviteCode: string
}
type Message = {
  id: number
  type: "CHAT" | "SYSTEM"
  sender: { userId: number; displayName: string } | null
  content: string | null
  systemEvent: "JOINED" | null
  subject: { userId: number; displayName: string } | null
  sentAt: string
}

const HOST: User = { id: 2, loginId: "host", name: "방장모아" }
const GUEST: User = { id: 3, loginId: "guest", name: "회원모아" }
const AT = "2026-11-28T09:00:00+09:00"

const code = (c: string, name: string, sortOrder: number) => ({ code: c, name, sortOrder, enabled: true })
const CODES: Record<string, ReturnType<typeof code>[]> = {
  GAME: [code("LOL", "리그 오브 레전드", 10), code("PUBG", "배틀그라운드", 40)],
  LOL_MODE: [code("SOLO_RANK", "솔로랭크", 10), code("NORMAL", "일반", 30)],
  LOL_POSITION: [
    code("TOP", "탑", 10),
    code("JUNGLE", "정글", 20),
    code("MID", "미드", 30),
    code("ADC", "원딜", 40),
    code("SUPPORT", "서포터", 50),
  ],
  LOL_TIER: [code("SILVER", "실버", 30), code("GOLD", "골드", 40), code("PLATINUM", "플래티넘", 50)],
  PUBG_MODE: [code("SQUAD", "스쿼드", 10)],
  PUBG_TIER: [code("GOLD", "골드", 30)],
}

// ---- 실시간 (STOMP) ----

type Stomp = { send: (destination: string, body: unknown) => Promise<void> }

async function mockStomp(page: Page): Promise<Stomp> {
  const subscriptions = new Map<string, string>()
  let socket: { send: (message: string) => void } | undefined
  let messageId = 0
  await page.routeWebSocket("**/ws", (ws) => {
    socket = ws
    ws.onMessage((raw) => {
      const frame = String(raw)
      const command = frame.split("\n")[0]
      if (command === "CONNECT" || command === "STOMP") {
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
    send: async (destination, body) => {
      await expect.poll(() => subscriptions.has(destination)).toBe(true)
      messageId += 1
      socket?.send(
        `MESSAGE\ndestination:${destination}\nsubscription:${subscriptions.get(destination)}\nmessage-id:${messageId}\ncontent-type:application/json\n\n${JSON.stringify(body)}\0`,
      )
    },
  }
}

// ---- 가짜 백엔드 ----

class Backend {
  parties: Party[] = []
  messages = new Map<number, Message[]>()
  sockets = new Map<number, Stomp>()
  // 마지막으로 받은 요청 몸통 (경로 → 몸통)
  sent = new Map<string, unknown>()
  private nextMessageId = 1

  find(id: number) {
    return this.parties.find((p) => p.id === id)
  }

  relationOf(party: Party, userId: number): Relation {
    if (party.hostUserId === userId) return "HOST"
    return party.members.some((m) => m.userId === userId) ? "MEMBER" : "NONE"
  }

  summary(party: Party) {
    const host = party.members.find((m) => m.userId === party.hostUserId)
    return {
      id: party.id,
      gameCode: party.gameCode,
      modeCode: party.modeCode,
      title: party.title,
      joinPolicy: party.joinPolicy,
      memberCount: party.members.length,
      capacity: party.capacity,
      openPositions: party.positions
        .filter((p) => p.filled < p.slots)
        .map((p) => ({ positionCode: p.positionCode, remaining: p.slots - p.filled })),
      tierMinCode: party.tierMinCode,
      tierMaxCode: party.tierMaxCode,
      host: { userId: party.hostUserId, displayName: host?.displayName ?? "" },
      createdAt: AT,
    }
  }

  detail(party: Party, userId: number) {
    const relation = this.relationOf(party, userId)
    return {
      ...this.summary(party),
      description: party.description,
      status: party.status,
      closedByHost: false,
      positionEnabled: party.positionEnabled,
      positions: party.positions,
      members: party.members,
      me: { relation },
      // 멤버에게만 온다 (US4-AC1)
      invite: relation === "NONE" ? null : { code: party.inviteCode, url: `/invite/${party.inviteCode}` },
    }
  }

  create(user: User, body: Record<string, unknown>): Party {
    const id = this.parties.length + 1
    const positions = ((body.positions as { positionCode: string; slots: number }[] | undefined) ?? []).map((p) => ({
      ...p,
      filled: p.positionCode === body.hostPositionCode ? 1 : 0,
    }))
    const party: Party = {
      id,
      gameCode: String(body.gameCode),
      modeCode: (body.modeCode as string | undefined) ?? null,
      title: String(body.title),
      description: (body.description as string | undefined) ?? null,
      joinPolicy: body.joinPolicy as Party["joinPolicy"],
      capacity: Number(body.capacity),
      status: "RECRUITING",
      tierMinCode: (body.tierMinCode as string | undefined) ?? null,
      tierMaxCode: (body.tierMaxCode as string | undefined) ?? null,
      positionEnabled: positions.length > 0,
      positions,
      members: [
        {
          userId: user.id,
          displayName: user.name,
          role: "HOST",
          positionCode: (body.hostPositionCode as string | undefined) ?? null,
          tierCode: null,
          joinedAt: AT,
        },
      ],
      hostUserId: user.id,
      inviteCode: `INV${String(id).padStart(5, "0")}`,
    }
    this.parties.push(party)
    this.messages.set(id, [])
    return party
  }

  private addMessage(partyId: number, message: Omit<Message, "id" | "sentAt">): Message {
    const saved = { ...message, id: this.nextMessageId++, sentAt: AT }
    this.messages.get(partyId)?.push(saved)
    return saved
  }

  // 지금 멤버 중 except가 아닌 회원의 파티 구독에 보낸다
  private async broadcast(party: Party, except: number, body: unknown) {
    for (const m of party.members) {
      if (m.userId === except) continue
      await this.sockets.get(m.userId)?.send(`/topic/parties/${party.id}`, body)
    }
  }

  // 참가·초대 입장의 공통 처리: 멤버가 되고 안내 메시지를 남긴 뒤 다른 멤버에게 알린다
  async admit(party: Party, user: User, body: { positionCode?: string; tierCode?: string }) {
    party.members.push({
      userId: user.id,
      displayName: user.name,
      role: "MEMBER",
      positionCode: body.positionCode ?? null,
      tierCode: body.tierCode ?? null,
      joinedAt: AT,
    })
    const position = party.positions.find((p) => p.positionCode === body.positionCode)
    if (position) position.filled += 1
    if (party.members.length >= party.capacity) party.status = "CLOSED"
    const ref = { userId: user.id, displayName: user.name }
    const joined = this.addMessage(party.id, {
      type: "SYSTEM",
      sender: null,
      content: null,
      systemEvent: "JOINED",
      subject: ref,
    })
    const event = (type: string, payload: unknown) => ({ type, partyId: party.id, payload, occurredAt: AT })
    await this.broadcast(party, user.id, event("MESSAGE", joined))
    await this.broadcast(
      party,
      user.id,
      event("MEMBERS_CHANGED", {
        members: party.members,
        positions: party.positions,
        memberCount: party.members.length,
        capacity: party.capacity,
        status: party.status,
        hostUserId: party.hostUserId,
      }),
    )
  }

  async chat(party: Party, user: User, content: string) {
    const message = this.addMessage(party.id, {
      type: "CHAT",
      sender: { userId: user.id, displayName: user.name },
      content,
      systemEvent: null,
      subject: null,
    })
    return {
      message,
      deliver: () =>
        this.broadcast(party, user.id, { type: "MESSAGE", partyId: party.id, payload: message, occurredAt: AT }),
    }
  }
}

const notFound = (route: Route) =>
  route.fulfill({ status: 404, json: { code: "R001", message: "", timestamp: "", path: "" } })

// 회원 한 명의 화면이 부르는 API. signedIn 뒤에 부른다 (나중에 건 route가 먼저다)
async function mockApis(page: Page, backend: Backend, user: User | undefined) {
  await page.route("**/api/v1/notices/new", (route) => route.fulfill({ json: { hasNew: false } }))
  await page.route("**/api/v1/notifications/unread-count", (route) => route.fulfill({ json: { count: 0 } }))
  await page.route("**/api/v1/notifications?*", (route) =>
    route.fulfill({ json: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 } }),
  )
  await page.route(
    (url) => url.pathname.startsWith("/api/v1/codes/"),
    (route) => {
      const group = decodeURIComponent(new URL(route.request().url()).pathname.split("/").at(-1) ?? "")
      return route.fulfill({ json: CODES[group] ?? [] })
    },
  )
  await page.route(
    (url) => url.pathname === "/api/v1/parties" || url.pathname.startsWith("/api/v1/parties/"),
    async (route) => {
      const request = route.request()
      const url = new URL(request.url())
      const path = url.pathname.replace("/api/v1/parties", "")
      const method = request.method()
      const body = (request.postDataJSON() ?? {}) as Record<string, unknown>
      if (method !== "GET") backend.sent.set(`${method} ${path}`, body)

      if (path === "" && method === "GET") {
        const content = backend.parties
          .filter((p) => p.status === "RECRUITING" && p.joinPolicy !== "PRIVATE")
          .sort((a, b) => b.id - a.id)
          .map((p) => backend.summary(p))
        return route.fulfill({
          json: { content, page: 0, size: 20, totalElements: content.length, totalPages: 1 },
        })
      }
      if (!user) return route.fulfill({ status: 401, json: { code: "A001", message: "", timestamp: "", path: "" } })
      if (path === "" && method === "POST")
        return route.fulfill({ status: 201, json: backend.detail(backend.create(user, body), user.id) })
      if (path === "/me") {
        const mine = backend.parties.find((p) => backend.relationOf(p, user.id) !== "NONE")
        return route.fulfill({
          json: {
            party: mine
              ? { ...backend.summary(mine), myRole: backend.relationOf(mine, user.id) === "HOST" ? "HOST" : "MEMBER" }
              : null,
            joinRequests: [],
          },
        })
      }

      const invite = path.match(/^\/invites\/([^/]+)(\/members)?$/)
      if (invite) {
        const party = backend.parties.find((p) => p.inviteCode === decodeURIComponent(invite[1] ?? ""))
        if (!party) return route.fulfill({ status: 404, json: { code: "B107", message: "", timestamp: "", path: "" } })
        if (invite[2] && method === "POST") {
          await backend.admit(party, user, body)
          return route.fulfill({ json: backend.detail(party, user.id) })
        }
        return route.fulfill({
          json: {
            id: party.id,
            gameCode: party.gameCode,
            modeCode: party.modeCode,
            title: party.title,
            host: backend.summary(party).host,
            memberCount: party.members.length,
            capacity: party.capacity,
            positions: party.positions,
            me: { relation: backend.relationOf(party, user.id) },
            joinable: true,
            reasonCode: null,
          },
        })
      }

      const [, idPart = "", ...restParts] = path.split("/")
      const party = /^\d+$/.test(idPart) ? backend.find(Number(idPart)) : undefined
      if (!party) return notFound(route)
      const rest = restParts.length > 0 ? `/${restParts.join("/")}` : ""
      if (rest === "" && method === "GET") return route.fulfill({ json: backend.detail(party, user.id) })
      if (rest === "/members" && method === "POST") {
        await backend.admit(party, user, body)
        return route.fulfill({ json: backend.detail(party, user.id) })
      }
      if (rest === "/messages" && method === "GET") {
        const after = Number(url.searchParams.get("after") ?? 0)
        const items = (backend.messages.get(party.id) ?? []).filter((m) => m.id > after)
        return route.fulfill({ json: { items, hasMore: false } })
      }
      if (rest === "/messages" && method === "POST") {
        const { message, deliver } = await backend.chat(party, user, String(body.content))
        await route.fulfill({ status: 201, json: message })
        return deliver()
      }
      return notFound(route)
    },
  )
}

// 로그인한 회원의 화면을 준비한다: 실시간 연결, 로그인 상태, API
async function asMember(page: Page, backend: Backend, user: User) {
  backend.sockets.set(user.id, await mockStomp(page))
  await signedIn(page, { me: { id: user.id, loginId: user.loginId, name: user.name } })
  await mockApis(page, backend, user)
}

const dialog = (page: Page, name: string) => page.getByRole("dialog", { name })
const chatInput = (page: Page) => page.getByLabel("메시지", { exact: true })

// 키보드로만 누른다: 그 요소로 포커스를 옮긴 뒤 Enter
async function pressEnterOn(locator: Locator) {
  await locator.focus()
  await locator.press("Enter")
}

let others: BrowserContext[] = []
test.afterEach(async () => {
  for (const context of others) await context.close()
  others = []
})

test("US2-AC6·US1-AC1·US2-AC1·US3-AC1·US9-AC1·US9-AC3·US9-AC7 로그인하지 않고 파티 만들기를 누르면 로그인한 뒤 돌아오고, 공개 파티를 만들면 다른 회원이 모집 페이지에서 찾아 참가해 채팅을 주고받는다 (키보드만으로)", async ({
  page,
  browser,
  baseURL,
  viewport,
}) => {
  const backend = new Backend()

  // 1. 로그인하지 않은 채 모집 페이지를 보고 "파티 만들기"를 누르면 로그인 화면을 거쳐 돌아온다 (US2-AC6)
  backend.sockets.set(HOST.id, await mockStomp(page))
  await signedOut(page)
  await mockApis(page, backend, undefined)
  await page.goto("/recruit")
  await expect(page.getByRole("heading", { level: 1, name: "모집" })).toBeVisible()
  await expectNoSeriousA11yViolations(page)
  await pressEnterOn(page.getByRole("button", { name: "파티 만들기" }).first())
  await expect(page).toHaveURL(/\/sign-in\?next=%2Frecruit$/)

  await page.unrouteAll()
  await signedIn(page, { me: { id: HOST.id, loginId: HOST.loginId, name: HOST.name } })
  await mockApis(page, backend, HOST)
  await page.route("**/api/v1/auth/login", (route) =>
    route.fulfill({ json: { accessToken: "e2e-token", tokenType: "Bearer", expiresIn: 900 } }),
  )
  await page.getByLabel("아이디").fill(HOST.loginId)
  await page.getByLabel("비밀번호").fill("host-pass")
  await page.getByLabel("비밀번호").press("Enter")
  await expect(page).toHaveURL(/\/recruit$/)

  // 2. 파티 만들기 창: Enter로 열고 Esc로 닫은 뒤, 다시 열어 키보드로 채우고 Enter로 만든다 (US1-AC1)
  const createButton = page.getByRole("button", { name: "파티 만들기" }).first()
  await pressEnterOn(createButton)
  const create = dialog(page, "파티 만들기")
  await expect(create).toBeVisible()
  await page.keyboard.press("Escape")
  await expect(create).toBeHidden()
  await pressEnterOn(createButton)
  await expect(create).toBeVisible()
  await create.getByLabel("게임").selectOption("LOL")
  await create.getByLabel("최소 티어").selectOption("GOLD")
  await expectNoSeriousA11yViolations(page)
  const title = create.getByLabel("제목")
  await title.focus()
  await page.keyboard.type("저녁 칼바람 한 판")
  await page.keyboard.press("Enter")

  await expect(page).toHaveURL(/\/parties\/1$/)
  await expect(page.getByRole("heading", { level: 1, name: "저녁 칼바람 한 판" })).toBeVisible()
  expect(backend.sent.get("POST ")).toMatchObject({
    gameCode: "LOL",
    title: "저녁 칼바람 한 판",
    capacity: 4,
    joinPolicy: "OPEN",
    tierMinCode: "GOLD",
  })
  const hostMembers = page.getByRole("region", { name: "멤버" })
  await expect(hostMembers.getByRole("listitem")).toHaveText([/방장모아/])
  await expect(page.getByText("1/4명")).toBeVisible()

  // 3. 다른 회원이 모집 페이지에서 찾아 참가한다 (US2-AC1, US3-AC1). 참가 창도 Enter·Esc로만 다룬다
  const guestContext = await browser.newContext({ baseURL, viewport })
  others.push(guestContext)
  const guest = await guestContext.newPage()
  await asMember(guest, backend, GUEST)
  await guest.goto("/recruit")
  const card = guest.getByRole("link", { name: /저녁 칼바람 한 판/ })
  await expect(card).toContainText("리그 오브 레전드")
  await expect(card).toContainText("1/4")
  await expect(card).toContainText("골드 이상")
  await pressEnterOn(card)
  await expect(guest).toHaveURL(/\/parties\/1$/)
  // 멤버가 아니면 채팅이 보이지 않는다
  await expect(guest.getByRole("region", { name: "채팅" })).toHaveCount(0)

  const joinButton = guest.getByRole("button", { name: "참가", exact: true })
  await pressEnterOn(joinButton)
  const join = dialog(guest, "참가 조건 고르기")
  await expect(join).toBeVisible()
  await guest.keyboard.press("Escape")
  await expect(join).toBeHidden()
  await pressEnterOn(joinButton)
  await expect(join).toBeVisible()
  await expectNoSeriousA11yViolations(guest)
  await pressEnterOn(join.getByRole("button", { name: "참가", exact: true }))

  await expect(guest.getByRole("region", { name: "채팅" })).toBeVisible()
  await expect(guest.getByText("2/4명")).toBeVisible()
  // 방장 화면의 멤버 목록과 채팅이 새로고침 없이 바뀐다 (US9-AC7, US9-AC3)
  await expect(hostMembers.getByRole("listitem")).toHaveText([/방장모아/, /회원모아/])
  await expect(page.getByText("2/4명")).toBeVisible()
  await expect(page.getByText("회원모아님이 들어왔어요.")).toBeVisible()

  // 4. 채팅을 주고받는다: 입력하고 Enter로 보낸다 (US9-AC1)
  await chatInput(guest).focus()
  await guest.keyboard.type("안녕하세요, 같이 해요")
  await guest.keyboard.press("Enter")
  await expect(chatInput(guest)).toHaveValue("")
  await expect(guest.getByText("안녕하세요, 같이 해요")).toBeVisible()
  await expect(page.getByText("안녕하세요, 같이 해요")).toBeVisible()
  await expect(page.getByRole("region", { name: "채팅" }).getByText("회원모아", { exact: true })).toBeVisible()

  await chatInput(page).focus()
  await page.keyboard.type("반가워요")
  await page.keyboard.press("Enter")
  await expect(guest.getByText("반가워요")).toBeVisible()
  await expect(guest.getByRole("region", { name: "채팅" }).getByText("방장모아", { exact: true })).toBeVisible()

  await expectNoSeriousA11yViolations(page)
  await expectNoSeriousA11yViolations(guest)
})

test("US1-AC2·US4-AC1·US4-AC2 비공개 파티를 만들면 초대 링크가 나오고, 다른 회원이 그 링크로 들어와 멤버가 된다", async ({
  page,
  browser,
  baseURL,
  viewport,
}) => {
  const backend = new Backend()
  await asMember(page, backend, HOST)
  await page.goto("/recruit")
  await page.getByRole("button", { name: "파티 만들기" }).first().click()
  const create = dialog(page, "파티 만들기")
  await create.getByLabel("게임").selectOption("PUBG")
  await create.getByLabel("제목").fill("친구끼리 스쿼드")
  await create.getByRole("radio", { name: "비공개" }).click()
  await create.getByRole("button", { name: "만들기" }).click()

  // 만든 뒤 초대 링크와 코드를 먼저 보인다 (US1-AC2)
  const created = dialog(page, "비공개 파티를 만들었어요")
  await expect(created).toBeVisible()
  const link = await created.getByLabel("초대 링크").inputValue()
  expect(link).toMatch(/\/invite\/INV00001$/)
  await expect(created.getByLabel("초대 코드")).toHaveValue("INV00001")
  await expectNoSeriousA11yViolations(page)
  await created.getByRole("button", { name: "파티 화면으로" }).click()
  await expect(page).toHaveURL(/\/parties\/1$/)
  // 멤버의 파티 화면에도 초대가 보인다 (US4-AC1)
  await expect(page.getByRole("region", { name: "초대" }).getByLabel("초대 코드")).toHaveValue("INV00001")

  // 다른 회원이 링크를 열어 들어온다 (US4-AC2)
  const guestContext = await browser.newContext({ baseURL, viewport })
  others.push(guestContext)
  const guest = await guestContext.newPage()
  await asMember(guest, backend, GUEST)
  await guest.goto(new URL(link).pathname)
  await expect(guest.getByRole("heading", { level: 1, name: "초대" })).toBeVisible()
  await expect(guest.getByText("친구끼리 스쿼드")).toBeVisible()
  await expectNoSeriousA11yViolations(guest)
  await guest.getByRole("button", { name: "들어가기" }).click()

  await expect(guest).toHaveURL(/\/parties\/1$/)
  await expect(guest.getByRole("heading", { level: 1, name: "친구끼리 스쿼드" })).toBeVisible()
  await expect(guest.getByRole("region", { name: "채팅" })).toBeVisible()
  await expect(guest.getByRole("region", { name: "멤버" }).getByRole("listitem")).toHaveText([/방장모아/, /회원모아/])
  await expect(page.getByRole("region", { name: "멤버" }).getByRole("listitem")).toHaveText([/방장모아/, /회원모아/])
  await expectNoSeriousA11yViolations(guest)
})

test("US10-AC5·US10-AC8 롤 포지션 파티에서 빈 포지션을 골라 들어오고, 범위 밖 티어를 적어도 들어와 멤버 목록에 보인다", async ({
  page,
}) => {
  const backend = new Backend()
  backend.create(HOST, {
    gameCode: "LOL",
    title: "플래 이상 자랭",
    capacity: 3,
    joinPolicy: "OPEN",
    tierMinCode: "PLATINUM",
    hostPositionCode: "MID",
    positions: [
      { positionCode: "MID", slots: 1 },
      { positionCode: "JUNGLE", slots: 1 },
      { positionCode: "TOP", slots: 1 },
    ],
  })
  await asMember(page, backend, GUEST)
  await page.goto("/recruit")
  const card = page.getByRole("link", { name: /플래 이상 자랭/ })
  await expect(card).toContainText("빈 포지션")
  await expect(card).toContainText("정글 1")
  await expect(card).toContainText("탑 1")
  await card.click()

  await expect(page.getByRole("heading", { level: 1, name: "플래 이상 자랭" })).toBeVisible()
  await page.getByRole("button", { name: "참가", exact: true }).click()
  const join = dialog(page, "참가 조건 고르기")
  await expect(join).toBeVisible()
  // 찬 포지션(방장의 미드)은 고를 수 없다
  await expect(join.getByRole("radio", { name: /미드/ })).toBeDisabled()
  await join.getByRole("radio", { name: /정글/ }).click()
  await join.getByLabel("내 티어").selectOption("GOLD")
  await expect(join.getByText("파티의 티어 범위 밖이에요. 그래도 들어갈 수 있어요.")).toBeVisible()
  await expectNoSeriousA11yViolations(page)
  await join.getByRole("button", { name: "참가", exact: true }).click()

  await expect(join).toBeHidden()
  expect(backend.sent.get("POST /1/members")).toEqual({ positionCode: "JUNGLE", tierCode: "GOLD" })
  const members = page.getByRole("region", { name: "멤버" })
  await expect(members.getByRole("listitem").filter({ hasText: "회원모아" })).toContainText("정글")
  await expect(members.getByRole("listitem").filter({ hasText: "회원모아" })).toContainText("골드")
  await expect(members.getByText("정글 1/1")).toBeVisible()
  await expect(page.getByRole("region", { name: "채팅" })).toBeVisible()
  await expectNoSeriousA11yViolations(page)
})
