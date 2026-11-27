"use client"

import { useTranslations } from "next-intl"

// 배너 미리보기 (specs/016-banner-popup FR-003, US1-AC10, research R2)
//   관리자 계정은 사용자 화면을 열 수 없어 이 미리보기로 모양을 본다
//   넓은 화면 틀(4:1)과 좁은 화면 틀(휴대폰 폭, 2:1)에 이미지를 채워 나란히 보인다
//   좁은 화면 이미지가 없으면 좁은 틀에 넓은 화면 이미지를 넣고 그렇다고 알린다
//   노출 자리가 정해지면 그 자리의 틀로 바꾼다 (FR-011)
export function BannerPreview({
  wideImageUrl,
  narrowImageUrl,
  altText,
}: {
  wideImageUrl: string
  narrowImageUrl: string
  altText: string
}) {
  const t = useTranslations("banner")
  const narrowSource = narrowImageUrl || wideImageUrl
  return (
    <section aria-label={t("preview")} className="flex flex-col gap-2">
      <div className="flex flex-wrap items-end gap-3">
        <Frame label={t("previewWide")} src={wideImageUrl} alt={altText} className="aspect-[4/1] w-full max-w-sm" />
        <Frame label={t("previewNarrow")} src={narrowSource} alt={altText} className="aspect-[2/1] w-36" />
      </div>
      {wideImageUrl === "" ? (
        <p className="text-sm text-muted-foreground">{t("previewEmpty")}</p>
      ) : (
        narrowImageUrl === "" && <p className="text-sm text-muted-foreground">{t("previewNarrowFallback")}</p>
      )}
    </section>
  )
}

function Frame({ label, src, alt, className }: { label: string; src: string; alt: string; className: string }) {
  return (
    <figure className="flex min-w-0 flex-col gap-1">
      <figcaption className="text-xs text-muted-foreground">{label}</figcaption>
      <div className={`overflow-hidden rounded-md border bg-muted ${className}`}>
        {src && (
          // 미리보기는 올린 원본을 그대로 보인다 (next/image 최적화를 거치지 않는다)
          // eslint-disable-next-line @next/next/no-img-element
          <img src={src} alt={alt} className="size-full object-cover" />
        )}
      </div>
    </figure>
  )
}
