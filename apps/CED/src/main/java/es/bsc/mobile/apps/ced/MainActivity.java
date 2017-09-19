/*
 *  Copyright 2008-2016 Barcelona Supercomputing Center (www.bsc.es)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package es.bsc.mobile.apps.ced;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import es.bsc.mobile.annotations.Orchestration;

@Orchestration
public class MainActivity extends Activity {

    TextView measures;
    double batteryCapacity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        measures = (TextView) findViewById(R.id.measures);
        Object mPowerProfile_ = null;

        final String POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile";
        Class<?> ppClass = null;
        try {
            ppClass = Class.forName(POWER_PROFILE_CLASS);
            mPowerProfile_ = ppClass.getConstructor(Context.class).newInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            batteryCapacity = (Double) ppClass.getMethod("getAveragePower", String.class).invoke(mPowerProfile_,
                    "battery.capacity");
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.registerReceiver(this.batteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    public void start(View aview) {

        new Compute().execute();
    }

    private class Compute extends AsyncTask<String, Void, Void> {

        Long time;

        protected Void doInBackground(String... args) {
            time = System.currentTimeMillis();
            String s = getPackageName();
            Resources r = getResources();
            Computation.start(r, s);
            return null;
        }

        protected void onPostExecute(String finalReport) {
            TextView result = (TextView) findViewById(R.id.result);
            result.setText("Ha tardat" + (System.currentTimeMillis() - time) + ".\n Aixo funciona: " + finalReport);
        }
    }

    private BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
            double capacity = level * batteryCapacity;

            measures.append(System.currentTimeMillis() + ": " + " " + capacity + "maH" + " " + voltage + "V " + "\n");
        }
    };
}
