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
package es.bsc.mobile.runtime;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.StrictMode;
import android.util.Log;

import es.bsc.comm.CommException;
import es.bsc.comm.Connection;
import es.bsc.comm.MessageHandler;
import es.bsc.mobile.comm.CommunicationManager;
import es.bsc.comm.nio.NIOConnection;
import es.bsc.comm.nio.NIONode;
import es.bsc.comm.stage.Transfer;
import es.bsc.mobile.annotations.Parameter.Direction;
import es.bsc.mobile.annotations.Parameter.Type;
import es.bsc.mobile.comm.CommunicationManager;
import es.bsc.mobile.data.DataManager;
import es.bsc.mobile.data.DataManager.DataOperationListener;
import es.bsc.mobile.data.DataManagerImpl;
import es.bsc.mobile.data.DataManagerImpl.DMBackBone;
import es.bsc.mobile.data.DataProvider;
import es.bsc.mobile.runtime.service.RuntimeServiceItf;
import es.bsc.mobile.runtime.types.CEI;
import es.bsc.mobile.runtime.service.RuntimeService;
import es.bsc.mobile.runtime.types.data.DataInfo;
import es.bsc.mobile.runtime.types.data.DataInstance;
import es.bsc.mobile.runtime.types.data.access.DataAccess;
import es.bsc.mobile.runtime.types.data.access.DataAccess.Action;
import es.bsc.mobile.runtime.types.data.access.ReadAccess;
import es.bsc.mobile.runtime.types.data.access.ReadWriteAccess;
import es.bsc.mobile.runtime.types.data.parameter.BasicTypeParameter;
import es.bsc.mobile.runtime.types.data.parameter.FileParameter;
import es.bsc.mobile.runtime.types.data.parameter.Parameter;
import es.bsc.mobile.runtime.types.data.parameter.RegisteredParameter;
import es.bsc.mobile.runtime.types.resources.ComputingPlatformBackend;
import es.bsc.mobile.runtime.types.resources.cpuplatform.CPUPlatformBackEnd;
import es.bsc.mobile.runtime.types.resources.gpuplatform.GPUPlatformBackEnd;
import es.bsc.mobile.runtime.types.resources.gpuplatform.OpenCLResource;
import es.bsc.mobile.runtime.types.resources.proxy.BackEndProxyImplementation;
import es.bsc.mobile.runtime.types.Task;
import es.bsc.mobile.runtime.utils.CoreManager;
import es.bsc.mobile.runtime.utils.DataRegistry;
import es.bsc.mobile.types.comm.Node;
import es.bsc.mobile.types.messages.runtime.DataExistenceNotification;
import es.bsc.mobile.types.messages.runtime.DataSourceResponse;
import es.bsc.mobile.types.messages.runtime.DataTransferRequest;
import java.io.IOException;
import java.net.URISyntaxException;

import java.util.HashMap;
import java.util.LinkedList;
import es.bsc.mobile.runtime.types.resources.proxy.requests.ProxiedBackEndRequest;


public class Runtime implements MessageHandler {

    public static final int ARGUMENTS_PER_PARAMETER = 3;
    public static final int ARGUMENT_TYPE_OFFSET = 0;
    public static final int ARGUMENT_DIRECTION_OFFSET = 1;
    public static final int ARGUMENT_VALUE_OFFSET = 2;

    private static final String LOGGER_TAG = "Runtime";
    private static final Node ME = new Node(null, 28000);

    private static RuntimeServiceItf RUNTIME_SERVICE;
    private static final DataManager DM = new DataManagerImpl(new DMBackBone() {
        @Override
        public boolean askDataExistence(String data) {
            try {
                RUNTIME_SERVICE.requestDataExistence(data, ME);
            } catch (RemoteException re) {
                Log.e(LOGGER_TAG, "Exception requesting data existence to service");
            }
            return false;
        }

        @Override
        public void announceDataPresence(String data) {
            try {
                RUNTIME_SERVICE.notifyDataCreation(data, ME);
            } catch (RemoteException re) {
                Log.e(LOGGER_TAG, "Exception announcing data presence to service");
            }
        }

        @Override
        public void requestDataAsObject(String dataId) {
            try {
                RUNTIME_SERVICE.requestDataLocations(dataId, ME);
            } catch (RemoteException re) {
                Log.e(LOGGER_TAG, "Exception requesting data locations for object " + dataId + " to service");
            }
        }

        @Override
        public void requestDataAsFile(String dataId, String targetLocation) {
            try {
                RUNTIME_SERVICE.requestDataLocations(dataId, ME);
            } catch (RemoteException re) {
                Log.e(LOGGER_TAG, "Exception requesting data locations for file " + dataId + " to service");
            }
        }
    });

    private static final DataRegistry<Integer> PRIVATE_REG = new DataRegistry<Integer>();
    private static final HashMap<Integer, Object> ID_TO_REPRESENTATIVE = new HashMap<Integer, Object>();

    private static CEI cei;

    private static final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RUNTIME_SERVICE = RuntimeServiceItf.Stub.asInterface(service);
            try {
                String[] signatures = cei.getAllSignatures();
                int[] coreIds = RUNTIME_SERVICE.getCoreIds(signatures);
                int implOffset = cei.getCoreCount();
                for (int i = 0; i < cei.getCoreCount(); i++) {
                    LinkedList<es.bsc.mobile.runtime.types.Implementation> impls = cei.getCoreImplementations(i);
                    String[] implsSignatures = new String[impls.size()];
                    for (int implId = 0; implId < impls.size(); implId++) {
                        implsSignatures[implId] = signatures[implOffset + implId];
                    }
                    implOffset += impls.size();
                    CoreManager.registerCore(coreIds[i], signatures[i], impls, implsSignatures);
                }
                //FALTA CONFIGURACIO DE LES PLATAFORMES
                ComputingPlatformBackend cpuBackend = new CPUPlatformBackEnd(4, DATA_PROVIDER);
                ComputingPlatformBackend gpuBackend = new GPUPlatformBackEnd(DATA_PROVIDER);
                gpuBackend.addResource(new OpenCLResource(2, "QUALCOMM Snapdragon(TM)", "QUALCOMM Adreno(TM)"));
                for (int coreId = 0; coreId < CoreManager.getCoreCount(); coreId++) {
                    LinkedList<es.bsc.mobile.types.Implementation> internalImpls = new LinkedList<es.bsc.mobile.types.Implementation>();
                    for (es.bsc.mobile.runtime.types.Implementation impl : CoreManager.getCoreImplementations(coreId)) {
                        internalImpls.add(impl.getInternalImplementation());
                    }
                    cpuBackend.registerNewCoreElement(coreId, internalImpls);
                    gpuBackend.registerNewCoreElement(coreId, internalImpls);
                }
                PROXIED_BACKENDS.addProxiedBackend("CPU", cpuBackend);
                PROXIED_BACKENDS.addProxiedBackend("GPU", gpuBackend);
            } catch (RemoteException e) {
                Log.wtf(LOGGER_TAG, "Could not contact the service to get the Core Elements.");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            RUNTIME_SERVICE = null;
        }
    };

    private static final BackEndProxyImplementation PROXIED_BACKENDS = new BackEndProxyImplementation();

    private static final DataProvider DATA_PROVIDER = new DataProvider() {

        @Override
        public DataManager.DataStatus getDataStatus(String dataId) {
            return DM.getDataStatus(dataId);
        }

        @Override
        public void requestDataExistence(String dataId, DataManager.DataExistenceListener listener) {
            DM.requestDataExistence(dataId, listener);
        }

        @Override
        public void obtainDataSize(String dataId, DataOperationListener listener) {
            DM.getSize(dataId, listener);
        }

        @Override
        public void obtainDataAsObject(String dataId, String dataRenaming, DataOperationListener listener) {
            DM.retrieveObject(dataId, dataRenaming, listener);
        }

        @Override
        public void obtainDataAsFile(String dataId, String dataRenaming, DataOperationListener listener) {
            DM.retrieveFile(dataId, dataRenaming, listener);
        }

        @Override
        public void storeObject(String dataId, Object value, DataOperationListener listener) {
            DM.storeObject(dataId, value, listener);
        }

        @Override
        public void storeFile(String dataId, String location, DataOperationListener listener) {
            DM.storeFile(dataId, location, listener);
        }
    };

    private Runtime() {
    }

    /**
     * ****************************************************************************************************************
     * ****************************************************************************************************************
     * ********************************** ---------------------------------- ******************************************
     * ********************************** | INVOCATIONS FROM THE MAIN CODE | ******************************************
     * ********************************** ---------------------------------- ******************************************
     * ****************************************************************************************************************
     * ****************************************************************************************************************
     */
    /**
     * Initializes all the runtime structures and launches the threads.
     *
     */
    public static void startRuntime(Context context) {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        Configuration.load(context);
        CommunicationManager.start(new Runtime());
        try {
            if (cei == null) {
                cei = new CEI(context, Class.forName("CEI"));
                if (!CoreManager.isInitialized()) {
                    Intent i = new Intent(context, RuntimeService.class);
                    i.putExtra("CEI", cei);
                    context.startService(i);
                }
            }
        } catch (ClassNotFoundException e) {
            Log.e(LOGGER_TAG, "Core Element Interface not found. " + e.getMessage());
        }

        Intent i = new Intent(context, RuntimeService.class);
        context.bindService(i, mConnection, Context.BIND_AUTO_CREATE);
    }

    public static void unbindRuntime(Context context) {
        context.unbindService(mConnection);
    }

    /**
     * Registers an object access from the main code of the application.
     *
     * Should be invoked when any object is accessed.
     *
     * @param <T> Type of the object to be registered
     * @param o Accessed representative object
     * @param isWritter {true} if the access modifies the content of the object
     * @return The current object value
     */
    public static <T> T newObjectAccess(T o, boolean isWritter) {
        if (o == null) {
            return null;
        }
        int dataId = getDataId(o);
        DataInfo data = PRIVATE_REG.findData(dataId);
        if (data == null) {
            // Data is not in the system return same value
            return o;
        }
        DataAccess da = null;
        try {
            da = PRIVATE_REG.registerLocalDataAccess(isWritter ? Direction.INOUT : Direction.IN, data);
        } catch (IOException e) {
            Log.wtf(LOGGER_TAG, "Error obtaining object", e);
            System.exit(1);
        }
        ValueAccess<T> va = new ValueAccess<T>(data);
        if (da != null) {
            String inRenaming;
            String outRenaming;
            if (isWritter) {
                DataInstance inInstance = ((ReadWriteAccess) da).getReadDataInstance();
                DataInstance outInstance = ((ReadWriteAccess) da).getWrittenDataInstance();
                inRenaming = inInstance.getRenaming();
                outRenaming = outInstance.getRenaming();
            } else {
                inRenaming = ((ReadAccess) da).getReadDataInstance().getRenaming();
                outRenaming = inRenaming;
            }
            DM.retrieveObject(inRenaming, outRenaming, va);
        } else {
            Object localValue = data.getLocalValue();
            va.setValue(null, localValue);
        }
        try {
            return (T) va.getValue();
        } catch (Exception e) {
            Log.wtf(LOGGER_TAG, "Error obtaining object");
            System.exit(1);
        }
        return null;
    }


    private static class ValueAccess<T> implements DataManager.DataOperationListener {

        private boolean isValueSet = false;
        private T o = null;
        private final DataInfo data;

        public ValueAccess(DataInfo data) {
            this.data = data;
        }

        @Override
        public void paused() {
            //Does not matter
        }

        @Override
        public void setSize(long value) {
            //Does not matter
        }

        @Override
        public void setValue(Class<?> type, Object value) {
            o = (T) value;
            isValueSet = true;
            data.setLocalValue(value);
            synchronized (this) {
                this.notifyAll();
            }
        }

        public T getValue() {
            synchronized (this) {
                if (!isValueSet) {
                    try {
                        this.wait();
                    } catch (InterruptedException ie) {

                    }
                }
            }
            return o;
        }
    }

    /**
     * Registers an object access from the main code, obtains its value and removes any data stored in the runtime
     * related to it.
     *
     * This method should be called when a task returns a primitive value and this value is recovered by the runtime.
     *
     * @param <T> Type of the object value
     * @param o Accessed representative object
     * @return final value of the primitive value
     * @throws InterruptedException Interruption while the value is computed or brough back to the master.
     */
    public static <T> T getValueAndRemove(T o)
            throws InterruptedException {
        int dataId = getDataId(o);
        DataInfo data = PRIVATE_REG.deleteData(dataId);
        if (data == null) {
            // Data is not in the system return same value
            return null;
        }

        DataAccess da = null;
        try {
            da = PRIVATE_REG.registerLocalDataAccess(Direction.IN, data);
        } catch (IOException e) {
            Log.wtf(LOGGER_TAG, "Error obtaining object");
            System.exit(1);
        }
        DataInstance inInstance = ((ReadWriteAccess) da).getReadDataInstance();
        String inRenaming = inInstance.getRenaming();
        ValueAccess<T> va = new ValueAccess<T>(data);
        DM.retrieveObject(inRenaming, inRenaming, va);
        try {
            return va.getValue();
        } catch (Exception e) {
            Log.wtf(LOGGER_TAG, "Error obtaining object");
            System.exit(1);
        }
        return null;
    }

    /**
     * Generates a new task whose execution will be managed by the runtime.
     *
     * @param methodClass name of the class containing the method that has been invoked
     * @param methodName name of the invoked method
     * @param hasTarget the method has been invoked on a callee object
     * @param parameters parameter values
     * @return a unique identifier for the task
     */
    public static int executeTask(String methodClass, String methodName,
            boolean hasTarget, Object... parameters) {
        Parameter[] params = null;
        try {
            params = processParameters(parameters);
        } catch (IOException ex) {
            Log.e(LOGGER_TAG, "Error preparing object parameters for remote execution.", ex);
            System.exit(1);
        } catch (URISyntaxException ex) {
            Log.e(LOGGER_TAG, "Error preparing object parameters for remote execution.", ex);
            System.exit(1);
        }
        Task t = new Task(methodClass, methodName, hasTarget, params);
        try {
            RUNTIME_SERVICE.executeTask(t);
        } catch (RemoteException e) {
            Log.wtf(LOGGER_TAG, "Error submitting task " + t.getId() + "existence");
            System.exit(1);
        }
        return t.getId();
    }

    private static Parameter[] processParameters(Object[] parameters) throws URISyntaxException, IOException {
        int parameterCount = parameters.length / ARGUMENTS_PER_PARAMETER;
        Parameter[] params = new Parameter[parameterCount];
        for (int paramIdx = 0; paramIdx < parameterCount; paramIdx++) {
            Type type = (Type) parameters[paramIdx * ARGUMENTS_PER_PARAMETER + ARGUMENT_TYPE_OFFSET];
            Direction direction = (Direction) parameters[paramIdx * ARGUMENTS_PER_PARAMETER + ARGUMENT_DIRECTION_OFFSET];
            switch (type) {
                case OBJECT:
                    Object object = parameters[paramIdx * ARGUMENTS_PER_PARAMETER + ARGUMENT_VALUE_OFFSET];
                    int dataId = getDataId(object);
                    DataInfo dataInfo = PRIVATE_REG.findData(dataId);
                    if (dataInfo == null) {
                        ID_TO_REPRESENTATIVE.put(dataId, object);
                        dataInfo = PRIVATE_REG.registerData(dataId);
                    }
                    boolean areNewValues = dataInfo.getCurrentVersion() == null;
                    DataAccess da = PRIVATE_REG.registerRemoteDataAccess(direction, dataInfo);
                    if (areNewValues && da.getAction() != Action.WRITE) {
                        String renaming = dataInfo.getCurrentVersion().getDataInstance().getRenaming();
                        DM.storeObject(renaming, object, new IgnoreOperation());
                    }
                    params[paramIdx] = new RegisteredParameter(Type.OBJECT, direction, da);
                    break;
                case FILE:
                    params[paramIdx] = new FileParameter(direction, (String) parameters[paramIdx * ARGUMENTS_PER_PARAMETER + ARGUMENT_VALUE_OFFSET]);
                    break;
                default:
                    // Basic Type
                    params[paramIdx] = new BasicTypeParameter(type, Direction.IN, parameters[paramIdx * ARGUMENTS_PER_PARAMETER + ARGUMENT_VALUE_OFFSET]);
            }
        }
        return params;
    }

    /**
     * Indicates that an app does not generate any more tasks thus, all the internal objects can be deleted once all the
     * tasks are done.
     *
     * @param appId Id of the application that ends.
     */
    public void noMoreTasks(long appId) {
        //Not implemented yet
    }

    /**
     * Indicates that a file has to be deleted from the whole system
     *
     * @param fileName name of the file to be rmeoved
     * @return
     */
    public boolean deleteFile(String fileName) {
        //Not implemented yet
        return true;
    }

    /**
     * ****************************************************************************************************************
     * ****************************************************************************************************************
     * ********************************* ---------------------------------------- *************************************
     * ********************************* | INVOCATIONS FROM OTHER RUNTIME PARTS | *************************************
     * ********************************* ---------------------------------------- *************************************
     * ****************************************************************************************************************
     * ****************************************************************************************************************
     */
    @Override
    public void init() {
        try {
            CommunicationManager.openServer(ME);
        } catch (CommException e) {
            Log.wtf(LOGGER_TAG, "Error starting server socket on Application side.", e);
        }
    }

    @Override
    public void commandReceived(Connection c, Transfer t) {
        if (t.getObject() instanceof DataTransferRequest) {
            DataTransferRequest dtr = (DataTransferRequest) t.getObject();
            String dataId = dtr.getDataSource();
            DM.transferData(dataId, c);
        }
        if (t.getObject() instanceof DataExistenceNotification) {
            c.finishConnection();
            DataExistenceNotification den = (DataExistenceNotification) t.getObject();
            String data = den.getData();
            if (den.getLocations().isEmpty()) {
                try {
                    RUNTIME_SERVICE.requestDataLocations(data, ME);
                } catch (RemoteException re) {
                    Log.e(LOGGER_TAG, "Exception requesting data locations for object " + data + " to service");
                }
            } else {
                es.bsc.comm.Node source = den.getLocations().iterator().next();
                completeSource((NIONode) source, c);
                CommunicationManager.askforDataObject(source, data);
            }
        }
        if (t.getObject() instanceof DataSourceResponse) {
            c.finishConnection();
            DataSourceResponse dsr = (DataSourceResponse) t.getObject();
            String data = dsr.getData();
            es.bsc.comm.Node source = dsr.getLocations().iterator().next();
            completeSource((NIONode) source, c);
            CommunicationManager.askforDataObject(source, data);
        }
        if (t.getObject() instanceof ProxiedBackEndRequest) {
            c.finishConnection();
            PROXIED_BACKENDS.process((ProxiedBackEndRequest) t.getObject());
        }
    }

    @Override
    public void errorHandler(Connection cnctn, Transfer trnsfr, CommException ce) {
        Log.e(LOGGER_TAG, "Error processing transfer " + trnsfr + " on connection " + cnctn, ce);
    }

    @Override
    public void dataReceived(Connection cnctn, Transfer trnsfr) {
        String receivedData = CommunicationManager.receivedData(cnctn);
        if (trnsfr.getDestination() == Transfer.Destination.FILE) {
            DM.receivedFile(receivedData, trnsfr.getFileName());
        } else {
            DM.receivedObject(receivedData, trnsfr.getArray());
        }
    }

    @Override
    public void writeFinished(Connection cnctn, Transfer trnsfr) {
        //End of transfer submission do nothing
    }

    @Override
    public void connectionFinished(Connection cnctn) {
        //Closed connection
    }

    @Override
    public void shutdown() {
        //Nothing to do on shutdown
    }

    private void completeSource(NIONode node, Connection connection) {
        if (node.getIp() == null) {
            String ip = ((NIONode) ((NIOConnection) connection).getNode()).getIp();
            node.setIp(ip);
        }
    }


    private static class Operation {

        private final Integer data;
        private final String source;
        private final String target;

        public Operation(int data, String source, String target) {
            this.data = data;
            this.source = source;
            this.target = target;
        }

        public int getData() {
            return data;
        }

        public String getTarget() {
            return target;
        }

        @Override
        public String toString() {
            return "object " + data + " with renaming " + target;
        }

    }

    private static Integer getDataId(Object o) {
        int hashCode = o.hashCode();
        while (true) {
            Object rep = ID_TO_REPRESENTATIVE.get(hashCode);
            if (rep == null || rep == o) {
                return hashCode;
            }
            hashCode++;
        }
    }


    private static class IgnoreOperation implements DataOperationListener {

        @Override
        public void paused() {
            //No need to do anything
        }

        @Override
        public void setSize(long value) {
            //No need to do anything
        }

        @Override
        public void setValue(Class<?> type, Object value) {
            //No need to do anything
        }
    }
}
