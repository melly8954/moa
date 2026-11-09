import { create } from "zustand"

// 액세스 토큰은 브라우저 메모리에만 둔다 (localStorage에 두지 않는다)
// 새로고침하면 사라지고, (main) 레이아웃의 AuthGate가 리프레시 토큰 쿠키로 다시 받는다
//   unknown: 아직 확인 전 / signedIn: 토큰 있음 / signedOut: 로그인 필요 / expired: 재발급 실패 (안내 후 로그인으로)
// signedOutByUser: 사용자가 로그아웃을 눌렀다. 이때는 로그인 뒤 돌아갈 주소(?next=)를 남기지 않는다
export type AuthStatus = "unknown" | "signedIn" | "signedOut" | "expired"

type AuthState = {
  accessToken: string | null
  status: AuthStatus
  signedOutByUser: boolean
  signIn: (accessToken: string) => void
  signOut: (byUser?: boolean) => void
  expire: () => void
}

export const useAuthStore = create<AuthState>()((set) => ({
  accessToken: null,
  status: "unknown",
  signedOutByUser: false,
  signIn: (accessToken) => set({ accessToken, status: "signedIn", signedOutByUser: false }),
  signOut: (byUser = false) => set({ accessToken: null, status: "signedOut", signedOutByUser: byUser }),
  expire: () => set({ accessToken: null, status: "expired", signedOutByUser: false }),
}))
