package jerry.filebrowser.image;

import android.graphics.Bitmap;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;

public class ImageManager {
    // 一张约3000-8000
    private static final LruCache<String, Bitmap> thumbnailCache = new LruCache<String, Bitmap>(2 * 1024 * 1024) {
        protected int sizeOf(@NonNull String key, Bitmap value) {
            return value.getAllocationByteCount();
        }
    };
//
//    public interface ImageLoadListener {
//        void onFinish();
//    }

    public static void loadThumbnail(String path, ImageView imageView) {
        Object object = imageView.getTag();
        if (object instanceof ImageLoadTask) {
            ImageLoadTask task = (ImageLoadTask) object;
            task.cancel();
        }
        Bitmap bitmap = thumbnailCache.get(path);
        if (bitmap != null) {
            //Log.i("666", "命中缓存：" + bitmap.getWidth() + "x" + bitmap.getHeight() + "-" + bitmap.getByteCount() + "-" + bitmap.getAllocationByteCount());
            imageView.setImageBitmap(bitmap);
        } else {
            //Log.i("666", "没有缓存");
            loadLocalThumbnailAsync(path, imageView);
        }
    }

//    public static void loadBigImage(Stone stone, ImageView imageView) {
//        if (bigImageCache != null) {
//            imageView.setImageBitmap(bigImageCache);
//        } else if (loadLocalBigImage(getImagePath(stone.chaName, 1), imageView) == null) {
//            if (stone.bigImageUrl != null) {
//                downloadImageAsync(stone.id, stone.thumbnailUrl, getImagePath(stone.chaName, 1), imageView);
//            }
//        }
//    }
//
//    public static Bitmap toBitmap(File file, int width) {
//        if (file.exists()) {
//            BitmapFactory.Options options = new BitmapFactory.Options();
//            options.inJustDecodeBounds = true;
//            options.inPreferredConfig = Bitmap.Config.RGB_565;
//            Bitmap bitmap = null;
//            try {
//                FileInputStream stream = new FileInputStream(file);
//                BitmapFactory.decodeStream(stream, null, options);
//                stream.close();
//                options.inJustDecodeBounds = false;
//                options.inSampleSize = calculateSampleSize(options.outWidth, width);
//                stream = new FileInputStream(file);
//                bitmap = BitmapFactory.decodeStream(stream, null, options);
//                if (bitmap != null)
//                    Log.i("666", bitmap.getWidth() + "x" + bitmap.getHeight() + "-" + bitmap.getByteCount() + "-" + bitmap.getAllocationByteCount());
//                stream.close();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            return bitmap;
//        } else {
//            return null;
//        }
//    }

    public static int calculateSampleSize(int width, int targetWidth) {
        if (targetWidth <= 0) return 1;
        int sampleSize = 1;
        while (width > targetWidth) {
            sampleSize = sampleSize << 1;
            width = width >> 1;
        }
        return sampleSize;
    }

//
//    private static Bitmap loadLocalImage(String path, ImageView imageView) {
//        ViewGroup.LayoutParams params = imageView.getLayoutParams();
//        return toBitmap(new File(path), params.width);
//    }

//
//    private static Bitmap loadLocalThumbnail(String path, ImageView imageView) {
//        Bitmap bitmap = loadLocalImage(path, imageView);
//        if (bitmap != null) {
//            imageView.setImageBitmap(bitmap);
//        }
//        return bitmap;
//    }

    public static void putBitmapCache(String path, Bitmap bitmap) {
        thumbnailCache.put(path, bitmap);
    }


    private static void loadLocalThumbnailAsync(String path, ImageView imageView) {
        ImageLoadTask task = new ImageLoadTask(path, imageView);
        imageView.setTag(task);
        task.execute();
    }

    public static void onLowMemory() {
        thumbnailCache.evictAll();
    }
}