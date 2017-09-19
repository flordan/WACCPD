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


public class Version {

    private int readers;
    private final DataInstance daId;
    private int location = 0;

    protected static final int LOCAL = 1;
    protected static final int REMOTE = 2;
    protected static final int LOCALREMOTE = 3;

    public Version(int dataId, int versionId) {
        readers = 0;
        daId = new DataInstance(dataId, versionId);
    }

    public int willBeRead() {
        readers++;
        return readers;
    }

    public int hasBeenRead() {
        readers--;
        return readers;
    }

    public int getReaders() {
        return readers;
    }

    public DataInstance getDataInstance() {
        return daId;
    }

    public boolean isLocal() {
        return (this.location & LOCAL) == 1;
    }

    public boolean isRemote() {
        return (this.location & REMOTE) == 2;
    }

    public Object dump(String prefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        sb.append(" ").append(daId);
        sb.append(" (");
        if (isLocal()) {
            sb.append("L");
        }
        if (isRemote()) {
            sb.append("R");
        }
        sb.append(")");
        return sb.toString();
    }

    public void setLocal() {
        location = location | LOCAL;
    }

    public void setRemote() {
        location = location | REMOTE;
    }
}
