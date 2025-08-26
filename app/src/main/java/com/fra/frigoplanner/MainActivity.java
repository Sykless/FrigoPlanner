package com.fra.frigoplanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.apache.commons.text.similarity.LevenshteinDistance;
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
    private TextView statusText;
    private RectView rectView;
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private TextRecognizer textRecognizer;

    Map<String, Integer> priceCounts = new HashMap<>();
    int foundItems = 0;

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        }

        // Bind statusText view to object in the code
        previewView = findViewById(R.id.previewView);
        statusText = findViewById(R.id.statusText);
        rectView = findViewById(R.id.rectView);

        // Declare Camera and Text analyser objects
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Start filming
        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Display Camera recording in PreviewView
                androidx.camera.core.Preview preview = new androidx.camera.core.Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Analyse camera feed in real time
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setResolutionSelector(
                                new ResolutionSelector.Builder()
                                        .setResolutionStrategy(
                                                new ResolutionStrategy(
                                                        new Size(1440, 1440),
                                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                                        ).build())
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                // Display feed from back camera
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Make sure the camera is only used when the app is open
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (Exception e) {
                Log.e("MLKitOCR", "Camera initialization failed.", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("UnsafeOptInUsageError") // ImageProxy.getImage is experimental
    private void analyzeImage(ImageProxy imageProxy) {
        android.media.Image mediaImage = imageProxy.getImage();

        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            // Start text processing
            textRecognizer.process(image)
                    .addOnSuccessListener(text -> processText(imageProxy, text))
                    .addOnFailureListener(e -> Log.e("MLKitOCR", "Text recognition failed", e))
                    .addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    private Rect retrieveTicketBorders(Text text)
    {
        int topBorder = -1, bottomBorder = -1;
        int minLeft = Integer.MAX_VALUE, maxRight = Integer.MIN_VALUE;

        // Make sure text is displayed to start processing
        if (text.getTextBlocks().isEmpty()) {
            runOnUiThread(() -> statusText.setText("Cannot find text"));
            return null;
        }

        // Iterate on each detected block of text
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines())
            {
                // Retrieve each separate line
                String lineText = line.getText().toUpperCase();

                // Detect top border from SIRET position and calculate angle
                if (getLevenshteinDistance(lineText, "N SIRET: 84498809700011") >= 0.5) {
                    topBorder = line.getBoundingBox().bottom;

                    // Calculate letters orientation
                    Point[] corners = line.getCornerPoints();
                    double angleRad = Math.atan2(corners[2].y - corners[3].y, corners[2].x - corners[3].x);
                    int rawAngleDeg = (int) Math.toDegrees(angleRad);
                    int angle = rawAngleDeg + (rawAngleDeg < -45 ? 90 : 0);

                    // If the letters are not straight enough, display an error
                    if (angle > 3) {
                        runOnUiThread(() -> statusText.setText("Angle de " + angle + "° trop élevé"));
                        return null;
                    }
                }

                // Detect bottom border from ARTICLES position
                if (getLevenshteinDistance(lineText, "NOMBRE D'ARTICLES VENDUS") >= 0.5) {
                    bottomBorder = line.getBoundingBox().top;
                }

                // Read each line within the borders to find the minLeft and maxRight border
                minLeft = Math.min(minLeft, line.getBoundingBox().left);
                maxRight = Math.max(maxRight, line.getBoundingBox().right);
            }
        }

        // Make sure the ticket contains the word "SIRET" to detect top border
        if (topBorder == -1) {
            runOnUiThread(() -> statusText.setText("Cannot find SIRET"));
            return null;
        }

        // Make sure the ticket contains the word "ARTICLES" to detect bottom border
        if (bottomBorder == -1) {
            runOnUiThread(() -> statusText.setText("Cannot find ARTICLES"));
            return null;
        }

        // Save each border in a Rect object
        return new Rect(minLeft, topBorder, maxRight, bottomBorder);
    }

    private void processText(ImageProxy imageProxy, Text text)
    {
        // Calculate zone in which products are listed
        Rect borders = retrieveTicketBorders(text);

        // Products detected : process the text
        if (borders != null)
        {
            // Draw a red rectangle around the zone
            Rect convertedRect = mapRectToView(borders,
                    imageProxy.getWidth(), imageProxy.getHeight(),
                    previewView.getWidth(), previewView.getHeight(),
                    imageProxy.getImageInfo().getRotationDegrees());

            rectView.setRect(convertedRect);
            statusText.setText(foundItems + " items found");
        }
        else {
            rectView.setRect(null);
        }
    }

    private Rect mapRectToView(Rect imageRect, int proxyImageWidth, int proxyImageHeight, int viewWidth, int viewHeight, int rotation)
    {
        // Swap width and height if the screen is rotated
        float imageWidth = (rotation == 90 || rotation == 270) ? proxyImageHeight : proxyImageWidth;
        float imageHeight = (rotation == 90 || rotation == 270) ? proxyImageWidth : proxyImageHeight;

        // Scale height and width depending on image proxy and screen resolution
        float scaleX = (float) viewWidth / imageWidth;
        float scaleY = (float) viewHeight / imageHeight;
        float scale = Math.max(scaleX, scaleY);

        // Calculate offsets if image is letterboxed
        float offsetX = (viewWidth - imageWidth * scale) / 2f;
        float offsetY = (viewHeight - imageHeight * scale) / 2f;

        // Calculate rect coordinates on view
        int left = (int)(Math.min(imageRect.left, imageRect.right) * scale + offsetX);
        int top = (int)(Math.min(imageRect.top, imageRect.bottom) * scale + offsetY);
        int right = (int)(Math.max(imageRect.left, imageRect.right) * scale + offsetX);
        int bottom = (int)(Math.max(imageRect.top, imageRect.bottom) * scale + offsetY);

        return new Rect(left, top, right, bottom);
    }

    private double getLevenshteinDistance(String text, String target) {
        LevenshteinDistance levenshtein = LevenshteinDistance.getDefaultInstance();
        int rawDistance = levenshtein.apply(text, target);
        return 1.0 - (double) rawDistance / Math.max(text.length(), target.length());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        textRecognizer.close();
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

            new Thread(() -> {
                BouffeDatabase db = BouffeDatabase.getInstance(this);
                BouffeDao bouffeDao = db.productDao();
                BouffeDicoDao dicoDao = db.dicoDao();

                // Default : retrieve any bouffe from after 10/2023
                Bouffe latestBouffe = bouffeDao.getLatestBouffe();
                int startMonth = 10;
                int startYear = 2023;

                // Data already present : only retrieve bouffe from last 6 months
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
                    for (int month = 1 ; month <= 12 ; month++) {

                        // Only retrieve data from months after startMonth
                        if (year == startYear && month < startMonth) {
                            continue;
                        }

                        int monthCol = 5 * (month - 1) + 3;
                        List<String> bouffeList = yearSheet.get(monthCol);
                        List<String> typeList = yearSheet.get(monthCol + 1);
                        List<String> priceList = yearSheet.get(monthCol + 3);

                        int minRowNumber = Math.min(priceList.size(), Math.min(bouffeList.size(), typeList.size()));

                        // Iterate on each row
                        for (int row = startingRow ; row < minRowNumber ; row++) {
                            String bouffeType = typeList.get(row).replace("Bouffe - ","").trim();

                            // Only retrieve bouffe you can make a dish with
                            if ("Repas".equals(bouffeType) || "Condiments".equals(bouffeType)) {
                                String bouffeName = bouffeList.get(row).trim();

                                // Convert price from string to double
                                String priceEuro = priceList.get(row).trim();
                                double price = Double.parseDouble(priceEuro.replace(" €","")
                                                                           .replace(",","."));

                                // Skip bouffe with negative price (price reductions)
                                if (price >= 0) {
                                    BouffeDico bouffeDico = new BouffeDico(bouffeName, 0, 0, null);
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
}