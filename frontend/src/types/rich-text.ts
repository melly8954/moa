// 본문 이미지 API 응답 (rich-text 헬퍼). 백엔드 RichTextImageUploadResponse
export type RichTextImage = {
  /** 이미지 ID (UUID) */
  id: string
  /** 본문 img src에 그대로 넣을 주소 (/api/v1/rich-text/images/{id}) */
  url: string
}
