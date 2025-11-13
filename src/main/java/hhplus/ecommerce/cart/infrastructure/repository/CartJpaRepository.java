package hhplus.ecommerce.cart.infrastructure.repository;

import hhplus.ecommerce.cart.domain.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartJpaRepository extends JpaRepository<Cart, Long> {

    // 사용자별 장바구니 조회
    List<Cart> findByUserId(Long userId);

    // 사용자 + 상품 옵션으로 장바구니 조회 (중복 체크용)
    Optional<Cart> findByUserIdAndProductOptionId(Long userId, Long productOptionId);

    // 사용자별 장바구니 전체 삭제
    void deleteByUserId(Long userId);

    // 상품 옵션별 장바구니 조회
    @Query("SELECT c FROM Cart c WHERE c.productOptionId = :productOptionId")
    List<Cart> findByProductOptionId(@Param("productOptionId") Long productOptionId);
}
