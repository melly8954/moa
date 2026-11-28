import type { AvatarList, MyProfile, NicknameChangeResponse, ProfileSaveRequest } from "@/types/profile"
import { http } from "./client"

// moa: 마이페이지 (specs/009-mypage contracts/profile-api.md). api.v1.profile로 부른다
//   get·save: 내 프로필 (닉네임·게임 정보 제외하고 저장). 관리자 계정은 A201
//   changeNickname: 닉네임만 따로 저장. 변경 제한 기간 안이면 B202 (화면은 get의 nicknameChangeableAt으로 먼저 막는다)
//   avatars: 기본 아바타 키 목록 (순서대로)
//   게임 정보(addGame·updateGame·deleteGame)는 US5 때 더한다
export const profileApi = {
  get: () => http.get<MyProfile>("/api/v1/users/me/profile").then((res) => res.data),
  save: (data: ProfileSaveRequest) => http.put<MyProfile>("/api/v1/users/me/profile", data).then((res) => res.data),
  changeNickname: (nickname: string) =>
    http.put<NicknameChangeResponse>("/api/v1/users/me/nickname", { nickname }).then((res) => res.data),
  avatars: () => http.get<AvatarList>("/api/v1/profile/avatars").then((res) => res.data),
}
