import type { MouseEvent } from "react"

// 트리 표의 행을 눌러 상세를 연다 (moa, specs/001-admin-menus FR-010, specs/004-admin-codes FR-005)
//   <TableRow onClick={openOnRowClick(() => onOpen(menu))} className={ROW_CLICKABLE}>
//   행 안의 단추·입력(체크박스, 끌기 손잡이, 접기 단추, 이름 단추)을 누른 것은 그 단추가 처리하므로 건너뛴다
//   키보드는 행이 아니라 이름 단추로 연다 (행에 tabIndex를 두지 않는다)
const INTERACTIVE = "button, a, input, select, textarea, [role='checkbox']"

export function openOnRowClick(open: () => void) {
  return (event: MouseEvent<HTMLElement>) => {
    if (event.target instanceof Element && event.target.closest(INTERACTIVE)) return
    open()
  }
}

export const ROW_CLICKABLE = "cursor-pointer"
