package com.groom.user.presentation.dto.response.user;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResUserListDtoV1 {

	private List<ResUserDtoV1> users;
	private int page;
	private int size;
	private long totalElements;
	private int totalPages;

	public static ResUserListDtoV1 from(Page<ResUserDtoV1> page) {
		return ResUserListDtoV1.builder()
			.users(page.getContent())
			.page(page.getNumber())
			.size(page.getSize())
			.totalElements(page.getTotalElements())
			.totalPages(page.getTotalPages())
			.build();
	}
}
