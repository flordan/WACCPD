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
package es.bsc.mobile.parallelizer.commands;

import es.bsc.mobile.parallelizer.configuration.Paths;
import es.bsc.mobile.parallelizer.manifest.AndroidManifest;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;

/**
 * The manifest command edits the Android Manifest so the application can
 * contact with the COMPSs runtime toolkit service.
 *
 * For this purpose, the method adds a new Service component that hosts the
 * Runtime Toolkit, it adds the monitoring activity to show the application
 * progress and enables the Internet and the write External permission so the
 * application can communicate with remote nodes and write temporary files in
 * the device storage.
 *
 *
 * @author flordan
 */
public class Manifest implements Command {

    private static final String LOAD_MANIFEST_ERROR = "Error loading Android Manifest";
    private static final String SAVE_MANIFEST_ERROR = "Error writing down the Android Manifest into a file";

    @Override
    public void execute(String projectDir, Paths paths) throws CommandExecutionException {

        AndroidManifest manifest;
        //Load an android Manifest from a file 
        try {
            manifest = new AndroidManifest(projectDir + paths.androidManifest());
        } catch (IOException e) {
            throw new CommandExecutionException(LOAD_MANIFEST_ERROR, e);
        } catch (SAXException e) {
            throw new CommandExecutionException(LOAD_MANIFEST_ERROR, e);
        } catch (ParserConfigurationException e) {
            throw new CommandExecutionException(LOAD_MANIFEST_ERROR, e);
        }

        /**
         * Edits the Android Manifest so the application can contact with the
         * COMPSs runtime toolkit service.
         *
         * For this purpose, the method adds a new Service component that hosts
         * the Runtime Toolkit, it adds the monitoring activity to show the
         * application progress and enables the Internet and the write External
         * permission so the application can communicate with remote nodes and
         * write temporary files in the device storage.
         *
         */
        manifest.addRuntimeService();
        //manifest.addMonitoringActivities();
        manifest.enablePermission("android.permission.INTERNET");
        manifest.enablePermission("android.permission.WRITE_EXTERNAL_STORAGE");
        //manifest.enablePermission("android.permission.WRITE_EXTERNAL_STORAGE");

        //Save the android Manifest on a file
        try {
            manifest.saveModifications(projectDir + paths.androidManifest());
        } catch (TransformerException ex) {
            throw new CommandExecutionException(SAVE_MANIFEST_ERROR, ex);
        }
    }

}
