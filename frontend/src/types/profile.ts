import type { Schema } from "./openapi"

// moa: 마이페이지 API의 응답·요청 (specs/009-mypage contracts/profile-api.md). npm run gen:api의 생성 타입을 쓴다
// 응답의 null 가능 칸(@Nullable)은 `field?: T`로 온다. 화면은 null과 빠짐을 같게 본다 (`?? ""`, `!!value`)
// 게임 정보(games)는 US5 때 더한다

// GET·PUT /api/v1/users/me/profile
export type MyProfile = Schema<"ProfileGetResponse">

export type PlayTime = Schema<"PlayTime">

export type Gender = NonNullable<MyProfile["gender"]>

export type DiscordVisibility = MyProfile["discordVisibility"]

// PUT /api/v1/users/me/profile. 모든 칸을 보낸다. null이나 빈 배열은 비움
// 서버는 gender·discordVisibility를 문자열로 받아 칸 오류(V001)로 확인한다. 화면은 목록 값만 보낸다
type SaveFields = Required<Schema<"ProfileSaveRequest">>
export type ProfileSaveRequest = { [K in keyof SaveFields]: SaveFields[K] | null } & {
  gender: Gender | null
  ageGenderPublic: boolean
  discordVisibility: DiscordVisibility
  playTimes: string[]
}

// PUT /api/v1/users/me/nickname
export type NicknameChangeResponse = Schema<"NicknameChangeResponse">

// GET /api/v1/profile/avatars
export type AvatarList = Schema<"AvatarListResponse">
