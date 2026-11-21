// 브라우저 알림(웹 푸시) 서비스 워커 (specs/007-notifications research R7·R13). moa에서 먼저 만들었다. 키트로 옮길 것
// push: moa 화면이 보이는 상태면 띄우지 않는다 (화면에는 토스트가 뜬다). 아니면 moa 아이콘과 내용 없는 문구로 띄운다
//   잠금화면이나 남이 보는 화면에 뜰 수 있어 알림 제목·보낸 사람·내용은 보이지 않는다 (spec FR-008). 서버도 제목을 보내지 않는다
//   문구는 next-intl을 쓸 수 없어 여기 둔다
// notificationclick: 열린 moa 창이 있으면 앞으로 가져와 link로 옮기고, 없으면 새 창을 연다

const TITLE = "moa"
const ICON = "/icons/notification-192.png"
const BODY = { MESSAGE: "새 메시지가 있습니다" }
const DEFAULT_BODY = "새 알림이 있습니다"

self.addEventListener("push", (event) => {
  const data = event.data ? event.data.json() : {}
  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((clients) => {
      if (clients.some((client) => client.visibilityState === "visible")) return
      return self.registration.showNotification(TITLE, {
        body: BODY[data.type] || DEFAULT_BODY,
        icon: ICON,
        tag: String(data.id ?? ""),
        data: { link: data.link || "/" },
      })
    }),
  )
})

self.addEventListener("notificationclick", (event) => {
  event.notification.close()
  const link = (event.notification.data && event.notification.data.link) || "/"
  const url = new URL(link, self.location.origin).href
  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((clients) => {
      const client = clients.find((c) => new URL(c.url).origin === self.location.origin)
      if (client) return client.focus().then((focused) => focused.navigate(url))
      return self.clients.openWindow(url)
    }),
  )
})
