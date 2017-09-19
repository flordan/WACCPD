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
package es.bsc.mobile;

import android.util.Log;
import java.io.File;


public class Configuration {

    private static String dataDir = "/sdcard/COMPSs-Mobile";

    public static void setDataDir(String dir) {
        Log.i("CONFIGURATION", "Setting data dir" + dir);
        dataDir = dir;
    }

    public static String getDataDir() {
        (new File(dataDir)).mkdirs();
        return dataDir;
    }

}
