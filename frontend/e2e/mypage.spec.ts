import { expect, test, type Page } from "@playwright/test"
import { signedIn, signedOut } from "./support/api"

// moa: 마이페이지 핵심 흐름 (specs/009-mypage US1-AC2·US2-AC1, tasks T053. 원칙 II "e2e는 핵심 흐름만")
// 백엔드 없이 돈다: 프로필·닉네임·공통코드 API는 정해 둔 값을 돌려준다
const PROFILE = {
  nickname: "모아킴",
  nicknameTag: "0421",
  avatar: "default",
  bio: null,
  birthYear: null,
  age: null,
  gender: null,
  ageGenderPublic: false,
  discordTag: null,
  discordVisibility: "PRIVATE",
  playTimes: [],
  nicknameChangeableAt: null,
}

async function mypageApis(page: Page, nickname: () => string) {
  await page.route("**/api/v1/users/me/profile", (route) =>
    route.fulfill({ json: { ...PROFILE, nickname: nickname() } }),
  )
  await page.route("**/api/v1/codes/PLAY_TIME**", (route) =>
    route.fulfill({ json: [{ code: "DAWN", name: "새벽", sortOrder: 0 }] }),
  )
}

test("US1-AC2 로그인하지 않고 마이페이지 주소로 들어가면 로그인 화면으로 가고, 로그인하면 마이페이지로 돌아온다", async ({
  page,
}) => {
  await signedOut(page)
  await page.goto("/account/profile")
  await expect(page).toHaveURL(/\/sign-in\?next=%2Faccount%2Fprofile$/)

  await page.unrouteAll()
  await signedIn(page, { me: { name: "모아킴" } })
  await mypageApis(page, () => "모아킴")
  await page.route("**/api/v1/auth/login", (route) =>
    route.fulfill({ json: { accessToken: "e2e-token", tokenType: "Bearer", expiresIn: 900 } }),
  )
  await page.getByLabel("아이디").fill("e2e")
  await page.getByLabel("비밀번호").fill("e2e-pass")
  await page.getByRole("button", { name: "로그인" }).click()

  await expect(page).toHaveURL(/\/account\/profile$/)
  await expect(page.getByRole("heading", { name: "마이페이지" })).toBeVisible()
  await expect(page.getByText("모아킴#0421")).toBeVisible()
})

test("US2-AC1 닉네임을 바꾸면 안내가 보이고 계정 바의 닉네임이 새로고침 없이 바뀐다 @wide", async ({ page }) => {
  let nickname = "모아킴"
  await signedIn(page)
  await page.route("**/api/v1/users/me", (route) =>
    route.fulfill({
      json: {
        id: 2,
        loginId: "e2e",
        name: nickname,
        role: "USER",
        passwordChangeRequired: false,
        hasPassword: true,
        avatar: "default",
        mypageAllowed: true,
      },
    }),
  )
  await mypageApis(page, () => nickname)
  await page.route("**/api/v1/users/me/nickname", (route) => {
    nickname = (route.request().postDataJSON() as { nickname: string }).nickname
    return route.fulfill({ json: { nickname, nicknameTag: "0421", nicknameChangeableAt: null } })
  })

  await page.goto("/account/profile")
  await expect(page.getByText("모아킴#0421")).toBeVisible()
  let loads = 0
  page.on("load", () => loads++)
  await page.getByLabel(/^닉네임/).fill("새닉네임")
  await page.getByRole("button", { name: "닉네임 바꾸기" }).click()

  await expect(page.getByText("닉네임을 바꿨어요.")).toBeVisible()
  await expect(page.getByText("새닉네임#0421")).toBeVisible()
  // 계정 바: 사이드바 바닥의 이름 자리
  await expect(page.getByText("새닉네임", { exact: true }).first()).toBeVisible()
  await expect(page.getByText("모아킴", { exact: true })).toHaveCount(0)
  expect(loads).toBe(0)
})
