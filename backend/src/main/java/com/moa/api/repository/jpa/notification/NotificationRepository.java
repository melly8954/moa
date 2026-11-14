package com.moa.api.repository.jpa.notification;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moa.api.entity.notification.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	Page<Notification> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

	long countByUserIdAndReadAtIsNull(Long userId);

	Optional<Notification> findByIdAndUserId(Long id, Long userId);

	@Modifying(clearAutomatically = true)
	@Query("update Notification n set n.readAt = :now where n.userId = :userId and n.readAt is null")
	int readAll(@Param("userId") Long userId, @Param("now") LocalDateTime now);

	@Modifying
	@Query("delete from Notification n where n.createdAt < :cutoff")
	int deleteCreatedBefore(@Param("cutoff") LocalDateTime cutoff);
}
