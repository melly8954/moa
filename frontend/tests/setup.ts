// DOM 테스트(*.dom.test.ts(x))에만 붙는다 (vitest.config.mts "dom")
import "@testing-library/jest-dom/vitest"
import { cleanup } from "@testing-library/react"
import { afterEach } from "vitest"

afterEach(() => cleanup())
