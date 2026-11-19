package com.moa.api.repository.jpa.user;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.moa.api.entity.user.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

	@Query("select p.nicknameTag from UserProfile p where p.nicknameKey = :nicknameKey")
	List<String> findTagsByNicknameKey(String nicknameKey);
}
