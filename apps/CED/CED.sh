#!/bin/bash

#-----------------------------
#     Script configuration
#-----------------------------

#Used resources
CPU_CORES=4

#Load Balancing
EXECUTION_PLATFORM=""
#EXECUTION_PLATFORM="" 			# Computing Platform picked dynamically
#EXECUTION_PLATFORM="gpu"		# All tasks run on the GPU except for those indicated in the FIX SCHEDULING section to run on CPU
#EXECUTION_PLATFORM="cpu"		# All tasks run on the CPU except for those indicated in the FIX SCHEDULING section to run on CPU

DYNAMIC_SCHEDULING_POLICY=""  # DEFAULT VALUE IS ENERGY
#DYNAMIC_SCHEDULING_POLICY="energy"
#DYNAMIC_SCHEDULING_POLICY="performance"

#application
FRAMES_COUNT="30"

#-----------------------------
#     FOLDERS DEFINITION
#-----------------------------
APP_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CURRENT_DIR=`pwd`
cd ${APP_DIR}/../../runtime
RUNTIME_DIR=`pwd`

rm -rf /tmp/app
rm -rf /tmp/runtime
mkdir -p /tmp/app
mkdir -p /tmp/
cp -R ${RUNTIME_DIR} /tmp
cp -R ${APP_DIR} /tmp/app
 
rm -rf ${mvnRepoDir}/es/bsc


#-----------------------------
#       PREPARE Master
#-----------------------------
echo "PREPARING MASTER"
echo "    Preparing runtime"
cd /tmp/runtime 


#-----------------------------
#       PREPARE CPU Platform
#-----------------------------

original="return new BackEndProxyInterface(\"CPU\", data, false);"
replace="System.out.println(\"PROXIED CPU\");\n"
replace="${replace}System.out.println(\"Management is also proxied\");\n"
replace="${replace}return new BackEndProxyInterface(\"CPU\", data, true);"
sed ":a;N;\$!ba;s/$original/$replace/" /tmp/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/cpuplatforms/CPUPlatform.java > tmpfile
mv tmpfile /tmp/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/cpuplatforms/CPUPlatform.java

original="ComputingPlatformBackend cpuBackend = new CPUPlatformBackEnd(4, DATA_PROVIDER);"
replace="ComputingPlatformBackend cpuBackend = new CPUPlatformBackEnd(${CPU_CORES}, DATA_PROVIDER);\n"
replace="${replace}System.out.println(\"Runtime has CPU with ${CPU_CORES} cores\");"
sed ":a;N;\$!ba;s/$original/$replace/" /tmp/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/Runtime.java > tmpfile
mv tmpfile /tmp/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/Runtime.java

#-----------------------------
#       PREPARE GPU Platform
#-----------------------------

original="return new BackEndProxyInterface(\"GPU\", new GPUDataProvider(data, onCreationData), true);"
replace="System.out.println(\"PROXIED GPU\");\n"
replace="${replace}System.out.println(\"Management is also proxied\");\n"
replace="${replace}return new BackEndProxyInterface(\"GPU\", new GPUDataProvider(data, onCreationData), true);"
sed ":a;N;\$!ba;s/$original/$replace/" /tmp/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/gpuplatforms/GPUPlatform.java > tmpfile
mv tmpfile /tmp/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/gpuplatforms/GPUPlatform.java



#-----------------------------
#       EDIT RESOURCE MANAGER
#-----------------------------
original="    public ResourceManager(RuntimeHandler rh) {"
extra="\n"
extra="${extra}    public void loadDefaults(RuntimeHandler rh) {\n"
extra="${extra}        try {\n"

#ADD CPU PLATFORM
extra="${extra}            ComputingPlatform cpu = new CPUPlatform(\"CPU\", ${CPU_CORES}, rh);\n"
extra="${extra}            platforms.add(cpu);\n"
extra="${extra}            cpu.defineDefaultProfiles(\""
extra="${extra}{"
extra="${extra}es.bsc.mobile.apps.ced.Operations.gaussian(OBJECT,OBJECT,INT,INT)=[[3000][50866666][50866666][50866666]][[30][76300000][76300000][76300000]]"
extra="${extra}, es.bsc.mobile.apps.ced.Operations.sobel(OBJECT,INT,INT,OBJECT,OBJECT)=[[3000][103000000][103000000][103000000]][[30][154500000][154500000][154500000]]"
extra="${extra}, es.bsc.mobile.apps.ced.Operations.nonMaxSupp(OBJECT,INT,INT)=[[3000][8900000][8900000][8900000]][[30][13350000][13350000][13350000]]"
extra="${extra}, es.bsc.mobile.apps.ced.Operations.hysteresis(OBJECT,INT,INT)=[[3000][4566666][4566666][4566666]][[30][6850000][6850000][6850000]]"
extra="${extra}}\");\n"

extra="${extra}            es.bsc.mobile.runtime.types.resources.StaticAssignationManager.registerPlatform(cpu);\n"
extra="${extra}\n"

#ADD GPU PLATFORM
extra="${extra}            ComputingPlatform gpu = new GPUPlatform(\"GPU\", rh, new HashSet<String>());\n"
extra="${extra}            platforms.add(gpu);\n"
extra="${extra}            OpenCLResource or = new OpenCLResource(2, \"QUALCOMM Snapdragon(TM)\", \"QUALCOMM Adreno(TM)\");\n"
extra="${extra}            gpu.addResource(or);\n"
extra="${extra}            gpu.defineDefaultProfiles(\""
extra="${extra}{"
extra="${extra}raw\/kernels.cl->gaussian(OBJECT,OBJECT,INT,INT)=[[3000][4000000][4000000][4000000]][[30][10000000][10000000][10000000]]"
extra="${extra}, raw\/kernels.cl->sobel(OBJECT,INT,INT,OBJECT,OBJECT)=[[3000][8000000][8000000][8000000]][[30][20000000][20000000][20000000]]"
extra="${extra}, raw\/kernels.cl->nonMaxSupp(OBJECT,INT,INT)=[[3000][1300000][1300000][1300000]][[30][3250000][3250000][3250000]]"
extra="${extra}, raw\/kernels.cl->hysteresis(OBJECT,INT,INT)=[[3000][700000][700000][700000]][[30][1750000][1750000][1750000]]"
extra="${extra}}\");\n"
extra="${extra}            es.bsc.mobile.runtime.types.resources.StaticAssignationManager.registerPlatform(gpu);\n"


#SELECT DEFAULT EXECUTION PLATFORM
if [[ !  -z  ${EXECUTION_PLATFORM}  ]]; then
	extra="${extra}            es.bsc.mobile.runtime.types.resources.StaticAssignationManager.setDefaultPlatform(${EXECUTION_PLATFORM});\n"
fi
extra="${extra}            System.out.println(\"PREFERRED PLATFORM ${EXECUTION_PLATFORM}\");\n"
extra="${extra}        } catch (Exception defaultException) {\n"

extra="${extra}        }\n"
extra="${extra}    }\n"
extra="${extra}\n"

sed ":a;N;\$!ba;s/$original/$extra\n$original/" /tmp/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/utils/ResourceManager.java > tmpfile
mv tmpfile /tmp/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/utils/ResourceManager.java
sed -i -e 's/load(rh);/loadDefaults(rh);/g' /tmp/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/utils/ResourceManager.java

#-----------------------------
#       FIX SCHEDULING
#-----------------------------
#STATIC LOCATION
# original="    static {"
# extra="\n"
# extra="${extra}        taskToPlatform = new ComputingPlatform[4];\n"
# extra="${extra}        LinkedList<Integer> l = new LinkedList<Integer>();\n"
# extra="${extra}        l.add(1);\n"
# extra="${extra}        l.add(2);\n"
# extra="${extra}        platformToTasks.put(\"GPU\", l);\n"
# sed ":a;N;\$!ba;s/$original/$original\n$extra/" /tmp/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/StaticAssignationManager.java > tmpfile
# mv tmpfile /tmp/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/StaticAssignationManager.java


#----------------------------------------------------------
#       SETTING DYNAMIC POLICY WEIGHTS
#----------------------------------------------------------

if [ "${DYNAMIC_SCHEDULING_POLICY}" = "performance" ]; then
	ENERGY_WEIGHT="0"
else
	ENERGY_WEIGHT="5"
fi
sed -i -e 's/ private static final int ENERGY_WEIGHT = 10;/ private static final int ENERGY_WEIGHT = '${ENERGY_WEIGHT}';/g' /tmp/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/ComputingPlatform.java

#-----------------------------
#       ARRAY ACCESS BUGFIX
#-----------------------------
sed -i -e 's/m.instrument(converter);//g' /tmp/runtime/programmingModel/parallelizer/src/main/java/es/bsc/mobile/parallelizer/commands/Instrument.java


#-----------------------------
#       COMPILE RUNTIME
#-----------------------------
echo "    Compiling runtime"
cd /tmp/runtime 
mvn -q -Dmaven.test.skip=true clean install

#-----------------------------
#     COMPILE APPLICATION
#-----------------------------
echo "    Compiling application"
cd /tmp/app/CED
sed -i -e 's/ private int reps = 10;/ private int reps = '${FRAMES_COUNT}';/g' /tmp/app/CED/src/main/java/es/bsc/mobile/apps/ced/Params.java
mvn -q -Dmaven.test.skip=true clean
echo "    Compiling "
mvn -q -Dmaven.test.skip=true compile
echo "    Packaging"
mvn -q -Dmaven.test.skip=true package

#-----------------------------
#       CLEAN WORKING DIR
#-----------------------------
adb shell rm -rf /sdcard/COMPSs-Mobile/*.IT

echo "    Preparing logcat"
adb logcat -c
time=`adb shell "date +\"%m-%d %H:%M:%S.000\"" |cut -c1-18`
/usr/bin/konsole --new-tab --hold -e adb logcat -v raw -T "${time}" System.out:V *:S

echo "    Launching application"
mvn -q android:deploy
mvn -q android:run
