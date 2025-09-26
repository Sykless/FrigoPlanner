package com.fra.frigoplanner.data.drive;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class DriveManager
{
    private final Drive driveService;
    private final Activity activity;

    public DriveManager(Drive driveService, Activity activity) {
        this.driveService = driveService;
        this.activity = activity;
    }

    public String getFolderId(String folderName) {
        String folderId = null;

        try {
            // Retrieve FrigoPlanner folder in Google Drive
            FileList frigoResult = driveService.files()
                    .list()
                    .setQ("mimeType = 'application/vnd.google-apps.folder' and name = '" + folderName + "' and trashed = false")
                    .setFields("files(id, name)")
                    .execute();

            // If folder is found, return its id
            if (!frigoResult.getFiles().isEmpty()) {
                folderId = frigoResult.getFiles().get(0).getId();
            }
            // If no folder is found, notify user
            else {
                activity.runOnUiThread(() -> Toast.makeText(activity, "Dossier " + folderName + " introuvable", Toast.LENGTH_SHORT).show());
            }
        }
        catch (Exception e) {
            Log.e("DriveManager", "Error retrieving folder " + folderName + " on Google Drive", e);
            activity.runOnUiThread(() -> Toast.makeText(activity, "Error retrieving folder " + folderName + " on Google Drive", Toast.LENGTH_SHORT).show());
        }

        return folderId;
    }

    public String getFolderIdWithParent(String folderName, String folderParent) {
        String folderId = null;

        try {
            // Retrieve Drive folderId from folder name
            String parentFolderId = getFolderId(folderParent);

            // Retrieve folder with FrigoPlanner parent in Google Drive
            if (parentFolderId != null) {
                FileList result = driveService.files()
                        .list()
                        .setQ("mimeType = 'application/vnd.google-apps.folder' and name = '" + folderName + "' and '" + parentFolderId + "' in parents and trashed = false")
                        .setFields("files(id, name)")
                        .execute();

                // Upload productList content in the FrigoPlanner folder
                if (!result.getFiles().isEmpty()) {
                    folderId = result.getFiles().get(0).getId();
                }
                // If no folder is found, notify user
                else {
                    activity.runOnUiThread(() -> Toast.makeText(activity, "Dossier " + folderName + " introuvable", Toast.LENGTH_SHORT).show());
                }
            }
        }
        catch (Exception e) {
            Log.e("DriveManager", "Error retrieving folder " + folderName + " on Google Drive", e);
            activity.runOnUiThread(() -> Toast.makeText(activity, "Error retrieving folder " + folderName + " on Google Drive", Toast.LENGTH_SHORT).show());
        }

        return folderId;
    }

    public java.io.File downloadFile(String filename) {
        java.io.File downloadedFile = null;

        try {
            // Retrive file from Google Drive
            List<File> files = driveService.files().list()
                    .setQ("name = '" + filename + "' and trashed = false")
                    .setFields("files(id, name)")
                    .execute()
                    .getFiles();

            // If a file is found, download it in app local storage
            if (files != null && !files.isEmpty()) {
                downloadedFile = new java.io.File(activity.getFilesDir(), filename);
                OutputStream output = new FileOutputStream(downloadedFile);

                // Download file from Google Drive ID
                driveService.files().get(files.get(0).getId()).executeMediaAndDownloadTo(output);
                output.close();

                // Notify user
                activity.runOnUiThread(() -> Toast.makeText(activity, filename + " téléchargé !", Toast.LENGTH_SHORT).show());
            }
            else {
                activity.runOnUiThread(() -> Toast.makeText(activity, "Fichier " + filename + " introuvable", Toast.LENGTH_SHORT).show());
            }
        }
        catch (Exception e) {
            Log.e("DriveManager", "Error downloading file on Google Drive", e);
            activity.runOnUiThread(() -> Toast.makeText(activity, "Error downloading file on Google Drive", Toast.LENGTH_SHORT).show());
            return null;
        }

        return downloadedFile;
    }

    public void uploadFile(String folderName, String fileName, String fileContent) {
        try {
            // Retrieve Drive folderId from folder name and folder parent
            String parentFolderId = getFolderIdWithParent(folderName, "FrigoPlanner");

            // Upload file in the provided parent folder
            if (parentFolderId != null)
            {
                // Create empty file under FrigoPlanner folder
                File fileMetadata = new File();
                fileMetadata.setParents(Collections.singletonList(parentFolderId));
                fileMetadata.setName(fileName);

                // Populate file with provided file content
                ByteArrayContent mediaContent = new ByteArrayContent(
                        "text/plain",
                        fileContent.getBytes(StandardCharsets.UTF_8)
                );

                // Upload file in Google Drive
                driveService.files()
                        .create(fileMetadata, mediaContent)
                        .setFields("id, parents")
                        .execute();

                // Notify user
                activity.runOnUiThread(() -> Toast.makeText(activity, fileName + " uploadé !", Toast.LENGTH_SHORT).show());
            }
        }
        catch (Exception e) {
            Log.e("DriveManager", "Error uploading file on Google Drive", e);
            activity.runOnUiThread(() -> Toast.makeText(activity, "Error uploading file on Google Drive", Toast.LENGTH_SHORT).show());
        }
    }

    public java.io.File getHeaviestBackup() {
        java.io.File heaviestBackup = null;

        try {
            // Retrieve Backup folder id
            String backupFolderId = getFolderIdWithParent("Backup","FrigoPlanner");

            // Retrive heaviest file from Google Drive
            if (backupFolderId != null) {
                List<File> files = driveService.files().list()
                        .setQ("'" + backupFolderId + "' in parents and trashed = false")
                        .setOrderBy("quotaBytesUsed desc")
                        .setPageSize(1) // Only keep the biggest one
                        .setFields("files(id, name, size)")
                        .execute()
                        .getFiles();

                // If a file is found, retrieve its name and download its contents
                if (files != null && !files.isEmpty()) {
                    String heaviestFileName = files.get(0).getName();
                    heaviestBackup = downloadFile(heaviestFileName);
                }
            }
        }
        catch (Exception e) {
            Log.e("DriveManager", "Error downloading file on Google Drive", e);
            activity.runOnUiThread(() -> Toast.makeText(activity, "Error downloading file on Google Drive", Toast.LENGTH_SHORT).show());
            return null;
        }

        return heaviestBackup;
    }
}
