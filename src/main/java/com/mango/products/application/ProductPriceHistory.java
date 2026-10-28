package com.mango.products.application;

import com.mango.products.domain.model.Price;
import com.mango.products.domain.model.Product;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record ProductPriceHistory(Product product, List<Price> prices, @Nullable PageInfo page) {

}
