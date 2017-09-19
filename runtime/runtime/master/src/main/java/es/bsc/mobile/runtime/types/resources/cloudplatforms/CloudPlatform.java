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
package es.bsc.mobile.runtime.types.resources.cloudplatforms;

import es.bsc.comm.Node;
import es.bsc.mobile.comm.CommunicationManager;
import es.bsc.mobile.runtime.types.Task;
import es.bsc.mobile.runtime.types.data.DataInstance;
import es.bsc.mobile.runtime.types.data.access.ReadAccess;
import es.bsc.mobile.runtime.types.data.access.ReadWriteAccess;
import es.bsc.mobile.runtime.types.data.access.WriteAccess;
import es.bsc.mobile.runtime.types.data.parameter.RegisteredParameter;
import es.bsc.mobile.runtime.types.profile.ImplementationProfile;
import es.bsc.mobile.runtime.types.resources.ComputingPlatformImplementation;
import es.bsc.mobile.runtime.utils.CoreManager;
import es.bsc.mobile.types.Job;
import es.bsc.mobile.types.JobProfile;
import es.bsc.mobile.types.calc.MinMax;
import es.bsc.mobile.types.messages.runtime.NewJob;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;


public abstract class CloudPlatform extends ComputingPlatformImplementation<RemoteResource> {

    private final TreeSet<String> offloadedData = new TreeSet<String>();
    private ImplementationProfile[] profiles = new ImplementationProfile[0];
    private final HashMap<Node, RemoteResource> remoteResources;
    private final HashMap<Integer, TaskListener> taskIdToListener;

    public CloudPlatform(String name) {
        super(name, null);
        remoteResources = new HashMap<Node, RemoteResource>();
        taskIdToListener = new HashMap<Integer, TaskListener>();
    }

    @Override
    public void addResource(RemoteResource res) {
        remoteResources.put(res.getNode(), res);
    }

    @Override
    public void init() {
        //Nothing to do
    }

    @Override
    public void defineDefaultProfiles(String profiles) {
        //Do nothing. Has no default values
    }

    @Override
    public final void resizeCores() {
        int newCoreCount = CoreManager.getCoreCount();
        int oldCoreCount = profiles.length;
        ImplementationProfile[] coreProfiles = new ImplementationProfile[newCoreCount];
        System.arraycopy(this.profiles, 0, coreProfiles, 0, oldCoreCount);

        for (int coreId = oldCoreCount; coreId < newCoreCount; coreId++) {
            coreProfiles[coreId] = new ImplementationProfile(0);
        }
    }

    @Override
    protected void registerNewCoreElement(int coreId, es.bsc.mobile.runtime.types.Implementation[] impls) {
        //Is never invoked by the platform. Handled directly in by resizeCores()
    }

    @Override
    protected void updateCoreElement(int coreId, es.bsc.mobile.runtime.types.Implementation[] newImpls) {
        //Is never invoked by the platform. Handled directly in by resizeCores()
    }

    @Override
    protected void registerDataFutureLocations(RegisteredParameter param, MinMax expectedCreation) {
        DataInstance daId;
        switch (param.getDirection()) {
            case IN:
                daId = ((ReadAccess) param.getDAccess()).getReadDataInstance();
                offloadedData.add(daId.getRenaming());
                break;
            case INOUT:
                daId = ((ReadWriteAccess) param.getDAccess()).getReadDataInstance();
                offloadedData.add(daId.getRenaming());
                daId = ((ReadWriteAccess) param.getDAccess()).getWrittenDataInstance();
                offloadedData.add(daId.getRenaming());
                break;
            default:
                daId = ((WriteAccess) param.getDAccess()).getWrittenDataInstance();
                offloadedData.add(daId.getRenaming());
        }
    }

    @Override
    public ExecutionScore getExecutionForecast(Task task, long submissionTime, LinkedList<TaskData> inData, LinkedList<TaskData> outData) {
        MinMax missingInput = new MinMax();
        MinMax remoteOutput = new MinMax();
        for (TaskData td : inData) {
            if (!offloadedData.contains(td.getDataName())) {
                missingInput.aggregate(td.getDataSize());
            }
        }
        for (TaskData td : outData) {
            remoteOutput.aggregate(td.getDataSize());
        }

        int coreId = task.getTaskParams().getCoreId();
        MinMax time = getTimeForecast(missingInput, remoteOutput, coreId);
        MinMax energy = getEnergyForecast(missingInput, remoteOutput, coreId);
        MinMax cost = getCostForecast(missingInput, remoteOutput, coreId);
        return new ExecutionScore(time, energy, cost);
    }

    private MinMax getTimeForecast(MinMax inData, MinMax outData, int coreId) {
        MinMax prediction = new MinMax();
        prediction.aggregate(profiles[coreId].getExecutionTime());
        return prediction;
    }

    private MinMax getEnergyForecast(MinMax inData, MinMax outData, int coreId) {
        MinMax prediction = new MinMax();
        return prediction;
    }

    private MinMax getCostForecast(MinMax inData, MinMax outData, int coreId) {
        MinMax prediction = new MinMax();
        return prediction;
    }

    @Override
    public boolean canRun(Task task) {
        return true;
    }

    @Override
    public void submitTask(Task task, ExecutionScore forecast, TaskListener listener) {
        Job job = createJob(task, forecast);
        int taskId = task.getId();
        taskIdToListener.put(taskId, listener);
        submitJob(job);
        task.hasBeenOffloaded();
    }

    protected abstract void submitJob(Job j);

    protected final void submitJob(Job job, RemoteResource worker) {
        NewJob nj = new NewJob(job);
        Node node = worker.getNode();
        CommunicationManager.notifyCommand(node, nj);

    }

    public void finishedJob(int taskId, JobProfile jp, Node executor) {
        int coreId = jp.getCoreId();
        profiles[coreId].registerProfiledJob(jp);
        TaskListener listener = this.taskIdToListener.get(taskId);
        RemoteResource worker = remoteResources.get(executor);
        finishedJob(jp.getJobId(), worker);
        listener.completedTask(taskId, jp);
    }

    protected abstract void finishedJob(String jobId, RemoteResource worker);

}
