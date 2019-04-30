/* //device/java/android/android/app/IAlarmManager.aidl
**
** Copyright 2006, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/
package com.nlscan.uhf.silion;

import com.nlscan.android.uhf.IUHFTagInventoryListener;

interface ISilionUHFService {

	/**
     * 上电
     * @return int (@See UHFReader.READER_STATE)
     */
	int powerOn();
	
	/**
     * 上电状态(上电/未上电)
     * @return true/false
     */
	boolean isPowerOn();
	
	/**
     * 下电
     * @return int (@See UHFReader.READER_STATE)
     */
	int powerOff();
	 
	/**
     * 启动盘点
     * @return int (@See UHFReader.READER_STATE)
     */
	 int startTagInventory();
	 
	 /**
     * 是否正在盘点
     * @return true/false
     */
	 boolean isInInventory();
	 
	 /**
     * 停止盘点
     * @return int (@See UHFReader.READER_STATE)
     */
	 int stopTagInventory();
	 
	 /**
     * 写入标签(各个分区)数据
     * @param bank 标签分区(@See UHFReader.BANK_TYPE)
     * @param address 起始地址
     * @param data 写了的数据(字节数组)
     * @param hexAccesspasswd  如果需要访问密码(4个字节16进制字符串,例(0x) : 0D0A0305,长度必须是2的倍数)，填入访问密码（四个字节的数据），如果不需要访问密码请填NULL
     * @return int (@See UHFReader.READER_STATE)
     */
	 int writeTagData(in int bank,in  int address,in byte[] data,in  String hexAccesspasswd);
	
	 /**
     * 写入标签(EPC分区)数据<p>
     * (不能写 EPC区 被锁定的标签，此函数一般用于初始化标签)
     * @param epcData 写了的数据(字节数组)
     * @param hexAccesspasswd  如果需要访问密码(4个字节16进制字符串,例(0x) : 0D0A0305,长度必须是2的倍数)，填入访问密码（四个字节的数据），如果不需要访问密码请填NULL
     * @return int (@See UHFReader.READER_STATE)
     */
	 int writeTagEpcEx(in byte[] epcData, in String hexAccesspasswd);
	
	/**
	* 获取指定分区的数据
	* @param bank 标签分区(@See UHFReader.BANK_TYPE)
	* @param address 起始地址,以块编地
	* @param blkcnt 要读取的块数
	* @param hexAccesspasswd 如果需要访问密码(4个字节16进制字符串,例(0x) : 0D0A0305,长度必须是2的倍数)，填入访问密码（四个字节的数据），如果不需要访问密码请填NULL
	* @return byte[]
	*/
	 byte[] GetTagData(in int bank,in  int address,in  int blkcnt,in  String hexAccesspasswd);
	 
	 /**
	* 锁定标签的内存区域<p>
	* 标签可以锁定的区域有：销毁密码，访问密码，EPC区(bank1)，TID区(bank2)，USER区(bank3)。<p>
	* 锁定标签必须先把访问密码设置为非0 <p>
	* 锁定的时候必须提供访问密码，函数调用一次可以同时对多个区域进行三种类型的锁操作（解锁定，暂时锁定，永久锁定）<p>
	* @param lockObject 锁定的区域，参数(@See UHFReader.Lock_Obj)
	* @param lockType 锁定类型，参数(@See UHFReader.Lock_Type)
	* @param hexAccesspasswd 如果需要访问密码(4个字节16进制字符串,例(0x) : 0D0A0305,长度必须是2的倍数)，填入访问密码（四个字节的数据），如果不需要访问密码请填NULL
	* @return byte[]
	*/
	 int LockTag(in int lockObjects,in  int lockTypes,in String hexAccesspasswd);
	 
	  /**
     * 销毁标签<p>
     * 标签一旦被销毁就不能再使用了，在销毁一个标签之前必须先将销毁密码设置为非0
     * @param hexAccesspasswd (4个字节16进制字符串,例(0x) : 0D0A0305销毁密码
     * @return int (@See UHFReader.READER_STATE)
     */
	 int KillTag(in String hexAccesspasswd);
	 
	 /**
     * 参数设置
     * @param paramKey 功能标识
    *  @param paramName 属性名
     * @param sValue 值数据,
     * @return int (@See UHFReader.READER_STATE)
     */
	 int setParams(in String paramKey,in String paramName,in String sValue);
	
	/**
     * 获取指定属性参数的值
     * @return 属性值String(int,int[], String等类型)
     */
	String getParam(in String paramKey,in String paramName);
	
	/**
     * 获取所有参数
     * @return 属性值Ｍap<String,Object>
     */
	Map  getAllParams();
	
	/**
     * 添加盘点结果回调接口
     */
	void registerTagInventoryListener(in IUHFTagInventoryListener listener);
	
	/**
     * 移除盘点结果回调接口
     */
	void unRegisterTagInventoryListener(in IUHFTagInventoryListener listener);
	
	/**
     * 获取设备型号
     * @return 设备型号
     */
	String getUHFDeviceModel();
	
	/**
     * UHF设备是否有效安装
     * @return true-是,false-否
     */
	boolean isDeviceAvailable();
	
	/**
     * 还原默认设置
     * @return true-是,false-否
     */
	boolean restoreDefaultSettings();
}

