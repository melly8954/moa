import type { InquiryAuthor } from "@/types/inquiry"

// 작성자 글자: "이름 (아이디)". 회원 행의 개인정보가 지워졌으면(탈퇴 회원 파기) withdrawn 문구 (US5)
export function authorText(author: InquiryAuthor | null | undefined, withdrawn: string): string {
  if (!author?.name && !author?.loginId) return withdrawn
  if (author.name && author.loginId) return `${author.name} (${author.loginId})`
  return author.name ?? author.loginId ?? withdrawn
}
