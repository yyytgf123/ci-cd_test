package com.groom.common.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
public class SwaggerConfig {

	@Bean
	public GroupedOpenApi publicApi() {
		return GroupedOpenApi.builder()
				.group("v1-ecommerce")
				.pathsToMatch("/api/v2/**")
				.addOpenApiCustomizer(apiSorter()) // 정렬 유틸리티 연결
				.build();
	}

	@Bean
	public OpenApiCustomizer apiSorter() {
		// 1. 서비스별 태그 치환 맵 정의 (순서가 유지되는 LinkedHashMap 권장)
		Map<String, String> tagMap = new LinkedHashMap<>();

		// 지민 님의 서비스 도메인에 맞게 매핑
		// Controller의 @Tag(name = "...") 값과 일치해야 합니다.
		tagMap.put("User", "👤 회원 관리 API");
		tagMap.put("Auth", "🔐 인증 API");
		tagMap.put("Product", "🛍️ 상품 관리 API");
		tagMap.put("Category", "📂 카테고리 API");
		tagMap.put("Cart", "🛒 장바구니 API");
		tagMap.put("Order", "📦 주문 관리 API");
		tagMap.put("Payment", "💳 결제 연동 API");
		tagMap.put("Admin", "👮 관리자 전용 API");
		tagMap.put("Test", "🛠 시스템 테스트 도구");

		// 2. 유틸리티를 호출하여 Customizer 반환
		return createSorter(tagMap);
	}

	/**
	 * 태그 이름을 매핑하고, 정의된 순서대로 정렬하는 OpenApiCustomizer를 생성합니다.
	 */
	public static OpenApiCustomizer createSorter(Map<String, String> tagMap) {
		return openApi -> {
			// 1. Rename tags in Operations
			openApi.getPaths().values().stream()
					.flatMap(pathItem -> pathItem.readOperations().stream())
					.forEach(operation -> {
						List<String> tags = operation.getTags();
						if (tags != null) {
							List<String> newTags = tags.stream()
									.map(tag -> tagMap.getOrDefault(tag, tag))
									.collect(Collectors.toList());
							operation.setTags(newTags);
						}
					});

			// 2. Reorder Tags in OpenAPI root
			// Collect all unique tags used in operations
			Set<String> usedTags = openApi.getPaths().values().stream()
					.flatMap(pathItem -> pathItem.readOperations().stream())
					.flatMap(op -> op.getTags() == null ? null : op.getTags().stream())
					.collect(Collectors.toSet());

			List<Tag> sortedTags = new ArrayList<>();
			// Add mapped tags in order
			for (String mappedName : tagMap.values()) {
				if (usedTags.contains(mappedName)) {
					sortedTags.add(new Tag().name(mappedName));
					usedTags.remove(mappedName);
				}
			}
			// Add remaining tags that were not in the map
			usedTags.stream().sorted().forEach(tagName -> sortedTags.add(new Tag().name(tagName)));

			openApi.setTags(sortedTags);
		};
	}

	@Bean
	public OpenAPI customOpenAPI() {
		String jwtSchemeName = "JWT Authentication";
		SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

		Components components = new Components()
				.addSecuritySchemes(jwtSchemeName, new SecurityScheme()
						.name(jwtSchemeName)
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT"));

		return new OpenAPI()
				.info(new Info()
						.title("이커머스 프로젝트 API")
						.description("MSA 기반 이커머스 서비스의 통합 API 명세서입니다.")
						.version("1.0.0"))
				.addSecurityItem(securityRequirement)
				.components(components);
	}
}