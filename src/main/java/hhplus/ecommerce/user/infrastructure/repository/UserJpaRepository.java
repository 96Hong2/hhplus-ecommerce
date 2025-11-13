package hhplus.ecommerce.user.infrastructure.repository;

import hhplus.ecommerce.user.domain.model.User;
import hhplus.ecommerce.user.domain.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<User, Long> {

    // 삭제되지 않은 사용자 중 username으로 조회
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.isDeleted = false")
    User findByUsername(@Param("username") String username);

    // 삭제되지 않은 사용자 중 userId로 조회
    @Query("SELECT u FROM User u WHERE u.userId = :userId AND u.isDeleted = false")
    User findByUserId(@Param("userId") Long userId);

    // username 중복 체크
    boolean existsByUsername(String username);

    // 역할별 사용자 조회 (페이징)
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isDeleted = false")
    Page<User> findAllByRole(@Param("role") UserRole role, Pageable pageable);

    // 삭제되지 않은 사용자 수
    @Query("SELECT COUNT(u) FROM User u WHERE u.isDeleted = false")
    long countAllActive();

    // 역할별 사용자 수
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.isDeleted = false")
    long countByRole(@Param("role") UserRole role);
}
