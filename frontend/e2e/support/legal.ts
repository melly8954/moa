import type { Page } from "@playwright/test"

// moa: 공개 문서 API 가로채기 (specs/012-terms-privacy contracts/api.md, research R8)
//   문서마다 old(예전), draft(지금), next(예정) 세 버전. 처리방침 draft에는 넓은 표가 있다

const VERSIONS = [
  { version: "next", effectiveDate: "2099-01-01", state: "UPCOMING" },
  { version: "draft", effectiveDate: "2026-11-01", state: "CURRENT" },
  { version: "old", effectiveDate: "2025-01-01", state: "PAST" },
] as const

const WIDE_TABLE =
  "<table><caption>보유 기간</caption><thead><tr>" +
  ["정보", "테이블", "기간", "끝나면", "상태", "근거"].map((h) => `<th scope="col">${h}</th>`).join("") +
  "</tr></thead><tbody><tr>" +
  ["접속 이력 (입력한 아이디, IP, 브라우저·기기)", "tb_login_history", "365일", "행 삭제", "구현됨", "통신비밀보호법"]
    .map((c) => `<td>${c}</td>`)
    .join("") +
  "</tr></tbody></table>"

export const LEGAL_TITLES = { terms: "moa 이용약관", privacy: "moa 개인정보처리방침" } as const

function content(document: "terms" | "privacy", version: string) {
  const effectiveDate = VERSIONS.find((v) => v.version === version)?.effectiveDate ?? ""
  const sections = [
    { id: "purpose", heading: "처리 목적", html: `<p>${document} ${version} 본문입니다.</p>` },
    ...Array.from({ length: 6 }, (_, i) => ({
      id: `chapter-${i + 1}`,
      heading: `장 ${i + 1}`,
      html: "<p>긴 문서라 아래로 넘긴다.</p>".repeat(8),
    })),
    {
      id: "retention",
      heading: "보유 기간",
      html: document === "privacy" && version === "draft" ? WIDE_TABLE : "<ul><li>365일</li></ul>",
    },
  ]
  return { version, effectiveDate, title: LEGAL_TITLES[document], sections }
}

export async function mockLegal(page: Page, { upcoming = true }: { upcoming?: boolean } = {}) {
  const versions = upcoming ? VERSIONS : VERSIONS.filter((v) => v.state !== "UPCOMING")
  await page.route(/\/api\/v1\/legal\/(terms|privacy)$/, (route) => {
    const document = new URL(route.request().url()).pathname.endsWith("terms") ? "terms" : "privacy"
    return route.fulfill({
      json: {
        document,
        current: content(document, "draft"),
        upcoming: upcoming ? { version: "next", effectiveDate: "2099-01-01" } : null,
        versions,
      },
    })
  })
  await page.route(/\/api\/v1\/legal\/(terms|privacy)\/versions\/[^/]+$/, (route) => {
    const [, , , , document, , version] = new URL(route.request().url()).pathname.split("/")
    const found = versions.find((v) => v.version === version)
    if (!found) {
      return route.fulfill({ status: 404, json: { code: "R001", message: "", timestamp: "", path: "" } })
    }
    return route.fulfill({
      json: {
        document,
        ...content(document as "terms" | "privacy", version),
        state: found.state,
        currentVersion: "draft",
      },
    })
  })
}
