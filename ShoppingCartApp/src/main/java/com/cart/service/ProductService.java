package com.cart.service;

import java.util.List;

import com.cart.entity.Product;
import com.cart.model.ProductInfo;

public interface ProductService {
	
	public Product findProduct(String code);
	
	public List<Product> getList();
	 
    public void save(ProductInfo productInfo);
    
}
