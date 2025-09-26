package com.fra.frigoplanner.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.fra.frigoplanner.R;
import com.fra.frigoplanner.data.drive.DriveManager;
import com.fra.frigoplanner.ui.fragments.ComptesFragment;
import com.fra.frigoplanner.ui.fragments.PlaceholderFragment;
import com.fra.frigoplanner.ui.fragments.TicketDisplayFragment;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity
{
    private BottomNavigationView bottomNav;
    private Map<Integer, Fragment> fragments = new HashMap<>();
    private Fragment activeFragment;
    private DriveManager driveManager;

    public DriveManager getDriveManager() {
        return this.driveManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Allow for better integration with top bar
        EdgeToEdge.enable(this);

        // Set margins according to device top/bottom bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Retrieve status/navigation bars
        Window window = getWindow();
        WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(window, window.getDecorView());

        // Set status (top) bar white and icons dark
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        insetsController.setAppearanceLightStatusBars(true);

        // Set navigation (bottom) bar grey and icons dark
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.grey_menu));
        insetsController.setAppearanceLightNavigationBars(true);

        // Configure Google login options
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
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
                                    this, Collections.singleton(DriveScopes.DRIVE_FILE));
                            credential.setSelectedAccount(account.getAccount());

                            // Populate Google Drive service object
                            Drive driveService = new Drive.Builder(
                                    new NetHttpTransport(),
                                    JacksonFactory.getDefaultInstance(),
                                    credential
                            ).setApplicationName("FrigoPlanner").build();

                            // Save Google Drive service object in a DriveManager for future operations (upload/download)
                            driveManager = new DriveManager(driveService, this);
                        }
                        catch (ApiException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        // Launch Google login activity
        googleSignInLauncher.launch(client.getSignInIntent());

        // Initialize fragments
        fragments.put(R.id.menu_1, new PlaceholderFragment());
        fragments.put(R.id.menu_2, new PlaceholderFragment());
        fragments.put(R.id.menu_3, new PlaceholderFragment());
        fragments.put(R.id.menu_4, new TicketDisplayFragment());
        fragments.put(R.id.menu_5, new ComptesFragment());

        // Add all fragments but display only the first one
        for (Map.Entry<Integer, Fragment> entry : fragments.entrySet()) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

            // Active fragment : display it
            if (entry.getKey() == R.id.menu_1) {
                fragmentTransaction.add(R.id.fragmentContainer, entry.getValue(), entry.getValue().getClass().getSimpleName());
                activeFragment = entry.getValue();
            }
            // Other framgnents : hide them
            else {
                fragmentTransaction.add(R.id.fragmentContainer, entry.getValue(), entry.getValue().getClass().getSimpleName())
                        .hide(entry.getValue());
            }

            fragmentTransaction.commit();
        }

        // Set menu buttons logic
        bottomNav = findViewById(R.id.bottomMenu);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragmentToShow = fragments.get(item.getItemId());

            // Click on another menu fragment : display it and hide previous one
            if (fragmentToShow != null && fragmentToShow != activeFragment) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.setReorderingAllowed(true);
                ft.hide(activeFragment).show(fragmentToShow).commit();
                activeFragment = fragmentToShow;
            }

            return true;
        });
    }
}