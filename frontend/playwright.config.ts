import { defineConfig, devices } from "@playwright/test"

// 화면 검사: 반응형(가로 넘침)과 접근성(axe). 폭은 360·768·1280
// 넓은 폭만 지원하는 화면(관리자 화면 등)은 테스트 제목에 @wide를 달아 1280에서만 돌린다
// 검사용 서버는 개발 서버(3000)와 겹치지 않게 E2E_PORT(기본 3100)로 띄운다
// 이미 떠 있는 서버를 검사하려면 E2E_BASE_URL을 준다 (그러면 서버를 띄우지 않는다)
const PORT = process.env.E2E_PORT ?? "3100"
const BASE_URL = process.env.E2E_BASE_URL ?? `http://localhost:${PORT}`
const narrow = (name: string, width: number, height: number) => ({
  name,
  use: { ...devices["Desktop Chrome"], viewport: { width, height } },
  grepInvert: /@wide/,
})

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? "github" : "list",
  use: { baseURL: BASE_URL, trace: "on-first-retry" },
  projects: [
    narrow("w360", 360, 800),
    narrow("w768", 768, 1024),
    { name: "w1280", use: { ...devices["Desktop Chrome"], viewport: { width: 1280, height: 800 } } },
  ],
  // 빌드한 앱을 띄운다. 백엔드는 띄우지 않는다 (API는 테스트 안에서 가로챈다 — e2e/support/api.ts)
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : {
        // 매번 새로 빌드해 띄운다. 예전 빌드나 다른 앱을 검사하지 않게 떠 있는 서버를 다시 쓰지 않는다
        command: `npm run build && npx next start -p ${PORT}`,
        url: BASE_URL,
        reuseExistingServer: false,
        timeout: 300_000,
        // 전체 부품 확인 화면(/dev/components)을 운영 빌드에서도 연다
        env: { ...(process.env as Record<string, string>), SHOW_DEV_PAGES: "true" },
      },
})
