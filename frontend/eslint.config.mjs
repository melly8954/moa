import { defineConfig, globalIgnores } from "eslint/config"
import nextVitals from "eslint-config-next/core-web-vitals"
import nextTs from "eslint-config-next/typescript"
import security from "eslint-plugin-security"

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  security.configs.recommended,
  {
    rules: {
      // 하이드레이션 가드 같은 통용 패턴과 부딪혀 오류가 아닌 경고로 둔다
      "react-hooks/set-state-in-effect": "warn",
      // 키가 정해진 객체 접근(문구 표 등)까지 잡아 경고가 대부분 오탐이다
      "security/detect-object-injection": "off",
    },
  },
  {
    // 개발용 스크립트는 정해진 폴더만 읽는다
    files: ["scripts/**"],
    rules: { "security/detect-non-literal-fs-filename": "off" },
  },
  globalIgnores([
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
    "playwright-report/**",
    "test-results/**",
    // shadcn 생성 코드. 고치지 않는다
    "src/components/ui/**",
    "src/hooks/use-mobile.ts",
    "src/hooks/use-highlighted-code.ts",
    "src/lib/highlighter.tsx",
    // npm run gen:api가 만든 타입
    "src/types/schema.d.ts",
  ]),
])

export default eslintConfig
