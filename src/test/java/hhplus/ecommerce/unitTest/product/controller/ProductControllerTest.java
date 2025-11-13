package hhplus.ecommerce.unitTest.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hhplus.ecommerce.common.presentation.response.PageResponse;
import hhplus.ecommerce.product.application.service.ProductMapper;
import hhplus.ecommerce.product.application.service.ProductService;
import hhplus.ecommerce.product.domain.model.Product;
import hhplus.ecommerce.product.domain.model.ProductOption;
import hhplus.ecommerce.product.presentation.controller.ProductController;
import hhplus.ecommerce.product.presentation.dto.request.ProductRegistrationRequest;
import hhplus.ecommerce.product.presentation.dto.response.ProductDetailResponse;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private ProductMapper productMapper;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = Product.create(
                "테스트 상품",
                "전자제품",
                "테스트 상품입니다",
                "http://image.url",
                BigDecimal.valueOf(10000),
                true
        );
    }

    @Test
    @DisplayName("상품 목록 조회 API 테스트")
    void getProducts() throws Exception {
        PageResponse<Product> mockPageResponse = new PageResponse<>(
                List.of(testProduct),
                0,
                20,
                1,
                1
        );

        when(productService.getProducts(anyInt(), anyInt(), anyString(), any()))
                .thenReturn(mockPageResponse);

        mockMvc.perform(get("/api/product")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "latest"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("상품 상세 조회 API 테스트")
    void getProductDetail() throws Exception {
        // ProductDetailResponse 생성자 파라미터 순서 수정
        ProductDetailResponse mockResponse = new ProductDetailResponse(
                1L,                          // productId
                "테스트 상품",                // productName
                "전자제품",                   // category
                "테스트 상품입니다",          // description
                "http://image.url",          // imageUrl
                BigDecimal.valueOf(10000),   // price
                true,                        // isExposed
                null,                        // createdAt
                null,                        // updatedAt
                List.of()                    // productOptions
        );

        when(productService.getProductDetail(anyLong()))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/product/{productId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("테스트 상품"));
    }

    @Test
    @DisplayName("상품 등록 API 테스트")
    void createProduct() throws Exception {
        ProductRegistrationRequest request = new ProductRegistrationRequest();
        request.setProductName("신규 상품");
        request.setCategory("전자제품");
        request.setDescription("신규 상품입니다");
        request.setImageUrl("http://image.url");
        request.setPrice(BigDecimal.valueOf(20000));
        request.setExposed(true);

        when(productService.registerProduct(anyString(), anyString(), anyString(), anyString(), any(), anyBoolean()))
                .thenReturn(testProduct);

        mockMvc.perform(post("/api/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인기 상품 조회 API 테스트")
    void getTopProducts() throws Exception {
        when(productService.getTopProducts(anyInt(), anyInt()))
                .thenReturn(List.of(testProduct));

        mockMvc.perform(get("/api/product/top")
                        .param("size", "5"))
                .andExpect(status().isOk());
    }
}
