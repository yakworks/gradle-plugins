package yakworks.gradle.util;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * IO utils. A bit of reinventing the wheel but we don't want extra dependencies at this stage and we want to be java.
 */
public class IOUtil {

    /**
     * Reads string from the file
     */
    public static String readFully(File input) {
        try {
            return readNow(new FileInputStream(input));
        } catch (Exception e) {
            throw new RuntimeException("Problems reading file: " + input, e);
        }
    }

    /**
     * Reads string from the file or returns empty text if file doesn't exist or can't open
     */
    public static String readFullyOrDefault(File input, String defaultValue) {
        try {
            return readNow(new FileInputStream(input));
        } catch (Exception e) {
            //TODO this potentially swallows exceptions
            //if the file does not exist, we should just use default value
            //if the file cannot be read, we should write a message to the log that we cannot use the file
            // + stack trace in debug level
            return defaultValue;
        }
    }

    /**
     * Reads string from the stream and closes it
     */
    public static String readFully(InputStream stream) {
        try {
            return readNow(stream);
        } catch (Exception e) {
            throw new RuntimeException("Problems reading stream", e);
        }
    }

    /**
     * Closes the target. Does nothing when target is null. Is not silent, throws exception on IOException.
     *
     * @param closeable the target, may be null
     */
    public static void close(Closeable closeable) {
        close(closeable, false);
    }

    private static void close(Closeable closeable, boolean quietly) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                if (!quietly) {
                    throw new RuntimeException("Problems closing closeable", e);
                }
            }
        }
    }

    /**
     * Closes the target. Does nothing when target is null. Is silent.
     *
     * @param closeable the target, may be null
     */
    public static void closeQuietly(Closeable closeable) {
        close(closeable, true);
    }

    public static void createParentDirectory(File file) {
        createDirectory(file.getParentFile());
    }

    public static void createDirectory(File file) {
        if (!file.exists()) {
            createDirectory(file.getParentFile());
            file.mkdir();
        }
    }

    /**
     * Downloads resource and saves it to a given file
     * @param url location of resource to download
     * @param file destination file (not a directory!) where downloaded content will be stored
     *             (file or its parent directories don't need to exist)
     */
    public static void downloadToFile(String url, File file) {
        InputStream input = null;
        try {
            input = new BufferedInputStream(new URL(url).openStream());

            IOUtil.createParentDirectory(file);

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                byte[] buf = new byte[1024];
                int n;
                while ((n = input.read(buf)) != -1) {
                    fos.write(buf, 0, n);
                }
            } finally {
                closeQuietly(fos);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(input);
        }
    }

    private static String readNow(InputStream is) {
        try (Scanner s = new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }

    public static void writeFile(File target, String content) {
        PrintWriter p = null;
        try {
            target.getParentFile().mkdirs();
            p = new PrintWriter(new OutputStreamWriter(new FileOutputStream(target), StandardCharsets.UTF_8));
            p.write(content);
        } catch (Exception e) {
            throw new RuntimeException("Problems writing text to file: " + target, e);
        } finally {
            close(p);
        }
    }
}
