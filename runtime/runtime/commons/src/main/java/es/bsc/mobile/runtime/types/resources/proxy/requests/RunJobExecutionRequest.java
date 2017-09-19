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

import es.bsc.mobile.runtime.types.resources.ComputingPlatformBackend;
import es.bsc.mobile.runtime.types.resources.proxy.ProxiedJobExecutionController;
import es.bsc.mobile.types.Implementation;
import es.bsc.mobile.types.JobExecution;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.LinkedList;


public class RunJobExecutionRequest implements ProxiedBackEndRequest, Externalizable {

    private String jobId;
    private Object executorId;
    private int implId;

    public RunJobExecutionRequest() {
    }

    public RunJobExecutionRequest(String jobId, Object executorId, int implId) {
        this.jobId = jobId;
        this.executorId = executorId;
        this.implId = implId;

    }

    @Override
    public void writeExternal(ObjectOutput oo) throws IOException {
        oo.writeUTF(jobId);
        oo.writeObject(executorId);
        oo.writeInt(implId);
    }

    @Override
    public void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
        this.jobId = oi.readUTF();
        this.executorId = oi.readObject();
        this.implId = oi.readInt();
    }

    @Override
    public void process(
            HashMap<String, ComputingPlatformBackend> platforms,
            HashMap<String, ProxiedJobExecutionController> jobExecutions,
            HashMap<String, LinkedList<ProxiedBackEndRequest>> pendingRequests
    ) {

        ProxiedJobExecutionController je = jobExecutions.get(jobId);
        if (je != null) {
            Implementation impl = null;
            for (Implementation implCandidate : je.getCompatibleImplementations()) {
                if (implCandidate.getImplementationId() == implId) {
                    impl = implCandidate;
                    je.executeOn(executorId, impl);
                    return;
                }
            }
        } else {
            LinkedList<ProxiedBackEndRequest> pendingReqs = pendingRequests.get(jobId);
            if (pendingReqs == null) {
                pendingReqs = new LinkedList<ProxiedBackEndRequest>();
                pendingRequests.put(jobId, pendingReqs);
            }
            pendingReqs.add(this);
        }
    }
}
