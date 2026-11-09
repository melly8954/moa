import { act, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it } from "vitest"
import { confirm, ConfirmHost } from "@/components/kit/confirm-dialog"
import { renderWithIntl } from "../intl"

describe("확인 창", () => {
  it("확인을 누르면 true", async () => {
    renderWithIntl(<ConfirmHost />)
    let answer: Promise<boolean> = Promise.resolve(false)
    act(() => {
      answer = confirm({ title: "지울까요?", description: "되돌릴 수 없어요." })
    })

    expect(await screen.findByRole("alertdialog", { name: "지울까요?" })).toBeInTheDocument()
    await userEvent.click(screen.getByRole("button", { name: "확인" }))
    await expect(answer).resolves.toBe(true)
  })

  it("취소를 누르면 false, 버튼 이름을 바꿀 수 있다", async () => {
    renderWithIntl(<ConfirmHost />)
    let answer: Promise<boolean> = Promise.resolve(true)
    act(() => {
      answer = confirm({ title: "나갈까요?", confirmLabel: "나가기", cancelLabel: "머무르기" })
    })

    await userEvent.click(await screen.findByRole("button", { name: "머무르기" }))
    await expect(answer).resolves.toBe(false)
  })

  it("창이 열린 채 새로 부르면 앞 창은 false", async () => {
    renderWithIntl(<ConfirmHost />)
    let first: Promise<boolean> = Promise.resolve(true)
    act(() => {
      first = confirm({ title: "첫째" })
      confirm({ title: "둘째" })
    })
    await expect(first).resolves.toBe(false)
    expect(await screen.findByRole("alertdialog", { name: "둘째" })).toBeInTheDocument()
  })
})
