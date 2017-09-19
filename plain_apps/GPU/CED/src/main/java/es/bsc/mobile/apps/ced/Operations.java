package es.bsc.mobile.apps.ced;

import java.io.DataInputStream;
import java.io.IOException;
import org.jocl.cl_mem;

public class Operations {

    public static byte[] initialized(byte[] data, float[][] gaus, int[][] sobx, int[][] soby) {
        return new byte[]{0};
    }

    private static float[] flatten(float[][] in) {
        int size = 0;
        for (float[] f : in) {
            size += f.length;
        }
        float[] flat = new float[size];
        int pos = 0;
        for (float[] f : in) {
            for (float ff : f) {
                flat[pos++] = ff;
            }
        }
        return flat;
    }

    private static int[] flatten(int[][] in) {
        int size = 0;
        for (int[] f : in) {
            size += f.length;
        }
        int[] flat = new int[size];
        int pos = 0;
        for (int[] f : in) {
            for (int ff : f) {
                flat[pos++] = ff;
            }
        }
        return flat;
    }

    public static byte[] gaussian(final byte[] data, final float[][] gaus, final int rows, final int cols) {

        cl_mem dataDevice = OpenCL.addInput(data);
        cl_mem gausDevice = OpenCL.addInput(flatten(gaus));

        byte[] out = new byte[rows * cols];
        cl_mem outDevice = OpenCL.addOutput(out);

        OpenCL.launchKernel("gaussian", new long[]{rows - 2, cols - 2}, new long[]{16, 16}, new long[]{1, 1}, dataDevice, gausDevice, rows, cols, outDevice);

        OpenCL.retrieveData(outDevice, out);

        OpenCL.releaseData(dataDevice);
        OpenCL.releaseData(gausDevice);
        OpenCL.releaseData(outDevice);

        return out;
    }

    /*
    public static byte[] sobel(final byte[] data, final int rows, final int cols, final int[][] sobx, final int[][] soby) {
        byte[] theta = new byte[2 * rows * cols];
        float PI = 3.14159265f;
        int thetaOffset = rows;
        for (int row = 1; row < rows - 1; row++) {
            for (int col = 1; col < cols - 1; col++) {
                double sumx = 0;
                double sumy = 0;
                double angle = 0;

                // find x and y derivatives
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        sumx += sobx[i][j] * (0xFF & data[(i + row + -1) * cols + (j + col + -1)]);
                        sumy += soby[i][j] * (0xFF & data[(i + row + -1) * cols + (j + col + -1)]);
                    }
                }

                // The output is now the square root of their squares, but they are
                // constrained to 0 <= value <= 255. Note that hypot is a built in function
                // defined as: hypot(x,y) = sqrt(x*x, y*y).
                theta[(row + thetaOffset) * cols + col] = (byte) Math.min(255, Math.max(0, Math.hypot(sumx, sumy)));
                // Compute the direction angle theta in radians
                // atan2 has a range of (-PI, PI) degrees
                angle = Math.atan2(sumy, sumx);

                // If the angle is negative,
                // shift the range to (0, 2PI) by adding 2PI to the angle,
                // then perform modulo operation of 2PI
                if (angle < 0) {
                    angle = (angle + 2 * PI) % (2 * PI);
                }

                // Round the angle to one of four possibilities: 0, 45, 90, 135 degrees
                // then store it in the theta buffer at the proper position
                if (angle <= PI / 8) {
                    theta[row * cols + col] = 0;
                } else if (angle <= 3 * PI / 8) {
                    theta[row * cols + col] = 45;
                } else if (angle <= 5 * PI / 8) {
                    theta[row * cols + col] = 90;
                } else if (angle <= 7 * PI / 8) {
                    theta[row * cols + col] = (byte) 135;
                } else if (angle <= 9 * PI / 8) {
                    theta[row * cols + col] = 0;
                } else if (angle <= 11 * PI / 8) {
                    theta[row * cols + col] = 45;
                } else if (angle <= 13 * PI / 8) {
                    theta[row * cols + col] = 90;
                } else if (angle <= 15 * PI / 8) {
                    theta[row * cols + col] = (byte) 135;
                } else {
                    theta[row * cols + col] = 0; // (angle <= 16*PI/8)
                }
            }
        }
        return theta;
    }
    *///*
    public static byte[] sobel(final byte[] data, final int rows, final int cols, final int[][] sobx, final int[][] soby) {
        cl_mem dataDevice = OpenCL.addInput(data);
        cl_mem sobxDevice = OpenCL.addInput(flatten(sobx));
        cl_mem sobyDevice = OpenCL.addInput(flatten(soby));
        byte[] out = new byte[2 * rows * cols];
        cl_mem outDevice = OpenCL.addOutput(out);

        OpenCL.launchKernel("sobel", new long[]{rows - 2, cols - 2}, new long[]{8, 8}, new long[]{1, 1}, dataDevice, rows, cols, sobxDevice, sobyDevice, outDevice);

        OpenCL.retrieveData(outDevice, out);
        OpenCL.releaseData(dataDevice);
        OpenCL.releaseData(sobxDevice);
        OpenCL.releaseData(sobyDevice);
        OpenCL.releaseData(outDevice);
        return out;
    }
    /*
    *///

    public static byte[] nonMaxSupp(final byte[] data, final int rows, final int cols) {
        cl_mem dataDevice = OpenCL.addInput(data);
        byte[] out = new byte[rows * cols];
        cl_mem outDevice = OpenCL.addOutput(out);

        OpenCL.launchKernel("nonMaxSupp", new long[]{rows - 2, cols - 2}, new long[]{16, 16}, new long[]{1, 1}, dataDevice, rows, cols, outDevice);

        OpenCL.retrieveData(outDevice, out);

        OpenCL.releaseData(dataDevice);
        OpenCL.releaseData(outDevice);
        return out;
    }

    public static byte[] hysteresis(final byte[] data, final int rows, final int cols) {
        cl_mem dataDevice = OpenCL.addInput(data);
        byte[] out = new byte[rows * cols];
        cl_mem outDevice = OpenCL.addOutput(out);

        OpenCL.launchKernel("hysteresis", new long[]{rows - 2, cols - 2}, new long[]{16, 16}, new long[]{1, 1}, dataDevice, rows, cols, outDevice);

        OpenCL.retrieveData(outDevice, out);

        OpenCL.releaseData(dataDevice);
        OpenCL.releaseData(outDevice);
        return out;
    }

    public static int validate(byte[] frame, int rows, int cols, DataInputStream dis) throws IOException {
        int count_error = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                byte pix = dis.readByte();
                if (frame[row * cols + col] != pix) {
                    count_error++;
                }
            }
        }
        return count_error;
    }
}
