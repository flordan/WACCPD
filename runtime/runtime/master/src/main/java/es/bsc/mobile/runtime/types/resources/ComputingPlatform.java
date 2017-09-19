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

import es.bsc.comm.Node;
import es.bsc.mobile.runtime.types.Task;
import es.bsc.mobile.types.JobProfile;
import es.bsc.mobile.types.calc.MinMax;
import java.util.LinkedList;


public interface ComputingPlatform<T extends Resource> {

    public class ExecutionScore implements Comparable<ExecutionScore> {

        private static final int TIME_WEIGHT = 1;
        private static final int ENERGY_WEIGHT = 10;
        private static final int ECONOMIC_WEIGHT = 0;

        private static final int TIME_FACTOR = TIME_WEIGHT * 1000;
        private static final int ENERGY_FACTOR = ENERGY_WEIGHT;
        private static final int ECONOMIC_FACTOR = ECONOMIC_WEIGHT * 1000000000;

        private final MinMax time;
        private final MinMax energy;
        private final MinMax economic;
        //private final MinMax value;

        public ExecutionScore(MinMax time, MinMax energy, MinMax economic) {
            this.time = time;
            this.energy = energy;
            this.economic = economic;
//            this.value = new MinMax();
//            this.value.aggregate(TIME_FACTOR, time);
//            this.value.aggregate(ENERGY_FACTOR, energy);
//            this.value.aggregate(ECONOMIC_FACTOR, economic);
        }

        @Override
        public int compareTo(ExecutionScore o) {
            if (o == null) {
                return 1;
            }
            return Long.compare(0, (this.time.average() - o.time.average()) * TIME_FACTOR + (this.energy.average() - o.energy.average()) * ENERGY_FACTOR);
        }

        public MinMax getTime() {
            return this.time;
        }

        @Override
        public String toString() {
            return "Time " + time.average() + " Energy " + energy.average() + " Cost " + economic.average();
        }

        public String toString(int prefixLength) {
            String spaces = String.format("%-" + prefixLength + "s", "");
            return spaces + "Time " + time.toString() + "\n"
                    + spaces + "Energy " + energy.toString() + "\n"
                    + spaces + "Cost " + economic.toString();
        }
    }


    public class TaskData {

        private final String dataName;
        private final MinMax dataSize;
        private long creationTime;

        public TaskData(String dataName, MinMax dataSize) {
            this.dataName = dataName;
            this.dataSize = dataSize;
        }

        public TaskData(String dataName, MinMax dataSize, long creationTime) {
            this.dataName = dataName;
            this.dataSize = dataSize;
            this.creationTime = creationTime;
        }

        public String getDataName() {
            return dataName;
        }

        public MinMax getDataSize() {
            return dataSize;
        }

        public void setCreationTime(long creationTime) {
            this.creationTime = creationTime;
        }

        public long getCreationTime() {
            return creationTime;
        }
    }


    public interface TaskListener {

        public void completedTask(int platformId, JobProfile jp);

        public void failedJob();

    }

    public abstract int getId();

    public abstract String getName();

    public abstract void addResource(T res);

    public abstract void defineDefaultProfiles(String profiles);

    public abstract void init();

    public abstract void resizeCores();

    public abstract ExecutionScore getExecutionForecast(Task task, long submissionTime, LinkedList<TaskData> inData, LinkedList<TaskData> outData);

    public boolean canRun(Task task);

    public abstract void submitTask(Task task, ExecutionScore forecast, TaskListener listener);

}
