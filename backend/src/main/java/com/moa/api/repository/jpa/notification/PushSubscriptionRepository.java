package com.moa.api.repository.jpa.notification;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moa.api.entity.notification.PushSubscription;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

	Optional<PushSubscription> findByEndpoint(String endpoint);

	List<PushSubscription> findByUserId(Long userId);

	@Modifying
	@Query("delete from PushSubscription s where s.userId = :userId")
	int deleteByUserId(@Param("userId") Long userId);
}
