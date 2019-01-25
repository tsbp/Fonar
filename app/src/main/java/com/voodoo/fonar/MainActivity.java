package com.voodoo.fonar;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;

import org.eclipse.paho.client.mqttv3.MqttClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    public final static String PARAM_TOPIC = "time";
    public final static String PARAM_DATA = "task";

    public final static String BROADCAST_ACTION = "com.voodoo.fonar.broadcast";
    ImageView bMode;
    ImageView bCfg;
    SeekBar sValue;
    TextView tValue, tState;
    Context c;
    BroadcastReceiver br;
    boolean seekbarActive = false;
    int light;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        c =   this.getApplicationContext();

        bMode  = (ImageView) findViewById(R.id.btnMode);
        bCfg   = (ImageView) findViewById(R.id.btnCfg);
        sValue = (SeekBar)   findViewById(R.id.sbValue);
        tValue = (TextView)  findViewById(R.id.tvValue);
        tState = (TextView)  findViewById(R.id.tvState);

        if(!isMyServiceRunning(MqttService.class)) MqttService.actionStart(this.getApplicationContext());

        sValue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            String s;
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                s = progress + "";
                tValue.setText(s + "%");
            }
            public void onStartTrackingTouch(SeekBar arg0) {
                seekbarActive = true;
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekbarActive = false;
                MqttService.MQTT_PUBLISH_MESSAGE = s;
                MqttService.actionPublish(c);
            }
        });

        // создаем BroadcastReceiver
        br = new BroadcastReceiver() {
            // действия при получении сообщений
            public void onReceive(Context context, Intent intent) {
                String topic = intent.getStringExtra(PARAM_TOPIC);
                String data = intent.getStringExtra(PARAM_DATA);
                //tState.setText(data);
                switch(topic)
                {
                    case "stt":
                    {
                        int indStart = 0, indStop = 0;
                        //parse message
                        //get time
                        indStop = data.indexOf(',');
                        String tmp = data.substring(indStart, indStop);
                        if(tmp.equals("CFG"))
                        {
                            dialogCfg(data.substring(indStop + 1, data.length()));
                        }
                        else {
                            tState.setText(tmp.substring(0, tmp.length() - 1));
                            //get mode
                            data = data.substring(indStop + 1, data.length());
                            indStop = data.indexOf(',');
                            //bMode.setText(data.substring(0, indStop));
                            int src;
                            if(data.substring(0, indStop).contains("AUTO")) src = R.drawable.auto;
                            else                                            src = R.drawable.manual;
                            bMode.setImageResource(src);
                            //get value
                            data = data.substring(indStop + 1, data.length());
                            indStop = data.indexOf(',');
                            light = Integer.parseInt(data.substring(0, indStop));
                            if (!seekbarActive)
                            {
                                sValue.setProgress(light);
                                tValue.setText( light + "%");
                                bCfg.setColorFilter(0xff000000 |
                                                ((int)(2.55*light) << 16) |
                                                ((int)(2.55*light) << 8) |
                                                ((int)(2.55*light) << 0),
                                        PorterDuff.Mode.SRC_IN);
                            }
                        }
                    } break;

                    case "termo":
                        break;
                }



            }
        };

        IntentFilter intFilt = new IntentFilter(MainActivity.BROADCAST_ACTION);
        registerReceiver(br, intFilt);
    }

    //==============================================================================================
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    //==============================================================================================
    public void onClickEvent(View v)
    {
        switch (v.getId())        {

            case R.id.btnMode:
                MqttService.MQTT_PUBLISH_MESSAGE = "MODE_AUTO";
                MqttService.actionPublish(this.getApplicationContext());
                break;

            case R.id.btnCfg:
                MqttService.MQTT_PUBLISH_MESSAGE = "GET,11,23,4,55,45";
                MqttService.actionPublish(this.getApplicationContext());
                break;
        }
    }
    //==============================================================================================
    int _hourStart,    _minStart,    _hourStop,    _minStop;
    //==============================================================================================
    void dialogCfg(String aData) {
        final AlertDialog.Builder popDialog = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        final LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View Viewlayout = inflater.inflate(R.layout.dialog_cfg, (ViewGroup) findViewById(R.id.dialCfg));

        //popDialog.setIcon(R.drawable.compass_small);
        popDialog.setTitle("cfg");
        popDialog.setView(Viewlayout);


        final Button bSetB = (Button)  Viewlayout.findViewById(R.id.btnSet);
        final Button bSetE = (Button)  Viewlayout.findViewById(R.id.btnSet2);
        final EditText bSetL = (EditText)  Viewlayout.findViewById(R.id.btnSet3);
        final EditText bSetD = (EditText)  Viewlayout.findViewById(R.id.btnSet4);
        final EditText bSetTZ = (EditText)  Viewlayout.findViewById(R.id.btnTimezone);


        int indStart = 0, indStop = 0;
        indStop = aData.indexOf(',');
        final int hourStart = Integer.parseInt(aData.substring(indStart, indStop));
        indStart = indStop + 1;
        aData = aData.substring(indStart, aData.length());
        indStop = aData.indexOf(',');
        final int minStart  = Integer.parseInt(aData.substring(0, indStop));
        indStart = indStop + 1;
        aData = aData.substring(indStart, aData.length());
        indStop = aData.indexOf(',');
        final int hourStop = Integer.parseInt(aData.substring(0, indStop));
        indStart = indStop + 1;
        aData = aData.substring(indStart, aData.length());
        indStop = aData.indexOf(',');
        final int minStop = Integer.parseInt(aData.substring(0, indStop));
        // light
        indStart = indStop + 1;
        aData = aData.substring(indStart, aData.length());
        indStop = aData.indexOf(',');
        bSetL.setText(aData.substring(0, indStop));//final int minStop = Integer.parseInt(aData.substring(0, indStop));
        // manual duration
        indStart = indStop + 1;
        aData = aData.substring(indStart, aData.length());
        indStop = aData.indexOf(',');
        bSetD.setText(aData.substring(0, indStop));

        // manual duration
        indStart = indStop + 1;
        aData = aData.substring(indStart, aData.length());
        indStop = aData.indexOf(',');
        bSetTZ.setText(aData.substring(0, indStop));

        _hourStart = hourStart;
        _minStart = minStart;
        _hourStop = hourStop;
        _minStop = minStop;

        bSetB.setText(String.format("%02d:%02d", hourStart, minStart));
        bSetE.setText(String.format("%02d:%02d", hourStop,  minStop));

        bSetB.setOnClickListener(new View.OnClickListener() {
                                         @Override
                                         public void onClick(View v) {
                                             TimePickerDialog mTimePicker;
                                             mTimePicker = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                                                 @Override
                                                 public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                                                     bSetB.setText(String.format("%02d:%02d", selectedHour, selectedMinute));
                                                     _hourStart = selectedHour;
                                                     _minStart  = selectedMinute;
                                                 }
                                             }, hourStart, minStart, true);//Yes 24 hour time
                                             mTimePicker.setTitle("Select Time start");
                                             mTimePicker.show();
                                         }
        });

        bSetE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        bSetE.setText(String.format("%02d:%02d", selectedHour, selectedMinute));
                        _hourStop = selectedHour;
                        _minStop  = selectedMinute;
                    }
                }, hourStop, minStop, true);//Yes 24 hour time
                mTimePicker.setTitle("Select Time start");
                mTimePicker.show();
            }
        });



        popDialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        MqttService.MQTT_PUBLISH_MESSAGE = String.format("SET,%d,%d,%d,%d,%d,%d,%d,",
                                _hourStart,
                                _minStart,
                                _hourStop,
                                _minStop,
                                Integer.parseInt(bSetL.getText().toString()),
                                Integer.parseInt(bSetD.getText().toString()),
                                Integer.parseInt(bSetTZ.getText().toString()));
                        MqttService.actionPublish(c);
                        dialog.dismiss();
                    }
                });
        popDialog.setNegativeButton("CANCEL",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        popDialog.create();
        popDialog.show();
    }
    //==============================================================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        MqttService.actionStop(this.getApplicationContext());
    }
}
