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
package es.bsc.mobile.runtime.types.data;

import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;


public class DataInfo {

    private static final int FIRST_DATA_ID = 1;
    private static final int FIRST_VERSION_ID = 0;
    private static final AtomicInteger NEXT_DATA_ID = new AtomicInteger(FIRST_DATA_ID);

    private final int dataId;

    protected TreeMap<Integer, Version> versions;
    protected Version currentVersion;
    private int currentVersionId;
    private Object localValue;

    public DataInfo() {
        this.dataId = NEXT_DATA_ID.getAndIncrement();
        this.versions = new TreeMap<Integer, Version>();
        localValue = null;
        currentVersionId = FIRST_VERSION_ID;
    }

    public int getDataId() {
        return dataId;
    }

    public Version getVersion(int versionId) {
        return versions.get(versionId);
    }

    public Version getCurrentVersion() {
        return currentVersion;
    }

    public int getCurrentVersionId() {
        return currentVersionId;
    }

    public Version addVersion() {
        currentVersionId++;
        Version newVersion = new Version(dataId, currentVersionId);
        versions.put(currentVersionId, newVersion);
        currentVersion = newVersion;
        return currentVersion;
    }

    public void removeVersion(int versionId) {
        versions.remove(versionId);
    }

    public void noVersion() {
        currentVersion = null;
    }

    public void setLocalValue(Object o) {
        localValue = o;
    }

    public Object getLocalValue() {
        return localValue;
    }

    public String dump(String prefix) {
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<Integer, Version> entry : versions.entrySet()) {

            Version version = entry.getValue();

            if (version == currentVersion) {
                sb.append("*");
            } else {
                sb.append(" ");
            }
            sb.append(prefix).append("version:").append(entry.getKey()).append("\n");
            sb.append(version.dump(prefix + "\t")).append("\n");
        }
        return sb.toString();
    }
}
