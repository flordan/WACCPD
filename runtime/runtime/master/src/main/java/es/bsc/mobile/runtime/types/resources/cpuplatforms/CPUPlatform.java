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
package es.bsc.mobile.runtime.types.resources.cpuplatforms;

import es.bsc.mobile.data.DataProvider;
import es.bsc.mobile.runtime.types.resources.cpuplatform.CPUResource;
import es.bsc.mobile.runtime.types.Task;
import es.bsc.mobile.runtime.types.profile.ImplementationProfile;
import es.bsc.mobile.runtime.types.resources.ComputingPlatformBackend;
import es.bsc.mobile.runtime.types.resources.LocalComputingPlatform;
import es.bsc.mobile.runtime.types.resources.cpuplatform.CPUPlatformBackEnd;
import es.bsc.mobile.runtime.types.resources.proxy.BackEndProxyInterface;
import es.bsc.mobile.runtime.utils.CoreManager;
import es.bsc.mobile.scheduler.BasicScheduler;
import es.bsc.mobile.scheduler.JobScheduler;
import es.bsc.mobile.types.calc.MinMax;
import es.bsc.mobile.types.Job;
import es.bsc.mobile.types.JobExecution;
import es.bsc.mobile.types.JobParameter;
import java.util.LinkedList;


public class CPUPlatform extends LocalComputingPlatform<CPUResource> {

    private final int coreCount;
    private final JobScheduler scheduler;

    public CPUPlatform(String name, int coreCount, DataProvider data) {
        super(constructBackEnd(coreCount, data), name);
        scheduler = new BasicScheduler(Integer.MAX_VALUE);
        this.coreCount = coreCount;
    }

    private static ComputingPlatformBackend constructBackEnd(int coreCount, DataProvider data) {
        //*
        return new BackEndProxyInterface("CPU", data, false);
        /*/
        return new CPUPlatformBackEnd(coreCount, data);
        /**/
    }

    @Override
    public void init() {
        scheduler.start();
    }

    @Override
    public MinMax getWaitingForecast(long submissionTime) {
        MinMax waiting = new MinMax(submissionTime);
        for (int coreId = 0; coreId < CoreManager.getCoreCount(); coreId++) {
            ImplementationProfile prof = getCoreProfile(coreId);
            waiting.aggregate(tasksToProcess[coreId] / coreCount, prof.getExecutionTime());
        }
        return waiting;
    }

    @Override
    protected MinMax getTimeForecast(LinkedList<DataAndStatus> inData, es.bsc.mobile.types.Implementation impl) {
        ImplementationProfile prof = getImplementationProfile(impl.getCoreElementId(), impl.getImplementationId());
        return new MinMax(prof.getExecutionTime());
    }

    @Override
    protected MinMax getEnergyForecast(LinkedList<DataAndStatus> inData, es.bsc.mobile.types.Implementation impl) {
        ImplementationProfile prof = getImplementationProfile(impl.getCoreElementId(), impl.getImplementationId());
        return prof.getEnergyConsumption();
    }

    @Override
    protected MinMax getCostForecast(LinkedList<DataAndStatus> inData, es.bsc.mobile.types.Implementation impl) {
        return new MinMax();
    }

    @Override
    public void submitTask(Task task, ExecutionScore forecast, TaskListener listener) {
        tasksToProcess[task.getTaskParams().getCoreId()]++;
        Job job = createJob(task, forecast);
        JobExecution je = newJobExecution(job, new CPUJobExecutionMonitor(listener));
        scheduler.newJob(je);
    }


    private class CPUJobExecutionMonitor extends LocalJobExecutionMonitor {

        public CPUJobExecutionMonitor(TaskListener listener) {
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
