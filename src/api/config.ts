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
interface Actions {
	[key: string]: string;
}

const APIConfig: Actions = {
	// auth
	login: '/v1/master/user/login',
	isLogin: '/v1/master/user/isLogin',
	logout: '/v1/master/user/logout',
	changePassword: '/v1/master/user/changePassword',
	// 集群相关
	getClusterList: '/v1/master/cluster/getClusterList',
	createCluster: '/v1/master/cluster/new',
	getDLCVersion: '/v1/master/dlc/service/list',
	parseHostname: '/v1/master/node/init/hostname/parse',
	nodeInitList: '/v1/master/node/init/parse/list', //轮询接口
	detect: '/v1/master/node/init/detect',
	detectList: '/v1/master/node/init/detect/list', //轮询接口
	check: '/v1/master/node/init/check',
	checkList: '/v1/master/node/init/check/list', //轮询接口
	dispatch: '/v1/master/node/init/dispatch',
	dispatchList: '/v1/master/node/init/dispatch/list',
	dispatchProgress: '/v1/master/node/job/dispatch/progress', //轮询接口
	startWorker: '/v1/master/node/init/startWorker',
	startWorkerList: '/v1/master/node/init/startWorker/list', //轮询接口
	add: '/v1/master/node/init/add',
	getProcedure: '/v1/master/init/procedure/get',
	setProcedure: '/v1/master/init/procedure/persist',
	removeCluster: '/v1/master/cluster/remove',
	//节点相关
	nodeList: '/v1/master/node/list',
	nodeListWithComponent: '/v1/master/node/listWithComponent',
	removeNode: '/v1/master/node/removeBatchByIds', // 支持批量删除
	operateNode: '/v1/master/node/operate',
	// 服务相关
	serviceList: '/v1/master/service/list',
	selectService: '/v1/master/service/select',
	// 组件相关
	componentList: '/v1/master/component/list',
	selectComponent: '/v1/master/component/select',
	preconfigList: '/v1/master/config/pre/list',
	preconfigSave: '/v1/master/config/pre/save',
	// 部署相关
	deploy: '/v1/master/deploy',
	jobProgress: '/v1/master/job/progress'
};
for (let key in APIConfig) {
	// APIConfig[key] = '/mock/2601924' + APIConfig[key];
	APIConfig[key] = '/api' + APIConfig[key];
}

export default APIConfig;