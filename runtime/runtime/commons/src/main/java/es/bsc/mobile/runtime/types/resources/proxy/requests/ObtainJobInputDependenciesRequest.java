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

import es.bsc.mobile.exceptions.UnexpectedDirectionException;
import es.bsc.mobile.runtime.types.resources.ComputingPlatformBackend;
import es.bsc.mobile.runtime.types.resources.proxy.ProxiedJobExecutionController;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.LinkedList;


public class ObtainJobInputDependenciesRequest implements ProxiedBackEndRequest, Externalizable {

    private String jobId;

    public ObtainJobInputDependenciesRequest() {
    }

    public ObtainJobInputDependenciesRequest(String jobId) {

        this.jobId = jobId;

    }

    @Override
    public void writeExternal(ObjectOutput oo) throws IOException {
        oo.writeUTF(jobId);
    }

    @Override
    public void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
        this.jobId = oi.readUTF();
    }

    @Override
    public void process(
            HashMap<String, ComputingPlatformBackend> platforms,
            HashMap<String, ProxiedJobExecutionController> jobExecutions,
            HashMap<String, LinkedList<ProxiedBackEndRequest>> pendingRequests
    ) {
        ProxiedJobExecutionController je = jobExecutions.get(jobId);
        if (je != null) {
            try {
                je.obtainJobInputDataDependencies();
            } catch (UnexpectedDirectionException e) {
                //Should be detected before submitting the message
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
