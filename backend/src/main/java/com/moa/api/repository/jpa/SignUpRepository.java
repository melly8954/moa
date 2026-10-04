package com.moa.api.repository.jpa;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moa.api.entity.SignUp;

public interface SignUpRepository extends JpaRepository<SignUp, Long> {

	Optional<SignUp> findByTokenHash(String tokenHash);

	@Modifying
	@Query("delete from SignUp s where s.expiresAt < :before")
	int deleteExpiredBefore(@Param("before") LocalDateTime before);
}
