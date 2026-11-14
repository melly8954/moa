import { fileURLToPath } from "node:url"
import { defineConfig } from "vitest/config"

// 단위 테스트: tests/unit. 화면 반응형·접근성은 Playwright(e2e)가 본다
// 기본은 node 환경이다. DOM이 필요한 테스트(render, renderHook, renderWithIntl 등)는 이름을 *.dom.test.ts(x)로 둔다
// → jsdom 준비가 테스트 시간 대부분을 차지해, 필요한 파일만 jsdom에서 돌린다. 빠뜨리면 "document is not defined"로 실패한다
export default defineConfig({
  resolve: { alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) } },
  oxc: { jsx: { runtime: "automatic" } },
  test: {
    // 작업자는 스레드로, 넷까지만 띄운다. 막 설치한 프로젝트에서 작업자를 한꺼번에 많이 띄우면
    // 부하가 높을 때 vitest의 작업자 시작 제한(60초)을 넘겨 실패한다 (2026-11 Windows에서 겪음)
    pool: "threads",
    maxWorkers: 4,
    projects: [
      {
        extends: true,
        test: {
          name: "node",
          environment: "node",
          include: ["tests/unit/**/*.test.{ts,tsx}"],
          exclude: ["tests/unit/**/*.dom.test.{ts,tsx}"],
        },
      },
      {
        extends: true,
        test: {
          name: "dom",
          environment: "jsdom",
          include: ["tests/unit/**/*.dom.test.{ts,tsx}"],
          setupFiles: ["tests/setup.ts"],
        },
      },
    ],
  },
})
