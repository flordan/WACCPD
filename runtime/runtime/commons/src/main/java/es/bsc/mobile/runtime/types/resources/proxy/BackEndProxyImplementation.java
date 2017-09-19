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
package es.bsc.mobile.runtime.types.resources.proxy;

import es.bsc.mobile.runtime.types.resources.ComputingPlatformBackend;
import es.bsc.mobile.runtime.types.resources.proxy.requests.ProxiedBackEndRequest;
import java.util.HashMap;
import java.util.LinkedList;


public class BackEndProxyImplementation {

    private final HashMap<String, ComputingPlatformBackend> backends;
    private final HashMap<String, ProxiedJobExecutionController> jobControllers;
    private final HashMap<String, LinkedList<ProxiedBackEndRequest>> pendingRequests;

    public BackEndProxyImplementation() {
        backends = new HashMap<String, ComputingPlatformBackend>();
        jobControllers = new HashMap<String, ProxiedJobExecutionController>();
        pendingRequests = new HashMap<String, LinkedList<ProxiedBackEndRequest>>();
    }

    public void process(ProxiedBackEndRequest req) {
        req.process(backends, jobControllers, pendingRequests);
    }

    public void addProxiedBackend(String name, ComputingPlatformBackend backend) {
        backends.put(name, backend);
    }

}
