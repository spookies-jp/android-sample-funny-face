package jp.co.spookies.android.funnyface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import com.github.mhendred.face4j.model.Point;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * ファイル操作用Utilクラス
 */
public class FileUtil {

    /**
     * 仮のJPEGファイルを生成
     * 
     * @param bmp
     * @param filename
     * @param dirPath
     * @return
     * @throws Exception
     */
    public static File createTempJPEGFile(Bitmap bmp, String filename,
            File dirPath) throws Exception {
        OutputStream out = null;
        try {
            File file = File.createTempFile(filename, "jpg", dirPath);
            out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
            return file;
        } catch (Exception e) {
            throw e;
        } finally {
            out.close();
        }
    }

    /**
     * Bitmap画像を合成
     * 
     * @param bm1
     * @param p1
     * @param bm2
     * @param p2
     * @return
     */
    public static Bitmap composeBitmap(Bitmap bm1, Point p1, Bitmap bm2,
            Point p2) {
        // bm1の大きさが基準
        Bitmap newBitmap = Bitmap.createBitmap(bm1.getWidth(), bm1.getHeight(),
                Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(newBitmap);
        canvas.drawBitmap(bm1, p1.x, p1.y, (Paint) null);
        // 重ねる画像をPointの中央に配置
        canvas.drawBitmap(bm2, p2.x - (bm2.getWidth() / 2),
                p2.y - (bm2.getHeight() / 2), (Paint) null);
        return newBitmap;
    }

}
