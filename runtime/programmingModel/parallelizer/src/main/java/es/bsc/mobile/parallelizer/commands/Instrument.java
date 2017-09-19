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
import es.bsc.mobile.parallelizer.utils.InstrumentationAssistant;
import es.bsc.mobile.parallelizer.utils.MethodEditor;
import java.io.IOException;
import java.util.TreeSet;
import java.util.logging.Logger;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CodeConverter;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * The Instrument class encapsulates the execution of the instrument command
 * which instruments the application classes to replace the CE invocations by
 * calls to the COMPSs runtime toolkit.
 *
 * For this purpose, when executed, the command instruments the code of all the
 * application classes and modifies the onCreate/onDestroy methods of the main
 * components of the application to instantiate the runtime service stub.
 *
 * @author flordan
 */
public class Instrument implements Command {

    private static final Logger LOGGER = Logger.getLogger("INSTRUMENTER");

    private static final String LOAD_MANIFEST_ERROR = "Error loading Android Manifest.";
    private static final String CEI_NOT_FOUND = "Could find Core Element Interface (CEI.java).";
    private static final String LOAD_UTILS_ERROR = "Error loading class for editing.";
    private static final String COPY_RUNTIME_ERROR = "Error inserting runtime classes into the project.";
    private static final String ERROR_INSTRUMENTING = "Error instrumenting class ";

    private static final ClassPool cp = ClassPool.getDefault();

    @Override
    public void execute(String projectDir, Paths paths) throws CommandExecutionException {

        String classesDir = projectDir + paths.compiledClassesDir();
        AndroidManifest manifest;
        //Load an android Manifest from a file 
        try {
            manifest = new AndroidManifest(projectDir + paths.androidManifest());
        } catch (IOException e) {
            throw new CommandExecutionException(LOAD_MANIFEST_ERROR, e);
        } catch (ParserConfigurationException e) {
            throw new CommandExecutionException(LOAD_MANIFEST_ERROR, e);
        } catch (SAXException e) {
            throw new CommandExecutionException(LOAD_MANIFEST_ERROR, e);
        }

        try {
            InstrumentationAssistant.load(classesDir);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            throw new CommandExecutionException(LOAD_UTILS_ERROR, ex);
        } catch (NotFoundException ex) {
            ex.printStackTrace();
            throw new CommandExecutionException(LOAD_UTILS_ERROR, ex);
        }

        final Class<?> cei;
        try {
            cei = Class.forName("CEI");
        } catch (ClassNotFoundException ex) {
            throw new CommandExecutionException(CEI_NOT_FOUND, ex);
        }
        /*        try {
         copyRuntime(projectDir);
         } catch (IOException ex) {
         throw new CommandExecutionException(COPY_RUNTIME_ERROR, ex);
         }
         */
        TreeSet<String> classes = new TreeSet<String>();
        classes.addAll(InstrumentationAssistant.getOrchestrationClasses());
        classes.addAll(manifest.getMainComponents());
        for (String className : classes) {
            try {
                modifyClass(classesDir, className, cei, manifest);
            } catch (NotFoundException ex) {
                throw new CommandExecutionException(ERROR_INSTRUMENTING + className, ex);
            } catch (CannotCompileException ex) {
                throw new CommandExecutionException(ERROR_INSTRUMENTING + className, ex);
            } catch (IOException ex) {
                throw new CommandExecutionException(ERROR_INSTRUMENTING + className, ex);
            }
        }
    }

    private void modifyClass(String classesDir, String className, Class<?> cei, AndroidManifest manifest) throws NotFoundException, CannotCompileException, IOException {
        CtClass appClass = cp.get(className);
        boolean isMain = manifest.isMain(className);
        boolean isOrchestration = InstrumentationAssistant.isOrchestratonClass(className);
        boolean onCreateEdited = false;
        boolean onDestroyEdited = false;

        // Create Code Converter
        CodeConverter converter = new CodeConverter();
        CtClass arrayWatcher = cp.get("es.bsc.mobile.runtime.ArrayAccessWatcher");
        CodeConverter.DefaultArrayAccessReplacementMethodNames names = new CodeConverter.DefaultArrayAccessReplacementMethodNames();
        converter.replaceArrayAccess(arrayWatcher, (CodeConverter.ArrayAccessReplacementMethodNames) names);

        MethodEditor methodeditor = new MethodEditor(cei.getDeclaredMethods());
        for (CtMethod m : appClass.getDeclaredMethods()) {
            LOGGER.info("Instrumenting method: " + m.getLongName());
            if (isOrchestration) {
                m.instrument(converter);
                m.instrument(methodeditor);
            }

            // Afegir start i stop
            if (isMain) {
                if (m.getName().compareTo("onCreate") == 0 && m.getSignature().compareTo("(Landroid/os/Bundle;)V") == 0) {
                    modifyCreateMethod(m);
                    onCreateEdited = true;
                }
                if (m.getName().compareTo("onDestroy") == 0 && m.getSignature().compareTo("()V") == 0) {
                    modifyDestroyMethod(m);
                    onDestroyEdited = true;
                }
            }
        }

        if (isMain) {
            if (!onCreateEdited) {
                CtMethod method = createOnCreate(appClass);
                modifyCreateMethod(method);
            }
            if (!onDestroyEdited) {
                CtMethod method = createOnDestroy(appClass);
                modifyDestroyMethod(method);
            }
        }

        // Salvar la classe
        appClass.writeFile(classesDir);
    }

    private static CtMethod createOnCreate(CtClass classe) throws CannotCompileException {
        LOGGER.info("onCreate Method added");
        CtMethod newMethod = CtNewMethod.make("protected void onCreate(Bundle savedInstanceState) {super.onCreate(saveInstanceState);}", classe);
        return newMethod;
    }

    private static CtMethod createOnDestroy(CtClass classe) throws CannotCompileException {
        LOGGER.info("onDestroy Method added");
        CtMethod newMethod = CtNewMethod.make("public void onDestroy() {super.onDestroy();}", classe);
        return newMethod;
    }

    private static void modifyCreateMethod(CtMethod method) throws CannotCompileException {
        LOGGER.info("Adding es.bsc.mobile.runtime.Runtime.startRuntime(this); to onCreate method.");
        method.insertAfter("es.bsc.mobile.runtime.Runtime.startRuntime(this);");
    }

    private static void modifyDestroyMethod(CtMethod method) throws CannotCompileException {
        LOGGER.info("Adding es.bsc.mobile.runtime.Runtime.unbindRuntime(this); to onDestroy method.");
        method.insertAfter("es.bsc.mobile.runtime.Runtime.unbindRuntime(this);");
    }
}
