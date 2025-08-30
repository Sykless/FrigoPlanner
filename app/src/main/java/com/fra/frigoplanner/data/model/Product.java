package com.fra.frigoplanner.data.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Product implements Parcelable
{
    private String productName;
    private String productPrice;

    public Product(String productName, String productPrice) {
        this.productName = productName;
        this.productPrice = productPrice;
    }

    protected Product(Parcel in) {
        productName = in.readString();
        productPrice = in.readString();
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
        parcel.writeString(productName);
        parcel.writeString(productPrice);
    }
}
