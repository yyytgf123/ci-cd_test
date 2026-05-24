package com.groom.order.presentation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

	@GetMapping("/api/v2/orders/health")
	public Map<String, String> health() {
		return Map.of("status", "UP");
	}
}

