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
package es.bsc.mobile.runtime.utils;

import java.io.IOException;
import java.util.HashMap;

import es.bsc.mobile.annotations.Parameter.Direction;
import es.bsc.mobile.runtime.types.data.DataInfo;
import es.bsc.mobile.runtime.types.data.DataInstance;
import es.bsc.mobile.runtime.types.data.Version;
import es.bsc.mobile.runtime.types.data.access.DataAccess;
import es.bsc.mobile.runtime.types.data.access.ReadAccess;
import es.bsc.mobile.runtime.types.data.access.ReadWriteAccess;
import es.bsc.mobile.runtime.types.data.access.WriteAccess;


/**
 *
 * @param <K> data Id type
 */
public class DataRegistry<K> {

    //Version structure
    private final HashMap<K, DataInfo> idToData;

    public DataRegistry() {
        idToData = new HashMap<K, DataInfo>();
    }

    public DataInfo registerData(K dataId) {
        DataInfo dataInfo = idToData.get(dataId);
        if (dataInfo == null) {
            dataInfo = new DataInfo();
            idToData.put(dataId, dataInfo);
        }
        return dataInfo;
    }

    public DataInfo findData(K dataId) {
        return idToData.get(dataId);
    }

    public DataAccess registerRemoteDataAccess(Direction direction, DataInfo dataInfo) {
        Version currentVersion = dataInfo.getCurrentVersion();
        DataInstance writtenInstance;
        DataAccess da;
        if (currentVersion == null) {
            currentVersion = dataInfo.addVersion();
            currentVersion.setLocal();
        }
        switch (direction) {
            case IN:
                currentVersion.setRemote();
                currentVersion.willBeRead();
                da = new ReadAccess(currentVersion.getDataInstance());
                break;
            case INOUT:
                currentVersion.setRemote();
                currentVersion.willBeRead();
                DataInstance readInstance = currentVersion.getDataInstance();
                currentVersion = dataInfo.addVersion();
                currentVersion.setRemote();
                writtenInstance = currentVersion.getDataInstance();
                da = new ReadWriteAccess(readInstance, writtenInstance);
                break;
            default: // OUT
                currentVersion = dataInfo.addVersion();
                currentVersion.setRemote();
                writtenInstance = currentVersion.getDataInstance();
                da = new WriteAccess(writtenInstance);
        }
        return da;
    }

    public DataAccess registerLocalDataAccess(Direction direction, DataInfo dataInfo) throws IOException {
        DataAccess da;
        Version currentVersion = dataInfo.getCurrentVersion();
        if (currentVersion == null) {
            return null;
        }
        DataInstance readInstance = currentVersion.getDataInstance();
        switch (direction) {
            case IN:
                currentVersion.setLocal();
                return new ReadAccess(readInstance);
            case INOUT:
                currentVersion.setLocal();
                dataInfo.noVersion();
                da = new ReadWriteAccess(readInstance, new DataInstance(dataInfo.getDataId(), -1));
                break;
            default:
                da = null;
        }
        return da;
    }

    public DataInfo deleteData(K dataId) {
        return idToData.remove(dataId);
    }

    public String dump() {
        StringBuilder sb = new StringBuilder("Data stored in the DataRegistry:\n");
        for (java.util.Map.Entry<K, DataInfo> entry : idToData.entrySet()) {
            sb.append("ID:").append(entry.getKey()).append("(").append(entry.getValue().getDataId()).append(")" + "\n");
            sb.append(entry.getValue().dump("\t"));
        }
        return sb.toString();
    }

    public boolean checkExistence(K dataId) {
        return idToData.get(dataId) != null;
    }

}
