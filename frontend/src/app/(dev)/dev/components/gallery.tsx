"use client"

import { Bold, Inbox, Italic, MoreHorizontal, Search, Underline } from "lucide-react"
import { useState } from "react"
import { Bar, BarChart, XAxis } from "recharts"
import { toast } from "sonner"
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { AspectRatio } from "@/components/ui/aspect-ratio"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb"
import { Bubble, BubbleContent } from "@/components/ui/bubble"
import { Button } from "@/components/ui/button"
import { ButtonGroup } from "@/components/ui/button-group"
import { Calendar } from "@/components/ui/calendar"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Carousel, CarouselContent, CarouselItem, CarouselNext, CarouselPrevious } from "@/components/ui/carousel"
import { type ChartConfig, ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart"
import { Checkbox } from "@/components/ui/checkbox"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from "@/components/ui/command"
import { ContextMenu, ContextMenuContent, ContextMenuItem, ContextMenuTrigger } from "@/components/ui/context-menu"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Drawer, DrawerContent, DrawerHeader, DrawerTitle, DrawerTrigger } from "@/components/ui/drawer"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Field, FieldDescription, FieldLabel } from "@/components/ui/field"
import { HoverCard, HoverCardContent, HoverCardTrigger } from "@/components/ui/hover-card"
import { Input } from "@/components/ui/input"
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group"
import { InputOTP, InputOTPGroup, InputOTPSlot } from "@/components/ui/input-otp"
import { Item, ItemContent, ItemDescription, ItemTitle } from "@/components/ui/item"
import { Kbd } from "@/components/ui/kbd"
import { Label } from "@/components/ui/label"
import { Marker, MarkerContent } from "@/components/ui/marker"
import { Menubar, MenubarContent, MenubarItem, MenubarMenu, MenubarTrigger } from "@/components/ui/menubar"
import { Message, MessageContent } from "@/components/ui/message"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import {
  NavigationMenu,
  NavigationMenuItem,
  NavigationMenuLink,
  NavigationMenuList,
} from "@/components/ui/navigation-menu"
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { Progress } from "@/components/ui/progress"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { ResizableHandle, ResizablePanel, ResizablePanelGroup } from "@/components/ui/resizable"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Separator } from "@/components/ui/separator"
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet"
import { Skeleton } from "@/components/ui/skeleton"
import { Slider } from "@/components/ui/slider"
import { Spinner } from "@/components/ui/spinner"
import { Switch } from "@/components/ui/switch"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Textarea } from "@/components/ui/textarea"
import { Toggle } from "@/components/ui/toggle"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip"
import { Trace, TraceDescription, TraceTitle } from "@/components/ui/trace"

// 이 화면은 부품이 테마·다크 모드에서 깨지지 않는지 보는 곳이다. 쓰는 법의 본보기가 아니다
// 겹쳐 뜨는 부품(대화상자, 서랍, 메뉴 등)은 여는 버튼만 보인다. 눌러서 확인한다
// Checkbox·Switch·Radio는 label로 감싸도 화면 낭독기 이름이 붙지 않는다(Base UI는 숨은 input에 붙인다). aria-label을 함께 준다

function Group({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="flex flex-col gap-4">
      <h2 className="border-b pb-2 text-lg font-semibold">{title}</h2>
      <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">{children}</div>
    </section>
  )
}

function Demo({ name, children }: { name: string; children: React.ReactNode }) {
  return (
    <div className="flex min-w-0 flex-col gap-3 rounded-lg border p-4">
      <p className="font-mono text-xs text-muted-foreground">{name}</p>
      <div className="flex min-w-0 flex-col items-start gap-3">{children}</div>
    </div>
  )
}

const chartConfig: ChartConfig = { total: { label: "매출", color: "var(--chart-2)" } }
const chartData = [
  { day: "11-01", total: 4 },
  { day: "11-02", total: 6 },
  { day: "11-03", total: 5 },
  { day: "11-04", total: 7 },
]

export function Gallery() {
  const [dark, setDark] = useState(false)
  const [date, setDate] = useState<Date | undefined>(new Date(2026, 10, 5))

  return (
    <div className={dark ? "dark" : undefined}>
      <main className="mx-auto flex min-h-svh max-w-7xl flex-col gap-10 bg-background p-4 text-foreground md:p-8">
        <header className="flex flex-wrap items-end justify-between gap-2">
          <div>
            <h1 className="text-2xl font-bold">전체 부품 확인</h1>
            <p className="text-sm text-muted-foreground">
              shadcn 부품 전부. 지금 모양(base-nova)은 임시 디자인이다. 개발 서버에서만 열린다.
            </p>
          </div>
          <Label>
            어두운 화면
            <Switch checked={dark} onCheckedChange={setDark} aria-label="어두운 화면" />
          </Label>
        </header>

        <Group title="버튼·입력">
          <Demo name="button">
            <div className="flex flex-wrap gap-2">
              <Button>기본</Button>
              <Button variant="secondary">보조</Button>
              <Button variant="outline">테두리</Button>
              <Button variant="ghost">고스트</Button>
              <Button variant="destructive">삭제</Button>
              <Button variant="link">링크</Button>
            </div>
          </Demo>
          <Demo name="button-group · toggle · toggle-group">
            <ButtonGroup>
              <Button variant="outline">이전</Button>
              <Button variant="outline">다음</Button>
            </ButtonGroup>
            <Toggle aria-label="굵게">
              <Bold />
            </Toggle>
            <ToggleGroup aria-label="글자 모양">
              <ToggleGroupItem value="b" aria-label="굵게">
                <Bold />
              </ToggleGroupItem>
              <ToggleGroupItem value="i" aria-label="기울임">
                <Italic />
              </ToggleGroupItem>
              <ToggleGroupItem value="u" aria-label="밑줄">
                <Underline />
              </ToggleGroupItem>
            </ToggleGroup>
          </Demo>
          <Demo name="input · label · field">
            <Field>
              <FieldLabel htmlFor="g-name">이름</FieldLabel>
              <Input id="g-name" placeholder="홍길동" />
              <FieldDescription>설명 글자</FieldDescription>
            </Field>
          </Demo>
          <Demo name="input-group">
            <InputGroup>
              <InputGroupAddon>
                <Search />
              </InputGroupAddon>
              <InputGroupInput aria-label="검색" placeholder="검색" />
            </InputGroup>
          </Demo>
          <Demo name="textarea">
            <Textarea aria-label="메모" placeholder="메모" />
          </Demo>
          <Demo name="checkbox · radio-group · switch">
            <Label>
              <Checkbox aria-label="동의" />
              동의
            </Label>
            <RadioGroup defaultValue="a" aria-label="선택">
              <Label>
                <RadioGroupItem value="a" aria-label="가" />가
              </Label>
              <Label>
                <RadioGroupItem value="b" aria-label="나" />나
              </Label>
            </RadioGroup>
            <Label>
              <Switch aria-label="사용" />
              사용
            </Label>
          </Demo>
          <Demo name="select · native-select">
            <Select
              defaultValue="paid"
              items={[
                { value: "paid", label: "결제 완료" },
                { value: "done", label: "완료" },
              ]}
            >
              <SelectTrigger className="w-44" aria-label="상태">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="paid">결제 완료</SelectItem>
                <SelectItem value="done">완료</SelectItem>
              </SelectContent>
            </Select>
            <NativeSelect aria-label="은행">
              <NativeSelectOption value="kb">국민은행</NativeSelectOption>
              <NativeSelectOption value="sh">신한은행</NativeSelectOption>
            </NativeSelect>
          </Demo>
          <Demo name="slider · input-otp">
            <Label className="w-48 flex-col items-start">
              음량
              <Slider defaultValue={[40]} className="w-full" />
            </Label>
            <InputOTP maxLength={4} aria-label="인증번호">
              <InputOTPGroup>
                <InputOTPSlot index={0} />
                <InputOTPSlot index={1} />
                <InputOTPSlot index={2} />
                <InputOTPSlot index={3} />
              </InputOTPGroup>
            </InputOTP>
          </Demo>
          <Demo name="calendar">
            <Calendar mode="single" selected={date} onSelect={setDate} defaultMonth={new Date(2026, 10)} />
          </Demo>
          <Demo name="command">
            <Command className="rounded-md border">
              <CommandInput placeholder="검색" />
              <CommandList>
                <CommandEmpty>결과 없음</CommandEmpty>
                <CommandGroup heading="메뉴">
                  <CommandItem>주문 목록</CommandItem>
                  <CommandItem>상품 목록</CommandItem>
                </CommandGroup>
              </CommandList>
            </Command>
          </Demo>
        </Group>

        <Group title="겹쳐 뜨는 것 (눌러서 확인)">
          <Demo name="dialog · alert-dialog">
            <Dialog>
              <DialogTrigger render={<Button variant="outline" />}>대화상자</DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>제목</DialogTitle>
                  <DialogDescription>설명</DialogDescription>
                </DialogHeader>
              </DialogContent>
            </Dialog>
            <AlertDialog>
              <AlertDialogTrigger render={<Button variant="destructive" />}>삭제 확인</AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>주문을 삭제할까요?</AlertDialogTitle>
                  <AlertDialogDescription>삭제하면 되돌릴 수 없습니다.</AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>취소</AlertDialogCancel>
                  <AlertDialogAction variant="destructive">삭제</AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          </Demo>
          <Demo name="sheet · drawer">
            <Sheet>
              <SheetTrigger render={<Button variant="outline" />}>시트</SheetTrigger>
              <SheetContent>
                <SheetHeader>
                  <SheetTitle>시트</SheetTitle>
                </SheetHeader>
              </SheetContent>
            </Sheet>
            <Drawer>
              <DrawerTrigger render={<Button variant="outline" />}>서랍</DrawerTrigger>
              <DrawerContent>
                <DrawerHeader>
                  <DrawerTitle>서랍</DrawerTitle>
                </DrawerHeader>
              </DrawerContent>
            </Drawer>
          </Demo>
          <Demo name="popover · hover-card · tooltip">
            <Popover>
              <PopoverTrigger render={<Button variant="outline" />}>팝오버</PopoverTrigger>
              <PopoverContent>팝오버 내용</PopoverContent>
            </Popover>
            <HoverCard>
              <HoverCardTrigger render={<Button variant="link" />}>마우스를 올리면</HoverCardTrigger>
              <HoverCardContent>호버 카드</HoverCardContent>
            </HoverCard>
            <Tooltip>
              <TooltipTrigger render={<Button variant="outline" />}>툴팁</TooltipTrigger>
              <TooltipContent>도움말</TooltipContent>
            </Tooltip>
          </Demo>
          <Demo name="dropdown-menu · context-menu">
            <DropdownMenu>
              <DropdownMenuTrigger render={<Button variant="outline" size="icon" aria-label="더 보기" />}>
                <MoreHorizontal />
              </DropdownMenuTrigger>
              <DropdownMenuContent>
                <DropdownMenuItem>수정</DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem variant="destructive">삭제</DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
            <ContextMenu>
              <ContextMenuTrigger className="rounded-md border border-dashed px-4 py-3 text-sm">
                오른쪽 클릭
              </ContextMenuTrigger>
              <ContextMenuContent>
                <ContextMenuItem>복사</ContextMenuItem>
              </ContextMenuContent>
            </ContextMenu>
          </Demo>
          <Demo name="menubar">
            <Menubar>
              <MenubarMenu>
                <MenubarTrigger>파일</MenubarTrigger>
                <MenubarContent>
                  <MenubarItem>새로 만들기</MenubarItem>
                </MenubarContent>
              </MenubarMenu>
              <MenubarMenu>
                <MenubarTrigger>편집</MenubarTrigger>
                <MenubarContent>
                  <MenubarItem>되돌리기</MenubarItem>
                </MenubarContent>
              </MenubarMenu>
            </Menubar>
          </Demo>
          <Demo name="sonner">
            <Button variant="outline" onClick={() => toast.success("저장했습니다.")}>
              토스트
            </Button>
          </Demo>
        </Group>

        <Group title="보여 주기">
          <Demo name="card">
            <Card className="w-full">
              <CardHeader>
                <CardTitle>카드 제목</CardTitle>
                <CardDescription>설명</CardDescription>
              </CardHeader>
              <CardContent className="text-sm">내용</CardContent>
              <CardFooter>
                <Button size="sm">확인</Button>
              </CardFooter>
            </Card>
          </Demo>
          <Demo name="alert">
            <Alert>
              <AlertTitle>안내</AlertTitle>
              <AlertDescription>설정이 아직 없습니다.</AlertDescription>
            </Alert>
            <Alert variant="destructive">
              <AlertTitle>오류</AlertTitle>
              <AlertDescription>불러오지 못했습니다.</AlertDescription>
            </Alert>
          </Demo>
          <Demo name="badge · avatar · kbd · spinner">
            <div className="flex flex-wrap items-center gap-2">
              <Badge>기본</Badge>
              <Badge variant="secondary">보조</Badge>
              <Badge variant="outline">테두리</Badge>
              <Badge variant="destructive">위험</Badge>
            </div>
            <div className="flex items-center gap-3">
              <Avatar>
                <AvatarFallback>관</AvatarFallback>
              </Avatar>
              <Kbd>Ctrl</Kbd>
              <Spinner />
            </div>
          </Demo>
          <Demo name="table">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>주문번호</TableHead>
                  <TableHead className="text-right">금액</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow>
                  <TableCell>ORD-120</TableCell>
                  <TableCell className="text-right tabular-nums">₩5,000</TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </Demo>
          <Demo name="empty">
            <Empty className="w-full border">
              <EmptyHeader>
                <EmptyMedia variant="icon">
                  <Inbox />
                </EmptyMedia>
                <EmptyTitle>아직 주문이 없습니다</EmptyTitle>
                <EmptyDescription>첫 주문을 등록해 보세요.</EmptyDescription>
              </EmptyHeader>
            </Empty>
          </Demo>
          <Demo name="skeleton · progress · separator">
            <Skeleton className="h-4 w-48" />
            <Skeleton className="h-4 w-32" />
            <Progress value={60} aria-label="진행" className="w-48" />
            <Separator />
          </Demo>
          <Demo name="item">
            <Item variant="outline" className="w-full">
              <ItemContent>
                <ItemTitle>주문 ORD-120</ItemTitle>
                <ItemDescription>2026-11-05 14:30</ItemDescription>
              </ItemContent>
            </Item>
          </Demo>
          <Demo name="aspect-ratio">
            <div className="w-48">
              <AspectRatio ratio={16 / 9} className="rounded-md bg-muted" />
            </div>
          </Demo>
          <Demo name="chart">
            <ChartContainer config={chartConfig} className="h-32 w-full">
              <BarChart data={chartData}>
                <XAxis dataKey="day" tickLine={false} axisLine={false} />
                <ChartTooltip content={<ChartTooltipContent />} />
                <Bar dataKey="total" fill="var(--color-total)" radius={4} />
              </BarChart>
            </ChartContainer>
          </Demo>
          <Demo name="carousel">
            <Carousel className="mx-12 w-40">
              <CarouselContent>
                {[1, 2, 3].map((n) => (
                  <CarouselItem key={n}>
                    <div className="flex h-24 items-center justify-center rounded-md border text-2xl">{n}</div>
                  </CarouselItem>
                ))}
              </CarouselContent>
              <CarouselPrevious />
              <CarouselNext />
            </Carousel>
          </Demo>
        </Group>

        <Group title="구조·이동">
          <Demo name="tabs">
            <Tabs defaultValue="a">
              <TabsList>
                <TabsTrigger value="a">기본 정보</TabsTrigger>
                <TabsTrigger value="b">이력</TabsTrigger>
              </TabsList>
              <TabsContent value="a" className="text-sm">
                기본 정보 내용
              </TabsContent>
              <TabsContent value="b" className="text-sm">
                이력 내용
              </TabsContent>
            </Tabs>
          </Demo>
          <Demo name="accordion · collapsible">
            <Accordion className="w-full">
              <AccordionItem value="a">
                <AccordionTrigger>배송 안내</AccordionTrigger>
                <AccordionContent>2~3일 걸립니다.</AccordionContent>
              </AccordionItem>
            </Accordion>
            <Collapsible>
              <CollapsibleTrigger render={<Button variant="ghost" size="sm" />}>상세 조건</CollapsibleTrigger>
              <CollapsibleContent className="text-sm">펼친 내용</CollapsibleContent>
            </Collapsible>
          </Demo>
          <Demo name="breadcrumb">
            <Breadcrumb>
              <BreadcrumbList>
                <BreadcrumbItem>
                  <BreadcrumbLink href="#">주문 관리</BreadcrumbLink>
                </BreadcrumbItem>
                <BreadcrumbSeparator />
                <BreadcrumbItem>
                  <BreadcrumbPage>주문 목록</BreadcrumbPage>
                </BreadcrumbItem>
              </BreadcrumbList>
            </Breadcrumb>
          </Demo>
          <Demo name="pagination">
            <Pagination>
              <PaginationContent>
                <PaginationItem>
                  <PaginationPrevious href="#" />
                </PaginationItem>
                <PaginationItem>
                  <PaginationLink href="#" isActive>
                    1
                  </PaginationLink>
                </PaginationItem>
                <PaginationItem>
                  <PaginationLink href="#">2</PaginationLink>
                </PaginationItem>
                <PaginationItem>
                  <PaginationEllipsis />
                </PaginationItem>
                <PaginationItem>
                  <PaginationNext href="#" />
                </PaginationItem>
              </PaginationContent>
            </Pagination>
          </Demo>
          <Demo name="navigation-menu">
            <NavigationMenu>
              <NavigationMenuList>
                <NavigationMenuItem>
                  <NavigationMenuLink href="#">주문</NavigationMenuLink>
                </NavigationMenuItem>
                <NavigationMenuItem>
                  <NavigationMenuLink href="#">상품</NavigationMenuLink>
                </NavigationMenuItem>
              </NavigationMenuList>
            </NavigationMenu>
          </Demo>
          <Demo name="scroll-area">
            <ScrollArea className="h-24 w-48 rounded-md border">
              <div className="p-3 text-sm">
                {Array.from({ length: 12 }, (_, i) => (
                  <p key={i}>줄 {i + 1}</p>
                ))}
              </div>
            </ScrollArea>
          </Demo>
          <Demo name="resizable">
            <ResizablePanelGroup orientation="horizontal" className="h-24 w-full rounded-md border">
              <ResizablePanel defaultSize="50%">
                <div className="p-3 text-sm">왼쪽</div>
              </ResizablePanel>
              <ResizableHandle withHandle />
              <ResizablePanel defaultSize="50%">
                <div className="p-3 text-sm">오른쪽</div>
              </ResizablePanel>
            </ResizablePanelGroup>
          </Demo>
          <Demo name="sidebar">
            <p className="text-sm text-muted-foreground">셸(사이드바형)에서 확인한다.</p>
          </Demo>
        </Group>

        <Group title="대화형 화면용 (AI 채팅 등)">
          <Demo name="message · bubble">
            <Message>
              <MessageContent>
                <Bubble>
                  <BubbleContent>안녕하세요. 무엇을 도와드릴까요?</BubbleContent>
                </Bubble>
              </MessageContent>
            </Message>
          </Demo>
          <Demo name="marker · trace">
            <Marker>
              <MarkerContent>오늘</MarkerContent>
            </Marker>
            <Trace>
              <TraceTitle>주문 조회</TraceTitle>
              <TraceDescription>3건 찾음</TraceDescription>
            </Trace>
          </Demo>
          <Demo name="그 밖 (여기서는 그리지 않음)">
            <p className="text-sm text-muted-foreground">
              attachment, code-block(shiki), message-scroller, questionnaire, toast(키트 알림은 sonner를 쓴다),
              direction
            </p>
          </Demo>
        </Group>
      </main>
    </div>
  )
}
