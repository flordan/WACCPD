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
package es.bsc.mobile.runtime.utils;

import android.util.Log;
import es.bsc.comm.Node;
import es.bsc.mobile.runtime.Configuration;
import es.bsc.mobile.runtime.service.RuntimeHandler;
import es.bsc.mobile.runtime.types.resources.ComputingPlatform;
import es.bsc.mobile.runtime.types.resources.cloudplatforms.RemoteResource;
import es.bsc.mobile.runtime.types.resources.cpuplatforms.CPUPlatform;
import es.bsc.mobile.runtime.types.resources.gpuplatform.OpenCLResource;
import es.bsc.mobile.runtime.types.resources.gpuplatforms.GPUPlatform;
import es.bsc.mobile.runtime.types.resources.Resource;
import es.bsc.mobile.runtime.types.resources.proxy.BackEndProxyInterface;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;


public class ResourceManager {

    private static final String LOGGER_TAG = "Runtime.Resources";

    public static final String RESOURCE_PATH = Configuration.getConfigurationFolder() + "resources.conf";
    private final HashMap<String, Resource> resources = new HashMap<String, Resource>();
    private final LinkedList<ComputingPlatform> platforms = new LinkedList();

    public ResourceManager(RuntimeHandler rh) {
        try {
            load(rh);
        } catch (Exception e) {
            Log.e(LOGGER_TAG, "Error loading resources configuration.", e);
        }
    }

    public void addResource(String name, Resource res) {
        resources.put(name, res);
        //save();
    }

    public Resource remove(String name) {
        Resource r = resources.remove(name);
        save();
        return r;
    }

    public Resource get(String name) {
        return resources.get(name);
    }

    public Collection<Resource> getAll() {
        return resources.values();
    }

    private void save() {
        BufferedWriter bw;
        try {
            File fout = new File(RESOURCE_PATH);
            FileOutputStream fos = new FileOutputStream(fout);
            bw = new BufferedWriter(new OutputStreamWriter(fos));
        } catch (FileNotFoundException e) {
            Log.e(LOGGER_TAG, "Location where to store the resources configuration does not exist.", e);
            return;
        }
        for (Resource res : resources.values()) {
            try {
                bw.write(res.toString());
                bw.newLine();
            } catch (IOException e) {
                Log.e(LOGGER_TAG, "Error adding resource to the resources configuration file.", e);
            }
        }
        try {
            bw.close();
        } catch (IOException e) {
            //Error closing do nothing
        }
    }

    private void load(RuntimeHandler rh) {
        ComputingPlatform cloud = null;

        try {
            Class<?> methodClass;
            methodClass = Class.forName(Configuration.getOffloaderClass());
                        Constructor method = methodClass.getDeclaredConstructor(String.class);
            cloud = (ComputingPlatform) method.newInstance("Cloud");

            Log.i(LOGGER_TAG, "Offloading tasks following the policies defined in " + Configuration.getOffloaderClass());
        } catch (ClassNotFoundException e) {
            Log.e(LOGGER_TAG, "Error loading the offloading policies " + Configuration.getOffloaderClass(), e);
        } catch (NoSuchMethodException e) {
            Log.e(LOGGER_TAG, "Error loading the offloading policies " + Configuration.getOffloaderClass(), e);
        } catch (SecurityException e) {
            Log.e(LOGGER_TAG, "Error loading the offloading policies " + Configuration.getOffloaderClass(), e);
        } catch (InstantiationException e) {
            Log.e(LOGGER_TAG, "Error loading the offloading policies " + Configuration.getOffloaderClass(), e);
        } catch (IllegalAccessException e) {
            Log.e(LOGGER_TAG, "Error loading the offloading policies " + Configuration.getOffloaderClass(), e);
        } catch (IllegalArgumentException e) {
            Log.e(LOGGER_TAG, "Error loading the offloading policies " + Configuration.getOffloaderClass(), e);
        } catch (InvocationTargetException e) {
            Log.e(LOGGER_TAG, "Error loading the offloading policies " + Configuration.getOffloaderClass(), e);
        }

        FileReader fr;
        try {
            fr = new FileReader(RESOURCE_PATH);
        } catch (FileNotFoundException e) {
            Log.w(LOGGER_TAG, "File " + RESOURCE_PATH + " does not exist and resources cannot be loaded.", e);
            return;
        }

        platforms.add(cloud);
        /*
        BufferedReader br = new BufferedReader(fr);
        try {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    Resource r = new Resource(line);
                    cloud.addResource(r);
                    String name = r.getName();
                    resources.put(name, r);
                } catch (Resource.NodeCreationException e) {
                    Log.e(LOGGER_TAG, "Error reading resource from the resources file.", e);
                }
            }

        } catch (IOException e) {
            Log.e(LOGGER_TAG, "Error reading from the resources file.", e);
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                //Do nothing
            }
        }*/
        platforms.add(new CPUPlatform("CPU", 4, rh));
        platforms.add(new GPUPlatform("GPU", rh, new HashSet<String>()));

    }

    public LinkedList<Node> getAllNodes() {
        LinkedList<Node> nodes = new LinkedList<Node>();
        for (Resource res : resources.values()) {
            if (res instanceof RemoteResource) {
                nodes.add(((RemoteResource) res).getNode());
            }
        }
        return nodes;
    }

    public LinkedList<ComputingPlatform> getComputingPlatforms() {
        return platforms;
    }
}
