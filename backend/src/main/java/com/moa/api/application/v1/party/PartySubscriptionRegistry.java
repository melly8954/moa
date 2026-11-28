package com.moa.api.application.v1.party;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * 회원별 파티 주소 구독 기록 (specs/008-party research R3 "주의"). 멤버에서 빠진 회원의 구독을 서버가 끊으려고 둔다.
 * 구독 검사 인터셉터가 적고, PartyRealtimeNotifier가 끊을 때 읽는다. 서버 한 대 기준의 메모리 기록이다.
 */
@Component
public class PartySubscriptionRegistry {

	/** 한 구독. 세션 ID와 그 세션 안의 구독 ID로 가리킨다 */
	public record Subscription(String sessionId, String subscriptionId, Long partyId) {
	}

	private final Map<Long, Set<Subscription>> byUser = new ConcurrentHashMap<>();

	public void add(Long userId, Subscription subscription) {
		byUser.computeIfAbsent(userId, id -> ConcurrentHashMap.newKeySet()).add(subscription);
	}

	public void remove(Long userId, String sessionId, String subscriptionId) {
		Set<Subscription> subscriptions = byUser.get(userId);
		if (subscriptions != null) {
			subscriptions.removeIf(s -> s.sessionId().equals(sessionId) && s.subscriptionId().equals(subscriptionId));
		}
	}

	/** 연결이 끊기면 그 세션의 구독을 모두 지운다 */
	public void removeSession(Long userId, String sessionId) {
		Set<Subscription> subscriptions = byUser.get(userId);
		if (subscriptions != null) {
			subscriptions.removeIf(s -> s.sessionId().equals(sessionId));
		}
	}

	/** 이 회원의 이 파티 구독을 기록에서 빼고 돌려준다 */
	public List<Subscription> takeAll(Long userId, Long partyId) {
		Set<Subscription> subscriptions = byUser.get(userId);
		if (subscriptions == null) {
			return List.of();
		}
		List<Subscription> taken = subscriptions.stream().filter(s -> s.partyId().equals(partyId)).toList();
		taken.forEach(subscriptions::remove);
		return taken;
	}
}
