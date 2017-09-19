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
package es.bsc.mobile.types.comm;

import android.os.Parcel;
import android.os.Parcelable;
import es.bsc.comm.nio.NIONode;


public class Node extends NIONode implements Parcelable {

    private static final long serialVersionUID = 1L;
    public static final Parcelable.Creator<Node> CREATOR = new Parcelable.Creator<Node>() {
        @Override
        public Node createFromParcel(Parcel in) {
            return new Node(in);
        }

        @Override
        public Node[] newArray(int size) {
            return new Node[size];
        }
    };

    public Node() {
        super();
    }

    public Node(String ip, int port) {
        super(ip, port);
    }

    private Node(Parcel in) {
        this.setPort(in.readInt());
        if (in.dataAvail() > 0) {
            this.setIp(in.readString());
        }

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.getPort());
        if (this.getIp() != null) {
            out.writeString(this.getIp());
        }
    }

}
