// 본문 모양 (편집기와 보기 부품이 함께 쓴다). 색은 테마 변수 클래스로만 쓴다
// 링크는 글자색을 따르고 밑줄로 구분한다 (primary는 배경용 색이라 테마에 따라 글자로 쓰면 대비가 모자란다)
// 디자인을 정하면 여기 한 곳을 바꾼다
export const RICH_TEXT_CONTENT_CLASS = [
  "text-sm leading-relaxed break-words",
  "[&_p]:my-2 [&_h2]:mt-6 [&_h2]:mb-2 [&_h2]:text-xl [&_h2]:font-semibold [&_h3]:mt-4 [&_h3]:mb-2 [&_h3]:text-lg [&_h3]:font-semibold",
  "[&_ol]:my-2 [&_ol]:list-decimal [&_ol]:pl-6 [&_ul]:my-2 [&_ul]:list-disc [&_ul]:pl-6 [&_li_p]:my-0.5",
  "[&_blockquote]:my-2 [&_blockquote]:border-l-2 [&_blockquote]:border-border [&_blockquote]:pl-4 [&_blockquote]:text-muted-foreground",
  "[&_a]:font-medium [&_a]:underline [&_a]:underline-offset-4",
  "[&_code]:rounded-sm [&_code]:bg-muted [&_code]:px-1 [&_code]:py-0.5 [&_code]:font-mono [&_code]:text-[0.9em]",
  "[&_pre]:my-2 [&_pre]:overflow-x-auto [&_pre]:rounded-md [&_pre]:bg-muted [&_pre]:p-3 [&_pre_code]:bg-transparent [&_pre_code]:p-0",
  "[&_hr]:my-4 [&_hr]:border-border [&_img]:my-2 [&_img]:h-auto [&_img]:max-w-full [&_img]:rounded-md",
  "[&>:first-child]:mt-0 [&>:last-child]:mb-0",
].join(" ")
