import { z } from "zod"

// 메뉴 폼. 규칙은 백엔드 MenuCreateRequest·MenuModifyRequest와 같다
//   이름 50자까지
//   moa: "종류" 칸은 없다. 연결 화면(programKey)을 고르면 화면 메뉴, 비우면 상위 메뉴다 (specs/001-admin-menus FR-009)
//   moa: 상위 메뉴(parentId, "" = 맨 위) 아래에 둔 메뉴는 연결 화면을 골라야 한다. 메뉴는 2단계까지다
//   공개 범위는 사용자 화면 메뉴만 보낸다 (FR-007). 로그인이 필요한 프로그램의 "비회원 포함"은 화면이 막고 서버가 다시 본다
export const menuSchema = z
  .object({
    parentId: z.string(),
    name: z.string().trim().min(1, "menuAdmin.nameRequired").max(50, "menuAdmin.nameMax"),
    programKey: z.string(),
    icon: z.string(),
    visibility: z.enum(["PUBLIC", "MEMBER"]),
    enabled: z.boolean(),
  })
  .refine((v) => v.parentId === "" || v.programKey !== "", {
    path: ["programKey"],
    message: "menuAdmin.programRequired",
  })

export type MenuFormInput = z.input<typeof menuSchema>
export type MenuFormOutput = z.output<typeof menuSchema>
