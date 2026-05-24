package com.groom.order.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groom.order.domain.entity.Order;

public interface OrderRepository extends JpaRepository<Order, UUID> {
	@Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.orderId = :id")
	@Override
	Optional<Order> findById(@Param("id") UUID id);

	@Query("select distinct o from Order o join fetch o.items where o.orderId in :ids")
	List<Order> findAllWithItemsByIdIn(@Param("ids") List<UUID> ids);

	@Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.items i WHERE i.productId = :productId")
	List<Order> findAllByProductId(@Param("productId") UUID productId);

	Page<Order> findAllByBuyerId(UUID buyerId, Pageable pageable);
}
