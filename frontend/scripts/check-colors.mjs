// 색 직접 클래스 검사: 색은 테마 변수 클래스(bg-primary, text-muted-foreground 등)로만 쓴다
// Tailwind 팔레트 색(text-red-500, bg-zinc-50, bg-white …)과 hex·rgb 인라인을 찾으면 실패한다
// shadcn 생성 코드(src/components/ui, 코드 강조 hook·lib)와 테마 파일은 보지 않는다
import { readdirSync, readFileSync, statSync } from "node:fs"
import { join, relative } from "node:path"
import { fileURLToPath } from "node:url"

const ROOT = fileURLToPath(new URL("..", import.meta.url))
const SRC = join(ROOT, "src")
const SKIP = [
  join(SRC, "components", "ui"),
  join(SRC, "hooks", "use-highlighted-code.ts"),
  join(SRC, "lib", "highlighter.tsx"),
]

const PALETTE =
  "slate|gray|zinc|neutral|stone|red|orange|amber|yellow|lime|green|emerald|teal|cyan|sky|blue|indigo|violet|purple|fuchsia|pink|rose"
const PATTERNS = [
  new RegExp(
    `\\b(?:bg|text|border|ring|outline|fill|stroke|from|via|to|divide|placeholder|decoration|shadow|accent|caret)-(?:${PALETTE})-\\d{2,3}\\b`,
  ),
  /\b(?:bg|text|border|ring|fill|stroke)-(?:white|black)\b/,
  /#[0-9a-fA-F]{3,8}\b(?=["'`;\s)])/,
  /\brgba?\(/,
]

function* files(dir) {
  for (const name of readdirSync(dir)) {
    const path = join(dir, name)
    if (SKIP.includes(path)) continue
    if (statSync(path).isDirectory()) yield* files(path)
    else if (/\.(tsx?|jsx?)$/.test(name)) yield path
  }
}

const found = []
for (const file of files(SRC)) {
  readFileSync(file, "utf8")
    .split("\n")
    .forEach((line, i) => {
      if (PATTERNS.some((p) => p.test(line))) found.push(`${relative(ROOT, file)}:${i + 1}: ${line.trim()}`)
    })
}

if (found.length > 0) {
  console.error("색 직접 지정이 있다. 테마 변수 클래스로 바꾼다:\n" + found.join("\n"))
  process.exit(1)
}
console.log("색 검사 통과")
