package com.groom.user.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.groom.user.domain.entity.user.UserEntity;
import com.groom.common.enums.UserRole;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

	Optional<UserEntity> findByEmail(String email);

	Optional<UserEntity> findByNickname(String nickname);

	boolean existsByEmailAndDeletedAtIsNull(String email);

	boolean existsByNicknameAndDeletedAtIsNull(String nickname);

	// ----
	// boolean existsByEmail(String email);
	// boolean existsByNickname(String nickname);
	// ----

	Optional<UserEntity> findByUserIdAndDeletedAtIsNull(UUID userId);

	Optional<UserEntity> findByCognitoSubAndDeletedAtIsNull(String cognitoSub);

	Optional<UserEntity> findByEmailAndDeletedAtIsNull(String email);

	// Manager용: 회원 목록 조회
	Page<UserEntity> findByDeletedAtIsNull(Pageable pageable);

	Page<UserEntity> findByRoleAndDeletedAtIsNull(UserRole role, Pageable pageable);

	Optional<UserEntity> findByCognitoSub(String cognitoSub);
}
