package jerry.filebrowser.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JerryFileProvider extends ContentProvider {
    public static final String COLUMNS_DISPLAY_NAME = OpenableColumns.DISPLAY_NAME;
    public static final String COLUMNS_SIZE = OpenableColumns.SIZE;
    //public static final String COLUMNS_DATA = MediaStore.MediaColumns.DATA;
    public static final String COLUMNS_DATA = "_data";

    private static final String[] COLUMNS = {COLUMNS_DISPLAY_NAME, COLUMNS_SIZE, COLUMNS_DATA};
    private static final String
            META_DATA_FILE_PROVIDER_PATHS = "android.support.FILE_PROVIDER_PATHS";

    private static final String TAG_ROOT_PATH = "root-path";
//    private static final String TAG_FILES_PATH = "files-path";
//    private static final String TAG_CACHE_PATH = "cache-path";
//    private static final String TAG_EXTERNAL = "external-path";
//    private static final String TAG_EXTERNAL_FILES = "external-files-path";
//    private static final String TAG_EXTERNAL_CACHE = "external-cache-path";
//    private static final String TAG_EXTERNAL_MEDIA = "external-media-path";
//
//    private static final String ATTR_NAME = "name";
//    private static final String ATTR_PATH = "path";

//    public static final String COLUMNS_TITLE = MediaStore.MediaColumns.TITLE;
//
//    private static final File DEVICE_ROOT = new File("/");

    @GuardedBy("sCache")
    private final static HashMap<String, PathStrategy> sCache = new HashMap<>();

    private PathStrategy mStrategy;

    @Override
    public boolean onCreate() {
        return true;
    }

    /**
     * After the JerryFileProvider is instantiated, this method is called to provide the system with
     * information about the provider.
     *
     * @param context A {@link Context} for the current component.
     * @param info    A {@link ProviderInfo} for the new provider.
     */
    @Override
    public void attachInfo(@NonNull Context context, @NonNull ProviderInfo info) {
        super.attachInfo(context, info);

        // Sanity check our security
        if (info.exported) {
            throw new SecurityException("Provider must not be exported");
        }
        if (!info.grantUriPermissions) {
            throw new SecurityException("Provider must grant uri permissions");
        }

        mStrategy = getPathStrategy(context, info.authority);
    }

    /**
     * Return a content URI for a given {@link File}. Specific temporary
     * permissions for the content URI can be set with
     * {@link Context#grantUriPermission(String, Uri, int)}, or added
     * to an {@link Intent} by calling {@link Intent#setData(Uri) setData()} and then
     * {@link Intent#setFlags(int) setFlags()}; in both cases, the applicable flags are
     * {@link Intent#FLAG_GRANT_READ_URI_PERMISSION} and
     * {@link Intent#FLAG_GRANT_WRITE_URI_PERMISSION}. A JerryFileProvider can only return a
     * <code>content</code> {@link Uri} for file paths defined in their <code>&lt;paths&gt;</code>
     * meta-data element. See the Class Overview for more information.
     *
     * @param context   A {@link Context} for the current component.
     * @param authority The authority of a {@link JerryFileProvider} defined in a
     *                  {@code <provider>} element in your app's manifest.
     * @param file      A {@link File} pointing to the filename for which you want a
     *                  <code>content</code> {@link Uri}.
     * @return A content URI for the file.
     * @throws IllegalArgumentException When the given {@link File} is outside
     *                                  the paths supported by the provider.
     */
    public static Uri getUriForFile(@NonNull Context context, @NonNull String authority,
                                    @NonNull File file) {
        final PathStrategy strategy = getPathStrategy(context, authority);
        return strategy.getUriForFile(file);
    }


    public static Uri getUriForFile(@NonNull Context context, @NonNull String authority,
                                    @NonNull String path) {
        final PathStrategy strategy = getPathStrategy(context, authority);
        return strategy.getUriForFile(new File(path));
    }

    /**
     * Use a content URI returned by
     * {@link #getUriForFile(Context, String, File) getUriForFile()} to get information about a file
     * managed by the JerryFileProvider.
     * JerryFileProvider reports the column names defined in {@link android.provider.OpenableColumns}:
     * <ul>
     * <li>{@link android.provider.OpenableColumns#DISPLAY_NAME}</li>
     * <li>{@link android.provider.OpenableColumns#SIZE}</li>
     * </ul>
     * For more information, see
     * {@link ContentProvider#query(Uri, String[], String, String[], String)
     * ContentProvider.query()}.
     *
     * @param uri           A content URI returned by {@link #getUriForFile}.
     * @param projection    The list of columns to put into the {@link Cursor}. If null all columns are
     *                      included.
     * @param selection     Selection criteria to apply. If null then all data that matches the content
     *                      URI is returned.
     * @param selectionArgs An array of {@link java.lang.String}, containing arguments to bind to
     *                      the <i>selection</i> parameter. The <i>query</i> method scans <i>selection</i> from left to
     *                      right and iterates through <i>selectionArgs</i>, replacing the current "?" character in
     *                      <i>selection</i> with the value at the current position in <i>selectionArgs</i>. The
     *                      values are bound to <i>selection</i> as {@link java.lang.String} values.
     * @param sortOrder     A {@link java.lang.String} containing the column name(s) on which to sort
     *                      the resulting {@link Cursor}.
     * @return A {@link Cursor} containing the results of the query.
     */
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {
        // ContentProvider has already checked granted permissions
        final File file = mStrategy.getFileForUri(uri);

        if (projection == null) {
            projection = COLUMNS;
        }
        String[] cols = new String[projection.length];
        Object[] values = new Object[projection.length];
        int i = 0;
        for (String col : projection) {
            if (COLUMNS_DISPLAY_NAME.equals(col)) {
                cols[i] = COLUMNS_DISPLAY_NAME;
                values[i++] = file.getName();
            } else if (COLUMNS_SIZE.equals(col)) {
                cols[i] = COLUMNS_SIZE;
                values[i++] = file.length();
            } else if (COLUMNS_DATA.equals(col)) {
                cols[i] = COLUMNS_DATA;
                String absPath;
                try {
                    absPath = file.getCanonicalPath();
                } catch (IOException e) {
                    e.printStackTrace();
                    absPath = file.getAbsolutePath();
                }
                values[i++] = absPath;
            }
        }
        DocumentsContract contract;

        cols = copyOf(cols, i);
        values = copyOf(values, i);

        final MatrixCursor cursor = new MatrixCursor(cols, 1);
        cursor.addRow(values);
        return cursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        final File file = mStrategy.getFileForUri(uri);
        final int lastDot = file.getName().lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = file.getName().substring(lastDot + 1);
            final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }
        return "application/octet-stream";
    }

    /**
     * By default, this method throws an {@link java.lang.UnsupportedOperationException}. You must
     * subclass JerryFileProvider if you want to provide different functionality.
     */
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("No external inserts");
    }

    /**
     * By default, this method throws an {@link java.lang.UnsupportedOperationException}. You must
     * subclass JerryFileProvider if you want to provide different functionality.
     */
    @Override
    public int update(@NonNull Uri uri, ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("No external updates");
    }

    /**
     * Deletes the file associated with the specified content URI, as
     * returned by {@link #getUriForFile(Context, String, File) getUriForFile()}. Notice that this
     * method does <b>not</b> throw an {@link java.io.IOException}; you must check its return value.
     *
     * @param uri           A content URI for a file, as returned by
     *                      {@link #getUriForFile(Context, String, File) getUriForFile()}.
     * @param selection     Ignored. Set to {@code null}.
     * @param selectionArgs Ignored. Set to {@code null}.
     * @return 1 if the delete succeeds; otherwise, 0.
     */
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        // ContentProvider has already checked granted permissions
        final File file = mStrategy.getFileForUri(uri);
        return file.delete() ? 1 : 0;
    }

    /**
     * By default, JerryFileProvider automatically returns the
     * {@link ParcelFileDescriptor} for a file associated with a <code>content://</code>
     * {@link Uri}. To get the {@link ParcelFileDescriptor}, call
     * {@link android.content.ContentResolver#openFileDescriptor(Uri, String)
     * ContentResolver.openFileDescriptor}.
     * <p>
     * To override this method, you must provide your own subclass of JerryFileProvider.
     *
     * @param uri  A content URI associated with a file, as returned by
     *             {@link #getUriForFile(Context, String, File) getUriForFile()}.
     * @param mode Access mode for the file. May be "r" for read-only access, "rw" for read and
     *             write access, or "rwt" for read and write access that truncates any existing file.
     * @return A new {@link ParcelFileDescriptor} with which you can access the file.
     */
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode)
            throws FileNotFoundException {
        // ContentProvider has already checked granted permissions
        final File file = mStrategy.getFileForUri(uri);
        final int fileMode = modeToMode(mode);
        return ParcelFileDescriptor.open(file, fileMode);
    }

    /**
     * Return {@link PathStrategy} for given authority, either by parsing or
     * returning from cache.
     */
    private static PathStrategy getPathStrategy(Context context, String authority) {
        PathStrategy strat;
        synchronized (sCache) {
            strat = sCache.get(authority);
            if (strat == null) {
                strat = parsePathStrategy(context, authority);
                sCache.put(authority, strat);
            }
        }
        return strat;
    }

    /**
     * Parse and return {@link PathStrategy} for given authority as defined in
     * {@link #META_DATA_FILE_PROVIDER_PATHS} {@code <meta-data>}.
     *
     * @see #getPathStrategy(Context, String)
     */
    private static PathStrategy parsePathStrategy(Context context, String authority) {
        final PathStrategy strat = new PathStrategy(authority);
//
//        final ProviderInfo info = context.getPackageManager()
//                .resolveContentProvider(authority, PackageManager.GET_META_DATA);
//        if (info == null) {
//            throw new IllegalArgumentException(
//                    "Couldn't find meta-data for provider with authority " + authority);
//        }
//
//        final XmlResourceParser in = info.loadXmlMetaData(
//                context.getPackageManager(), META_DATA_FILE_PROVIDER_PATHS);
//        if (in == null) {
//            throw new IllegalArgumentException(
//                    "Missing " + META_DATA_FILE_PROVIDER_PATHS + " meta-data");
//        }
//
//        int type;
//        while ((type = in.next()) != END_DOCUMENT) {
//            if (type == START_TAG) {
//                final String tag = in.getName();
//
//                final String name = in.getAttributeValue(null, ATTR_NAME);
//                String path = in.getAttributeValue(null, ATTR_PATH);
//
//                File target = null;
//                if (TAG_ROOT_PATH.equals(tag)) {
//                    target = DEVICE_ROOT;
//                } else if (TAG_FILES_PATH.equals(tag)) {
//                    target = context.getFilesDir();
//                } else if (TAG_CACHE_PATH.equals(tag)) {
//                    target = context.getCacheDir();
//                } else if (TAG_EXTERNAL.equals(tag)) {
//                    target = Environment.getExternalStorageDirectory();
//                } else if (TAG_EXTERNAL_FILES.equals(tag)) {
//                    File[] externalFilesDirs = ContextCompat.getExternalFilesDirs(context, null);
//                    if (externalFilesDirs.length > 0) {
//                        target = externalFilesDirs[0];
//                    }
//                } else if (TAG_EXTERNAL_CACHE.equals(tag)) {
//                    File[] externalCacheDirs = ContextCompat.getExternalCacheDirs(context);
//                    if (externalCacheDirs.length > 0) {
//                        target = externalCacheDirs[0];
//                    }
//                } else if (TAG_EXTERNAL_MEDIA.equals(tag)) {
//                    File[] externalMediaDirs = context.getExternalMediaDirs();
//                    if (externalMediaDirs.length > 0) {
//                        target = externalMediaDirs[0];
//                    }
//                }
//                if (target != null) {
//                    strat.addRoot(name, buildPath(target, path));
//                }
//            }
//        }
        strat.addRoot(TAG_ROOT_PATH, "/");
        return strat;
    }

    /**
     * Strategy that provides access to files living under a narrow whitelist of
     * filesystem roots. It will throw {@link SecurityException} if callers try
     * accessing files outside the configured roots.
     * <p>
     * For example, if configured with
     * {@code addRoot("myfiles", context.getFilesDir())}, then
     * {@code context.getFileStreamPath("foo.txt")} would map to
     * {@code content://myauthority/myfiles/foo.txt}.
     */
    static class PathStrategy {
        private final String mAuthority;
        private final HashMap<String, String> mRoots = new HashMap<>();

        PathStrategy(String authority) {
            mAuthority = authority;
        }

        /**
         * Add a mapping from a name to a filesystem root. The provider only offers
         * access to files that live under configured roots.
         */
//        void addRoot(String name, File root) {
//            if (TextUtils.isEmpty(name)) {
//                throw new IllegalArgumentException("Name must not be empty");
//            }
//
//            try {
//                // Resolve to canonical path to keep path checking fast
//                root = root.getCanonicalFile();
//            } catch (IOException e) {
//                throw new IllegalArgumentException("Failed to resolve canonical path for " + root, e);
//            }
//
//            mRoots.put(name, root);
//        }
        void addRoot(String name, String absPath) {
            mRoots.put(name, absPath);
        }

        public Uri getUriForFile(File file) {
            String path;
            try {
                path = file.getCanonicalPath();
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to resolve canonical path for " + file);
            }
            return getUriForPath(path);
        }

        public Uri getUriForPath(String path) {
            Map.Entry<String, String> mostSpecific = null;
            for (Map.Entry<String, String> root : mRoots.entrySet()) {
                final String rootPath = root.getValue();
                if (path.startsWith(rootPath) && (mostSpecific == null
                        || rootPath.length() > mostSpecific.getValue().length())) {
                    mostSpecific = root;
                }
            }

            if (mostSpecific == null) {
                throw new IllegalArgumentException(
                        "Failed to find configured root that contains " + path);
            }

            // Start at first char of path under root
            final String rootPath = mostSpecific.getValue();
            if (rootPath.endsWith("/")) {
                path = path.substring(rootPath.length());
            } else {
                path = path.substring(rootPath.length() + 1);
            }

            // Encode the tag and path separately
            path = Uri.encode(mostSpecific.getKey()) + '/' + Uri.encode(path, "/");
            return new Uri.Builder().scheme("content")
                    .authority(mAuthority).encodedPath(path).build();
        }

        public File getFileForUri(Uri uri) {
            String path = uri.getEncodedPath();

            final int splitIndex = path.indexOf('/', 1);
            final String tag = Uri.decode(path.substring(1, splitIndex));
            path = Uri.decode(path.substring(splitIndex + 1));

            final String rootPath = mRoots.get(tag);
            if (rootPath == null) {
                throw new IllegalArgumentException("Unable to find configured root for " + uri);
            }

            File file = new File(rootPath, path);
            try {
                file = file.getCanonicalFile();
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to resolve canonical path for " + file);
            }

            if (!file.getPath().startsWith(rootPath)) {
                throw new SecurityException("Resolved path jumped beyond configured root");
            }

            return file;
        }
    }

    /**
     * Copied from ContentResolver.java
     */
    private static int modeToMode(String mode) {
        int modeBits;
        if ("r".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_ONLY;
        } else if ("w".equals(mode) || "wt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else if ("wa".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_APPEND;
        } else if ("rw".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE;
        } else if ("rwt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else {
            throw new IllegalArgumentException("Invalid mode: " + mode);
        }
        return modeBits;
    }

    private static File buildPath(File base, String child) {
        if (".".equals(child)) {
            return base;
        } else {
            return new File(base, child);
        }
    }

    private static String[] copyOf(String[] original, int newLength) {
        final String[] result = new String[newLength];
        System.arraycopy(original, 0, result, 0, newLength);
        return result;
    }

    private static Object[] copyOf(Object[] original, int newLength) {
        final Object[] result = new Object[newLength];
        System.arraycopy(original, 0, result, 0, newLength);
        return result;
    }
}