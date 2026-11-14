import DOMPurify from "dompurify"

// 서식 있는 글(HTML) 규칙 (rich-text 헬퍼). 백엔드 rich-text 헬퍼(HtmlSanitizer)와 같은 허용 목록이다
// 허용 목록을 늘리면 백엔드 HtmlSanitizer, 편집기(components/rich-text), 이 파일을 함께 고친다

export const RICH_TEXT_TAGS = [
  "p",
  "h2",
  "h3",
  "strong",
  "em",
  "s",
  "ul",
  "ol",
  "li",
  "blockquote",
  "a",
  "img",
  "br",
  "code",
  "pre",
  "hr",
]

/** 본문 이미지 주소의 시작. 백엔드 HtmlSanitizer.IMAGE_PATH_PREFIX와 같다 */
export const RICH_TEXT_IMAGE_PATH_PREFIX = "/api/v1/rich-text/images/"
// 접두사 뒤는 경로 글자만 (../, //, 쿼리·스킴 없이). 백엔드와 같은 규칙
const IMAGE_PATH = /^\/api\/v1\/rich-text\/images\/(?!.*\.\.)[A-Za-z0-9._~/-]+$/

/** 올릴 수 있는 이미지 형식과 크기. 백엔드 app.rich-text.image-max-size(기본 5MB)를 바꾸면 함께 바꾼다 */
export const RICH_TEXT_IMAGE_TYPES = ["image/png", "image/jpeg", "image/gif", "image/webp"]
export const RICH_TEXT_IMAGE_MAX_SIZE = 5 * 1024 * 1024

const LINK_PROTOCOLS = ["http:", "https:", "mailto:"]

export function isAllowedImageSrc(src: string | null | undefined): boolean {
  return !!src && IMAGE_PATH.test(src)
}

export function isAllowedLink(href: string | null | undefined): boolean {
  if (!href) return false
  try {
    return LINK_PROTOCOLS.includes(new URL(href).protocol)
  } catch {
    return false
  }
}

// 편집기 링크 칸에 적은 값 → 넣을 주소. 스킴이 없으면 https://를 붙인다 (example.com → https://example.com)
// http·https·mailto가 아니면 null
export function normalizeLink(input: string): string | null {
  const value = input.trim()
  if (!value) return null
  const href = /^[a-z][a-z0-9+.-]*:/i.test(value) ? value : `https://${value}`
  return isAllowedLink(href) ? href : null
}

// 올리기 전에 형식·크기를 본다 (서버도 파일 내용으로 다시 본다). 문제가 없으면 null
export function imageFileProblem(file: File): "imageType" | "imageTooLarge" | null {
  if (!RICH_TEXT_IMAGE_TYPES.includes(file.type)) return "imageType"
  if (file.size > RICH_TEXT_IMAGE_MAX_SIZE) return "imageTooLarge"
  return null
}

const PURIFY_CONFIG = {
  ALLOWED_TAGS: RICH_TEXT_TAGS,
  ALLOWED_ATTR: ["href", "src", "alt"],
  ALLOW_DATA_ATTR: false,
  ALLOW_ARIA_ATTR: false,
}

// 허용 목록으로 거른 HTML. 보기 부품(RichTextView)이 그리기 전에, 편집기가 붙여 넣은 HTML에 쓴다
// 저장된 HTML은 서버가 이미 걸렀다. 한 번 더 거르는 것은 서버 설정이 바뀌거나 다른 길로 들어온 값에 대한 대비다
//   - img: src가 본문 이미지 주소가 아니면 img째 지운다 (외부 주소, data:, ../)
//   - a: href가 http·https·mailto가 아니면 href를 지운다. target을 지우고 rel="noopener noreferrer"를 붙인다
// 브라우저에서만 거를 수 있다 (DOM이 필요하다). 서버에서 부르면 빈 문자열을 돌려준다
export function sanitizeRichText(html: string | null | undefined): string {
  if (!html || typeof window === "undefined" || !DOMPurify.isSupported) return ""
  const fragment = DOMPurify.sanitize(html, { ...PURIFY_CONFIG, RETURN_DOM_FRAGMENT: true })
  fragment.querySelectorAll("img").forEach((img) => {
    if (!isAllowedImageSrc(img.getAttribute("src"))) img.remove()
  })
  fragment.querySelectorAll("a").forEach((a) => {
    if (!isAllowedLink(a.getAttribute("href"))) a.removeAttribute("href")
    a.removeAttribute("target")
    a.setAttribute("rel", "noopener noreferrer")
  })
  const container = document.createElement("div")
  container.append(fragment)
  return container.innerHTML
}
