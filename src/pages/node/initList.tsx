import React from 'react';
import { Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';

// import { useTranslation } from 'react-i18next';
// import RequestHttp from '@/api';
// import APIConfig from '@/api/config';

interface DataType {
	key: React.Key;
	name: string;
	age: number;
	address: string;
}

const columns: ColumnsType<DataType> = [
	{
		title: 'Name',
		dataIndex: 'name',
		render: (text: string) => <a>{text}</a>
	},
	{
		title: 'Age',
		dataIndex: 'age'
	},
	{
		title: 'Address',
		dataIndex: 'address'
	}
];

const data: DataType[] = [
	{
		key: '1',
		name: 'John Brown',
		age: 32,
		address: 'New York No. 1 Lake Park'
	},
	{
		key: '2',
		name: 'Jim Green',
		age: 42,
		address: 'London No. 1 Lake Park'
	},
	{
		key: '3',
		name: 'Joe Black',
		age: 32,
		address: 'Sydney No. 1 Lake Park'
	},
	{
		key: '4',
		name: 'Disabled User',
		age: 99,
		address: 'Sydney No. 1 Lake Park'
	}
];

// rowSelection object indicates the need for row selection
const rowSelection = {
	onChange: (selectedRowKeys: React.Key[], selectedRows: DataType[]) => {
		console.log(`selectedRowKeys: ${selectedRowKeys}`, 'selectedRows: ', selectedRows);
	},
	getCheckboxProps: (record: DataType) => ({
		disabled: record.name === 'Disabled User', // Column configuration not to be checked
		name: record.name
	})
};
const InitNodeList: React.FC = () => {
	// const { t } = useTranslation();
	// const handleOk = () => {};

	return (
		<Table
			rowSelection={{
				...rowSelection
			}}
			columns={columns}
			dataSource={data}
		/>
	);
};

export default InitNodeList;
