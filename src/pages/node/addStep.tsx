import { useState, forwardRef, useImperativeHandle } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Table } from 'antd';
import { useTranslation } from 'react-i18next';
import type { ColumnsType } from 'antd/es/table';
import useStore from '@/store/store';
import APIConfig from '@/api/config';
import RequestHttp from '@/api';

interface DataType {
	NodeId: React.Key;
	Hostname: string;
	CpuCores: number;
	CpuArch: string;
	DiskTotal: string;
	NodeState: string;
}

const AddStep: React.FC = forwardRef((props, ref) => {
	const { selectedRows, dispatchedList, setCheckedList } = useStore();
	const [tableData] = useState(dispatchedList);
	const { t } = useTranslation();
	const [searchParams] = useSearchParams();
	const id = searchParams.get('id');
	const columns: ColumnsType<DataType> = [
		{
			title: t('node.node'),
			dataIndex: 'Hostname',
			render: (text: string) => <a>{text}</a>
		},
		{
			title: t('node.speed'),
			dataIndex: 'CpuCores',
			render: (text: string, record) => (
				<a>
					{text}
					{t('node.core')}
					{record.CpuArch}
					{t('node.gb')}
					{record.DiskTotal}
					{t('node.gb')}
				</a>
			)
		},
		{
			title: t('node.detail'),
			dataIndex: 'NodeState',
			render: () => <a> {t('node.detecting')}</a>
		}
	];

	useImperativeHandle(ref, () => ({
		handleOk
	}));
	const handleOk = async () => {
		const apiCheck = APIConfig.check;
		const params = {
			ClusterId: id,
			NodeActionTypeEnum: 'CHECk',
			NodeInfoList: selectedRows.map(({ Hostname, NodeId }) => ({ Hostname, NodeId })),
			SshPort: 22
		};
		const jobData = await RequestHttp.post(apiCheck, params);
		setCheckedList(jobData);
		return jobData;
	};

	return <Table rowKey="NodeId" columns={columns} dataSource={tableData} />;
});
export default AddStep;
