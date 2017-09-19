package es.bsc.mobile.apps.ece;

import org.jocl.cl_mem;

public class Operations {

    public static byte[] initialized(int[] xdims, float[] x, int[] conv1dims, float[] conv1, int[] conv2dims, float[] conv2, int[] fc1dims, float[] fc1, int[] fc2dims, float[] fc2, int[] adims, int[] bdims, int[] cdims, int[] ddims, int[] ddims2, int[] edims, int[] fdims) {
        return new byte[0];
    }

    public static float[] conv_forward_3D_1channel(float[] x, int[] xDims, float[] w, int[] wDims, int[] outDims) {
        cl_mem xDevice = OpenCL.addInput(x);
        cl_mem xDimsDevice = OpenCL.addInput(xDims);
        cl_mem wDevice = OpenCL.addInput(w);
        cl_mem wDimsDevice = OpenCL.addInput(wDims);
        cl_mem outDimsDevice = OpenCL.addInput(outDims);
        float[] out = new float[outDims[0] * outDims[1] * outDims[2] * outDims[3]];
        cl_mem outDevice = OpenCL.addOutput(out);

        OpenCL.launchKernel("conv_forward_3D_1channel", new long[]{outDims[0] * 256}, new long[]{256}, new long[]{0}, xDevice, xDimsDevice, wDevice, wDimsDevice, outDimsDevice, outDevice);

        OpenCL.retrieveData(outDevice, out);

        OpenCL.releaseData(xDevice);
        OpenCL.releaseData(xDimsDevice);
        OpenCL.releaseData(wDevice);
        OpenCL.releaseData(wDimsDevice);
        OpenCL.releaseData(outDimsDevice);
        OpenCL.releaseData(outDevice);

        return out;
    }

    public static float[] conv_forward_valid(float[] x, int[] xDims, float[] w, int[] wDims, int[] outDims) {
        cl_mem xDevice = OpenCL.addInput(x);
        cl_mem xDimsDevice = OpenCL.addInput(xDims);
        cl_mem wDevice = OpenCL.addInput(w);
        cl_mem wDimsDevice = OpenCL.addInput(wDims);
        cl_mem outDimsDevice = OpenCL.addInput(outDims);
        float[] out = new float[outDims[0] * outDims[1] * outDims[2] * outDims[3]];
        cl_mem outDevice = OpenCL.addOutput(out);

        OpenCL.launchKernel("conv_forward_valid", new long[]{outDims[0] * 256}, new long[]{256}, new long[]{0}, xDevice, xDimsDevice, wDevice, wDimsDevice, outDimsDevice, outDevice);

        OpenCL.retrieveData(outDevice, out);

        OpenCL.releaseData(xDevice);
        OpenCL.releaseData(xDimsDevice);
        OpenCL.releaseData(wDevice);
        OpenCL.releaseData(wDimsDevice);
        OpenCL.releaseData(outDimsDevice);
        OpenCL.releaseData(outDevice);

        return out;
    }

    public static float[] average_pool(float[] x, int[] xDims, int poolSize, int[] outDims) {
        float[] out = new float[outDims[0] * outDims[1] * outDims[2] * outDims[3]];
        cl_mem xDevice = OpenCL.addInput(x);
        cl_mem xDimsDevice = OpenCL.addInput(xDims);
        cl_mem outDimsDevice = OpenCL.addInput(outDims);
        cl_mem outDevice = OpenCL.addOutput(out);

        OpenCL.launchKernel("average_pool", new long[]{256}, new long[]{256}, new long[]{0}, xDevice, xDimsDevice, poolSize, outDimsDevice, outDevice);

        OpenCL.retrieveData(outDevice, out);

        OpenCL.releaseData(xDevice);
        OpenCL.releaseData(xDimsDevice);
        OpenCL.releaseData(outDimsDevice);
        OpenCL.releaseData(outDevice);

        return out;
    }

    public static float[] relu2D(float[] x, int[] xDims) {
        cl_mem xDevice = OpenCL.addInput(x);
        cl_mem xDimsDevice = OpenCL.addInput(xDims);
        int limit = xDims[0] * xDims[1];
        float[] out = new float[limit];
        cl_mem outDevice = OpenCL.addOutput(out);

        OpenCL.launchKernel("relu2D", new long[]{xDims[0] * xDims[1]}, new long[]{256}, new long[]{0}, xDevice, xDimsDevice, outDevice);

        OpenCL.retrieveData(outDevice, out);

        OpenCL.releaseData(xDevice);
        OpenCL.releaseData(xDimsDevice);
        OpenCL.releaseData(outDevice);
        return out;
    }

    public static float[] fully_forward(float[] x, int[] xDims, float[] w, int[] wDims, int[] outDims) {
        float[] out = new float[outDims[0] * outDims[1]];

        cl_mem xDevice = OpenCL.addInput(x);
        cl_mem xDimsDevice = OpenCL.addInput(xDims);
        cl_mem wDevice = OpenCL.addInput(w);
        cl_mem wDimsDevice = OpenCL.addInput(wDims);
        cl_mem outDimsDevice = OpenCL.addInput(outDims);
        cl_mem outDevice = OpenCL.addOutput(out);

        OpenCL.launchKernel("fully_forward", new long[]{((outDims[1] + 16 - 1) / 16) * 16, ((outDims[0] + 16 - 1) / 16) * 16}, new long[]{16, 16}, new long[]{0,0}, xDevice, xDimsDevice, wDevice, wDimsDevice, outDimsDevice, outDevice);

        OpenCL.retrieveData(outDevice, out);

        OpenCL.releaseData(xDevice);
        OpenCL.releaseData(xDimsDevice);
        OpenCL.releaseData(wDevice);
        OpenCL.releaseData(wDimsDevice);
        OpenCL.releaseData(outDimsDevice);
        OpenCL.releaseData(outDevice);

        return out;
    }

    public static int[] argmax(float[] f, int[] dims) {

        cl_mem fDevice = OpenCL.addInput(f);
        cl_mem dimsDevice = OpenCL.addInput(dims);
        int[] out = new int[dims[0]];
        cl_mem outDevice = OpenCL.addOutput(out);

        OpenCL.launchKernel("argmax", new long[]{((dims[0] + 256 - 1) / 256) * 256}, new long[]{256}, new long[]{0}, fDevice, dimsDevice, outDevice);

        OpenCL.retrieveData(outDevice, out);

        OpenCL.releaseData(fDevice);
        OpenCL.releaseData(dimsDevice);
        OpenCL.releaseData(outDevice);

        return out;

    }

}
