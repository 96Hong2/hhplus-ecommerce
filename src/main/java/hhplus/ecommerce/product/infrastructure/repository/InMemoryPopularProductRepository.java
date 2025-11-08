package hhplus.ecommerce.product.infrastructure.repository;

import hhplus.ecommerce.product.domain.model.PopularProduct;
import hhplus.ecommerce.product.domain.repository.PopularProductRepository;
import org.springframework.stereotype.Repository;

import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryPopularProductRepository implements PopularProductRepository {

    private final CopyOnWriteArrayList<PopularProduct> popularProductList = new CopyOnWriteArrayList<>();

    @Override
    public PopularProduct save(PopularProduct popularProduct) {
        if (popularProduct.getPopularProductId() == null) {
            PopularProduct newPopularProduct = PopularProduct.create(
                    popularProduct.getProductId(),
                    popularProduct.getSalesCount(),
                    popularProduct.getCalculationDate(),
                    popularProduct.getRank()
            );
            popularProductList.add(newPopularProduct);
            return newPopularProduct;
        }

        popularProductList.add(popularProduct);
        return popularProduct;
    }
}
