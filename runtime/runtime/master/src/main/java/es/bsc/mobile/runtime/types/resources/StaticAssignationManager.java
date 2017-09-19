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
package es.bsc.mobile.runtime.types.resources;

import es.bsc.mobile.runtime.types.Task;
import java.util.HashMap;
import java.util.LinkedList;


public class StaticAssignationManager {

    private static final HashMap<String, LinkedList<Integer>> PLATFORM_TO_TASK = new HashMap<String, LinkedList<Integer>>();
    private static final ComputingPlatform[] TASK_TO_PLATFORM = new ComputingPlatform[0];
    public static ComputingPlatform defaultPlatform;

    static {
        //Set LinkedList with taskIds to each platform
    }

    public static void setDefaultPlatform(ComputingPlatform cp) {
        defaultPlatform = cp;
        for (int i = 0; i < TASK_TO_PLATFORM.length; i++) {
            if (TASK_TO_PLATFORM[i] == null) {
                TASK_TO_PLATFORM[i] = cp;
            }
        }
    }

    public static void registerPlatform(ComputingPlatform cp) {
        LinkedList<Integer> tasks = PLATFORM_TO_TASK.remove(cp.getName());
        if (tasks == null) {
            return;
        }

        for (int taskId : tasks) {
            TASK_TO_PLATFORM[taskId] = cp;
        }
    }

    public static boolean runsTask(Task t, ComputingPlatform cp) {
        int taskId = t.getId();
        if (taskId < TASK_TO_PLATFORM.length && TASK_TO_PLATFORM[t.getId()] != null) {
            return TASK_TO_PLATFORM[t.getId()] == cp;
        } else {
            return defaultPlatform == cp;
        }
    }

    public static ComputingPlatform getPredefinedPlatform(Task t) {
        int taskId = t.getId();
        if (taskId < TASK_TO_PLATFORM.length && TASK_TO_PLATFORM[t.getId()] != null) {
            return TASK_TO_PLATFORM[t.getId()];
        } else {
            return defaultPlatform;
        }
    }

}
