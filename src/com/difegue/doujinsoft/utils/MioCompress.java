package com.difegue.doujinsoft.utils;

import java.io.*;
import java.nio.file.Files;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MioCompress {

    /**
     * Compress and uncompress .mio files with ZipEntry.
     */

    public static void compressMio(File orig, File dest, String desiredName) throws IOException {

        String zipFileName = dest.getAbsolutePath();
        // Creates a file output stream to write to the file(zipFileName ) with the specified name.
        // Creates a new ZIP output stream
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName));
        zos.putNextEntry(new ZipEntry(desiredName));

        byte[] bytes = Files.readAllBytes(orig.toPath()); // read the byte from the orig
        zos.write(bytes, 0, bytes.length); // write to
        zos.closeEntry();
        zos.close();
    }

    public static File uncompressMio(File compressedMio) throws IOException {

        Logger logger = Logger.getLogger("Mio Unzip");
        // Files are extracted to tempfolder/their hash to avoid conflicts
        var hash = compressedMio.getName().replaceFirst("[.][^.]+$", "");
        System.out.println("----");
        System.out.println(hash);
        String tDir = System.getProperty("java.io.tmpdir") + File.separator + hash;


        // Create folder if it doesn't exist
        File tempDir = new File(tDir);
        System.out.println(compressedMio.toPath().toString());
        System.out.println(tempDir.toString());
        System.out.println(tempDir.exists());
        if (!tempDir.exists()) {
            System.out.println("here1");
            tempDir.mkdir();
        }

        // Uncompress given file
        ZipInputStream zis = new ZipInputStream(new FileInputStream(compressedMio.getAbsolutePath()));
        ZipEntry entry = zis.getNextEntry();
        byte[] buffer = new byte[1024];

        File uncompressedMio = null;
        while (entry != null) {

            String fileName = entry.getName();
            System.out.println(fileName);
            uncompressedMio = new File(tDir, fileName);

            // "cache" implementation (sort of)
            if (uncompressedMio.exists()) {
                System.out.println("here");
                // You can get a race condition here if someone starts a download and a
                // concurrent user starts one right afterwards.
                // User n°2 might get an incomplete .mio file.
                // Considering how small those files are, the timing is fairly tight...
                break;
            }
            FileOutputStream fos = new FileOutputStream(uncompressedMio);
            logger.log(Level.FINE, "Uncompressing .mio to " + uncompressedMio.getAbsolutePath());

            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }

            fos.close();
            entry = zis.getNextEntry();
        }
        System.out.println("----");

        zis.closeEntry();
        zis.close();

        // Return away
        return uncompressedMio;
    }

}
