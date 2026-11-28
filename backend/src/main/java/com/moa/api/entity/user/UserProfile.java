package com.moa.api.entity.user;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.moa.api.dto.user.DefaultAvatar;
import com.moa.api.dto.user.DiscordVisibility;
import com.moa.api.dto.user.Gender;
import com.moa.common.entity.BaseTimeEntity;
import com.moa.common.entity.HardDelete;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로필 (specs/010-social-login data-model, specs/009-mypage data-model "프로필").
 * 닉네임은 겹쳐도 되고 (nicknameKey, nicknameTag)가 고유하다. 표시 닉네임은 tb_user.name이다.
 * <p>
 * 009 칸 중 NOT NULL인 avatarKey·ageGenderPublic·discordVisibility는 필드 기본값을 둔다.
 * 009 칸을 모르는 010 가입(new UserProfile(userId, key, tag))이 만든 행도 비지 않게 한다.
 */
@Getter
@Entity
@Table(name = "tb_user_profile")
@HardDelete(reason = "회원 하나에 한 행. 탈퇴 때 처리는 회원 탈퇴 스펙이 정한다")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends BaseTimeEntity {

	@Id
	@Column(name = "user_id")
	private Long userId;

	/** tb_user.name의 정규화 값 (NicknamePolicy.key) */
	@Column(name = "nickname_key", nullable = false, length = 50)
	private String nicknameKey;

	/** 0001~9999 */
	@Column(name = "nickname_tag", nullable = false, length = 4)
	private String nicknameTag;

	/** moa 009: DefaultAvatar 키 */
	@Column(name = "avatar_key", nullable = false, length = 30)
	private String avatarKey = DefaultAvatar.DEFAULT.key();

	@Column(name = "bio", length = 500)
	private String bio;

	@Column(name = "birth_year")
	private Short birthYear;

	@Enumerated(EnumType.STRING)
	@Column(name = "gender", length = 20)
	private Gender gender;

	/** 나이·성별 공개 여부 하나 (잠정, FR-007) */
	@Column(name = "age_gender_public", nullable = false)
	private boolean ageGenderPublic;

	@Column(name = "discord_tag", length = 32)
	private String discordTag;

	@Enumerated(EnumType.STRING)
	@Column(name = "discord_visibility", nullable = false, length = 20)
	private DiscordVisibility discordVisibility = DiscordVisibility.PRIVATE;

	/** 마지막 닉네임 변경 시각. null이면 가입 뒤 안 바꿈 (FR-004) */
	@Column(name = "nickname_changed_at")
	private LocalDateTime nicknameChangedAt;

	/** 플레이 가능 시간대 코드 (공통코드 PLAY_TIME). 저장은 보낸 목록으로 바꾸기 */
	@ElementCollection
	@CollectionTable(name = "tb_user_profile_play_time", joinColumns = @JoinColumn(name = "user_id"))
	@Column(name = "play_time_code", nullable = false, length = 50)
	private Set<String> playTimes = new HashSet<>();

	public UserProfile(Long userId, String nicknameKey, String nicknameTag) {
		this.userId = userId;
		this.nicknameKey = nicknameKey;
		this.nicknameTag = nicknameTag;
	}

	public void changeNickname(String nicknameKey, String nicknameTag) {
		this.nicknameKey = nicknameKey;
		this.nicknameTag = nicknameTag;
	}

	/** moa 009: 회원이 마이페이지에서 닉네임을 바꾼 시각 (관리자 이름 바꾸기는 적지 않는다, research R5) */
	public void markNicknameChanged(LocalDateTime now) {
		this.nicknameChangedAt = now;
	}

	/** moa 009: 프로필 저장. 모든 칸을 보낸 값으로 바꾼다 (null·빈 목록은 비움) */
	public void update(String avatarKey, String bio, Short birthYear, Gender gender, boolean ageGenderPublic,
		String discordTag, DiscordVisibility discordVisibility, Collection<String> playTimes) {
		this.avatarKey = avatarKey;
		this.bio = bio;
		this.birthYear = birthYear;
		this.gender = gender;
		this.ageGenderPublic = ageGenderPublic;
		this.discordTag = discordTag;
		this.discordVisibility = discordVisibility;
		this.playTimes.clear();
		this.playTimes.addAll(playTimes);
	}
}
