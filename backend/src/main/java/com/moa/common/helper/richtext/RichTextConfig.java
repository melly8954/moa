package com.moa.common.helper.richtext;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 본문 이미지 설정을 켠다 (선택 헬퍼 rich-text) */
@Configuration
@EnableConfigurationProperties(RichTextProperties.class)
public class RichTextConfig {
	// 설정 클래스만 등록한다
}
