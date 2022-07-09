package eichlerjiri.myvocab.utils;

import android.util.Log;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Common {

    public static String exceptionToString(Throwable t) {
        String ret = t.getMessage();
        if (ret == null) {
            ret = t.getClass().getCanonicalName();
        }
        return ret;
    }

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
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setInstanceFollowRedirects(false);

            byte[] response = readHTTPResponse(conn);
            int code = conn.getResponseCode();
            if (code != 200) {
                errors.add(code + ": " + conn.getResponseMessage());
                Log.e("Common", "Cannot download file " + url + ": " + code + ": " + conn.getResponseMessage());
                return null;
            }
            return response;
        } catch (InterruptedIOException e) {
            throw e;
        } catch (UnknownHostException e) {
            errors.add("Unknown hostname");
            Log.e("Common", "Cannot download file " + url + ": unknown hostname", e);
            return null;
        } catch (IOException e) {
            errors.add(exceptionToString(e));
            Log.e("Common", "Cannot download file " + url, e);
            return null;
        }
    }

    public static byte[] readAllAndClose(InputStream is) throws IOException {
        try {
            return readAll(is);
        } finally {
            is.close();
        }
    }

    public static byte[] readHTTPResponse(HttpURLConnection conn) throws IOException {
        try {
            return readAllAndClose(conn.getInputStream());
        } catch (IOException e) {
            InputStream errorStream = conn.getErrorStream();
            if (errorStream != null) {
                return readAllAndClose(errorStream);
            } else if (conn.getResponseCode() == -1) {
                throw e;
            } else {
                return new byte[]{};
            }
        }
    }

    public static byte[] readFile(File file) throws InterruptedIOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return readAll(fis);
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            Log.e("Common", "Cannot read file: " + file, e);
            return null;
        }
    }

    public static byte[] readFilePartial(File file, int startPos, int length) throws InterruptedIOException {
        try (RandomAccessFile is = new RandomAccessFile(file, "r")) {
            is.seek(startPos);
            byte[] data = new byte[length];
            is.readFully(data);
            return data;
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            Log.e("Common", "Cannot read file: " + file, e);
            return null;
        }
    }

    public static boolean writeFile(File file, byte[] data) throws InterruptedIOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            return true;
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            Log.e("Common", "Cannot write file: " + file, e);
            return false;
        }
    }

    public static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(is.available());
        byte[] buffer = new byte[4096];
        int num;
        while ((num = is.read(buffer)) != -1) {
            baos.write(buffer, 0, num);
        }
        return baos.toByteArray();
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
