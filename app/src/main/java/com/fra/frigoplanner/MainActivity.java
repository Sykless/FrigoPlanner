package com.fra.frigoplanner;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FrigoPlanner";
    private Drive driveService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Allow for better integration with top bar
        EdgeToEdge.enable(this);

        // Set margins according to device top/bottom bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Configure Google login options
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_READONLY))
                .build();

        GoogleSignInClient client = GoogleSignIn.getClient(this, gso);

        // Login to Google services
        ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);

                        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                                this, Collections.singleton(DriveScopes.DRIVE_READONLY));
                        credential.setSelectedAccount(account.getAccount());

                        // Populate Google Drive service object
                        driveService = new Drive.Builder(
                                new NetHttpTransport(),
                                JacksonFactory.getDefaultInstance(),
                                credential
                        ).setApplicationName("FrigoPlanner").build();
                    }
                    catch (ApiException e) {
                        e.printStackTrace();
                    }
                }
            }
        );

        // Launch Google login activity
        googleSignInLauncher.launch(client.getSignInIntent());
    }

    public void downloadComptesFile(View button) {
        new Thread(() -> {
            try {
                // Retrive Comptes.ods from Google Drive
                List<File> files = driveService.files().list()
                        .setQ("name = 'Comptes.ods' and trashed = false")
                        .setFields("files(id, name)")
                        .setPageSize(2)
                        .execute()
                        .getFiles();

                // If a file is found, download it in app local storage
                if (files != null && !files.isEmpty()) {
                    java.io.File file = new java.io.File(getFilesDir(), "Comptes.ods");
                    OutputStream output = new FileOutputStream(file);

                    // Download Comptes.ods from Google Drive ID
                    driveService.files().get(files.get(0).getId()).executeMediaAndDownloadTo(output);
                    output.close();
                } else {
                    Log.e(TAG, "Comptes.ods not found in Drive");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading Comptes.ods", e);
            }
        }).start();
    }
}