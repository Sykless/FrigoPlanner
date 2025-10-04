package com.fra.frigoplanner.data.model;

import com.fra.frigoplanner.data.db.dao.ProductDicoDao;
import com.fra.frigoplanner.data.db.dao.ProductTypeDicoDao;
import com.fra.frigoplanner.data.db.dao.TicketNameDicoDao;
import com.fra.frigoplanner.data.db.entity.ProductDico;
import com.fra.frigoplanner.data.db.entity.TicketNameDico;

import java.util.HashMap;
import java.util.Map;

public class TicketProduct
{
    private final Map<String, Integer> possibleNames = new HashMap<>();
    private final Map<String, Integer> possiblePrices = new HashMap<>();

    private String validatedName = null;
    private String validatedPrice = null;

    public TicketProduct() {}

    // Only used when knowing for sure the validated names/prices
    public TicketProduct(String validatedName, String validatedPrice) {
        this.validatedName = validatedName;
        this.validatedPrice = validatedPrice;
    }

    public ComptesProduct createValidatedProduct(TicketNameDicoDao dicoDao, ProductTypeDicoDao productTypeDicoDao) {
        TicketNameDico productDico = dicoDao.getByTicketName(this.validatedName);
        String productName = productDico != null ? productDico.productName : "";

        // Retrieve product type from database if a match has been found in ProductDico
        String productType = productName.isEmpty() ? "Bouffe - Repas" // Default value
                : productTypeDicoDao.getMostFrequentProductType(productName); // Take the most frequent produt type in database

        return new ComptesProduct(this.validatedName,
                Double.parseDouble(this.validatedPrice),
                productName, productType);
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

    public void findMostProbableName() {
        if (validatedName == null) {
            int numberOfOccurrences = 0;

            for (Map.Entry<String, Integer> entrySet : possibleNames.entrySet()) {
                if (entrySet.getValue() > numberOfOccurrences) {
                    numberOfOccurrences = entrySet.getValue();
                    validatedName = entrySet.getKey();
                }
            }
        }
    }

    public void findMostProbablePrice() {
        if (validatedPrice == null) {
            int numberOfOccurrences = 0;

            for (Map.Entry<String, Integer> entrySet : possiblePrices.entrySet()) {
                if (entrySet.getValue() > numberOfOccurrences) {
                    numberOfOccurrences = entrySet.getValue();
                    validatedPrice = entrySet.getKey();
                }
            }
        }
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

    public void setValidatedName(String validatedName) {
        this.validatedName = validatedName;
    }

    public void setValidatedPrice(String validatedPrice) {
        this.validatedPrice = validatedPrice;
    }
}
