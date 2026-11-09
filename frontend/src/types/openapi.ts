import type { components } from "./schema"

// 백엔드 API 설명서에서 만든 요청·응답 타입 (npm run gen:api → schema.d.ts)
//   type UserAdminResponse = Schema<"UserAdminResponse">
// 백엔드 칸은 기본 필수다. @Nullable 칸만 `field?: T`로 오고, null이거나 빠질 수 있다
// 페이징 목록은 생성 타입(PageResponseUserAdminResponse 등)을 쓰지 않고 PageResponse<Schema<"항목">>로 적는다 (types/api.ts)
// 골격의 schema.d.ts는 빈 설명서로 만든 자리다. 프로젝트에서 백엔드를 띄우고 처음 한 번 만든다
export type Schema<Name extends keyof components["schemas"]> = components["schemas"][Name]
