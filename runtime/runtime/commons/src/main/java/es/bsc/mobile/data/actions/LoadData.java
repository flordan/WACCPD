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
package es.bsc.mobile.data.actions;

import es.bsc.mobile.data.DataManager;
import es.bsc.mobile.types.Operation;


public class LoadData extends DataAction {

    private final Class<?> type;
    private final Object value;
    private final Operation op;

    public LoadData(Class<?> type, Object value, Operation op) {
        this.type = type;
        this.value = value;
        this.op = op;
    }

    @Override
    public void perform() {
        DataManager.DataOperationListener listener = op.getListener();
        listener.setValue(type, value);
    }

}
