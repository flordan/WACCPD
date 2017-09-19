/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/flordan/Android/workspace/MobileRuntime/src/es/bsc/mobile/runtime/service/RuntimeServiceItf.aidl
 */
package es.bsc.mobile.runtime.service;


public interface RuntimeServiceItf extends android.os.IInterface {

    /**
     * Local-side IPC implementation stub class.
     */
    public static abstract class Stub extends android.os.Binder implements es.bsc.mobile.runtime.service.RuntimeServiceItf {

        private static final java.lang.String DESCRIPTOR = "es.bsc.mobile.runtime.service.RuntimeServiceItf";

        /**
         * Construct the stub at attach it to the interface.
         */
        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        /**
         * Cast an IBinder object into an es.bsc.mobile.runtime.service.RuntimeServiceItf interface, generating a proxy
         * if needed.
         */
        public static es.bsc.mobile.runtime.service.RuntimeServiceItf asInterface(android.os.IBinder obj) {
            if ((obj == null)) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin != null) && (iin instanceof es.bsc.mobile.runtime.service.RuntimeServiceItf))) {
                return ((es.bsc.mobile.runtime.service.RuntimeServiceItf) iin);
            }
            return new es.bsc.mobile.runtime.service.RuntimeServiceItf.Stub.Proxy(obj);
        }

        @Override
        public android.os.IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                case TRANSACTION_notifyDataCreation: {
                    data.enforceInterface(DESCRIPTOR);
                    java.lang.String _arg0;
                    _arg0 = data.readString();
                    es.bsc.mobile.types.comm.Node _arg1;
                    if ((0 != data.readInt())) {
                        _arg1 = es.bsc.mobile.types.comm.Node.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    this.notifyDataCreation(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_requestDataExistence: {
                    data.enforceInterface(DESCRIPTOR);
                    java.lang.String _arg0;
                    _arg0 = data.readString();
                    es.bsc.mobile.types.comm.Node _arg1;
                    if ((0 != data.readInt())) {
                        _arg1 = es.bsc.mobile.types.comm.Node.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    this.requestDataExistence(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_requestDataLocations: {
                    data.enforceInterface(DESCRIPTOR);
                    java.lang.String _arg0;
                    _arg0 = data.readString();
                    es.bsc.mobile.types.comm.Node _arg1;
                    if ((0 != data.readInt())) {
                        _arg1 = es.bsc.mobile.types.comm.Node.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    this.requestDataLocations(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_getCoreIds: {
                    data.enforceInterface(DESCRIPTOR);
                    java.lang.String[] _arg0;
                    _arg0 = data.createStringArray();
                    int[] _result = this.getCoreIds(_arg0);
                    reply.writeNoException();
                    reply.writeIntArray(_result);
                    return true;
                }
                case TRANSACTION_executeTask: {
                    data.enforceInterface(DESCRIPTOR);
                    es.bsc.mobile.runtime.types.Task _arg0;
                    if ((0 != data.readInt())) {
                        _arg0 = es.bsc.mobile.runtime.types.Task.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    this.executeTask(_arg0);
                    reply.writeNoException();
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }


        private static class Proxy implements es.bsc.mobile.runtime.service.RuntimeServiceItf {

            private android.os.IBinder mRemote;

            Proxy(android.os.IBinder remote) {
                mRemote = remote;
            }

            @Override
            public android.os.IBinder asBinder() {
                return mRemote;
            }

            public java.lang.String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }
// Interface Operations
//String openFile(String fileName, int openMode);
//boolean deleteFile(String fileName);

            @Override
            public void notifyDataCreation(java.lang.String daId, es.bsc.mobile.types.comm.Node n) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(daId);
                    if ((n != null)) {
                        _data.writeInt(1);
                        n.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(Stub.TRANSACTION_notifyDataCreation, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void requestDataExistence(java.lang.String daId, es.bsc.mobile.types.comm.Node n) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(daId);
                    if ((n != null)) {
                        _data.writeInt(1);
                        n.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(Stub.TRANSACTION_requestDataExistence, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void requestDataLocations(java.lang.String daId, es.bsc.mobile.types.comm.Node n) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(daId);
                    if ((n != null)) {
                        _data.writeInt(1);
                        n.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(Stub.TRANSACTION_requestDataLocations, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int[] getCoreIds(java.lang.String[] signatures) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                int[] _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStringArray(signatures);
                    mRemote.transact(Stub.TRANSACTION_getCoreIds, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.createIntArray();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void executeTask(es.bsc.mobile.runtime.types.Task t) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    if ((t != null)) {
                        _data.writeInt(1);
                        t.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(Stub.TRANSACTION_executeTask, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
        static final int TRANSACTION_notifyDataCreation = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
        static final int TRANSACTION_requestDataExistence = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
        static final int TRANSACTION_requestDataLocations = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
        static final int TRANSACTION_getCoreIds = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
        static final int TRANSACTION_executeTask = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    }
// Interface Operations
//String openFile(String fileName, int openMode);
//boolean deleteFile(String fileName);

    public void notifyDataCreation(java.lang.String daId, es.bsc.mobile.types.comm.Node n) throws android.os.RemoteException;

    public void requestDataExistence(java.lang.String daId, es.bsc.mobile.types.comm.Node n) throws android.os.RemoteException;

    public void requestDataLocations(java.lang.String daId, es.bsc.mobile.types.comm.Node n) throws android.os.RemoteException;

    public int[] getCoreIds(java.lang.String[] signatures) throws android.os.RemoteException;

    public void executeTask(es.bsc.mobile.runtime.types.Task t) throws android.os.RemoteException;
}
