package jerry.filebrowser.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;

public class ImageLoadTask extends AsyncTask<Void, Void, Bitmap> {
    private final String path;
    private volatile WeakReference<ImageView> reference;
    private final int width;

    public ImageLoadTask(String path, ImageView imageView) {
        this.path = path;
        this.reference = new WeakReference<>(imageView);
        // 不使用imageView.getWidth()，因为这个时候imageView可能还没有测量布局
        this.width = imageView.getLayoutParams().width - imageView.getPaddingStart() - imageView.getPaddingEnd();
//        this.width = imageView.getLayoutParams().width;
    }


    @Override
    protected Bitmap doInBackground(Void... voids) {
        File file = new File(path);
        if (!file.exists()) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = null;
        if (isCancelled()) return null;
        try {
            FileInputStream stream = new FileInputStream(file);
            BitmapFactory.decodeStream(stream, null, options);
            stream.close();
            if (isCancelled()) return null;
            options.inJustDecodeBounds = false;
            options.inSampleSize = ImageManager.calculateSampleSize(options.outWidth, width);
            stream = new FileInputStream(file);
            bitmap = BitmapFactory.decodeStream(stream, null, options);
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!isCancelled()) return bitmap;
        return null;
    }


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
