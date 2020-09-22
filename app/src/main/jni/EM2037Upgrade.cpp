#include <stdio.h>
#include <jni.h>
#include <malloc.h>
#include <string.h>
#include <strings.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/inotify.h>
#include <fcntl.h>
#include <stdint.h>
#include <android/log.h>
#include <sys/stat.h>


#include <string>
#include <sstream>
#include <vector>
#include <termios.h>
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



#define LOG_TAG "System.out.c"
#define LOGI(...) //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) //__android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
//===================以下为头文件所定义

extern "C" {


JNIEXPORT jint JNICALL Java_com_newland_uhf_silionBle_upgrade_Native_ImportBinFile
  (JNIEnv *, jobject, jstring);

}

//===================以上为头文件所定义


// bin2 固件结构：  bin2文件最末尾是一个 TuIMGPack_All 结构体
typedef struct  tagPackageBlockInfo{
	int blockAddress; // 文件起始位置
	int Lens;         // 文件长度
	char Block[32];   // 块类型    (字符串： boot kern flah appl）
	int ObjAddress;   // 块目标地址 (仅当 Block为字符串"flah"时有效 )
	int reserve[8];
}TPackageBlockInfo;

typedef struct taguIMGPack_All{
	char PackFlag[8];	// 包标志"uIMG"
	unsigned int  CRC;  // CRC校验值
	int totallens;		// 文件包大小
	int Offset;			// 信息包大小
	char HardName[32];	// 硬件名称
	char Version[24];	// 软件主版本
	char Date[16];		// 出厂日期
	char SpecInfo[24];	// 特殊用户标志
	char Product[100]; //  产品序列
	int  Mirror;      // 是否镜像 
	TPackageBlockInfo block[4];         // 固件数据
	int reserve[16];
}TuIMGPack_All;

static const unsigned int crc32table[256] =  
{  
    0x00000000, 0x77073096, 0xEE0E612C, 0x990951BA,   /* 0x00 */  
    0x076DC419, 0x706AF48F, 0xE963A535, 0x9E6495A3,   /* 0x04 */  
    0x0EDB8832, 0x79DCB8A4, 0xE0D5E91E, 0x97D2D988,   /* 0x08 */  
    0x09B64C2B, 0x7EB17CBD, 0xE7B82D07, 0x90BF1D91,   /* 0x0C */  
    0x1DB71064, 0x6AB020F2, 0xF3B97148, 0x84BE41DE,   /* 0x10 */  
    0x1ADAD47D, 0x6DDDE4EB, 0xF4D4B551, 0x83D385C7,   /* 0x14 */  
    0x136C9856, 0x646BA8C0, 0xFD62F97A, 0x8A65C9EC,   /* 0x18 */  
    0x14015C4F, 0x63066CD9, 0xFA0F3D63, 0x8D080DF5,   /* 0x1C */  
    0x3B6E20C8, 0x4C69105E, 0xD56041E4, 0xA2677172,   /* 0x20 */  
    0x3C03E4D1, 0x4B04D447, 0xD20D85FD, 0xA50AB56B,   /* 0x24 */  
    0x35B5A8FA, 0x42B2986C, 0xDBBBC9D6, 0xACBCF940,   /* 0x28 */  
    0x32D86CE3, 0x45DF5C75, 0xDCD60DCF, 0xABD13D59,   /* 0x2C */  
    0x26D930AC, 0x51DE003A, 0xC8D75180, 0xBFD06116,   /* 0x30 */  
    0x21B4F4B5, 0x56B3C423, 0xCFBA9599, 0xB8BDA50F,   /* 0x34 */  
    0x2802B89E, 0x5F058808, 0xC60CD9B2, 0xB10BE924,   /* 0x38 */  
    0x2F6F7C87, 0x58684C11, 0xC1611DAB, 0xB6662D3D,   /* 0x3C */  
    0x76DC4190, 0x01DB7106, 0x98D220BC, 0xEFD5102A,   /* 0x40 */  
    0x71B18589, 0x06B6B51F, 0x9FBFE4A5, 0xE8B8D433,   /* 0x44 */  
    0x7807C9A2, 0x0F00F934, 0x9609A88E, 0xE10E9818,   /* 0x48 */  
    0x7F6A0DBB, 0x086D3D2D, 0x91646C97, 0xE6635C01,   /* 0x4C */  
    0x6B6B51F4, 0x1C6C6162, 0x856530D8, 0xF262004E,   /* 0x50 */  
    0x6C0695ED, 0x1B01A57B, 0x8208F4C1, 0xF50FC457,   /* 0x54 */  
    0x65B0D9C6, 0x12B7E950, 0x8BBEB8EA, 0xFCB9887C,   /* 0x58 */  
    0x62DD1DDF, 0x15DA2D49, 0x8CD37CF3, 0xFBD44C65,   /* 0x5C */  
    0x4DB26158, 0x3AB551CE, 0xA3BC0074, 0xD4BB30E2,   /* 0x60 */  
    0x4ADFA541, 0x3DD895D7, 0xA4D1C46D, 0xD3D6F4FB,   /* 0x64 */  
    0x4369E96A, 0x346ED9FC, 0xAD678846, 0xDA60B8D0,   /* 0x68 */  
    0x44042D73, 0x33031DE5, 0xAA0A4C5F, 0xDD0D7CC9,   /* 0x6C */  
    0x5005713C, 0x270241AA, 0xBE0B1010, 0xC90C2086,   /* 0x70 */  
    0x5768B525, 0x206F85B3, 0xB966D409, 0xCE61E49F,   /* 0x74 */  
    0x5EDEF90E, 0x29D9C998, 0xB0D09822, 0xC7D7A8B4,   /* 0x78 */  
    0x59B33D17, 0x2EB40D81, 0xB7BD5C3B, 0xC0BA6CAD,   /* 0x7C */  
    0xEDB88320, 0x9ABFB3B6, 0x03B6E20C, 0x74B1D29A,   /* 0x80 */  
    0xEAD54739, 0x9DD277AF, 0x04DB2615, 0x73DC1683,   /* 0x84 */  
    0xE3630B12, 0x94643B84, 0x0D6D6A3E, 0x7A6A5AA8,   /* 0x88 */  
    0xE40ECF0B, 0x9309FF9D, 0x0A00AE27, 0x7D079EB1,   /* 0x8C */  
    0xF00F9344, 0x8708A3D2, 0x1E01F268, 0x6906C2FE,   /* 0x90 */  
    0xF762575D, 0x806567CB, 0x196C3671, 0x6E6B06E7,   /* 0x94 */  
    0xFED41B76, 0x89D32BE0, 0x10DA7A5A, 0x67DD4ACC,   /* 0x98 */  
    0xF9B9DF6F, 0x8EBEEFF9, 0x17B7BE43, 0x60B08ED5,   /* 0x9C */  
    0xD6D6A3E8, 0xA1D1937E, 0x38D8C2C4, 0x4FDFF252,   /* 0xA0 */  
    0xD1BB67F1, 0xA6BC5767, 0x3FB506DD, 0x48B2364B,   /* 0xA4 */  
    0xD80D2BDA, 0xAF0A1B4C, 0x36034AF6, 0x41047A60,   /* 0xA8 */  
    0xDF60EFC3, 0xA867DF55, 0x316E8EEF, 0x4669BE79,   /* 0xAC */  
    0xCB61B38C, 0xBC66831A, 0x256FD2A0, 0x5268E236,   /* 0xB0 */  
    0xCC0C7795, 0xBB0B4703, 0x220216B9, 0x5505262F,   /* 0xB4 */  
    0xC5BA3BBE, 0xB2BD0B28, 0x2BB45A92, 0x5CB36A04,   /* 0xB8 */  
    0xC2D7FFA7, 0xB5D0CF31, 0x2CD99E8B, 0x5BDEAE1D,   /* 0xBC */  
    0x9B64C2B0, 0xEC63F226, 0x756AA39C, 0x026D930A,   /* 0xC0 */  
    0x9C0906A9, 0xEB0E363F, 0x72076785, 0x05005713,   /* 0xC4 */  
    0x95BF4A82, 0xE2B87A14, 0x7BB12BAE, 0x0CB61B38,   /* 0xC8 */  
    0x92D28E9B, 0xE5D5BE0D, 0x7CDCEFB7, 0x0BDBDF21,   /* 0xCC */  
    0x86D3D2D4, 0xF1D4E242, 0x68DDB3F8, 0x1FDA836E,   /* 0xD0 */  
    0x81BE16CD, 0xF6B9265B, 0x6FB077E1, 0x18B74777,   /* 0xD4 */  
    0x88085AE6, 0xFF0F6A70, 0x66063BCA, 0x11010B5C,   /* 0xD8 */  
    0x8F659EFF, 0xF862AE69, 0x616BFFD3, 0x166CCF45,   /* 0xDC */  
    0xA00AE278, 0xD70DD2EE, 0x4E048354, 0x3903B3C2,   /* 0xE0 */  
    0xA7672661, 0xD06016F7, 0x4969474D, 0x3E6E77DB,   /* 0xE4 */  
    0xAED16A4A, 0xD9D65ADC, 0x40DF0B66, 0x37D83BF0,   /* 0xE8 */  
    0xA9BCAE53, 0xDEBB9EC5, 0x47B2CF7F, 0x30B5FFE9,   /* 0xEC */  
    0xBDBDF21C, 0xCABAC28A, 0x53B39330, 0x24B4A3A6,   /* 0xF0 */  
    0xBAD03605, 0xCDD70693, 0x54DE5729, 0x23D967BF,   /* 0xF4 */  
    0xB3667A2E, 0xC4614AB8, 0x5D681B02, 0x2A6F2B94,   /* 0xF8 */  
    0xB40BBE37, 0xC30C8EA1, 0x5A05DF1B, 0x2D02EF8D,   /* 0xFC */  
};  

/*************************************************
 Function:		LRC_Calc
 Descroption:
 Input:
	1.char *pData
	2.nLen
	3.char byLRC
 Output:
 Return:
 Other:
*************************************************/
static unsigned char LRC_Calc(unsigned char *pData, int nLen, unsigned char byLRC)
{
	while(nLen > 0)
	{
		byLRC ^= *pData;
		pData++;
		nLen --;
	}
	return byLRC;
}

/*************************************************
 Function:		CalculateCrc32
 Descroption:	 
 Input: 
	1.char *address
	2.int size
	3.char *unCRC
 Output: 
 Return: 	
 Other:  
*************************************************/
static void CalculateCrc32(unsigned char *address,unsigned int size, unsigned char *unCRC, unsigned int *pnCRC)  
{  
    unsigned int dwTempCrc = 0xFFFFFFFF;  
      
    while(size--)  
    {  
        dwTempCrc = (dwTempCrc >> 8)^crc32table[(dwTempCrc^(*address)) & 0xFF];  
        address++;  
    }  
  
    dwTempCrc=dwTempCrc^ 0xFFFFFFFF;  
    unCRC[0]=(dwTempCrc&0xff000000)>>24;  
    unCRC[1]=(dwTempCrc&0x00ff0000)>>16;  
    unCRC[2]=(dwTempCrc&0x0000ff00)>>8;  
    unCRC[3]=dwTempCrc&0x000000ff;  
   *pnCRC = dwTempCrc;
    return ;  
}  

/*************************************************
 Function:		EM2037_Upgrade_PackCmd
 Descroption:	 
 Input: 
	1.in
	2.inlen
	3.out
 Output: 
 Return: 	
 Other:  
*************************************************/
int EM2037_Upgrade_PackCmd(char* in, int inlen, char* out)
{
    int outlen = 0;
    unsigned char crc[4];
    unsigned int nCRC;
    if(NULL == in || NULL == out || inlen < 1)
    {
        return 0;
    }
    
    out[0] = 0x02;
    out[1] = 0x05;
    out[2] = (inlen & 0xFF00) >> 8;
    out[3] = inlen & 0xFF;
    outlen = 4;
    memcpy(out+4, in, inlen);
    outlen += inlen;
    CalculateCrc32((unsigned char*)out, outlen, crc, &nCRC);
    out[outlen++] = crc[0];
    out[outlen++] = crc[1];
    out[outlen++] = crc[2];
    out[outlen++] = crc[3];
    return outlen;
}

/*************************************************
 Function:		EM2037_Upgrade_PackData
 Descroption:	 
 Input: 
	1.in
	2.inlen
	3.out
 Output: 
 Return: 	
 Other:  
*************************************************/
int EM2037_Upgrade_PackData(char* in, int inlen, char* out)
{
    int outlen = 0;
    unsigned char crc[4];
    unsigned int nCRC;
    if(NULL == in || NULL == out || inlen < 1)
    {
        return 0;
    }
    
    out[0] = 0x02;
    memcpy(out+1, in, inlen);
    outlen = inlen+1;
    CalculateCrc32((unsigned char*)out, outlen, crc, &nCRC);
    out[outlen++] = crc[0];
    out[outlen++] = crc[1];
    out[outlen++] = crc[2];
    out[outlen++] = crc[3];
    return outlen;
}


/*************************************************
 Function:		get_file_size
 Descroption:	
 Input: 
	1.*path
 Output: 
 Return: 	
 Other:  
*************************************************/
unsigned long get_file_size(char *path)
{
    unsigned long filesize = 0;
    struct stat statbuff;

    if(stat(path, &statbuff) < 0)
    {
        return filesize;
    }
    else
    {
        filesize = statbuff.st_size;
    }

    return filesize;     
}

/*************************************************
 Function:		ReadBinFile
 Descroption:	 
 Input: 
	1.filename
	2.char** buf
	3.len
 Output: 
 Return: 	
 Other:  
*************************************************/
int ReadBinFile(const char* filename, unsigned char** buf, int* len)
{
	*len = get_file_size(( char*)filename);

	if (*len == 0)
	{
		return -1;
	}

	FILE *fp = fopen(filename, "rb");
	if (fp == 0)
	{
		*len = 0;
		return -2;
	}
	*buf = (unsigned char*) malloc((*len)+1);
	fread(*buf, 1, *len, fp);
	fclose(fp);
    
    return 0;
}
unsigned char* m_lvfilebuf = NULL;
int m_nFilelen = 0;
int m_nBootPackIdx = -1;
int m_nKernPackIdx = -1;
int m_nFlashPackIdx = -1;
int m_nApplPackIdx = -1;
int m_nImportStatus = -1;
unsigned int c_FrameSize = 230*8;
const int UPGRADE_TYPE_NONE = 0;
const int UPGRADE_TYPE_BOOT = 1;
const int UPGRADE_TYPE_KERNEL = 2;
const int UPGRADE_TYPE_FLASH = 4;
const int UPGRADE_TYPE_APPL = 8;




extern "C" {

JNIEXPORT void JNICALL Java_com_nlscan_uhf_silionBle_upgrade_Native_SetFrameSize
		(JNIEnv *env, jobject obj, jint jSize) {
	c_FrameSize = jSize;
}

JNIEXPORT jint JNICALL Java_com_nlscan_uhf_silionBle_upgrade_Native_ImportBinFile
		(JNIEnv *env, jobject obj, jstring jFileName) {
	if (m_lvfilebuf) {
		free(m_lvfilebuf);
		m_lvfilebuf = NULL;
	}
	m_nBootPackIdx = -1;
	m_nKernPackIdx = -1;
	m_nFlashPackIdx = -1;
	m_nApplPackIdx = -1;
	m_nImportStatus = -1;
	const char *strFileName = env->GetStringUTFChars( jFileName, (jboolean* )false);
	do {
		if (strFileName == NULL) {
			m_nImportStatus = -1;/* OutOfMemoryError already thrown */
			break;
		}
		if (0 != ReadBinFile(strFileName, &m_lvfilebuf, &m_nFilelen)) {
			m_nImportStatus = -2;
			break;
		}
		if (m_nFilelen < sizeof(TuIMGPack_All)) {
			m_nImportStatus = -3;
			break;
		}
		TuIMGPack_All Pack_All_Info;
		memcpy(&Pack_All_Info, &m_lvfilebuf[m_nFilelen - sizeof(TuIMGPack_All)],
			   sizeof(TuIMGPack_All));
		TuIMGPack_All *pPack_All_Info = (TuIMGPack_All *) &m_lvfilebuf[m_nFilelen -
																	   sizeof(TuIMGPack_All)];
		pPack_All_Info->CRC = 0;

		if (0 != memcmp(Pack_All_Info.PackFlag, "uIMG", 4)) {
			m_nImportStatus = -4;
			break;
		}

		unsigned char crc[4];
		unsigned int nCrc = 0;
		CalculateCrc32(m_lvfilebuf, m_nFilelen, crc, &nCrc);
		if (nCrc != Pack_All_Info.CRC) {
			m_nImportStatus = -5;
			break;
		}

		int nHadDataBlocks = 0;
		int iLoop = 0;
		for (iLoop = 0; iLoop < 4; iLoop++) {
			if (0 == memcmp(Pack_All_Info.block[iLoop].Block, "boot", 4)) {
				m_nBootPackIdx = iLoop;
				nHadDataBlocks++;
			}
			if (0 == memcmp(Pack_All_Info.block[iLoop].Block, "kern", 4)) {
				m_nKernPackIdx = iLoop;
				nHadDataBlocks++;
			}
			if (0 == memcmp(Pack_All_Info.block[iLoop].Block, "flah", 4)) {
				m_nFlashPackIdx = iLoop;
				nHadDataBlocks++;
			}
			if (0 == memcmp(Pack_All_Info.block[iLoop].Block, "appl", 4)) {
				m_nApplPackIdx = iLoop;
				nHadDataBlocks++;
			}
		}
		if (0 == nHadDataBlocks) {
			m_nImportStatus = -6;
			break;
		}
		m_nImportStatus = 0;
	} while (false);

	if (0 != m_nImportStatus) {
		if (m_lvfilebuf) {
			free(m_lvfilebuf);
			m_lvfilebuf = NULL;
		}
	}
	if (strFileName) {
		env->ReleaseStringUTFChars( jFileName, strFileName);
	}
	return m_nImportStatus;
}

JNIEXPORT jstring JNICALL Java_com_nlscan_uhf_silionBle_upgrade_Native_GetDevNameFromImportFile
		(JNIEnv *env, jobject obj) {
	if ((NULL == m_lvfilebuf) || (0 != m_nImportStatus)) {
		return NULL;
	}
	TuIMGPack_All *pPack_All_Info = (TuIMGPack_All *) &m_lvfilebuf[m_nFilelen -
																   sizeof(TuIMGPack_All)];
	return env->NewStringUTF( pPack_All_Info->HardName);
}

JNIEXPORT jstring JNICALL Java_com_nlscan_uhf_silionBle_upgrade_Native_GetVersionFromImportFile
		(JNIEnv *env, jobject obj) {
	if ((NULL == m_lvfilebuf) || (0 != m_nImportStatus)) {
		return NULL;
	}
	TuIMGPack_All *pPack_All_Info = (TuIMGPack_All *) &m_lvfilebuf[m_nFilelen -
																   sizeof(TuIMGPack_All)];
	return env->NewStringUTF( pPack_All_Info->Version);
}

//native int GetParam(Integer p1, Integer p2);
//Integer pImportFileStatus = new Integer(0);
//Integer pFrameSize = new Integer(0);
//Integer pCmdPackCnt = new Integer(0);
//Integer pDataPackCnt = new Integer(0);
//int nRet = GetParam(pImportFileStatus, pFrameSize,pCmdPackCnt,pDataPackCnt);
JNIEXPORT jint JNICALL Java_com_nlscan_uhf_silionBle_upgrade_Native_GetParam
		(JNIEnv *env, jobject obj, jint nUpgradeType, jobject pImportFileStatus, jobject pFrameSize,
		 jobject pCmdPackCnt, jobject pDataPackCnt) {
	if (NULL == m_lvfilebuf) {
		return -1;
	}
	int nTypeIdx = -1;
	if (UPGRADE_TYPE_BOOT == nUpgradeType) {
		nTypeIdx = m_nBootPackIdx;
	} else if (UPGRADE_TYPE_KERNEL == nUpgradeType) {
		nTypeIdx = m_nKernPackIdx;
	} else if (UPGRADE_TYPE_FLASH == nUpgradeType) {
		nTypeIdx = m_nFlashPackIdx;
	} else if (UPGRADE_TYPE_APPL == nUpgradeType) {
		nTypeIdx = m_nApplPackIdx;
	}
	if (nTypeIdx < 0) {
		return -2;
	}

	jclass c;
	jfieldID id;
	c = env->FindClass( "java/lang/Integer");
	if (c == NULL) {
		//LOGD("FindClass failed");
		return -3;
	}

	id = env->GetFieldID( c, "value", "I");
	if (id == NULL) {
		//LOGD("GetFiledID failed");
		return -4;
	}

	env->SetIntField( pImportFileStatus, id, m_nImportStatus);

	if (0 == m_nImportStatus) {
		TuIMGPack_All *pPack_All_Info = (TuIMGPack_All *) &m_lvfilebuf[m_nFilelen -
																	   sizeof(TuIMGPack_All)];
		int Lens = pPack_All_Info->block[nTypeIdx].Lens;
		unsigned int Frame = (Lens + c_FrameSize - 1) / c_FrameSize;

		env->SetIntField( pFrameSize, id, c_FrameSize);
		env->SetIntField( pCmdPackCnt, id, 5);
		env->SetIntField( pDataPackCnt, id, Frame);
	} else {
		env->SetIntField( pFrameSize, id, 0);
		env->SetIntField( pCmdPackCnt, id, 0);
		env->SetIntField( pDataPackCnt, id, 0);
	}
	return 0;
}

JNIEXPORT jbyteArray JNICALL Java_com_nlscan_uhf_silionBle_upgrade_Native_GetPackData
		(JNIEnv *env, jobject obj, jint nUpgradeType, jint nPackIdx) {
	if (NULL == m_lvfilebuf) {
		return NULL;
	}
	int nTypeIdx = -1;
	if (UPGRADE_TYPE_BOOT == nUpgradeType) {
		nTypeIdx = m_nBootPackIdx;
	} else if (UPGRADE_TYPE_KERNEL == nUpgradeType) {
		nTypeIdx = m_nKernPackIdx;
	} else if (UPGRADE_TYPE_FLASH == nUpgradeType) {
		nTypeIdx = m_nFlashPackIdx;
	} else if (UPGRADE_TYPE_APPL == nUpgradeType) {
		nTypeIdx = m_nApplPackIdx;
	}
	if (nTypeIdx < 0) {
		return NULL;
	}
	unsigned char lvHeadBuf[32];
	unsigned char lvPackCmdBuf[64];
	memset(lvHeadBuf, 0, sizeof(lvHeadBuf));

	TuIMGPack_All *pPack_All_Info = (TuIMGPack_All *) &m_lvfilebuf[m_nFilelen -
																   sizeof(TuIMGPack_All)];
	switch (nPackIdx) {
		case 0:
			//固件数据总大小：     "!DataLens:XX"   （大小为124时，发送"!DataLens:124"）
			sprintf(lvHeadBuf,  "!DataLens:%d", pPack_All_Info->block[nTypeIdx].Lens);
			break;
		case 1:
			//每次发送的数据大小：  "!FrameSize:XX"  （一般为512，有的设备最大可设为4096）
			sprintf(lvHeadBuf, "!FrameSize:%d", c_FrameSize);
			break;
		case 2:
			//发送次数：  "!Frames:XX" （等于 (DataLens + nFrameSize - 1) / nFrameSize ）
			sprintf(lvHeadBuf, "!Frames:%d",
					(pPack_All_Info->block[nTypeIdx].Lens + c_FrameSize - 1) / (c_FrameSize));
			break;
		case 3:
			//"!FileType:kern"
			if (UPGRADE_TYPE_BOOT == nUpgradeType) {
				sprintf(lvHeadBuf, "%s","!FileType:boot");
			} else if (UPGRADE_TYPE_KERNEL == nUpgradeType) {
				sprintf(lvHeadBuf, "%s","!FileType:kern");
			} else if (UPGRADE_TYPE_FLASH == nUpgradeType) {
				sprintf(lvHeadBuf, "%s","!FileType:flah:9999");
			} else if (UPGRADE_TYPE_APPL == nUpgradeType) {
				sprintf(lvHeadBuf,"%s", "!FileType:appl");
			}
			break;
		case 4:
			//">Start"
			sprintf(lvHeadBuf, ">Start",1);
			break;
		default:
			return NULL;
	}
	int nLenRet = EM2037_Upgrade_PackCmd(lvHeadBuf, strlen(lvHeadBuf), lvPackCmdBuf);
	if (nLenRet < 1) {
		return NULL;
	}
	jbyteArray array = env->NewByteArray( nLenRet);
	env->SetByteArrayRegion( array, 0, nLenRet, (jbyte *)lvPackCmdBuf);
	return array;
}


JNIEXPORT jbyteArray JNICALL Java_com_nlscan_uhf_silionBle_upgrade_Native_GetPackCmd
		(JNIEnv *env, jobject obj, jint nUpgradeType, jint nPackIdx) {
	if (NULL == m_lvfilebuf) {
		return NULL;
	}
	int nTypeIdx = -1;
	if (UPGRADE_TYPE_BOOT == nUpgradeType) {
		nTypeIdx = m_nBootPackIdx;
	} else if (UPGRADE_TYPE_KERNEL == nUpgradeType) {
		nTypeIdx = m_nKernPackIdx;
	} else if (UPGRADE_TYPE_FLASH == nUpgradeType) {
		nTypeIdx = m_nFlashPackIdx;
	} else if (UPGRADE_TYPE_APPL == nUpgradeType) {
		nTypeIdx = m_nApplPackIdx;
	}
	if (nTypeIdx < 0) {
		return NULL;
	}

	TuIMGPack_All *pPack_All_Info = (TuIMGPack_All *) &m_lvfilebuf[m_nFilelen -
																   sizeof(TuIMGPack_All)];
	unsigned char *pData = m_lvfilebuf + pPack_All_Info->block[nTypeIdx].blockAddress;
	int Lens = pPack_All_Info->block[nTypeIdx].Lens;
	unsigned int Frame = (Lens + c_FrameSize - 1) / c_FrameSize;
	unsigned char lvPackDataBufin[c_FrameSize];
	unsigned char lvPackDataBufout[c_FrameSize + 10];
	memset(lvPackDataBufin, 0, sizeof(lvPackDataBufin));

	if (nPackIdx >= Frame) {
		return NULL;
	}

	if (nPackIdx == (Frame - 1)) {
		memcpy(lvPackDataBufin, pData + c_FrameSize * nPackIdx, Lens - c_FrameSize * nPackIdx);
	} else {
		memcpy(lvPackDataBufin, pData + c_FrameSize * nPackIdx, c_FrameSize);
	}

	int nLenRet = EM2037_Upgrade_PackData((char*)lvPackDataBufin, c_FrameSize, (char*)lvPackDataBufout);
	if (nLenRet < 1) {
		return NULL;
	}
	jbyteArray array = env->NewByteArray(nLenRet);
	env->SetByteArrayRegion( array, 0, nLenRet, (jbyte *)lvPackDataBufout);
	return array;
}


JNIEXPORT jstring JNICALL
Java_com_nlscan_uhf_silionBle_upgrade_Native_getCrcStr(JNIEnv *env, jobject thiz, jbyteArray buf) {

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