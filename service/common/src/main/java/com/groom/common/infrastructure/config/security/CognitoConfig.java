package com.groom.common.infrastructure.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
@ConditionalOnProperty(prefix = "aws.cognito", name = "enabled", havingValue = "true")
public class CognitoConfig {

	@Value("${aws.cognito.region}")
	private String region;

	@Bean
	public CognitoIdentityProviderClient cognitoClient() {
		return CognitoIdentityProviderClient.builder()
			.region(Region.of(region))
			.credentialsProvider(DefaultCredentialsProvider.create())
			.build();
	}
}
