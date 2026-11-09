import { cn } from "@/lib/utils"

// 화면 머리: 제목(h1), 설명, 오른쪽 버튼(등록 등)
// 셸 헤더는 위치(메뉴 이름)만 보이고, 화면 제목은 여기서 h1으로 둔다
export function PageHeader({
  title,
  description,
  actions,
  className,
}: {
  title: string
  description?: string
  actions?: React.ReactNode
  className?: string
}) {
  return (
    <div className={cn("flex flex-wrap items-end justify-between gap-3", className)}>
      <div className="flex min-w-0 flex-col gap-1">
        <h1 className="text-xl font-semibold">{title}</h1>
        {description && <p className="text-sm text-muted-foreground">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  )
}

// 목록 화면 틀: 화면 머리 → 검색 영역 → 표 (간격은 --page-gap)
//   <ListPage title={t("title")} actions={<Button>등록</Button>} search={<SearchPanel …/>}>
//     <DataTable … />
//   </ListPage>
export function ListPage({
  title,
  description,
  actions,
  search,
  children,
}: {
  title: string
  description?: string
  actions?: React.ReactNode
  search?: React.ReactNode
  children: React.ReactNode
}) {
  return (
    <div className="flex flex-col gap-(--page-gap)">
      <PageHeader title={title} description={description} actions={actions} />
      {search}
      {children}
    </div>
  )
}
