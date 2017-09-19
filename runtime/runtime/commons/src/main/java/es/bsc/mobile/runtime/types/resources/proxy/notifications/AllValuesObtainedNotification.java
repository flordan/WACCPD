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
package es.bsc.mobile.runtime.types.resources.proxy.notifications;

import es.bsc.mobile.types.JobExecution;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


public class AllValuesObtainedNotification extends ProxiedBackEndNotification implements Externalizable {

    private String jobId;

    public AllValuesObtainedNotification() {
    }

    public AllValuesObtainedNotification(String jobId) {
        this.jobId = jobId;
    }

    @Override
    public void writeExternal(ObjectOutput oo) throws IOException {
        oo.writeUTF(jobId);
    }

    @Override
    public void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
        jobId = oi.readUTF();
    }

    @Override
    public void process() {
        JobExecution je = getProxiedJobExecution(jobId);
        je.allDataPresent();
    }

}
