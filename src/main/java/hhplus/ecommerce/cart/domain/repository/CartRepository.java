package hhplus.ecommerce.cart.domain.repository;

import hhplus.ecommerce.cart.domain.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    List<Cart> findByUserId(Long userId);

    Optional<Cart> findByUserIdAndProductOptionId(Long userId, Long productOptionId);

    void deleteByUserId(Long userId);

    // 사용자ID와 상품옵션ID로 장바구니 아이템 삭제
    void deleteByUserIdAndProductOptionId(Long userId, Long productOptionId);

    @Query("SELECT c FROM Cart c WHERE c.productOptionId = :productOptionId")
    List<Cart> findByProductOptionId(@Param("productOptionId") Long productOptionId);
}
