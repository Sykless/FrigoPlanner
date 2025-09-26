package com.fra.frigoplanner.ui.fragments;

import static android.view.View.VISIBLE;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.fra.frigoplanner.R;
import com.fra.frigoplanner.data.db.ProductDatabase;
import com.fra.frigoplanner.data.db.dao.ProductDicoDao;
import com.fra.frigoplanner.data.db.dao.ProductTypeDicoDao;
import com.fra.frigoplanner.data.db.dao.TempProductDao;
import com.fra.frigoplanner.data.db.dao.TicketNameDicoDao;
import com.fra.frigoplanner.data.db.entity.ProductDico;
import com.fra.frigoplanner.data.db.entity.ProductTypeDico;
import com.fra.frigoplanner.data.db.entity.TempProduct;
import com.fra.frigoplanner.data.db.entity.TicketNameDico;
import com.fra.frigoplanner.data.drive.DriveManager;
import com.fra.frigoplanner.data.model.ComptesProduct;
import com.fra.frigoplanner.ui.activities.MainActivity;
import com.fra.frigoplanner.ui.activities.TicketReaderActivity;
import com.fra.frigoplanner.ui.adapter.ProductAdapter;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TicketDisplayFragment extends Fragment
{
    private static final String TAG = "FrigoPlanner";
    private List<ComptesProduct> productList = new ArrayList<>();
    private ActivityResultLauncher<Intent> ticketReaderLauncher;
    private FloatingActionButton cameraButton;
    private ExtendedFloatingActionButton uploadButton;
    private ProductAdapter productAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ticket_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cameraButton = view.findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(v -> {
            startTextAnalysis();
        });

        uploadButton = view.findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(v -> {
            saveTicket();
        });

        // Setup TicketReader display once ticket has been processed
        ticketReaderLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK)
                    {
                        Intent data = result.getData();
                        productList = data.getParcelableArrayListExtra("productList", ComptesProduct.class);

                        // Run DAO query off the UI thread
                        new Thread(() -> {
                            ProductDatabase db = ProductDatabase.getInstance(requireContext());
                            ProductDicoDao dicoDao = db.productDicoDao();
                            List<String> productNamesDico = dicoDao.getAllProductNames();

                            requireActivity().runOnUiThread(() -> {
                                // Create adapter once candidates are ready
                                productAdapter = new ProductAdapter(productList, productNamesDico);

                                // Display upload button
                                uploadButton.setVisibility(VISIBLE);
                                uploadButton.setEnabled(true);

                                // Setup RecyclerView
                                RecyclerView recyclerView = requireActivity().findViewById(R.id.productRecyclerView);
                                recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
                                recyclerView.setAdapter(productAdapter);

                                // Remove light fade animation when reloading a card
                                if (recyclerView.getItemAnimator() instanceof SimpleItemAnimator) {
                                    ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
                                }

                                // Enable upload button if all expiration dates are set
                                /*productAdapter.setOnExpirationDateChangeListener(
                                        expirationDatesSet -> uploadButton.setEnabled(expirationDatesSet)
                                );*/
                            });
                        }).start();
                    }
                }
        );
    }

    public void startTextAnalysis() {
        Intent intent = new Intent(requireActivity(), TicketReaderActivity.class);
        ticketReaderLauncher.launch(intent);
    }

    public void saveTicket()
    {
        // Call Google Drive API and Database in a dedicated thread
        new Thread(() -> {
            ProductDatabase db = ProductDatabase.getInstance(requireContext());
            TicketNameDicoDao ticketDicoDao = db.ticketNameDicoDao();
            ProductDicoDao productDicoDao = db.productDicoDao();
            ProductTypeDicoDao productTypeDicoDao = db.productTypeDicoDao();
            TempProductDao tempProductDao = db.tempProductDao();

            // Save timestamp for future file names
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH-mm-ss");
            String fileName = LocalDateTime.now().format(formatter);

            // Add every ticket name in the TicketNameDico table
            for (int productId = 0 ; productId < productList.size() ; productId++) {
                ComptesProduct product = productList.get(productId);

                // Save each ticket name in TicketNameDico to facilitate next ticket reading
                if (product.getProductPrice() >= 0) {
                    String productName = product.getProductName().strip();
                    String ticketName = product.getTicketName().strip();
                    String productType = product.getProductType();

                    if (!productName.isEmpty() && !ticketName.isEmpty())
                    {
                        // Populate ProductDico table, ignored if already present in db
                        ProductDico productDico = new ProductDico(productName);
                        productDicoDao.insert(productDico);

                        ProductTypeDico productTypeDico = productTypeDicoDao.getProduct(productName, productType);

                        // Match product type with product name, increase occurrence if already present
                        if (productTypeDico != null) {
                            productTypeDicoDao.increaseOccurrence(productName, productType);
                        } else {
                            productTypeDico = new ProductTypeDico(productName, productType);
                            productTypeDicoDao.insert(productTypeDico);
                        }

                        // Populate TicketNameDico table, ignored if already present in db
                        TicketNameDico ticketDico = new TicketNameDico(productName, ticketName);
                        ticketDicoDao.insert(ticketDico);

                        // Populate TempProduct table for future adding to Product table
                        TempProduct tempProduct = new TempProduct(fileName, productId,
                                productName, productType, product.getProductPrice(), product.getExpirationDate());
                        tempProductDao.insert(tempProduct);
                    }
                }
            }

            // Upload ticket as txt manually importable in Comptes.ods
            uploadTicketTxt(fileName);

            // Upload TicketNameDico to backup ticket names
            backupTicketNames(fileName);

            // Empty productList so we can retrieve a new one
            productList.clear();
            requireActivity().runOnUiThread(() -> productAdapter.notifyDataSetChanged());
        }).start();
    }

    public void uploadTicketTxt(String fileName)
    {
        // Set file content as productList converted to .txt data
        StringBuilder fileContent = new StringBuilder();
        productList.forEach(product -> fileContent.append(product.getTotalType() != null ? "" : product.toString()));

        // Upload ticket file in Google Drive
        DriveManager driveManager = ((MainActivity) requireActivity()).getDriveManager();
        driveManager.uploadFile("Tickets", fileName + ".txt", fileContent.toString());
    }

    public void backupTicketNames(String fileName) {
        try {
            // Retrieve all ticket names from the database
            ProductDatabase db = ProductDatabase.getInstance(requireContext());
            TicketNameDicoDao ticketNameDicoDao = db.ticketNameDicoDao();
            List<TicketNameDico> ticketNamesList = ticketNameDicoDao.getAll();

            // Convert ticket names list to JSON
            Gson gson = new Gson();
            String json = gson.toJson(ticketNamesList);

            // Upload backup file in Google Drive
            DriveManager driveManager = ((MainActivity) requireActivity()).getDriveManager();
            driveManager.uploadFile("Backup", fileName + ".json", json);
        }
        catch (Exception e) {
            Log.e("TicketDisplayFragment", "Error exporting TicketNameDico", e);
        }
    }
}