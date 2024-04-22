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
/**
 * DeployStep - 部署步骤
 * @author Tracy
 */
import { forwardRef, useImperativeHandle, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Table, Progress, Typography, Alert } from 'antd';
import { useTranslation } from 'react-i18next';
import type { ColumnsType } from 'antd/es/table';
import useStore from '@/store/store';
import APIConfig from '@/api/config';
import RequestHttp from '@/api';
import usePolling from '@/hooks/usePolling';
import useStepLogic from '@/hooks/useStepLogic';
import JobPlanModal from '@/components/jobPlanModal';
import LogModal from '@/components/logModal';
import { NodeType, ExecProgressPerNodeVo, ExecProgressStepVo } from '@/api/interface';

const { Text } = Typography;

const twoColors = { '0%': '#108ee9', '100%': '#87d068' };
const disabledState = ['RUNNING', 'SUSPEND', 'ERROR'];
const preStepName = 'previewStep'; // 当前步骤页面基于上一步的输入和选择生成
const stepName = 'deployStep'; // 当前步骤结束时需要存储步骤数据
const operation = 'DEPLOY'; // 当前步骤操作，NodeActionTypeEnum
// const preStepName = 'dispachStep'; // 当前步骤页面基于上一步的输入和选择生成
// const stepName = 'deployStep'; // 当前步骤结束时需要存储步骤数据
const DeployStep: React.FC = forwardRef((_props, ref) => {
	const { t } = useTranslation();
	const [searchParams] = useSearchParams();
	const id = searchParams.get('id');
	const { stableState, jobId, setJobId, setCurrentPageDisabled, isRefresh } = useStore();
	// const [selectedRowsList, setSelectedRowsList] = useState<NodeType[]>([]);
	const [openAlert, setOpenAlert] = useState(false);
	const [deployState, setDeployState] = useState(true);
	const [isModalOpen, setIsModalOpen] = useState(false);
	const [isLogModalOpen, setIsLogModalOpen] = useState(false);
	const [activeNodeId, setActiveNodeId] = useState('');
	const { useGetSepData } = useStepLogic();
	const { webState } = useGetSepData(preStepName, stepName); //获取前后步骤操作存储的数据
	const errorText = t('任务执行异常');
	const columns: ColumnsType<NodeType> = [
		{
			title: t('node.node'),
			dataIndex: 'Hostname',
			width: 100,
			render: (text: string) => <a>{text}</a>
		},
		{
			title: t('node.deployProgress'),
			dataIndex: 'ExecProgress',
			width: 350,
			render: text => {
				return <Progress percent={parseFloat(parseFloat(text).toFixed(2))} strokeColor={twoColors} />;
			}
		},
		{
			title: t('node.detail'),
			dataIndex: 'ExecProgressStepList',
			render: (text: ExecProgressStepVo[]) => {
				const runningStep = text.find(step => step.StepExecState === 'RUNNING');
				const errorStep = text.reverse().find(step => step.StepExecState === 'ERROR');
				const okStep = text.reverse().find(step => step.StepExecState === 'OK');
				return runningStep ? (
					<Text className="text-blue-500">{runningStep?.StepName}</Text>
				) : errorStep ? (
					<Text className="text-red-500">{errorStep?.StepName}</Text>
				) : (
					<Text className="text-green-500">{okStep?.StepName}</Text>
				);
			}
		},
		{
			title: t('node.log'),
			dataIndex: 'NodeState',
			key: 'NodeState',
			render: (_text, record) => <a onClick={() => viewLog(record.NodeId)}> {t('node.viewLog')}</a>
		}
	];

	useImperativeHandle(ref, () => ({
		deploy
	}));
	const handleModalOk = () => {
		setIsModalOpen(false);
	};
	const deploy = async () => {
		setDeployState(false);
		setIsModalOpen(true);
		const api = APIConfig.deploy;
		const serviceNameList = webState[preStepName];
		const params = {
			ActionTypeEnum: operation,
			ClusterId: id,
			IsOneByOne: false,
			ServiceNameList: serviceNameList
		};
		try {
			const data = await RequestHttp.post(api, params);
			setJobId(data.Data.JobId);
			setDeployState(data.Code === '00000');
		} catch (error) {
			console.error('请求失败:', error);
		} finally {
			setIsModalOpen(false); // 在请求完成后关闭模态框，无论成功还是失败
		}
	};
	const viewLog = (nodeId: string) => {
		setIsLogModalOpen(true);
		setActiveNodeId(nodeId);
	};
	const handleLogModalCancel = () => {
		setIsLogModalOpen(false);
	};

	const getList = async () => {
		const api = APIConfig.jobProgress;
		const progressData = await RequestHttp.get(api, { params: { JobId: jobId } });
		const {
			Data: {
				JobExecProgress: { ExecProgressPerNodeList, JobExecStateEnum }
			}
		} = progressData;
		const updatedArray = ExecProgressPerNodeList.map((obj: ExecProgressPerNodeVo) => ({
			...obj,
			JobExecStateEnum
		}));
		const basicDisabled = disabledState.includes(JobExecStateEnum);
		setOpenAlert(JobExecStateEnum === 'ERROR');
		const disableNext = basicDisabled;
		const disableRetry = basicDisabled || JobExecStateEnum === 'OK';
		const disablePrev = disableNext || disableRetry;
		const disableCancel = disableNext || disableRetry;
		setCurrentPageDisabled({
			nextDisabled: disableNext,
			retryDisabled: disableRetry,
			prevDisabled: disablePrev,
			cancelDisabled: disableCancel
		});
		return updatedArray; // 将JobExecStateEnum并入每一条数据，作为轮询终止的条件
	};
	useEffect(() => {
		//基于上一步的数据重新执行当前页面步骤
		if (webState[preStepName]) {
			// 判断是否是刷新后的新加载
			if (isRefresh) {
				setDeployState(true);
			} else {
				// deploy();
			}
		}
	}, [webState, isRefresh]);
	const tableData = usePolling(getList, stableState, 1000, [deployState, webState]);
	return (
		<>
			{openAlert ? <Alert message={errorText} type="error" /> : null}
			<Table
				className="data-light-table" //使用自定义class重定义table行高
				rowKey="NodeId"
				columns={columns}
				dataSource={tableData}
			/>
			{isModalOpen ? <JobPlanModal isModalOpen={isModalOpen} handleOk={handleModalOk} /> : null}
			{isLogModalOpen ? (
				<LogModal isModalOpen={isLogModalOpen} nodeId={activeNodeId} handleCancel={handleLogModalCancel} type="jobProgress" />
			) : null}
		</>
	);
});
export default DeployStep;
