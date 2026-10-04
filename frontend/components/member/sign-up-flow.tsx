"use client"

import { createContext, useContext, useState } from "react"

type PendingEmail = { email: string; password: string; resendAvailableAt: string }

type SignUpFlow = {
  /** 인증 메일을 보낸 이메일·비밀번호. 다시 보내기에 쓴다. 메모리에만 두고 새로고침하면 사라진다 */
  pendingEmail: PendingEmail | null
  setPendingEmail: (pending: PendingEmail | null) => void
}

const SignUpFlowContext = createContext<SignUpFlow | null>(null)

/** 가입 단계 사이에 넘길 값을 가입 화면 묶음 안에서만 둔다 */
export function SignUpFlowProvider({ children }: { children: React.ReactNode }) {
  const [pendingEmail, setPendingEmail] = useState<PendingEmail | null>(null)
  return <SignUpFlowContext.Provider value={{ pendingEmail, setPendingEmail }}>{children}</SignUpFlowContext.Provider>
}

export function useSignUpFlow() {
  const flow = useContext(SignUpFlowContext)
  if (!flow) {
    throw new Error("SignUpFlowProvider 안에서 써야 합니다")
  }
  return flow
}
