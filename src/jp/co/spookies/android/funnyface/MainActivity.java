package jp.co.spookies.android.funnyface;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import com.github.mhendred.face4j.model.Face;
import com.github.mhendred.face4j.model.Point;

import jp.co.spookies.android.funnyface.webservice.FaceComClient;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {
    // 戻り値でのACTION判定用定数
    private static final int REQUEST_GALLERY = Menu.FIRST;
    // 戻り値でのACTION判定用定数
    private static final int MENU_PARTS_SELECT = Menu.FIRST + 1;

    private Thread facecomThread;
    private Handler handler = new Handler();
    private Bitmap bmp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    /**
     * ギャラリ画像呼び出し（画像選択）
     * 
     * @param v
     * @throws Exception
     */
    public void onClickSelectImage(View v) throws Exception {
        Intent intent = new Intent();
        // 画像を指定
        intent.setType("image/*");
        // データ取得Intentを指定
        intent.setAction(Intent.ACTION_PICK);
        // 結果判別用に値をセット
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    /**
     * 他アクティビティからの遷移
     */
    protected void onActivityResult(int requestCode, int resultCode,
            Intent intent) {
        if (resultCode == RESULT_OK) {
            // ギャラリー画像選択後の処理
            if (requestCode == REQUEST_GALLERY) {
                try {
                    // 選択した画像データを復元
                    ContentResolver cr = getContentResolver();
                    Uri uri = intent.getData();
                    InputStream in = cr.openInputStream(uri);
                    byte[] image = new byte[in.available()];
                    in.read(image);

                    // 画像の回転データを取得
                    Cursor query = MediaStore.Images.Media.query(
                        cr,
                        uri,
                        new String[] { 
                                MediaStore.Images.ImageColumns.ORIENTATION 
                                },
                        null, null);
                    query.moveToFirst();
                    int orientation = query.getInt(0);

                    bmp = decodeBitmap(image, orientation);
                    in.close();

                    // 変換処理を行う
                    startFaceComThread();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * メニュー画面
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_PARTS_SELECT, Menu.NONE, R.string.parts_select)
                .setIcon(android.R.drawable.ic_menu_agenda);
        return super.onCreateOptionsMenu(menu);

    }

    /**
     * メニュー画面選択時の処理
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        // パーツ選択
        case MENU_PARTS_SELECT:
            selectParts();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 変換パーツ選択のActivityを呼び出し
     */
    private void selectParts() {
        Intent intent = new Intent(this, SelectPartsActivity.class);
        startActivity(intent);
    }

    /**
     * 画像データを復元 ※画面サイズに縮小している
     * 
     * @param image
     * @return
     */
    private Bitmap decodeBitmap(byte[] image, int orientation) {
        WindowManager manager = 
            (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        int windowWidth = display.getWidth();
        int windowHeight = display.getHeight();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        // 先に画像情報だけを読み込み
        BitmapFactory.decodeByteArray(image, 0, image.length, options);

        // 画像情報からScaleサイズを設定 （ 1 / inSampleSize ）
        if (options.outWidth > options.outHeight) {
            options.inSampleSize = (int) Math.ceil((float) options.outWidth
                    / windowWidth) - 1;
        } else {
            options.inSampleSize = (int) Math.ceil((float) options.outHeight
                    / windowHeight) - 1;
        }
        // スケールしたサイズで画像データを読み込み
        options.inJustDecodeBounds = false;

        // 回転
        Matrix matrix = new Matrix();
        Bitmap bm = BitmapFactory.decodeByteArray(image, 0, image.length,
                options);
        matrix.postRotate(orientation);
        return Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(),
                matrix, true);
    }

    /**
     * 設定画面で選択した顔パーツを変換
     * 画像を変更する場合は、res/drewable-*dpiの中にある画像を変更してください。
     * @param face
     * @param bmp
     * @return
     */
    private Bitmap convertSelectedParts(Face face, Bitmap bmp) {
        // 顔のサイズからパーツのスケールを変更
        double faceWidth = face.getWidth();
        double faceHeight = face.getHeight();
        // 左目
        if (enableEyeLeft(getBaseContext())) {
            Bitmap eyeLeft = getPartBitmap(faceWidth, faceHeight,
                    BitmapFactory.decodeResource(getResources(),
                            R.drawable.eye_left));
            bmp = convertPart(face.getLeftEye(), bmp, eyeLeft);
        }
        
        // 右目
        if (enableEyeRight(getBaseContext())) {
            Bitmap eyeRight = getPartBitmap(faceWidth, faceHeight,
                    BitmapFactory.decodeResource(getResources(),
                            R.drawable.eye_left));
            bmp = convertPart(face.getRightEye(), bmp, eyeRight);
        }
        
        // 鼻
        if (enableNose(getBaseContext())) {
            Bitmap nose = getPartBitmap(faceWidth, faceHeight,
                    BitmapFactory.decodeResource(getResources(), R.drawable.nose));
            bmp = convertPart(face.getNose(), bmp, nose);
        }
        
        // 口
        if (enableMouse(getBaseContext())) {
            Bitmap mouse = getPartBitmap(faceWidth, faceHeight,
                    BitmapFactory.decodeResource(getResources(), R.drawable.mouse));
            bmp = convertPart(face.getMouthCenter(), bmp, mouse);
        }
        return bmp;
    }

    /**
     * 顔のパーツ位置を計算して、パーツ合成を行う 100%表示から、画像サイズでの座標位置を計算
     * 
     * @param point
     * @param baseBmp
     * @param addBitmap
     * @return
     */
    private Bitmap convertPart(Point point, Bitmap baseBmp, Bitmap addBitmap) {
        point.x = (point.x / 100) * baseBmp.getWidth();
        point.y = (point.y / 100) * baseBmp.getHeight();
        baseBmp = FileUtil.composeBitmap(baseBmp, new Point(0, 0), addBitmap,
                point);
        return baseBmp;
    }

    /**
     * 顔サイズからリソース画像をスケール 各パーツファイルは画面サイズいっぱいとして生成
     * 
     * @param faceWidth
     * @param faceHeight
     * @param resBitmap
     * @return
     */
    private Bitmap getPartBitmap(double faceWidth, double faceHeight,
            Bitmap resBitmap) {
        Matrix matrix = new Matrix();
        float scaleWidth = (float) (faceWidth / 100);
        float scaleHeight = (float) (faceHeight / 100);
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap newBitmap = Bitmap.createBitmap(resBitmap, 0, 0,
                resBitmap.getWidth(), resBitmap.getHeight(), matrix, true);
        return newBitmap;
    }

    /**
     * 変換が有効か（左目）
     * 
     * @param context
     * @return
     */
    public boolean enableEyeLeft(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(getResources().getString(R.string.eye_left_key),
                        true);
    }

    /**
     * 変換が有効か（右目）
     * 
     * @param context
     * @return
     */
    public boolean enableEyeRight(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(getResources().getString(R.string.eye_right_key),
                        true);
    }

    /**
     * 変換が有効か（鼻）
     * 
     * @param context
     * @return
     */
    public boolean enableNose(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(getResources().getString(R.string.nose_key), true);
    }

    /**
     * 変換が有効か（口）
     * 
     * @param context
     * @return
     */
    public boolean enableMouse(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(getResources().getString(R.string.mouse_key), true);
    }

    /**
     * face.com APIへ接続し、画像を変更
     * 
     * @throws Exception
     */
    private void startFaceComThread() throws Exception {
        if (facecomThread != null) {
            return;
        }
        // 変換中アニメーションを表示
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(R.string.changing));
        progressDialog.show();

        facecomThread = new Thread() {
            @Override
            public void run() {
                try {
                    File picture = FileUtil.createTempJPEGFile(bmp, "face",
                            getFilesDir());

                    FaceComClient apiClient = new FaceComClient();
                    final List<Face> faces = apiClient.getFaceData(picture);

                    // 接続終了時の処理
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // 変換中アニメーションを消去
                            progressDialog.dismiss();

                            if (faces.size() == 0) {
                                // 顔データナシ
                                Toast.makeText(MainActivity.this,
                                        R.string.no_face, Toast.LENGTH_LONG)
                                        .show();
                            } else {
                                // 見つかった顔の数だけ繰り返し
                                for (Face face : faces) {
                                    bmp = convertSelectedParts(face, bmp);
                                }
                            }
                            // Viewの画像を差し替え
                            ImageView imageView = (ImageView) findViewById(R.id.selectImage);
                            imageView.setImageBitmap(bmp);
                        }
                    });
                } catch (Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // エラートーストを表示
                            Toast.makeText(MainActivity.this, R.string.failed,
                                    Toast.LENGTH_LONG).show();
                            progressDialog.cancel();
                        }
                    });
                }
                facecomThread = null;
            }
        };
        facecomThread.start();
    }

}