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
package es.bsc.mobile.runtime.types.resources.proxy.requests;

import es.bsc.mobile.runtime.types.resources.proxy.notifications.AllValuesReadyNotification;
import es.bsc.mobile.comm.CommunicationManager;
import es.bsc.mobile.exceptions.UnexpectedDirectionException;
import es.bsc.mobile.runtime.types.resources.ComputingPlatformBackend;
import es.bsc.mobile.runtime.types.resources.proxy.ProxiedJobExecutionController;
import es.bsc.mobile.runtime.types.resources.proxy.notifications.AllValuesObtainedNotification;
import es.bsc.mobile.runtime.types.resources.proxy.notifications.CompletedJobNotification;
import es.bsc.mobile.runtime.types.resources.proxy.notifications.FinishedJobNotification;
import es.bsc.mobile.types.Implementation;
import es.bsc.mobile.types.Job;
import es.bsc.mobile.types.JobExecution;
import es.bsc.mobile.types.JobParameter;
import es.bsc.mobile.types.JobProfile;
import es.bsc.mobile.types.comm.Node;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;


public class JobExecutionRequest implements ProxiedBackEndRequest, Externalizable {

    private String platform;
    private Job job;
    private boolean proxiedManagement;

    public JobExecutionRequest() {
    }

    public JobExecutionRequest(String platform, Job job, boolean proxiedManagement) {
        this.platform = platform;
        this.job = job;
        this.proxiedManagement = proxiedManagement;
    }

    @Override
    public void writeExternal(ObjectOutput oo) throws IOException {
        oo.writeUTF(platform);
        oo.writeObject(job);
        oo.writeBoolean(proxiedManagement);
    }

    @Override
    public void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
        platform = oi.readUTF();
        job = (Job) oi.readObject();
        proxiedManagement = oi.readBoolean();
    }

    @Override
    public void process(
            HashMap<String, ComputingPlatformBackend> platforms,
            HashMap<String, ProxiedJobExecutionController> jobExecutions,
            HashMap<String, LinkedList<ProxiedBackEndRequest>> pendingRequests
    ) {
        ComputingPlatformBackend realBackEnd = platforms.get(platform);
        String jobId = job.getId();
        job.startProfiling();
        ProxiedJobExecutionController monitor;
        if (proxiedManagement) {
            monitor = new MonitorAndManager(realBackEnd, job);
        } else {
            monitor = new Monitor(realBackEnd, job);
        }

        jobExecutions.put(jobId, monitor);
        LinkedList<ProxiedBackEndRequest> pendingReqs = pendingRequests.get(jobId);
        if (pendingReqs != null) {
            for (ProxiedBackEndRequest pber : pendingReqs) {
                pber.process(platforms, jobExecutions, pendingRequests);
            }
        }
    }


    private static class Monitor implements ProxiedJobExecutionController {

        protected final JobExecution je;
        protected static final LinkedBlockingQueue<Monitor> pendingStore = new LinkedBlockingQueue<Monitor>();

        static {
            new Thread() {
                @Override
                public void run() {
                    Monitor mon;
                    try {
                        while ((mon = pendingStore.take()) != null) {
                            mon.je.storeJobOutputDataDependencies();
                        }
                    } catch (InterruptedException ie) {
                    }
                }
            }.start();
        }

        private Monitor(ComputingPlatformBackend backend, Job job) {
            je = backend.newJobExecution(job, this);
        }

        @Override
        public LinkedList<Implementation> getCompatibleImplementations() {
            return je.getCompatibleImplementations();
        }

        @Override
        public void notifyParamValueExistence(JobExecution je, JobParameter jp) {
        }

        @Override
        public void dependencyFreeJob(JobExecution je) {
        }

        @Override
        public void obtainJobInputDataDependencies() throws UnexpectedDirectionException {
            je.obtainJobInputDataDependencies();
        }

        @Override
        public void obtainDataAsObject(String dataId, String dataRenaming, int paramId) {
            je.obtainDataAsFile(dataId, dataRenaming, paramId);
        }

        @Override
        public void allValuesObtained(JobExecution je) {
            CommunicationManager.notifyCommand(new Node(null, 43000), new AllValuesObtainedNotification(je.getJob().getId()));
        }

        @Override
        public void prepareJobInputDataDependencies() {
            je.prepareJobInputDataDependencies();
        }

        @Override
        public void allValuesReady(JobExecution je) {
            CommunicationManager.notifyCommand(new Node(null, 43000), new AllValuesReadyNotification(je.getJob().getId()));
        }

        @Override
        public void executeOn(Object executorId, Implementation impl) {
            je.executeOn(executorId, impl);
        }

        @Override
        public void executedJob(JobExecution je) {
            CommunicationManager.notifyCommand(new Node(null, 43000), new FinishedJobNotification(je.getJob().getId()));
            storeJobOutputDataDependencies();
        }

        @Override
        public void storeJobOutputDataDependencies() {
            pendingStore.offer(this);
        }

        @Override
        public void completedJob(JobExecution je) {
            JobProfile jp = je.getJob().getJobProfile();
            CommunicationManager.notifyCommand(new Node(null, 43000), new CompletedJobNotification(je.getJob().getId(), jp));
        }

    }


    private static class MonitorAndManager extends Monitor {

        private boolean readyValues;
        private Object executorId;
        private Implementation impl;

        private MonitorAndManager(ComputingPlatformBackend backEnd, Job job) {
            super(backEnd, job);
            readyValues = false;
            executorId = null;
            impl = null;
            dependencyFreeJob(je);
        }

        @Override
        public void dependencyFreeJob(JobExecution je) {
            try {
                je.obtainJobInputDataDependencies();
            } catch (UnexpectedDirectionException ude) {
                //Should be checked on proxy interface
            }
        }

        @Override
        public void allValuesObtained(JobExecution je) {
            je.prepareJobInputDataDependencies();
        }

        @Override
        public void allValuesReady(JobExecution je) {
            boolean executeArrived;
            synchronized (this) {
                readyValues = true;
                executeArrived = impl != null;
            }
            if (executeArrived) {
                je.executeOn(executorId, impl);
            }
        }

        @Override
        public void executeOn(Object executorId, Implementation impl) {
            this.executorId = executorId;
            boolean ready = false;
            synchronized (this) {
                this.impl = impl;
                ready = readyValues;
            }
            if (ready) {
                je.executeOn(executorId, impl);
            }
        }

    }

}
