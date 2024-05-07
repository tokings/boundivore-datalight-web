#!/bin/bash

# 全局路径变量参考：
# DATALIGHT_DIR="/opt/datalight"
# SERVICE_DIR="/srv/datalight"
# LOG_DIR="/data/datalight/logs"
# PID_DIR="/data/datalight/pids"
# DATA_DIR="/data/datalight/data"

YARN_HOME="${SERVICE_DIR}/YARN"
SPARK_HDFS_DIR="$1"

"${YARN_HOME}/bin/hadoop" fs -test -e "${SPARK_HDFS_DIR}"

if [ $? -eq 0 ] ;then
    echo "${SPARK_HDFS_DIR} already exists."
    su -s /bin/bash hadoop -c "${YARN_HOME}/bin/hadoop fs -chmod  -R 777 ${SPARK_HDFS_DIR}"
else
    echo "${SPARK_HDFS_DIR} does not exist.Creating..."
    su -s /bin/bash hadoop -c "${YARN_HOME}/bin/hadoop fs -mkdir -p ${SPARK_HDFS_DIR}"
    su -s /bin/bash hadoop -c "${YARN_HOME}/bin/hadoop fs -chmod  -R 777 ${SPARK_HDFS_DIR}"
fi