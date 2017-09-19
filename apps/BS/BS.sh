#!/bin/bash

#-----------------------------
#     Script configuration
#-----------------------------

#Used resources
CPU_CORES=1

#Load Balancing
EXECUTION_PLATFORM=""
#EXECUTION_PLATFORM="" 			# Computing Platform picked dynamically
#EXECUTION_PLATFORM="gpu"		# All tasks run on the GPU except for those indicated in the FIX SCHEDULING section to run on CPU
#EXECUTION_PLATFORM="cpu"		# All tasks run on the CPU except for those indicated in the FIX SCHEDULING section to run on GPU

DYNAMIC_SCHEDULING_POLICY=""  # DEFAULT VALUE IS ENERGY
#DYNAMIC_SCHEDULING_POLICY="energy"
#DYNAMIC_SCHEDULING_POLICY="performance"


#application
TILE_SIZE="512"

#-----------------------------
#     FOLDERS DEFINITION
#-----------------------------
export mvnRepoDir="~/.m2/repository"
export WDir="/tmp/wDir"
export masterDir="${WDir}/master" 
export codeDir="${WDir}/project"


# INITIAL PROFILE VALUES
if [ "${TILE_SIZE}" = "128" ];then
	if [ "${CPU_CORES}" = "1" ];then
		PROFILE_CPU="[[[3000][124000000][124000000][124000000]][[3000][198000000][198000000][198000000]]]"
	fi
	if [ "${CPU_CORES}" = "2" ];then
		PROFILE_CPU="[[[3000][141000000][141000000][141000000]][[3000][225887500][225887500][225887500]]]"
	fi
	if [ "${CPU_CORES}" = "3" ];then
		PROFILE_CPU="[[[3000][165000000][165000000][165000000]][[3000][264369246][264369246][264369246]]]"
	fi
	if [ "${CPU_CORES}" = "4" ];then
		PROFILE_CPU="[[[3000][179000000][179000000][179000000]][[3000][286950000][286950000][286950000]]]"
	fi
	PROFILE_GPU="[[[3000][41000000][41000000][41000000]][[3000][10240234][10240234][10240234]]]"
fi
if [ "${TILE_SIZE}" = "256" ];then
	if [ "${CPU_CORES}" = "1" ];then
		PROFILE_CPU="[[[3000][499000000][499000000][499000000]][[3000][798400000][798400000][798400000]]]"
	fi
	if [ "${CPU_CORES}" = "2" ];then
		PROFILE_CPU="[[[3000][571000000][571000000][571000000]][[3000][913600000][913600000][913600000]]]"
	fi
	if [ "${CPU_CORES}" = "3" ];then
		PROFILE_CPU="[[[3000][660000000][660000000][660000000]][[3000][1056000000][1056000000][1056000000]]]"
	fi
	if [ "${CPU_CORES}" = "4" ];then
		PROFILE_CPU="[[[3000][725000000][725000000][725000000]][[3000][1160000000][1160000000][1160000000]]]"
	fi
	PROFILE_GPU="[[[3000][167000000][167000000][167000000]][[3000][41750000][41750000][41750000]]]"
fi
if [ "${TILE_SIZE}" = "512" ];then
	if [ "${CPU_CORES}" = "1" ];then
		PROFILE_CPU="[[[3000][2001000000][2001000000][2001000000]][[3000][3201600000][3201600000][3201600000]]]"
	fi
	if [ "${CPU_CORES}" = "2" ];then
		PROFILE_CPU="[[[3000][2314000000][2314000000][2314000000]][[3000][3702400000][3702400000][3702400000]]]"
	fi
	if [ "${CPU_CORES}" = "3" ];then
		PROFILE_CPU="[[[3000][2665000000][2665000000][2665000000]][[3000][4264000000][4264000000][4264000000]]]"
	fi
	if [ "${CPU_CORES}" = "4" ];then
		PROFILE_CPU="[[[3000][2950000000][2950000000][2950000000]][[3000][4720000000][4720000000][4720000000]]]"
	fi
	PROFILE_GPU="[[[3000][673000000][673000000][673000000]][[3000][168187500][168187500][168187500]]]"
fi
if [ "${TILE_SIZE}" = "1024" ];then
	PROFILE_CPU="[[[3000][8035000000][8035000000][8035000000]][[3000][12856000000][12856000000][12856000000]]]"
	PROFILE_GPU="[[[3000][2692000000][2692000000][2692000000]][[3000][673000000][673000000][673000000]]]"
fi




#-----------------------------
#       COPY PROJECT
#-----------------------------
rm -rf ${WDir}
mkdir -p ${WDir}/
cp -r /tmp/WACCPD ${codeDir}
cd ${codeDir}


#-----------------------------
#       PREPARE Master
#-----------------------------
git co ${MASTER_BRANCH}
echo "PREPARING MASTER"
echo "    Preparing runtime"
cd ${codeDir}/runtime 

#-----------------------------
#       PREPARE CPU Platform
#-----------------------------

original="return new BackEndProxyInterface(\"CPU\", data, false);"
replace="System.out.println(\"PROXIED CPU\");\n"
replace="${replace}System.out.println(\"Management is also proxied\");\n"
replace="${replace}return new BackEndProxyInterface(\"CPU\", data, true);"
sed ":a;N;\$!ba;s/$original/$replace/" ${codeDir}/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/cpuplatforms/CPUPlatform.java > tmpfile
mv tmpfile ${codeDir}/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/cpuplatforms/CPUPlatform.java

original="ComputingPlatformBackend cpuBackend = new CPUPlatformBackEnd(4, DATA_PROVIDER);"
replace="ComputingPlatformBackend cpuBackend = new CPUPlatformBackEnd(${CPU_CORES}, DATA_PROVIDER);\n"
replace="${replace}System.out.println(\"Runtime has CPU with ${CPU_CORES} cores\");"
sed ":a;N;\$!ba;s/$original/$replace/" ${codeDir}/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/Runtime.java > tmpfile
mv tmpfile ${codeDir}/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/Runtime.java

#-----------------------------
#       PREPARE GPU Platform
#-----------------------------

original="return new BackEndProxyInterface(\"GPU\", new GPUDataProvider(data, onCreationData), true);"
replace="System.out.println(\"PROXIED GPU\");\n"
replace="${replace}System.out.println(\"Management is also proxied\");\n"
replace="${replace}return new BackEndProxyInterface(\"GPU\", new GPUDataProvider(data, onCreationData), true);"
sed ":a;N;\$!ba;s/$original/$replace/" ${codeDir}/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/gpuplatforms/GPUPlatform.java > tmpfile
mv tmpfile ${codeDir}/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/gpuplatforms/GPUPlatform.java


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
extra="${extra}es.bsc.mobile.apps.bs.Operations.processTile(OBJECT,INT,INT,INT,INT,INT,INT,INT)=${PROFILE_CPU}"
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
extra="${extra}raw\/kernels.cl->processTile(OBJECT,INT,INT,INT,INT,INT,INT,INT)=${PROFILE_GPU}"
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

sed ":a;N;\$!ba;s/$original/$extra\n$original/" ${codeDir}/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/utils/ResourceManager.java > tmpfile
mv tmpfile ${codeDir}/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/utils/ResourceManager.java
sed -i -e 's/load(rh);/loadDefaults(rh);/g' ${codeDir}/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/utils/ResourceManager.java

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
# sed ":a;N;\$!ba;s/$original/$original\n$extra/" runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/StaticAssignationManager.java > tmpfile
# mv tmpfile runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/StaticAssignationManager.java

#----------------------------------------------------------
#       SETTING DYNAMIC POLICY WEIGHTS
#----------------------------------------------------------

if [ "${DYNAMIC_SCHEDULING_POLICY}" = "performance" ]; then
	ENERGY_WEIGHT="0"
else	
	ENERGY_WEIGHT="5"
fi
sed -i -e 's/ private static final int ENERGY_WEIGHT = 10;/ private static final int ENERGY_WEIGHT = '${ENERGY_WEIGHT}';/g' ${codeDir}/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/ComputingPlatform.java

#-----------------------------
#       ARRAY ACCESS BUGFIX
#-----------------------------
sed -i -e 's/m.instrument(converter);//g' ${codeDir}/runtime/programmingModel/parallelizer/src/main/java/es/bsc/mobile/parallelizer/commands/Instrument.java


#-----------------------------
#       COMPILE RUNTIME
#-----------------------------
rm -rf ${mvnRepoDir}/es/bsc
echo "    Compiling runtime"
cd ${codeDir}/runtime 
mvn -q -Dmaven.test.skip=true clean install

echo "    Compiling application"
cd ${codeDir}/apps/BS
sed -i -e 's/ private final int nTileSize = 16;/ private int nTileSize = '${TILE_SIZE}';/g' ${codeDir}/apps/BS/src/main/java/es/bsc/mobile/apps/bs/Params.java
sed -i -e 's/ private final int outSizeI = 512;/ private int outSizeI = 1024;/g' ${codeDir}/apps/BS/src/main/java/es/bsc/mobile/apps/bs/Params.java
sed -i -e 's/ private final int outSizeJ = 512;/ private int outSizeJ = 1024;/g' ${codeDir}/apps/BS/src/main/java/es/bsc/mobile/apps/bs/Params.java
mvn -q -Dmaven.test.skip=true clean
mvn -q -Dmaven.test.skip=true compile
mvn -q -Dmaven.test.skip=true package

#-----------------------------
#       CLEAN WORKING DIR
#-----------------------------
adb shell rm /sdcard/COMPSs-Mobile/*.IT

echo "    Preparing logcat"
/home/flordan/Android/Android-4.4/sdk/platform-tools/adb logcat -c
time=`adb shell "date +\"%m-%d %H:%M:%S.000\"" |cut -c1-18`
/usr/bin/konsole --new-tab --hold -e /home/flordan/Android/Android-4.4/sdk/platform-tools/adb logcat -v time -T "${time}" System.out:D *:S

echo "    Launching application"
mvn -q android:deploy
mvn -q android:run