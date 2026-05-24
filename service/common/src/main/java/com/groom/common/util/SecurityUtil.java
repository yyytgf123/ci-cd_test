package com.groom.common.util;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.groom.common.infrastructure.config.security.CustomUserDetails;
import com.groom.common.enums.UserRole;
import com.groom.common.presentation.advice.CustomException;
import com.groom.common.presentation.advice.ErrorCode;


@Component
public class SecurityUtil {

	public static UUID getCurrentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (auth == null || !auth.isAuthenticated()) {
			throw new CustomException(ErrorCode.UNAUTHORIZED);
		}

		Object principal = auth.getPrincipal();
		if (principal instanceof CustomUserDetails userDetails) {
			return userDetails.getUserId();
		}

		Jwt jwt = extractJwt(auth, principal);
		String id = firstNonBlankClaim(jwt.getClaims(), "userId", "custom:userId", "sub");
		if (id == null) {
			throw new CustomException(ErrorCode.UNAUTHORIZED);
		}

		try {
			return UUID.fromString(id);
		} catch (IllegalArgumentException e) {
			throw new CustomException(ErrorCode.UNAUTHORIZED);
		}
	}

	public static UserRole getCurrentUserRole() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (auth == null || !auth.isAuthenticated()) {
			throw new CustomException(ErrorCode.UNAUTHORIZED);
		}

		Object principal = auth.getPrincipal();
		if (principal instanceof CustomUserDetails userDetails) {
			return UserRole.valueOf(userDetails.getRole());
		}

		Jwt jwt = extractJwt(auth, principal);
		String role = firstNonBlankClaim(jwt.getClaims(), "role", "custom:role");
		if (role != null) {
			return parseRole(role);
		}

		Object groups = jwt.getClaims().get("cognito:groups");
		if (groups instanceof Collection<?> groupList) {
			for (Object group : groupList) {
				if (group instanceof String groupName && !groupName.isBlank()) {
					return parseRole(groupName);
				}
			}
		}

		for (GrantedAuthority authority : auth.getAuthorities()) {
			String value = authority.getAuthority();
			if (value != null && value.startsWith("ROLE_")) {
				return parseRole(value.substring("ROLE_".length()));
			}
		}

		throw new CustomException(ErrorCode.UNAUTHORIZED);
	}

	private static Jwt extractJwt(Authentication auth, Object principal) {
		if (principal instanceof Jwt jwt) {
			return jwt;
		}
		if (auth instanceof JwtAuthenticationToken jwtAuth) {
			return jwtAuth.getToken();
		}
		throw new CustomException(ErrorCode.UNAUTHORIZED);
	}

	private static String firstNonBlankClaim(Map<String, Object> claims, String... keys) {
		for (String key : keys) {
			Object value = claims.get(key);
			if (value instanceof String stringValue && !stringValue.isBlank()) {
				return stringValue;
			}
		}
		return null;
	}


	private static UserRole parseRole(String role) {
		try {
			return UserRole.valueOf(role.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new CustomException(ErrorCode.UNAUTHORIZED);
		}
	}
}
