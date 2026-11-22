import { avatarSrc } from "@/lib/avatars"
import { cn } from "@/lib/utils"

// moa: 기본 아바타 그림 (specs/009-mypage FR-009·FR-014). 계정 바와 마이페이지가 같이 쓴다
//   그림은 public/avatars/<키>.svg라 최적화할 것이 없어 img를 그대로 쓴다. 모르는 키는 default
//   alt를 비우면(기본) 꾸밈 그림으로 본다. 옆에 이름이 없을 때만 alt를 넘긴다
export function AvatarImage({
  avatar,
  alt = "",
  className,
}: {
  avatar?: string | null
  alt?: string
  className?: string
}) {
  return (
    // eslint-disable-next-line @next/next/no-img-element -- 정적 SVG
    <img
      src={avatarSrc(avatar)}
      alt={alt}
      draggable={false}
      className={cn("size-7 shrink-0 rounded-full", className)}
    />
  )
}
