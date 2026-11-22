package com.moa.api.dto.user;

import java.util.List;

/**
 * 프로필 저장 값 (specs/009-mypage contracts "프로필 저장"). 닉네임·게임 정보는 따로 저장한다.
 * 목록 값(gender·discordVisibility)은 문자열로 받아 서비스가 칸 오류(V001)로 확인한다.
 *
 * @param avatar 기본 아바타 키. null이면 default
 * @param discordVisibility null이면 PRIVATE
 * @param playTimes 공통코드 PLAY_TIME 코드. null이면 빈 목록
 */
public record ProfileSaveCommand(String avatar, String bio, Integer birthYear, String gender,
	Boolean ageGenderPublic, String discordTag, String discordVisibility, List<String> playTimes) {
}
