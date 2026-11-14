package com.moa.api.dto.notification;

import com.moa.api.entity.notification.PushSubscription;

public record PushSubscriptionDto(Long id, Long userId, String endpoint, String p256dh, String auth) {

	public static PushSubscriptionDto from(PushSubscription subscription) {
		return new PushSubscriptionDto(subscription.getId(), subscription.getUserId(), subscription.getEndpoint(),
			subscription.getP256dh(), subscription.getAuth());
	}
}
