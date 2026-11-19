package com.moa.common.security.oauth;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * 켜진 제공자 클라이언트를 경로 값으로 찾는다. 모르는 제공자와 꺼진 제공자는 같게 "없음"이다 (A008은 부르는 쪽이 정한다).
 */
@Component
public class OAuthProviderRegistry {

	private final List<OAuthProviderClient> clients;

	public OAuthProviderRegistry(List<OAuthProviderClient> clients) {
		this.clients = List.copyOf(clients);
	}

	public Optional<OAuthProviderClient> find(String pathValue) {
		return SocialProvider.fromPathValue(pathValue).flatMap(this::find);
	}

	public Optional<OAuthProviderClient> find(SocialProvider provider) {
		return clients.stream().filter(client -> client.provider() == provider && client.enabled()).findFirst();
	}

	/** 켜진 제공자, enum 순서 */
	public List<SocialProvider> enabledProviders() {
		return clients.stream()
			.filter(OAuthProviderClient::enabled)
			.map(OAuthProviderClient::provider)
			.distinct()
			.sorted(Comparator.naturalOrder())
			.toList();
	}
}
