//package com.groom.common.infrastructure.config.security;
//
//import java.util.Base64;
//import java.util.Date;
//import java.util.UUID;
//
//import javax.crypto.SecretKey;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import com.groom.common.presentation.advice.CustomException;
//import com.groom.common.presentation.advice.ErrorCode;
//
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.ExpiredJwtException;
//import io.jsonwebtoken.JwtException;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.security.Keys;
//import jakarta.annotation.PostConstruct;
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//@Component
//// 토큰 발급 & 해석
//public class JwtUtil {
//
//	@Value("${jwt.secret:defaultSecretKeyForDevelopmentPurposeOnly12345}")
//	private String secretKey;
//
//	@Value("${jwt.access-token-expiration:3600000}")
//	private long accessTokenExpiration;
//
//	@Value("${jwt.refresh-token-expiration:604800000}")
//	private long refreshTokenExpiration;
//
//	private SecretKey key;
//
//	@PostConstruct
//	public void init() {
//		byte[] keyBytes = Base64.getEncoder().encode(secretKey.getBytes());
//		this.key = Keys.hmacShaKeyFor(keyBytes);
//	}
//
////	public String generateAccessToken(UUID userId, String email, String role) {
////		return generateToken(userId, email, role, accessTokenExpiration);
////	}
////
////	public String generateRefreshToken(UUID userId, String email, String role) {
////		return generateToken(userId, email, role, refreshTokenExpiration);
////	}
////
////	private String generateToken(UUID userId, String email, String role, long expiration) {
////		Date now = new Date();
////		return Jwts.builder()
////			.setSubject(userId.toString())
////			.claim("email", email)
////			.claim("role", role)
////			.setIssuedAt(now)
////			.setExpiration(new Date(now.getTime() + expiration))
////			.signWith(key)
////			.compact();
////	}
//
//	public UUID getUserIdFromToken(String token) {
//		return UUID.fromString(parseClaims(token).getSubject());
//	}
//
//	public String getEmailFromToken(String token) {
//		return parseClaims(token).get("email", String.class);
//	}
//
//	public String getRoleFromToken(String token) {
//		return parseClaims(token).get("role", String.class);
//	}
//
//	public boolean validateToken(String token) {
//		try {
//			parseClaims(token);
//			return true;
//		} catch (ExpiredJwtException e) {
//			throw new CustomException(ErrorCode.EXPIRED_TOKEN);
//		} catch (JwtException e) {
//			throw new CustomException(ErrorCode.INVALID_TOKEN);
//		}
//	}
//
//	private Claims parseClaims(String token) {
//		return Jwts.parserBuilder()
//			.setSigningKey(key)
//			.build()
//			.parseClaimsJws(token)
//			.getBody();
//	}
//}
