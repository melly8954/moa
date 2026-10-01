package com.moa.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Swagger UI 설정. API의 정본은 docs/design/api/*.md이고, 어노테이션은 그 문서를 옮겨 적은 것이다.
 * 둘이 어긋나면 문서를 기준으로 코드를 고친다.
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
