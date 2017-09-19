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
import java.util.HashMap;
import java.util.LinkedList;


public interface ProxiedBackEndRequest {

    public void process(
            HashMap<String, ComputingPlatformBackend> platforms,
            HashMap<String, ProxiedJobExecutionController> jobExecutions,
            HashMap<String, LinkedList<ProxiedBackEndRequest>> pendingRequests
    );

}
