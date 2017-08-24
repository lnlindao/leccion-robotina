package com.example.dptoredes.leccionrobotina;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    ListView lstServicio;
    private long last_update = 0, last_movement = 0;
    private float prevX = 0, prevY = 0, prevZ = 0;
    private float curX = 0, curY = 0, curZ = 0;
    private ArrayAdapter arrayAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView lstServicio = (ListView)findViewById(R.id.lstServicio);
        leerServicio servicio= new leerServicio();
        servicio.execute();
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public class leerServicio extends AsyncTask<Object, Object, Void> {

        @Override
        protected Void doInBackground(Object... params) {


            HttpClient httpClient = new DefaultHttpClient();

            HttpGet del =
                    new HttpGet("https://jsonplaceholder.typicode.com/posts");

            del.setHeader("content-type", "application/json");

            try
            {
                Log.e("ServicioRest","Exitoso!");
                HttpResponse resp = httpClient.execute(del);
                String respStr = EntityUtils.toString(resp.getEntity());
                Log.e(respStr);
                JSONArray respJSON = new JSONArray(respStr);

                String[] clientes = new String[respJSON.length()];

                for(int i=0; i<respJSON.length(); i++)
                {
                    JSONObject obj = respJSON.getJSONObject(i);

                    int userid = obj.getInt("userId");
                    int id = obj.getInt("id");
                    String titulo = obj.getString("title");
                    String body = obj.getString("body");

                    clientes[i] = "" + userid + "-" + id + "-" + titulo + "-" + body;
                }

                //Rellenamos la lista con los resultados
                arrayAdapter = new ArrayAdapter(MainActivity.this,
                                android.R.layout.simple_list_item_1, clientes);

                lstServicio.setAdapter(arrayAdapter);
            }
            catch(Exception ex)
            {
                Log.e("ServicioRest","Error!", ex);
            }
            return null;
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            long current_time = event.timestamp;

            curX = event.values[0];
            curY = event.values[1];
            curZ = event.values[2];

            if (prevX == 0 && prevY == 0 && prevZ == 0) {
                last_update = current_time;
                last_movement = current_time;
                prevX = curX;
                prevY = curY;
                prevZ = curZ;
            }

            long time_difference = current_time - last_update;
            if (time_difference > 0) {
                float movement = Math.abs((curX + curY + curZ) - (prevX - prevY - prevZ)) / time_difference;
                int limit = 1500;
                float min_movement = 1E-6f;
                if (movement > min_movement) {
                    if (current_time - last_movement >= limit) {
                        Toast.makeText(getApplicationContext(), "Hay movimiento de " + movement, Toast.LENGTH_SHORT).show();
                    }
                    last_movement = current_time;
                }
                prevX = curX;
                prevY = curY;
                prevZ = curZ;
                last_update = current_time;
            }

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> sensors = sm.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if (sensors.size() > 0) {
            sm.registerListener(this, sensors.get(0), SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onStop() {
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        sm.unregisterListener(this);
        super.onStop();
    }


}
