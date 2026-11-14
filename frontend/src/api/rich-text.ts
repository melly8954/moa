import type { RichTextImage } from "@/types/rich-text"
import { http } from "./client"

// 본문 이미지 (rich-text 헬퍼). api.v1.richText로 부른다
//   uploadImage: 로그인한 사용자. png·jpg·gif·webp, 5MB 이하 (어기면 V001). 올린 이미지는 글을 저장할 때까지 임시다
//   보여 주기(GET /api/v1/rich-text/images/{id})는 img 태그가 바로 부른다 (로그인 없이 열린다)
export const richTextApi = {
  uploadImage: (file: File) => {
    const body = new FormData()
    body.append("file", file)
    return http.post<RichTextImage>("/api/v1/rich-text/images", body).then((res) => res.data)
  },
}
