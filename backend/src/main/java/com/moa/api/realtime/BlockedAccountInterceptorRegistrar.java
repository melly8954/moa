package com.moa.api.realtime;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.support.AbstractMessageChannel;
import org.springframework.stereotype.Component;

/**
 * {@link BlockedAccountStompInterceptor}를 들어오는 채널의 맨 끝에 붙인다 (specs/018-member-withdrawal research R3).
 *
 * <p>
 * 왜 {@code @Order}가 아닌가: 설정({@code WebSocketMessageBrokerConfigurer})들의 인터셉터는 설정의 {@code @Order} 순서로 붙는다.
 * 키트 {@code WebSocketConfig}는 순서가 없어 가장 낮은 순서({@code LOWEST_PRECEDENCE})로 정렬되므로, 어떤 {@code @Order}로도 그
 * 뒤에 설 수 없다(같은 값이면 빈 등록 순서라 정해지지 않는다). 그래서 모든 설정이 채널을 만든 뒤 맨 끝에 덧붙인다. 키트 파일은 고치지 않는다.
 * </p>
 */
@Component
public class BlockedAccountInterceptorRegistrar implements SmartInitializingSingleton {

	private final AbstractMessageChannel clientInboundChannel;
	private final BlockedAccountStompInterceptor interceptor;

	public BlockedAccountInterceptorRegistrar(
		@Qualifier("clientInboundChannel") AbstractMessageChannel clientInboundChannel,
		BlockedAccountStompInterceptor interceptor) {
		this.clientInboundChannel = clientInboundChannel;
		this.interceptor = interceptor;
	}

	/** 모든 싱글턴이 만들어진 뒤(설정의 인터셉터가 모두 붙은 뒤), 서버가 연결을 받기 전에 돈다 */
	@Override
	public void afterSingletonsInstantiated() {
		if (!clientInboundChannel.getInterceptors().contains(interceptor)) {
			clientInboundChannel.addInterceptor(interceptor);
		}
	}
}
