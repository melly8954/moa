// 생성 코드 목록: 검사에서 빼는 파일·폴더의 원천이다. 손으로 고치지 않는 코드라 검사하지 않는다
// eslint.config.mjs, scripts/check-colors.mjs, scripts/check-i18n.mjs가 이 목록을 읽는다
// .prettierignore는 다른 파일을 읽지 못해 같은 항목을 따로 적는다. 빠뜨리면 이 스크립트가 실패한다 (npm run check)
//   node scripts/generated-files.mjs
// 검사마다 따로 빼는 것(문구 검사의 (dev) 화면 등)은 이 목록이 아니라 그 검사에 둔다
import { readFileSync } from "node:fs"
import { fileURLToPath } from "node:url"

// 저장소 루트 기준 경로. 폴더는 끝에 /를 붙이지 않는다
export const GENERATED = [
  // shadcn 생성 코드
  "src/components/ui",
  "src/hooks/use-mobile.ts",
  "src/hooks/use-highlighted-code.ts",
  "src/lib/highlighter.tsx",
  // npm run gen:api가 만든 API 타입
  "src/types/schema.d.ts",
  // 프로젝트가 더하는 생성 코드는 이 아래에 적는다 (.prettierignore에도 같이 적는다)
]

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const ignored = new Set(
    readFileSync(new URL("../.prettierignore", import.meta.url), "utf8")
      .split("\n")
      .map((line) => line.trim().replace(/\/$/, ""))
      .filter((line) => line && !line.startsWith("#")),
  )
  const missing = GENERATED.filter((path) => !ignored.has(path))
  if (missing.length > 0) {
    console.error("생성 코드 목록(scripts/generated-files.mjs)에 있는데 .prettierignore에 없다:")
    for (const path of missing) console.error(`  ${path}`)
    process.exit(1)
  }
}
