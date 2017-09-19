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
package es.bsc.comm;

import android.os.StrictMode;
import android.util.Log;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import es.bsc.comm.nio.NIOException;

public class TransferManager extends Thread {

    public static final String LOGGER_TAG = "Communication";

    private static final String PROPERTIES_ERROR = "Error loading properties file";

    private EventManager<?> em;

    public TransferManager() {
        super("TransferManager");
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
    }

    /**
     *
     * Initializes the Transfer manager to use a protocol to open connections.
     *
     * @param eventManagerClassName Classname of the eventManager
     * @param properties Location of the properties files
     * @param mh Handler for the events produced by the connections
     * @throws es.bsc.comm.CommException Error loading or instantiating the
     * eventManager class
     */
    public final void init(String eventManagerClassName, String properties, MessageHandler mh) throws CommException {
        // Name thread
        Thread.currentThread().setName("TransferManager");

        // Get Event Manager for reflection
        try {
            Constructor<?> constrEventManager = Class.forName(eventManagerClassName).getConstructor(MessageHandler.class);
            this.em = (EventManager<?>) constrEventManager.newInstance(mh);
        } catch (ClassNotFoundException e) {
            Log.e(LOGGER_TAG, "Can not find adaptor class " + eventManagerClassName + ".", e);
            throw new NIOException(NIOException.ErrorType.LOADING_EVENT_MANAGER_CLASS, e);
        } catch (NoSuchMethodException e) {
            Log.e(LOGGER_TAG, "Class " + eventManagerClassName + " has no valid constructor.", e);
            throw new NIOException(NIOException.ErrorType.LOADING_EVENT_MANAGER_CLASS, e);
        } catch (InstantiationException e) {
            Log.e(LOGGER_TAG, "Can not instantiate adaptor " + eventManagerClassName + ".", e);
            throw new NIOException(NIOException.ErrorType.LOADING_EVENT_MANAGER_CLASS, e);
        } catch (IllegalAccessException e) {
            Log.e(LOGGER_TAG, "Illegal access on adaptor " + eventManagerClassName + " creation", e);
            throw new NIOException(NIOException.ErrorType.LOADING_EVENT_MANAGER_CLASS, e);
        } catch (IllegalArgumentException e) {
            Log.e(LOGGER_TAG, "Illegal argument on adaptor " + eventManagerClassName + " creation", e);
            throw new NIOException(NIOException.ErrorType.LOADING_EVENT_MANAGER_CLASS, e);
        } catch (InvocationTargetException e) {
            Log.e(LOGGER_TAG, "Wrong target for " + eventManagerClassName + " invocation", e);
            throw new NIOException(NIOException.ErrorType.LOADING_EVENT_MANAGER_CLASS, e);
        } catch (Exception e) {
            Log.e(LOGGER_TAG, e.getMessage(), e);
            throw new NIOException(NIOException.ErrorType.LOADING_EVENT_MANAGER_CLASS, e);
        }

        // Initialize the EventManager
        try {
            em.init(properties);
        } catch (Exception e) {
            Log.e(LOGGER_TAG, PROPERTIES_ERROR, e);
            throw new NIOException(NIOException.ErrorType.EVENT_MANAGER_INIT, e);
        }

        // Initialize the MessageHandler
        try {
            mh.init();
        } catch (Exception e) {
            Log.e(LOGGER_TAG, PROPERTIES_ERROR, e);
            throw new NIOException(NIOException.ErrorType.MESSAGE_HANDLER_INIT, e);
        }
    }

    /**
     *
     * Starts the Transfer Manager with the given properties
     *
     */
    @Override
    public void run() {
        em.run();
    }

    /**
     * Start a new server on the local node
     *
     * @param n Local node representation where to open a connection
     * @throws es.bsc.comm.CommException Exception during the server socket
     * creation. Further details on the exception cause.
     */
    public void startServer(Node n) throws CommException {
        em.startServer(n);
    }

    /**
     * Opens and starts a client connection to a remote node to transfer data
     * through
     *
     * @param node Remote node
     * @return Connection between both nodes
     */
    public Connection startConnection(Node node) {
        return em.startConnection(node);
    }

    /**
     * Shuts down all the communication platform.
     *
     *
     * @param notifyTo Connection to let a remote node know that the node has
     * been shut down.
     */
    public void shutdown(Connection notifyTo) {
        em.shutdown(notifyTo);
    }

}
