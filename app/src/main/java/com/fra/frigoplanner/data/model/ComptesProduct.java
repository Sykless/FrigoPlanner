package com.fra.frigoplanner.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Locale;

public class ComptesProduct implements Parcelable
{
    private String ticketName;
    private String productName;
    private String productType;
    private String expirationDate = "DD/MM/YYYY";
    private double productPrice;
    private TotalType totalType = null;
    private boolean ticketRestaurant = false;
    private boolean mismatch = false;
    private boolean displayTicketName;

    public ComptesProduct(String ticketName, double productPrice, String productName, String productType) {
        this.ticketName = ticketName;
        this.productPrice = productPrice;
        this.productName = productName;
        this.productType = productType;
        this.displayTicketName = productName.isEmpty();
    }

    @NonNull
    public String toString() {
        return this.getBestProductName() + "\t"
            + (this.productType != null ? this.productType : "") + "\t"
            + (this.ticketRestaurant ? "R" : "") + "\t"
            + String.format(Locale.FRANCE, "%.2f", this.productPrice) + "\n";
    }

    public String getBestProductName() {
        return this.productName.isEmpty() ? this.ticketName : this.productName;
    }

    public String getCurrentName() {
        return this.displayTicketName ? this.ticketName : this.productName;
    }

    public void setCurrentName(String name) {
        if (this.displayTicketName) {
            this.ticketName = name;
        } else {
            this.productName = name;
        }
    }

    public String getProductName() {
        return this.productName;
    }

    public String getTicketName() {
        return this.ticketName;
    }

    public String getProductType() {
        return this.productType;
    }

    public String getExpirationDate() {
        return this.expirationDate;
    }

    public double getProductPrice() {
        return this.productPrice;
    }

    public TotalType getTotalType() {
        return this.totalType;
    }

    public boolean isTicketRestaurant() {
        return this.ticketRestaurant;
    }

    public boolean isMismatch() {
        return this.mismatch;
    }

    public boolean isDisplayTicketName() {
        return this.displayTicketName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public void setProductPrice(double productPrice) {
        this.productPrice = productPrice;
    }

    public void setTicketRestaurant(boolean ticketRestaurant) {
        this.ticketRestaurant = ticketRestaurant;
    }

    public void setTotalType(TotalType totalType) {
        this.totalType = totalType;
    }

    public void setMismatch(boolean mismatch) {
        this.mismatch = mismatch;
    }

    public void setDisplayTicketName(boolean displayTicketName) {
        this.displayTicketName = displayTicketName;
    }

    protected ComptesProduct(Parcel in) {
        ticketName = in.readString();
        productName = in.readString();
        productType = in.readString();
        expirationDate = in.readString();
        productPrice = in.readDouble();
        ticketRestaurant = in.readBoolean();
        mismatch = in.readBoolean();
        displayTicketName = in.readBoolean();

        String typeName = in.readString();
        totalType = typeName == null ? null : TotalType.valueOf(typeName);
    }

    public static final Creator<ComptesProduct> CREATOR = new Creator<>() {
        @Override
        public ComptesProduct createFromParcel(Parcel in) {
            return new ComptesProduct(in);
        }

        @Override
        public ComptesProduct[] newArray(int size) {
            return new ComptesProduct[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(ticketName);
        parcel.writeString(productName);
        parcel.writeString(productType);
        parcel.writeString(expirationDate);
        parcel.writeDouble(productPrice);
        parcel.writeBoolean(ticketRestaurant);
        parcel.writeBoolean(mismatch);
        parcel.writeBoolean(displayTicketName);
        parcel.writeString(totalType == null ? null : totalType.name());
    }
}
