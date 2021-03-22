package com.thematic.blindTool;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Locale;

public class Features_Activity extends AppCompatActivity implements View.OnLongClickListener {

    TextToSpeech textToSpeech;
    float a = 1.3f;
    boolean sensor1 = false;
    boolean sensor2 = true;
    boolean sensor3 = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_features_);

        Intent getInt = getIntent();
        String s1 = getInt.getStringExtra("sensor_1");
        String s2 = getInt.getStringExtra("sensor_2");
        String s3 = getInt.getStringExtra("sensor_3");

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.TAIWAN);

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(Features_Activity.this, "not sup", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(Features_Activity.this, "success", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(Features_Activity.this, "fail", Toast.LENGTH_SHORT).show();
                }
            }
        });
        textToSpeech.setPitch(a);
        textToSpeech.setSpeechRate(a);

        sensor1 = get_intent(s1);
        sensor2 = get_intent(s2);
        sensor3 = get_intent(s3);
    }

    private boolean get_intent(String v) {
        if (v.equals("1")) {
            return true;
        } else {
            return false;
        }
    }

    public void change_check_status(View view) {

        switch (view.getId()) {
            case R.id.sensor1:
                sensor1 = !sensor1;
                TextSpeech(1, status(sensor1));
                break;
            case R.id.sensor2:
                sensor2 = !sensor2;
                TextSpeech(2, status(sensor2));
                break;
            case R.id.sensor3:
                sensor3 = !sensor3;
                TextSpeech(3, status(sensor3));
                break;
            case R.id.check_and_back:
                Intent check_and_back_intent = getIntent();
                check_and_back_intent.putExtra("check_status", finish_check(sensor1, sensor2, sensor3));
                setResult(3, check_and_back_intent);
                finish();
                break;
        }
    }

    //判斷關閉或開啟
    private String status(boolean status) {
        if (status) {
            return "開啟";
        } else {
            return "關閉";
        }
    }

    //語音
    private void TextSpeech(int btn, String status_text) {
        String sensor_name = "";
        switch (btn) {
            case 1:
                sensor_name = "測距";
                break;
            case 2:
                sensor_name = "提示音";
                break;
            case 3:
                sensor_name = "震動";
                break;
        }
        //textToSpeech.speak(sensor_name + status_text, TextToSpeech.QUEUE_FLUSH, null);
        textToSpeech.setPitch(a);
        textToSpeech.setSpeechRate(a);
        textToSpeech.speak(sensor_name + status_text, TextToSpeech.QUEUE_FLUSH, null);
    }

    private String finish_check(boolean sen1, boolean sen2, boolean sen3) {
        String sen1_str = "0";
        String sen2_str = "1";
        String sen3_str = "1";

        sen1_str = ff(sen1);
        sen2_str = ff(sen2);
        sen3_str = ff(sen3);

        return sen1_str + sen2_str + sen3_str;
    }

    private String ff(boolean f_2) {
        if (f_2) {
            return "1";
        } else {
            return "0";
        }
    }

    @Override
    public boolean onLongClick(View view) {
        return false;
    }
}
