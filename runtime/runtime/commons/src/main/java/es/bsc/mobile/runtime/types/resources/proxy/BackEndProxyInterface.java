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
package es.bsc.mobile.runtime.types.resources.proxy;

import es.bsc.mobile.annotations.Parameter.Direction;
import es.bsc.mobile.annotations.Parameter.Type;
import es.bsc.mobile.comm.CommunicationManager;
import es.bsc.mobile.data.DataManager;
import es.bsc.mobile.data.DataProvider;
import es.bsc.mobile.exceptions.UnexpectedDirectionException;
import es.bsc.mobile.runtime.types.resources.ComputingPlatformBackend;
import es.bsc.mobile.runtime.types.resources.Resource;
import es.bsc.mobile.runtime.types.resources.proxy.notifications.ProxiedBackEndNotification;
import es.bsc.mobile.runtime.types.resources.proxy.requests.JobExecutionRequest;
import es.bsc.mobile.runtime.types.resources.proxy.requests.ObtainJobInputDependenciesRequest;
import es.bsc.mobile.runtime.types.resources.proxy.requests.ObtainDataAsObjectRequest;
import es.bsc.mobile.runtime.types.resources.proxy.requests.PrepareJobInputDependenciesRequest;
import es.bsc.mobile.runtime.types.resources.proxy.requests.RunJobExecutionRequest;
import es.bsc.mobile.types.Implementation;
import es.bsc.mobile.types.Job;
import es.bsc.mobile.types.JobExecution;
import es.bsc.mobile.types.JobExecutionMonitor;
import es.bsc.mobile.types.JobParameter;
import es.bsc.mobile.types.Kernel;
import es.bsc.mobile.types.Method;
import es.bsc.mobile.types.comm.Node;
import java.util.LinkedList;


public class BackEndProxyInterface implements ComputingPlatformBackend {

    private final String name;
    private final DataProvider data;
    private LinkedList<Implementation>[] compatibleImpls;
    private final boolean proxiedManagement;

    public BackEndProxyInterface(String platformName, DataProvider data, boolean proxiedManagement) {
        this.name = platformName;
        this.data = data;
        compatibleImpls = new LinkedList[0];
        this.proxiedManagement = proxiedManagement;
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
            if (name.compareTo("GPU") == 0) {
                if (impl instanceof Kernel) {
                    this.compatibleImpls[coreId].add(impl);
                }
            } else if (name.compareTo("CPU") == 0) {
                if (impl instanceof Method) {
                    this.compatibleImpls[coreId].add(impl);
                }
            }
        }
    }

    @Override
    public void updateCoreElement(int coreId, LinkedList<Implementation> internalImpls) {
        this.compatibleImpls[coreId] = internalImpls;
    }

    @Override
    public LinkedList<Implementation> getCompatibleImplementations(int coreId) {
        return compatibleImpls[coreId];
    }

    @Override
    public JobExecution newJobExecution(Job job, JobExecutionMonitor orchestrator) {
        if (proxiedManagement) {
            return new ProxiedJobExecutionAndManagement(job, orchestrator);
        } else {
            return new ProxiedJobExecution(job, orchestrator);
        }
    }

    @Override
    public DataManager.DataStatus getDataStatus(String dataId) {
        return data.getDataStatus(dataId);
    }


    private class ProxiedJobExecution extends JobExecution {

        public ProxiedJobExecution(Job job, JobExecutionMonitor jobMonitor) {
            super(job, jobMonitor);
            ProxiedBackEndNotification.registerProxiedJobExecution(job.getId(), this);
            CommunicationManager.notifyCommand(new Node(null, 28000), new JobExecutionRequest(name, job, false));
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
        public LinkedList<Implementation> getCompatibleImplementations() {
            return BackEndProxyInterface.this.getCompatibleImplementations(getJob().getImplementations()[0].getCoreElementId());
        }

        @Override
        public void obtainJobInputDataDependencies() throws UnexpectedDirectionException {
            JobParameter[] params = getJob().getParams();
            for (int paramId = 0; paramId < params.length; paramId++) {
                JobParameter jp = params[paramId];
                if ((jp.getType() == Type.OBJECT || jp.getType() == Type.FILE)
                        && (jp.getDirection() != Direction.IN
                        && jp.getDirection() != Direction.OUT
                        && jp.getDirection() != Direction.INOUT)) {
                    throw new UnexpectedDirectionException(UNEXPECTED_DIRECTION + jp.getDirection() + FOR_PARAM + paramId);
                }
            }

            JobParameter target = getJob().getTarget();
            if (target != null && (target.getDirection() == Direction.IN || target.getDirection() == Direction.INOUT)) {
                throw new UnexpectedDirectionException(UNEXPECTED_DIRECTION + target.getDirection() + FOR_TARGET + FULL_STOP);
            }
            CommunicationManager.notifyCommand(new Node(null, 28000), new ObtainJobInputDependenciesRequest(this.getJob().getId()));
        }

        @Override
        public void obtainDataAsObject(String dataId, String dataRenaming, int paramId, LoadParamData listener) {
            CommunicationManager.notifyCommand(new Node(null, 28000), new ObtainDataAsObjectRequest(dataId, dataRenaming, this.getJob().getId(), paramId));
        }

        @Override
        public void obtainDataAsFile(String dataId, String dataRenaming, int paramId, LoadParamData listener) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void allDataPresent() {
            this.notifyJobExecutionAllDataPresent();
        }

        @Override
        public void prepareJobInputDataDependencies() {
            CommunicationManager.notifyCommand(new Node(null, 28000), new PrepareJobInputDependenciesRequest(this.getJob().getId()));
        }

        @Override
        public void prepareJobParameter(int paramId) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void prepareTargetObject() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void prepareResult() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void allDataReady() {
            this.notifyJobExecutionAllDataReady();
        }

        @Override
        public void executeOn(Object id, Implementation impl) {
            CommunicationManager.notifyCommand(new Node(null, 28000), new RunJobExecutionRequest(this.getJob().getId(), id, impl.getImplementationId()));
        }

        @Override
        public void failedExecution() {
            this.notifyJobExecutionFailed();
        }

        @Override
        public void finishedExecution() {
            this.notifyJobExecutionFinished();
        }

        @Override
        public void storeJobOutputDataDependencies() {
            //Automatically done for all
        }

        @Override
        protected void storeObject(String dataId, Object value, DataManager.DataOperationListener listener) {
            //Automatically done for all
        }

        @Override
        protected void storeFile(String dataId, String location, DataManager.DataOperationListener listener) {
            //Automatically done for all
        }

        @Override
        public void obtainDataSize(String dataId, int paramId, DataManager.DataOperationListener listener) {
            //SET AT JOB COMPLETION
        }

        @Override
        public void completed() {
            notifyJobExecutionCompletion();
        }
    }


    private class ProxiedJobExecutionAndManagement extends JobExecution {

        public ProxiedJobExecutionAndManagement(Job job, JobExecutionMonitor jobMonitor) {
            super(job, jobMonitor);
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
            ProxiedBackEndNotification.registerProxiedJobExecution(getJob().getId(), this);
            CommunicationManager.notifyCommand(new Node(null, 28000), new JobExecutionRequest(name, getJob(), true));
        }

        @Override
        public LinkedList<Implementation> getCompatibleImplementations() {
            return BackEndProxyInterface.this.getCompatibleImplementations(getJob().getImplementations()[0].getCoreElementId());
        }

        @Override
        public void obtainJobInputDataDependencies() throws UnexpectedDirectionException {
            //All data is considered present on the proxy node
            this.notifyJobExecutionAllDataPresent();
        }

        @Override
        public void obtainDataAsObject(String dataId, String dataRenaming, int paramId, LoadParamData listener) {
            //Automatically done
            listener.setValue(null, null);
        }

        @Override
        public void obtainDataAsFile(String dataId, String dataRenaming, int paramId, LoadParamData listener) {
            //Automatically done
            listener.setValue(null, null);
        }

        @Override
        public void allDataPresent() {
            this.notifyJobExecutionAllDataPresent();
        }

        @Override
        public void prepareJobInputDataDependencies() {
            //All data is considered ready on the proxy node
            this.allDataReady();
        }

        @Override
        public void prepareJobParameter(int paramId) {
            //Parameter is prepared on proxy
        }

        @Override
        public void prepareTargetObject() {
            //Target is prepared on proxy
        }

        @Override
        public void prepareResult() {
            //Result
        }

        @Override
        public void allDataReady() {
            this.notifyJobExecutionAllDataReady();
        }

        @Override
        public void executeOn(Object id, Implementation impl) {
            Job job = getJob();
            CommunicationManager.notifyCommand(new Node(null, 28000), new RunJobExecutionRequest(job.getId(), id, impl.getImplementationId()));
        }

        @Override
        public void failedExecution() {
            this.notifyJobExecutionFailed();
        }

        @Override
        public void finishedExecution() {
            this.notifyJobExecutionFinished();
        }

        @Override
        public void storeJobOutputDataDependencies() {
            //Automatically done for all
        }

        @Override
        protected void storeObject(String dataId, Object value, DataManager.DataOperationListener listener) {
            //Automatically done for all
        }

        @Override
        protected void storeFile(String dataId, String location, DataManager.DataOperationListener listener) {
            //Automatically done for all
        }

        @Override
        public void obtainDataSize(String dataId, int paramId, DataManager.DataOperationListener listener) {
            //SET AT JOB COMPLETION
        }

        @Override
        public void completed() {
            notifyJobExecutionCompletion();
        }
    }
}
