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
package cn.boundivore.dl.plugin.kafka.config;

import cn.boundivore.dl.plugin.base.bean.PluginConfig;
import cn.boundivore.dl.plugin.base.config.AbstractConfigLogic;

import java.io.File;

import static cn.boundivore.dl.plugin.kafka.config.ConfigLogicJmxYaml.SERVICE_NAME;

/**
 * Description: 配置 kafka-run-class.sh 文件
 * Created by: Boundivore
 * E-mail: boundivore@foxmail.com
 * Creation time: 2024/5/7
 * Modification description:
 * Modified by:
 * Modification time:
 * Version: V1.0
 */
public class ConfigLogicKafkaRunClassSh extends AbstractConfigLogic {

    public ConfigLogicKafkaRunClassSh(PluginConfig pluginConfig) {
        super(pluginConfig);
    }

    @Override
    public String config(File file, String replacedTemplated) {
        super.printFilename(
                pluginConfig.getCurrentMetaComponent().getHostname(),
                file
        );

        // {{KAFKA_LOG_DIR}}
        String kafkaLogDir = this.kafkaLogDir();

        return replacedTemplated
                .replace(
                        "{{KAFKA_LOG_DIR}}",
                        kafkaLogDir
                )
                ;
    }

    /**
     * Description: 获取 Kafka 日志存储目录
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2024/5/7
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @return String Kafka 日志存储目录
     */
    private String kafkaLogDir() {
        return String.format(
                "%s/%s",
                super.logDir(),
                SERVICE_NAME
        );
    }
}
