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
 * 仪表盘组件
 * @author Tracy.Guo
 */
import { useEffect, useState } from 'react';
import APIConfig from '@/api/config';
import RequestHttp from '@/api';
// import { Gauge } from '@ant-design/plots';
import ReactECharts from 'echarts-for-react';
import useStore from '@/store/store';

const GaugeComponent = ({ clusterId, query, height = 300 }) => {
	const { jobName, instance } = useStore();
	const [option, setOption] = useState({
		tooltip: {
			formatter: '{a} <br/>{b} : {c}%'
		},
		series: [
			{
				type: 'gauge',
				progress: {
					show: true,
					width: 6,
					itemStyle: {
						color: '#3fc27f',
						opacity: 0.7
					}
				},
				axisLine: {
					lineStyle: {
						width: 8
					}
				},
				axisTick: {
					show: false
				},
				splitLine: {
					length: 5,
					lineStyle: {
						width: 1,
						color: '#999'
					}
				},
				axisLabel: {
					distance: 15,
					color: '#999',
					fontSize: 10
				},
				anchor: {
					show: true,
					showAbove: true,
					size: 14,
					itemStyle: {
						borderWidth: 4,
						borderColor: '#3fc27f'
					}
				},
				title: {
					show: false
				},
				detail: {
					valueAnimation: true,
					fontSize: 24,
					offsetCenter: [0, '70%']
				},
				data: [
					{
						value: 0,
						itemStyle: {
							color: '#3fc27f'
						}
					}
				]
			}
		]
	});
	const getGaugeData = async () => {
		const api = APIConfig.prometheus;
		const params = {
			Body: '',
			ClusterId: clusterId,
			Path: '/api/v1/query',
			QueryParamsMap: {
				query
				// time: '1712128729.98'
			},
			RequestMethod: 'GET'
		};
		const { Data } = await RequestHttp.post(api, params);
		const {
			data: { result }
		} = JSON.parse(Data);
		const gaugeData = parseFloat(result[0].value[1]).toFixed(2);
		const updatedOption = { ...option };
		updatedOption.series[0].data[0].value = gaugeData;
		console.log('updatedOption', updatedOption);
		setOption(updatedOption);
	};
	useEffect(() => {
		getGaugeData();
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [jobName, instance]);

	return <ReactECharts key={option.series[0].data[0].value} option={option} style={{ height }} />;
};
export default GaugeComponent;
