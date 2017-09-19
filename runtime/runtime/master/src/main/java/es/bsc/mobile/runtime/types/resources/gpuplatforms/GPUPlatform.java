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
package es.bsc.mobile.runtime.types.resources.gpuplatforms;

import es.bsc.mobile.annotations.Parameter.Type;
import es.bsc.mobile.data.DataManager;
import es.bsc.mobile.data.DataProvider;
import es.bsc.mobile.data.access.RWAccess;
import es.bsc.mobile.data.access.WAccess;
import es.bsc.mobile.runtime.types.resources.gpuplatform.OpenCLResource;
import es.bsc.mobile.types.Implementation;
import es.bsc.mobile.runtime.types.Task;
import es.bsc.mobile.runtime.types.profile.ImplementationProfile;
import es.bsc.mobile.runtime.types.resources.ComputingPlatformBackend;
import es.bsc.mobile.runtime.types.resources.LocalComputingPlatform;
import es.bsc.mobile.runtime.types.resources.gpuplatform.GPUPlatformBackEnd;
import es.bsc.mobile.runtime.types.resources.proxy.BackEndProxyInterface;
import es.bsc.mobile.runtime.utils.CoreManager;
import es.bsc.mobile.scheduler.BasicScheduler;
import es.bsc.mobile.scheduler.JobScheduler;
import es.bsc.mobile.types.Job;
import es.bsc.mobile.types.JobExecution;
import es.bsc.mobile.types.JobParameter;
import es.bsc.mobile.types.JobParameter.ObjectJobParameter;
import es.bsc.mobile.types.calc.MinMax;
import java.util.LinkedList;
import java.util.Set;


public class GPUPlatform extends LocalComputingPlatform<OpenCLResource> {

    private final JobScheduler scheduler;
    private final Set<String> onCreationData;

    public GPUPlatform(String name, DataProvider data, Set<String> onCreationData) {
        super(constructBackEnd(data, onCreationData), name);
        scheduler = new BasicScheduler(Integer.MAX_VALUE);
        this.onCreationData = onCreationData;
    }

    private static ComputingPlatformBackend constructBackEnd(DataProvider data, Set<String> onCreationData) {
        //*
        return new BackEndProxyInterface("GPU", new GPUDataProvider(data, onCreationData), true);
        /*/
        return new GPUPlatformBackEnd(new GPUDataProvider(data, onCreationData));
        /**/
    }


    private static class GPUDataProvider implements DataProvider {

        private final Set<String> onCreationData;
        private final DataProvider data;

        public GPUDataProvider(DataProvider data, Set<String> onCreationData) {
            this.data = data;
            this.onCreationData = onCreationData;
        }

        @Override
        public DataManager.DataStatus getDataStatus(String dataId) {
            return data.getDataStatus(dataId);
        }

        @Override
        public void requestDataExistence(String dataId, DataManager.DataExistenceListener listener) {
            if (onCreationData.contains(dataId)) {
                listener.exists();
            } else {
                data.requestDataExistence(dataId, listener);
            }
        }

        @Override
        public void obtainDataSize(String dataId, DataManager.DataOperationListener listener) {
            data.obtainDataSize(dataId, listener);
        }

        @Override
        public void obtainDataAsObject(String dataId, String dataRenaming, DataManager.DataOperationListener listener) {
            data.obtainDataAsObject(dataId, dataRenaming, listener);
        }

        @Override
        public void obtainDataAsFile(String dataId, String dataRenaming, DataManager.DataOperationListener listener) {
            data.obtainDataAsFile(dataId, dataRenaming, listener);
        }

        @Override
        public void storeObject(String dataId, Object value, DataManager.DataOperationListener listener) {
            data.storeObject(dataId, value, listener);
        }

        @Override
        public void storeFile(String dataId, String location, DataManager.DataOperationListener listener) {
            data.storeFile(dataId, location, listener);
        }

    }

    @Override
    public void init() {
        scheduler.start();
    }

    @Override
    public void submitTask(Task task, ExecutionScore forecast, TaskListener listener) {
        tasksToProcess[task.getTaskParams().getCoreId()]++;
        Job job = createJob(task, forecast);
        JobExecution je = newJobExecution(job, new GPUJobExecutionMonitor(listener));
        final JobParameter[] params = job.getParams();
        for (JobParameter jp : params) {
            if (jp.getType() == Type.OBJECT) {
                ObjectJobParameter op = (ObjectJobParameter) jp;
                String outDataId;
                switch (op.getDirection()) {
                    case INOUT:
                        RWAccess readWrite = (RWAccess) op.getDataAccess();
                        outDataId = readWrite.getWrittenDataInstance();
                        onCreationData.add(outDataId);
                        break;
                    case OUT: //case OUT:
                        WAccess write = (WAccess) op.getDataAccess();
                        outDataId = write.getWrittenDataInstance();
                        onCreationData.add(outDataId);
                }
            }
        }
        if (job.getResult() != null) {
            ObjectJobParameter op = (ObjectJobParameter) job.getResult();
            String dataId = ((WAccess) op.getDataAccess()).getWrittenDataInstance();
            onCreationData.add(dataId);
        }
        scheduler.newJob(je);
    }

    @Override
    public MinMax getWaitingForecast(long submissionTime) {
        MinMax waiting = new MinMax(submissionTime);
        for (int coreId = 0; coreId < CoreManager.getCoreCount(); coreId++) {
            ImplementationProfile prof = getCoreProfile(coreId);
            waiting.aggregate(tasksToProcess[coreId], prof.getExecutionTime());
        }
        return waiting;
    }

    @Override
    protected MinMax getTimeForecast(LinkedList<DataAndStatus> inData, es.bsc.mobile.types.Implementation impl) {
        ImplementationProfile prof = getImplementationProfile(impl.getCoreElementId(), impl.getImplementationId());
        return new MinMax(prof.getExecutionTime());
    }

    @Override
    protected MinMax getEnergyForecast(LinkedList<DataAndStatus> inData, Implementation impl) {
        ImplementationProfile prof = getImplementationProfile(impl.getCoreElementId(), impl.getImplementationId());
        return prof.getEnergyConsumption();
    }

    @Override
    protected MinMax getCostForecast(LinkedList<DataAndStatus> inData, Implementation impl) {
        return new MinMax();
    }


    private class GPUJobExecutionMonitor extends LocalJobExecutionMonitor {

        public GPUJobExecutionMonitor(TaskListener listener) {
            super(listener);
        }

        @Override
        public void notifyParamValueExistence(JobExecution je, JobParameter jp) {
            scheduler.notifyParamValueExistence(je, jp);
        }

        @Override
        public void dependencyFreeJob(JobExecution je) {
            scheduler.dependencyFreeJob(je);
        }

        @Override
        public void allValuesObtained(JobExecution je) {
            scheduler.allValuesObtained(je);
        }

        @Override
        public void allValuesReady(JobExecution je) {
            scheduler.allValuesReady(je);
        }

        @Override
        public void executedJob(JobExecution je) {
            tasksToProcess[je.getJob().getJobProfile().getCoreId()]--;
            scheduler.executedJob(je);
        }

        @Override
        public void completedJob(JobExecution je) {
            super.completedJob(je);
            scheduler.completedJob(je);
        }
    }
}
