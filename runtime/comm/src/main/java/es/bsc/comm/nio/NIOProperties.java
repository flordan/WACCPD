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
package es.bsc.comm.nio;

import android.util.Log;

import es.bsc.comm.TransferManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class NIOProperties {

    private static final String LOGGER_TAG = TransferManager.LOGGER_TAG;

    public static final String MAX_BUFFERED_PACKETS_NAME = "MAX_BUFFERED_PACKETS";
    private static final int DEFAULT_MAX_BUFFERED_PACKETS = 10;
    public static int MAX_BUFFERED_PACKETS;

    public static final String MIN_BUFFERED_PACKETS_NAME = "MIN_BUFFERED_PACKETS";
    private static final int DEFAULT_MIN_BUFFERED_PACKETS = 5;
    public static int MIN_BUFFERED_PACKETS;

    public static final String PACKET_SIZE_NAME = "PACKET_SIZE";
    private static final int DEFAULT_PACKET_SIZE = 8192;
    public static int PACKET_SIZE;

    public static final String NETWORK_BUFFER_SIZE_NAME = "NETWORK_BUFFER_SIZE";
    private static final int DEFAULT_NETWORK_BUFFER_SIZE = 81920;
    public static int NETWORK_BUFFER_SIZE;

    public static final String MAX_SENDS_NAME = "MAX_SEND";
    private static final int DEFAULT_MAX_SENDS = Integer.MAX_VALUE;
    public static int MAX_SENDS;

    public static final String MAX_RECEIVES_NAME = "MAX_RECEIVES";
    private static final int DEFAULT_MAX_RECEIVES = Integer.MAX_VALUE;
    public static int MAX_RECEIVES;

    public static final String MAX_RETRIES_NAME = "MAX_RETRIES";
    private static final int DEFAULT_MAX_RETRIES = 5;
    public static int MAX_RETRIES;

    private static final String PROP_FILE_NAME = "nio.cfg";

    public static void importProperties() {
        System.getProperty("MAX_PACKETS_PER_TRANSFER");
    }

    public static void importProperties(String loc) {

        if (loc == null) { // Default parameters
            MAX_BUFFERED_PACKETS = DEFAULT_MAX_BUFFERED_PACKETS;
            PACKET_SIZE = DEFAULT_PACKET_SIZE;
            MIN_BUFFERED_PACKETS = DEFAULT_MIN_BUFFERED_PACKETS;
            NETWORK_BUFFER_SIZE = DEFAULT_NETWORK_BUFFER_SIZE;

            MAX_SENDS = DEFAULT_MAX_SENDS;
            MAX_RECEIVES = DEFAULT_MAX_RECEIVES;
            MAX_RETRIES = DEFAULT_MAX_RETRIES;
        } else { // File parameters
            FileInputStream inputStream;
            String path = loc + File.separator + PROP_FILE_NAME;
            try {
                inputStream = new FileInputStream(path);
            } catch (FileNotFoundException e) {
                Log.e(LOGGER_TAG, "Can not find properties file " + path);
                return;
            }

            try {
                Properties prop = new Properties();
                prop.load(inputStream);
                MAX_BUFFERED_PACKETS = Integer.parseInt(prop.getProperty(MAX_BUFFERED_PACKETS_NAME));
                MIN_BUFFERED_PACKETS = Integer.parseInt(prop.getProperty(MIN_BUFFERED_PACKETS_NAME));
                PACKET_SIZE = Integer.parseInt(prop.getProperty(PACKET_SIZE_NAME));
                NETWORK_BUFFER_SIZE = Integer.parseInt(prop.getProperty(NETWORK_BUFFER_SIZE_NAME));

                MAX_SENDS = Integer.parseInt(prop.getProperty(MAX_SENDS_NAME));
                MAX_RECEIVES = Integer.parseInt(prop.getProperty(MAX_RECEIVES_NAME));
                MAX_RETRIES = Integer.parseInt(prop.getProperty(MAX_RETRIES_NAME));
            } catch (IOException e) {
                Log.e(LOGGER_TAG, "Error loading properties", e);
            }
            try {
                inputStream.close();
            } catch (IOException ie) {
                Log.e(LOGGER_TAG, "Error closing stream", ie);
            }
        }
    }

    public static void printProperties() {
        Log.d(LOGGER_TAG, "MAX_BUFFERED_PACKETS = " + MAX_BUFFERED_PACKETS);
        Log.d(LOGGER_TAG, "MIN_BUFFERED_PACKETS = " + MIN_BUFFERED_PACKETS);
        Log.d(LOGGER_TAG, "PACKET_SIZE = " + PACKET_SIZE);
        Log.d(LOGGER_TAG, "NETWORK_BUFFER_SIZE = " + NETWORK_BUFFER_SIZE);
        Log.d(LOGGER_TAG, MAX_BUFFERED_PACKETS_NAME + " = " + MAX_BUFFERED_PACKETS);
        Log.d(LOGGER_TAG, MIN_BUFFERED_PACKETS_NAME + " = " + MIN_BUFFERED_PACKETS);
        Log.d(LOGGER_TAG, PACKET_SIZE_NAME + " = " + PACKET_SIZE);
        Log.d(LOGGER_TAG, NETWORK_BUFFER_SIZE_NAME + " = " + NETWORK_BUFFER_SIZE);
        Log.d(LOGGER_TAG, MAX_SENDS_NAME + " = " + MAX_SENDS);
        Log.d(LOGGER_TAG, MAX_RECEIVES_NAME + " = " + MAX_RECEIVES);
        Log.d(LOGGER_TAG, MAX_RETRIES_NAME + " = " + MAX_RETRIES);
    }

    private NIOProperties() {
        throw new RuntimeException("Class NIOProperties can not be instantiated.");
    }

}
