import { fileURLToPath } from "node:url"
import { defineConfig } from "vitest/config"

// 단위 테스트: tests/unit. 화면 반응형·접근성은 Playwright(e2e)가 본다
export default defineConfig({
  resolve: { alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) } },
  oxc: { jsx: { runtime: "automatic" } },
  test: {
    // 작업자는 스레드로, 넷까지만 띄운다. 막 설치한 프로젝트에서 작업자를 한꺼번에 많이 띄우면
    // 부하가 높을 때 vitest의 작업자 시작 제한(60초)을 넘겨 실패한다 (2026-11 Windows에서 겪음)
    pool: "threads",
    maxWorkers: 4,
    environment: "jsdom",
    include: ["tests/unit/**/*.test.{ts,tsx}"],
    setupFiles: ["tests/setup.ts"],
  },
})
