package com.moa.api.repository.jpa.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.moa.api.entity.user.UserProfile;

import jakarta.persistence.LockModeType;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

	@Query("select p.nicknameTag from UserProfile p where p.nicknameKey = :nicknameKey")
	List<String> findTagsByNicknameKey(String nicknameKey);

	/** moa 009: 고치기 전에 행을 잠근다 (SELECT … FOR UPDATE, specs/009-mypage research R8) */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from UserProfile p where p.userId = :userId")
	Optional<UserProfile> findForUpdate(Long userId);
}
