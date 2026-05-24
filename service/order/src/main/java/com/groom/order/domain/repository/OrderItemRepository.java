package com.groom.order.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groom.order.domain.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
	@Query("""
    select count(oi) > 0
    from OrderItem oi
    where oi.order.orderId = :orderId
      and oi.productId = :productId
""")
	boolean existsByOrderIdAndProductId(
		@Param("orderId") UUID orderId,
		@Param("productId") UUID productId
	);
}
