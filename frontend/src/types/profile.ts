// moa: 마이페이지 API의 응답·요청 (specs/009-mypage contracts/profile-api.md)
// 백엔드 ProfileGetResponse, ProfileSaveRequest, NicknameChangeRequest·Response, AvatarListResponse
// gen:api 뒤 Schema<"ProfileGetResponse"> 등으로 바꿔도 된다. 게임 정보(games)는 US5 때 더한다

export type Gender = "MALE" | "FEMALE" | "UNDISCLOSED"

export type DiscordVisibility = "PRIVATE" | "FRIENDS" | "MEMBERS"

export type PlayTime = { code: string; name: string }

export type MyProfile = {
  nickname: string
  // 프로필 행이 아직 없으면 null. 화면은 "닉네임#태그"로 보이고, 없으면 닉네임만
  nicknameTag: string | null
  avatar: string
  bio: string | null
  birthYear: number | null
  // 올해 − 출생 연도
  age: number | null
  gender: Gender | null
  ageGenderPublic: boolean
  discordTag: string | null
  discordVisibility: DiscordVisibility
  // 공통코드 순서. 꺼진 코드도 이름과 함께
  playTimes: PlayTime[]
  // 다음에 닉네임을 바꿀 수 있는 시각. 지금 바꿀 수 있으면 null
  nicknameChangeableAt: string | null
}

// 모든 칸을 보낸다. null이나 빈 배열은 비움
export type ProfileSaveRequest = {
  avatar: string | null
  bio: string | null
  birthYear: number | null
  gender: Gender | null
  ageGenderPublic: boolean
  discordTag: string | null
  discordVisibility: DiscordVisibility
  playTimes: string[]
}

export type NicknameChangeResponse = {
  nickname: string
  nicknameTag: string | null
  nicknameChangeableAt: string | null
}

export type AvatarList = { avatars: string[] }
