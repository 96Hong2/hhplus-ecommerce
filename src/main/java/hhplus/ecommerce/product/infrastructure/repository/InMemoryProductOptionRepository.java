package hhplus.ecommerce.product.infrastructure.repository;

import hhplus.ecommerce.product.domain.model.ProductOption;
import hhplus.ecommerce.product.domain.repository.ProductOptionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Repository
public class InMemoryProductOptionRepository implements ProductOptionRepository {
    private final ConcurrentHashMap<Long, ProductOption> productOptionMap = new ConcurrentHashMap<>();

    @Override
    public ProductOption save(ProductOption productOption) {
        if (productOption.getProductOptionId() == null) {
            ProductOption newProductOption = ProductOption.create(
                    productOption.getProductId(),
                    productOption.getOptionName(),
                    productOption.getPriceAdjustment(),
                    productOption.getStockQuantity(),
                    productOption.isExposed()
            );
            productOptionMap.put(newProductOption.getProductOptionId(), newProductOption);
            return newProductOption;
        }

        productOptionMap.put(productOption.getProductOptionId(), productOption);
        return productOption;
    }

    @Override
    public Optional<ProductOption> findById(Long productOptionId) {
        return Optional.ofNullable(productOptionMap.get(productOptionId));
    }

    @Override
    public List<ProductOption> findAllByProductId(Long productId) {
        return productOptionMap.values().stream()
                .filter(e -> e.getProductId().equals(productId))
                .collect(Collectors.toList());
    }
}
