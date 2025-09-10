package com.fra.frigoplanner.data.model;

import com.fra.frigoplanner.data.db.dao.BouffeDicoDao;
import com.fra.frigoplanner.data.db.entity.BouffeDico;

import java.util.HashMap;
import java.util.Map;

public class TicketProduct
{
    private final Map<String, Integer> possibleNames = new HashMap<>();
    private final Map<String, Integer> possiblePrices = new HashMap<>();

    private String validatedName = null;
    private String validatedPrice = null;

    public Product createValidatedProduct(BouffeDicoDao dicoDao) {
        BouffeDico bouffeDico = dicoDao.getByTicketName(this.validatedName);
        String productName = bouffeDico != null ? bouffeDico.name : "";

        return new Product(this.validatedName,
                Double.parseDouble(this.validatedPrice),
                productName);
    }

    public void addNameCandidate(String name) {
        if (!name.isEmpty()) {
            possibleNames.merge(name, 1, Integer::sum);
            validateNameIfStable();
        }
    }

    public void addPriceCandidate(String price) {
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
