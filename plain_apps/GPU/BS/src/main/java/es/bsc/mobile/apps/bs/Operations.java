package es.bsc.mobile.apps.bs;

import org.jocl.cl_mem;

public class Operations {

    public static float[] processTile(float[] in, int inRows, int inCols, int outRows, int outCols, int tileIdI, int tileIdJ, int tileSize) {

        cl_mem inDevice = OpenCL.addInput(in);
        float[] out = new float[tileSize * tileSize * 3];
        cl_mem outDevice = OpenCL.addOutput(out);

        OpenCL.launchKernel("processTile", new long[]{8, 8}, new long[]{8, 8}, new long[]{0, 0}, inDevice, inRows, inCols, outRows, outCols, tileIdI, tileIdJ, tileSize, outDevice);

        OpenCL.retrieveData(outDevice, out);

        OpenCL.releaseData(inDevice);
        OpenCL.releaseData(outDevice);

        return out;
    }

    public static boolean verify(float[] in, float[][][] out, int inRows, int inCols, int outRows, int outCols, int tileSize) {
        float[] gold = new float[outRows * outCols * 3];
        Utils.BezierCPU(in, gold, inRows, inCols, outRows, outRows);
        double error = compareOutputs(out, gold, outRows, outRows, tileSize);
        return (error < 1e-6);
    }

    private static double compareOutputs(float[][][] outp, float[] outpCPU, int RESOLUTIONI, int RESOLUTIONJ, int tileSize) {
        double sum_delta2 = 0;
        double sum_ref2 = 0;

        for (int i = 0; i < RESOLUTIONI; i++) {
            for (int j = 0; j < RESOLUTIONJ; j++) {

                int tileI = i / tileSize;
                int tileJ = j / tileSize;
                int tileRow = i % tileSize;
                int tileCol = j % tileSize;

                sum_delta2 += Math.abs(outp[tileI][tileJ][(tileRow * tileSize + tileCol) * 3] - outpCPU[(i * RESOLUTIONJ + j) * 3]);
                sum_ref2 += Math.abs(outp[tileI][tileJ][(tileRow * tileSize + tileCol) * 3]);
                sum_delta2 += Math.abs(outp[tileI][tileJ][(tileRow * tileSize + tileCol) * 3 + 1] - outpCPU[(i * RESOLUTIONJ + j) * 3 + 1]);
                sum_ref2 += Math.abs(outp[tileI][tileJ][(tileRow * tileSize + tileCol) * 3 + 1]);
                sum_delta2 += Math.abs(outp[tileI][tileJ][(tileRow * tileSize + tileCol) * 3 + 2] - outpCPU[(i * RESOLUTIONJ + j) * 3 + 2]);
                sum_ref2 += Math.abs(outp[tileI][tileJ][(tileRow * tileSize + tileCol) * 3 + 2]);

            }
        }
        return (sum_delta2 / sum_ref2);
    }
}
