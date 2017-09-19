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
package es.bsc.mobile.data.access;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


public class RAccess extends DaAccess {

    protected String readDataInstance;

    public RAccess() {
        super(Action.READ);
    }

    public RAccess(String data) {
        super(Action.READ);
        this.readDataInstance = data;
    }

    public String getReadDataInstance() {
        return readDataInstance;
    }

    public void setReadDataInstance(String data) {
        this.readDataInstance = data;
    }

    @Override
    public String toString() {
        return "Data Access for lecture on data " + readDataInstance;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeUTF(readDataInstance);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        readDataInstance = in.readUTF();
    }

}
