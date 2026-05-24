package com.groom.user.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.groom.user.domain.entity.owner.OwnerEntity;
import com.groom.user.domain.entity.owner.OwnerStatus;

@Repository
public interface OwnerRepository extends JpaRepository<OwnerEntity, UUID> {
	Optional<OwnerEntity> findByUserUserIdAndDeletedAtIsNull(UUID userId);

	// 승인 상태별 조회
	Page<OwnerEntity> findByOwnerStatusAndDeletedAtIsNull(OwnerStatus ownerStatus, Pageable pageable);

	// ownerId로 조회 (삭제되지 않은 것)
	Optional<OwnerEntity> findByOwnerIdAndDeletedAtIsNull(UUID ownerId);

}
