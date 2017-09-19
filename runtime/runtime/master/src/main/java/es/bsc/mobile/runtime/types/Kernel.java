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
package es.bsc.mobile.runtime.types;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import java.io.InputStream;
import java.io.IOException;


public class Kernel extends Implementation {

    private final String program;
    private final String code;
    private final String[] resultSize;
    private final String[] workloadSize;
    private final String[] localSize;
    private final String[] offset;
    private final Class<?> resultType;

    public static final Parcelable.Creator<Kernel> CREATOR = new Parcelable.Creator<Kernel>() {
        @Override
        public Kernel createFromParcel(Parcel in) {
            return new Kernel(in);
        }

        @Override
        public Kernel[] newArray(int size) {
            return new Kernel[size];
        }
    };

    public Kernel(Context context, int coreElementId, int implementationId, String program, String methodName, Class<?> resultType, String[] resultSize, String[] workloadSize, String[] localSize, String[] offset) {
        super(coreElementId, implementationId, methodName);
        String[] split = program.split("/");
        int programId = context.getResources().getIdentifier(split[1].substring(0, split[1].length() - 3), split[0], context.getPackageName());
        this.program = program;
        InputStream input = context.getResources().openRawResource(programId);
        String code = null;
        if (input != null) {
            try {
                int size = input.available();
                byte[] content = new byte[size];
                input.read(content);
                code = new String(content);
            } catch (IOException ioe) {
                code = null;
            }
        }
        this.code = code;
        Class<?> rType = resultType;
        int dims = 0;
        while (rType.isArray()) {
            rType = rType.getComponentType();
            dims++;
        }
        this.resultType = rType;
        if (resultSize.length == dims) {
            this.resultSize = resultSize;
        } else {
            throw new RuntimeException("Dimensions of result for the method and for annotations does not match. Annotation provided " + resultSize.length + " expressions and method generates a " + dims + "-dimension matrix");
        }
        this.workloadSize = workloadSize;
        this.localSize = localSize;
        this.offset = offset;
    }

    public Kernel(Parcel in) {
        super(in);
        program = in.readString();
        code = in.readString();
        Class<?> resultTypeClass;

        String resultTypeName = in.readString();
        try {
            resultTypeClass = Class.forName(resultTypeName);
        } catch (ClassNotFoundException cnfe) {
            resultTypeClass = null;
            if (resultTypeName.equals("char")) {
                resultTypeClass = char.class;
            }
            if (resultTypeName.equals("byte")) {
                resultTypeClass = byte.class;
            }
            if (resultTypeName.equals("short")) {
                resultTypeClass = short.class;
            }
            if (resultTypeName.equals("int")) {
                resultTypeClass = int.class;
            }
            if (resultTypeName.equals("long")) {
                resultTypeClass = long.class;
            }

            if (resultTypeName.equals("float")) {
                resultTypeClass = float.class;
            }
            if (resultTypeName.equals("double")) {
                resultTypeClass = double.class;
            }
            if (resultTypeName.equals("void")) {
                resultTypeClass = void.class;
            }
        }
        if (resultTypeClass == null) {
            (new RuntimeException("Could not find class " + resultTypeName)).printStackTrace();
        }
        this.resultType = resultTypeClass;
        int dims = in.readInt();
        resultSize = new String[dims];
        for (int i = 0; i < dims; i++) {
            resultSize[i] = in.readString();
        }
        dims = in.readInt();
        workloadSize = new String[dims];
        offset = new String[dims];
        for (int i = 0; i < dims; i++) {
            workloadSize[i] = in.readString();
            offset[i] = in.readString();
        }

        dims = in.readInt();
	localSize = new String[dims];
       for (int i = 0; i < dims; i++) {
            localSize[i] = in.readString();
        }
    }

    public String getProgram() {
        return program;
    }

    @Override
    public String completeSignature(String methodSignature) {
        return program + "->" + methodSignature;
    }

    public String getSourceCode() {
        return code;
    }

    @Override
    public es.bsc.mobile.types.Implementation getInternalImplementation() {
        return new es.bsc.mobile.types.Kernel(getCoreElementId(), getImplementationId(), getMethodName(), program, resultType, resultSize, workloadSize, localSize, offset, code);
    }

    @Override
    public int describeContents() {
        return 2;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(program);
        out.writeString(code);
        out.writeString(resultType.getName());
        out.writeInt(resultSize.length);
        for (int i = 0; i < resultSize.length; i++) {
            out.writeString(resultSize[i]);
        }
        out.writeInt(workloadSize.length);
        for (int i = 0; i < workloadSize.length; i++) {
            out.writeString(workloadSize[i]);
            out.writeString(offset[i]);
        }
        out.writeInt(localSize.length);
        for (int i = 0; i < localSize.length; i++) {
            out.writeString(localSize[i]);

        }
    }
}
