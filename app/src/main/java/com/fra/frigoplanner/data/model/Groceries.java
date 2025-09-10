package com.fra.frigoplanner.data.model;

import java.util.ArrayList;
import java.util.List;

public class Groceries
{
    private final List<TicketProduct> ticketProductList = new ArrayList<>();
    private int numberOfOcurrences = 0;

    public Groceries(int listSize) {
        for (int i = 0 ; i < listSize ; i++) {
            ticketProductList.add(new TicketProduct());
        }
    }

    public TicketProduct getTicketProduct(int productId) {
        return this.ticketProductList.get(productId);
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