/*********************************************************
 Copyright (C),2011-2017,Electronic Technology Co.,Ltd.
 File name: 		serial.c
 Author: 			Tangxl
 Version: 			1.0
 Date: 				2014-6-16
 Description: 		
 History: 			
 					
   1.Date:	 		2014-6-16
 	 Author:	 	Tangxl
 	 Modification:  Created file
 	 
*********************************************************/

#include <android/log.h>
#include <jni.h>
#include <string>
#include <sstream>
#include <vector>


#include <unistd.h>
#include <stdio.h>
#include <termios.h>
#include <fcntl.h>
#include <string.h>
#include <time.h>


const static char* DTAG = "CrcTest";

#define  LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,DTAG,__VA_ARGS__)

#define MSG_CRC_INIT		    0xFFFF
#define MSG_CCITT_CRC_POLY		0x1021

typedef unsigned char uint8;
typedef unsigned short uint16;

void CRC_calcCrc8(uint16 *crcReg, uint16 poly, uint16 u8Data)
{
	uint16 i;
	uint16 xorFlag;
	uint16 bit;
	uint16 dcdBitMask = 0x80;
	for (i = 0; i < 8; i++)
	{
		xorFlag = *crcReg & 0x8000;
		*crcReg <<= 1;
		bit = ((u8Data & dcdBitMask) == dcdBitMask);
		*crcReg |= bit;
		if (xorFlag)
		{
			*crcReg = *crcReg ^ poly;
		}
		dcdBitMask >>= 1;
	}
}


uint16 CalcCRC(uint8 *msgbuf, uint8 msglen)
{
	uint16 calcCrc = MSG_CRC_INIT;
	uint8  i;
	for (i = 1; i < msglen; ++i)
		CRC_calcCrc8(&calcCrc, MSG_CCITT_CRC_POLY, msgbuf[i]);
	return calcCrc;
}




/**
 * #purpose	: 字符转十六进制
 * #note	: 不适用于汉字字符
 * #param ch    : 要转换成十六进制的字符
 * #return	: 接收转换后的字符串
 */
std::string chToHex(unsigned char ch)
{
	const std::string hex = "0123456789ABCDEF";

	std::stringstream ss;
	ss << hex[ch >> 4] << hex[ch & 0xf];

	return ss.str();
}

/**
 * #purpose	: 字符串转十六进制字符串
 * #note	: 可用于汉字字符串
 * #param str		: 要转换成十六进制的字符串
 * #param separator	: 十六进制字符串间的分隔符
 * #return	: 接收转换后的字符串
 */
std::string strToHex(std::string str, std::string separator = "")
{
	const std::string hex = "0123456789ABCDEF";
	std::stringstream ss;

	for (std::string::size_type i = 0; i < str.size(); ++i)
		ss << hex[(unsigned char)str[i] >> 4] << hex[(unsigned char)str[i] & 0xf] << separator;

	return ss.str();
}



extern "C" {

// public native boolean Init(AssetManager mgr);
JNIEXPORT jstring JNICALL Java_com_nlscan_uhf_silionBle_CrcModel_getCrcStr(JNIEnv *env,jobject thiz,jbyteArray buf) {

    int len = env->GetArrayLength(buf);
    jbyte *copyBuf = env->GetByteArrayElements(buf, 0);
    uint8 data[len];

    for (int i = 0; i < len; i++) {
        data[i] = (uint8) copyBuf[i];
    }

    uint16 result = CalcCRC(data, (uint8) len);

    char relc[5];
    sprintf(relc, "%04X", result);

    jstring relStr = env->NewStringUTF(relc);

    return relStr;

}

}










