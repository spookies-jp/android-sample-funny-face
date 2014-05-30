package jp.co.spookies.android.funnyface;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * メニュー　パーツ設定画面用Activity
 */
public class SelectPartsActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.select_parts);
    }
}