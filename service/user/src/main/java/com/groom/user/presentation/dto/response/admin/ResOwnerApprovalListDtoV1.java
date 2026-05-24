package com.groom.user.presentation.dto.response.admin;

import java.util.List;

import org.springframework.data.domain.Page;

import com.groom.user.presentation.dto.response.owner.ResOwnerApprovalDtoV1;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResOwnerApprovalListDtoV1 {

	private List<ResOwnerApprovalDtoV1> content;
	private int page;
	private int size;
	private long totalElements;
	private int totalPages;
	private boolean first;
	private boolean last;

	public static ResOwnerApprovalListDtoV1 from(Page<ResOwnerApprovalDtoV1> page) {
		return ResOwnerApprovalListDtoV1.builder()
			.content(page.getContent())
			.page(page.getNumber())
			.size(page.getSize())
			.totalElements(page.getTotalElements())
			.totalPages(page.getTotalPages())
			.first(page.isFirst())
			.last(page.isLast())
			.build();
	}
}
