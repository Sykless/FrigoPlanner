package com.fra.frigoplanner.ui.adapter;

import static android.graphics.Typeface.ITALIC;
import static android.graphics.Typeface.NORMAL;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fra.frigoplanner.R;
import com.fra.frigoplanner.data.model.ComptesProduct;
import com.fra.frigoplanner.ui.view.ProductEditText;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final List<ComptesProduct> productList;
    private OnExpirationDateChangeListener listener;
    private final List<String> productNamesDico;
    private static final String[] PRODUCT_TYPES = {
            "Bouffe - Repas",
            "Bouffe - Condiments",
            "Bouffe - Dessert",
            "Bouffe - Apéro",
            "Bouffe - Goûter",
            "Boissons - Alcool",
            "Boissons - Soda",
            "Boissons - Chaudes",
            "Cuisine",
            "Salle de Bain",
            "Santé",
            "Pito",
            "Drip",
            "Loisirs",
            "Micromaniac",
            "Transport",
            "Exceptionnel",
            "Rinçage"};

    public interface OnExpirationDateChangeListener {
        void onExpirationDatesSet(boolean expirationDatesSet);
    }

    public ProductAdapter(List<ComptesProduct> productList, List<String> productNamesDico) {
        this.productList = productList;
        this.productNamesDico = productNamesDico;
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
        ComptesProduct product = productList.get(position);

        holder.productName.setText(product.getCurrentName());
        holder.productName.setCandidates(productNamesDico);
        holder.productName.setShowCandidates(true);
        holder.productPrice.setText(String.format(Locale.FRANCE, "%.2f", product.getProductPrice()));
        holder.expirationDate.setText(product.getExpirationDate());
        holder.expirationDate.setTypeface(null, "DD/MM/YYYY".equals(product.getExpirationDate()) ? NORMAL : ITALIC);
        holder.productDateLayout.setVisibility(product.getExpirationDate() != null ? VISIBLE : INVISIBLE);

        ArrayAdapter<String> productTypeAdapter = (ArrayAdapter<String>) holder.productTypeSpinner.getAdapter();
        int spinnerPosition = productTypeAdapter.getPosition(product.getProductType());
        if (spinnerPosition >= 0) {
            holder.productTypeSpinner.setSelection(spinnerPosition);
        }

        // Set red background if there's a sum mismatch
        if (product.isMismatch()) {
            holder.productCard.setCardBackgroundColor(Color.RED);
        } else {
            holder.productCard.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.grey_menu));
        }

        // Set yellow background to ticket restaurant products
        if (product.isTicketRestaurant()) {
            holder.productCard.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.yellow_ticketrestau)
            );
        }

        // Switch ticket or product name
        holder.switchTicketName.setOnClickListener(v -> {
            product.setCurrentName(holder.productName.getText().toString());
            product.setDisplayTicketName(!product.isDisplayTicketName());
            holder.productName.setShowCandidates(!product.isDisplayTicketName());
            this.notifyItemChanged(position);
        });

        // Display DatePicker when clicking on expiration date
        holder.expirationDate.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    v.getContext(),
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String date = String.format(Locale.getDefault(), "%02d/%02d/%04d",
                                selectedDay, selectedMonth + 1, selectedYear);

                        // Update expiration date
                        product.setExpirationDate(date);
                        holder.expirationDate.setText(date);
                        holder.expirationDate.setTypeface(null, NORMAL);
                        checkAllExpirationDates();
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

        // Remove expiration date if held
        holder.expirationDate.setOnLongClickListener(view -> {
            product.setExpirationDate(null);
            holder.productDateLayout.setVisibility(INVISIBLE);
            checkAllExpirationDates();
            return true;
        });

        // Update product price and total cost when a price EditText is closed
        holder.productPrice.setOnBackPressedListener(() -> {
            product.setProductPrice(Double.parseDouble(holder.productPrice.getText().toString().replace(',', '.')));
            holder.productPrice.clearEditText(holder.euroSymbol);
            holder.productPrice.post(this::recalculateTotal);
        });

        // Update product name when a name EditText is closed
        holder.productName.setOnBackPressedListener(() -> {
            product.setCurrentName(holder.productName.getText().toString());
            holder.productPrice.clearEditText(holder.euroSymbol);
        });

        holder.productName.setOnFocusChangeListener((v, hasFocus) -> {
            product.setCurrentName(holder.productName.getText().toString());
            holder.productName.setFocused(hasFocus);
        });

        // Supply candidates
        holder.productName.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (holder.productName.enoughToFilter()) {
                    holder.productName.showDropDown();
                }
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        // Update product type when selected on the dropdown list
        holder.productTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                product.setProductType(parent.getItemAtPosition(position).toString());
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {};
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public void setOnExpirationDateChangeListener(OnExpirationDateChangeListener listener) {
        this.listener = listener;
    }

    private void checkAllExpirationDates() {
        boolean expirationDatesSet = productList.stream()
                .noneMatch(p -> "DD/MM/YYYY".equals(p.getExpirationDate()));

        listener.onExpirationDatesSet(expirationDatesSet);
    }

    private void recalculateTotal() {
        double countedTotal = 0;
        double readTotal = 0;
        double readCBTotal = 0;
        double readTicketRestaurantTotal = 0;
        int totalCostId = productList.size() - 1;

        // Retrieve all read totals and calculate true total cost
        for (int productId = 0 ; productId < productList.size() ; productId++) {
            ComptesProduct product = productList.get(productId);

            // Type of total : save it for later comparison
            if (product.getTotalType() != null) {
                switch (product.getTotalType())
                {
                    case TOTAL:
                        totalCostId = productId;
                        readTotal = product.getProductPrice();
                        break;

                    case TOTAL_TICKETRESTAURANT:
                        readTicketRestaurantTotal = product.getProductPrice();
                        break;

                    case TOTAL_CB:
                    case TOTAL_CB_CONTACTLESS:
                        readCBTotal = product.getProductPrice();
                        break;
                }
            }
            // Regular product : add price to counted total cost
            else {
                countedTotal += product.getProductPrice();
            }
        }

        // Check if the total matches the sum of products
        productList.get(totalCostId).setMismatch(
                Math.abs(readTotal - readCBTotal - readTicketRestaurantTotal) > 0.001
                        || Math.abs(readTotal - countedTotal) > 0.001
        );

        // Update total card
        this.notifyItemChanged(totalCostId);
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ProductEditText productName, productPrice;
        TextView expirationDate, expirationDateText, switchTicketName, euroSymbol;
        CardView productCard;
        Spinner productTypeSpinner;
        LinearLayout productDateLayout;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            productCard = itemView.findViewById(R.id.productCard);
            expirationDate = itemView.findViewById(R.id.expirationDate);
            expirationDateText = itemView.findViewById(R.id.expirationDateText);
            switchTicketName = itemView.findViewById(R.id.switchTicketName);
            euroSymbol = itemView.findViewById(R.id.euroSymbol);
            productTypeSpinner = itemView.findViewById(R.id.productTypeSpinner);
            productDateLayout = itemView.findViewById(R.id.productDateLayout);

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                    itemView.getContext(),
                    android.R.layout.simple_spinner_item,
                    PRODUCT_TYPES
            );
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            productTypeSpinner.setAdapter(spinnerAdapter);
        }
    }
}
