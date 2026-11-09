import type ko from "../../messages/ko.json"
import type { Locale } from "./config"

// 문구 키를 타입으로 검사한다. ko.json에 없는 키를 t("...")로 부르면 typecheck가 실패한다
declare module "next-intl" {
  interface AppConfig {
    Locale: Locale
    Messages: typeof ko
  }
}
