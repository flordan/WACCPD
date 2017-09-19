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
package es.bsc.mobile.parallelizer.configuration;

import java.io.File;

/**
 * The Paths class is used for encapsulating the important paths used during the
 * instrumentation process.
 *
 *
 * @author flordan
 */
public interface Paths {

    /**
     * Runtime front-end absolute path
     */
    public static final String RUNTIME_PATH = "/home/flordan/Android/workspace/MobileRuntime";

    /**
     *
     * @return Relative path for compiled classes folder
     */
    String compiledClassesDir();

    String resources();

    String strings();

    String layouts();

    /**
     *
     * @return Relative path for the Android Manifest xml file
     */
    String androidManifest();

    public static class Maven implements Paths {

        @Override
        public String compiledClassesDir() {
            return File.separator + "classes";
        }

        @Override
        public String resources() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String strings() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String layouts() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String androidManifest() {
            return File.separator + "AndroidManifest.xml";
        }

    }

    public static class Eclipse implements Paths {

        @Override
        public String compiledClassesDir() {
            return File.separator + "bin" + File.separator + "classes";
        }

        @Override
        public String resources() {
            return File.separator + "res";
        }

        @Override
        public String strings() {
            return File.separator + "res" + File.separator + "values" + File.separator + "strings.xml";
        }

        @Override
        public String layouts() {
            return File.separator + "res" + File.separator + "layout";
        }

        @Override
        public String androidManifest() {
            return File.separator + "bin" + File.separator + "AndroidManifest.xml";
        }

    }
}
