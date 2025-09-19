package com.fra.frigoplanner.ui.adapter;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fra.frigoplanner.R;
import com.fra.frigoplanner.data.model.Product;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final List<Product> productList;
    private OnExpirationDateChangeListener listener;
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
            "Bar",
            "Transport"};

    public interface OnExpirationDateChangeListener {
        void onExpirationDatesSet(boolean expirationDatesSet);
    }

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
        holder.productPrice.setText(String.format(Locale.FRANCE, "%.2f", product.getProductPrice()));

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
                        holder.expirationDate.setTypeface(null, Typeface.NORMAL);
                        checkAllExpirationDates();
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

        // Remove expiration date if held
        holder.expirationDate.setOnLongClickListener(view -> {
            product.setExpirationDate(null);
            holder.productDateLayout.setVisibility(View.INVISIBLE);
            checkAllExpirationDates();
            return true;
        });

        // Update product price and total cost when a price EditText is updated
        holder.productPrice.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
            {
                // Update product price and recalculate total cost
                product.setProductPrice(Double.parseDouble(v.getText().toString().replace(',', '.')));
                recalculateTotal();

                // Hide keyboard and remove focus
                clearEditText(v);
                return true;
            }
            return false;
        });

        // Update product name when a name EditText is updated
        holder.productName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
            {
                // Update product name
                product.setProductName(v.getText().toString());

                // Hide keyboard and remove focus
                clearEditText(v);
                return true;
            }
            return false;
        });

        // Update product type when selected on the dropdown list
        holder.productTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                product.setProductType(parent.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {};
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
            Product product = productList.get(productId);

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

    private void clearEditText(View editText)
    {
        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

        // Clear focus
        editText.clearFocus();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        EditText productName, productPrice;
        TextView expirationDate, expirationDateText;
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
