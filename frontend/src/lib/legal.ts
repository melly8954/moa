import DOMPurify from "dompurify"
import type { LegalDocumentKey } from "@/types/legal"

// moa: 이용약관·개인정보처리방침 (specs/012-terms-privacy research R3)
// 문안은 백엔드 리소스 파일이고 공개 API로 온다. 고정 문안이지만 그리기 전에 한 번 거른다
// 서식 있는 글(lib/rich-text.ts)과 허용 목록이 다르다: 표를 허용하고 이미지를 막는다 (회원이 쓰는 글에는 표를 열지 않는다)

export const LEGAL_TAGS = [
  "p",
  "h3",
  "ul",
  "ol",
  "li",
  "a",
  "strong",
  "em",
  "br",
  "table",
  "thead",
  "tbody",
  "tr",
  "th",
  "td",
  "caption",
]

const PURIFY_CONFIG = {
  ALLOWED_TAGS: LEGAL_TAGS,
  ALLOWED_ATTR: ["href", "colspan", "rowspan", "scope"],
  ALLOW_DATA_ATTR: false,
  ALLOW_ARIA_ATTR: false,
}

// 문안 안의 링크: 같은 사이트 주소(/…, #…)와 http·https·mailto만 남긴다
function isAllowedHref(href: string | null): boolean {
  if (!href) return false
  if (href.startsWith("#") || (href.startsWith("/") && !href.startsWith("//"))) return true
  try {
    return ["http:", "https:", "mailto:"].includes(new URL(href).protocol)
  } catch {
    return false
  }
}

// 브라우저에서만 거를 수 있다 (DOM이 필요하다). 서버에서 부르면 빈 문자열
export function sanitizeLegalHtml(html: string | null | undefined): string {
  if (!html || typeof window === "undefined" || !DOMPurify.isSupported) return ""
  const fragment = DOMPurify.sanitize(html, { ...PURIFY_CONFIG, RETURN_DOM_FRAGMENT: true })
  fragment.querySelectorAll("a").forEach((a) => {
    const href = a.getAttribute("href")
    if (!isAllowedHref(href)) a.removeAttribute("href")
    // 바깥 주소는 새 탭에서 연다
    if (href && /^(https?|mailto):/i.test(href)) {
      a.setAttribute("target", "_blank")
      a.setAttribute("rel", "noopener noreferrer")
    }
  })
  // 표는 좁은 폭에서 표 안에서만 가로로 넘긴다. 키보드로도 넘길 수 있게 초점을 받는다
  fragment.querySelectorAll("table").forEach((table) => table.setAttribute("tabindex", "0"))
  const container = document.createElement("div")
  container.append(fragment)
  return container.innerHTML
}

// 문서 화면 주소
export function legalPath(document: LegalDocumentKey, version?: string): string {
  return version ? `/${document}/versions/${encodeURIComponent(version)}` : `/${document}`
}

// "YYYY-MM-DD" → 문구에 끼울 년·월·일 (한국 날짜 그대로. 시간대로 바꾸지 않는다)
export function dateParts(date: string): { year: number; month: number; day: number } {
  const [year, month, day] = date.split("-").map(Number)
  return { year, month, day }
}
