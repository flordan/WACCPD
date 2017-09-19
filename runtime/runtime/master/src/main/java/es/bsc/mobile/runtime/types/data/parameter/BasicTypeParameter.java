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


public class BasicTypeParameter extends Parameter implements Parcelable {

    public static final Parcelable.Creator<BasicTypeParameter> CREATOR = new Parcelable.Creator<BasicTypeParameter>() {
        @Override
        public BasicTypeParameter createFromParcel(Parcel in) {
            return new BasicTypeParameter(in);
        }

        @Override
        public BasicTypeParameter[] newArray(int size) {
            return new BasicTypeParameter[size];
        }
    };

    private Object value;

    public BasicTypeParameter(Parcel in) {
        super(in);
        switch (this.getType()) {
            case BOOLEAN:
                value = (in.readByte() == 1);
                break;
            case CHAR:
                value = (char) in.readByte();
                break;
            case STRING:
                value = in.readString();
                break;
            case BYTE:
                value = in.readByte();
                break;
            case SHORT:
                value = (short) in.readInt();
                break;
            case INT:
                value = in.readInt();
                break;
            case LONG:
                value = in.readLong();
                break;
            case FLOAT:
                value = in.readFloat();
                break;
            case DOUBLE:
                value = in.readDouble();
                break;
            default:
                break;

        }
    }

    public BasicTypeParameter(Type type, Direction direction, Object value) {
        super(type, direction);
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value + " " + getType() + " " + getDirection();
    }

    @Override
    public int describeContents() {
        return 2;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        switch (this.getType()) {
            case BOOLEAN:
                byte b = (byte) (((Boolean) value) ? 1 : 0);
                out.writeByte(b);
                break;
            case CHAR:
                char car = (Character) value;
                out.writeByte((byte) car);
                break;
            case STRING:
                out.writeString((String) value);
                break;
            case BYTE:
                out.writeByte((Byte) value);
                break;
            case SHORT:
                out.writeInt((Short) value);
                break;
            case INT:
                out.writeInt((Integer) value);
                break;
            case LONG:
                out.writeLong((Long) value);
                break;
            case FLOAT:
                out.writeFloat((Float) value);
                break;
            case DOUBLE:
                out.writeDouble((Double) value);
                break;
            default:
                break;

        }
    }
}
