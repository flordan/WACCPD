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
package es.bsc.mobile.runtime.types.data.parameter;

import android.os.Parcel;
import android.os.Parcelable;
import es.bsc.mobile.annotations.Parameter.Direction;
import es.bsc.mobile.annotations.Parameter.Type;
import es.bsc.mobile.runtime.types.data.access.DataAccess;
import es.bsc.mobile.runtime.types.data.access.ReadAccess;
import es.bsc.mobile.runtime.types.data.access.ReadWriteAccess;
import es.bsc.mobile.runtime.types.data.access.WriteAccess;


public class RegisteredParameter extends Parameter implements Parcelable {

    public static final Parcelable.Creator<RegisteredParameter> CREATOR = new Parcelable.Creator<RegisteredParameter>() {
        @Override
        public RegisteredParameter createFromParcel(Parcel in) {
            return new RegisteredParameter(in);
        }

        @Override
        public RegisteredParameter[] newArray(int size) {
            return new RegisteredParameter[size];
        }
    };

    private DataAccess dAccess;

    public RegisteredParameter(Type type, Direction direction, DataAccess dAccess) {
        super(type, direction);
        this.dAccess = dAccess;
    }

    public RegisteredParameter(Parcel in) {
        super(in);
        int type = in.readInt();
        switch (DataAccess.Action.values()[type]) {
            case READ:
                dAccess = in.readParcelable(ReadAccess.class.getClassLoader());
                break;
            case UPDATE:
                dAccess = in.readParcelable(ReadWriteAccess.class.getClassLoader());
                break;
            default:
                //WRITE
                dAccess = in.readParcelable(WriteAccess.class.getClassLoader());
        }

    }

    public DataAccess getDAccess() {
        return dAccess;
    }

    public void setDAccess(DataAccess dAccess) {
        this.dAccess = dAccess;
    }

    @Override
    public String toString() {
        return dAccess + " " + getType() + " " + getDirection();
    }

    @Override
    public int describeContents() {
        return 3;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(dAccess.getAction().ordinal());
        out.writeParcelable(dAccess, 0);
    }
}
