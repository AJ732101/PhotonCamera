package com.particlesdevs.photoncamera.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * Utility class for MediaStore operations.
 */
public class MediaStoreUtils {

    /**
     * Finds the latest media file (image or video) matching a specific set of MIME types.
     * This version queries the combined 'files' table for maximum compatibility, especially
     * for non-standard formats like DNG and HEIC.
     *
     * @param contentResolver The ContentResolver to use for the query.
     * @return The Uri of the latest media file, or null if none is found.
     */
    public static Uri getLatestImageUri(ContentResolver contentResolver) {
        // Query the 'files' table which contains all media types.
        Uri queryUri = MediaStore.Files.getContentUri("external");

        String[] projection = {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATE_ADDED
        };

        // A comprehensive list of MIME types for all supported formats.
        String selection = MediaStore.Files.FileColumns.MIME_TYPE + " IN (?, ?, ?, ?, ?, ?)";
        String[] selectionArgs = {
                "image/jpeg",        // .jpg
                "image/heic",        // .heic
                "image/heif",        // .heif
                "image/x-adobe-dng", // .dng
                "video/mp4",         // .mp4
                "video/webm"         // .webm
        };

        // Sort by date added in descending order and limit to the single latest result.
        String sortOrder = MediaStore.Files.FileColumns.DATE_ADDED + " DESC LIMIT 1";

        try (Cursor cursor = contentResolver.query(queryUri, projection, selection, selectionArgs, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID));
                // Construct the URI using the base URI for the files table.
                return ContentUris.withAppendedId(queryUri, id);
            }
        } catch (Exception e) {
            // This can happen on some devices with strict permissions or custom MediaStore implementations.
            // Returning null is a safe fallback.
        }

        // Return null if no media file was found.
        return null;
    }
}
