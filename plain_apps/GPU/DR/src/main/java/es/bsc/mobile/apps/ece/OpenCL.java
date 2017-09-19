package es.bsc.mobile.apps.ece;

import java.io.IOException;
import java.io.InputStream;
import org.jocl.CL;
import static org.jocl.CL.CL_DEVICE_TYPE_ACCELERATOR;
import static org.jocl.CL.CL_DEVICE_TYPE_CPU;
import static org.jocl.CL.CL_DEVICE_TYPE_CUSTOM;
import static org.jocl.CL.CL_DEVICE_TYPE_GPU;
import static org.jocl.CL.CL_FALSE;
import static org.jocl.CL.CL_MEM_READ_ONLY;
import static org.jocl.CL.CL_SUCCESS;
import static org.jocl.CL.CL_TRUE;
import static org.jocl.CL.clGetDeviceIDs;
import static org.jocl.CL.clGetPlatformInfo;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_device_id;
import org.jocl.cl_event;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;

/**
 *
 * @author flordan
 */
public class OpenCL {

    private static cl_device_id device = null;
    private static cl_context context = null;
    private static cl_command_queue queue = null;
    private static cl_program program = null;

    static {
        CL.setExceptionsEnabled(CL_TRUE);

        // Obtain the number of platforms
        int numPlatforms[] = new int[1];
        CL.clGetPlatformIDs(0, null, numPlatforms);
        // Obtain the platform IDs
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms[0]];
        CL.clGetPlatformIDs(platforms.length, platforms, null);

        // Collect all devices of all platforms
        for (int i = 0; i < platforms.length; i++) {
            cl_platform_id platformId = platforms[i];
            int numDevices[] = new int[1];
            CL.setExceptionsEnabled(CL_FALSE);

            int ret;
            int[] devicePerType = new int[4];
            int deviceCount = 0;

            ret = clGetDeviceIDs(platformId, CL.CL_DEVICE_TYPE_CPU, 0, null, numDevices);
            if (ret == CL_SUCCESS) {
                devicePerType[0] = numDevices[0];
                deviceCount += numDevices[0];
            }

            ret = clGetDeviceIDs(platformId, CL.CL_DEVICE_TYPE_GPU, 0, null, numDevices);
            if (ret == CL_SUCCESS) {
                devicePerType[1] = numDevices[0];
                deviceCount += numDevices[0];
            }

            ret = clGetDeviceIDs(platformId, CL.CL_DEVICE_TYPE_ACCELERATOR, 0, null, numDevices);
            if (ret == CL_SUCCESS) {
                devicePerType[2] = numDevices[0];
                deviceCount += numDevices[0];
            }

            ret = clGetDeviceIDs(platformId, CL.CL_DEVICE_TYPE_CUSTOM, 0, null, numDevices);
            if (ret == CL_SUCCESS) {
                devicePerType[3] = numDevices[0];
                deviceCount += numDevices[0];
            }

            cl_device_id[] devices = new cl_device_id[deviceCount];
            cl_context[] contexts = new cl_context[deviceCount];
            cl_command_queue[] queues = new cl_command_queue[deviceCount];
            CL.setExceptionsEnabled(CL_TRUE);
            int totalDevices = 0;
            if (devicePerType[0] > 0) {
                cl_device_id devicesArray[] = new cl_device_id[devicePerType[0]];
                clGetDeviceIDs(platformId, CL_DEVICE_TYPE_CPU, devicePerType[0], devicesArray, null);
                for (cl_device_id deviceId : devicesArray) {
                    devices[totalDevices] = deviceId;
                    int[] errcode_ret = new int[1];
                    contexts[totalDevices] = CL.clCreateContext(null, 1, new cl_device_id[]{deviceId}, null, null, errcode_ret);
                    queues[totalDevices] = CL.clCreateCommandQueue(contexts[totalDevices], devices[totalDevices], CL.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE | CL.CL_QUEUE_PROFILING_ENABLE, errcode_ret);
                    totalDevices++;
                }
            }

            if (devicePerType[1] > 0) {
                cl_device_id devicesArray[] = new cl_device_id[devicePerType[1]];
                clGetDeviceIDs(platformId, CL_DEVICE_TYPE_GPU, devicePerType[1], devicesArray, null);
                for (cl_device_id deviceId : devicesArray) {
                    devices[totalDevices] = deviceId;
                    int[] errcode_ret = new int[1];
                    contexts[totalDevices] = CL.clCreateContext(null, 1, new cl_device_id[]{deviceId}, null, null, errcode_ret);
                    queues[totalDevices] = CL.clCreateCommandQueue(contexts[totalDevices], devices[totalDevices], CL.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE | CL.CL_QUEUE_PROFILING_ENABLE, errcode_ret);
                    totalDevices++;
                }
            }

            if (devicePerType[2] > 0) {
                cl_device_id devicesArray[] = new cl_device_id[devicePerType[2]];
                clGetDeviceIDs(platformId, CL_DEVICE_TYPE_ACCELERATOR, devicePerType[2], devicesArray, null);
                for (cl_device_id deviceId : devicesArray) {
                    devices[totalDevices] = deviceId;
                    int[] errcode_ret = new int[1];
                    contexts[totalDevices] = CL.clCreateContext(null, 1, new cl_device_id[]{deviceId}, null, null, errcode_ret);
                    queues[totalDevices] = CL.clCreateCommandQueue(contexts[totalDevices], devices[totalDevices], CL.CL_QUEUE_PROFILING_ENABLE, errcode_ret);
                    totalDevices++;
                }
            }

            if (devicePerType[3] > 0) {
                cl_device_id devicesArray[] = new cl_device_id[devicePerType[3]];
                clGetDeviceIDs(platformId, CL_DEVICE_TYPE_CUSTOM, devicePerType[3], devicesArray, null);
                for (cl_device_id deviceId : devicesArray) {
                    devices[totalDevices] = deviceId;
                    int[] errcode_ret = new int[1];
                    contexts[totalDevices] = CL.clCreateContext(null, 1, new cl_device_id[]{deviceId}, null, null, errcode_ret);
                    queues[totalDevices] = CL.clCreateCommandQueue(contexts[totalDevices], devices[totalDevices], CL.CL_QUEUE_PROFILING_ENABLE, errcode_ret);
                    totalDevices++;
                }
            }

            device = devices[0];
            context = contexts[0];
            queue = queues[0];
        }

    }

    public static void loadKernels(InputStream input) {
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
        program = createProgramFromSource(code);
    }

    private static cl_program createProgramFromSource(String sourceCode) {
        int[] errCode = new int[1];
        cl_program program = CL.clCreateProgramWithSource(context, 1, new String[]{sourceCode}, new long[]{sourceCode.length()}, errCode);
        CL.clBuildProgram(program, 1, new cl_device_id[]{device}, null, null, errCode);
        return program;
    }

    public static cl_mem addInput(int[] data) {
        int[] errCode = new int[1];
        cl_mem buffer = CL.clCreateBuffer(context, CL_MEM_READ_ONLY, Sizeof.cl_int * data.length, null, errCode);
        CL.clEnqueueWriteBuffer(queue, buffer, CL_TRUE, 0, Sizeof.cl_int * data.length, Pointer.to(data), 0, null, null);
        return buffer;
    }

    public static cl_mem addInput(float[] data) {
        int[] errCode = new int[1];
        cl_mem buffer = CL.clCreateBuffer(context, CL_MEM_READ_ONLY, Sizeof.cl_float * data.length, null, errCode);
        CL.clEnqueueWriteBuffer(queue, buffer, CL_TRUE, 0, Sizeof.cl_float * data.length, Pointer.to(data), 0, null, null);
        return buffer;
    }

    public static cl_mem addOutput(int[] data) {
        int[] errCode = new int[1];
        return CL.clCreateBuffer(context, CL_MEM_READ_ONLY, Sizeof.cl_int * data.length, null, errCode);
    }

    public static cl_mem addOutput(float[] data) {
        int[] errCode = new int[1];
        return CL.clCreateBuffer(context, CL_MEM_READ_ONLY, Sizeof.cl_float * data.length, null, errCode);
    }

    public static void releaseData(cl_mem mem) {
        CL.clReleaseMemObject(mem);
    }

    public static void launchKernel(String methodName, long[] workload, long[] workgroup, long[] offset, Object... params) {
        int[] errout = new int[1];
        cl_kernel kernel = CL.clCreateKernel(program, methodName, errout);
        int paramId = 0;
        for (Object param : params) {
            if (param instanceof cl_mem) {
                CL.clSetKernelArg(kernel, paramId, Sizeof.cl_mem, Pointer.to((cl_mem) param));
            } else if (param instanceof java.lang.Byte) {
                CL.clSetKernelArg(kernel, paramId, Sizeof.cl_char, Pointer.to(new byte[]{((Byte) param)}));
            } else if (param instanceof java.lang.Character) {
                CL.clSetKernelArg(kernel, paramId, Sizeof.cl_short, Pointer.to(new char[]{((Character) param)}));
            } else if (param instanceof java.lang.Short) {
                CL.clSetKernelArg(kernel, paramId, Sizeof.cl_short, Pointer.to(new short[]{((Short) param)}));
            } else if (param instanceof java.lang.Integer) {
                CL.clSetKernelArg(kernel, paramId, Sizeof.cl_int, Pointer.to(new int[]{((Integer) param)}));
            } else if (param instanceof java.lang.Long) {
                CL.clSetKernelArg(kernel, paramId, Sizeof.cl_long, Pointer.to(new long[]{((Long) param)}));
            } else if (param instanceof java.lang.Float) {
                CL.clSetKernelArg(kernel, paramId, Sizeof.cl_float, Pointer.to(new float[]{((Float) param)}));
            } else if (param instanceof java.lang.Double) {
                CL.clSetKernelArg(kernel, paramId, Sizeof.cl_double, Pointer.to(new double[]{((Double) param)}));
            }
            paramId++;
        }
        CL.clEnqueueNDRangeKernel(queue, kernel, workload.length, offset, workload, workgroup, 0, null, null);
        CL.clFinish(queue);
        CL.clReleaseKernel(kernel);
    }

    public static void retrieveData(cl_mem outDevice, float[] out) {
        CL.clEnqueueReadBuffer(queue, outDevice, CL_TRUE, 0, Sizeof.cl_float * out.length, Pointer.to(out), 0, null, null);
    }

    public static void retrieveData(cl_mem outDevice, int[] out) {
        CL.clEnqueueReadBuffer(queue, outDevice, CL_TRUE, 0, Sizeof.cl_int * out.length, Pointer.to(out), 0, null, null);
    }
}
