package hhplus.ecommerce.product.infrastructure.repository;

import hhplus.ecommerce.common.presentation.response.PageResponse;
import hhplus.ecommerce.product.domain.model.Product;
import hhplus.ecommerce.product.domain.repository.ProductRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryProductRepository implements ProductRepository {

    private final ConcurrentHashMap<Long, Product> productMap = new ConcurrentHashMap<>();

    @Override
    public Product save(Product product) {
        Long id = product.getProductId();

        if (id == null) {
            Product newProduct = Product.create(
                    product.getProductName(),
                    product.getCategory(),
                    product.getDescription(),
                    product.getImageUrl(),
                    product.getPrice(),
                    product.isExposed()
            );
            productMap.put(newProduct.getProductId(), newProduct);
            return newProduct;
        }

        productMap.put(product.getProductId(), product);
        return product;
    }

    @Override
    public Optional<Product> findById(Long productId) {
        return Optional.ofNullable(productMap.get(productId));
    }

    @Override
    public long countAll() {
        return productMap.size();
    }

    @Override
    public PageResponse<Product> findAllByCategoryWithPage(int page, int size, String category) {
        int offset = page * size;
        List<Product> productList;
        long totalElements;

        if (category != null && !category.isEmpty()) {
            // 카테고리 필터 적용
            productList = productMap.entrySet().stream()
                    .filter(entry -> entry.getValue().getCategory().equals(category))
                    .filter(entry -> entry.getValue().isExposed())
                    .sorted((h1, h2) -> h2.getValue().getCreatedAt().compareTo(h1.getValue().getCreatedAt()))
                    .skip(offset)
                    .limit(size)
                    .map(Map.Entry::getValue)
                    .toList();

            //  카테고리 필터링된 전체 개수 계산
            totalElements = productMap.values().stream()
                    .filter(product -> product.getCategory().equals(category))
                    .filter(Product::isExposed)
                    .count();
        } else {
            // 전체 상품 조회
            productList = productMap.entrySet().stream()
                    .filter(entry -> entry.getValue().isExposed())
                    .sorted((h1, h2) -> h2.getValue().getCreatedAt().compareTo(h1.getValue().getCreatedAt()))
                    .skip(offset)
                    .limit(size)
                    .map(Map.Entry::getValue)
                    .toList();

            // 전체 노출 상품 개수 계산
            totalElements = productMap.values().stream()
                    .filter(Product::isExposed)
                    .count();
        }

        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(productList, page, size, totalElements, totalPages);
    }

    @Override
    public PageResponse<Product> findAllByPriceWithPaging(int page, int size, boolean isAsc) {
        int offset = page * size;
        Comparator<Map.Entry<Long, Product>> comparator;

        // 가격 오름차순/내림차순 정렬
        if (isAsc) {
            comparator = Comparator.comparing((Map.Entry<Long, Product> e) -> e.getValue().getPrice());
        } else {
            comparator = Comparator.comparing((Map.Entry<Long, Product> e) -> e.getValue().getPrice()).reversed();
        }

        // 페이징 처리된 상품 리스트 조회
        List<Product> productList = productMap.entrySet().stream()
                .filter(entry -> entry.getValue().isExposed())
                .sorted(comparator)
                .skip(offset)
                .limit(size)
                .map(Map.Entry::getValue)
                .toList();

        // 전체 노출 상품 개수 계산
        long totalElements = productMap.values().stream()
                .filter(Product::isExposed)
                .count();

        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(productList, page, size, totalElements, totalPages);
    }

    @Override
    public List<Product> findTopN(int TopN, int searchDays) {
        return productMap.values().stream()
                .filter(product -> product.getCreatedAt().isAfter(LocalDateTime.now().minusDays(searchDays)))
                .sorted((h1, h2) -> Long.compare(h2.getSalesCount(), h1.getSalesCount()))
                .limit(TopN)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<Product> findAllBySalesWithPaging(int page, int size) {
        int offset = page * size;

        List<Product> productList = productMap.entrySet().stream()
                .filter(entry -> entry.getValue().isExposed())
                .sorted((e1, e2) -> Long.compare(e2.getValue().getSalesCount(), e1.getValue().getSalesCount()))
                .skip(offset)
                .limit(size)
                .map(Map.Entry::getValue)
                .toList();

        // 전체 노출 상품 개수 계산
        long totalElements = productMap.values().stream()
                .filter(Product::isExposed)
                .count();

        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(productList, page, size, totalElements, totalPages);
    }
}
