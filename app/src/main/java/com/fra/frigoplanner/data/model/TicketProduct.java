package com.fra.frigoplanner.data.model;

import java.util.HashMap;
import java.util.Map;

public class TicketProduct
{
    private final Map<String, Integer> possibleNames = new HashMap<>();
    private final Map<String, Integer> possiblePrices = new HashMap<>();

    private String validatedName = null;
    private String validatedPrice = null;

    public Product createValidatedProduct() {
        return new Product(this.validatedName, this.validatedPrice);
    }

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
