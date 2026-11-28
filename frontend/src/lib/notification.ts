// 알림 표시 규칙 (specs/007-notifications contracts/notifications-screens.md)

// 벨 숫자: 0이면 비우고, 99를 넘으면 "99+"
export function unreadLabel(count: number): string {
  if (count <= 0) return ""
  return count > 99 ? "99+" : String(count)
}

import type { NotificationType } from "@/types/notification"

// 알림 링크는 moa 화면 경로만 연다 (/로 시작, //는 다른 사이트라 막는다)
export function safeLink(link: string): string {
  return link.startsWith("/") && !link.startsWith("//") ? link : "/"
}

// 개인 알림의 관련 화면을 보고 있으면 토스트를 띄우지 않는다 (spec FR-007, research R14)
//   기본: 알림의 연결 화면(쿼리·# 뺀 경로)을 보고 있으면 관련 화면이다
//   기능이 그 밖의 화면을 관련 화면으로 더할 때는 RELATED_SCREENS에 종류별 판정을 더한다 (그 기능의 스펙에 적는다)
type RelatedScreen = (link: string, pathname: string) => boolean

// moa: 파티 (specs/008-party spec.md FR-029 "관련 화면"). 내보내짐·해산은 연결(/recruit)에 더해 파티 화면도 관련 화면이다
//   파티 화면이 그 일을 직접 알리고 모집 페이지로 보낸다 (US9-AC8). 알림에 파티 id가 없어, 회원은 파티에 하나만 속하므로 파티 화면이면 관련 화면으로 본다
const onPartyScreen: RelatedScreen = (_link, pathname) => /^\/parties\/\d+\/?$/.test(pathname)

const RELATED_SCREENS: Partial<Record<NotificationType, RelatedScreen>> = {
  PARTY_KICKED: onPartyScreen,
  PARTY_DISSOLVED: onPartyScreen,
}

function pathOf(link: string): string {
  const path = safeLink(link).split(/[?#]/)[0]
  return path.length > 1 ? path.replace(/\/+$/, "") : path
}

export function isOnRelatedScreen(
  notification: { notificationType: NotificationType; link: string },
  pathname: string,
): boolean {
  if (pathOf(notification.link) === pathOf(pathname)) return true
  return RELATED_SCREENS[notification.notificationType]?.(notification.link, pathname) ?? false
}
