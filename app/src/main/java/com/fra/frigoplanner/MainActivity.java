package com.fra.frigoplanner;


import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import com.google.mlkit.vision.text.TextRecognizer;

import java.util.concurrent.ExecutorService;

import android.content.Intent;
import android.util.Xml;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

    public void startTextAnalysis(View button) {
        Intent intent = new Intent(this, TicketReader.class);
        startActivityForResult(intent, 1001); // request code
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

                    Log.d("ODS", "Download completed in " + (System.currentTimeMillis() - startTime) + " ms");

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
                        // Cell only before 65 columns
                        else if ("table-cell".equals(tagName) && currentRow > 300 && currentColumn < 65) {
                            StringBuilder cellValue = new StringBuilder();
                            int repeatedColumns = 1;
                            int spannedColumns = 1;

                            // Save number of repeated columns if any
                            String repeatColumn = xmlParser.getAttributeValue(null, "number-columns-repeated");
                            if (repeatColumn != null) {
                                repeatedColumns = Integer.parseInt(repeatColumn);
                            }

                            // Save number of merged columns if any
                            String mergedCell = xmlParser.getAttributeValue(null, "number-columns-spanned");
                            if (mergedCell != null) {
                                spannedColumns = Integer.parseInt(mergedCell);
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
                            for (int r = 0 ; r < repeatedColumns ; r++) {
                                List<String> rowCells = currentSheet.computeIfAbsent(currentColumn, k -> new ArrayList<>());

                                // Populate row with empty cells if not already defined
                                while (rowCells.size() < currentRow) rowCells.add("");

                                // Add cell value to target columnId/row
                                rowCells.add(cellValue.toString().trim());
                                currentColumn++;

                                // Fill merged columns with empty placeholders
                                for (int s = 1 ; s < spannedColumns ; s++) {
                                    List<String> spannedCells = currentSheet.computeIfAbsent(currentColumn, k -> new ArrayList<>());

                                    // Populate row with empty cells if not already defined
                                    while (spannedCells.size() < currentRow) spannedCells.add("");

                                    // Add empty value to target columnId/row because part of a merged cell
                                    spannedCells.add("");
                                    currentColumn++;
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

            // Convert ODS file to 2D Array
            Map<Integer, Map<Integer, List<String>>> parsedFile = parseOdsFile(odsFile);
            Log.d("ODS", "Parse completed in " + (System.currentTimeMillis() - startTime) + " ms");

            new Thread(() -> {
                BouffeDatabase db = BouffeDatabase.getInstance(this);
                BouffeDao bouffeDao = db.productDao();
                BouffeDicoDao dicoDao = db.dicoDao();

                // Default : retrieve any bouffe from after 10/2023
                Bouffe latestBouffe = bouffeDao.getLatestBouffe();
                int startMonth = 10;
                int startYear = 2023;

                // Data already present : only retrieve bouffe from the last 6 months
                if (latestBouffe != null) {
                    startMonth = (latestBouffe.month - 5) % 12;
                    startYear = latestBouffe.year - (latestBouffe.month < 6 ? 1 : 0);
                }

                // Iterate on each sheet/year
                for (Map.Entry<Integer, Map<Integer, List<String>>> mapEntry : parsedFile.entrySet()) {
                    Map<Integer, List<String>> yearSheet = mapEntry.getValue();
                    Integer year = mapEntry.getKey();

                    // Only retrieve data from years after startYear
                    if (year < startYear) {
                        continue;
                    }

                    // Find the column containing "Tickets de Caisse" by iterating on January rows
                    List<String> januaryRows = yearSheet.get(JANUARY);
                    int startingRow = -1;

                    for (int row = 300 ; row < januaryRows.size() && startingRow < 0 ; row++) {
                        if ("Tickets de Caisse".equals(januaryRows.get(row))) {
                            startingRow = row + 3;
                        }
                    }

                    // Iterate on each month
                    for (int month = 1 ; month <= 12 ; month++)
                    {
                        // Only retrieve data from months after startMonth
                        if (year == startYear && month < startMonth) {
                            continue;
                        }

                        // Convert month number to column number
                        int monthCol = 5 * (month - 1) + 3;
                        List<String> bouffeList = yearSheet.get(monthCol);
                        List<String> typeList = yearSheet.get(monthCol + 1);
                        List<String> priceList = yearSheet.get(monthCol + 3);

                        // Iterate until we reach the end of one row
                        int minRowNumber = Math.min(priceList.size(), Math.min(bouffeList.size(), typeList.size()));

                        // Iterate on each row
                        for (int row = startingRow ; row < minRowNumber ; row++) {
                            String bouffeType = typeList.get(row).trim();
                            String bouffeName = bouffeList.get(row).trim();

                            if (!bouffeType.isEmpty() && !bouffeName.isEmpty())
                            {
                                // Convert price from string to double
                                double price = Double.parseDouble(
                                        priceList.get(row).trim()
                                                .replace(" €","")
                                                .replace(",","."));

                                // Skip bouffe with negative price (price reductions)
                                if (price >= 0) {
                                    BouffeDico bouffeDico = new BouffeDico(bouffeName, 0, 0, null, null);
                                    dicoDao.insert(bouffeDico);

                                    Bouffe bouffe = new Bouffe(year, month, row - startingRow, bouffeName, bouffeType, price);
                                    bouffeDao.insert(bouffe);
                                }
                            }
                        }
                    }
                }
            }).start();
        }
        catch (Exception e) {
            Log.e("ODS", "Error parsing Comptes file", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            ArrayList<Product> products = getIntent().getParcelableArrayListExtra("productList", Product.class);
        }
    }
}