package hhplus.ecommerce.unitTest.user.repository;

import hhplus.ecommerce.user.domain.model.User;
import hhplus.ecommerce.user.domain.model.UserRole;
import hhplus.ecommerce.user.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserJpaRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private UserRepository userJpaRepository;

    @Test
    @DisplayName("JPA: 사용자 저장 및 조회 (username, userId)")
    void saveAndFindUser() {
        User saved = userJpaRepository.save(User.create("user1", UserRole.CUSTOMER));

        User byUsername = userJpaRepository.findByUsername("user1");
        User byUserId = userJpaRepository.findByUserId(saved.getUserId());

        assertThat(byUsername).isNotNull();
        assertThat(byUserId).isNotNull();
        assertThat(byUsername.getUserId()).isEqualTo(saved.getUserId());
    }

    @Test
    @DisplayName("JPA: username 중복 여부와 역할별 페이징 조회 및 카운트")
    void existsAndPagingAndCount() {
        userJpaRepository.save(User.create("u1", UserRole.CUSTOMER));
        userJpaRepository.save(User.create("u2", UserRole.CUSTOMER));
        userJpaRepository.save(User.create("u3", UserRole.ADMIN));

        boolean exists = userJpaRepository.existsByUsername("u1");
        assertThat(exists).isTrue();

        Page<User> customers = userJpaRepository.findAllByRole(UserRole.CUSTOMER, PageRequest.of(0, 10));
        assertThat(customers.getTotalElements()).isEqualTo(2);

        long activeCount = userJpaRepository.countAllActive();
        long adminCount = userJpaRepository.countByRole(UserRole.ADMIN);
        assertThat(activeCount).isEqualTo(3);
        assertThat(adminCount).isEqualTo(1);
    }

    @Test
    @DisplayName("JPA: 논리삭제된 사용자는 커스텀 조회에서 제외")
    void excludeDeletedInCustomQueries() {
        User u = userJpaRepository.save(User.create("tempUser", UserRole.CUSTOMER));
        // 논리 삭제 후 저장
        u.delete();
        userJpaRepository.save(u);

        User shouldBeNull = userJpaRepository.findByUsername("tempUser");
        assertThat(shouldBeNull).isNull();
    }
}

