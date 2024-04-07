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
package cn.boundivore.dl.service.worker.service;

import cn.boundivore.dl.base.constants.AutoPullComponentState;
import cn.boundivore.dl.base.request.impl.common.AbstractAutoPullRequest;
import cn.boundivore.dl.base.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Description: 进程自动拉起开关状态切换
 * Created by: Boundivore
 * E-mail: boundivore@foxmail.com
 * Creation time: 2024/3/21
 * Modification description:
 * Modified by:
 * Modification time:
 * Throws:
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorkerAutoPullService {

    /**
     * Description: 将自动拉起 Component 进程的开关切换至目标状态
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2024/3/21
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param request 将开关切换至目标状态
     * @return Result<String> 成功或失败
     */
    public Result<String> switchAutoPullComponent(AbstractAutoPullRequest.AutoPullComponentRequest request) {

        // 创建开关状态缓存
        AutoPullComponentState.CacheBean cacheBean = new AutoPullComponentState.CacheBean();
        cacheBean.setClusterId(request.getClusterId());
        cacheBean.updatePullComponent(request.getAutoPullComponent(), request.getCloseDuration());
        // 设置缓存
        AutoPullComponentState.putAutoPullComponentState(cacheBean);

        return Result.success();
    }

}