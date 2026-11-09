// 백엔드 API 설명서(OpenAPI)에서 요청·응답 타입을 만든다 → src/types/schema.d.ts (커밋한다)
// 로컬 백엔드를 띄운 뒤 npm run gen:api. 백엔드는 local 프로필에서만 /v3/api-docs를 연다
// 주소: 인자 > OPENAPI_URL > API_BASE_URL(.env.local)/v3/api-docs > http://localhost:8080/v3/api-docs
// 타입만 만든다. 화면은 src/types/openapi.ts의 Schema<"이름">으로 가져오고, 폼 검증은 Zod가 따로 맡는다
import { existsSync, writeFileSync } from "node:fs"
import { fileURLToPath } from "node:url"
import openapiTS, { astToString } from "openapi-typescript"

const ROOT = fileURLToPath(new URL("..", import.meta.url))
const OUT = new URL("../src/types/schema.d.ts", import.meta.url)
const HEADER = "// npm run gen:api가 만든 파일이다. 손으로 고치지 않는다 (scripts/gen-api.mjs)\n\n"

if (existsSync(`${ROOT}.env.local`)) process.loadEnvFile(`${ROOT}.env.local`)
const base = process.env.API_BASE_URL || "http://localhost:8080"
const source = process.argv[2] ?? process.env.OPENAPI_URL ?? `${base}/v3/api-docs`

try {
  const ast = await openapiTS(/^https?:/.test(source) ? new URL(source) : new URL(source, `file:///${ROOT}`))
  writeFileSync(OUT, HEADER + astToString(ast))
  console.log(`타입을 만들었다: ${source} → src/types/schema.d.ts`)
} catch (error) {
  console.error(`API 설명서를 읽지 못했다: ${source}\n로컬 백엔드가 local 프로필로 떠 있는지 본다\n`, error)
  process.exit(1)
}
