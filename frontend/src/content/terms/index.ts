import type { TermsType } from "@/types/oauth"
import draft from "./draft.json"

// moa: 약관 문안 (specs/010-social-login FR-013, research R11)
// 버전마다 JSON 파일 하나. 서버(GET /api/v1/terms)가 준 버전의 문안을 고른다
// 문안은 길고 법무 문서라 messages/ko.json에 넣지 않는다. JSON이라 문구 검사(check:i18n) 대상도 아니다
// (잠정) draft는 자리표시다. 배포 전에 팀이 준비한 문안과 버전으로 바꾼다 (tasks T056)
export type TermsText = { summary: string; body: string }

const VERSIONS: Record<string, Record<TermsType, TermsText>> = {
  draft,
}

// 없는 버전이면 undefined (화면은 가입을 막는다)
export function termsText(type: TermsType, version: string): TermsText | undefined {
  return VERSIONS[version]?.[type]
}
