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
package es.bsc.mobile.runtime.types.resources.cpuplatform;

import es.bsc.mobile.data.DataManager;
import es.bsc.mobile.data.DataProvider;
import es.bsc.mobile.runtime.types.resources.ComputingPlatformBackend;
import es.bsc.mobile.runtime.types.resources.Resource;
import es.bsc.mobile.types.Implementation;
import es.bsc.mobile.types.Job;
import es.bsc.mobile.types.JobExecution;
import es.bsc.mobile.types.JobExecutionMonitor;
import es.bsc.mobile.types.JobParameter;
import es.bsc.mobile.types.Method;
import es.bsc.mobile.utils.JobExecutor;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Computing Platform backend for the CPU Computing Platform.
 *
 */
public class CPUPlatformBackEnd implements ComputingPlatformBackend {

    private final static long POWER_CONSUMPTION = 1600l;

    private final DataProvider data;
    private LinkedList<Implementation>[] compatibleImpls;

    private final ExecutorService executor;
    private final LinkedBlockingQueue<JobExecution> toExecuteJobs;

    public CPUPlatformBackEnd(int cores, DataProvider localDataProvider) {
        executor = Executors.newFixedThreadPool(cores);
        toExecuteJobs = new LinkedBlockingQueue<JobExecution>();
        for (int i = 0; i < cores; i++) {
            executor.execute(new JobExecutor(toExecuteJobs));
        }
        data = localDataProvider;
        compatibleImpls = new LinkedList[0];
    }

    @Override
    public void addResource(Resource resource) {

    }

    @Override
    public void registerNewCoreElement(int coreId, LinkedList<Implementation> internalImpls) {
        int newCount = coreId + 1;
        int oldCount = compatibleImpls.length;
        if (oldCount <= coreId) {
            LinkedList<Implementation>[] compatibleImpls = new LinkedList[newCount];
            System.arraycopy(this.compatibleImpls, 0, compatibleImpls, 0, oldCount);
            for (int core = oldCount; core < newCount; core++) {
                compatibleImpls[core] = new LinkedList<Implementation>();
            }
            this.compatibleImpls = compatibleImpls;
        }
        for (Implementation impl : internalImpls) {
            if (canRun(impl)) {
                compatibleImpls[coreId].add(impl);
            }
        }
    }

    @Override
    public void updateCoreElement(int coreId, LinkedList<Implementation> impls) {
        for (Implementation impl : impls) {
            if (canRun(impl)) {
                compatibleImpls[coreId].add(impl);
            }
        }
    }

    private boolean canRun(Implementation impl) {
        return impl instanceof Method;
    }

    @Override
    public LinkedList<Implementation> getCompatibleImplementations(int coreId) {
        return compatibleImpls[coreId];
    }

    @Override
    public JobExecution newJobExecution(Job job, JobExecutionMonitor orchestrator) {
        return new CPUJobExecution(job, orchestrator);
    }

    @Override
    public DataManager.DataStatus getDataStatus(String dataId) {
        return data.getDataStatus(dataId);
    }


    private class CPUJobExecution extends JobExecution {

        public CPUJobExecution(Job j, JobExecutionMonitor orchestrator) {
            super(j, orchestrator);
        }

        @Override
        protected void requestParamDataExistence(String dataId, int paramId, DataManager.DataExistenceListener listener) {
            data.requestDataExistence(dataId, listener);
        }

        @Override
        public void paramDataExists(JobParameter jp) {
            this.notifyJobExecutionDataExistence(jp);
        }

        @Override
        public void allParamDataExists() {
            this.notifyJobExecutionAllDataExistence();
        }

        @Override
        public void obtainDataSize(String dataId, int paramId, DataManager.DataOperationListener listener) {
            data.obtainDataSize(dataId, listener);
        }

        @Override
        public void obtainDataAsObject(String dataId, String dataRenaming, int paramId, JobExecution.LoadParamData listener) {
            data.obtainDataAsObject(dataId, dataRenaming, listener);
        }

        @Override
        public void obtainDataAsFile(String dataId, String dataRenaming, int paramId, JobExecution.LoadParamData listener) {
            data.obtainDataAsFile(dataId, dataRenaming, listener);
        }

        @Override
        public void allDataPresent() {
            allDataReady();
        }

        @Override
        public void prepareJobParameter(int paramId) {
            //Parameters do not go through preparation stage. Upon obtention can already be used
        }

        @Override
        public void prepareTargetObject() {
            //Parameters do not go through preparation stage. Upon obtention can already be used
        }

        @Override
        public void prepareResult() {
            //Parameters do not go through preparation stage. Upon obtention can already be used
        }

        @Override
        public void allDataReady() {
            this.notifyJobExecutionAllDataReady();
        }

        @Override
        public LinkedList<es.bsc.mobile.types.Implementation> getCompatibleImplementations() {
            return CPUPlatformBackEnd.this.getCompatibleImplementations(getJob().getImplementations()[0].getCoreElementId());
        }

        @Override
        public void executeOn(Object ID, es.bsc.mobile.types.Implementation impl) {
            getJob().selectImplementation(impl);
            toExecuteJobs.offer(this);
        }

        @Override
        public void failedExecution() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void finishedExecution() {
            this.notifyJobExecutionFinished();
        }

        @Override
        protected void storeObject(String dataId, Object value, DataManager.DataOperationListener listener) {
            data.storeObject(dataId, value, listener);
        }

        @Override
        protected void storeFile(String dataId, String location, DataManager.DataOperationListener listener) {
            data.storeFile(dataId, location, listener);
        }

        @Override
        public void completed() {
            long length = this.getJob().getJobProfile().getExecutionTime();
            this.getJob().setConsumption(POWER_CONSUMPTION * length);
            notifyJobExecutionCompletion();
        }
    }
}
