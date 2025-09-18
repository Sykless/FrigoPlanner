package com.fra.frigoplanner.ui;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fra.frigoplanner.R;
import com.fra.frigoplanner.data.model.Product;
import com.fra.frigoplanner.ui.adapter.ProductAdapter;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TicketDisplayFragment extends Fragment
{
    private static final String TAG = "FrigoPlanner";
    private List<Product> productList = new ArrayList<>();
    private ActivityResultLauncher<Intent> ticketReaderLauncher;
    FloatingActionButton cameraButton;
    ExtendedFloatingActionButton uploadButton;

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
        uploadButton.setVisibility(INVISIBLE);
        uploadButton.setOnClickListener(v -> {
            uploadTicketTxt();
        });

        // Setup TicketReader display once ticket has been processed
        ticketReaderLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        productList = data.getParcelableArrayListExtra("productList", Product.class);

                        // Display upload button
                        uploadButton.setVisibility(VISIBLE);

                        // Display productList in the RecyclerView
                        RecyclerView recyclerView = requireActivity().findViewById(R.id.productRecyclerView);
                        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
                        recyclerView.setAdapter(new ProductAdapter(productList));
                    }
                }
        );
    }

    public void startTextAnalysis() {
        Intent intent = new Intent(requireActivity(), TicketReader.class);
        ticketReaderLauncher.launch(intent);
    }

    public void uploadTicketTxt()
    {
        // Only upload ticket if products have been retrieved
        if (productList.isEmpty()) {
            Toast.makeText(requireActivity(), "Aucun ticket à uploader", Toast.LENGTH_SHORT).show();
            return;
        }

        // Call Google Drive API in a dedicated thread
        new Thread(() -> {
            try {
                Drive driveService = ((MainActivity) requireActivity()).getDriveService();

                // Retrieve FrigoPlanner folder in Google Drive
                FileList result = driveService.files()
                        .list()
                        .setQ("mimeType = 'application/vnd.google-apps.folder' and name = 'FrigoPlanner' and trashed = false")
                        .setFields("files(id, name)")
                        .execute();

                // Upload productList content in the FrigoPlanner folder
                if (!result.getFiles().isEmpty()) {
                    String folderId = result.getFiles().get(0).getId();

                    // Create empty file under FrigoPlanner folder
                    File fileMetadata = new File();
                    fileMetadata.setParents(Collections.singletonList(folderId));

                    // Set file name as current timestamp
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH-mm-ss");
                    fileMetadata.setName(LocalDateTime.now().format(formatter)  + ".txt");

                    // Set file content as productList converted to .txt data
                    StringBuilder fileContent = new StringBuilder();
                    productList.forEach(product -> fileContent.append(product.isTotal() ? "" : product.toString()));
                    ByteArrayContent mediaContent = new ByteArrayContent(
                            "text/plain",
                            fileContent.toString().getBytes(StandardCharsets.UTF_8)
                    );

                    // Upload file in Google Drive
                    driveService.files()
                            .create(fileMetadata, mediaContent)
                            .setFields("id, parents")
                            .execute();

                    // Empty productList so we can retrieve a new one
                    productList.clear();

                    // Notify user
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireActivity(), "Ticket uploadé !", Toast.LENGTH_SHORT).show());
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Error uploading ticket on Google Drive", e);
            }
        }).start();
    }
}