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
 * SelectServiceStep - 选择服务步骤
 * @author Tracy.Guo
 */
import { FC, forwardRef, useImperativeHandle, useEffect, useState, Key } from 'react';
import _ from 'lodash';
import { useSearchParams } from 'react-router-dom';
import { Table, Badge } from 'antd';
import { useTranslation } from 'react-i18next';
import type { ColumnsType } from 'antd/es/table';
import useStore from '@/store/store';
import APIConfig from '@/api/config';
import RequestHttp from '@/api';
import { BadgeStatus, ServiceItemType } from '@/api/interface';

const SelectServiceStep: FC = forwardRef((_props, ref) => {
	const { t } = useTranslation();
	const [searchParams] = useSearchParams();
	const id = searchParams.get('id');
	const { selectedServiceRowsList, setSelectedServiceRowsList, stateText, setCurrentPageDisabled, currentPageDisabled } =
		useStore();
	const [tableData, setTableData] = useState<ServiceItemType[]>([]);
	const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([]);
	const columns: ColumnsType<ServiceItemType> = [
		{
			title: t('service.serviceName'),
			dataIndex: 'ServiceName',
			render: (text: string) => <a>{text}</a>
		},
		{
			title: t('description'),
			dataIndex: 'Desc'
		},
		{
			title: t('node.state'),
			dataIndex: 'SCStateEnum',
			render: (text: string) => <Badge status={stateText[text].status as BadgeStatus} text={t(stateText[text].label)} />
		}
	];
	useImperativeHandle(ref, () => ({
		handleOk
	}));
	const handleOk = async () => {
		const apiSelect = APIConfig.selectService;
		// 合并原数据和本次操作选择的数据
		console.log('selectedServiceRowsList', selectedServiceRowsList);
		console.log('tableData', tableData);
		const combinedArray = selectedServiceRowsList.concat(
			tableData
				.filter(itemA => !selectedServiceRowsList.some(itemB => itemA.ServiceName === itemB.ServiceName))
				.map(item => ({ ...item, SCStateEnum: 'UNSELECTED' }))
		);
		console.log('combinedArray', combinedArray);
		const params = {
			ClusterId: id,
			ServiceList: combinedArray.map(({ SCStateEnum, ServiceName }) => ({ SCStateEnum, ServiceName }))
		};
		const jobData = await RequestHttp.post(apiSelect, params);
		return Promise.resolve(jobData);
	};

	const getList = async () => {
		const apiList = APIConfig.serviceList;
		const params = {
			ClusterId: id
		};
		const data = await RequestHttp.get(apiList, { params });
		const serviceData: ServiceItemType[] = data.Data.ServiceSummaryList;
		setTableData(serviceData);
		const defaultSelectedKeys = serviceData.filter(item => item.SCStateEnum === 'SELECTED').map(item => item.ServiceName);
		setSelectedRowKeys(defaultSelectedKeys);
		setSelectedServiceRowsList(serviceData.filter(item => item.SCStateEnum === 'SELECTED'));
	};
	useEffect(() => {
		getList();
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, []);
	useEffect(() => {
		// 该页面没有异步轮询操作，可以取消流程，cancelDisabled直接置为false
		setCurrentPageDisabled({ ...currentPageDisabled, nextDisabled: !selectedRowKeys.length, cancelDisabled: false });
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [selectedRowKeys]);
	const rowSelection = {
		selectedRowKeys,
		onChange: (selectedRowKeys: Key[], selectedRows: ServiceItemType[]) => {
			setSelectedRowKeys(selectedRowKeys);
			setSelectedServiceRowsList(
				_.cloneDeep(selectedRows).map(item => {
					item.SCStateEnum = 'SELECTED';
					return item;
				})
			);
		}
	};
	return (
		<Table
			rowSelection={{
				...rowSelection
			}}
			rowKey="ServiceName"
			columns={columns}
			dataSource={tableData}
		/>
	);
});
export default SelectServiceStep;