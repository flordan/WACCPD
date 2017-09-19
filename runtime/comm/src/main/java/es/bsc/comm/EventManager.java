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

import android.util.Log;

import es.bsc.comm.event.Event;
import es.bsc.comm.stage.Reception;
import es.bsc.comm.stage.Submission;
import es.bsc.comm.stage.Transfer;

import java.util.LinkedList;
import java.util.List;

public abstract class EventManager<T extends Event> {

    protected static final String LOGGER_TAG = TransferManager.LOGGER_TAG;

    private boolean isBlocked;
    protected List<T> events;

    private boolean stopReceived;

    private final MessageHandler mh;

    public EventManager(MessageHandler messageReceiver) {
        stopReceived = false;
        events = new LinkedList<T>();
        isBlocked = false;
        mh = messageReceiver;
    }

    // While worker is running
    public void run() {
        Log.d(LOGGER_TAG, "Event Manager started");

        LinkedList<T> privateEvents = new LinkedList<T>();
        while (!stopReceived || !events.isEmpty()) {
            // Handle specific impementation actions
            specificActions();

            // Copy the events to a private list
            synchronized (this) {
                LinkedList<T> list = (LinkedList<T>) events;
                events = privateEvents;
                privateEvents = list;
            }

            // Process the events
            while (!privateEvents.isEmpty()) {
                Event event = privateEvents.removeFirst();
                processEvent(event);
            }

            waitForEvents();
        }

        handleSpecificStop();
        mh.shutdown();
        Log.d(LOGGER_TAG, "Event Manager stopped");
    }

    protected void waitForEvents() {
        // Sleep since there is nprocessEventothing to process
        if (events.isEmpty() && !stopReceived) {
            synchronized (this) {
                if (events.isEmpty() && !stopReceived) {
                    isBlocked = true;
                    try {
                        this.wait(waitEventsTimeout());
                    } catch (InterruptedException e) {
                        // No need to handle such exception
                    }
                    isBlocked = false;
                }
            }
        }
    }

    private void processEvent(Event e) {
        e.processEventOnConnection(this);
    }

    public void addEvent(T e) {
        synchronized (this) {
            events.add(e);
            if (isBlocked) {
                this.notify();
            }
        }
    }

    public void shutdown() {
        stopReceived = true;
        synchronized (this) {
            if (isBlocked) {
                this.notify();
            }
        }
    }

    public void notifyError(Connection con, Transfer t, CommException exception) {
        mh.errorHandler(con, t, exception);
    }

    public void dataReceived(Connection c, Reception t) {
        mh.dataReceived(c, t);
    }

    public void commandReceived(Connection c, Reception t) {
        mh.commandReceived(c, t);
    }

    public void writeFinished(Connection c, Submission t) {
        mh.writeFinished(c, t);
    }

    public void connectionFinished(Connection c) {
        mh.connectionFinished(c);
    }

    public abstract void init(String properties) throws CommException;

    public abstract void startServer(Node n) throws CommException;

    public abstract Connection startConnection(Node n);

    protected abstract long waitEventsTimeout();

    protected abstract void specificActions();

    protected abstract void handleSpecificStop();

    public abstract void shutdown(Connection c);

}
