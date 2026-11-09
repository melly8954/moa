import type { VariantProps } from "class-variance-authority"
import Link from "next/link"
import { buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"

// 버튼 모양의 링크. 화면을 옮기는 것은 이것을 쓴다 (화면 낭독기에 "링크"로 읽힌다)
//   <LinkButton href="/orders/new">등록</LinkButton>
// <Button render={<Link />} nativeButton={false}>는 쓰지 않는다: Base UI가 role="button"을 붙여 "버튼"으로 읽힌다
export function LinkButton({
  variant,
  size,
  className,
  ...props
}: React.ComponentProps<typeof Link> & VariantProps<typeof buttonVariants>) {
  return <Link className={cn(buttonVariants({ variant, size }), className)} {...props} />
}
