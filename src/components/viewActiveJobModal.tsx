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
 * viewActiveJobModal - 选择组件时绑定的节点弹窗
 * @param {boolean} isModalOpen - 弹窗是否打开
 * @param {function} handleOk - 弹窗确定的回调函数
 * @param {function} handleCancel - 弹窗取消的回调函数
 * @param {string} component - 关联的组件名称
 * @author Tracy.Guo
 */
import { FC, useState, useEffect } from 'react';
import { Modal, Table, Tag, Flex } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useTranslation } from 'react-i18next';
import APIConfig from '@/api/config';
import RequestHttp from '@/api';
import { extractUpperCaseAndNumbers } from '@/utils/helper';
import { NodeType } from '@/api/interface';

interface ViewActiveJobProps {
	isModalOpen: boolean;
	handleOk: (list: NodeType[]) => void;
	handleCancel: () => void;
}

const ViewActiveJobModal: FC<ViewActiveJobProps> = ({ isModalOpen, handleOk, handleCancel, type }) => {
	const { t } = useTranslation();
	const [tableData, setTableData] = useState([]);
	console.log(handleOk, type);
	const columns: ColumnsType<NodeType> = [
		{
			title: t('node.node'),
			dataIndex: 'Hostname',
			key: 'Hostname',
			render: (text: string) => <a>{text}</a>
		},
		{
			title: t('includeComponent'),
			dataIndex: 'ComponentName',
			key: 'ComponentName',
			width: '60%',
			render: (text: string[]) => (
				<Flex wrap="wrap" gap="small">
					{text.map(component => (
						<Tag bordered={false} color="processing">
							{extractUpperCaseAndNumbers(component)}
						</Tag>
					))}
				</Flex>
			)
		},
		{
			title: t('node.state'),
			dataIndex: 'NodeState',
			key: 'NodeState',
			render: (text: string) => <a>{t(text.toLowerCase())}</a>
		}
	];
	const getList = async () => {
		const apiList = APIConfig[type];
		const data = await RequestHttp.get(apiList);
		const {
			Data: { ClusterId, NodeJobId }
		} = data;
		console.log(ClusterId, NodeJobId);
		setTableData(ClusterId);
	};
	useEffect(() => {
		getList();
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, []);

	return (
		<Modal title={t('viewActiveJob')} open={isModalOpen} onCancel={handleCancel}>
			<Table rowKey="NodeId" dataSource={tableData} columns={columns} />
		</Modal>
	);
};
export default ViewActiveJobModal;
