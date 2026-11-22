// moa: 기본 아바타 (specs/009-mypage FR-009, research R10)
//   그림은 public/avatars/<키>.svg. 같은 키로 파일만 바꿔 끼우면 그림이 바뀐다
//   키 목록의 정본은 서버(GET /api/v1/profile/avatars, DefaultAvatar)다. 여기 목록은 모르는 키를 default로 바꿀 때만 쓴다
export const DEFAULT_AVATAR = "default"

export const AVATAR_KEYS = [DEFAULT_AVATAR, "cat", "dog", "fox", "bear", "rabbit", "penguin", "robot"] as const

export type AvatarKey = (typeof AVATAR_KEYS)[number]

export function isAvatarKey(key: string | null | undefined): key is AvatarKey {
  return AVATAR_KEYS.includes(key as AvatarKey)
}

// 키 → 그림 주소. 모르는 키(목록에서 빠진 키 포함)는 default
export function avatarSrc(key: string | null | undefined): string {
  return `/avatars/${isAvatarKey(key) ? key : DEFAULT_AVATAR}.svg`
}
