package com.brahos.accessibilitychecker.utility;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
GroupedOpenApi publicApi() {
		return GroupedOpenApi.builder().group("public").pathsToMatch("/api/**")
				.addOpenApiCustomizer(openApi -> openApi.info(new Info().title("Accessibility Checker API")
						.version("1.0.0").description("API for checking website accessibility compliance")
						.license(new io.swagger.v3.oas.models.info.License().name("MIT")
								.url("https://opensource.org/licenses/MIT"))))
				.build();
	}
}
