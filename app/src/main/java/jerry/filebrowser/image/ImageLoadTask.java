package jerry.filebrowser.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.util.Size;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

import jerry.filebrowser.file.FileType;

public class ImageLoadTask extends AsyncTask<Void, Void, Bitmap> {
    private final String path;
    private final int type;
    private volatile WeakReference<ImageView> reference;
    private final int width;
    private final int height;

    public ImageLoadTask(String path, int type, ImageView imageView) {
        this.path = path;
        this.type = type;
        this.reference = new WeakReference<>(imageView);
        // 不使用imageView.getWidth()，因为这个时候imageView可能还没有测量布局
        this.width = imageView.getLayoutParams().width - imageView.getPaddingStart() - imageView.getPaddingEnd();
        this.height = imageView.getLayoutParams().height - imageView.getPaddingTop() - imageView.getPaddingBottom();
//        this.width = imageView.getLayoutParams().width;
    }


    @Override
    protected Bitmap doInBackground(Void... voids) {
        File file = new File(path);
        if (!file.exists()) return null;

        if (isCancelled()) return null;

        Bitmap bitmap = null;
        FileInputStream stream = null;
        try {
            if (type == FileType.TYPE_IMAGE) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                options.inPreferredConfig = Bitmap.Config.RGB_565;

                stream = new FileInputStream(file);
                BitmapFactory.decodeStream(stream, null, options);
                stream.close();


                if (isCancelled()) return null;

                options.inJustDecodeBounds = false;
                options.inSampleSize = ImageManager.calculateSampleRate(options.outWidth, width);

                stream = new FileInputStream(file);
                bitmap = BitmapFactory.decodeStream(stream, null, options);
                stream.close();
                stream = null;
            } else if (type == FileType.TYPE_VIDEO) {
                Size size = new Size(width, height);
                bitmap = ThumbnailUtils.createVideoThumbnail(new File(path), size, null);
                // Log.i("ImageLoadTask", "size: " + size + ", bitmap: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }
        if (!isCancelled()) return bitmap;
        return null;
    }

    // @Override
    // protected Bitmap doInBackground(Void... voids) {
    //     File file = new File(path);
    //     if (!file.exists()) return null;
    //     Bitmap bitmap = null;
    //     if (isCancelled()) return null;
    //     try {
    //         if (isCancelled()) return null;
    //         // ImageManager.calculateSampleSize(options.outWidth, width * 2);
    //         if (type == FileType.TYPE_IMAGE) {
    //             bitmap = ThumbnailUtils.createImageThumbnail(file, new Size(), null);
    //         } else if (type == FileType.TYPE_VIDEO) {
    //             bitmap = ThumbnailUtils.createVideoThumbnail(file, new Size(), null);
    //         }
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }
    //     if (!isCancelled()) return bitmap;
    //     return null;
    // }


    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (bitmap != null) {
            ImageManager.putBitmapCache(path, bitmap);
        }
        if (reference == null) return;
        final ImageView imageView = reference.get();
        if (imageView == null) return;
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        }
        imageView.setTag(null);
        reference = null;
    }

    public void cancel() {
        if (reference == null) return;
        final ImageView imageView = reference.get();
        if (imageView != null) {
            imageView.setTag(null);
        }
        reference = null;
        cancel(true);
    }
}
