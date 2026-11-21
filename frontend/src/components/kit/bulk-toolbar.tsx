// 일괄 처리 줄 (moa, specs/001-admin-menus FR-010, specs/004-admin-codes FR-005). 표에서 체크한 것이 있을 때 표 위에 둔다
//   <BulkToolbar label={t("bulkToolbar")} selected={t("bulkSelected", { count })}>
//     <Button size="sm" variant="outline">사용</Button> …
//   </BulkToolbar>
//   note가 있으면 단추 대신 안내를 보인다 (예: 한 그룹씩만 처리할 때)
export function BulkToolbar({
  label,
  selected,
  note,
  children,
}: {
  label: string
  selected: string
  note?: React.ReactNode
  children: React.ReactNode
}) {
  return (
    <div
      role="toolbar"
      aria-label={label}
      className="flex min-h-11 flex-wrap items-center gap-2 rounded-lg border bg-muted px-3 py-2"
    >
      <span className="text-sm font-medium">{selected}</span>
      <span className="flex-1" />
      {note ? <span className="text-sm">{note}</span> : children}
    </div>
  )
}
