package com.moa.api.entity.user;

import com.moa.common.entity.BaseTimeEntity;
import com.moa.common.entity.HardDelete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로필 (최소형, specs/010-social-login data-model, specs/009-mypage research R4).
 * 닉네임은 겹쳐도 되고 (nicknameKey, nicknameTag)가 고유하다. 표시 닉네임은 users.name이다. 009가 칸을 더한다.
 */
@Getter
@Entity
@Table(name = "user_profiles")
@HardDelete(reason = "회원 하나에 한 행. 탈퇴 때 처리는 회원 탈퇴 스펙이 정한다")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends BaseTimeEntity {

	@Id
	@Column(name = "user_id")
	private Long userId;

	/** users.name의 정규화 값 (NicknamePolicy.key) */
	@Column(name = "nickname_key", nullable = false, length = 50)
	private String nicknameKey;

	/** 0001~9999 */
	@Column(name = "nickname_tag", nullable = false, length = 4)
	private String nicknameTag;

	public UserProfile(Long userId, String nicknameKey, String nicknameTag) {
		this.userId = userId;
		this.nicknameKey = nicknameKey;
		this.nicknameTag = nicknameTag;
	}

	public void changeNickname(String nicknameKey, String nicknameTag) {
		this.nicknameKey = nicknameKey;
		this.nicknameTag = nicknameTag;
	}
}
