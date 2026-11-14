// 알림 표시 규칙 (specs/007-notifications contracts/notifications-screens.md)

// 벨 숫자: 0이면 비우고, 99를 넘으면 "99+"
export function unreadLabel(count: number): string {
  if (count <= 0) return ""
  return count > 99 ? "99+" : String(count)
}

// 알림 링크는 moa 화면 경로만 연다 (/로 시작, //는 다른 사이트라 막는다)
export function safeLink(link: string): string {
  return link.startsWith("/") && !link.startsWith("//") ? link : "/"
}
