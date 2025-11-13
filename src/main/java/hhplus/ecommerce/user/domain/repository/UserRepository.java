package hhplus.ecommerce.user.domain.repository;

import hhplus.ecommerce.user.domain.model.User;
import hhplus.ecommerce.user.domain.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * User JPA Repository
 * InMemoryRepository 제거하고 JPA로 통합
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // username으로 조회 (논리삭제된 유저 제외)
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.isDeleted = false")
    User findByUsername(@Param("username") String username);

    // userId로 조회 (논리삭제된 유저 제외)
    @Query("SELECT u FROM User u WHERE u.userId = :userId AND u.isDeleted = false")
    User findByUserId(@Param("userId") Long userId);

    // 역할별 유저 조회 (논리삭제 제외)
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isDeleted = false")
    Page<User> findAllByRole(@Param("role") UserRole role, Pageable pageable);

    // 활성 유저 수 (논리삭제 제외)
    @Query("SELECT COUNT(u) FROM User u WHERE u.isDeleted = false")
    long countAllActive();

    // 역할별 유저 수
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.isDeleted = false")
    long countByRole(@Param("role") UserRole role);

    // username 중복 체크
    boolean existsByUsername(String username);
}
