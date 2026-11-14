import { describe, expect, it } from "vitest"
import { ApiError, NETWORK_ERROR } from "@/api/error"
import { useImageUploadErrorText } from "@/components/rich-text/use-image-upload"
import { imageFileProblem, isAllowedLink, normalizeLink, sanitizeRichText } from "@/lib/rich-text"
import ko from "../../messages/ko.json"
import { renderHookWithIntl } from "../intl"

const IMG = "/api/v1/rich-text/images/3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c"

describe("서식 있는 글 거르기 (백엔드 HtmlSanitizer와 같은 허용 목록)", () => {
  it("허용한 서식은 남긴다", () => {
    const html =
      "<h2>제목</h2><h3>작은 제목</h3><p><strong>a</strong><em>b</em><s>c</s><code>d</code><br></p>" +
      "<ul><li>e</li></ul><ol><li>f</li></ol><blockquote><p>g</p></blockquote><pre><code>h</code></pre><hr>"
    expect(sanitizeRichText(html)).toBe(html)
  })

  it("허용하지 않은 태그는 지우고 글자는 남긴다. script는 내용까지 지운다", () => {
    expect(
      sanitizeRichText("<h1>큰</h1><div><u>밑줄</u></div><script>alert(1)</script><iframe src='x'></iframe>"),
    ).toBe("큰밑줄")
  })

  it("style·class·on* 속성을 지운다", () => {
    expect(sanitizeRichText('<p style="color:red" class="x" onclick="alert(1)" data-x="1">글</p>')).toBe("<p>글</p>")
  })

  it("본문 이미지 주소만 남기고, 다른 img는 통째로 지운다", () => {
    expect(sanitizeRichText(`<img src="${IMG}" alt="그림" onerror="alert(1)">`)).toBe(`<img src="${IMG}" alt="그림">`)
    expect(sanitizeRichText('<p><img src="https://example.com/a.png">글</p>')).toBe("<p>글</p>")
    expect(sanitizeRichText('<img src="data:image/png;base64,AAAA">')).toBe("")
    expect(sanitizeRichText('<img src="/api/v1/rich-text/images/../../admin">')).toBe("")
  })

  it("링크는 http·https·mailto만 남기고 target을 지우고 rel을 붙인다", () => {
    expect(sanitizeRichText('<a href="https://example.com" target="_blank">a</a>')).toBe(
      '<a href="https://example.com" rel="noopener noreferrer">a</a>',
    )
    expect(sanitizeRichText('<a href="mailto:a@example.com">a</a>')).toBe(
      '<a href="mailto:a@example.com" rel="noopener noreferrer">a</a>',
    )
    expect(sanitizeRichText('<a href="javascript:alert(1)">a</a>')).toBe('<a rel="noopener noreferrer">a</a>')
    expect(sanitizeRichText('<a href="ftp://example.com">a</a>')).toBe('<a rel="noopener noreferrer">a</a>')
  })

  it("비어 있으면 빈 문자열", () => {
    expect(sanitizeRichText(null)).toBe("")
    expect(sanitizeRichText("")).toBe("")
  })
})

describe("링크 주소", () => {
  it("스킴이 없으면 https://를 붙인다", () => {
    expect(normalizeLink(" example.com/a ")).toBe("https://example.com/a")
    expect(normalizeLink("mailto:a@example.com")).toBe("mailto:a@example.com")
  })

  it("http·https·mailto가 아니면 받지 않는다", () => {
    expect(normalizeLink("javascript:alert(1)")).toBeNull()
    expect(normalizeLink("ftp://example.com")).toBeNull()
    expect(normalizeLink("  ")).toBeNull()
    expect(isAllowedLink("http://example.com")).toBe(true)
    expect(isAllowedLink("/relative")).toBe(false)
  })
})

describe("본문 이미지 올리기", () => {
  const file = (type: string, size: number) => new File([new Uint8Array(size)], "a", { type })

  it("png·jpg·gif·webp, 5MB 이하만 올린다", () => {
    expect(imageFileProblem(file("image/png", 10))).toBeNull()
    expect(imageFileProblem(file("image/webp", 5 * 1024 * 1024))).toBeNull()
    expect(imageFileProblem(file("image/svg+xml", 10))).toBe("imageType")
    expect(imageFileProblem(file("image/jpeg", 5 * 1024 * 1024 + 1))).toBe("imageTooLarge")
  })

  it("서버가 V001을 주면 이미지 규칙 문구, 다른 오류는 공통 문구", () => {
    const errorText = renderHookWithIntl(() => useImageUploadErrorText()).result.current
    expect(errorText(new ApiError(400, "V001"))).toBe(ko.richText.imageRejected)
    expect(errorText(new ApiError(401, "A001"))).toBe(ko.errors.A001)
    expect(errorText(new ApiError(0, NETWORK_ERROR))).toBe(ko.errors.NETWORK)
    expect(errorText(new ApiError(500, "S001"))).toBe(ko.errors.fallback)
  })
})
