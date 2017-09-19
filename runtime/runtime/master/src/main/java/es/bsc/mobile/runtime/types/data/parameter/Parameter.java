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
import es.bsc.mobile.annotations.Parameter.Type;
import es.bsc.mobile.annotations.Parameter.Direction;


public abstract class Parameter implements Parcelable {

    private final Type type;
    private final Direction direction;

    public Parameter(Type type, Direction direction) {
        this.type = type;
        this.direction = direction;
    }

    public Parameter(Parcel in) {
        type = Type.values()[in.readInt()];
        direction = Direction.values()[in.readInt()];
    }

    public Direction getDirection() {
        return direction;
    }

    public Type getType() {
        return type;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(type.ordinal());
        out.writeInt(direction.ordinal());
    }
}
