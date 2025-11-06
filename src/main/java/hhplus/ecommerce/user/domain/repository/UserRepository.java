package hhplus.ecommerce.user.domain.repository;

import hhplus.ecommerce.user.domain.model.User;
import hhplus.ecommerce.user.domain.model.UserRole;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    /**
     * 유저를 등록/수정한다.
     * @param user 저장할 유저
     * @return 저장된 유저
     */
    User save(User user);

    /**
     * ID로 유저를 조회한다.
     * @param userId 유저 ID
     * @return Optional로 감싼 유저 (없으면 Optional.empty())
     */
    Optional<User> findById(Long userId);

    /**
     * 페이징을 적용하여 유저 목록을 조회한다.
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 유저 목록
     */
    List<User> findAllWithPage(int page, int size);

    /**
     * 역할별로 필터링하여 페이징을 적용한 유저 목록을 조회한다.
     * @param role 유저 역할
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 유저 목록
     */
    List<User> findAllByRoleWithPage(UserRole role, int page, int size);

    /**
     * 총 유저 수를 조회한다.
     * @return 전체 유저 수
     */
    int countAll();

    /**
     * 특정 역할을 가진 유저 수를 조회한다.
     * @param role 유저 역할
     * @return 해당 역할을 가진 유저 수
     */
    int countByRole(UserRole role);

    /**
     * username이 이미 존재하는지 확인한다.
     * @param username 확인할 사용자명
     * @return 존재하면 true, 아니면 false
     */
    boolean existsByUsername(String username);
}
