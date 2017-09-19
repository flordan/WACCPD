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

import es.bsc.comm.CommException;
import es.bsc.comm.Connection;
import es.bsc.comm.EventManager;
import es.bsc.comm.InternalConnection;
import es.bsc.comm.Node;
import es.bsc.comm.TransferManager;
import es.bsc.comm.nio.event.NIOEvent;
import es.bsc.comm.nio.event.NewTransferEvent;
import es.bsc.comm.nio.NIOException.ErrorType;
import es.bsc.comm.stage.Stage;
import es.bsc.comm.stage.Reception;
import es.bsc.comm.stage.Submission;
import es.bsc.comm.stage.Shutdown;
import es.bsc.comm.stage.Transfer;

import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

// NIO specific transfer
public class NIOConnection implements Connection, InternalConnection {

    protected static final String LOGGER_TAG = TransferManager.LOGGER_TAG;

    private static final HashMap<SocketChannel, NIOConnection> OPEN_CHANNELS = new HashMap<SocketChannel, NIOConnection>();
    private static final LinkedList<NIOConnection> PENDING_ESTABLISH_CONNECTIONS = new LinkedList<NIOConnection>();

    // Socket where transfer is made
    private SocketChannel sc;

    // Status booleans
    private boolean established;
    // To not to overpass the transfer limits
    private boolean paused;

    private boolean closedSocket;
    private boolean closedConnection;

    //Amount of connection drops due to timeout on connect stage
    private int conRetries = 0;

    // Current transfer
    protected Stage currentStage;
    // Queued transfers
    private final LinkedList<Stage> pendingStages;

    // Stored packets for next transfers
    private final LinkedList<ByteBuffer> receiveBuffer;
    private final LinkedList<ByteBuffer> sendBuffer;

    // Connection made to this node
    private final NIONode node;

    protected EventManager em;

    // Start a connection
    public NIOConnection(NIOEventManager ntm, SocketChannel sc, NIONode n) {
        this.em = ntm;
        this.node = n;

        currentStage = null;
        pendingStages = new LinkedList<Stage>();

        receiveBuffer = new LinkedList<ByteBuffer>();
        sendBuffer = new LinkedList<ByteBuffer>();

        established = false;
        closedSocket = false;
        closedConnection = false;

        if (sc != null) {
            registerChannel(sc);
        }
    }

    @Override
    public Node getNode() {
        return node;
    }

    /**
     * Sends a command through the connection
     *
     * @param cmd Command to submit through the connection
     */
    @Override
    public void sendCommand(Object cmd) {
        Transfer t = new Submission(Transfer.Type.COMMAND, cmd);
        em.addEvent(new NewTransferEvent(this, t));
    }

    /**
     * Sends data stored in a file through the connection.
     *
     * @param name Location of the file that will be submitted
     */
    @Override
    public void sendDataFile(String name) {
        Transfer t = new Submission(name);
        em.addEvent(new NewTransferEvent(this, t));
    }

    /**
     * Sends an object through the connection.
     *
     * @param o Object that will be submitted
     */
    @Override
    public void sendDataObject(Object o) {
        Transfer t = new Submission(Transfer.Type.DATA, o);
        em.addEvent(new NewTransferEvent(this, t));
    }

    /**
     * Sends the data stored in a byte arrey through the connection.
     *
     * @param array Data to be transferred through the connection.
     */
    @Override
    public void sendDataArray(byte[] array) {
        Transfer t = new Submission(Transfer.Type.DATA, array);
        em.addEvent(new NewTransferEvent(this, t));
    }

    /**
     * Enables the connection to receive some command or data and notify it
     */
    @Override
    public void receive() {
        Transfer t = new Reception();
        em.addEvent(new NewTransferEvent(this, t));
    }

    /**
     * Enable the connection to receive a command or some data. In case of
     * receiving a data that originally was stored in a file, the data will be
     * saved in a file.
     *
     * @param name Location of the file where data can potentially be saved
     */
    @Override
    public void receive(String name) {
        Reception t = new Reception();
        t.setReceptionDefaultFileName(name);
        em.addEvent(new NewTransferEvent(this, t));
    }

    /**
     * Enables the connection to receive some data and store it as an object
     */
    @Override
    public void receiveDataObject() {
        Reception t = new Reception(Transfer.Destination.OBJECT);
        em.addEvent(new NewTransferEvent(this, t));
    }

    /**
     * Enables the connection to receive some data and store it as a byte array
     */
    @Override
    public void receiveDataArray() {
        Reception t = new Reception(Transfer.Destination.ARRAY);
        em.addEvent(new NewTransferEvent(this, t));
    }

    /**
     * Enables the connection to receive some data and store it in a file
     *
     * @param name Location of the file where to save the received data
     */
    @Override
    public void receiveDataFile(String name) {
        Reception t = new Reception(Transfer.Destination.FILE);
        t.setReceptionDefaultFileName(name);
        em.addEvent(new NewTransferEvent(this, t));
    }

    /**
     * Closes the connection after all the previous ordered transfers (Receives
     * and Transmit) have been processed.
     */
    @Override
    public void finishConnection() {
        Stage t = new Shutdown();
        em.addEvent(new NewTransferEvent(this, t));
    }

    /*
     ************************************
     ************************************
     *********** Internal use ***********
     ************************************
     ************************************
     */
    // Enqueue a new transfer to this connection
    @Override
    public void requestStage(Stage t) {
        pendingStages.add(t);
        if (currentStage == null) {
            handleNextTransfer();
        }
    }

    public void established() {
        established = true;
        handleNextTransfer();
    }

    @Override
    public void receivedPacket(ByteBuffer buffer) {
        receiveBuffer.add(buffer);
        if (currentStage != null) {
            progressCurrentTransfer();
        }
    }

    @Override
    public void lowSendBuffer() {
        if (currentStage != null) {
            progressCurrentTransfer();
        }
    }

    @Override
    public void emptySendBuffer() {
        if (currentStage != null && currentStage.isComplete(receiveBuffer, sendBuffer)) {
            currentStage.notifyCompletion(this, em);
            // Handle the next entry in the queue
            handleNextTransfer();
        }
    }

    @Override
    public void closedChannel() {
        closedSocket = true;
        if (currentStage != null && !paused) {
            currentStage.notifyError(this,
                    em,
                    new NIOException(ErrorType.CLOSED_CONNECTION, new Exception("Channel was already closed for connection " + this.hashCode())));
            handleNextTransfer();
        }
        if (closedConnection) {
            unregisterChannel();
            em.connectionFinished(this);
        }
    }

    // Close this connection
    public void closeConnection() {
        if (!closedSocket) {
            NIOListener.closeSocket(this, sc);
        } else if (!closedConnection) {
            unregisterChannel();
            em.connectionFinished(this);
        }
        closedConnection = true;
        handleNextTransfer();
    }

    private void registerChannel(SocketChannel socket) {
        synchronized (OPEN_CHANNELS) {
            OPEN_CHANNELS.put(socket, this);
        }
        sc = socket;
    }

    protected void unregisterChannel() {
        synchronized (OPEN_CHANNELS) {
            OPEN_CHANNELS.remove(sc);
        }
        sc = null;
    }

    public void replaceChannel(SocketChannel newSocket) {
        synchronized (OPEN_CHANNELS) {
            OPEN_CHANNELS.remove(sc);
            OPEN_CHANNELS.put(newSocket, this);
        }
        sc = newSocket;
    }

    private void pause() {
        paused = true;
        currentStage.pause(this);
    }

    @Override
    public void resume() {
        paused = false;
        startCurrentTransfer();
    }

    private void handleNextTransfer() {
        if (paused || !established) {
            return;
        }
        if (!pendingStages.isEmpty()) {
            // Update the previous and actual states
            currentStage = pendingStages.removeFirst();
            // Check if it is a shutdown, a send or a receive
            if (currentStage.isShutdown()) {
                closeConnection();
            } else {
                startCurrentTransfer();
            }
        } else {
            currentStage = null;
        }
    }

    protected void startCurrentTransfer() {
        if (paused) {
            return;
        }
        try {
            if (currentStage.checkViability(closedSocket, receiveBuffer, sendBuffer)) {
                //The transfer can potentially success. If it is a submission 
                //transfer, the channel is still open to send the data. In 
                //case of a Receive transfer, although the channel is closed,
                //some data can be held in the receive buffer.

                //The transfer is initialized
                try {
                    currentStage.start(this, receiveBuffer, sendBuffer);
                    //And processed as much as possible given the current buffers
                    progressCurrentTransfer();
                } catch (Exception e) {
                    currentStage.notifyError(this, em, new NIOException(ErrorType.CLOSED_CONNECTION, e));
                    handleNextTransfer();
                }
            } else {
                pause();
            }
        } catch (CommException ne) {
            //There is no way to accomplish the transfer. The error has to be notified to the message handler 
            currentStage.notifyError(this, em,
                    new NIOException(ErrorType.CLOSED_CONNECTION, new Exception("Channel was already closed for connection " + this.hashCode())));
            //We try to handle the next transfer in the queue
            handleNextTransfer();
        }
    }

    public void progressCurrentTransfer() {
        if (paused) {
            return;
        }
        try {
            currentStage.progress(this, receiveBuffer, sendBuffer);
            if (currentStage.isComplete(receiveBuffer, sendBuffer)) {
                //The transfer has been completed.
                //The transfer completion is notified
                currentStage.notifyCompletion(this, em);
                //We deal with the next transfer
                handleNextTransfer();
            } else if (closedSocket) {
                //The socket may have been closed and the transfer would be impossible to end
                //Checking viability
                if (!currentStage.checkViability(closedSocket, receiveBuffer, sendBuffer)) {
                    //It won't be possible to finish. 
                    //NOtify the error
                    currentStage.notifyError(this,
                            em,
                            new NIOException(ErrorType.CLOSED_CONNECTION, new Exception("Channel was already closed for connection " + this.hashCode())));
                    //Deal with the following transfer transfer in the queue.
                    handleNextTransfer();
                }
            } else if (!sendBuffer.isEmpty()) {
                NIOListener.changeInterest(this, sc, SelectionKey.OP_WRITE);
            }
        } catch (Exception e) {
            currentStage.notifyError(this, em, new NIOException(ErrorType.CLOSED_CONNECTION, e));
            handleNextTransfer();
        }
    }

    @Override
    public void error(CommException exception) {
        Log.e(LOGGER_TAG, "Processing error on connection " + this.hashCode(), exception);
        NIOException ne = (NIOException) exception;
        if ((ne.getError() == ErrorType.FINISHING_CONNECTION)
                || (ne.getError() == ErrorType.STARTING_CONNECTION)
                || (ne.getError() == ErrorType.RESTARTING_CONNECTION)) {

            conRetries++;
            if (conRetries < NIOProperties.MAX_RETRIES) {
                if (ne.getCause() instanceof SocketTimeoutException) {
                    reestablishConnection();
                } else {
                    PENDING_ESTABLISH_CONNECTIONS.add(this);
                }
            } else {
                closedSocket = true;
                unregisterChannel();
                handleNextTransfer();
            }
        } else {
            currentStage.notifyError(this, em, exception);
            handleNextTransfer();
        }
    }

    public static void establishPendingConnections() {
        for (NIOConnection nc : PENDING_ESTABLISH_CONNECTIONS) {
            nc.reestablishConnection();
        }
        PENDING_ESTABLISH_CONNECTIONS.clear();
    }

    private void reestablishConnection() {
        NIOListener.restartConnection(this, node);
    }

    public static NIOConnection getConnection(SocketChannel sc) {
        synchronized (OPEN_CHANNELS) {
            return OPEN_CHANNELS.get(sc);
        }
    }

    public static Collection<SocketChannel> getAllSockets() {
        return OPEN_CHANNELS.keySet();
    }

    public static void abortPendingConnections() {
        for (NIOConnection nc : PENDING_ESTABLISH_CONNECTIONS) {
            OPEN_CHANNELS.remove(nc.sc);
        }
        PENDING_ESTABLISH_CONNECTIONS.clear();
    }

    public static boolean areAliveConnections() {
        return !OPEN_CHANNELS.isEmpty();
    }

    public LinkedList<ByteBuffer> getSendBuffer() {
        return sendBuffer;
    }

    public Stage getCurrentTransfer() {
        return currentStage;
    }

    public SocketChannel getSocket() {
        return sc;
    }
}
