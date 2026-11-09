import { z } from "zod"

// 메뉴 폼. 규칙은 백엔드 MenuCreateRequest·MenuModifyRequest와 같다
//   이름 50자까지 / 경로: 비우거나(그룹) "/"로 시작하는 내부 경로, 200자까지 (외부 주소 안 됨)
//   정렬 순서 0 이상 / 권한 키 100자까지 (비우면 로그인한 사용자 모두)
// parentId는 선택 상자 값이라 문자열이다 ("" = 1단계)
export const menuSchema = z.object({
  parentId: z.string(),
  name: z.string().trim().min(1, "menuAdmin.nameRequired").max(50, "menuAdmin.nameMax"),
  path: z
    .string()
    .trim()
    .max(200, "menuAdmin.pathMax")
    .refine((v) => v === "" || (v.startsWith("/") && !v.startsWith("//")), "menuAdmin.pathPattern"),
  icon: z.string(),
  sortOrder: z
    .string()
    .trim()
    .regex(/^\d+$/, "menuAdmin.sortOrderInvalid")
    .transform((v) => Number(v)),
  permission: z.string().trim().max(100, "menuAdmin.permissionMax"),
  enabled: z.boolean(),
})

export type MenuFormInput = z.input<typeof menuSchema>
export type MenuFormOutput = z.output<typeof menuSchema>
