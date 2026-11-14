import { AxiosError, AxiosHeaders } from "axios"
import { describe, expect, it } from "vitest"
import { ApiError, NETWORK_ERROR, toApiError } from "@/api/error"
import { useErrorMessages } from "@/lib/error-messages"
import ko from "../../messages/ko.json"
import { renderHookWithIntl } from "../intl"

function axiosError(status: number | undefined, data?: unknown) {
  const config = { headers: new AxiosHeaders() }
  const response = status === undefined ? undefined : { status, statusText: "", headers: {}, config, data }
  return new AxiosError("fail", undefined, config, undefined, response)
}

const { errorMessage, describeError } = renderHookWithIntl(() => useErrorMessages()).result.current
const FALLBACK = ko.errors.fallback

describe("오류 문구", () => {
  it("기본 에러 코드는 모두 문구가 있다 (CONTRACT.md 4절)", () => {
    for (const code of [
      "V001",
      "V002",
      "V003",
      "A001",
      "A002",
      "A003",
      "A004",
      "A005",
      "A006",
      "A007",
      "R001",
      "R002",
      "B001",
      NETWORK_ERROR,
    ]) {
      expect(errorMessage(code)).not.toBe(FALLBACK)
    }
  })

  it("표에 없는 코드와 S001은 공통 문구다", () => {
    expect(errorMessage("X999")).toBe(FALLBACK)
    expect(errorMessage("S001")).toBe(FALLBACK)
  })

  it("S001은 traceId를 함께 준다", () => {
    const error = toApiError(
      axiosError(500, { code: "S001", message: "서버 메시지", timestamp: "", path: "/api/v1/x", traceId: "t-1" }),
    )
    expect(describeError(error)).toEqual({ message: FALLBACK, traceId: "t-1" })
  })

  it("서버 message는 화면 문구로 쓰지 않는다", () => {
    const error = toApiError(
      axiosError(404, { code: "R001", message: "대상을 찾을 수 없습니다.", timestamp: "", path: "" }),
    )
    expect(describeError(error).message).toBe(errorMessage("R001"))
  })
})

describe("toApiError", () => {
  it("응답이 없으면 연결 실패다", () => {
    expect(toApiError(axiosError(undefined)).code).toBe(NETWORK_ERROR)
  })

  it("V001의 칸 오류를 꺼낸다", () => {
    const error = toApiError(
      axiosError(400, {
        code: "V001",
        message: "",
        timestamp: "",
        path: "",
        errors: [{ field: "loginId", message: "필수" }],
      }),
    )
    expect(error).toBeInstanceOf(ApiError)
    expect(error.fieldErrors).toEqual([{ field: "loginId", message: "필수" }])
  })

  it("오류 형식이 아닌 5xx는 S001로 본다", () => {
    expect(toApiError(axiosError(502, "<html>")).code).toBe("S001")
  })
})
