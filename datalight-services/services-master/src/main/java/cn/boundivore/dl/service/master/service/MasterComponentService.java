/**
 * Copyright (C) <2023> <Boundivore> <boundivore@foxmail.com>
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Apache License, Version 2.0
 * as published by the Apache Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License, Version 2.0 for more details.
 * <p>
 * You should have received a copy of the Apache License, Version 2.0
 * along with this program; if not, you can obtain a copy at
 * http://www.apache.org/licenses/LICENSE-2.0.
 */
package cn.boundivore.dl.service.master.service;

import cn.boundivore.dl.base.constants.ICommonConstant;
import cn.boundivore.dl.base.enumeration.impl.ClusterTypeEnum;
import cn.boundivore.dl.base.enumeration.impl.NodeStateEnum;
import cn.boundivore.dl.base.enumeration.impl.SCStateEnum;
import cn.boundivore.dl.base.enumeration.impl.ServiceTypeEnum;
import cn.boundivore.dl.base.request.impl.master.AbstractServiceComponentRequest;
import cn.boundivore.dl.base.response.impl.master.AbstractClusterVo;
import cn.boundivore.dl.base.response.impl.master.AbstractServiceComponentVo;
import cn.boundivore.dl.base.response.impl.master.ServiceDependenciesVo;
import cn.boundivore.dl.base.result.Result;
import cn.boundivore.dl.exception.BException;
import cn.boundivore.dl.exception.DatabaseException;
import cn.boundivore.dl.orm.mapper.custom.ComponentNodeMapper;
import cn.boundivore.dl.orm.po.TBasePo;
import cn.boundivore.dl.orm.po.custom.ComponentNodeDto;
import cn.boundivore.dl.orm.po.single.TDlComponent;
import cn.boundivore.dl.orm.po.single.TDlNode;
import cn.boundivore.dl.orm.service.single.impl.TDlComponentServiceImpl;
import cn.boundivore.dl.service.master.converter.IServiceComponentConverter;
import cn.boundivore.dl.service.master.manage.service.bean.ClusterMeta;
import cn.boundivore.dl.service.master.resolver.ResolverYamlServiceDetail;
import cn.boundivore.dl.service.master.resolver.ResolverYamlServiceManifest;
import cn.boundivore.dl.service.master.resolver.yaml.YamlServiceDetail;
import cn.boundivore.dl.service.master.resolver.yaml.YamlServiceManifest;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static cn.boundivore.dl.base.enumeration.impl.SCStateEnum.*;

/**
 * Description: 组件操作相关逻辑
 * Created by: Boundivore
 * E-mail: boundivore@foxmail.com
 * Creation time: 2023/6/15
 * Modification description:
 * Modified by:
 * Modification time:
 * Version: V1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MasterComponentService {

    private final MasterServiceService masterServiceService;

    private final MasterClusterService masterClusterService;

    private final MasterNodeService masterNodeService;

    private final TDlComponentServiceImpl tDlComponentService;

    private final MasterConfigPreService masterConfigPreService;

    private final ComponentNodeMapper componentNodeMapper;

    private final IServiceComponentConverter iServiceComponentConverter;

    private final MasterInitProcedureService masterInitProcedureService;

    /**
     * Description: 获取所有组件信息列表，并附带组件在当前集群的状态
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/7/18
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param clusterId 集群 ID
     * @return Result<AbstractServiceComponentVo.ComponentVo> 当前集群中的所有组件信息
     */
    public Result<AbstractServiceComponentVo.ComponentVo> getComponentList(Long clusterId) {

        // 获取当前集群中服务的状态
        Map<String, AbstractServiceComponentVo.ServiceSummaryVo> serviceSummaryVoMap = this.masterServiceService
                .getServiceSummaryVoMap(clusterId);

        // 过滤掉本轮操作没有被选择的服务
        List<AbstractServiceComponentVo.ServiceSummaryVo> selectedServiceSummaryList = serviceSummaryVoMap.values()
                .stream()
                .filter(i -> i.getScStateEnum() == SELECTED || i.getScStateEnum() == SELECTED_ADDITION)
                .collect(Collectors.toList());

        return Result.success(
                new AbstractServiceComponentVo.ComponentVo(
                        clusterId,
                        ResolverYamlServiceManifest.SERVICE_MANIFEST_YAML
                                .getDataLight()
                                .getDlcVersion(),
                        // 组装 List<ServiceComponentSummaryVo>
                        selectedServiceSummaryList.stream()
                                .map(i -> new AbstractServiceComponentVo.ServiceComponentSummaryVo(
                                                i,
                                                this.getComponentSummaryMap(clusterId, i.getServiceName())
                                        )
                                )
                                .sorted(Comparator.comparing(o -> o.getServiceSummaryVo().getPriority()))
                                .collect(Collectors.toList())
                )
        );
    }

    /**
     * Description: 获取服务名称与组件的映射关系（包括组件在集群中的状态）
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/7/18
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param clusterId   集群 ID
     * @param serviceName 服务名称
     * @return List<AbstractServiceComponentVo.ComponentSummaryVo> 服务名称与组件的映射关系的列表
     */
    private List<AbstractServiceComponentVo.ComponentSummaryVo> getComponentSummaryMap(Long clusterId,
                                                                                       String serviceName) {
        // 根据服务名称获取组件列表，包含组件在集群中的状态
        return ResolverYamlServiceDetail.COMPONENT_LIST_MAP
                .get(serviceName)
                .stream()
                // 将组件信息转换为 ComponentSummaryVo，并设置组件节点信息
                .map(i -> this.iServiceComponentConverter
                        .convert2ComponentSummaryVo(i)
                        .setComponentNodeList(
                                this.getComponentNodeVo(
                                        clusterId,
                                        i.getName()
                                )
                        )
                )
                // 按组件优先级进行排序
                .sorted(Comparator.comparing(AbstractServiceComponentVo.ComponentSummaryVo::getPriority))
                .collect(Collectors.toList());
    }

    /**
     * Description: 获取组件在节点中的分布情况
     * EASY TO FIX: 目前此种操作推测不会出现数据库性能瓶颈，在超大规模集群下，
     * 如果数据库访问速度降低，可考虑一次性读取数据库中的所有组件分布信息，一次性完成所有组装
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/7/18
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param clusterId     集群 ID
     * @param componentName 组件名称
     * @return List<AbstractServiceComponentVo.ComponentNodeVo> 组件节点信息的列表
     */
    private List<AbstractServiceComponentVo.ComponentNodeVo> getComponentNodeVo(Long clusterId,
                                                                                String componentName) {
        // 根据集群 ID 和组件名称查询 TDlComponent 列表
        List<TDlComponent> tDlComponentList = this.tDlComponentService.lambdaQuery()
                .select()
                .eq(TDlComponent::getClusterId, clusterId)
                .eq(TDlComponent::getComponentName, componentName)
                .notIn(TDlComponent::getComponentState, REMOVED, UNSELECTED)
                .list();

        if (!tDlComponentList.isEmpty()) {
            // 提取 TDlComponent 列表中的 nodeId 并转换为 nodeIdList
            List<Long> nodeIdList = tDlComponentList.stream()
                    .map(TDlComponent::getNodeId)
                    .collect(Collectors.toList());

            // 获取 NodeId 对应的 TDlNode 映射关系
            Map<Long, TDlNode> tDlNodeMap = this.masterNodeService.getNodeListInNodeIds(clusterId, nodeIdList)
                    .stream()
                    .filter(i -> i.getNodeState() != NodeStateEnum.REMOVED)
                    .collect(Collectors.toMap(TBasePo::getId, i -> i));

            Assert.isTrue(
                    tDlNodeMap.size() == nodeIdList.size(),
                    () -> new DatabaseException("数据库异常, 组件列表中存在无效的节点 ID")
            );

            // 根据映射关系创建组件节点信息的列表
            return tDlComponentList
                    .stream()
                    .map(i -> new AbstractServiceComponentVo.ComponentNodeVo(
                                    i.getNodeId(),
                                    tDlNodeMap.get(i.getNodeId()).getHostname(),
                                    tDlNodeMap.get(i.getNodeId()).getIpv4(),
                                    tDlNodeMap.get(i.getNodeId()).getNodeState(),
                                    i.getComponentState()
                            )
                    )
                    // 按节点主机名进行排序
                    .sorted(Comparator.comparing(AbstractServiceComponentVo.ComponentNodeVo::getHostname))
                    .collect(Collectors.toList());
        }

        // 若 tDlComponentList 为空，则返回空列表
        return new ArrayList<>();
    }


    /**
     * Description: 选择准备部署的组件以及组件在节点中的分布情况，本接口多次操作为幂等性操作
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/7/17
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param request 选择组件请求体
     * @return 成功返回 success() 失败抛出异常
     */
    @Transactional(
            timeout = ICommonConstant.TIMEOUT_TRANSACTION_SECONDS,
            rollbackFor = DatabaseException.class
    )
    public Result<String> saveComponentSelected(AbstractServiceComponentRequest.ComponentSelectRequest request) {

        // 组件公共检查项
        this.checkComponentCommon(request);

        // 获取数据库中当前集群已存在的组件列表
        // Map<String, TDlComponent> "<组件名称 + 节点 ID, TDlComponent>"与组件实体的映射关系
        Map<String, TDlComponent> tdlComponentMap = this.getComponentListByCluster(request.getClusterId());

        // 创建数据库条目实例
        List<TDlComponent> newTDlComponentList = request.getComponentList()
                .stream()
                .flatMap(i ->
                        i.getNodeIdList()
                                .stream()
                                .map(nodeId -> {
                                    TDlComponent tDlComponent = tdlComponentMap.getOrDefault(
                                            i.getComponentName() + nodeId,
                                            new TDlComponent()
                                    );
                                    SCStateEnum currentSCStateEnum = tDlComponent.getComponentState() == null ?
                                            UNSELECTED :
                                            tDlComponent.getComponentState();

                                    // 使用组件状态机切换状态
                                    SCStateEnum newSCStateEnum = currentSCStateEnum.transitionSelectedComponentState(
                                            i.getScStateEnum()
                                    );

                                    tDlComponent.setClusterId(request.getClusterId());
                                    tDlComponent.setNodeId(nodeId);
                                    tDlComponent.setServiceName(i.getServiceName());
                                    tDlComponent.setComponentName(i.getComponentName());
                                    tDlComponent.setComponentState(newSCStateEnum);
                                    tDlComponent.setPriority(
                                            ResolverYamlServiceDetail.COMPONENT_MAP
                                                    .get(i.getComponentName())
                                                    .getPriority()
                                    );

                                    return tDlComponent;
                                }))
                .collect(Collectors.toList());

        // 检查组件在节点的分布是否合理
        this.checkComponentDistribution(
                request.getClusterId(),
                newTDlComponentList
        );

        Assert.isTrue(
                this.tDlComponentService.saveOrUpdateBatch(newTDlComponentList),
                () -> new DatabaseException("记录组件选项到数据库失败")
        );

        // 清除处于 UNSELECTED 状态的组件记录（以免遗留过多垃圾数据）
        List<TDlComponent> toRemoveTDlComponentIds = newTDlComponentList
                .stream()
                .filter(i -> i.getComponentState() == UNSELECTED)
                .collect(Collectors.toList());

        if (!toRemoveTDlComponentIds.isEmpty()) {
            this.tDlComponentService.removeBatchByIds(toRemoveTDlComponentIds);
        }

        return Result.success();
    }


    /**
     * Description: 组件的公共项检查
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/7/17
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param request 被操作的组件
     */
    private void checkComponentCommon(AbstractServiceComponentRequest.ComponentSelectRequest request) {

        // 检查服务名称与组件名称的对应关系
        request.getComponentList()
                .forEach(i -> {
                            boolean contains = ResolverYamlServiceDetail.COMPONENT_LIST_MAP
                                    .get(i.getServiceName())
                                    .stream()
                                    .map(YamlServiceDetail.Component::getName)
                                    .collect(Collectors.toList())
                                    .contains(i.getComponentName());

                            Assert.isTrue(
                                    contains,
                                    () -> new BException(
                                            String.format(
                                                    "部署意图不合法: 传递了错误的服务、组件包含关系(%s 中不包含 %s)",
                                                    i.getServiceName(),
                                                    i.getComponentName()
                                            )
                                    )
                            );
                        }
                );

        // 检查选择的组件对应的服务状态是否为 SELECTED 或 SELECTED_ADDITION
        request.getComponentList()
                .forEach(i -> {
                    SCStateEnum serviceState = this.masterServiceService.getServiceState(
                            request.getClusterId(),
                            i.getServiceName()
                    );
                    Assert.isTrue(
                            serviceState == SELECTED || serviceState == SELECTED_ADDITION,
                            () -> new BException(
                                    String.format(
                                            "部署意图不合法: 传递了未被选择服务(%s)下的组件",
                                            i.getServiceName()
                                    )
                            )
                    );

                });

        // 检查单次请求中是否重复传递了相同的组件名
        Assert.isTrue(
                request.getComponentList()
                        .stream()
                        .map(AbstractServiceComponentRequest.ComponentRequest::getComponentName)
                        .distinct()
                        .count() == request.getComponentList().size(),
                () -> new BException("单次请求中, 重复传递了相同的组件名称")
        );

        // 检查组件名称是否合法
        request.getComponentList().forEach(
                i -> Assert.notNull(
                        ResolverYamlServiceDetail.COMPONENT_MAP.get(i.getComponentName()),
                        () -> new BException(
                                String.format(
                                        "不存在的组件名称: %s",
                                        i.getComponentName()
                                )
                        )
                )
        );

        // 检查当前集群 ID 是否存在
        AbstractClusterVo.ClusterVo clusterVo = this.masterClusterService.getClusterById(
                request.getClusterId()
        ).getData();

        Assert.notNull(
                clusterVo,
                () -> new BException("未找到对应的集群 ID")
        );

        // 检查组件分布的节点是否存在
        this.masterNodeService.checkNodeExistsById(
                request.getComponentList()
                        .stream()
                        .peek(i -> {
                            // 检查组件列表中的状态值是否合法（前端传递进来只有两种意图：选择 SELECTED 或 不选择 UNSELECTED）
                            Assert.isTrue(
                                    i.getScStateEnum() == SELECTED || i.getScStateEnum() == UNSELECTED,
                                    () -> new BException(
                                            String.format(
                                                    "选择组件 %s 的过程中只允许传递 %s 或 %s 的枚举",
                                                    i.getComponentName(),
                                                    SELECTED,
                                                    UNSELECTED
                                            )
                                    )
                            );

                            // 检查组件的节点列表是否为空
                            Assert.notEmpty(
                                    i.getNodeIdList(),
                                    () -> new BException(
                                            String.format(
                                                    "操作的组件 %s 节点列表为空",
                                                    i.getComponentName()
                                            )
                                    )
                            );
                        })
                        .flatMap(i -> i.getNodeIdList().stream())
                        .distinct()
                        .collect(Collectors.toList())
        );

        request.getComponentList()
                .forEach(i -> {
                    // 检查每个组件中传递的节点 ID 是否存在重复
                    Assert.isTrue(
                            i.getNodeIdList().stream().distinct().count() == i.getNodeIdList().size(),
                            () -> new BException(
                                    String.format(
                                            "%s-%s 传递的节点列表中存在重复的节点 ID",
                                            i.getServiceName(),
                                            i.getComponentName()
                                    )
                            )
                    );
                });
    }

    /**
     * Description: 检查组件在节点中的分布是否合法
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/7/18
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param clusterId           集群 ID
     * @param newTDlComponentList 根据用户选择结果，准备变更的组件
     */
    private void checkComponentDistribution(Long clusterId, List<TDlComponent> newTDlComponentList) {

        //<ComponentName, List<TDlComponent>>
        Map<String, List<TDlComponent>> componentNameNewTDlComponentListMap = newTDlComponentList.stream()
                .collect(Collectors.groupingBy(TDlComponent::getComponentName));


        // 已部署 + 准备部署 的服务
        List<String> prepareServiceNameList = newTDlComponentList.stream()
                .map(TDlComponent::getServiceName)
                .distinct()
                .collect(Collectors.toList());

        // 已部署 + 准备部署 的服务对应的一系列组件
        Map<String, YamlServiceDetail.Component> componentConstantMap = ResolverYamlServiceDetail.SERVICE_MAP
                .values()
                .stream()
                .filter(i -> prepareServiceNameList.contains(i.getName()))
                .flatMap(i -> i.getComponents().stream())
                .collect(Collectors.toMap(
                                YamlServiceDetail.Component::getName,
                                i -> i
                        )
                );

        // 检查未选择的每个服务下的组件最小部署是否合理
        componentConstantMap.forEach(
                (k, v) -> {
                    // 如果当前集群中某个组件没有部署且不准备部署，则检查该组件最小部署个数是否满足 <= 0，不满足，则抛出异常
                    if (componentNameNewTDlComponentListMap.get(k) == null) {
                        Assert.isTrue(
                                v.getMin() <= 0,
                                () -> new BException(
                                        String.format(
                                                "组件 %s 不满足最小部署个数",
                                                k
                                        )
                                )
                        );
                    }
                }
        );


        componentNameNewTDlComponentListMap.forEach(
                (componentName, tdlComponentList) -> {
                    // 从数据库获取当前 “已服役+准备部署” 的组件在节点中的分布情况
                    Set<Long> nodeIdList = this.getComponentDistributionList(
                            clusterId,
                            componentName,
                            tdlComponentList
                    );

                    YamlServiceDetail.Component yamlComponent = ResolverYamlServiceDetail.COMPONENT_MAP.get(componentName);

                    // 检查每个服务下的组件 最小 部署数量是否合理
                    Assert.isTrue(
                            nodeIdList.size() >= yamlComponent.getMin(),
                            () -> new BException(
                                    String.format(
                                            "组件 %s 不满足最小部署数量: %s, 当前值: %s",
                                            componentName,
                                            yamlComponent.getMin(),
                                            nodeIdList.size()
                                    )
                            )
                    );

                    // 检查每个服务下的组件 最大 部署数量是否合理
                    Assert.isTrue(
                            yamlComponent.getMax() == -1 || nodeIdList.size() <= yamlComponent.getMax(),
                            () -> new BException(
                                    String.format(
                                            "组件 %s 不满足最大部署数量: %s, 当前值: %s",
                                            componentName,
                                            yamlComponent.getMax(),
                                            nodeIdList.size()
                                    )
                            )
                    );

                    // 检查组件是否在某些节点存在互斥
                    yamlComponent.getMutexes()
                            .forEach(mutexComponentName -> {
                                Set<Long> mutexNodeIdList = this.getComponentDistributionList(
                                        clusterId,
                                        mutexComponentName,
                                        componentNameNewTDlComponentListMap.getOrDefault(
                                                mutexComponentName,
                                                new ArrayList<>()
                                        )
                                );
                                Assert.isFalse(
                                        mutexNodeIdList.stream().anyMatch(nodeIdList::contains),
                                        () -> new BException(
                                                String.format(
                                                        "组件 %s 与组件 %s 存在部署互斥",
                                                        componentName,
                                                        mutexComponentName
                                                )
                                        )

                                );
                            });


                });

    }

    /**
     * Description: 获得某个组件在集群内的分布情况，返回分布的节点 ID
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/7/18
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @return Set<Long> 返回分布的节点 ID
     */
    private Set<Long> getComponentDistributionList(Long clusterId,
                                                   String componentName,
                                                   List<TDlComponent> newTDlComponentList) {
        // 从数据库获取当前已服役组件在节点中的分布情况
        Set<Long> nodeIdList = this.tDlComponentService
                .lambdaQuery()
                .select()
                .eq(TDlComponent::getClusterId, clusterId)
                .eq(TDlComponent::getComponentName, componentName)
                .notIn(TDlComponent::getComponentState, UNSELECTED, REMOVED)
                .list()
                .stream()
                .map(TDlComponent::getNodeId)
                .collect(Collectors.toSet());

        // 将本次操作的节点加入到数据库列表中
        nodeIdList.addAll(
                newTDlComponentList
                        .stream()
                        .filter(i -> i.getComponentState() != UNSELECTED && i.getComponentState() != REMOVED)
                        .map(TDlComponent::getNodeId)
                        .collect(Collectors.toSet())
        );

        return nodeIdList;
    }

    /**
     * Description: 获取数据库中当前集群已存在的组件列表
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/7/17
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param clusterId 目标集群 ID
     * @return Map<String, TDlComponent> "组件名称 +节点 ID"与组件实体的映射关系
     */
    private Map<String, TDlComponent> getComponentListByCluster(Long clusterId) {
        return this.tDlComponentService.lambdaQuery()
                .select()
                .eq(TDlComponent::getClusterId, clusterId)
                .list()
                .stream()
                .collect(Collectors.toMap(i -> i.getComponentName() + i.getNodeId(), i -> i));
    }

    /**
     * Description: 切换指定组件状态
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/6/15
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param clusterId     集群 ID
     * @param nodeId        节点 ID
     * @param serviceName   服务名称
     * @param componentName 组件名称
     * @param scStateEnum   切换为指定状态
     */
    @Transactional(
            timeout = ICommonConstant.TIMEOUT_TRANSACTION_SECONDS,
            rollbackFor = DatabaseException.class
    )
    public void switchComponentState(Long clusterId,
                                     Long nodeId,
                                     String serviceName,
                                     String componentName,
                                     SCStateEnum scStateEnum) {

        TDlComponent tDlComponent = tDlComponentService.lambdaQuery()
                .select()
                .eq(TDlComponent::getClusterId, clusterId)
                .eq(TDlComponent::getNodeId, nodeId)
                .eq(TDlComponent::getServiceName, serviceName)
                .eq(TDlComponent::getComponentName, componentName)
                .ne(TDlComponent::getComponentState, SCStateEnum.REMOVED)
                .one();

        tDlComponent.setComponentState(scStateEnum);

        Assert.isTrue(tDlComponentService.updateById(
                        tDlComponent),
                () -> new DatabaseException(
                        String.format(
                                "组件 %s 状态 %s 变更失败",
                                componentName,
                                scStateEnum
                        )
                )
        );
    }

    /**
     * Description: 通过组件确定服务的状态枚举 TODO 改为状态机
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/6/15
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param clusterId   集群 ID
     * @param serviceName 服务名称
     * @return SCStateEnum 返回服务应该处于的状态
     */
    public SCStateEnum determineServiceStateViaComponent(Long clusterId, String serviceName) {

        // 查询满足条件的组件列表
        List<TDlComponent> tDlComponentList = tDlComponentService.lambdaQuery()
                .select()
                .eq(TDlComponent::getClusterId, clusterId)
                .eq(TDlComponent::getServiceName, serviceName)
                .list();

        // 提取组件状态并将其收集到状态枚举集合中
        Set<SCStateEnum> scStateEnumSet = tDlComponentList.stream()
                .map(TDlComponent::getComponentState)
                .collect(Collectors.toSet());

        // 如果组件状态枚举集合中只有一种类型的状态（Set 集合已去重）
        // TODO 考虑 SELECTED_ADDITION
        if (scStateEnumSet.size() == 1) {
            // 如果状态枚举集合包含 REMOVED 状态
            if (scStateEnumSet.contains(SCStateEnum.REMOVED)) {
                return SCStateEnum.REMOVED;
            }

            // 如果状态枚举集合包含 SELECTED 状态
            if (scStateEnumSet.contains(SCStateEnum.SELECTED)) {
                return SCStateEnum.SELECTED;
            }

            // 如果状态枚举集合包含 UNSELECTED 状态
            if (scStateEnumSet.contains(SCStateEnum.UNSELECTED)) {
                return SCStateEnum.UNSELECTED;
            }
        }

        // 默认返回 DEPLOYED 状态
        return SCStateEnum.DEPLOYED;
    }


    /**
     * Description: 获取当前集群下，当前服务所依赖服务和组件的分布情况
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/6/19
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param clusterMeta        当前集群与可能存在的关联的集群的元数据信息
     * @param currentServiceName 指定服务名称
     * @return 返回当前集群下，当前服务依赖、以及受影响的服务和组件的分布情况
     */
    public Result<ServiceDependenciesVo> getServiceDependencies(ClusterMeta clusterMeta,
                                                                String currentServiceName) {
        YamlServiceManifest.Service yamlService = ResolverYamlServiceManifest
                .MANIFEST_SERVICE_MAP
                .get(currentServiceName);

        // 当前服务依赖的服务
        List<String> dependencies = yamlService.getDependencies();

        // 去重服务名
        Set<String> relativeService = new HashSet<>(dependencies);
        // 将当前服务自己添加到集合中
        relativeService.add(currentServiceName);

        // 按照部署优先级排序
        List<String> sortedServiceList = relativeService.stream()
                .map(ResolverYamlServiceManifest.MANIFEST_SERVICE_MAP::get)
                .sorted(Comparator.comparing(YamlServiceManifest.Service::getPriority))
                .map(YamlServiceManifest.Service::getName)
                .collect(Collectors.toList());

        // 准备返回内容
        ServiceDependenciesVo serviceDependenciesVo = new ServiceDependenciesVo();
        serviceDependenciesVo.setCurrentServiceName(currentServiceName);

        List<ServiceDependenciesVo.ServiceDetailVo> serviceDetailList = sortedServiceList.stream()
                .map(serviceName -> {

                    // 判断，如果当前依赖的服务为存储服务，且当前集群为 COMPUTE 集群，则应从存储集群中读取服务信息
                    ServiceTypeEnum serviceTypeEnum = ResolverYamlServiceManifest.MANIFEST_SERVICE_MAP
                            .get(serviceName)
                            .getType();

                    ClusterTypeEnum currentClusterTypeEnum = clusterMeta.getCurrentClusterTypeEnum();

                    boolean isFromRelativeCluster = currentClusterTypeEnum == ClusterTypeEnum.COMPUTE
                            && serviceTypeEnum == ServiceTypeEnum.STORAGE;

                    Long clusterId = isFromRelativeCluster ?
                            clusterMeta.getRelativeCusterId() :
                            clusterMeta.getCurrentClusterId();

                    String clusterName = isFromRelativeCluster ?
                            clusterMeta.getRelativeClusterName() :
                            clusterMeta.getCurrentClusterName();

                    ClusterTypeEnum clusterTypeEnum = isFromRelativeCluster ?
                            clusterMeta.getRelativeClusterTypeEnum() :
                            clusterMeta.getCurrentClusterTypeEnum();


                    // 获取当前服务状态
                    SCStateEnum serviceState = this.masterServiceService.getServiceState(clusterId, serviceName);

                    // 容错，过滤无效的信息
                    if (serviceState == SCStateEnum.REMOVED || serviceState == SCStateEnum.UNSELECTED) {
                        log.error("存在不合逻辑的服务状态: {}", serviceState);
                    }

                    // 服务的配置文件目录以及模板目录
                    List<ServiceDependenciesVo.ConfDirVo> confDirList = this.masterConfigPreService.getConfDirList(serviceName);
                    // 服务预配置信息
                    List<ServiceDependenciesVo.PropertyVo> propertyList = this.masterConfigPreService.getPropertyList(clusterId, serviceName);

                    // 获取服务组件详细组件分布情况
                    List<ComponentNodeDto> componentNodeDtoList = this.componentNodeMapper.selectComponentNodeNotInStatesDto(
                            clusterId,
                            serviceName,
                            CollUtil.newArrayList(
                                    SCStateEnum.REMOVED,
                                    SCStateEnum.UNSELECTED
                            )
                    );

                    return new ServiceDependenciesVo.ServiceDetailVo()
                            .setClusterId(clusterId)
                            .setClusterName(clusterName)
                            .setClusterType(clusterTypeEnum)
                            .setServiceName(serviceName)
                            .setServiceState(serviceState)
                            .setConfDirList(confDirList)
                            .setPropertyList(propertyList)
                            .setComponentDetailList(
                                    componentNodeDtoList.stream()
                                            .map(i -> new ServiceDependenciesVo.ComponentDetailVo(
                                                            i.getComponentName(),
                                                            i.getComponentState(),
                                                            i.getNodeId(),
                                                            i.getIpv4(),
                                                            i.getHostname(),
                                                            i.getRam(),
                                                            i.getCpuCores()
                                                    )
                                            )
                                            .collect(Collectors.toList())
                            );
                })
                .collect(Collectors.toList());

        serviceDependenciesVo.setServiceDetailList(serviceDetailList);
        return Result.success(serviceDependenciesVo);
    }

    /**
     * Description: 获取某个集群下对应服务的组件列表
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/7/27
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param clusterId   集群 ID
     * @param serviceName 服务名称
     * @return 返回组件列表
     */
    public List<TDlComponent> getTDlComponentListByServiceNameInNode(Long clusterId,
                                                                     Long nodeId,
                                                                     String serviceName) {
        return this.tDlComponentService.lambdaQuery()
                .select()
                .eq(TDlComponent::getClusterId, clusterId)
                .eq(TDlComponent::getNodeId, nodeId)
                .eq(TDlComponent::getServiceName, serviceName)
                .list();
    }

    /**
     * Description: 获取某集群，某服务下所有组件的状态
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/7/27
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param clusterId   集群 ID
     * @param serviceName 服务名称
     * @return 返回组件列表
     */
    public List<TDlComponent> getTDlComponentListByServiceName(Long clusterId, String serviceName) {
        return this.tDlComponentService.lambdaQuery()
                .select()
                .eq(TDlComponent::getClusterId, clusterId)
                .eq(TDlComponent::getServiceName, serviceName)
                .list();
    }


}