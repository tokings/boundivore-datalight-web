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
package cn.boundivore.dl.service.master.grafana;

import cn.boundivore.dl.api.third.define.IThirdGrafanaAPI;
import cn.boundivore.dl.base.result.Result;
import cn.boundivore.dl.service.master.service.RemoteInvokeGrafanaService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.PostConstruct;

/**
 * Description: 测试 Grafana API
 * Created by: Boundivore
 * E-mail: boundivore@foxmail.com
 * Creation time: 2023/8/16
 * Modification description:
 * Modified by:
 * Modification time:
 * Version: V1.0
 */
@SpringBootTest
//@TestPropertySource(locations = "classpath:application-test.yaml")
@Slf4j
public class GrafanaTest {

    @Autowired
    private RemoteInvokeGrafanaService remoteInvokeGrafanaService;
    private final static String GRAFANA_HOST = "node01";
    private final static String GRAFANA_PORT = "3000";


    private final static String GRAFANA_USER_ADMIN = "admin";
    private final static String GRAFANA_PASSWORD_ADMIN = "admin";

    private IThirdGrafanaAPI iThirdGrafanaAPI;

    @PostConstruct
    public void init() {
        this.iThirdGrafanaAPI = this.remoteInvokeGrafanaService.iThirdGrafanaAPI(
                GRAFANA_HOST,
                GRAFANA_PORT,
                GRAFANA_USER_ADMIN,
                GRAFANA_PASSWORD_ADMIN
        );
    }

    /**
     * Description: 1 获取账号信息
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/8/16
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     */
    @Test
    public void createOrg() {
        Result<String> result = this.remoteInvokeGrafanaService.createOrg(
                this.iThirdGrafanaAPI,
                "datalight"
        );

        log.info(result.toString());
    }

    /**
     * Description: 2 创建用户
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/8/16
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     */
    @Test
    public void createUsers() {
        Result<String> result = this.remoteInvokeGrafanaService.createUsers(
                this.iThirdGrafanaAPI,
                "datalight",
                "datalight",
                "datalight"
        );
        log.info(result.toString());
    }

    /**
     * Description: 获取状态信息
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/8/21
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     */
    @Test
    public void getStats() {
        Result<String> result = this.remoteInvokeGrafanaService.getStats(
                this.iThirdGrafanaAPI
        );
        log.info(result.toString());
    }
}
