package hhplus.ecommerce.user.presentation.controller;

import hhplus.ecommerce.common.presentation.response.ApiResponse;
import hhplus.ecommerce.common.presentation.response.PageResponse;
import hhplus.ecommerce.user.application.service.UserService;
import hhplus.ecommerce.user.domain.model.User;
import hhplus.ecommerce.user.presentation.dto.request.UserRegistrationRequest;
import hhplus.ecommerce.user.presentation.dto.response.UserPointBalanceResponse;
import hhplus.ecommerce.user.presentation.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public PageResponse<UserResponse> getUserListWithPage(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (size > 100) {
            size = 100;
        }

        PageResponse<User> userPage = userService.getUserListWithPage(role, page, size);

        List<UserResponse> userResponses = userPage.getContent().stream()
                .map(UserResponse::of)
                .collect(Collectors.toList());

        return new PageResponse<>(
                userResponses,
                userPage.getPage(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages()
        );
    }

    @PostMapping
    public ApiResponse<UserResponse> registerUser(@RequestBody UserRegistrationRequest request) {
        User user = userService.registerUser(request);
        return ApiResponse.success(UserResponse.of(user));
    }

    @GetMapping("/point/{userId}")
    public ApiResponse<UserPointBalanceResponse> getUserPointBalance(@PathVariable Long userId) {
        return ApiResponse.success(UserPointBalanceResponse.of(userService.getUserById(userId)));
    }
}
