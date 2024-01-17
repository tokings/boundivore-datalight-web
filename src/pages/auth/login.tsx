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
import { Button, Form, Input, Col, Row, Space } from 'antd';
import { md5 } from 'js-md5';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import APIConfig from '@/api/config';
import RequestHttp from '@/api';
import useStore, { usePersistStore } from '@/store/store';
import Logo from '@/assets/logo.png';
import { useEffect } from 'react';

type FieldType = {
	Principal?: string;
	Credential?: string;
};

const LoginPage: React.FC = () => {
	const [form] = Form.useForm();
	const { t } = useTranslation();
	const navigate = useNavigate();
	const { setIsNeedChangePassword } = useStore();
	const { userInfo, setUserInfo } = usePersistStore();
	const onFinish = async (values: any) => {
		const { Credential, Principal } = values;
		const hexHash = md5(Credential);
		const apiLogin = APIConfig.login;
		const params = {
			Credential: hexHash,
			IdentityType: 'USERNAME',
			Principal
		};
		const authData = await RequestHttp.post(apiLogin, params);
		const {
			Code,
			Data: { UserId, Nickname, Realname, IsNeedChangePassword }
		} = authData;
		if (Code === '00000') {
			setUserInfo({ userId: UserId, nickName: Nickname, realName: Realname });
			setIsNeedChangePassword(IsNeedChangePassword);
			navigate('/home');
		}
	};

	const onFinishFailed = (errorInfo: any) => {
		console.log('Failed:', errorInfo);
	};
	const isLogin = async () => {
		const apiIsLogin = APIConfig.isLogin;
		const loginData = await RequestHttp.get(apiIsLogin);
		loginData.Data && navigate('/home');
	};
	useEffect(() => {
		userInfo.userId && isLogin();
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, []);
	return (
		<div className="min-w-[1200px] bg-[url('/loginBg.jpg')] bg-cover bg-center h-screen flex">
			<Row className="w-8/12 height-[150] m-auto p-auto border border-blue-500 shadow-2xl shadow-blue-500">
				{/* <Col span={12} className="bg-[url('/login.png')] bg-cover bg-center"></Col> */}
				<Col span={12} className="pt-20 pb-10 px-10 flex items-center justify-center flex-col">
					<Space direction="vertical" align="center" size="large">
						<img src={Logo} height={60} className="m-auto p-auto" />
						<Form
							className="w-[300px]"
							form={form}
							name="basic"
							layout="vertical"
							// labelCol={{ span: 8 }}
							// wrapperCol={{ span: 16 }}
							// style={{ width: 300, marginTop: '50px' }}
							onFinish={onFinish}
							onFinishFailed={onFinishFailed}
							autoComplete="off"
							requiredMark={false}
						>
							<Form.Item<FieldType>
								label={t('login.principal')}
								name="Principal"
								rules={[{ required: true, message: 'Please input your username!' }]}
							>
								<Input />
							</Form.Item>

							<Form.Item<FieldType>
								label={t('login.credential')}
								name="Credential"
								rules={[{ required: true, message: 'Please input your password!' }]}
							>
								<Input.Password />
							</Form.Item>
							<Form.Item wrapperCol={{ offset: 8, span: 16 }}>
								<Button type="primary" htmlType="submit">
									{t('login.confirm')}
								</Button>
							</Form.Item>
						</Form>
						<span>{t('poweredBy')}</span>
					</Space>
				</Col>
				<Col span={12} className="bg-[url('/login.png')] bg-cover bg-center"></Col>
			</Row>
		</div>
	);
};

export default LoginPage;
