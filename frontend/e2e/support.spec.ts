import { expect, type Page, test } from "@playwright/test"
import { signedIn, signedOut } from "./support/api"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 고객센터 (specs/017-customer-center) 핵심 흐름. 백엔드 대신 API를 가로챈다
// 분류·권한·남의 문의·알림 같은 규칙은 백엔드 CustomerCenterAcceptanceTest가 본다
//   여기서는 화면에서만 보이는 것을 본다 (research R13):
//   US2-AC3 질문을 눌러 펼치고 접기, US3-AC2 로그인하지 않고 문의하기를 누르면 로그인으로 보냄,
//   US3-AC3 다섯 장이면 붙이기가 꺼지고 안내, US4-AC2 관리 드로어에서 첨부를 크게 보기

const PNG = Buffer.from(
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=",
  "base64",
)
const imageUrl = (n: number) => `/api/v1/rich-text/images/00000000-0000-4000-8000-00000000000${n}`
const CATEGORIES = [
  { code: "ACCOUNT", name: "계정" },
  { code: "USAGE", name: "이용 방법" },
]
const FAQS = [
  {
    id: 1,
    categoryCode: "ACCOUNT",
    categoryName: "계정",
    question: "비밀번호를 잊었어요",
    answer: "<p>찾기에서 바꿔요</p>",
  },
  {
    id: 2,
    categoryCode: "USAGE",
    categoryName: "이용 방법",
    question: "파티는 어떻게 만들어요",
    answer: "<p>모집에서 만들어요</p>",
  },
]

async function mockSupport(page: Page) {
  await page.route("**/api/v1/faqs/categories", (route) => route.fulfill({ json: CATEGORIES }))
  await page.route(
    (url) => url.pathname === "/api/v1/faqs",
    (route) => {
      const url = new URL(route.request().url())
      const category = url.searchParams.get("categoryCode")
      const keyword = url.searchParams.get("keyword")
      const found = FAQS.filter((f) => !category || f.categoryCode === category).filter(
        (f) => !keyword || f.question.includes(keyword) || f.answer.includes(keyword),
      )
      return route.fulfill({ json: found })
    },
  )
  let next = 0
  await page.route("**/api/v1/rich-text/images", (route) => {
    next += 1
    return route.fulfill({ status: 201, json: { id: String(next), url: imageUrl(next) } })
  })
  await page.route("**/api/v1/rich-text/images/*", (route) =>
    route.fulfill({ status: 200, contentType: "image/png", body: PNG }),
  )
}

test("US2-AC3·AC2 로그인 없이 FAQ에서 분류를 고르고 질문을 눌러 답을 펼치고 접는다 (가로 넘침·접근성)", async ({
  page,
}) => {
  await signedOut(page)
  await mockSupport(page)
  await page.goto("/support")

  await expect(page.getByRole("button", { name: "비밀번호를 잊었어요" })).toBeVisible()
  await page.getByRole("button", { name: "이용 방법", exact: true }).click()
  await expect(page).toHaveURL(/category=USAGE/)
  await expect(page.getByRole("button", { name: /비밀번호를 잊었어요/ })).toHaveCount(0)

  const question = page.getByRole("button", { name: /파티는 어떻게 만들어요/ })
  await question.click()
  await expect(page.getByText("모집에서 만들어요")).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)
  await question.click()
  await expect(page.getByText("모집에서 만들어요")).toBeHidden()
})

test("US3-AC2 로그인하지 않고 1:1 문의하기를 누르면 돌아올 주소와 함께 로그인 화면으로 간다", async ({ page }) => {
  await signedOut(page)
  await mockSupport(page)
  await page.goto("/support")

  await page.getByRole("link", { name: "1:1 문의하기" }).first().click()
  await expect(page).toHaveURL(/\/sign-in\?next=%2Fsupport%2Finquiries%2Fnew$/)
})

test("US3-AC1·AC3 문의 쓰기: 다섯 장이면 붙이기가 꺼지고 안내가 보이며, 두 장으로 보내면 상세로 간다 (가로 넘침·접근성)", async ({
  page,
}) => {
  await signedIn(page)
  await mockSupport(page)
  let sent: { imageUrls?: string[]; title?: string } | undefined
  await page.route("**/api/v1/inquiries", (route) => {
    sent = route.request().postDataJSON()
    return route.fulfill({
      status: 201,
      json: {
        id: 7,
        categoryCode: "ACCOUNT",
        categoryName: "계정",
        title: sent?.title,
        status: "WAITING",
        createdAt: "2026-11-25T10:00:00+09:00",
        content: "내용",
        imageUrls: sent?.imageUrls,
        deletable: true,
      },
    })
  })
  await page.route("**/api/v1/inquiries/7", (route) =>
    route.fulfill({
      json: {
        id: 7,
        categoryCode: "ACCOUNT",
        categoryName: "계정",
        title: "로그인이 안 돼요",
        status: "WAITING",
        createdAt: "2026-11-25T10:00:00+09:00",
        content: "어제부터 안 돼요",
        imageUrls: [imageUrl(1), imageUrl(2)],
        deletable: true,
      },
    }),
  )
  await page.goto("/support/inquiries/new")

  await page.getByLabel("분류").selectOption("ACCOUNT")
  await page.getByLabel("제목").fill("로그인이 안 돼요")
  await page.getByLabel("내용").fill("어제부터 안 돼요")
  const files = Array.from({ length: 5 }, (_, i) => ({ name: `shot${i}.png`, mimeType: "image/png", buffer: PNG }))
  await page.getByLabel("이미지", { exact: true }).setInputFiles(files)
  await expect(page.getByRole("button", { name: /이미지 5 크게 보기/ })).toBeVisible()
  await expect(page.getByRole("button", { name: "이미지 붙이기" })).toBeDisabled()
  await expect(page.getByText("이미지는 다섯 장까지 붙일 수 있어요.")).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)

  for (let i = 0; i < 3; i += 1) await page.getByRole("button", { name: "이미지 3 빼기" }).click()
  await expect(page.getByRole("button", { name: "이미지 붙이기" })).toBeEnabled()
  await page.getByRole("button", { name: "보내기" }).click()

  await expect(page).toHaveURL(/\/support\/inquiries\/7$/)
  expect(sent?.imageUrls).toEqual([imageUrl(1), imageUrl(2)])
  await expect(page.getByRole("heading", { name: "로그인이 안 돼요" })).toBeVisible()
  await expect(page.getByRole("button", { name: "지우기" })).toBeVisible()
})

test("US4-AC2·AC3 문의 관리 드로어에서 첨부를 크게 보고 답변을 저장한다 (가로 넘침·접근성)", async ({ page }) => {
  await signedIn(page, { me: { role: "ADMIN" }, permissions: ["INQUIRY_MANAGE"] })
  await mockSupport(page)
  const item = {
    id: 3,
    status: "WAITING",
    categoryCode: "ACCOUNT",
    categoryName: "계정",
    title: "로그인이 안 돼요",
    author: { userId: 21, loginId: "member-a", name: "회원A" },
    createdAt: "2026-11-25T10:00:00+09:00",
    answeredAt: null,
  }
  await page.route("**/api/v1/admin/inquiries?*", (route) =>
    route.fulfill({ json: { content: [item], page: 0, size: 10, totalElements: 1, totalPages: 1 } }),
  )
  await page.route("**/api/v1/admin/inquiries/3", (route) =>
    route.fulfill({
      json: {
        ...item,
        content: "어제부터\n안 돼요",
        imageUrls: [imageUrl(1)],
        answer: null,
        answeredByName: null,
        answerable: true,
      },
    }),
  )
  let answer: { content?: string } | undefined
  await page.route("**/api/v1/admin/inquiries/3/answer", (route) => {
    answer = route.request().postDataJSON()
    return route.fulfill({ status: 204 })
  })
  await page.goto("/admin/inquiries")

  await page.getByRole("button", { name: "로그인이 안 돼요" }).click()
  const drawer = page.getByRole("dialog", { name: "1:1 문의" })
  await expect(drawer.getByText("회원A (member-a)")).toBeVisible()
  await drawer.getByRole("button", { name: "이미지 1 크게 보기" }).click()
  const image = page.getByRole("dialog", { name: "이미지 1" })
  await expect(image).toBeVisible()
  await image.getByRole("button", { name: "Close" }).click()
  await expect(image).toBeHidden()
  await expect(drawer).toBeVisible()

  const editor = drawer.locator('[contenteditable="true"]')
  await editor.click()
  await editor.pressSequentially("비밀번호 찾기를 해 보세요.")
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)
  await drawer.getByRole("button", { name: "답변 저장" }).click()
  await expect(page.getByText("답변을 저장했어요. 회원에게 알림이 갔어요.")).toBeVisible()
  expect(answer?.content).toContain("비밀번호 찾기를 해 보세요.")
})
