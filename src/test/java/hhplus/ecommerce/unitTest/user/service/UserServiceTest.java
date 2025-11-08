package hhplus.ecommerce.unitTest.user.service;

import hhplus.ecommerce.common.domain.exception.UserException;
import hhplus.ecommerce.user.application.service.UserService;
import hhplus.ecommerce.user.domain.model.User;
// claude review : User 생성자와 UserRegistrationRequest 사용을 위해 UserRole import 추가
import hhplus.ecommerce.user.domain.model.UserRole;
import hhplus.ecommerce.user.domain.repository.UserRepository;
import hhplus.ecommerce.user.presentation.dto.request.UserRegistrationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("유저를 정상적으로 등록할 수 있다")
    void registerUser() {
        // claude review : setRole은 UserRole 타입
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername("테스트유저");
        request.setRole(UserRole.CUSTOMER);

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.registerUser(request);

        assertThat(result).isNotNull();
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("중복된 사용자명으로 등록 시 예외가 발생한다")
    void registerUserWithDuplicateName() {
        // claude review : setRole은 UserRole 타입
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername("중복유저");
        request.setRole(UserRole.CUSTOMER);

        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(request))
                .isInstanceOf(UserException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("ID로 유저를 조회할 수 있다")
    void getUserById() {
        // claude review : User 생성자 파라미터 수정
        User mockUser = new User(1L, "테스트유저", BigDecimal.ZERO, UserRole.CUSTOMER);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(mockUser));

        User result = userService.getUserById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("테스트유저");
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 유저 조회 시 예외가 발생한다")
    void getUserByIdNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(UserException.class);

        verify(userRepository, times(1)).findById(999L);
    }
}
