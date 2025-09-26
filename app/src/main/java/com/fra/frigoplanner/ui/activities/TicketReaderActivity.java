package com.fra.frigoplanner.ui.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fra.frigoplanner.data.db.BouffeDatabase;
import com.fra.frigoplanner.R;
import com.fra.frigoplanner.data.db.dao.ProductTypeDicoDao;
import com.fra.frigoplanner.data.db.dao.TicketNameDicoDao;
import com.fra.frigoplanner.data.model.Groceries;
import com.fra.frigoplanner.data.model.ComptesProduct;
import com.fra.frigoplanner.data.model.TicketProduct;
import com.fra.frigoplanner.data.model.TotalType;
import com.fra.frigoplanner.ui.view.RectView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TicketReaderActivity extends AppCompatActivity {

    private TextView statusText;
    private RectView rectView;
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private TextRecognizer textRecognizer;
    Map<Integer, Groceries> groceryList = new HashMap<>();
    private static final String TAG = "FrigoPlanner";
    int validatedProducts = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ticket_reader);

        // Allow for better integration with top bar
        EdgeToEdge.enable(this);

        // Set margins according to device top/bottom bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Grant access to camera
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
        groceryList = new HashMap<>();

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

        if (mediaImage != null)
        {
            // Remove light grey pixels to avoid ghost text
            Bitmap processed = preprocessReceipt(mediaImage);

            // Convert Bitmap image to InputImage so it can be read
            InputImage image = InputImage.fromBitmap(processed, imageProxy.getImageInfo().getRotationDegrees());

            // Start text processing
            textRecognizer.process(image)
                    .addOnSuccessListener(text -> processText(imageProxy, text))
                    .addOnFailureListener(e -> Log.e(TAG, "Text recognition failed", e))
                    .addOnCompleteListener(task -> imageProxy.close());
        }
        else {
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

                // Detect top border from SIRET position
                if (getLevenshteinDistance(lineText, "N SIRET: 84498809700011") >= 0.35) {
                    topBorder = line.getBoundingBox().bottom;
                }

                // Detect bottom border from ARTICLES position and calculate angle
                if (getLevenshteinDistance(lineText, "NOMBRE D'ARTICLES VENDUS") >= 0.35) {
                    bottomBorder = line.getBoundingBox().top;

                    // Calculate letters orientation
                    Point[] corners = line.getCornerPoints();
                    double angleRad = Math.atan2(corners[2].y - corners[3].y, corners[2].x - corners[3].x);
                    int rawAngleDeg = (int) Math.toDegrees(angleRad);
                    int angle = rawAngleDeg + (rawAngleDeg < -45 ? 90 : 0);

                    // If the letters are not straight enough, display an error
                    if (angle > 3 || angle < -3) {
                        runOnUiThread(() -> statusText.setText("Angle de " + angle + "° trop penché"));
                        return null;
                    }
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
        // Only process text is camera is still running
        if (cameraExecutor == null || cameraExecutor.isShutdown()) {
            return;
        }

        // Only process text is activity is still running
        if (isFinishing() || isDestroyed()) {
            return;
        }

        // Calculate zone in which products are listed
        Rect borders = retrieveTicketBorders(text);

        // Products detected : process the text
        if (borders != null)
        {
            // Draw a red rectangle around the zone
            rectView.setRect(mapRectToView(borders,
                    imageProxy.getWidth(), imageProxy.getHeight(),
                    previewView.getWidth(), previewView.getHeight(),
                    imageProxy.getImageInfo().getRotationDegrees()));

            // Run a dedicated thread to run database requests
            cameraExecutor.execute(() -> {
                List<Text.Line> allLines = new ArrayList<>();

                // Store all detected text in a list
                for (Text.TextBlock block : text.getTextBlocks()) {
                    allLines.addAll(block.getLines());
                }

                // Sort by vertical position (top of bounding box)
                Collections.sort(allLines, Comparator.comparingInt(
                        e -> e.getBoundingBox().top));

                // Keep only elements inside product zone
                List<Text.Line> productLines = allLines.stream()
                        .filter(e -> e.getBoundingBox().top > borders.top
                                && e.getBoundingBox().bottom < borders.bottom)
                        .toList();

                // Estimate average text height to use as tolerance
                int tolerance = (int) (0.4 * productLines.stream()
                        .mapToInt(line -> line.getBoundingBox().height())
                        .average()
                        .orElse(0));

                // Store product name and price in two separate lists
                List<List<Text.Line>> productNameList = new ArrayList<>();
                List<StringBuilder> productPriceList = new ArrayList<>();

                // Calculate horizontal split between product name and price
                int priceSplit = (borders.right - borders.left) * 6 / 10;
                int lastTopPosition = -1;
                int currentLine = -1;

                // Iterate over each filtered line
                for (Text.Line line : productLines) {
                    int topPosition = line.getBoundingBox().top;

                    // Position difference too large : we're on a new line
                    if (lastTopPosition == -1 || Math.abs(topPosition - lastTopPosition) > tolerance) {
                        currentLine++; // New line

                        // Add new product
                        productNameList.add(new ArrayList<>());
                        productPriceList.add(new StringBuilder(""));
                    }

                    // After price split : save price if valid format
                    if (line.getBoundingBox().left >= priceSplit)
                    {
                        // Only keep digits and save commas as dots
                        String lineText = line.getText()
                                .replaceAll("[^0-9.,-]", "")
                                .replace(",",".");

                        // Save price if value is a valid price
                        if (lineText.matches("-?\\d{1,3}.\\d{2}")) {
                            productPriceList.get(currentLine).append(lineText);
                        }
                    }
                    // Before price split : add line to product name
                    else if (!line.getText().isEmpty()) {
                        productNameList.get(currentLine).add(line);
                    }

                    lastTopPosition = topPosition;
                }

                // Store product list in a global groceries list
                Groceries groceries = groceryList.computeIfAbsent(productNameList.size(), Groceries::new);
                groceries.increaseOcurrences();
                validatedProducts = 0;

                // Retrieve all possible ticket names from database
                BouffeDatabase db = BouffeDatabase.getInstance(this);
                TicketNameDicoDao ticketDicoDao = db.ticketNameDicoDao();
                ProductTypeDicoDao productTypeDicoDao = db.productTypeDicoDao();
                List<String> ticketNameDico = ticketDicoDao.getAllTicketNames();

                // Iterate over each read product name to put them together
                for (int i = 0 ; i < productNameList.size() ; i++)
                {
                    // Sort horizontally inside the line
                    productNameList.get(i).sort(Comparator.comparingInt(
                            line -> line.getBoundingBox().left));

                    // Concat each word in the line
                    String lineText = productNameList.get(i).stream()
                            .map(Text.Line::getText)
                            .collect(Collectors.joining(" "))
                            .toUpperCase();

                    // Find closest ticket name from database ticket dico
                    String ticketName = getClosestTicketName(lineText, ticketNameDico);

                    // For lines under the format 2 X 3.45 EUR, update price for previous line
                    if (ticketName.matches("\\d{1,2}[ ]{1,3}X[ ]{1,3}\\d{1,3}.\\d{2}[ ]{1,3}EUR")) {
                        groceries.getTicketProduct(i - 1).addPriceCandidate(productPriceList.get(i).toString());
                    }

                    // Add best match to grocery list and check if the product has been validated
                    validatedProducts += groceries.addProduct(i, ticketName, productPriceList.get(i).toString()) ? 1 : 0;
                }

                // Every product has been validated : return to previous menu
                if (validatedProducts == productNameList.size())
                {
                    ArrayList<ComptesProduct> productList = new ArrayList<>();
                    double totalBimpliCost = 0;

                    // Convert each TicketProduct to Product objects
                    for (int productId = 0 ; productId < groceries.getProductList().size() ; productId++)
                    {
                        TicketProduct ticketProduct = groceries.getProductList().get(productId);
                        ComptesProduct validatedProduct = ticketProduct.createValidatedProduct(ticketDicoDao, productTypeDicoDao);
                        productList.add(validatedProduct);

                        // Tag all total and subtotals so we don't treat them as products
                        switch (ticketProduct.getValidatedName())
                        {
                            case "MONTANT DU":
                                validatedProduct.setTotalType(TotalType.TOTAL);
                                break;

                            case "TRD BIMPLI":
                                totalBimpliCost = validatedProduct.getProductPrice();
                                validatedProduct.setTotalType(TotalType.TOTAL_TICKETRESTAURANT);
                                break;

                            case "CB EMV":
                                validatedProduct.setTotalType(TotalType.TOTAL_CB);
                                break;

                            case "CB SANS CONTACT":
                                validatedProduct.setTotalType(TotalType.TOTAL_CB_CONTACTLESS);
                                break;
                        }
                    }

                    // Detect which products were bought with Ticket Restaurant
                    if (totalBimpliCost > 0)
                    {
                        // Filter out ticket totals, only keep actual products
                        List<ComptesProduct> ticketRestauList = new ArrayList<>();
                        List<ComptesProduct> filteredProductList = productList.stream()
                                .filter(product -> product.getTotalType() == null)
                                .collect(Collectors.toList());

                        // Find products bought with Ticket Restaurant from their price and total Ticket Restaurant price
                        if (backtrack(filteredProductList, 0, totalBimpliCost, ticketRestauList)) {
                            ticketRestauList.forEach(product -> product.setTicketRestaurant(true));
                        }
                    }

                    // Send product names and prices and go back to previous menu
                    runOnUiThread(() -> {
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.putParcelableArrayListExtra("productList", productList);
                        setResult(RESULT_OK, intent);
                        finish();
                    });
                }
            });

            statusText.setText(validatedProducts + " items found");
        }
        else {
            rectView.setRect(null);
        }
    }

    private String getClosestTicketName(String productName, List<String> ticketNameDico) {
        String bestMatch = "";
        double bestDistance = 0;

        // Check every ticket name in the dico to find the closest match
        for (String ticketName : ticketNameDico) {
            double distance = getLevenshteinDistance(productName, ticketName);

            if (distance > bestDistance) {
                bestDistance = distance;
                bestMatch = ticketName;
            }
        }

        // If the best distance is not high enough, keep the original product name
        if (bestDistance < 0.85) {
            bestMatch = productName;
        }

        return bestMatch;
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

    private Bitmap preprocessReceipt(android.media.Image originalImage)
    {
        // Convert original image (YUV format) to RGB format
        Bitmap bitmapImage = yuvToRgb(originalImage);
        int width = bitmapImage.getWidth();
        int height = bitmapImage.getHeight();

        // Store each pixel in a 2D Array
        int[] pixels = new int[width * height];
        bitmapImage.getPixels(pixels, 0, width, 0, 0, width, height);

        // Iterate over each pixel
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];

            // Calculate Grey value from Red/Green/Blue values
            int r = (pixel >> 16) & 0xff;
            int g = (pixel >> 8) & 0xff;
            int b = pixel & 0xff;
            int grey = (r + g + b) / 3;

            // Remove lighter greys
            if (grey > 150) {
                pixels[i] = 0xffffffff;
            }
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    private Bitmap yuvToRgb(Image image) {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("Expected YUV_420_888 format");
        }

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        // Copy Y plane
        yBuffer.get(nv21, 0, ySize);

        // Interleave V and U to NV21 format
        byte[] uBytes = new byte[uSize];
        byte[] vBytes = new byte[vSize];
        uBuffer.get(uBytes);
        vBuffer.get(vBytes);

        for (int i = 0; i < uSize; i++) {
            nv21[ySize + i * 2] = vBytes[i];
            nv21[ySize + i * 2 + 1] = uBytes[i];
        }

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);
        byte[] jpegBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
    }

    private double getLevenshteinDistance(String text, String target) {
        LevenshteinDistance levenshtein = LevenshteinDistance.getDefaultInstance();
        int rawDistance = levenshtein.apply(text, target);
        return 1.0 - (double) rawDistance / Math.max(text.length(), target.length());
    }

    private boolean backtrack(List<ComptesProduct> prices, int index, double remaining, List<ComptesProduct> subset) {
        if (Math.abs(remaining) < 0.001) return true;
        if (index >= prices.size()) return false;

        // include current item
        subset.add(prices.get(index));
        if (backtrack(prices, index + 1, remaining - prices.get(index).getProductPrice(), subset)) return true;

        // exclude current item
        subset.remove(subset.size() - 1);
        return backtrack(prices, index + 1, remaining, subset);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        textRecognizer.close();
    }
}
