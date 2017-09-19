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
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ObjectTypeParameter extends Parameter implements Parcelable {

    public static final Parcelable.Creator<ObjectTypeParameter> CREATOR = new Parcelable.Creator<ObjectTypeParameter>() {
        @Override
        public ObjectTypeParameter createFromParcel(Parcel in) {
            return new ObjectTypeParameter(in);
        }

        @Override
        public ObjectTypeParameter[] newArray(int size) {
            return new ObjectTypeParameter[size];
        }
    };

    protected Parcelable value;

    public ObjectTypeParameter(es.bsc.mobile.annotations.Parameter.Direction direction, Parcelable value)
            throws URISyntaxException, IOException {
        super(es.bsc.mobile.annotations.Parameter.Type.OBJECT, direction);
        this.value = value;
    }

    public ObjectTypeParameter(Parcel in) {
        super(in);
        String objectClass = in.readString();
        try {
            value = in.readParcelable(Class.forName(objectClass).getClassLoader());
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ObjectTypeParameter.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(value.getClass().toString());
        out.writeParcelable((Parcelable) value, flags);
    }

}
