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
package es.bsc.mobile.data;


public interface DataProvider {

    public DataManager.DataStatus getDataStatus(String dataId);

    public void requestDataExistence(String dataId, DataManager.DataExistenceListener listener);

    public void obtainDataSize(String dataId, DataManager.DataOperationListener listener);

    public void obtainDataAsObject(String dataId, String dataRenaming, DataManager.DataOperationListener listener);

    public void obtainDataAsFile(String dataId, String dataRenaming, DataManager.DataOperationListener listener);

    public void storeObject(String dataId, Object value, DataManager.DataOperationListener listener);

    public void storeFile(String dataId, String location, DataManager.DataOperationListener listener);

}
