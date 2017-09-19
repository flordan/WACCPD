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
package es.bsc.mobile.runtime.types.data.access;

import android.os.Parcel;
import android.os.Parcelable;
import es.bsc.mobile.runtime.types.data.DataInstance;
import es.bsc.mobile.data.access.DaAccess;
import es.bsc.mobile.data.access.RWAccess;


public class ReadWriteAccess extends DataAccess {

    protected DataInstance readDataInstance;
    protected DataInstance writeDataInstance;
    public static final Parcelable.Creator<ReadWriteAccess> CREATOR = new Parcelable.Creator<ReadWriteAccess>() {
        @Override
        public ReadWriteAccess createFromParcel(Parcel in) {
            return new ReadWriteAccess(in);
        }

        @Override
        public ReadWriteAccess[] newArray(int size) {
            return new ReadWriteAccess[size];
        }
    };

    public ReadWriteAccess() {
        super(Action.UPDATE);
    }

    public ReadWriteAccess(DataInstance rData, DataInstance wData) {
        super(Action.UPDATE);
        this.readDataInstance = rData;
        this.writeDataInstance = wData;
    }

    public ReadWriteAccess(Parcel in) {
        super(in);
        readDataInstance = in.readParcelable(DataInstance.class.getClassLoader());
        writeDataInstance = in.readParcelable(DataInstance.class.getClassLoader());
    }

    public DataInstance getReadDataInstance() {
        return readDataInstance;
    }

    public void setReadDataInstance(DataInstance data) {
        this.readDataInstance = data;
    }

    public DataInstance getWrittenDataInstance() {
        return writeDataInstance;
    }

    public void setWriteDataInstance(DataInstance data) {
        this.writeDataInstance = data;
    }

    @Override
    public String toString() {
        return "Data Access for updating data " + readDataInstance + " to  " + writeDataInstance;
    }

    @Override
    public Integer getDataID() {
        return readDataInstance.getDataId();
    }

    @Override
    public DaAccess prepareToOffload() {
        return new RWAccess(readDataInstance.getRenaming(), writeDataInstance.getRenaming());
    }

    @Override
    public int describeContents() {
        return 1;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeParcelable(readDataInstance, flags);
        out.writeParcelable(writeDataInstance, flags);
    }
}
