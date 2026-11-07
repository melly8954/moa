package com.moa.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Swagger UI 설정. API의 정본은 컨트롤러와 요청·응답 DTO이고, Swagger 문서는 어노테이션으로 코드에서 만든다.
 */
@Configuration
public class OpenApiConfig {

	private static final String BEARER = "bearerAuth";

	@Bean
	public OpenAPI openApi() {
		return new OpenAPI()
			.info(new Info().title("API").version("v1"))
			.components(new Components().addSecuritySchemes(BEARER,
				new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
			.addSecurityItem(new SecurityRequirement().addList(BEARER));
	}
}
