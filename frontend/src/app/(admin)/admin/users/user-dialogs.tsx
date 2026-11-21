"use client"

import { useQueryClient } from "@tanstack/react-query"
import { Copy } from "lucide-react"
import { useTranslations } from "next-intl"
import { toast } from "sonner"
import { toApiError } from "@/api/error"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { useErrorMessages } from "@/lib/error-messages"
import type { Role } from "@/types/user"

// 사용자 관리 화면의 공용 조각: 계정 종류 목록, 오류 문구, 임시 비밀번호 보이기 (만들기·고치기는 user-drawer.tsx의 드로어)
// moa: 이름 바꾸기·계정 종류 바꾸기 창은 없앴다. 드로어 한 폼에서 고친다 (specs/002-admin-users FR-007, 2026-11-22)

export const ROLES: Role[] = ["USER", "ADMIN"]

export function useInvalidateUsers() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: ["admin", "users"] })
}

// 서버 오류 문구. A005(ADMIN 보호)·B001(자기 자신)은 사용자 관리에 맞는 문구로
export function useUserErrorText() {
  const t = useTranslations("userAdmin")
  const { describeError } = useErrorMessages()
  return (error: unknown) => {
    const apiError = toApiError(error)
    if (apiError.code === "A005") return t("adminOnly")
    if (apiError.code === "B001") return t("notAllowed")
    return describeError(apiError).message
  }
}

// 임시 비밀번호: 이 창(또는 만들기 드로어)에서만 보인다. 복사 버튼을 둔다
export function TemporaryPasswordPanel({ value, loginId }: { value: string; loginId: string }) {
  const t = useTranslations("userAdmin")
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(value)
      toast.success(t("copied"))
    } catch {
      toast.error(t("copyFailed"))
    }
  }
  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-muted-foreground">{t("temporaryPasswordDescription", { loginId })}</p>
      <div className="flex items-center gap-2">
        <Input readOnly value={value} aria-label={t("temporaryPassword")} className="font-mono" />
        <Button variant="outline" onClick={() => void copy()}>
          <Copy />
          {t("copy")}
        </Button>
      </div>
      <Alert>
        <AlertDescription>{t("temporaryPasswordOnce")}</AlertDescription>
      </Alert>
    </div>
  )
}

export function TemporaryPasswordDialog({
  value,
  loginId,
  onClose,
}: {
  value?: string
  loginId?: string
  onClose: () => void
}) {
  const t = useTranslations("userAdmin")
  return (
    <Dialog open={!!value} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("temporaryPasswordTitle")}</DialogTitle>
          <DialogDescription className="sr-only">{t("temporaryPasswordTitle")}</DialogDescription>
        </DialogHeader>
        <TemporaryPasswordPanel value={value ?? ""} loginId={loginId ?? ""} />
        <DialogFooter>
          <Button onClick={onClose}>{t("done")}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
