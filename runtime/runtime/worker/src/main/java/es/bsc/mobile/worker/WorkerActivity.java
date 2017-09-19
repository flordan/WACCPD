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
package es.bsc.mobile.worker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class WorkerActivity extends Activity {

    public WorkerActivity() {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_launcher);
    }

    public void start(View aview) {
        
        EditText masterIP = (EditText) findViewById(R.id.ipMaster_text);
        EditText masterPort = (EditText) findViewById(R.id.portMaster_text);
        EditText slots = (EditText) findViewById(R.id.cores_text);
        EditText workerPort = (EditText) findViewById(R.id.portWorker_text);

        Intent serviceIntent = new Intent(this, Worker.class);
        serviceIntent.putExtra("masterIP", masterIP.getText().toString());
        serviceIntent.putExtra("masterPort", masterPort.getText().toString());
        serviceIntent.putExtra("workerPort", workerPort.getText().toString());
        serviceIntent.putExtra("slots", slots.getText().toString());
        this.startService(serviceIntent);
        
        finish();
    }

}
