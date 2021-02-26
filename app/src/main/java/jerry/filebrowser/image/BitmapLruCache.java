package jerry.filebrowser.image;

import android.graphics.Bitmap;

import java.util.Iterator;
import java.util.LinkedHashMap;

public class BitmapLruCache extends LinkedHashMap<String, Bitmap> {
    private final int maxSize;
    private int size;

    public BitmapLruCache(int maxSize) {
        super(16, 0.75f, true);
        this.maxSize = maxSize;
    }

    @Override // 把图像存入到缓存，key用来取出图像
    public Bitmap put(String key, Bitmap value) {
        size += value.getAllocationByteCount();
        Bitmap bitmap = super.put(key, value);
        if (bitmap != null) {// 判断是否已经在缓存中
            size -= bitmap.getAllocationByteCount();// 减少缓存空间
        }
        if (size > maxSize) {// 判断缓存是否已满
            removeLast();// 淘汰队列尾部的Bitmap缓存
        }
        return bitmap;
    }
    // 移除队尾图像并回收缓存空间
    private synchronized void removeLast() {
        Entry<String, Bitmap> last;
        Iterator<Entry<String, Bitmap>> cacheIterator;
        while (size > maxSize) {// 寻找队尾的Bitmap对象
            cacheIterator = entrySet().iterator();
            last = cacheIterator.next();
            final Bitmap bitmap = last.getValue();
            size -= bitmap.getAllocationByteCount();// 回收缓存空间
            cacheIterator.remove();// 移除缓存
        }
    }

    @Override
    public Bitmap remove(Object key) {
        Bitmap bitmap = super.remove(key);
        if (bitmap != null) size -= bitmap.getAllocationByteCount();
        return bitmap;
    }

    @Override
    public void clear() {
        size = 0;
        super.clear();
    }
}
