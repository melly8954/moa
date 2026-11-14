package com.moa.common.helper.webpush;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 웹 푸시 설정을 켠다 (moa 007, 키트 헬퍼로 옮길 것) */
@Configuration
@EnableConfigurationProperties(WebPushProperties.class)
public class WebPushConfig {
	// 설정 클래스만 등록한다
}
