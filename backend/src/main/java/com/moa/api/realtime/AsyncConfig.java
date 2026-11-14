package com.moa.api.realtime;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 실행을 켠다 (specs/007-notifications research R4). 웹 푸시는 느릴 수 있어 커밋 뒤 @Async로 보낸다.
 * 키트 Application.java는 @EnableScheduling만 켜므로 moa 설정 클래스에 둔다 (키트 파일은 고치지 않는다).
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
