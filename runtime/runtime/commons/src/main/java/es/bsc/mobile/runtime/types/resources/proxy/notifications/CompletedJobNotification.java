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

import es.bsc.mobile.types.Job;
import es.bsc.mobile.types.JobExecution;
import es.bsc.mobile.types.JobProfile;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


public class CompletedJobNotification extends ProxiedBackEndNotification implements Externalizable {

    private String jobId;
    private JobProfile jp;

    public CompletedJobNotification() {
    }

    public CompletedJobNotification(String jobId, JobProfile jp) {
        this.jobId = jobId;
        this.jp = jp;
    }

    @Override
    public void writeExternal(ObjectOutput oo) throws IOException {
        oo.writeUTF(jobId);
        oo.writeObject(jp);
    }

    @Override
    public void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
        jobId = oi.readUTF();
        jp = (JobProfile) oi.readObject();
    }

    @Override
    public void process() {
        JobExecution je = getProxiedJobExecution(jobId);
        Job job = je.getJob();
        JobProfile originalProfile = job.getJobProfile();
        originalProfile.setImplementationId(jp.getImplementationId());
        int params = job.getParams().length;
        for (int paramId = 0; paramId < params; paramId++) {
            originalProfile.setParamSize(true, paramId, jp.getParamsSize(true, paramId));
            originalProfile.setParamSize(false, paramId, jp.getParamsSize(false, paramId));
        }
        if (job.getTarget() != null) {
            originalProfile.setTargetSize(true, jp.getTargetSize(true));
            originalProfile.setTargetSize(false, jp.getTargetSize(false));
        }
        if (job.getResult() != null) {
            originalProfile.setResultSize(jp.getResultSize());
        }
        originalProfile.startsExecution(jp.getExecutionStartTime());
        originalProfile.endsExecution(jp.getExecutionEndTime());
        originalProfile.endsTransfers(jp.getTransfersEndTime());
        originalProfile.setExecutionEnergy(jp.getExecutionEnergy());

        je.completed();
    }

}
