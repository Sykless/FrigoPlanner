package com.fra.frigoplanner.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fra.frigoplanner.R;
import com.fra.frigoplanner.data.model.Product;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final List<Product> productList;

    public ProductAdapter(List<Product> productList) {
        this.productList = productList;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.product_layout, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.productName.setText(product.getProductName());
        holder.productPrice.setText(String.format(Locale.FRANCE, "%.2f", product.getProductPrice()) + " €");

        // Set yellow background to ticket restaurant products
        if (product.isTicketRestaurant()) {
            holder.productRoot.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.yellow_ticketrestau)
            );
        }

        // Set red background if there's a sum mismatch
        if (product.isMismatch()) {
            holder.productRoot.setBackgroundColor(Color.RED);
        }

        // Make line separator appear before total
        if (product.getProductName().equals("Total")) {
            holder.totalDivider.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productPrice;
        LinearLayout productRoot;
        View totalDivider;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            productRoot = itemView.findViewById(R.id.productRoot);
            totalDivider = itemView.findViewById(R.id.totalDivider);
        }
    }
}
