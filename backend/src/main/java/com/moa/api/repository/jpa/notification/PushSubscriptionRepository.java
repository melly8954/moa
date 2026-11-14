package com.moa.api.repository.jpa.notification;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moa.api.entity.notification.PushSubscription;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

	Optional<PushSubscription> findByEndpoint(String endpoint);

	List<PushSubscription> findByUserId(Long userId);
}
