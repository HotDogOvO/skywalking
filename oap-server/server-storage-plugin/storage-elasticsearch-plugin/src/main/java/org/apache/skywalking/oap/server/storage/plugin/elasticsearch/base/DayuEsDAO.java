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

package org.apache.skywalking.oap.server.storage.plugin.elasticsearch.base;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.network.arthas.v3.*;
import org.apache.skywalking.apm.network.dayu.v3.Machine;
import org.apache.skywalking.apm.network.dayu.v3.MachineMetric;
import org.apache.skywalking.library.elasticsearch.requests.search.*;
import org.apache.skywalking.library.elasticsearch.response.search.SearchHit;
import org.apache.skywalking.library.elasticsearch.response.search.SearchResponse;
import org.apache.skywalking.oap.server.core.analysis.manual.arthas.FlameDiagramSamplingStatus;
import org.apache.skywalking.oap.server.core.storage.model.arthas.CpuStack;
import org.apache.skywalking.oap.server.core.analysis.manual.arthas.ArthasConstant;
import org.apache.skywalking.oap.server.core.analysis.manual.machine.MachineConstant;
import org.apache.skywalking.oap.server.core.storage.IDayuDAO;
import org.apache.skywalking.oap.server.library.client.elasticsearch.ElasticSearchClient;
import org.apache.skywalking.oap.server.library.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class DayuEsDAO extends EsDAO implements IDayuDAO {

    public DayuEsDAO(ElasticSearchClient client) {
        super(client);
    }

    @Override
    public void saveMachineMetrics(Machine machine) {
        try {
            boolean exists = getClient().isExistsIndex(MachineConstant.INDEX_NAME);
            if (!exists) {
                getClient().createIndex(MachineConstant.INDEX_NAME);
            }
            String serviceName = machine.getServiceName();
            String instanceName = machine.getInstanceName();
            for (MachineMetric machineMetric : machine.getMetricsList()) {
                Map<String, Object> map = Maps.newHashMap();
                map.put("serviceName", serviceName);
                map.put("instanceName", instanceName);
                map.put("cpuCore", machineMetric.getCpuCore());
                map.put("memoryTotal", machineMetric.getMemoryTotal());
                map.put("processCount", machineMetric.getProcessCount());
                map.put("threadCount", machineMetric.getThreadCount());
                map.put("systemCpuUsed", machineMetric.getSystemCpuUsed());
                map.put("processCpuUsed", machineMetric.getProcessCpuUsed());
                map.put("machineMemoryUsed", machineMetric.getMachineMemoryUsed());
                map.put("netRecv", machineMetric.getNetRecv());
                map.put("netSent", machineMetric.getNetSent());
                map.put("time", machineMetric.getTime());
                map.put("date", new Date(machineMetric.getTime()));
                getClient().forceInsert(MachineConstant.INDEX_NAME, UUID.randomUUID().toString(), map);
            }
        } catch (Exception e) {
            log.error("save process machine data fail, {}", e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void saveArthasData(ArthasSamplingData arthasSamplingData) {
        switch (arthasSamplingData.getSamplingEnum()) {
            case CPU:
                saveCpuData(arthasSamplingData);
                break;
            case MEM:
                saveMemData(arthasSamplingData);
                break;
            case SYSTEM:
                saveSystemData(arthasSamplingData);
                break;
            case CLASS:
                saveClassData(arthasSamplingData);
                break;
        }
    }

    @Override
    public void saveFlameDiagramData(Integer profileTaskId,
                                     String flameDiagramId,
                                     String flameDiagramData, FlameDiagramSamplingStatus status) {
        try {
            String indexName = ArthasConstant.FLAME_DIAGRAM_INDEX_NAME + profileTaskId;
            boolean exists = getClient().isExistsIndex(indexName);
            if (!exists) {
                getClient().createIndex(indexName);
            }
            Map<String, Object> map = Maps.newHashMap();
            map.put(ArthasConstant.FLAME_DIAGRAM_DATA, flameDiagramData);
            map.put(ArthasConstant.CREATE_TIME, new Date());
            map.put(ArthasConstant.FLAME_DIAGRAM_SAMPLING_STATUS, status);
            getClient().forceInsert(indexName, flameDiagramId, map);
        } catch (Exception e) {
            log.error("save flame diagram data fail, {}", e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void updateFlameDiagramData(Integer profileTaskId, String flameDiagramId, String flameDiagramData, FlameDiagramSamplingStatus status) {
        try {
            String indexName = ArthasConstant.FLAME_DIAGRAM_INDEX_NAME + profileTaskId;
            Map<String, Object> map = Maps.newHashMap();
            map.put(ArthasConstant.FLAME_DIAGRAM_DATA, flameDiagramData);
            map.put(ArthasConstant.CREATE_TIME, new Date());
            map.put(ArthasConstant.FLAME_DIAGRAM_SAMPLING_STATUS, status);
            getClient().forceUpdate(indexName, flameDiagramId, map);
        } catch (Exception e) {
            log.error("update flame diagram data fail, {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkIndexName(String indexName) {
        boolean exists = getClient().isExistsIndex(indexName);
        if (!exists) {
            getClient().createIndex(indexName);
        }
    }

    private void saveCpuData(ArthasSamplingData arthasSamplingData) {
        try {
            int profileTaskId = arthasSamplingData.getProfileTaskId();
            String indexName = ArthasConstant.CPU_INDEX_NAME + profileTaskId;
            checkIndexName(indexName);

            Map<String, Object> map = Maps.newHashMap();
            map.put("dataSamplingTime", arthasSamplingData.getDataSamplingTime());
            map.put("samplingEnum", arthasSamplingData.getSamplingEnum());
            map.put("cpuData", arthasSamplingData.getCpuData());
            List<CpuStack> cpuStackList = arthasSamplingData.getStackListList().stream()
                    .map(x -> CpuStack.builder().cpu(x.getCpu()).id(x.getId())
                            .state(x.getState()).name(x.getName())
                            .priority(x.getPriority()).group(x.getGroup()).build())
                    .collect(Collectors.toList());
            map.put("stackList", cpuStackList);
            getClient().forceInsert(indexName, UUID.randomUUID().toString(), map);
            saveThreadData(profileTaskId, arthasSamplingData.getDataSamplingTime(), cpuStackList);
        } catch (Exception e) {
            log.error("save arthas profile task cpu data fail, {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveMemData(ArthasSamplingData arthasSamplingData) {
        try {
            int profileTaskId = arthasSamplingData.getProfileTaskId();
            String indexName = ArthasConstant.MEM_INDEX_NAME + profileTaskId;
            checkIndexName(indexName);

            Map<String, Object> map = Maps.newHashMap();
            map.put("dataSamplingTime", arthasSamplingData.getDataSamplingTime());
            map.put("samplingEnum", arthasSamplingData.getSamplingEnum());
            Gson gson = new Gson();
            String memoryData = gson.toJson(arthasSamplingData.getMemoryData());
            map.put("memData", memoryData);
            getClient().forceInsert(indexName, UUID.randomUUID().toString(), map);
        } catch (Exception e) {
            log.error("save arthas profile task mem data fail, {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveSystemData(ArthasSamplingData arthasSamplingData) {
        try {
            int profileTaskId = arthasSamplingData.getProfileTaskId();
            String indexName = ArthasConstant.SYSTEM_INDEX_NAME + profileTaskId;
            checkIndexName(indexName);

            Map<String, Object> map = Maps.newHashMap();
            SystemData systemData = arthasSamplingData.getSystemData();
            map.put("jvmInfo", systemData.getJvmInfo());
            map.put("sysEnv", systemData.getSysEnv());
            map.put("sysProp", systemData.getSysProp());
            map.put("vmOption", systemData.getVmOption());
            getClient().forceInsert(indexName, UUID.randomUUID().toString(), map);
        } catch (Exception e) {
            log.error("save arthas profile task system data fail, {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveClassData(ArthasSamplingData arthasSamplingData) {
        try {
            int profileTaskId = arthasSamplingData.getProfileTaskId();
            String indexName = ArthasConstant.CLASS_INDEX_NAME + profileTaskId;
            checkIndexName(indexName);

            ClassData classData = arthasSamplingData.getClassData();
            List<String> classNameList = classData.getClassNameList();

            Map<String, Object> map = Maps.newHashMap();
            ElasticSearchClient client = getClient();
            map.put("classNameList", classNameList);
            client.forceInsert(indexName, UUID.randomUUID().toString(), map);
        } catch (Exception e) {
            log.error("save arthas profile task class data fail, {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveThreadData(Integer profileTaskId, String dataSamplingTime, List<CpuStack> cpuStackList) {
        String indexName = ArthasConstant.THREAD_INDEX_NAME + profileTaskId;
        checkIndexName(indexName);

        Map<String, Object> map = Maps.newHashMap();
        map.put("dataSamplingTime", dataSamplingTime);

        List<CpuStack> lastThreadList = getLastThreadData(profileTaskId);
        int threadAddCount = 0;
        int threadDecCount = 0;
        List<CpuStack> threadAddList = Lists.newArrayList();
        List<CpuStack> threadDecList = Lists.newArrayList();

        if (CollectionUtils.isNotEmpty(lastThreadList)) {
            threadAddList = cpuStackList.stream()
                    .filter(x -> !lastThreadList.stream().map(CpuStack::getName).collect(Collectors.toList()).contains(x.getName()))
                    .collect(Collectors.toList());
            threadDecList = lastThreadList.stream()
                    .filter(x -> !cpuStackList.stream().map(CpuStack::getName).collect(Collectors.toList()).contains(x.getName()))
                    .collect(Collectors.toList());
            threadAddCount = threadAddList.size();
            threadDecCount = threadDecList.size();
        }

        int threadTotal = cpuStackList.size();
        map.put("threadTotal", threadTotal);
        map.put("threadAddCount", threadAddCount);
        map.put("threadDecCount", threadDecCount);
        map.put("threadAddList", threadAddList);
        map.put("threadDecList", threadDecList);

        getClient().forceInsert(indexName, UUID.randomUUID().toString(), map);
    }

    private List<CpuStack> getLastThreadData(Integer profileTaskId) {
        String cpuIndexName = ArthasConstant.CPU_INDEX_NAME + profileTaskId;
        SearchBuilder builder = Search.builder();
        BoolQueryBuilder boolQueryBuilder = Query.bool();
        boolQueryBuilder.must(Query.term(ArthasConstant.SAMPLING_ENUM_KEYWORD, SamplingEnum.CPU));

        builder.query(boolQueryBuilder);
        builder.source(ArthasConstant.STACK_LIST);
        builder.sort(ArthasConstant.DATA_SAMPLING_TIME_KEYWORD, Sort.Order.DESC);
        builder.size(1);

        List<CpuStack> result = Lists.newArrayList();

        boolean hasIndex = getClient().isExistsIndex(cpuIndexName);
        if (!hasIndex) {
            return result;
        }

        SearchResponse response = getClient().search(cpuIndexName, builder.build());
        for (SearchHit hit : response.getHits().getHits()) {
            List<Map<String, Object>> stackListData = (List<Map<String, Object>>) hit.getSource().get(ArthasConstant.STACK_LIST);
            stackListData.forEach(x -> {
                CpuStack.CpuStackBuilder cpuStackBuilder = CpuStack.builder();
                cpuStackBuilder.id((Integer) x.get("id")).cpu((Double) x.get("cpu"))
                        .state((String) x.get("state")).name((String) x.get("name"))
                        .priority((Integer) x.get("priority")).group((String) x.get("group"));
                result.add(cpuStackBuilder.build());
            });
        }
        return result;
    }

//    public static void main(String[] args) {
//        ElasticSearchClient elasticSearchClient = new ElasticSearchClient(
//                "172.21.0.246:9200", "http", "", "",
//                "", "",
//                indexNameConverter(""), 3000,
//                30000, 15000, 0);
//        elasticSearchClient.connect();
//
//        DayuEsDAO dayuEsDAO = new DayuEsDAO(elasticSearchClient);
//        List<CpuStack> lastThreadData = dayuEsDAO.getLastThreadData(2034070);
//        dayuEsDAO.saveThreadData(2034322, "2024-04-08 13:59:23", lastThreadData );
//    }

}
