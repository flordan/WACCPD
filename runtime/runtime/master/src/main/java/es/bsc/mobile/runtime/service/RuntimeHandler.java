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
package es.bsc.mobile.runtime.service;

import android.util.Log;
import es.bsc.comm.Connection;
import es.bsc.comm.Node;
import es.bsc.comm.stage.Transfer;
import es.bsc.mobile.annotations.Parameter.Type;
import es.bsc.mobile.data.DataManager;
import es.bsc.mobile.node.RuntimeNode;
import es.bsc.mobile.runtime.components.Analyser;
import es.bsc.mobile.runtime.components.Executor;
import es.bsc.mobile.runtime.types.Task;
import es.bsc.mobile.runtime.types.data.DataInfo;
import es.bsc.mobile.runtime.types.data.access.DataAccess;
import es.bsc.mobile.runtime.types.data.parameter.FileParameter;
import es.bsc.mobile.runtime.types.data.parameter.Parameter;
import es.bsc.mobile.runtime.types.data.parameter.RegisteredParameter;
import es.bsc.mobile.runtime.types.resources.ComputingPlatform.TaskListener;
import es.bsc.mobile.runtime.types.resources.proxy.notifications.ProxiedBackEndNotification;
import es.bsc.mobile.runtime.utils.CoreManager;
import es.bsc.mobile.runtime.utils.DataRegistry;
import es.bsc.mobile.runtime.utils.ResourceManager;
import es.bsc.mobile.types.Job;
import es.bsc.mobile.types.JobProfile;
import es.bsc.mobile.types.messages.runtime.DataCreationNotification;


public final class RuntimeHandler extends RuntimeNode {

    private static final String LOGGER_TAG = "Runtime.Tasks";
    private static final DataRegistry<String> SHARED_REG = new DataRegistry<String>();

    private final ResourceManager rm = new ResourceManager(this);
    private final Analyser analyser = new Analyser(this);
    private final Executor executor = new Executor(rm);

    public RuntimeHandler(Node n) {
        super(n);
        analyser.start();
        executor.start();
    }

    @Override
    public void init() {
        super.init();
        startP2P(rm.getAllNodes());
    }

    @Override
    public void commandReceived(Connection c, Transfer t) {
        try {
            super.commandReceived(c, t);
        } catch (Exception e) {
            ProxiedBackEndNotification rn = (ProxiedBackEndNotification) t.getObject();
            rn.process();
        }
    }

    public int[] getCoreIds(String[] signatures) {
        int[] coreIds = new int[signatures.length];
        for (int i = 0; i < signatures.length; i++) {
            coreIds[i] = CoreManager.getCoreId(signatures[i]);
        }
        return coreIds;
    }

    public void executeTask(Task t) {
        Log.i(LOGGER_TAG, "New task (" + t.getId() + ") for CE " + t.getTaskParams().getCoreId() + ".");
        Log.d(LOGGER_TAG, t.toString());
        for (int i = 0; i < t.getTaskParams().getParameters().length; i++) {
            Parameter p = t.getTaskParams().getParameters()[i];
            if (p.getType() == Type.FILE) {
                String fileLocation = ((FileParameter) p).getFullFileName();
                DataInfo dataInfo = SHARED_REG.findData(fileLocation);
                if (dataInfo == null) {
                    dataInfo = SHARED_REG.registerData(fileLocation);
                    String dataId = dataInfo.getCurrentVersion().getDataInstance().getRenaming();
                    storeFile(dataId, fileLocation, new AppCreatedData(dataId));
                    appDataCreation(dataId, getMe());
                }
                DataAccess da = SHARED_REG.registerRemoteDataAccess(p.getDirection(), dataInfo);
                t.getTaskParams().getParameters()[i] = new RegisteredParameter(Type.FILE, p.getDirection(), da);
            }
        }

        analyser.newTask(t);
    }

    public void taskFinished(int taskId) {
        Log.i(LOGGER_TAG, "Task " + taskId + " has ended.");
        analyser.taskEnd(taskId);
    }

    public void resizeCoreStructures() {
        executor.resizeCores();
    }

    public void appDataCreation(String daId, Node newNode) {
        handleDataCreation(new DataCreationNotification(daId, newNode));
    }

    @Override
    public void updateMaster(Node node) {
        //I'm the master!!!! Do nothing!
    }

    public void runTask(final Task task) {
        executor.executeTask(task, new TaskListener() {
            @Override
            public void completedTask(int platformId, JobProfile jp) {
                taskFinished(task.getId());
            }

            @Override
            public void failedJob() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
    }

    @Override
    public void runJob(Job job) {
        //Never receives this message. Do nothing!
    }

    @Override
    public void notifyCloudJobEnd(int taskId, int platformId, JobProfile jp, Node host) {
        executor.notifyCloudJobEnd(taskId, platformId, jp, host);
    }


    private class AppCreatedData implements DataManager.DataOperationListener {

        private final String dataId;

        public AppCreatedData(String dataId) {
            this.dataId = dataId;
        }

        @Override
        public void paused() {
        }

        @Override
        public void setSize(long value) {
        }

        @Override
        public void setValue(Class<?> type, Object value) {
        }

        @Override
        public String toString() {
            return "value created from the application for data " + dataId + ".";
        }
    }
}
