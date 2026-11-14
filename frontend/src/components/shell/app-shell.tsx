// 셸 형태를 고른다. apply.sh --shell sidebar|topnav가 이 줄을 바꾸고 쓰지 않는 셸 파일을 지운다
// moa: 사이드바형 셸 하나를 관리 셸·사용자 셸 두 벌로 쓴다 (specs/001-admin-menus FR-005, 키트로 옮길 것)
export { AccountShell } from "./account-shell"
export { AdminShell } from "./admin-shell"
export { UserShell } from "./user-shell"
