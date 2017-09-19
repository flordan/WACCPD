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
import es.bsc.mobile.data.access.WAccess;


public class WriteAccess extends DataAccess {

    protected DataInstance writtenDataInstance;

    public static final Parcelable.Creator<WriteAccess> CREATOR = new Parcelable.Creator<WriteAccess>() {
        @Override
        public WriteAccess createFromParcel(Parcel in) {
            return new WriteAccess(in);
        }

        @Override
        public WriteAccess[] newArray(int size) {
            return new WriteAccess[size];
        }
    };

    public WriteAccess() {
        super(Action.WRITE);
    }

    public WriteAccess(DataInstance data) {
        super(Action.WRITE);
        this.writtenDataInstance = data;
    }

    public WriteAccess(Parcel in) {
        super(in);
        writtenDataInstance = in.readParcelable(DataInstance.class.getClassLoader());
    }

    public DataInstance getWrittenDataInstance() {
        return writtenDataInstance;
    }

    public void setWrittenDataInstance(DataInstance data) {
        this.writtenDataInstance = data;
    }

    @Override
    public String toString() {
        return "Data Access to create data " + writtenDataInstance;
    }

    @Override
    public Integer getDataID() {
        return writtenDataInstance.getDataId();
    }

    @Override
    public DaAccess prepareToOffload() {
        return new WAccess(writtenDataInstance.getRenaming());
    }

    @Override
    public int describeContents() {
        return 2;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeParcelable(writtenDataInstance, flags);
    }
}
