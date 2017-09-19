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
package es.bsc.mobile.node;

import android.util.Log;
import es.bsc.comm.CommException;
import es.bsc.comm.Node;
import es.bsc.mobile.comm.CommunicationManager;
import es.bsc.mobile.types.comm.NodeAndHash;
import es.bsc.mobile.types.comm.NodeRegistry;
import es.bsc.mobile.types.messages.comm.AdditionNotification;
import es.bsc.mobile.types.messages.comm.JoinReply;
import es.bsc.mobile.types.messages.comm.JoinRequest;
import es.bsc.mobile.types.messages.comm.ReplaceFingers;
import es.bsc.mobile.types.messages.comm.StartP2P;
import es.bsc.mobile.utils.DistributedHashTable;
import es.bsc.mobile.utils.Fingers;
import java.util.LinkedList;


public class P2PNode<T extends DistributedHashTable> {

    private static final String LOGGER_TAG = "Runtime.P2P";

    protected final T dataRegistry;

    //Ring Management
    private final NodeAndHash me;
    private NodeAndHash successor;
    private boolean responsible = false;
    private final LinkedList<NodeAndHash> representeds = new LinkedList<NodeAndHash>();

    //Query Accesses
    private final Fingers fingers;

    //Pending operations
    private final LinkedList<AdditionNotification> pendingAdditionForwards;

    public P2PNode(Node ref, T dht) {
        me = new NodeAndHash(ref, ref.toString() + System.nanoTime());
        dataRegistry = dht;
        successor = null;
        fingers = new Fingers(me, NodeAndHash.getHashSize() + 1);
        pendingAdditionForwards = new LinkedList<AdditionNotification>();
    }

    public void startP2P(LinkedList<Node> resources) {
        startP2P(true, me.getNode());
        for (Node n : resources) {
            StartP2P start = new StartP2P(false, me.getNode());
            CommunicationManager.notifyCommand(n, start);
        }
    }

    public Node getMe() {
        return me.getNode();
    }

    public es.bsc.comm.Node getResponsibleFor(String data) {
        short hash = NodeAndHash.getHash(data);
        return getResponsibleForHash(hash);
    }

    private Node getResponsibleForHash(short hash) {
        NodeAndHash finger = fingers.getResponsibleFor(hash);
        if (finger.getNode().equals(me.getNode()) && NodeAndHash.isCloserPreviousThan(successor, me, hash)) {
            finger = successor;
        }
        return finger.getNode();
    }

    public void init() {
        try {
            CommunicationManager.openServer(me.getNode());
        } catch (CommException e) {
            Log.wtf(LOGGER_TAG, "Error starting server socket.", e);
        }
    }

    public void startP2P(boolean takeResponsbilities, Node contactNode) {
        responsible = takeResponsbilities;
        successor = me;
        if (!getMe().equals(contactNode)) {
            JoinRequest jr = new JoinRequest(me, takeResponsbilities);
            CommunicationManager.notifyCommand(contactNode, jr);
        }
    }

    public void joinP2P(JoinRequest jrq) {
        NodeAndHash newNode = jrq.getNodeAndHash();
        Node resp = getResponsibleForHash(newNode.getHash());
        if (resp != getMe()) {
            CommunicationManager.notifyCommand(resp, jrq);
        } else {
            newNode.setNode(NodeRegistry.getRepresentive(newNode.getNode()));
            if (!jrq.isResponsible()) {
                addRepresented(newNode);
            } else if (responsible) {
                shareResponsibilites(newNode);
            } else {
                passResponsibilities(newNode);
            }
        }
    }

    public void addRepresented(NodeAndHash newNode) {
        representeds.add(newNode);
        ReplaceFingers rf = new ReplaceFingers(fingers);
        CommunicationManager.notifyCommand(newNode.getNode(), rf);
    }

    private void passResponsibilities(NodeAndHash newNode) {
        fingers.setAllResponsible(newNode);
        for (NodeAndHash represented : representeds) {
            JoinRequest jr = new JoinRequest(represented, false);
            CommunicationManager.notifyCommand(newNode.getNode(), jr);
        }
        representeds.clear();
    }

    private void shareResponsibilites(NodeAndHash newNode) {
        NodeAndHash oldSuccessor = successor;
        successor = newNode;
        handlePendingSuccessorNotifications();

        boolean updatedTable = fingers.updateResponsibles(newNode);
        LinkedList<NodeAndHash> myRepresented = new LinkedList<NodeAndHash>();
        LinkedList<NodeAndHash> newRepresented = new LinkedList<NodeAndHash>();
        ReplaceFingers rf = new ReplaceFingers(fingers);
        for (NodeAndHash represented : representeds) {
            if (NodeAndHash.isCloserPreviousThan(me, newNode, represented.getHash())) {
                if (updatedTable) {
                    myRepresented.add(represented);
                    CommunicationManager.notifyCommand(represented.getNode(), rf);
                }
            } else {
                newRepresented.add(represented);
            }
        }
        representeds.clear();
        representeds.addAll(myRepresented);

        JoinReply jrp = new JoinReply(oldSuccessor, newRepresented, dataRegistry.retrieveBiggerThan(newNode.getHash()));
        CommunicationManager.notifyCommand(newNode.getNode(), jrp);
    }

    public void replaceFingers(Fingers newFingers) {
        fingers.replace(newFingers);
    }

    public void joinReplied(JoinReply jr) {
        //The contact node replies the nodes that should be my successor
        NodeAndHash succ = jr.getSuccessor();
        succ.setNode(NodeRegistry.getRepresentive(succ.getNode()));

        //While the join message was being transferred, the node may had 
        //received a joinRequest or a newPredecessor notification. If the 
        //just received successor is the closer node the node know but itself,
        //register as the successor
        if (successor.getNode().equals(me.getNode()) || NodeAndHash.isCloserPreviousThan(succ, successor, me)) {
            successor = succ;
            handlePendingSuccessorNotifications();
        }

        //The fingers table is updated with the new node. 
        boolean updatedTable = fingers.updateResponsibles(succ);

        if (updatedTable) {
            forwardFingersToRepresented();
        }

        for (NodeAndHash rep : jr.getRepresenteds()) {
            rep.setNode(NodeRegistry.getRepresentive(rep.getNode()));
            addRepresented(rep);
        }

        short[] indexes = fingers.getIndexes();
        NodeAndHash[] nodes = new NodeAndHash[indexes.length];
        handleAdditionNotification(new AdditionNotification(me, indexes, nodes));

        dataRegistry.addRegisters(jr.getRegisters());

    }

    public void forwardAdditionNotification(AdditionNotification an) {
        NodeAndHash newNode = an.getNewNode();
        if (newNode.getNode().equals(me.getNode())) {
            for (NodeAndHash n : an.getNodes()) {
                n.setNode(NodeRegistry.getRepresentive(n.getNode()));
            }
            fingers.setResponsibles(an.getNodes());
            forwardFingersToRepresented();
        } else {
            additionNotification(an);
        }
    }

    private void handlePendingSuccessorNotifications() {
        for (AdditionNotification an : pendingAdditionForwards) {
            handleAdditionNotification(an);
        }
    }

    private void handleAdditionNotification(AdditionNotification an) {
        short[] indexes = an.getIndexes();
        NodeAndHash[] nodes = an.getNodes();
        for (int i = 0; i < indexes.length; i++) {
            if (nodes[i] == null && NodeAndHash.isCloserPreviousThan(me, successor, indexes[i])) {
                nodes[i] = me;
            }
        }
    }

    private void forwardFingersToRepresented() {
        ReplaceFingers rf = new ReplaceFingers(fingers);
        for (NodeAndHash represented : representeds) {
            CommunicationManager.notifyCommand(represented.getNode(), rf);
        }
    }

    private void additionNotification(AdditionNotification an) {
        NodeAndHash newNode = an.getNewNode();
        newNode.setNode(NodeRegistry.getRepresentive(newNode.getNode()));
        boolean updatedTable = fingers.updateResponsibles(newNode);
        if (updatedTable) {
            forwardFingersToRepresented();
        }

        //Check if we have already been given a successor where to forward the notification
        if (successor.getNode() == me.getNode()) {
            //I have no successor. Enqueue the notification forward for later.
            pendingAdditionForwards.add(an);
        } else {
            handleAdditionNotification(an);
        }
    }
}
