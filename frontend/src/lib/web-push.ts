import { api } from "@/api"

// 브라우저 알림(웹 푸시) 켜기·끄기 (specs/007-notifications US4, research R7·R8). moa에서 먼저 만들었다. 키트로 옮길 것
//   서비스 워커는 public/sw.js. 화면이 보이면 띄우지 않고, 누르면 알림의 link를 연다
const SW_URL = "/sw.js"

export type PushState = "unsupported" | "denied" | "off" | "on"

export function isPushSupported(): boolean {
  return (
    typeof window !== "undefined" && "serviceWorker" in navigator && "PushManager" in window && "Notification" in window
  )
}

async function currentSubscription(): Promise<PushSubscription | null> {
  if (!isPushSupported()) return null
  const registration = await navigator.serviceWorker.getRegistration(SW_URL)
  return (await registration?.pushManager.getSubscription()) ?? null
}

// 이 브라우저의 상태 (권한 거부는 브라우저 설정에서만 바꿀 수 있다)
export async function pushState(): Promise<PushState> {
  if (!isPushSupported()) return "unsupported"
  if (Notification.permission === "denied") return "denied"
  return (await currentSubscription()) ? "on" : "off"
}

// base64url 공개 키 → applicationServerKey
function toUint8Array(base64url: string): Uint8Array<ArrayBuffer> {
  const base64 = (base64url + "=".repeat((4 - (base64url.length % 4)) % 4)).replace(/-/g, "+").replace(/_/g, "/")
  const raw = atob(base64)
  const bytes = new Uint8Array(new ArrayBuffer(raw.length))
  for (let i = 0; i < raw.length; i++) bytes[i] = raw.charCodeAt(i)
  return bytes
}

// 켜기: 권한 요청 → 서비스 워커 등록 → 구독 → 서버 저장. 권한을 거부하면 "denied"
export async function enablePush(publicKey: string): Promise<PushState> {
  const permission = await Notification.requestPermission()
  if (permission !== "granted") return permission === "denied" ? "denied" : "off"
  const registration = await navigator.serviceWorker.register(SW_URL)
  await navigator.serviceWorker.ready
  const subscription = await registration.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: toUint8Array(publicKey),
  })
  const json = subscription.toJSON()
  await api.v1.notification.subscribePush({
    endpoint: subscription.endpoint,
    keys: { p256dh: json.keys?.p256dh ?? "", auth: json.keys?.auth ?? "" },
  })
  return "on"
}

// 끄기: 서버에서 지우고 브라우저 구독을 해제한다. 로그아웃 단추도 서버 로그아웃 전에 부른다
export async function disablePush(): Promise<void> {
  const subscription = await currentSubscription()
  if (!subscription) return
  await api.v1.notification.unsubscribePush(subscription.endpoint).catch(() => undefined)
  await subscription.unsubscribe()
}

// 브라우저 쪽 구독만 해제한다 (서버를 부를 수 없는 로그아웃 상태). 서버 행은 푸시 서버가 410을 돌려줄 때 지워진다
export async function unsubscribeBrowserOnly(): Promise<void> {
  const subscription = await currentSubscription().catch(() => null)
  await subscription?.unsubscribe().catch(() => undefined)
}
