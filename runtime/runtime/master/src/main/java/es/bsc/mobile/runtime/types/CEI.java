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
import es.bsc.mobile.annotations.JavaMethod;
import es.bsc.mobile.annotations.OpenCL;
import java.util.LinkedList;

import android.os.Parcel;
import android.os.Parcelable;
import es.bsc.mobile.types.Constraints;
import java.util.Arrays;


public class CEI implements Parcelable {

    private final String[] ceSignatures;
    private final String[][] implSignatures;
    private final LinkedList<Implementation>[] coreElements;
    private final int[] coreParamsCount;

    public static final Parcelable.Creator<CEI> CREATOR = new Parcelable.Creator<CEI>() {
        @Override
        public CEI createFromParcel(Parcel in) {
            return new CEI(in);
        }

        @Override
        public CEI[] newArray(int size) {
            return new CEI[size];
        }
    };

    public CEI(Context context, Class<?> itf) {
        int coreCount = itf.getDeclaredMethods().length;
        ceSignatures = new String[coreCount];
        coreElements = new LinkedList[coreCount];
        coreParamsCount = new int[coreCount];
        implSignatures = new String[coreCount][];
        int ceId = 0;
        for (java.lang.reflect.Method m : itf.getDeclaredMethods()) {
            if (m.isAnnotationPresent(es.bsc.mobile.annotations.CoreElement.class)) {
                coreElements[ceId] = loadCoreElement(context, m, ceId);
                coreParamsCount[ceId] = m.getParameterAnnotations().length;
            }
            ceId++;
        }
    }

    protected CEI(Parcel in) {
        int coreCount = in.readInt();
        coreElements = new LinkedList[coreCount];
        ceSignatures = new String[coreCount];
        implSignatures = new String[coreCount][];
        coreParamsCount = new int[coreCount];
        for (int i = 0; i < coreCount; i++) {
            coreElements[i] = new LinkedList<Implementation>();
            ceSignatures[i] = in.readString();
            coreParamsCount[i] = in.readInt();
            int implementations = in.readInt();
            implSignatures[i] = new String[implementations];
            for (int j = 0; j < implementations; j++) {
                String signature = in.readString();
                Implementation impl = in.readParcelable(es.bsc.mobile.runtime.types.Implementation.class.getClassLoader());
                coreElements[i].add(impl);
            }

        }
    }

    private LinkedList<Implementation> loadCoreElement(Context context, java.lang.reflect.Method m, int ceId) {
        LinkedList<Implementation> impls = new LinkedList<Implementation>();
        String methodName = m.getName();
        String methodSignature = Method.getSignature(m);
        ceSignatures[ceId] = methodSignature;
        es.bsc.mobile.annotations.CoreElement ceAnnot = m.getAnnotation(es.bsc.mobile.annotations.CoreElement.class);

        int implementationCount = ceAnnot.methods().length + ceAnnot.openclKernels().length;
        implSignatures[ceId] = new String[implementationCount];

        int implId = 0;
        for (JavaMethod mAnnot : ceAnnot.methods()) {
            String declaringClass = mAnnot.declaringClass();
            Constraints ctrs = new Constraints(mAnnot.constraints());
            if (ctrs.processorCoreCount() == 0) {
                ctrs.setProcessorCoreCount(1);
            }
            Method method = new Method(context, ceId, implId, methodName, declaringClass, ctrs);
            implSignatures[ceId][implId] = method.completeSignature(methodSignature);
            impls.add(method);
            implId++;
        }

        for (OpenCL oclAnnot : ceAnnot.openclKernels()) {
            String program = oclAnnot.kernel();
            Class<?> resultType = m.getReturnType();
            String[] resultSize = oclAnnot.resultSize();
            String[] workload = oclAnnot.workloadSize();
            String[] localSize = oclAnnot.localSize();
            String[] readOffset = oclAnnot.offset();
            String[] offset;
            if (readOffset.length == workload.length) {
                offset = readOffset;
            } else {
                offset = new String[workload.length];
                int i = 0;
                for (; i < offset.length && i < readOffset.length; i++) {
                    offset[i] = readOffset[i];
                }
                for (; i < offset.length; i++) {
                    offset[i] = "0";
                }
            }
            Kernel kernel = new Kernel(context, ceId, implId, program, methodName, resultType, resultSize, workload, localSize, offset);
            implSignatures[ceId][implId] = kernel.completeSignature(methodSignature);
            impls.add(kernel);
            implId++;
        }

        return impls;
    }

    public int getCoreCount() {
        return coreElements.length;
    }

    public String getCoreSignature(int coreIdx) {
        return ceSignatures[coreIdx];

    }

    public LinkedList<Implementation> getCoreImplementations(int coreIdx) {
        return coreElements[coreIdx];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        int coreCount = coreElements.length;
        out.writeInt(coreCount);
        for (int i = 0; i < coreCount; i++) {
            int implementations = coreElements[i].size();
            out.writeString(ceSignatures[i]);
            out.writeInt(coreParamsCount[i]);
            out.writeInt(implementations);
            for (int j = 0; j < implementations; j++) {
                out.writeString(implSignatures[i][j]);
                out.writeParcelable(coreElements[i].get(j), 0);
            }
        }
    }

    public int getParamsCount(int ceiCE) {
        return coreParamsCount[ceiCE];
    }

    public String[] getAllSignatures() {
        LinkedList<String> signatures = new LinkedList<String>();
        signatures.addAll(Arrays.asList(ceSignatures));
        for (String[] impls : implSignatures) {
            for (String sign : impls) {
                if (sign != null) {
                    signatures.addAll(Arrays.asList(sign));
                }
            }
        }
        String[] signArray = new String[signatures.size()];
        int i = 0;
        for (String signature : signatures) {
            signArray[i++] = signature;
        }
        return signArray;
    }
}
