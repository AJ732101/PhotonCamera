package com.particlesdevs.photoncamera.gallery.files;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.IntentSender;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.particlesdevs.photoncamera.api.CameraMode;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.particlesdevs.photoncamera.gallery.interfaces.ImagesDeletedCallback;
import com.particlesdevs.photoncamera.settings.PreferenceKeys;

import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by Vibhor Srivastava on October 13, 2021
 */
public class GalleryFileOperations {
    public static final int REQUEST_PERM_DELETE = 1010;
    private static final String[] INCLUDED_IMAGE_FOLDERS = new String[]{"%DCIM/PhotonVidCam/%", "%DCIM/PhotonVidCam/Raw/%", "%DCIM/PhotonVidCam/AVIF/%",
            "%DCIM/PhotonVidCam/APV/%", "%DCIM/PhotonVidCam/M4A/%", "%DCIM/PhotonVidCam/HEIC_10_Bit/%", "%DCIM/Camera/%"};
    private static final ArrayList<String> SELECTED_FOLDERS_IDS = new ArrayList<>();
    private static final ArrayList<ImagesFolder> ALL_FOLDERS = new ArrayList<>();
    private static final ArrayList<ImagesFolder> SELECTED_FOLDERS = new ArrayList<>();

    public static ArrayList<ImagesFolder> getSelectedFolders() {
        return SELECTED_FOLDERS;
    }

    public static ArrayList<ImagesFolder> _fetchSelectedFolders(ContentResolver contentResolver) {
        FindAllFoldersWithImages(contentResolver);
        SELECTED_FOLDERS_IDS.clear();
        SELECTED_FOLDERS.clear();
        try {
            SELECTED_FOLDERS_IDS.addAll(PreferenceKeys.getStringSet(PreferenceKeys.Key.FOLDERS_LIST));
        } catch (Exception e) {
            Log.d("GalleryFileOperations", "Warning: failed fetching selected folders from shared preferences " + Log.getStackTraceString(e));
        }
        if (SELECTED_FOLDERS_IDS.isEmpty()) {
            SELECTED_FOLDERS_IDS.add("Camera"); //in case the user has not selected any folder
            SELECTED_FOLDERS_IDS.add("Raw");
        }
        SELECTED_FOLDERS_IDS.forEach(s -> ALL_FOLDERS.forEach(imagesFolder -> {
            if (String.valueOf(imagesFolder.folderId).equals(s) || imagesFolder.folderName.equals(s)) {
                SELECTED_FOLDERS.add(imagesFolder);
            }
        }));
        SELECTED_FOLDERS.sort(Comparator.comparing(o -> o.folderName));
        return SELECTED_FOLDERS;
    }

    public static List<ImageFile> extractAllSelectedImages() {
        ArrayList<ImageFile> imageFiles = new ArrayList<>();
        SELECTED_FOLDERS.forEach(imagesFolder -> imageFiles.addAll(imagesFolder.getAllImageFiles()));
        imageFiles.sort(Comparator.comparingLong(value -> -value.getLastModified()));
        return imageFiles;
    }

    @Nullable
    public static ImageFile fetchLatestImage(ContentResolver contentResolver) {
        ImageFile imageFile = null;

        // --- Common variables for both paths ---
        String[] projection = {
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.SIZE
        };
        String sortOrder = MediaStore.MediaColumns.DATE_ADDED + " DESC";
        String[] selectionArgs = INCLUDED_IMAGE_FOLDERS;

        // --- Path-specific variables ---
        Uri queryUri;
        Uri baseContentUri;
        String selection;

        if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            // -- Video specific assignments --
            queryUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            baseContentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

            String selectionColumn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                    MediaStore.Video.Media.RELATIVE_PATH : MediaStore.Video.Media.DATA;

            StringBuilder selectionBuilder = new StringBuilder();
            for (int i = 0; i < INCLUDED_IMAGE_FOLDERS.length; i++) {
                selectionBuilder.append(selectionColumn).append(" LIKE ?");
                if (i < INCLUDED_IMAGE_FOLDERS.length - 1) {
                    selectionBuilder.append(" OR ");
                }
            }
            selection = selectionBuilder.toString();

        } else {
            // -- Photo specific assignments --
            queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            baseContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

            String selectionColumn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                    MediaStore.Images.Media.RELATIVE_PATH : MediaStore.Images.Media.DATA;

            StringBuilder selectionBuilder = new StringBuilder();
            for (int i = 0; i < INCLUDED_IMAGE_FOLDERS.length; i++) {
                selectionBuilder.append(selectionColumn).append(" LIKE ?");
                if (i < INCLUDED_IMAGE_FOLDERS.length - 1) {
                    selectionBuilder.append(" OR ");
                }
            }
            selection = selectionBuilder.toString();
        }

        // --- Common query and processing logic ---
        try (Cursor cursor = contentResolver.query(
                queryUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
        )) {

            if (cursor != null && cursor.moveToFirst()) {
                // Get column indices once
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
                int dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED);
                int displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

                // Extract data for the first (latest) entry
                long id = cursor.getLong(idColumn);
                Uri contentUri = ContentUris.withAppendedId(baseContentUri, id);
                String displayName = cursor.getString(displayNameColumn);
                long dateModified = TimeUnit.SECONDS.toMillis(cursor.getLong(dateModifiedColumn));
                long size = cursor.getLong(sizeColumn);
                String absolutePath = cursor.getString(dataColumn);

                // Create the result object
                imageFile = new ImageFile(id, contentUri, displayName, dateModified, size, absolutePath);
            }
        } catch (Exception e) {
            // Log any errors during the query
            Log.e("GalleryFileOperations", "Error fetching latest media file.", e);
        }

        return imageFile;
    }

    public static Uri createNewImageFile(ContentResolver contentResolver, String relativePath, String newImageName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, newImageName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, URLConnection.guessContentTypeFromName(newImageName));
        String column = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? MediaStore.MediaColumns.RELATIVE_PATH : MediaStore.MediaColumns.DATA;
        values.put(column, relativePath);
        return contentResolver.insert(MediaStore.Files.getContentUri("external"), values);
    }

    public static void deleteImageFiles(Activity activity, List<ImageFile> toDelete, ImagesDeletedCallback deletedCallback) {
        List<Uri> toDeleteUriList = toDelete.stream().map(ImageFile::getFileUri).collect(Collectors.toList());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            PendingIntent pi = MediaStore.createDeleteRequest(activity.getContentResolver(), toDeleteUriList);
            try {
                ActivityCompat.startIntentSenderForResult(activity, pi.getIntentSender(), REQUEST_PERM_DELETE, null, 0, 0, 0, null);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
                deletedCallback.deleted(false);
            }
        } else {
            ContentResolver contentResolver = activity.getContentResolver();
            for (ImageFile file : toDelete) {
                try {
                    contentResolver.delete(file.getFileUri(), MediaStore.Images.Media._ID + "= ?", new String[]{String.valueOf(file.getId())});
                } catch (SecurityException e) {
                    e.printStackTrace();
                    deletedCallback.deleted(false);
                    return;
                }
            }
            deletedCallback.deleted(true);
        }
    }

    public static class ImagesFolder {
        private String folderName;
        private ArrayList<ImageFile> imageFiles;
        private long folderId;
        private ImageFile topImage;


        public ImageFile getTopImage() {
            return topImage;
        }

        public long getFolderId() {
            return folderId;
        }
        public void setFolderId(long folderId) {
            this.folderId = folderId;
        }
        public String getFolderName() {
            return folderName;
        }
        public ArrayList<ImageFile> getAllImageFiles() {
            return imageFiles;
        }
        public void setFolderName(String name) {
            this.folderName = name;
        }
        public void setAllImageFiles(ArrayList<ImageFile> imageFiles) {
            this.imageFiles = imageFiles;
        }
    }

    public static ArrayList<ImagesFolder> FindAllFoldersWithImages(@NonNull ContentResolver contentResolver) {

        // Clear the global list before fetching
        ALL_FOLDERS.clear();

        // --- Part 1: Fetch all folders containing IMAGES ---
        Uri imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] imageProjection = {
                MediaStore.MediaColumns.DATA,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE
        };
        // Query images, ordered by date to have the newest ones first
        try (Cursor imageCursor = contentResolver.query(imageUri, imageProjection, null, null, MediaStore.Images.Media.DATE_TAKEN + " DESC")) {
            if (imageCursor != null) {
                // Get column indices once
                int image_column_index_data = imageCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                int image_column_bucket_name = imageCursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                int image_column_bucket_id = imageCursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID);
                int image_column_id = imageCursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int image_column_date_modified = imageCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);
                int image_column_display_name = imageCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                int image_column_size = imageCursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);

                while (imageCursor.moveToNext()) {
                    String bucketName = imageCursor.getString(image_column_bucket_name);
                    if (bucketName == null) continue;

                    // Find existing folder or create a new one
                    ImagesFolder folder = ALL_FOLDERS.stream()
                            .filter(f -> f.getFolderName().equals(bucketName))
                            .findFirst()
                            .orElse(null);

                    if (folder == null) {
                        folder = new ImagesFolder();
                        folder.setFolderName(bucketName);
                        folder.setFolderId(imageCursor.getLong(image_column_bucket_id));
                        folder.setAllImageFiles(new ArrayList<>());
                        ALL_FOLDERS.add(folder);
                    }

                    // Extract file data and add it to the folder
                    long id = imageCursor.getLong(image_column_id);
                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                    String displayName = imageCursor.getString(image_column_display_name);
                    long dateModified = TimeUnit.SECONDS.toMillis(imageCursor.getLong(image_column_date_modified));
                    long size = imageCursor.getLong(image_column_size);
                    String absolutePathOfImage = imageCursor.getString(image_column_index_data);

                    folder.getAllImageFiles().add(new ImageFile(id, contentUri, displayName, dateModified, size, absolutePathOfImage));
                }
            }
        }

        // --- Part 2: Fetch all folders containing VIDEOS and merge them ---
        Uri videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] videoProjection = {
                MediaStore.MediaColumns.DATA,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.BUCKET_ID,
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.SIZE
        };
        // Query videos, ordered by date
        try (Cursor videoCursor = contentResolver.query(videoUri, videoProjection, null, null, MediaStore.Video.Media.DATE_TAKEN + " DESC")) {
            if (videoCursor != null) {
                // Get column indices once
                int video_column_index_data = videoCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                int video_column_bucket_name = videoCursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
                int video_column_bucket_id = videoCursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID);
                int video_column_id = videoCursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int video_column_date_modified = videoCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED);
                int video_column_display_name = videoCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                int video_column_size = videoCursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);

                while (videoCursor.moveToNext()) {
                    String bucketName = videoCursor.getString(video_column_bucket_name);
                    if (bucketName == null) continue;

                    // Find existing folder (might have been created by the image scan) or create a new one
                    ImagesFolder folder = ALL_FOLDERS.stream()
                            .filter(f -> f.getFolderName().equals(bucketName))
                            .findFirst()
                            .orElse(null);

                    if (folder == null) {
                        folder = new ImagesFolder();
                        folder.setFolderName(bucketName);
                        folder.setFolderId(videoCursor.getLong(video_column_bucket_id));
                        folder.setAllImageFiles(new ArrayList<>());
                        ALL_FOLDERS.add(folder);
                    }

                    // Extract file data and add it to the folder
                    long id = videoCursor.getLong(video_column_id);
                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                    String displayName = videoCursor.getString(video_column_display_name);
                    long dateModified = TimeUnit.SECONDS.toMillis(videoCursor.getLong(video_column_date_modified));
                    long size = videoCursor.getLong(video_column_size);
                    String absolutePathOfImage = videoCursor.getString(video_column_index_data);

                    folder.getAllImageFiles().add(new ImageFile(id, contentUri, displayName, dateModified, size, absolutePathOfImage));
                }
            }
        }

        // --- Part 3: Set top image for each folder ---
        // After all files are added, find the newest file in each folder to use as a thumbnail
        for(ImagesFolder folder : ALL_FOLDERS){
            if(folder.getAllImageFiles() != null && !folder.getAllImageFiles().isEmpty()){
                folder.getAllImageFiles().sort(Comparator.comparingLong(ImageFile::getLastModified).reversed());
                folder.topImage = folder.getAllImageFiles().get(0);
            }
        }

        return ALL_FOLDERS;
    }
}
