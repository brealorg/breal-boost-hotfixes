package app.morphe.extension.imgur;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Locale;

public final class ShareMediaFile {
    private static final String AUTHORITY = "com.imgur.mobile.reactionsprovider";
    private static final String CACHE_DIR_NAME = "morphe-share";
    private static final int BUFFER_SIZE = 64 * 1024;

    private ShareMediaFile() {
    }

    public static void share(final Activity activity, final String urlString) {
        if (activity == null) {
            return;
        }

        if (urlString == null || urlString.trim().isEmpty()) {
            toast(activity, "No media URL found");
            activity.finish();
            return;
        }

        toast(activity, "Preparing media share...");

        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File cacheDir = new File(activity.getCacheDir(), CACHE_DIR_NAME);
                    if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                        throw new IllegalStateException("Could not create cache directory");
                    }

                    clearDirectory(cacheDir);

                    String fileName = deriveFileName(urlString);
                    File target = new File(cacheDir, fileName);
                    download(urlString, target);

                    shareDownloadedFile(activity, target);
                } catch (final Throwable throwable) {
                    runOnUiThread(activity, new Runnable() {
                        @Override
                        public void run() {
                            toast(activity, "Share failed");
                            activity.finish();
                        }
                    });
                }
            }
        }, "morphe-imgur-share");

        worker.start();
    }

    private static void shareDownloadedFile(final Activity activity, final File file) {
        runOnUiThread(activity, new Runnable() {
            @Override
            public void run() {
                try {
                    Uri uri = getUriForFile(activity, file);
                    String mimeType = guessMimeType(file.getName());

                    Intent send = new Intent(Intent.ACTION_SEND);
                    send.setType(mimeType);
                    send.putExtra(Intent.EXTRA_STREAM, uri);
                    send.putExtra(Intent.EXTRA_TITLE, file.getName());
                    send.setClipData(ClipData.newUri(activity.getContentResolver(), file.getName(), uri));
                    send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    Intent chooser = Intent.createChooser(send, "Share media file");
                    chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    activity.startActivity(chooser);
                } catch (Throwable throwable) {
                    toast(activity, "Share failed");
                } finally {
                    activity.finish();
                }
            }
        });
    }

    private static void download(String urlString, File target) throws Exception {
        HttpURLConnection connection = null;
        InputStream input = null;
        FileOutputStream output = null;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(45000);
            connection.setRequestProperty("User-Agent", "Imgur/7.33.0");

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IllegalStateException("Unexpected HTTP response: " + responseCode);
            }

            input = connection.getInputStream();
            output = new FileOutputStream(target);

            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }

            output.flush();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (Throwable ignored) {
                }
            }

            if (input != null) {
                try {
                    input.close();
                } catch (Throwable ignored) {
                }
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static Uri getUriForFile(Context context, File file) throws Exception {
        Class<?> fileProviderClass = Class.forName("androidx.core.content.FileProvider");
        Method method = fileProviderClass.getMethod(
                "getUriForFile",
                Context.class,
                String.class,
                File.class
        );

        return (Uri) method.invoke(null, context, AUTHORITY, file);
    }

    private static String deriveFileName(String urlString) {
        try {
            URL url = new URL(urlString);
            String path = url.getPath();
            String name = path.substring(path.lastIndexOf('/') + 1);

            if (name.length() == 0) {
                name = "imgur-media";
            }

            name = URLDecoder.decode(name, "UTF-8");
            name = sanitizeFileName(name);

            if (!name.contains(".")) {
                name = name + ".bin";
            }

            return name;
        } catch (Throwable ignored) {
            return "imgur-media.bin";
        }
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String guessMimeType(String fileName) {
        String extension = "";

        int dot = fileName.lastIndexOf('.');
        if (dot >= 0 && dot + 1 < fileName.length()) {
            extension = fileName.substring(dot + 1).toLowerCase(Locale.US);
        }

        String mimeType = null;
        if (extension.length() > 0) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        if (mimeType != null) {
            return mimeType;
        }

        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            return "image/jpeg";
        }

        if ("png".equals(extension)) {
            return "image/png";
        }

        if ("gif".equals(extension)) {
            return "image/gif";
        }

        if ("mp4".equals(extension)) {
            return "video/mp4";
        }

        if ("webm".equals(extension)) {
            return "video/webm";
        }

        return "*/*";
    }

    private static void clearDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            deleteRecursively(file);
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }

        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    private static void toast(final Activity activity, final String message) {
        runOnUiThread(activity, new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static void runOnUiThread(Activity activity, Runnable runnable) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        activity.runOnUiThread(runnable);
    }
}
