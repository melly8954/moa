package com.moa.common.security.token;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from RefreshToken t where t.tokenHash = :tokenHash")
	Optional<RefreshToken> findForUpdateByTokenHash(@Param("tokenHash") String tokenHash);

	@Modifying(clearAutomatically = true)
	@Query("update RefreshToken t set t.revokedAt = :now where t.userId = :userId and t.revokedAt is null")
	int revokeAllByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

	@Modifying
	@Query("delete from RefreshToken t where t.expiresAt < :before")
	int deleteExpiredBefore(@Param("before") LocalDateTime before);
}
