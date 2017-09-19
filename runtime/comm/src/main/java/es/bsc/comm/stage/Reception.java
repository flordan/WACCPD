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
package es.bsc.comm.stage;

import android.util.Log;
import es.bsc.comm.CommException;
import es.bsc.comm.Connection;
import es.bsc.comm.EventManager;
import es.bsc.comm.InternalConnection;
import es.bsc.comm.nio.NIOException;
import es.bsc.comm.nio.NIOProperties;
import static es.bsc.comm.stage.Stage.LOGGER_TAG;
import es.bsc.comm.util.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import java.util.LinkedList;
import java.util.List;

public class Reception extends Transfer {

    private static int tokens = NIOProperties.MAX_RECEIVES;
    private static LinkedList<InternalConnection> pausedConnections = new LinkedList<InternalConnection>();

    // To know whether the destination has been initialized or not
    private boolean destInit;
    private OutputStream streamOut;
    private boolean hasToken = false;
    private Token token;

    // New file/object to receive
    public Reception() {
        super(true);
        remainingSize = 0;
        destInit = false;
    }

    public Reception(boolean notifyErrors) {
        super(notifyErrors);
        remainingSize = 0;
        destInit = false;
    }

    // New object to receive
    public Reception(Destination destination) {
        super(true);
        this.type = Type.DATA;
        this.destination = destination;
        this.destInit = true;
    }

    public Reception(Destination destination, boolean notifyErrors) {
        super(notifyErrors);
        this.type = Type.DATA;
        this.destination = destination;
        this.destInit = true;
    }

    // New unexpected command to receive
    public Reception(Type type) {
        super(true);
        this.type = type;
        this.destination = Destination.OBJECT;
        destInit = false;
    }

    public Reception(Type type, boolean notifyErrors) {
        super(notifyErrors);
        this.type = type;
        this.destination = Destination.OBJECT;
        destInit = false;
    }

    // Initialize the new transfer
    public void initTransfer(Token t) throws FileNotFoundException {
        byte[] header = t.get(HEADER_SIZE);
        ByteBuffer bb = ByteBuffer.wrap(header);
        long size = bb.getLong();
        if (destInit == false) {
            destInit = true;
            type = Type.values()[bb.getInt()];
            destination = Destination.values()[bb.getInt()];
        }
        setSize(size);
        openStream();
    }

    // Open the receive stream
    private void openStream() throws FileNotFoundException {
        // Open a stream to the destination
        if (destination == Destination.FILE) {
            // destination file
            streamOut = (OutputStream) new FileOutputStream(getFileName(), true);
        } else {
            // destination byte array
            streamOut = (OutputStream) new ByteArrayOutputStream((int) totalSize);
        }
    }

    // Write the buffer to the stream
    private void write(Token t) throws IOException, ClassNotFoundException {
        long length = Math.min(remainingSize, t.length());
        byte[] content = t.get((int) length);
        streamOut.write(content);
        remainingSize -= content.length;
        if (remainingSize == 0) {
            closeStream();
        }
    }

    private void closeStream() throws ClassNotFoundException, IOException {
        if (destination != Destination.FILE) {
            //Obtain the received byte array
            ByteArrayOutputStream baos = (ByteArrayOutputStream) streamOut;
            array = baos.toByteArray();
            if (destination == Destination.OBJECT) {
                // Deserialize the byte array into an object
                object = Serializer.deserialize(array);
            }
        }
        try {
            // Close the file stream
            streamOut.close();
        } catch (IOException e) {
            Log.e(LOGGER_TAG, "Error closing output stream on connection " + this);
        }

    }

    @Override
    public Direction getDirection() {
        return Direction.RECEIVE;
    }

    public void setReceptionDefaultFileName(String fname) {
        this.fileName = fname;
    }

    @Override
    public void start(InternalConnection connection, List<ByteBuffer> received, List<ByteBuffer> transmit) {
        //The initalization will be done when the first packet arrives. Not when
        //the transfer is started
        token = new Token();
        tokens--;
        hasToken = true;
    }

    @Override
    public void progress(InternalConnection connection, List<ByteBuffer> received, List<ByteBuffer> transmit) throws IOException, ClassNotFoundException {
        while (!token.isCompletelyFilled() && !received.isEmpty()) {
            this.loadToken(token, received);
            if (token.isCompletelyFilled()) {
                if (!destInit || !sizeInit) {
                    initTransfer(token);
                }
                write(token);
                if (remainingSize > 0) {
                    token = new Token();
                }
            }
        }
    }

    @Override
    public boolean checkViability(boolean closedCommunication, List<ByteBuffer> received, List<ByteBuffer> transmit) throws NIOException {
        if (!closedCommunication || !received.isEmpty()) {
            return tokens > 0;
        } else {
            throw new NIOException(NIOException.ErrorType.CLOSED_CONNECTION, new Exception("Channel was already closed for connection " + this));
        }
    }

    @Override
    public void pause(InternalConnection ic) {
        pausedConnections.add(ic);
    }

    @Override
    public boolean isComplete(List<ByteBuffer> received, List<ByteBuffer> transmit) {
        return sizeInit && remainingSize == 0;
    }

    @Override
    public void notifyCompletion(Connection c, EventManager<?> em) {
        if (isData()) { // DataTransfer
            em.dataReceived(c, this);
        } else if (isCommand()) { // CommandTransfer
            em.commandReceived(c, this);
        }
        tokens++;
        hasToken = false;
        if (!pausedConnections.isEmpty()) {
            InternalConnection ic = pausedConnections.removeFirst();
            ic.resume();
        }
    }

    @Override
    public void notifyError(Connection c, EventManager<?> em, CommException exc) {
        if (notifyErrors) {
            em.notifyError(c, this, exc);
        }
        if (hasToken) {
            tokens++;
            hasToken = false;
            if (!pausedConnections.isEmpty()) {
                InternalConnection ic = pausedConnections.removeFirst();
                ic.resume();
            }
        }

    }

}
