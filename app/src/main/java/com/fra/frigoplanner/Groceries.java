package com.fra.frigoplanner;

import android.os.Bundle;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Groceries
{
    private final List<Product> productList = new ArrayList<>();
    private int numberOfOcurrences = 0;

    Groceries(int listSize) {
        for (int i = 0 ; i < listSize ; i++) {
            productList.add(new Product());
        }
    }

    public void increaseOcurrences() {
        this.numberOfOcurrences++;
    }

    public boolean addProduct(int productId, String productName, String productPrice)
    {
        Product targetProduct = this.productList.get(productId);
        targetProduct.addNameCandidate(productName);
        targetProduct.addPriceCandidate(productPrice);
        return targetProduct.isValidated();
    }

    public List<Product> getProductList() {
        return productList;
    }
}

class Product
{
    private final Map<String, Integer> possibleNames = new HashMap<>();
    private final Map<String, Integer> possiblePrices = new HashMap<>();

    private String validatedName = null;
    private String validatedPrice = null;

    public void addNameCandidate(String name)
    {
        if (!name.isEmpty()) {
            possibleNames.merge(name, 1, Integer::sum);
            validateNameIfStable();
        }
    }

    public void addPriceCandidate(String price)
    {
        if (!price.isEmpty()) {
            possiblePrices.merge(price, 1, Integer::sum);
            validatePriceIfStable();
        }
    }

    private void validateNameIfStable() {
        possibleNames.forEach((name, count) -> {
            if (count >= 5 && validatedName == null) {
                validatedName = name;
            }
        });
    }

    private void validatePriceIfStable() {
        possiblePrices.forEach((price, count) -> {
            if (count >= 5 && validatedPrice == null) {
                validatedPrice = price;
            }
        });
    }

    public boolean isValidated() {
        return validatedName != null && validatedPrice != null;
    }

    public String getValidatedName() {
        return validatedName;
    }

    public String getValidatedPrice() {
        return validatedPrice;
    }
}
