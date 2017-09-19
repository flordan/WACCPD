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
package es.bsc.mobile.parallelizer.manifest;

import es.bsc.mobile.parallelizer.utils.XMLUtils;
import java.io.IOException;
import java.util.LinkedList;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The AndroidManifest class loads the AndroidManifest file of a project and
 * allows to do some modifications so the application can host the COMPSs
 * runtime toolkit.
 *
 * @author flordan
 */
public class AndroidManifest {

    //Android Manifest XML tags
    private static final String MANIFEST_TAG = "manifest";
    private static final String APPLICATION = "application";
    private static final String PACKAGE = "package";
    private static final String ANDROID_NAME = "android:name";
    private static final String ANDROID_USES_PERMISSION = "uses-permission";
    private static final String ANDROID_COMPONENT_ACTIVITY = "activity";
    private static final String ANDROID_COMPONENT_SERVICE = "service";
    private static final String ANDROID_COMPONENT_RECEIVER = "receiver";
    private static final String ANDROID_COMPONENT_PROVIDER = "provider";
    private static final String ANDROID_INTENT_FILTER = "intent-filter";
    private static final String ANDROID_ACTION = "action";
    private static final String ANDROID_ACTION_MAIN = "android.intent.action.MAIN";

    //COMPSs Runtime toolkit classes 
    private static final String COMPSS_RUNTIME_SERVICE = "es.bsc.mobile.runtime.service.RuntimeService";
    private static final String COMPSS_MONITOR_ACTIVITY = "es.bsc.mobile.monitor.MonitorActivity";

    //Document containing the manifest xml
    private final Document manifestDoc;

    private final LinkedList<Component> components;
    private final LinkedList<String> permissions;
    private Node manifestNode;
    private String appPackage;
    private Node application;

    /**
     * Loads a given AndroidManifest and parses all the important tags.
     *
     * @param manifestLocation Relative path where the AndroidManifest is
     * located.
     * @throws IOException If any IO errors occur.
     * @throws SAXException If any parse errors occur.
     * @throws ParserConfigurationException during the loading of the manifest
     * file, a DocumentBuilder cannot be created which satisfies the
     * configuration requested.
     */
    public AndroidManifest(String manifestLocation) throws SAXException, IOException, ParserConfigurationException {
        components = new LinkedList<Component>();
        permissions = new LinkedList<String>();
        manifestDoc = XMLUtils.load(manifestLocation);
        NodeList nl = manifestDoc.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (XMLUtils.isNodeName(n, MANIFEST_TAG)) {
                manifestNode = n;
                parseManifestNode(n);
            }
        }

    }

    private void parseManifestNode(Node manifest) {
        appPackage = XMLUtils.getAttributeValue(manifest, PACKAGE);
        NodeList nl = manifest.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (XMLUtils.isNodeName(n, APPLICATION)) {
                parseApplicationNode(n);
            }
            if (XMLUtils.isNodeName(n, ANDROID_USES_PERMISSION)) {
                parsePermissionsNode(n);
            }
        }
    }

    private void parseApplicationNode(Node application) {
        this.application = application;
        NodeList nl = application.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (XMLUtils.isNodeName(n, ANDROID_COMPONENT_ACTIVITY)) {
                parseActivityNode(n);
            }
            if (XMLUtils.isNodeName(n, ANDROID_COMPONENT_SERVICE)) {
                parseServiceNode(n);
            }
            if (XMLUtils.isNodeName(n, ANDROID_COMPONENT_PROVIDER)) {
                parseProviderNode(n);
            }
            if (XMLUtils.isNodeName(n, ANDROID_COMPONENT_RECEIVER)) {
                parseReceiverNode(n);
            }
        }
    }

    private void parsePermissionsNode(Node activity) {
        String name = XMLUtils.getAttributeValue(activity, ANDROID_NAME);
        permissions.add(name);
    }

    private void parseActivityNode(Node activity) {
        String name = XMLUtils.getAttributeValue(activity, ANDROID_NAME);
        if (name.startsWith(".")) {
            name = appPackage + name;
        }
        boolean isMain = isMain(activity);
        Component comp = new Component(Component.Type.ACTIVITY, name, activity, isMain);
        components.add(comp);
    }

    private void parseServiceNode(Node service) {
        String name = XMLUtils.getAttributeValue(service, ANDROID_NAME);
        if (name.startsWith(".")) {
            name = appPackage + name;
        }
        boolean isMain = isMain(service);
        Component comp = new Component(Component.Type.SERVICE, name, service, isMain);
        components.add(comp);
    }

    private void parseReceiverNode(Node receiver) {
        String name = XMLUtils.getAttributeValue(receiver, ANDROID_NAME);
        if (name.startsWith(".")) {
            name = appPackage + name;
        }
        boolean isMain = isMain(receiver);
        Component comp = new Component(Component.Type.BROADCAST_RECEIVER, name, receiver, isMain);
        components.add(comp);
    }

    private void parseProviderNode(Node provider) {
        String name = XMLUtils.getAttributeValue(provider, ANDROID_NAME);
        if (name.startsWith(".")) {
            name = appPackage + name;
        }
        boolean isMain = isMain(provider);
        Component comp = new Component(Component.Type.CONTENT_PROVIDER, name, provider, isMain);
        components.add(comp);
    }

    private boolean isMain(Node n) {

        boolean result = false;
        NodeList nl = n.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node child = nl.item(i);
            if (XMLUtils.isNodeName(child, ANDROID_INTENT_FILTER)) {
                NodeList filterChilds = child.getChildNodes();
                for (int i2 = 0; i2 < filterChilds.getLength(); i2++) {
                    Node filterChild = filterChilds.item(i2);
                    if (XMLUtils.isNodeName(filterChild, ANDROID_ACTION)) {
                        String name = XMLUtils.getAttributeValue(filterChild, ANDROID_NAME);
                        if (name.compareTo(ANDROID_ACTION_MAIN) == 0) {
                            result = true;
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Checks if a given component of the application is declared as a main
     * component in the AndroidManifest.
     *
     * @param component Name of the component to check
     *
     * @return {@literal true} if the component is declared as main
     */
    public boolean isMain(String component) {
        for (Component comp : components) {
            if (comp.name.compareTo(component) == 0) {
                return comp.isMain;
            }
        }
        return false;
    }

    /**
     * Gets all the application components that have been declared as main in
     * the application's Android Manifest.
     *
     * @return a list with the names of every application component declared as
     * a main component.
     */
    public LinkedList<String> getMainComponents() {
        LinkedList<String> main = new LinkedList<String>();
        for (Component comp : components) {
            if (comp.isMain) {
                main.add(comp.name);
            }
        }
        return main;
    }

    /**
     * Adds a service to handle the COMPSs Runtime calls
     */
    public void addRuntimeService() {
        for (Component comp : components) {
            if (comp.name.compareTo(COMPSS_RUNTIME_SERVICE) == 0) {
                return;
            }
        }
        Element n = manifestDoc.createElement(ANDROID_COMPONENT_SERVICE);
        n.setAttribute(ANDROID_NAME, COMPSS_RUNTIME_SERVICE);
        n.setAttribute("android:process", ":newprocess");
        application.appendChild(n);
    }

    /**
     * Adds an activity to Monitor the runtime execution
     */
    public void addMonitoringActivities() {
        for (Component comp : components) {
            if (comp.name.compareTo(COMPSS_MONITOR_ACTIVITY) == 0) {
                return;
            }
        }

        Element n = manifestDoc.createElement(ANDROID_COMPONENT_ACTIVITY);
        Element filter = manifestDoc.createElement(ANDROID_INTENT_FILTER);
        n.appendChild(filter);
        Element action = manifestDoc.createElement(ANDROID_ACTION);
        action.setAttribute(ANDROID_NAME, ANDROID_ACTION_MAIN);
        filter.appendChild(action);
        Element category = manifestDoc.createElement("category");
        category.setAttribute(ANDROID_NAME, "android.intent.category.LAUNCHER");
        filter.appendChild(category);
        n.setAttribute(ANDROID_NAME, COMPSS_MONITOR_ACTIVITY);
        application.appendChild(n);
    }

    /**
     * Enables an Android permission on the Android Manifest.
     *
     * @param name Name of the permission to enable
     */
    public void enablePermission(String name) {
        if (!permissions.contains(name)) {
            Element n = manifestDoc.createElement(ANDROID_USES_PERMISSION);
            n.setAttribute(ANDROID_NAME, name);
            manifestNode.insertBefore(n, application);
        }
    }

    /**
     * Edits the Android Manifest to remove all the changes done so the
     * application can contact with the COMPSs runtime toolkit service.
     *
     * The method removes the Runtime service and the monitoring activity, and
     * disables the Internet and the write External permission.
     *
     */
    public void clean() {
        NodeList nl = application.getChildNodes();
        LinkedList<Node> removables = new LinkedList<Node>();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (!XMLUtils.isNodeName(n, ANDROID_COMPONENT_ACTIVITY)
                    && !(XMLUtils.isNodeName(n, ANDROID_COMPONENT_SERVICE))
                    && !(XMLUtils.isNodeName(n, ANDROID_COMPONENT_RECEIVER))
                    && !(XMLUtils.isNodeName(n, ANDROID_COMPONENT_PROVIDER))) {
                continue;
            }
            String name = XMLUtils.getAttributeValue(n, ANDROID_NAME);
            if (name.compareTo(COMPSS_MONITOR_ACTIVITY) == 0
                    || name.compareTo(COMPSS_RUNTIME_SERVICE) == 0) {
                removables.add(n);
            }
        }
        for (Node removable : removables) {
            application.removeChild(removable);
        }
    }

    /**
     * Writes down the current Android Manifest to a file.
     *
     * @param location Absolute path where the Android Manifest should be stored
     * @throws TransformerException If an unrecoverable error occurs during the
     * course of the transformation.
     */
    public void saveModifications(String location) throws TransformerException {
        XMLUtils.save(manifestDoc, location);
    }

    private static class Component {

        private enum Type {

            ACTIVITY,
            SERVICE,
            CONTENT_PROVIDER,
            BROADCAST_RECEIVER
        }

        final Type type;
        final String name;
        final Node node;
        final boolean isMain;

        private Component(Type type, String name, Node node, boolean isMain) {
            this.type = type;
            this.name = name;
            this.node = node;
            this.isMain = isMain;
        }
    }
}
