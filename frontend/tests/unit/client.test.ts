import { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from "axios"
import { beforeEach, describe, expect, it } from "vitest"
import { http } from "@/api/client"
import { useAuthStore } from "@/stores/auth"

// 백엔드 대신 응답을 정하는 어댑터. 요청마다 handler가 응답(또는 오류 상태)을 고른다
type Reply = { status: number; data?: unknown }
let calls: InternalAxiosRequestConfig[] = []
let handler: (config: InternalAxiosRequestConfig) => Reply

http.defaults.adapter = async (config) => {
  calls.push(config)
  const { status, data } = handler(config)
  const response = { status, statusText: "", headers: {}, config, data }
  if (status >= 400) throw new AxiosError("fail", undefined, config, undefined, response)
  return response
}

const expired = { status: 401, data: { code: "A002", message: "", timestamp: "", path: "" } }
const bearer = (config: InternalAxiosRequestConfig) => AxiosHeaders.from(config.headers).get("Authorization")

beforeEach(() => {
  calls = []
  useAuthStore.setState({ accessToken: "old", status: "signedIn" })
})

describe("api/client", () => {
  it("액세스 토큰을 싣는다", async () => {
    handler = () => ({ status: 200, data: "ok" })
    await http.get("/api/v1/items")
    expect(bearer(calls[0])).toBe("Bearer old")
  })

  it("A002면 재발급을 한 번만 하고 기다리던 요청을 새 토큰으로 다시 보낸다", async () => {
    handler = (config) => {
      if (config.url === "/api/v1/auth/token/refresh") return { status: 200, data: { accessToken: "new" } }
      return bearer(config) === "Bearer new" ? { status: 200, data: config.url } : expired
    }
    const results = await Promise.all([http.get("/api/v1/a"), http.get("/api/v1/b")])

    expect(results.map((r) => r.data)).toEqual(["/api/v1/a", "/api/v1/b"])
    expect(calls.filter((c) => c.url === "/api/v1/auth/token/refresh")).toHaveLength(1)
    expect(useAuthStore.getState().accessToken).toBe("new")
  })

  it("재발급 요청에는 토큰을 싣지 않는다", async () => {
    handler = (config) =>
      config.url === "/api/v1/auth/token/refresh" ? { status: 200, data: { accessToken: "new" } } : expired
    await http.get("/api/v1/a").catch(() => undefined)
    expect(bearer(calls.find((c) => c.url === "/api/v1/auth/token/refresh")!)).toBeFalsy()
  })

  it("재발급이 실패하면 세션을 비운다", async () => {
    handler = (config) =>
      config.url === "/api/v1/auth/token/refresh"
        ? { status: 401, data: { code: "A003", message: "", timestamp: "", path: "" } }
        : expired
    await expect(http.get("/api/v1/a")).rejects.toMatchObject({ code: "A003" })
    expect(useAuthStore.getState()).toMatchObject({ accessToken: null, status: "expired" })
  })

  it("A002가 아닌 401은 재발급하지 않는다", async () => {
    handler = () => ({ status: 401, data: { code: "A001", message: "", timestamp: "", path: "" } })
    await expect(http.get("/api/v1/a")).rejects.toMatchObject({ code: "A001" })
    expect(calls).toHaveLength(1)
  })
})
