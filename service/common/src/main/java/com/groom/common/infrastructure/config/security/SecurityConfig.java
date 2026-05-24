package com.groom.common.infrastructure.config.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	@Value("${aws.cognito.jwk-set-uri}")
	private String jwkSetUri;

	@Bean
	public WebSecurityCustomizer webSecurityCustomizer() {
		return (web) -> web.ignoring()
			.requestMatchers(PathRequest.toStaticResources().atCommonLocations())
			.requestMatchers("/pay.html", "/pay-success.html", "/pay-fail.html");
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/health","/test/**", "/swagger-ui/**", "/v3/api-docs/**", "/api/v2/orders/**")
				.permitAll()
				.requestMatchers("/api/v2/auth/signup", "/api/v2/auth/login").permitAll()
				.requestMatchers(
					"/api/v2/auth/signup",
					"/api/v2/auth/login",
					"/api/v2/auth/confirm",
					"/api/v2/auth/resend-code",
					"/api/v2/auth/refresh").permitAll()
				.requestMatchers("/api/v2/payments/**").permitAll()
				.requestMatchers("/api/v2/products", "/api/v2/products/{productId}").permitAll()
				.requestMatchers("/api/v1/internal/**").permitAll()
				.requestMatchers("/api/v2/internal/**").permitAll()
				.requestMatchers("/actuator/**", "/api/actuator/**", "/internal/**").permitAll()
				.requestMatchers("/api/v2/categories", "/api/v2/categories/{categoryId}").permitAll()
				.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
				.requestMatchers("/", "/favicon.ico", "/error").permitAll()
				.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
				.anyRequest().authenticated())
			.oauth2ResourceServer(oauth2 -> oauth2
				.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

		return http.build();
	}

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(new CognitoGroupsConverter());
		return converter;
	}

	// Cognito 그룹을 Spring Security Role로 변환
	static class CognitoGroupsConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
		@Override
		public Collection<GrantedAuthority> convert(Jwt jwt) {
			List<String> groups = jwt.getClaimAsStringList("cognito:groups");
			if (groups == null || groups.isEmpty()) {
				return Collections.emptyList();
			}
			return groups.stream()
				.map(group -> new SimpleGrantedAuthority("ROLE_" + group))
				.collect(Collectors.toList());
		}
	}

	@Bean
	public JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
