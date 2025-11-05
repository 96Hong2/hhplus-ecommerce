package hhplus.ecommerce.user.domain.repository;

import hhplus.ecommerce.user.domain.model.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    /**
     * 유저를 등록한다.
     * @param user
     * @return User
     */
    User save(User user);

    /**
    * id로 유저를 조회한다.
    * */
    Optional<User> findById(Long userId);

    /**
     * 페이징을 적용하여 유저 목록을 조회한다.
     * */
    List<User> findAllWithPage(int page, int size);

    /**
     * 유저 포인트 잔액을 조회한다.
     */
    BigDecimal findPointBalanceByUserId(Long userId);
}
