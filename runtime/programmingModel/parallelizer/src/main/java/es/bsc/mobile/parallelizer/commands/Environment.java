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
import es.bsc.mobile.parallelizer.resources.Strings;
import es.bsc.mobile.parallelizer.utils.FileUtils;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;

/**
 * The Environment class encapsulates the execution of the environment command
 * which modifies the application to include the COMPSs runtime toolkit classes.
 *
 * For this purpose, when executed, the command adds all the runtime toolkit
 * classes to the application ones and modifies some XML files in the project
 * resources so the runtime can run properly in the mobile device.
 *
 * @author flordan
 */
public class Environment implements Command {

    private static final String LOAD_STRINGS_ERROR = "Can not load Strings file";
    private static final String SAVE_STRINGS_ERROR = "Error saving Strings file";
    private static final String COPY_RUNTIME_ERROR = "Error copying the runtime into the appl directory";

    @Override
    public void execute(String projectDir, Paths paths) throws CommandExecutionException {
        Strings strings;
        try {
            strings = new Strings(projectDir + paths.strings());
        } catch (ParserConfigurationException e) {
            throw new CommandExecutionException(LOAD_STRINGS_ERROR, e);
        } catch (IOException e) {
            throw new CommandExecutionException(LOAD_STRINGS_ERROR, e);
        } catch (SAXException e) {
            throw new CommandExecutionException(LOAD_STRINGS_ERROR, e);
        }

        strings.addValue("mobiless_monitor", "monitor");

        try {
            strings.saveModifications(projectDir + paths.strings());
        } catch (TransformerException ex) {
            throw new CommandExecutionException(SAVE_STRINGS_ERROR, ex);
        }

        File runtimeClasses = new File(Paths.RUNTIME_PATH + paths.layouts());
        File appClasses = new File(projectDir + paths.layouts());

        try {
            FileUtils.copy(runtimeClasses, appClasses);
        } catch (IOException ex) {
            throw new CommandExecutionException(COPY_RUNTIME_ERROR, ex);
        }
    }

}
