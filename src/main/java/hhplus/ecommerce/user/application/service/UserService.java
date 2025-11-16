package hhplus.ecommerce.user.application.service;

import hhplus.ecommerce.common.domain.exception.UserException;
import hhplus.ecommerce.common.presentation.response.PageResponse;
import hhplus.ecommerce.user.domain.model.User;
import hhplus.ecommerce.user.domain.model.UserRole;
import hhplus.ecommerce.user.domain.repository.UserRepository;
import hhplus.ecommerce.user.presentation.dto.request.UserRegistrationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 페이징을 적용하여 유저 목록을 가져온다.
     * @param role 필터링할 역할 (CUSTOMER, ADMIN) - 선택사항
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이징된 유저 목록
     */
    public PageResponse<User> getUserListWithPage(String role, int page, int size) {
        Page<User> userPage;
        Pageable pageable = PageRequest.of(page, size);

        if (role != null && !role.isBlank()) {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            userPage = userRepository.findAllByRole(userRole, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        return new PageResponse<>(
                userPage.getContent(),
                page,
                size,
                userPage.getTotalElements(),
                userPage.getTotalPages()
        );
    }

    /**
     * 아이디로 유저를 조회한다.
     * @param userId 유저 ID
     * @return 조회된 유저
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> UserException.userNotFound(userId));
    }

    /**
     * 유저를 등록한다.
     * @param request 유저 등록 요청
     * @return 등록된 유저
     */
    public User registerUser(UserRegistrationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw UserException.creationFailed("이미 존재하는 사용자명입니다.");
        }

        User user = User.create(
                request.getUsername(),
                request.getRole()
        );

        return userRepository.save(user);
    }
}
