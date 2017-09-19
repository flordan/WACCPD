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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import es.bsc.comm.Node;
import es.bsc.comm.nio.NIONode;
import es.bsc.mobile.comm.CommunicationManager;


public class Worker extends Service {

    private static final String LOGGER_TAG = "Runtime.Worker";

    public Worker() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOGGER_TAG, "Servei creat!");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.i(LOGGER_TAG, "Servei Apagat!");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int myPort = Integer.parseInt((String) intent.getExtras().get("workerPort"));
        int slots = Integer.parseInt((String) intent.getExtras().get("slots"));
        String masterIP = (String) intent.getExtras().get("masterIP");
        int masterRuntimePort = Integer.parseInt((String) intent.getExtras().get("masterPort"));

        System.out.println("Master IP " + masterIP);
        System.out.println("Master Port " + masterRuntimePort);
        System.out.println("Worker Port " + myPort);
        System.out.println("Cores " + slots);

        final Node me = new NIONode(null, myPort);
        Log.i(LOGGER_TAG, "Opening server on " + me + " allowing " + slots + " tasks");

        final Node masterRuntime = new NIONode(masterIP, masterRuntimePort);
        Log.i(LOGGER_TAG, "Locating runtime at " + masterRuntime);

        RuntimeWorker rw = new RuntimeWorker(slots, me, masterRuntime);
        CommunicationManager.start(rw);
        return START_NOT_STICKY;
    }
}
