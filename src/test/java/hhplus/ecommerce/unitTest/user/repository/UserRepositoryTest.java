package hhplus.ecommerce.unitTest.user.repository;

import hhplus.ecommerce.user.domain.model.User;
// claude review : User.create 사용을 위해 UserRole import 추가
import hhplus.ecommerce.user.domain.model.UserRole;
import hhplus.ecommerce.user.infrastructure.repository.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest {

    private InMemoryUserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
    }

    @Test
    @DisplayName("유저를 저장할 수 있다")
    void saveUser() {
        // claude review : User 생성자 파라미터 수정
        User user = User.create("테스트유저", UserRole.CUSTOMER);

        User savedUser = userRepository.save(user);

        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getUserId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("테스트유저");
    }

    @Test
    @DisplayName("ID로 유저를 조회할 수 있다")
    void findById() {
        // claude review : User 생성자 파라미터 수정
        User user = User.create("테스트유저", UserRole.CUSTOMER);
        User savedUser = userRepository.save(user);

        Optional<User> foundUser = userRepository.findById(savedUser.getUserId());

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("테스트유저");
    }

    @Test
    @DisplayName("사용자명으로 중복 여부를 확인할 수 있다")
    void existsByUsername() {
        // claude review : User 생성자 파라미터 수정
        User user = User.create("중복유저", UserRole.CUSTOMER);
        userRepository.save(user);

        boolean exists = userRepository.existsByUsername("중복유저");
        boolean notExists = userRepository.existsByUsername("없는유저");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("전체 유저 수를 조회할 수 있다")
    void countAll() {
        // claude review : User 생성자 파라미터 수정
        userRepository.save(User.create("유저1", UserRole.CUSTOMER));
        userRepository.save(User.create("유저2", UserRole.CUSTOMER));
        userRepository.save(User.create("유저3", UserRole.CUSTOMER));

        int count = userRepository.countAll();

        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("페이징을 적용하여 유저 목록을 조회할 수 있다")
    void findAllWithPage() {
        // claude review : User 생성자 파라미터 수정
        for (int i = 1; i <= 10; i++) {
            userRepository.save(User.create("유저" + i, UserRole.CUSTOMER));
        }

        List<User> page1 = userRepository.findAllWithPage(0, 5);
        List<User> page2 = userRepository.findAllWithPage(1, 5);

        assertThat(page1).hasSize(5);
        assertThat(page2).hasSize(5);
    }
}
