package com.fra.frigoplanner;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
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

import org.xmlpull.v1.XmlPullParser;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "FrigoPlanner";
    private static final int JANUARY = 3;
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
                        } catch (ApiException e) {
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
                long startTime = System.currentTimeMillis();

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

                    processComptesFile(file);
                    Log.d("ODS", "Total process completed in " + (System.currentTimeMillis() - startTime) + " ms");
                }
                else {
                    Log.e(TAG, "Comptes.ods not found in Drive");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading Comptes.ods", e);
            }
        }).start();
    }

    private Map<Integer, Map<Integer, List<String>>> parseOdsFile(java.io.File odsFile) {

        // Store file data in a dedicated year -> <columnId, rows> map
        Map<Integer, Map<Integer, List<String>>> parsedFile = new HashMap<>();

        // ODS files are actually zip files with data store in a content.xml
        try (ZipFile zipFile = new ZipFile(odsFile)) {
            ZipEntry contentFile = zipFile.getEntry("content.xml");

            if (contentFile == null) {
                Log.e("ODS", "content.xml not found");
                return null;
            }

            // Extract content.xml file as an XML parser object
            InputStream inputStream = zipFile.getInputStream(contentFile);
            XmlPullParser xmlParser = Xml.newPullParser();
            xmlParser.setInput(inputStream, "UTF-8");

            // Store parse state in dedicated variables
            int currentRow = -1;
            int currentColumn = 0;

            Map<Integer, List<String>> currentSheet = null;
            String currentSheetName = null;
            boolean yearSheet = false;

            // Iterate on content.xml
            while (xmlParser.next() != XmlPullParser.END_DOCUMENT) {
                String tagName = xmlParser.getName();

                // New tag : Differentiate rows, cells and other tags
                if (xmlParser.getEventType() == XmlPullParser.START_TAG)
                {
                    // Sheet
                    if ("table".equals(tagName)) {
                        currentSheetName = xmlParser.getAttributeValue(null, "name");
                        yearSheet = currentSheetName != null && currentSheetName.startsWith("20");

                        if (yearSheet) {
                            currentSheet = new HashMap<>();
                            parsedFile.put(Integer.parseInt(currentSheetName), currentSheet);
                            currentRow = -1;
                        }
                    }
                    // Only parse year sheets
                    else if (yearSheet)
                    {
                        // Row
                        if ("table-row".equals(tagName)) {
                            currentRow++;
                            currentColumn = 0;
                        }
                        // Cell only before 30 columns
                        else if ("table-cell".equals(tagName) && currentRow > 300 && currentColumn < 30) {
                            int repeatedColumns = 1;
                            StringBuilder cellValue = new StringBuilder();

                            // Save number of repeated columns if any
                            String repeatColumn = xmlParser.getAttributeValue(null, "number-columns-repeated");
                            if (repeatColumn != null) {
                                repeatedColumns = Integer.parseInt(repeatColumn);
                            }

                            // Handle merged cells
                            String mergedCell = xmlParser.getAttributeValue(null, "number-columns-spanned");
                            if (mergedCell != null) {
                                repeatedColumns = Math.max(repeatedColumns, Integer.parseInt(mergedCell));
                            }

                            // Go to next tag
                            int parserState = xmlParser.next();

                            // Retrieve cell value from text tag
                            if ("p".equals(xmlParser.getName()) && parserState == XmlPullParser.START_TAG) {
                                while (!(xmlParser.getEventType() == XmlPullParser.END_TAG && "p".equals(xmlParser.getName())))
                                {
                                    // Text with no tag : append it
                                    if (xmlParser.getEventType() == XmlPullParser.TEXT) {
                                        cellValue.append(xmlParser.getText());
                                    }

                                    xmlParser.next();
                                }
                            }

                            // Add cell value to parsedFile, repeat if repeated columns
                            for (int i = 0 ; i < repeatedColumns ; i++) {
                                List<String> rowCells = currentSheet.computeIfAbsent(currentColumn, k -> new ArrayList<>());

                                // Populate row with empty cells if not already defined
                                while (rowCells.size() < currentRow) rowCells.add("");

                                // Add cell value to target columnId/row
                                rowCells.add(cellValue.toString().trim());
                                currentColumn++;

                                // Stop parsing after 30 columns
                                if (currentColumn > 30) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            inputStream.close();
        }
        catch (Exception e) {
            Log.e("ODS", "Error parsing ODS", e);
            return null;
        }

        return parsedFile;
    }

    public void processComptesFile(java.io.File odsFile) {
        try
        {
            long startTime = System.currentTimeMillis();
            Map<Integer, Map<Integer, List<String>>> parsedFile = parseOdsFile(odsFile);

            Log.d("ODS", "Parse completed in " + (System.currentTimeMillis() - startTime) + " ms");

            // Find the column containing "Tickets de Caisse" by iterating on January rows
            Map<Integer, List<String>> currentYearSheet = parsedFile.get(2025);
            List<String> januaryRows = currentYearSheet.get(JANUARY);
            int startingRow = -1;

            for (int row = 300 ; row < januaryRows.size() && startingRow < 0 ; row++) {
                if ("Tickets de Caisse".equals(januaryRows.get(row))) {
                    startingRow = row + 3;
                }
            }

            Log.i("ODS", "Products start at column " + startingRow);

            final int finalStartingRow = startingRow;
            BouffeDatabase db = BouffeDatabase.getInstance(this);
            BouffeDao dao = db.productDao();

            new Thread(() -> {
                for (int monthCol = JANUARY; monthCol < Collections.max(currentYearSheet.keySet()); monthCol += 5) {
                    List<String> names = currentYearSheet.get(monthCol);
                    List<String> types = currentYearSheet.get(monthCol + 1);

                    if (names == null || types == null) continue;

                    int minSize = Math.min(names.size(), types.size());

                    for (int row = finalStartingRow ; row < minSize ; row++) {
                        String name = names.get(row).trim();
                        String type = types.get(row).trim();

                        if ("Bouffe - Repas".equals(type) || "Bouffe - Condiments".equals(type)) {
                            Bouffe b = new Bouffe(2025, monthCol, row, name, type);
                            dao.insert(b);
                        }
                    }
                }
            }).start();
        }
        catch (Exception e) {
            Log.e("ODS", "Error parsing ODS", e);
        }
    }
}