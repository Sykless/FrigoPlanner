package com.fra.frigoplanner.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fra.frigoplanner.R;
import com.fra.frigoplanner.data.db.BouffeDatabase;
import com.fra.frigoplanner.data.db.dao.ProductDao;
import com.fra.frigoplanner.data.db.dao.ProductDicoDao;
import com.fra.frigoplanner.data.db.dao.ProductTypeDicoDao;
import com.fra.frigoplanner.data.db.dao.TicketNameDicoDao;
import com.fra.frigoplanner.data.db.entity.Product;
import com.fra.frigoplanner.data.db.entity.ProductDico;
import com.fra.frigoplanner.data.db.entity.ProductTypeDico;
import com.fra.frigoplanner.data.db.entity.TicketNameDico;
import com.fra.frigoplanner.data.drive.DriveManager;
import com.fra.frigoplanner.ui.activities.MainActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ComptesFragment extends Fragment
{
    private static final String TAG = "FrigoPlanner";
    private static final int JANUARY = 3;
    FloatingActionButton refreshDrive;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_comptes_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        refreshDrive = view.findViewById(R.id.refreshDrive);
        refreshDrive.setOnClickListener(v -> {
            downloadComptesFile();
        });
    }

    public void downloadComptesFile()
    {
        // Call Google Drive API and Database in a dedicated thread
        new Thread(() ->
        {
            // Download Comptes.ods from Google Drive
            DriveManager driveManager = ((MainActivity) requireActivity()).getDriveManager();
            File comptesFile = driveManager.downloadFile("Comptes.ods");

            // Process Comptes.ods file and populate database
            processComptesFile(comptesFile);

            // Restore TicketNameDico backup if empty
            importTicketNameDico();
        }).start();
    }

    private Map<Integer, Map<Integer, List<String>>> parseOdsFile(java.io.File odsFile)
    {
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
            requireActivity().runOnUiThread(() -> Toast.makeText(requireActivity(), "Error parsing Comptes file", Toast.LENGTH_SHORT).show());
            Log.e("ODS", "Error parsing Comptes file", e);
            return null;
        }

        return parsedFile;
    }

    public void processComptesFile(java.io.File odsFile) {
        try  {
            // Convert ODS file to 2D Array
            Map<Integer, Map<Integer, List<String>>> parsedFile = parseOdsFile(odsFile);

            if (parsedFile != null)
            {
                // Retrieve database objects
                BouffeDatabase db = BouffeDatabase.getInstance(requireActivity());
                ProductDao productDao = db.productDao();
                ProductDicoDao dicoDao = db.productDicoDao();
                ProductTypeDicoDao productTypeDicoDao = db.productTypeDicoDao();
                TicketNameDicoDao ticketNameDicoDao = db.ticketNameDicoDao();

                // productDao.clearAll();
                // productTypeDicoDao.clearAll();
                // ticketNameDicoDao.clearAll();
                // dicoDao.clearAll();

                // Default : retrieve any bouffe from after 10/2023
                Product latestProduct = productDao.getLatestProduct();
                int startMonth = 10;
                int startYear = 2023;

                // Data already present : only retrieve bouffe from the last 6 months
                if (latestProduct != null) {
                    startMonth = (latestProduct.month - 5) % 12;
                    startYear = latestProduct.year - (latestProduct.month < 6 ? 1 : 0);
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
                                if (price >= 0)
                                {
                                    // Insert in ProductDico to enable foreign keys, skip if already present
                                    ProductDico productDico = new ProductDico(bouffeName);
                                    dicoDao.insert(productDico);

                                    ProductTypeDico productTypeDico = productTypeDicoDao.getProduct(bouffeName, bouffeType);

                                    // Match product type with product name, increase occurrence if already present
                                    if (productTypeDico != null) {
                                        productTypeDicoDao.increaseOccurrence(bouffeName, bouffeType);
                                    } else {
                                        productTypeDico = new ProductTypeDico(bouffeName, bouffeType);
                                        productTypeDicoDao.insert(productTypeDico);
                                    }

                                    Product product = new Product(year, month, row - startingRow, bouffeName, bouffeType, price);
                                    productDao.insert(product);
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            requireActivity().runOnUiThread(() -> Toast.makeText(requireActivity(), "Error processing Comptes file", Toast.LENGTH_SHORT).show());
            Log.e("ODS", "Error processing Comptes file", e);
        }
    }

    public void importTicketNameDico() {
        try {
            // Retrieve database objects
            BouffeDatabase db = BouffeDatabase.getInstance(requireActivity());
            TicketNameDicoDao ticketNameDicoDao = db.ticketNameDicoDao();
            ProductDicoDao productDicoDao = db.productDicoDao();

            // If TicketNameDico table is empty, restore backup from Google Drive
            if (ticketNameDicoDao.getTicketNamesCount() == 0)
            {
                // Start by adding "Total" type products to ProductDico since they're manual imports
                productDicoDao.insert(new ProductDico("Total"));
                productDicoDao.insert(new ProductDico("Total Ticket Restaurant"));
                productDicoDao.insert(new ProductDico("Total CB"));
                productDicoDao.insert(new ProductDico("Total CB Sans Contact"));

                // Add their ticket name equivalent
                ticketNameDicoDao.insert(new TicketNameDico("Total", "MONTANT DU"));
                ticketNameDicoDao.insert(new TicketNameDico("Total Ticket Restaurant", "TRD BIMPLI"));
                ticketNameDicoDao.insert(new TicketNameDico("Total CB", "CB EMV"));
                ticketNameDicoDao.insert(new TicketNameDico("Total CB Sans Contact", "CB SANS CONTACT"));

                // Retrieve heaviest TicketNameDico backup and convert it to JSON
                DriveManager driveManager = ((MainActivity) requireActivity()).getDriveManager();
                File jsonBackupFile = driveManager.getHeaviestBackup();
                String json = new String(Files.readAllBytes(jsonBackupFile.toPath()), StandardCharsets.UTF_8);

                // Convert JSON data to TicketNameDico and insert it in database to fill it back
                Gson gson = new Gson();
                Type listType = new TypeToken<List<TicketNameDico>>() {}.getType();
                List<TicketNameDico> entries = gson.fromJson(json, listType);
                ticketNameDicoDao.insertAll(entries);
            }
        }
        catch (Exception e) {
            Log.e("DriveManager", "Error reading Json Backup", e);
            requireActivity().runOnUiThread(() -> Toast.makeText(requireActivity(), "Error reading Json Backup", Toast.LENGTH_SHORT).show());
        }
    }
}
