package com.moa.api.service.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.moa.api.entity.user.User;
import com.moa.common.security.Role;

/** 다른 회원에게 보이는 이름 (specs/018-member-withdrawal FR-008) */
class MemberDisplayTest {

	@Test
	@DisplayName("US1-AC7 기본 조회에 없는 회원(탈퇴·지움·파기)의 이름은 \"탈퇴한 회원\"이고, 있으면 그 이름이다")
	void withdrawnMemberName() {
		assertThat(MemberDisplay.nameOf(Optional.empty())).isEqualTo("탈퇴한 회원");
		assertThat(MemberDisplay.nameOf(Optional.of(new User("moa", null, "모아킴", Role.USER)))).isEqualTo("모아킴");
	}
}
