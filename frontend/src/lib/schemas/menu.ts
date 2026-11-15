import { z } from "zod"

// 메뉴 폼. 규칙은 백엔드 MenuCreateRequest·MenuModifyRequest와 같다
//   이름 50자까지 / 정렬 순서 0 이상
//   moa: 종류가 화면 메뉴면 프로그램을 골라야 한다. 묶음 메뉴는 프로그램이 없다 (specs/001-admin-menus FR-009)
//   공개 범위는 사용자 화면 메뉴만 보낸다 (FR-007). 로그인이 필요한 프로그램의 "비회원 포함"은 화면이 막고 서버가 다시 본다
// parentId는 선택 상자 값이라 문자열이다 ("" = 1단계)
export const menuSchema = z
  .object({
    parentId: z.string(),
    name: z.string().trim().min(1, "menuAdmin.nameRequired").max(50, "menuAdmin.nameMax"),
    kind: z.enum(["screen", "group"]),
    programKey: z.string(),
    icon: z.string(),
    sortOrder: z
      .string()
      .trim()
      .regex(/^\d+$/, "menuAdmin.sortOrderInvalid")
      .transform((v) => Number(v)),
    visibility: z.enum(["PUBLIC", "MEMBER"]),
    enabled: z.boolean(),
  })
  .refine((v) => v.kind === "group" || v.programKey !== "", {
    path: ["programKey"],
    message: "menuAdmin.programRequired",
  })

export type MenuFormInput = z.input<typeof menuSchema>
export type MenuFormOutput = z.output<typeof menuSchema>
