package com.fra.frigoplanner.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Locale;

public class Product implements Parcelable
{
    private String ticketName = "";
    private String productName = "";
    private double productPrice = 0;
    private boolean ticketRestaurant = false;
    private boolean isTotal = false;
    private boolean mismatch = false;

    public Product(String ticketName, double productPrice, String productName) {
        this.ticketName = ticketName;
        this.productPrice = productPrice;
        this.productName = productName;
    }

    @NonNull
    public String toString() {
        return this.getProductName() + "\t\t"
            + (this.ticketRestaurant ? "R" : "") + "\t"
            + String.format(Locale.FRANCE, "%.2f", this.productPrice) + "\n";
    }

    public String getProductName() {
        return this.productName.isEmpty() ? this.ticketName : this.productName;
    }

    public double getProductPrice() {
        return this.productPrice;
    }

    public boolean isTicketRestaurant() {
        return this.ticketRestaurant;
    }

    public boolean isTotal() {
        return this.isTotal;
    }

    public boolean isMismatch() {
        return this.mismatch;
    }

    public void setTicketRestaurant(boolean ticketRestaurant) {
        this.ticketRestaurant = ticketRestaurant;
    }

    public void setTotal(boolean isTotal) {
        this.isTotal = isTotal;
    }

    public void setMismatch(boolean mismatch) {
        this.mismatch = mismatch;
    }

    protected Product(Parcel in) {
        ticketName = in.readString();
        productName = in.readString();
        productPrice = in.readDouble();
        ticketRestaurant = in.readBoolean();
        isTotal = in.readBoolean();
        mismatch = in.readBoolean();
    }

    public static final Creator<Product> CREATOR = new Creator<>() {
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
        parcel.writeString(ticketName);
        parcel.writeString(productName);
        parcel.writeDouble(productPrice);
        parcel.writeBoolean(ticketRestaurant);
        parcel.writeBoolean(isTotal);
        parcel.writeBoolean(mismatch);
    }
}
