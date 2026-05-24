//package com.groom.common.infrastructure.config.security;
//
//import java.io.IOException;
//import java.util.List;
//
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Component;
//import org.springframework.util.StringUtils;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//
//// @Component
//@RequiredArgsConstructor
//// 서비스 이용 시(매 요청) 토큰 검증 + 인증정보 세팅
//// OncePerRequestFilter : 서블릿 필터의 한 종류, 하나의 HTTP 요청에 대해 1번만 실행되도록 보장
//public class JwtAuthenticationFilter extends OncePerRequestFilter {
//
//	private final JwtUtil jwtUtil;
//
//	@Override
//	protected boolean shouldNotFilter(HttpServletRequest request) {
//		String path = request.getRequestURI();
//		// 내부 API 호출은 JWT 필터 스킵
//		return path.startsWith("/internal/") || path.startsWith("/api/v1/internal/");
//	}
//
//	@Override
//	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
//		FilterChain filterChain) throws ServletException, IOException {
//		String token = resolveToken(request);
//
//		if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
//			CustomUserDetails userDetails = new CustomUserDetails(
//				jwtUtil.getUserIdFromToken(token),
//				jwtUtil.getEmailFromToken(token),
//				jwtUtil.getRoleFromToken(token)
//			);
//
//			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
//				userDetails, null, List.of(new SimpleGrantedAuthority("ROLE_" + userDetails.getRole()))
//			);
//
//			SecurityContextHolder.getContext().setAuthentication(authentication);
//		}
//
//		filterChain.doFilter(request, response);
//	}
//
//	private String resolveToken(HttpServletRequest request) {
//		String bearerToken = request.getHeader("Authorization");
//		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
//			return bearerToken.substring(7);
//		}
//		return null;
//	}
//}
