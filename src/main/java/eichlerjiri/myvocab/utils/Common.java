package eichlerjiri.myvocab.utils;

import android.util.Log;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Common {

    public static byte[] cachedDownload(String baseUrl, File cacheDir, String filename, boolean forceReload, ArrayList<String> errors) throws InterruptedIOException {
        byte[] data = null;

        File cachedFile = new File(cacheDir, filename);
        if (!forceReload) {
            if (cachedFile.exists()) {
                data = readFile(cachedFile);
            }
        }

        if (data == null) {
            data = download(baseUrl + filename, errors);
            if (data != null) {
                writeFile(cachedFile, data);
            }
        }

        return data;
    }

    public static byte[] download(String url, ArrayList<String> errors) throws InterruptedIOException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            InputStream is = conn.getInputStream();
            try {
                return readAll(is, errors);
            } finally {
                closeStream(is);
            }
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            Log.e("Common", "Cannot download file: " + url, e);
            if (conn != null) {
                addError(errors, getHttpErrorMessage(e, conn));

                InputStream es = conn.getErrorStream();
                if (es != null) {
                    readAll(es, null);
                    closeStream(es);
                }
            }
            return null;
        }
    }

    public static String getHttpErrorMessage(IOException e, HttpURLConnection conn) {
        try {
            int code = conn.getResponseCode();
            String message = conn.getResponseMessage();
            if (code != -1 && message != null) {
                return code + ": " + message;
            }
        } catch (IOException e2) {
            // no HTTP error message available
        }
        return e.getMessage();
    }

    public static byte[] readFile(File file) throws InterruptedIOException {
        try {
            FileInputStream fis = new FileInputStream(file);
            try {
                return readAll(fis, null);
            } finally {
                closeStream(fis);
            }
        } catch (FileNotFoundException e) {
            Log.e("Common", "Cannot read file: " + file, e);
            return null;
        }
    }

    public static byte[] readFilePartial(File file, int startPos, int length) throws InterruptedIOException {
        try {
            RandomAccessFile is = new RandomAccessFile(file, "r");
            try {
                is.seek(startPos);
                byte[] data = new byte[length];
                is.readFully(data);
                return data;
            } finally {
                closeStream(is);
            }
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            Log.e("Common", "Cannot read file: " + file, e);
            return null;
        }
    }

    public static boolean writeFile(File file, byte[] data) throws InterruptedIOException {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {
                fos.write(data);
            } finally {
                closeStream(fos);
            }
            return true;
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            Log.e("Common", "Cannot write file: " + file, e);
            return false;
        }
    }

    public static byte[] readAll(InputStream is, ArrayList<String> errors) throws InterruptedIOException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(is.available());
            byte[] buffer = new byte[4096];
            int num;
            while ((num = is.read(buffer)) != -1) {
                baos.write(buffer, 0, num);
            }
            return baos.toByteArray();
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            Log.e("Common", "Cannot read stream", e);
            addError(errors, e.getMessage());
            return null;
        }
    }

    public static void closeStream(Closeable is) throws InterruptedIOException {
        try {
            is.close();
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            Log.e("Common", "Cannot close stream", e);
        }
    }

    public static File createTempFile(String prefix, String suffix) {
        try {
            return File.createTempFile(prefix, suffix);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public static void unlinkFile(File file) {
        if (!file.delete()) {
            Log.e("Common", "Cannot delete file: " + file.getPath());
        }
    }

    public static void addError(ArrayList<String> errors, String error) {
        if (errors != null) {
            errors.add(error);
        }
    }

    public static BufferedReader readByLine(byte[] data) {
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8));
    }

    public static String readLine(BufferedReader r) throws InterruptedIOException {
        try {
            return r.readLine();
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public static ArrayList<String> splitLine(String line, char separator) {
        StringBuilder sb = new StringBuilder();
        ArrayList<String> ret = new ArrayList<>();

        boolean escaped = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '\\') {
                if (escaped) {
                    sb.append('\\');
                    escaped = false;
                } else {
                    escaped = true;
                }
            } else if (c == separator) {
                if (escaped) {
                    sb.append(c);
                    escaped = false;
                } else {
                    ret.add(sb.toString().trim());
                    sb.setLength(0);
                }
            } else {
                if (escaped) {
                    sb.append('\\');
                    escaped = false;
                }
                sb.append(c);
            }
        }
        if (escaped) {
            sb.append('\\');
        }
        ret.add(sb.toString().trim());

        return ret;
    }

    public static int parseInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
