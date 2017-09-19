#!/bin/bash

#-----------------------------
#     Script configuration
#-----------------------------

#Used resources
PROXIED_CPU=true
PROXIED_CPU_MGMT="true"
CPU_CORES=1

PROXIED_GPU=true
PROXIED_GPU_MGMT="true"

#Load Balancing
EXECUTION_PLATFORM=""
#EXECUTION_PLATFORM="" 			# Computing Platform picked dynamically
#EXECUTION_PLATFORM="gpu"		# All tasks run on the GPU except for those indicated in the FIX SCHEDULING section to run on CPU
#EXECUTION_PLATFORM="cpu"		# All tasks run on the CPU except for those indicated in the FIX SCHEDULING section to run on CPU

DYNAMIC_SCHEDULING_POLICY=""  # DEFAULT VALUE IS ENERGY
#DYNAMIC_SCHEDULING_POLICY="energy"
#DYNAMIC_SCHEDULING_POLICY="performance"

#application
BATCH_SIZE="128"

#-----------------------------
#     FOLDERS DEFINITION
#-----------------------------
export mvnRepoDir="~/.m2/repository"
export WDir="/tmp/wDir"
export masterDir="${WDir}/master" 
export codeDir="${WDir}/project"

if [ "${BATCH_SIZE}" = "128" ];then
	# conv_forward_3D_1channel
	CONV_1_CPU="[[[3000][596000000][596000000][596000000]][[3000][953600000][953600000][953600000]]]"
	CONV_1_GPU="[[[3000][76000000][76000000][76000000]][[3000][190000000][190000000][190000000]]]"
	#conv_forward_valid
	CONV_CPU="[[[3000][3832000000][3832000000][3832000000]][[3000][6131200000][6131200000][6131200000]]]"
	CONV_GPU="[[[3000][578000000][578000000][578000000]][[3000][1445000000][1445000000][1445000000]]]"
	#relu2D
	RELU_CPU="[[[3000][2000000][2000000][2000000]][[3000][3200000][3200000][3200000]]]"
	RELU_GPU="[[[3000][0][0][0]][[3000][0][0][0]]]"
	#average_pool
	AVG_CPU="[[[3000][42500000][42500000][42500000]][[3000][68000][68000][68000]]]"
	AVG_GPU="[[[3000][8500000][8500000][8500000]][[3000][21250][21250][21250]]]"
	#fully_forward
	FFW_CPU="[[[3000][108500000][108500000][108500000]][[3000][173600][173600][173600]]]"
	FFW_GPU="[[[3000][13500000][13500000][13500000]][[3000][33750][33750][33750]]]"
	#argmax
	MAX_CPU="[[[3000][500000][500000][500000]][[3000][800][800][800]]]"
	MAX_GPU="[[[3000][125000][125000][125000]][[3000][312][312][312]]]"
fi
if [ "${BATCH_SIZE}" = "256" ];then
	# conv_forward_3D_1channel
	CONV_1_CPU="[[[3000][1245000000][1245000000][1245000000]][[3000][1992000000][1992000000][1992000000]]]"
	CONV_1_GPU="[[[3000][153000000][153000000][153000000]][[3000][382500000][382500000][382500000]]]"
	#conv_forward_valid
	CONV_CPU="[[[3000][7572000000][7572000000][7572000000]][[3000][12115200000][12115200000][12115200000]]]"
	CONV_GPU="[[[3000][1160000000][1160000000][1160000000]][[3000][2900000000][2900000000][2900000000]]]"
	#relu2D
	RELU_CPU="[[[3000][2000000][2000000][2000000]][[3000][3200000][3200000][3200000]]]"
	RELU_GPU="[[[3000][0][0][0]][[3000][0][0][0]]]"
	#average_pool
	AVG_CPU="[[[3000][79000000][79000000][79000000]][[3000][126400][126400][126400]]]"
	AVG_GPU="[[[3000][21500000][21500000][21500000]][[3000][53750][53750][53750]]]"
	#fully_forward
	FFW_CPU="[[[3000][202500000][202500000][202500000]][[3000][324000][324000][324000]]]"
	FFW_GPU="[[[3000][28000000][28000000][28000000]][[3000][70000][70000][70000]]]"
	#argmax
	MAX_CPU="[[[3000][500000][500000][500000]][[3000][800][800][800]]]"
	MAX_GPU="[[[3000][125000][125000][125000]][[3000][312][312][312]]]"
fi
if [ "${BATCH_SIZE}" = "512" ];then
	# conv_forward_3D_1channel
	CONV_1_CPU="[[[3000][2377000000][2377000000][2377000000]][[3000][3803200000][3803200000][3803200000]]]"
	CONV_1_GPU="[[[3000][307000000][307000000][307000000]][[3000][767500000][767500000][767500000]]]"
	#conv_forward_valid
	CONV_CPU="[[[3000][15088000000][15088000000][15088000000]][[3000][24140800000][24140800000][24140800000]]]"
	CONV_GPU="[[[3000][2321000000][2321000000][2321000000]][[3000][5802500000][5802500000][5802500000]]]"
	#relu2D
	RELU_CPU="[[[3000][2000000][2000000][2000000]][[3000][3200000][3200000][3200000]]]"
	RELU_GPU="[[[3000][0][0][0]][[3000][0][0][0]]]"
	#average_pool
	AVG_CPU="[[[3000][153500000][153500000][153500000]][[3000][245600][245600][245600]]]"
	AVG_GPU="[[[3000][43500000][43500000][43500000]][[3000][108750][108750][108750]]]"
	#fully_forward
	FFW_CPU="[[[3000][371000000][371000000][371000000]][[3000][593600][593600][593600]]]"
	FFW_GPU="[[[3000][56000000][56000000][56000000]][[3000][140000][140000][140000]]]"
	#argmax
	MAX_CPU="[[[3000][500000][500000][500000]][[3000][800][800][800]]]"
	MAX_GPU="[[[3000][125000][125000][125000]][[3000][312][312][312]]]"
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
if [ "${PROXIED_CPU}" = true ]; then
	original="return new BackEndProxyInterface(\"CPU\", data, false);"
	replace="System.out.println(\"PROXIED CPU\");\n"
	if [ "${PROXIED_CPU_MGMT}" = true ]; then
		replace="${replace}System.out.println(\"Management is also proxied\");\n"
	else
		replace="${replace}System.out.println(\"Management is not proxied\");\n"
	fi
	replace="${replace}return new BackEndProxyInterface(\"CPU\", data, ${PROXIED_CPU_MGMT});"
	sed ":a;N;\$!ba;s/$original/$replace/" ${codeDir}/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/cpuplatforms/CPUPlatform.java > tmpfile
	mv tmpfile ${codeDir}/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/cpuplatforms/CPUPlatform.java
else
	original="return new BackEndProxyInterface(\"CPU\", data, false);"
	replace="System.out.println(\"NO PROXIED CPU\");\n"
	replace="${replace}System.out.println(\"BackEnd corecount \"+coreCount );\n"
	replace="${replace}return new CPUPlatformBackEnd(coreCount, data);"
	sed ":a;N;\$!ba;s/$original/$replace/" ${codeDir}/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/cpuplatforms/CPUPlatform.java > tmpfile
	mv tmpfile ${codeDir}/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/cpuplatforms/CPUPlatform.java
fi


original="ComputingPlatformBackend cpuBackend = new CPUPlatformBackEnd(4, DATA_PROVIDER);"
replace="ComputingPlatformBackend cpuBackend = new CPUPlatformBackEnd(${CPU_CORES}, DATA_PROVIDER);\n"
replace="${replace}System.out.println(\"Runtime has CPU with ${CPU_CORES} cores\");"
sed ":a;N;\$!ba;s/$original/$replace/" ${codeDir}/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/Runtime.java > tmpfile
mv tmpfile ${codeDir}/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/Runtime.java

#-----------------------------
#       PREPARE GPU Platform
#-----------------------------
if [ "${PROXIED_GPU}" = true ]; then
	original="return new BackEndProxyInterface(\"GPU\", new GPUDataProvider(data, onCreationData), true);"
	replace="System.out.println(\"PROXIED GPU\");\n"
	if [ "${PROXIED_CPU_MGMT}" = true ]; then
		replace="${replace}System.out.println(\"Management is also proxied\");\n"
	else
		replace="${replace}System.out.println(\"Management is not proxied\");\n"
	fi
	replace="${replace}return new BackEndProxyInterface(\"GPU\", new GPUDataProvider(data, onCreationData), ${PROXIED_GPU_MGMT});"
	sed ":a;N;\$!ba;s/$original/$replace/" ${codeDir}/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/gpuplatforms/GPUPlatform.java > tmpfile
	mv tmpfile ${codeDir}/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/gpuplatforms/GPUPlatform.java
else
	original="return new BackEndProxyInterface(\"GPU\", new GPUDataProvider(data, onCreationData), true);"
	replace="System.out.println(\"NO PROXIED GPU\");\n"
	replace="${replace}return new GPUPlatformBackEnd(new GPUDataProvider(data, onCreationData));"
	sed ":a;N;\$!ba;s/$original/$replace/" ${codeDir}/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/gpuplatforms/GPUPlatform.java > tmpfile
	mv tmpfile ${codeDir}/runtime/runtime/master/src/main/java/es/bsc/mobile/runtime/types/resources/gpuplatforms/GPUPlatform.java
fi

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
extra="${extra}es.bsc.mobile.apps.ece.Operations.average_pool(OBJECT,OBJECT,INT,OBJECT)=${AVG_CPU},"
extra="${extra}es.bsc.mobile.apps.ece.Operations.conv_forward_3D_1channel(OBJECT,OBJECT,OBJECT,OBJECT,OBJECT)=${CONV_1_CPU},"
extra="${extra}es.bsc.mobile.apps.ece.Operations.conv_forward_valid(OBJECT,OBJECT,OBJECT,OBJECT,OBJECT)=${CONV_CPU},"
extra="${extra}es.bsc.mobile.apps.ece.Operations.fully_forward(OBJECT,OBJECT,OBJECT,OBJECT,OBJECT)=${FFW_CPU},"
extra="${extra}es.bsc.mobile.apps.ece.Operations.relu2D(OBJECT,OBJECT)=${RELU_CPU},"
extra="${extra}es.bsc.mobile.apps.ece.Operations.argmax(OBJECT,OBJECT)=${MAX_CPU}"
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
extra="${extra}raw\/kernels.cl->average_pool(OBJECT,OBJECT,INT,OBJECT)=${AVG_GPU},"
extra="${extra}raw\/kernels.cl->conv_forward_3D_1channel(OBJECT,OBJECT,OBJECT,OBJECT,OBJECT)=${CONV_1_GPU},"
extra="${extra}raw\/kernels.cl->conv_forward_valid(OBJECT,OBJECT,OBJECT,OBJECT,OBJECT)=${CONV_GPU},"
extra="${extra}raw\/kernels.cl->fully_forward(OBJECT,OBJECT,OBJECT,OBJECT,OBJECT)=${FFW_GPU},"
extra="${extra}raw\/kernels.cl->relu2D(OBJECT,OBJECT)=${RELU_GPU},"
extra="${extra}raw\/kernels.cl->argmax(OBJECT,OBJECT)=${MAX_GPU}"
extra="${extra}}\");\n"
extra="${extra}            es.bsc.mobile.runtime.types.resources.StaticAssignationManager.registerPlatform(gpu);\n"

#SELECT DEFAULT EXECUTION PLATFORM
if [[ !  -z  ${EXECUTION_PLATFORM}  ]]; then
	extra="${extra}            es.bsc.mobile.runtime.types.resources.StaticAssignationManager.setDefaultPlatform(${EXECUTION_PLATFORM});\n"
fi
extra="${extra}            System.out.println(\"PREFERRED PLATFORM ${EXECUTION_PLATFORM}\");\n"
extra="${extra}        } catch (Exception defaultException) {\n"
extra="${extra}defaultException.printStackTrace(System.out);\n"
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
	ENERGY_WEIGHT="10"
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
cd ${codeDir}/apps/DR
sed -i -e 's/batchSize = 10;/batchSize = '${BATCH_SIZE}';/g' ${codeDir}/apps/DR/src/main/java/es/bsc/mobile/apps/ece/MainActivity.java
mvn -q -Dmaven.test.skip=true clean
echo "    Compiling "
mvn -q -Dmaven.test.skip=true compile
echo "    Packaging"
mvn -q -Dmaven.test.skip=true package

echo "    Preparing logcat"
/home/flordan/Android/Android-4.4/sdk/platform-tools/adb logcat -c
time=`adb shell "date +\"%m-%d %H:%M:%S.000\"" |cut -c1-18`
/usr/bin/konsole --new-tab --hold -e /home/flordan/Android/Android-4.4/sdk/platform-tools/adb logcat -v time -T "${time}" System.out:D *:S

echo "    Launching application"
mvn -q android:deploy
mvn -q android:run
