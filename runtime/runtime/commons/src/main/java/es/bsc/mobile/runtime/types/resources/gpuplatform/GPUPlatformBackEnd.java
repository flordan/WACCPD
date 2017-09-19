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
package es.bsc.mobile.runtime.types.resources.gpuplatform;

import es.bsc.mobile.data.DataManager;
import es.bsc.mobile.data.DataProvider;
import es.bsc.mobile.data.access.RAccess;
import es.bsc.mobile.data.access.RWAccess;
import es.bsc.mobile.data.access.WAccess;
import es.bsc.mobile.runtime.types.resources.ComputingPlatformBackend;
import es.bsc.mobile.runtime.types.resources.Resource;
import es.bsc.mobile.types.Implementation;
import es.bsc.mobile.types.Job;
import es.bsc.mobile.types.JobExecution;
import es.bsc.mobile.types.JobExecutionMonitor;
import es.bsc.mobile.types.JobParameter;
import es.bsc.mobile.types.JobParameter.ObjectJobParameter;
import es.bsc.mobile.types.Kernel;
import es.bsc.mobile.utils.Expression;
import es.bsc.opencl.wrapper.Device;
import java.lang.reflect.Array;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;


public class GPUPlatformBackEnd implements ComputingPlatformBackend {

    private final static long POWER_CONSUMPTION = 2500l;

    private final DataProvider data;
    private LinkedList<Implementation>[] compatibleImpls;

    private Device device;
    private final ValueFetcher vf = new ValueFetcher();
    private final TreeMap<String, OnCreationDataInformation> onCreationData = new TreeMap<String, OnCreationDataInformation>();

    private int tokens = 2;
    private final LinkedList<Object[]> pendingExecutions = new LinkedList<Object[]>();

    public GPUPlatformBackEnd(DataProvider localDataProvider) {
        data = localDataProvider;
        compatibleImpls = new LinkedList[0];
        new Thread(vf).start();
    }

    @Override
    public void addResource(Resource res) {
        device = ((OpenCLResource) res).getDevice();
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
        try {
            Kernel ocl = (Kernel) impl;
            if (device.addProgramFromSource(ocl.getProgram(), ocl.getSourceCode()) == null) {
                return false;
            }
        } catch (ClassCastException cce) {
            return false;
        }
        return true;
    }

    @Override
    public LinkedList<Implementation> getCompatibleImplementations(int coreId) {
        return compatibleImpls[coreId];
    }

    @Override
    public JobExecution newJobExecution(Job job, JobExecutionMonitor orchestrator) {
        return new GPUJobExecution(job, orchestrator);
    }

    @Override
    public DataManager.DataStatus getDataStatus(String dataId) {
        return data.getDataStatus(dataId);
    }


    private class ValueFetcher implements Runnable {

        LinkedBlockingQueue<GPUJobExecution> queue = new LinkedBlockingQueue<GPUJobExecution>();

        @Override
        public void run() {
            while (true) {
                try {
                    GPUJobExecution gje = queue.take();
                    for (int i = 0; i < gje.getJob().getParams().length; i++) {
                        gje.fetchParamValue(i);
                    }
                } catch (InterruptedException ex) {

                }
            }
        }

        public void fetch(GPUJobExecution job) {
            queue.offer(job);
        }
    }


    private class OnCreationDataInformation {

        public boolean issued = false;
        public LinkedList<JobExecution.LoadParamData> listeners = new LinkedList<JobExecution.LoadParamData>();
    }


    private class GPUJobExecution extends JobExecution {

        private class ParamInfo {

            final String inData;
            final String outData;
            final boolean reads;
            final boolean writes;
            boolean bypassed;

            public ParamInfo() {
                inData = "";
                outData = "";
                reads = true;
                writes = false;
                bypassed = false;
            }

            public ParamInfo(String inData, String outData, boolean bypassed) {
                this.inData = inData;
                this.reads = (inData != null);
                this.outData = outData;
                this.writes = (outData != null);
                this.bypassed = bypassed;
            }

        }
        private final ParamInfo[] paramsInfo;
        private final boolean[] updates;

        private final Object[] parValues;

        private Class<?> resultType;
        private int[] resultSizes;
        private final AtomicInteger pendingPrepares;
        private final AtomicInteger pendingResults;

        public GPUJobExecution(Job job, JobExecutionMonitor orchestrator) {
            super(job, orchestrator);
            int paramCount = job.getParamValues().length;

            if (job.getResult() != null) {
                paramCount++;
            }

            updates = new boolean[paramCount];

            parValues = new Object[paramCount];
            paramsInfo = new ParamInfo[paramCount];
            this.pendingPrepares = new AtomicInteger(paramCount);
            this.pendingResults = new AtomicInteger(paramCount);

            int paramId = 0;
            final JobParameter[] params = job.getParams();
            for (; paramId < params.length; paramId++) {
                JobParameter jp = params[paramId];
                switch (jp.getType()) {
                    case OBJECT:
                        ObjectJobParameter op = (ObjectJobParameter) jp;
                        String inDataId;
                        String outDataId;
                        switch (op.getDirection()) {
                            case IN:
                                RAccess read = (RAccess) op.getDataAccess();
                                inDataId = read.getReadDataInstance();
                                paramsInfo[paramId] = new ParamInfo(read.getReadDataInstance(), null, onCreationData.containsKey(inDataId));
                                break;
                            case INOUT:
                                RWAccess readWrite = (RWAccess) op.getDataAccess();
                                inDataId = readWrite.getReadDataInstance();
                                outDataId = readWrite.getWrittenDataInstance();
                                paramsInfo[paramId] = new ParamInfo(readWrite.getReadDataInstance(), outDataId, onCreationData.containsKey(inDataId));
                                onCreationData.put(outDataId, new OnCreationDataInformation());
                                break;
                            default: //case OUT:
                                WAccess write = (WAccess) op.getDataAccess();
                                outDataId = write.getWrittenDataInstance();
                                paramsInfo[paramId] = new ParamInfo(null, outDataId, false);
                                onCreationData.put(outDataId, new OnCreationDataInformation());
                        }
                        break;
                    default:
                }
            }
            if (job.getResult() != null) {
                ObjectJobParameter op = (ObjectJobParameter) job.getResult();
                String dataId = ((WAccess) op.getDataAccess()).getWrittenDataInstance();
                paramsInfo[paramsInfo.length - 1] = new ParamInfo(null, dataId, false);
                onCreationData.put(dataId, new OnCreationDataInformation());
            }
        }

        @Override
        protected void requestParamDataExistence(String dataId, int paramId, DataManager.DataExistenceListener listener) {
            if (onCreationData.containsKey(dataId)) {
                paramsInfo[paramId].bypassed = true;
                listener.exists();
            } else {
                paramsInfo[paramId].bypassed = false;
                data.requestDataExistence(dataId, listener);
            }
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
        public LinkedList<es.bsc.mobile.types.Implementation> getCompatibleImplementations() {
            return GPUPlatformBackEnd.this.getCompatibleImplementations(getJob().getImplementations()[0].getCoreElementId());
        }

        @Override
        public void obtainDataSize(String dataId, int paramId, DataManager.DataOperationListener listener) {
            data.obtainDataSize(dataId, listener);
        }

        @Override
        public void obtainDataAsObject(String dataId, String dataRenaming, int paramId, JobExecution.LoadParamData listener) {
            if (!paramsInfo[paramId].bypassed) {
                data.obtainDataAsObject(dataId, dataRenaming, listener);
            } else {
                OnCreationDataInformation dataInfo = onCreationData.get(dataId);
                if (!dataInfo.issued) {
                    LinkedList<LoadParamData> pendingPrepares = dataInfo.listeners;
                    pendingPrepares.add(listener);
                } else {
                    listener.skipLoadValue();
                }
            }
        }

        @Override
        public void obtainDataAsFile(String dataId, String dataRenaming, int paramId, JobExecution.LoadParamData listener) {
            data.obtainDataAsFile(dataId, dataRenaming, listener);
        }

        @Override
        public void allDataPresent() {
            this.notifyJobExecutionAllDataPresent();
        }

        @Override
        public void prepareJobParameter(int paramId) {
            JobParameter jp = getJob().getParams()[paramId];
            PrepareListener prepListenner = new PrepareListener(paramId);
            switch (jp.getType()) {
                case OBJECT:
                    ObjectJobParameter op = (ObjectJobParameter) jp;
                    boolean copyIn = true;
                    boolean canWrite = true;
                    String valueRenaming;
                    switch (op.getDirection()) {
                        case IN:
                            canWrite = false;
                            valueRenaming = ((RAccess) op.getDataAccess()).getReadDataInstance();
                            break;
                        case INOUT:
                            valueRenaming = ((RWAccess) op.getDataAccess()).getWrittenDataInstance();
                            updates[paramId] = true;
                            break;
                        default: //case OUT:
                            copyIn = false;
                            valueRenaming = ((WAccess) op.getDataAccess()).getWrittenDataInstance();
                            updates[paramId] = true;
                    }
                    Object o = getJob().getParamValues()[paramId];
                    int size = 1;
                    Class<?> oClass = null;
                    if (o != null) {
                        oClass = o.getClass();
                        Object sizeObject = o;
                        while (oClass.isArray()) {
                            size *= Array.getLength(sizeObject);
                            sizeObject = Array.get(sizeObject, 0);
                            oClass = oClass.getComponentType();
                        }
                    }
                    device.prepareValueForKernel(prepListenner, valueRenaming, o, oClass, size, copyIn, canWrite);
                    break;
                default:
                    prepListenner.completed(getJob().getParamValues()[paramId]);
            }
        }

        @Override
        public void prepareTargetObject() {
            //For OpenCL there is no target object
        }

        @Override
        public void prepareResult() {
            ObjectJobParameter op = (ObjectJobParameter) getJob().getResult();
            if (op != null) {
                updates[parValues.length - 1] = true;
                PrepareListener prepListenner = new PrepareListener(parValues.length - 1);
                String valueRenaming = ((WAccess) op.getDataAccess()).getWrittenDataInstance();
                Kernel k = (Kernel) this.getCompatibleImplementations().getFirst();
                Expression[] resultSizeExpressions = k.getResultSizeExpressions();
                resultType = k.getResultType();
                int resultDims = resultSizeExpressions.length;
                resultSizes = new int[resultDims];
                int resultSize = 1;
                for (int resultDim = 0; resultDim < resultDims; resultDim++) {
                    resultSizes[resultDim] = resultSizeExpressions[resultDim].evaluate(getJob().getParamValues());
                    resultSize *= resultSizes[resultDim];
                }
                device.prepareValueForKernel(prepListenner, valueRenaming, null, resultType, resultSize, false, true);
            }
        }


        private class PrepareListener extends Device.OpenCLMemoryPrepare {

            private final int paramId;

            private PrepareListener(int paramId) {
                this.paramId = paramId;
            }

            @Override
            public void completed(Object paramValue) {
                parValues[paramId] = paramValue;
                int pending = pendingPrepares.decrementAndGet();
                if (pending == 0) {
                    allDataReady();
                }
            }

            @Override
            public void failed() {
            }
        }

        @Override
        public void allDataReady() {
            this.notifyJobExecutionAllDataReady();
        }

        @Override
        public void executeOn(Object id, Implementation impl) {
            Job job = getJob();
            job.selectImplementation(impl);
            Kernel k = ((Kernel) job.getSelectedImplementation());
            String programName = k.getProgram();
            String methodName = k.getMethodName();
            Expression[] workSizeExpressions = k.getWorkloadExpressions();
            Expression[] localSizeExpressions = k.getLocalSizeExpressions();
            Expression[] offsetExpressions = k.getOffsetExpressions();
            int workSizeDims = workSizeExpressions.length;
            long[] globalWorkSize = new long[workSizeDims];
            long[] offset = new long[workSizeDims];
            long[] localSize = new long[localSizeExpressions.length];
            for (int i = 0; i < workSizeDims; i++) {
                globalWorkSize[i] = workSizeExpressions[i].evaluate(getJob().getParamValues());
                offset[i] = offsetExpressions[i].evaluate(getJob().getParamValues());
                if (localSizeExpressions.length != 0) {
                    localSize[i] = localSizeExpressions[i].evaluate(getJob().getParamValues());
                }
            }
            boolean run = false;
            synchronized (GPUPlatformBackEnd.this) {
                if (GPUPlatformBackEnd.this.tokens == 0) {
                    Device.OpenCLExecutionListener list = new ExecutionListenner();
                    RetrieveListener resultListener = new RetrieveListener(-1);
                    Object[] pendingExecution = new Object[]{this, programName, methodName, updates, parValues, globalWorkSize, localSize, offset, list, resultListener};
                    GPUPlatformBackEnd.this.pendingExecutions.add(pendingExecution);
                } else {
                    GPUPlatformBackEnd.this.tokens--;
                    run = true;
                }
            }
            if (run) {
                device.runKernel(programName, methodName, updates, parValues, globalWorkSize, localSize, offset, new ExecutionListenner());
                for (int paramId = 0; paramId < paramsInfo.length; paramId++) {
                    if (paramsInfo[paramId] != null && paramsInfo[paramId].writes) {
                        OnCreationDataInformation dataInfo = onCreationData.get(paramsInfo[paramId].outData);
                        dataInfo.issued = true;
                        LinkedList<LoadParamData> waiting = dataInfo.listeners;
                        for (LoadParamData listener : waiting) {
                            listener.skipLoadValue();
                        }
                    }
                }
                device.collectValueFromKernel(new RetrieveListener(-1), parValues[parValues.length - 1], resultType, resultSizes);
            }
        }


        private class RetrieveListener extends Device.OpenCLMemoryRetrieve {

            private final int paramId;

            public RetrieveListener(int paramId) {
                this.paramId = paramId;
            }

            public void completed(Object value) {
                if (paramId == -1) {
                    getJob().setResultValue(value);
                } else {
                }
                int pending = pendingResults.decrementAndGet();
                if (pending == 0) {
                    finishedExecution();
                }
            }

            public void failed() {
            }
        }


        private class ExecutionListenner extends Device.OpenCLExecutionListener {

            @Override
            public void completed(long start, long end) {
                long length = end - start;
                long startTime = System.currentTimeMillis() * 1000000 - length;
                getJob().startExecution(startTime);
                getJob().endsExecution();
                getJob().setConsumption(POWER_CONSUMPTION * length);
                vf.fetch(GPUJobExecution.this);
            }

            @Override
            public void failed() {
                failedExecution();
            }
        }

        @Override
        public void failedExecution() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void finishedExecution() {
            boolean run = true;
            Object[] pe;
            synchronized (GPUPlatformBackEnd.this) {
                pe = GPUPlatformBackEnd.this.pendingExecutions.pollFirst();
                if (pe == null) {
                    GPUPlatformBackEnd.this.tokens++;
                    run = false;
                }
            }
            if (run) {
                GPUJobExecution gpe = (GPUJobExecution) pe[0];
                device.runKernel((String) pe[1], (String) pe[2], (boolean[]) pe[3], (Object[]) pe[4], (long[]) pe[5], (long[]) pe[6], (long[]) pe[7], (Device.OpenCLExecutionListener) pe[8]);
                for (int paramId = 0; paramId < gpe.paramsInfo.length; paramId++) {
                    ParamInfo pi = gpe.paramsInfo[paramId];
                    if (pi != null && pi.writes) {
                        OnCreationDataInformation dataInfo = onCreationData.get(pi.outData);
                        dataInfo.issued = true;
                        LinkedList<LoadParamData> waiting = dataInfo.listeners;
                        for (LoadParamData listener : waiting) {
                            listener.skipLoadValue();
                        }
                    }
                }
                device.collectValueFromKernel((RetrieveListener) pe[9], gpe.parValues[gpe.parValues.length - 1], gpe.resultType, gpe.resultSizes);
            }
            this.notifyJobExecutionFinished();
        }

        private void fetchParamValue(int paramId) {
            JobParameter jp = getJob().getParams()[paramId];
            switch (jp.getType()) {
                case OBJECT:
                    Object o = getJob().getParamValues()[paramId];
                    device.collectValueFromKernel(parValues[paramId], o, updates[paramId]);
                    break;
                default:
            }
            int pending = pendingResults.decrementAndGet();
            if (pending == 0) {
                finishedExecution();
            }
        }

        @Override
        protected void storeObject(String dataId, Object value, DataManager.DataOperationListener listener) {
            OnCreationDataInformation dataInfo = onCreationData.remove(dataId);
            data.storeObject(dataId, value, listener);
            for (LoadParamData lpd : dataInfo.listeners) {
                data.obtainDataSize(dataId, lpd);
            }
        }

        @Override
        protected void storeFile(String dataId, String location, DataManager.DataOperationListener listener) {
            data.storeFile(dataId, location, listener);
        }

        @Override
        public void completed() {
            long length = this.getJob().getJobProfile().getExecutionTime();
            this.getJob().setConsumption(POWER_CONSUMPTION * length);
            this.notifyJobExecutionCompletion();
        }

    }

}
