package com.fra.frigoplanner;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Groceries
{
    private final List<TicketProduct> ticketProductList = new ArrayList<>();
    private int numberOfOcurrences = 0;

    Groceries(int listSize) {
        for (int i = 0 ; i < listSize ; i++) {
            ticketProductList.add(new TicketProduct());
        }
    }

    public void increaseOcurrences() {
        this.numberOfOcurrences++;
    }

    public boolean addProduct(int productId, String productName, String productPrice)
    {
        TicketProduct targetProduct = this.ticketProductList.get(productId);
        targetProduct.addNameCandidate(productName);
        targetProduct.addPriceCandidate(productPrice);
        return targetProduct.isValidated();
    }

    public List<TicketProduct> getProductList() {
        return ticketProductList;
    }
}

class TicketProduct
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

class Product implements Parcelable
{
    private String productName = null;
    private String productPrice = null;

    public Product(String productName, String productPrice) {
        this.productName = productName;
        this.productPrice = productPrice;
    }

    protected Product(Parcel in) {
        productName = in.readString();
        productPrice = in.readString();
    }

    public static final Creator<Product> CREATOR = new Creator<Product>() {
        @Override
        public Product createFromParcel(Parcel in) {
            return new Product(in);
        }

        @Override
        public Product[] newArray(int size) {
            return new Product[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(productName);
        parcel.writeString(productPrice);
    }
}