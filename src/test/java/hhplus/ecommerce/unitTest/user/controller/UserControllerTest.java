package hhplus.ecommerce.unitTest.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hhplus.ecommerce.common.presentation.response.PageResponse;
import hhplus.ecommerce.user.application.service.UserService;
import hhplus.ecommerce.user.domain.model.User;
import hhplus.ecommerce.user.domain.model.UserRole;
import hhplus.ecommerce.user.presentation.controller.UserController;
import hhplus.ecommerce.user.presentation.dto.request.UserRegistrationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("유저 목록 조회 API 테스트")
    void getUserListWithPage() throws Exception {
        User mockUser = new User(1L, "테스트유저", BigDecimal.ZERO, UserRole.CUSTOMER);
        PageResponse<User> mockPageResponse = new PageResponse<>(
                List.of(mockUser),
                0,
                20,
                1,
                1
        );

        when(userService.getUserListWithPage(any(), anyInt(), anyInt()))
                .thenReturn(mockPageResponse);

        mockMvc.perform(get("/api/user")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("유저 등록 API 테스트")
    void registerUser() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername("신규유저");
        request.setRole(UserRole.CUSTOMER);

        User mockUser = new User(1L, "신규유저", BigDecimal.ZERO, UserRole.CUSTOMER);

        when(userService.registerUser(any(UserRegistrationRequest.class)))
                .thenReturn(mockUser);

        mockMvc.perform(post("/api/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("신규유저"));
    }

    @Test
    @DisplayName("유저 포인트 잔액 조회 API 테스트")
    void getUserPointBalance() throws Exception {
        User mockUser = new User(1L, "테스트유저", BigDecimal.valueOf(50000), UserRole.CUSTOMER);

        when(userService.getUserById(anyLong()))
                .thenReturn(mockUser);

        mockMvc.perform(get("/api/user/point/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pointBalance").value(50000));
    }
}
