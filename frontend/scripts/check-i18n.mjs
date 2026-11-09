// 문구 검사
//   1. 화면 코드(src)에 한글 글자를 직접 적지 않는다. 글자는 messages/ko.json에 키로 두고 t("키")로 쓴다
//      (주석은 보지 않는다. shadcn 생성 코드와 개발용 화면 (dev)는 보지 않는다)
//   2. messages/<언어>.json은 ko.json과 키가 같아야 한다. 빠진 키는 실행할 때 한국어로 보이지만 여기서 실패한다
import { readdirSync, readFileSync, statSync } from "node:fs"
import { join, relative } from "node:path"
import { fileURLToPath } from "node:url"

const ROOT = fileURLToPath(new URL("..", import.meta.url))
const SRC = join(ROOT, "src")
const MESSAGES = join(ROOT, "messages")
const SKIP = [
  join(SRC, "components", "ui"),
  join(SRC, "hooks", "use-highlighted-code.ts"),
  join(SRC, "lib", "highlighter.tsx"),
  join(SRC, "app", "(dev)"),
]
const HANGUL = /[ㄱ-ㆎ가-힣]/

function* files(dir) {
  for (const name of readdirSync(dir)) {
    const path = join(dir, name)
    if (SKIP.includes(path)) continue
    if (statSync(path).isDirectory()) yield* files(path)
    else if (/\.(tsx?|jsx?)$/.test(name) && !name.endsWith(".d.ts")) yield path
  }
}

// 주석을 지운다: /* */ 덩어리, 그리고 줄 끝 // (앞이 ':'이면 주소라서 남긴다)
function stripComments(text) {
  return text
    .replace(/\/\*[\s\S]*?\*\//g, (block) => block.replace(/[^\n]/g, " "))
    .split("\n")
    .map((line) => line.replace(/(^|[^:])\/\/.*$/, "$1"))
}

const problems = []

for (const file of files(SRC)) {
  stripComments(readFileSync(file, "utf8")).forEach((line, i) => {
    if (HANGUL.test(line))
      problems.push(`${relative(ROOT, file)}:${i + 1}: 한글을 직접 적었다 → messages/ko.json 키로: ${line.trim()}`)
  })
}

function keys(object, prefix = "") {
  return Object.entries(object).flatMap(([key, value]) =>
    typeof value === "object" && value !== null ? keys(value, `${prefix}${key}.`) : [`${prefix}${key}`],
  )
}

const base = new Set(keys(JSON.parse(readFileSync(join(MESSAGES, "ko.json"), "utf8"))))
for (const name of readdirSync(MESSAGES).filter((n) => n.endsWith(".json") && n !== "ko.json")) {
  const other = new Set(keys(JSON.parse(readFileSync(join(MESSAGES, name), "utf8"))))
  for (const key of base) if (!other.has(key)) problems.push(`messages/${name}: 빠진 키 ${key}`)
  for (const key of other) if (!base.has(key)) problems.push(`messages/${name}: ko.json에 없는 키 ${key}`)
}

if (problems.length > 0) {
  console.error("문구 검사 실패:\n" + problems.join("\n"))
  process.exit(1)
}
console.log("문구 검사 통과")
