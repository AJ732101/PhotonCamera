package com.particlesdevs.photoncamera.processing;

import android.os.Bundle;

import com.particlesdevs.photoncamera.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class XmpMetaDataWriter {

    private static final String TAG = "AvifEncoder";

    public static void writeXmpMetadata(File outputFile, Bundle metadata) throws IOException {
        if (metadata != null) {
            // --- NEW APPROACH: SAVE METADATA TO A SIDECAR .XMP FILE ---
            // Get the file path without extension
            String baseFilePath = outputFile.getAbsolutePath();
            int lastDot = baseFilePath.lastIndexOf('.');
            if (lastDot > 0) {
                baseFilePath = baseFilePath.substring(0, lastDot) + ".xmp";
            }
            File xmpFile = new File(baseFilePath);

            try (FileWriter writer = new FileWriter(xmpFile)) {
                // Create a simple XMP structure as a string
                StringBuilder xmpBuilder = new StringBuilder();
                xmpBuilder.append("<x:xmpmeta xmlns:x='adobe:ns:meta/' x:xmptk='PhotonCamera'>\n");
                xmpBuilder.append("  <rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>\n");
                xmpBuilder.append("    <rdf:Description rdf:about='' xmlns:exif='http://ns.adobe.com/exif/1.0/'>\n");

                if (metadata.containsKey("iso")) {
                    xmpBuilder.append("      <exif:ISOSpeedRatings><rdf:Seq><rdf:li>")
                            .append(metadata.getInt("iso"))
                            .append("</rdf:li></rdf:Seq></exif:ISOSpeedRatings>\n");
                }
                if (metadata.containsKey("exposureTime")) {
                    long exposureNanos = metadata.getLong("exposureTime");
                    if (exposureNanos > 0) {
                        double exposureSeconds = exposureNanos / 1_000_000_000.0;
                        // XMP often uses fractions for exposure time
                        xmpBuilder.append("      <exif:ExposureTime>")
                                .append(String.format(java.util.Locale.US, "1/%.0f", 1.0 / exposureSeconds))
                                .append("</exif:ExposureTime>\n");
                    }
                }
                if (metadata.containsKey("focalLength")) {
                    xmpBuilder.append("      <exif:FocalLength>")
                            .append(String.format(java.util.Locale.US, "%.1f", metadata.getFloat("focalLength")))
                            .append("</exif:FocalLength>\n");
                }

                xmpBuilder.append("    </rdf:Description>\n");
                xmpBuilder.append("  </rdf:RDF>\n");
                xmpBuilder.append("</x:xmpmeta>");

                writer.write(xmpBuilder.toString());
                Log.d(TAG, "Successfully wrote metadata to XMP sidecar file: " + xmpFile.getName());
                writer.close();
            } catch (IOException e) {
                Log.w(TAG, "Could not write XMP sidecar file.", e);
            }
        }
    }
}
