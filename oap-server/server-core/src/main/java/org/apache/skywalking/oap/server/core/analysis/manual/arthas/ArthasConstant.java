/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.server.core.analysis.manual.arthas;

public class ArthasConstant {

    public static final String CPU_INDEX_NAME = "dayu_arthas-profile-task-cpu_";
    public static final String FLAME_DIAGRAM_INDEX_NAME = "dayu_arthas-profile-task-flame-diagram_";
    public static final String MEM_INDEX_NAME = "dayu_arthas-profile-task-mem_";
    public static final String THREAD_INDEX_NAME = "dayu_arthas-profile-task-thread_";
    public static final String SYSTEM_INDEX_NAME = "dayu_arthas-profile-task-system_";
    public static final String CLASS_INDEX_NAME = "dayu_arthas-profile-task-class_";
    public static final String DATA_SAMPLING_TIME = "dataSamplingTime";
    public static final String DATA_SAMPLING_TIME_KEYWORD = "dataSamplingTime.keyword";
    public static final String CPU_DATA = "cpuData";
    public static final String SAMPLING_ENUM = "samplingEnum";
    public static final String SAMPLING_ENUM_KEYWORD = "samplingEnum.keyword";
    public static final String STACK_LIST = "stackList";
    public static final String MEM_DATA = "memData";
    public static final String CLASS_NAME_LIST = "classNameList";
    public static final String JVM_INFO = "jvmInfo";
    public static final String SYS_ENV = "sysEnv";
    public static final String SYS_PROP = "sysProp";
    public static final String VM_OPTION = "vmOption";

    public static final String ID = "_id";

    public static final String CREATE_TIME = "createTime";
    public static final String FLAME_DIAGRAM_DATA = "flameDiagramData";
    public static final String FLAME_DIAGRAM_SAMPLING_STATUS = "samplingStatus";

    public static final String DEFAULT_FLAME_DIAGRAM_DATA = "flame diagram data is sampling";

}