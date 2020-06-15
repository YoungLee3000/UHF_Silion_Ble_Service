package com.nlscan.uhf.silionBle;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.nlscan.blecommservice.IBleInterface;
import com.uhf.api.cls.BackReadOption;
import com.uhf.api.cls.ErrInfo;
import com.uhf.api.cls.GpiInfo_ST;
import com.uhf.api.cls.GpiTriggerBoundaryListener;
import com.uhf.api.cls.GpiTriggerListener;
import com.uhf.api.cls.ReadExceptionListener;
import com.uhf.api.cls.ReadListener;
import com.uhf.api.cls.Reader;

import java.util.ArrayList;
import java.util.List;

public class BleReader extends Reader {


    private Context mContext;
    private static final String TAG = "UHFSilionBleService";

    private CrcModel mCrcModel = new CrcModel();


    private IBleInterface mBleInterface;
    private static final String COMMAND_HEADER = "FF";
    private static final String RESULT_FAIL = "failed";

    //存储标签信息的全局变量
    private List<TAGINFO> mIvnTagList = new ArrayList<>();
    private int mIvnCount = 0;

    public BleReader(IBleInterface iBleInterface) {
        this.mBleInterface = iBleInterface;
    }




    @Override
    public void Hex2Str(byte[] buf, int len, char[] out) {
        super.Hex2Str(buf, len, out);
    }

    @Override
    public READER_ERR InitReader(String src, Reader_Type rtype) {

        return READER_ERR.MT_OK_ERR;

    }

    @Override
    public READER_ERR InitReader_Notype(String src, int rtype) {
        try {
            return mBleInterface.isBleAccess() ?  READER_ERR.MT_OK_ERR : READER_ERR.MT_CMD_FAILED_ERR ;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return READER_ERR.MT_CMD_FAILED_ERR;
    }

    @Override
    public String GetReaderAddress() {
        return super.GetReaderAddress();
    }

    @Override
    public READER_ERR GetHardwareDetails(HardwareDetails val) {
        return super.GetHardwareDetails(val);
    }

    @Override
    public void CloseReader() {
//        mBleInterface = null;
    }

    @Override
    public READER_ERR GetTagData(int ant, char bank, int address, int blkcnt, byte[] data, byte[] accesspasswd, short timeout) {
        return super.GetTagData(ant, bank, address, blkcnt, data, accesspasswd, timeout);
    }

    @Override
    public READER_ERR WriteTagData(int ant, char bank, int address, byte[] data, int datalen, byte[] accesspasswd, short timeout) {
        return super.WriteTagData(ant, bank, address, data, datalen, accesspasswd, timeout);
    }

    @Override
    public READER_ERR WriteTagEpcEx(int ant, byte[] Epc, int epclen, byte[] accesspwd, short timeout) {
        return super.WriteTagEpcEx(ant, Epc, epclen, accesspwd, timeout);
    }

    @Override
    public READER_ERR TagInventory(int[] ants, int antcnt, short timeout, TAGINFO[] pTInfo, int[] tagcnt) {
        return super.TagInventory(ants, antcnt, timeout, pTInfo, tagcnt);
    }

    @Override
    public READER_ERR TagInventory_Raw(int[] ants, int antcnt, short timeout, int[] tagcnt) {
//        return super.TagInventory_Raw(ants, antcnt, timeout, tagcnt);


        mIvnTagList.clear();
        mIvnCount = 0;

        //发送多标签盘存命令
        StringBuilder command = new StringBuilder(COMMAND_HEADER);
        String operateCode = "22";
        String option = "00";
        String searchFlag = "0000";
        String strTimeOut = String.format("%04X",timeout);

        int length = ( option.length() + searchFlag.length() +
                        strTimeOut.length()) / 2;
        String strLength = String.format("%02X",length);

        command.append(strLength);
        command.append(operateCode);
        command.append(searchFlag);
        command.append(strTimeOut);

        String  crcStr =     mCrcModel.getCrcStr(HexUtil.toByteArray(command.toString())) ;
        command.append(crcStr);

        String resultCode = "failed";

        try {
            resultCode = mBleInterface.sendUhfCommand(command.toString());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (resultCode.equals(RESULT_FAIL))
            return READER_ERR.MT_CMD_FAILED_ERR;


        //解析返回结果,得到扫描到的标签数
        int relLen = resultCode.length();
        if (relLen < 22) return READER_ERR.MT_CMD_FAILED_ERR;

        String relStatus =  resultCode.substring(6,10) ;
        if (!relStatus.equals("0000"))
            return READER_ERR.MT_CMD_FAILED_ERR;
        String relSearchFlag =  resultCode.substring(12,16) ;
        if (relSearchFlag.equals("0000")){
            tagcnt[0] = Integer.parseInt(resultCode.substring(16,18),16);
        }
        else{
            tagcnt[0] = Integer.parseInt(resultCode.substring(16,24),16);
        }



        //获取盘点到的标签信息
        return getInvTag();
    }


    /**
     * 重载盘点方法
     * @param ants
     * @param antcnt
     * @param timeout
     * @param tagcnt
     * @param taginfos
     * @return
     */
    public READER_ERR TagInventory_Raw(int[] ants, int antcnt, short timeout, int[] tagcnt ,TAGINFO[] taginfos) {
//        return super.TagInventory_Raw(ants, antcnt, timeout, tagcnt);


        mIvnTagList.clear();
        mIvnCount = 0;

        //发送多标签盘存命令
        StringBuilder command = new StringBuilder(COMMAND_HEADER);
        String operateCode = "22";
        String option = "00";
        String searchFlag = "0000";
        String strTimeOut = String.format("%04X",timeout);

        int length = ( option.length() + searchFlag.length() +
                        strTimeOut.length()) / 2;
        String strLength = String.format("%02X",length);

        command.append(strLength);
        command.append(operateCode);
        command.append(searchFlag);
        command.append(strTimeOut);

        String  crcStr =     mCrcModel.getCrcStr(HexUtil.toByteArray(command.toString())) ;
        command.append(crcStr);

        String resultCode = "failed";

        try {
            resultCode = mBleInterface.sendUhfCommand(command.toString());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (resultCode.equals(RESULT_FAIL))
            return READER_ERR.MT_CMD_FAILED_ERR;


        //解析返回结果,得到扫描到的标签数
        int relLen = resultCode.length();
        if (relLen < 22) return READER_ERR.MT_CMD_FAILED_ERR;

        String relStatus =  resultCode.substring(6,10) ;
        if (!relStatus.equals("0000"))
            return READER_ERR.MT_CMD_FAILED_ERR;
        String relSearchFlag =  resultCode.substring(12,16) ;
        if (relSearchFlag.equals("0000")){
            tagcnt[0] = Integer.parseInt(resultCode.substring(16,18),16);
        }
        else{
            tagcnt[0] = Integer.parseInt(resultCode.substring(16,24),16);
        }


//        tagcnt[0] = 2;
        //获取盘点到的标签信息
        return getInvTag(taginfos);
    }





    /**
     * 获取一轮盘点中的标签信息
     */
    private READER_ERR getInvTag(TAGINFO[] taginfos){


        //发送获取标签命令
        StringBuilder command = new StringBuilder(COMMAND_HEADER);
        String operateCode = "29";
        String metadataFlag = "00BF";
        String readOption = "00";
        int length = ( metadataFlag.length() + readOption.length() ) / 2;
        String strLength = String.format("%02X",length);

        command.append(strLength);
        command.append(operateCode);
        command.append(metadataFlag);
        command.append(readOption);

        String  crcStr = mCrcModel.getCrcStr(HexUtil.toByteArray(command.toString())) ;
        command.append(crcStr);

        String resultCode = "failed";

        try {
//            resultCode = mBleInterface.sendUhfCommand(command.toString());
            resultCode = mBleInterface.getUhfTagData();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (   resultCode.equals(RESULT_FAIL))
            return READER_ERR.MT_CMD_FAILED_ERR;

        //String resultCode = "FF3C29000000BF000207E3110E222A00008D8F0000000000602000201522223333697CC24107C3110E222A00008D8F000000000060200011112222FF0047BBC241DC46";

        //解析获取到的标签
        int relLen = resultCode.length();
        if (relLen < 24) return READER_ERR.MT_CMD_FAILED_ERR;
        String relStatus =  resultCode.substring(6,10) ;
        if (!relStatus.equals("0000"))
            return READER_ERR.MT_CMD_FAILED_ERR;
        String tagTotalInfo = resultCode.substring(18, relLen -4);
        Log.d(TAG,"the total info is " + tagTotalInfo);
        int tagCount = Integer.parseInt(resultCode.substring(16,18),16);

        int tagTotLen = tagTotalInfo.length();
        int beginIndex = 0;

        while (beginIndex < tagTotLen){
            TAGINFO tfs = new TAGINFO();
            tfs.ReadCnt = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex,beginIndex+2));
            tfs.RSSI = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+2,beginIndex+4));
            tfs.AntennaID = (byte)HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+4,beginIndex+6));
            tfs.Frequency = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+6,beginIndex+12));
            tfs.TimeStamp = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+12,beginIndex+20));
            tfs.Res = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+20,beginIndex+24));
            int epcLen = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+28,beginIndex+32)) /8 * 2;
            int tagLen = epcLen - 8;
            tfs.PC = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+32,beginIndex+36));
            tfs.EpcId = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+36,beginIndex+36+tagLen));
            tfs.CRC = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+36+tagLen,beginIndex+36+tagLen+4));
            mIvnTagList.add(tfs);
            beginIndex += 32 + epcLen;
            taginfos[mIvnCount] = tfs;
            mIvnCount++;
        }

        Log.d(TAG,"tag list size is " + mIvnTagList.size());

        return READER_ERR.MT_OK_ERR;


    }




    /**
     * 获取一轮盘点中的标签信息
     */
    private READER_ERR getInvTag(){


//        //发送获取标签命令
//        StringBuilder command = new StringBuilder(COMMAND_HEADER);
//        String operateCode = "29";
//        String metadataFlag = "00BF";
//        String readOption = "00";
//        int length = ( metadataFlag.length() + readOption.length() ) / 2;
//        String strLength = String.format("%02X",length);
//
//        command.append(strLength);
//        command.append(operateCode);
//        command.append(metadataFlag);
//        command.append(readOption);
//
//        String  crcStr = mCrcModel.getCrcStr(HexUtil.toByteArray(command.toString())) ;
//        command.append(crcStr);
//
//        String resultCode = "failed";
//
//        try {
//            resultCode = mBleInterface.sendUhfCommand(command.toString());
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
//
//        if (resultCode.equals(RESULT_FAIL))
//            return READER_ERR.MT_CMD_FAILED_ERR;

        String resultCode = "FF3C29000000BF000207E3110E222A00008D8F0000000000602000201522223333697CC24107C3110E222A00008D8F000000000060200011112222FF0047BBC241DC46";

        //解析获取到的标签
        int relLen = resultCode.length();
        if (relLen < 24) return READER_ERR.MT_CMD_FAILED_ERR;
        String relStatus =  resultCode.substring(6,10) ;
        if (!relStatus.equals("0000"))
            return READER_ERR.MT_CMD_FAILED_ERR;
        String tagTotalInfo = resultCode.substring(18, relLen -4);
        Log.d(TAG,"the total info is " + tagTotalInfo);
        int tagCount = Integer.parseInt(resultCode.substring(16,18),16);

        int tagTotLen = tagTotalInfo.length();
        int beginIndex = 0;

        while (beginIndex < tagTotLen){
            TAGINFO tfs = new TAGINFO();
            tfs.ReadCnt = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex,beginIndex+2));
            tfs.RSSI = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+2,beginIndex+4));
            tfs.AntennaID = (byte)HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+4,beginIndex+6));
            tfs.Frequency = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+6,beginIndex+12));
            tfs.TimeStamp = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+12,beginIndex+20));
            tfs.Res = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+20,beginIndex+24));
            int epcLen = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+28,beginIndex+32)) /8 * 2;
            int tagLen = epcLen - 8;
            tfs.PC = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+32,beginIndex+36));
            tfs.EpcId = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+36,beginIndex+36+tagLen));
            tfs.CRC = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+36+tagLen,beginIndex+36+tagLen+4));
            mIvnTagList.add(tfs);
            beginIndex += 32 + epcLen;
            mIvnCount++;
        }

        Log.d(TAG,"tag list size is " + mIvnTagList.size());

        return READER_ERR.MT_OK_ERR;


    }

    @Override
    public READER_ERR TagInventory_BaseType(int[] ants, int antcnt, short timeout, byte[] outbuf, int[] tagcnt) {
        return super.TagInventory_BaseType(ants, antcnt, timeout, outbuf, tagcnt);
    }




    @Override
    public READER_ERR GetNextTag(TAGINFO TI) {
//        return super.GetNextTag(TI);

        Log.d(TAG,"get next tag " );
        if (mIvnCount < mIvnTagList.size()){
            TI = mIvnTagList.get(mIvnCount);
            Log.d(TAG,"TI is null " + (TI == null));
            mIvnCount++;
            return READER_ERR.MT_OK_ERR;
        }
        else {
            return  READER_ERR.MT_CMD_FAILED_ERR;
        }




    }

    @Override
    public READER_ERR GetNextTag_BaseType(byte[] outbuf) {
        return super.GetNextTag_BaseType(outbuf);
    }

    @Override
    public READER_ERR LockTag(int ant, byte lockobjects, short locktypes, byte[] accesspasswd, short timeout) {
        return super.LockTag(ant, lockobjects, locktypes, accesspasswd, timeout);
    }

    @Override
    public READER_ERR KillTag(int ant, byte[] killpasswd, short timeout) {
        return super.KillTag(ant, killpasswd, timeout);
    }

    @Override
    public READER_ERR Lock180006BTag(int ant, int startblk, int blkcnt, short timeout) {
        return super.Lock180006BTag(ant, startblk, blkcnt, timeout);
    }

    @Override
    public READER_ERR BlockPermaLock(int ant, int readlock, int startblk, int blkrange, byte[] mask, byte[] pwd, short timeout) {
        return super.BlockPermaLock(ant, readlock, startblk, blkrange, mask, pwd, timeout);
    }

    @Override
    public READER_ERR BlockErase(int ant, int bank, int wordaddr, int wordcnt, byte[] pwd, short timeout) {
        return super.BlockErase(ant, bank, wordaddr, wordcnt, pwd, timeout);
    }

    @Override
    public READER_ERR EraseDataOnReader() {
        return super.EraseDataOnReader();
    }

    @Override
    public READER_ERR SaveDataOnReader(int address, byte[] data, int datalen) {
        return super.SaveDataOnReader(address, data, datalen);
    }

    @Override
    public READER_ERR ReadDataOnReader(int address, byte[] data, int datalen) {
        return super.ReadDataOnReader(address, data, datalen);
    }

    @Override
    public READER_ERR CustomCmd(int ant, CustomCmdType cmdtype, Object CustomPara, Object CustomRet) {
        return super.CustomCmd(ant, cmdtype, CustomPara, CustomRet);
    }

    @Override
    public READER_ERR CustomCmd_BaseType(int ant, int cmdtype, byte[] CustomPara, byte[] CustomRet) {
        return super.CustomCmd_BaseType(ant, cmdtype, CustomPara, CustomRet);
    }

    @Override
    public READER_ERR SetGPO(int gpoid, int val) {
        return super.SetGPO(gpoid, val);
    }

    @Override
    public READER_ERR GetGPI(int gpoid, int[] val) {
        return super.GetGPI(gpoid, val);
    }

    @Override
    public READER_ERR GetGPIEx(GpiInfo_ST gpist) {
        return super.GetGPIEx(gpist);
    }

    @Override
    public READER_ERR PsamTransceiver(int soltid, int coslen, byte[] cos, int[] cosresplen, byte[] cosresp, byte[] errcode, short timeout) {
        return super.PsamTransceiver(soltid, coslen, cos, cosresplen, cosresp, errcode, timeout);
    }

    @Override
    public READER_ERR ParamGet(Mtr_Param key, Object val) {
//        return super.ParamGet(key, val);
        return READER_ERR.MT_CMD_FAILED_ERR;
    }

    @Override
    public READER_ERR ParamSet(Mtr_Param key, Object val) {
//        return super.ParamSet(key, val);

        //发送设置参数命令
        StringBuilder command = new StringBuilder(COMMAND_HEADER);
        String operateCode ;

        //----Gen2项-----
        String protocolValue = "";
        String parameter = "";
        String option ;
        String value = "";
        //----Gen2项-----

        //----AntPower项-----
        option = "";
        String antNum = "";
        String readPower = "";
        String writePower = "";
        //----AntPower项-----


        //---频率区---
        String zoneCode = "";
        //---频率区---

        int [] intArray;
        int intVal;


        switch (key.value()){
            case 0://设置session
                operateCode = "9B";
                protocolValue = "05";
                parameter = "00";
                intArray = (int []) val;
                value = String.format("%02X", intArray[0]);
                break;
            case 1://设置Q值
                operateCode = "9B";
                protocolValue = "05";
                parameter = "12";
                intArray = (int []) val;
                if (intArray[0] == -1){
                    option = "00";
                }
                else{
                    option = "01";
                    value = String.format("%02X", intArray[0]);
                }
                break;
            case 19://设置Target

                operateCode = "9B";
                protocolValue = "05";
                parameter = "01";
                intArray = (int []) val;
                switch (intArray[0]){
                    case 0:
                        option = "01";
                        value = "00";
                        break;
                    case 1:
                        option = "01";
                        value = "01";
                        break;
                    case 2:
                        option = "00";
                        value = "00";
                        break;
                    case 3:
                        option = "00";
                        value = "01";
                        break;
                }
                break;

            case 4: //设置天线读写功率
                Reader.AntPowerConf powerConf = (Reader.AntPowerConf ) val;
                operateCode = "91";
                option = "03";
                antNum = "01";
                readPower =  String.format("%02X",powerConf.Powers[0].readPower);
                writePower = String.format("%02X",powerConf.Powers[0].writePower);
                break;

            case 15:
                Reader.Region_Conf region_conf = (Reader.Region_Conf ) val;
                switch (region_conf.value()){
                    case 1://北美
                        zoneCode = "01";
                        break;
                    case 6://中国1
                        zoneCode = "06";
                        break;
                    case 7://中国2
                        zoneCode = "0A";
                        break;
                    case 4: //欧洲3
                        zoneCode = "08";
                        break;
                    case 8: //全频
                        zoneCode = "FF";
                        break;

                }
                operateCode = "97";



                break;
            default:
                return READER_ERR.MT_OK_ERR;

        }

        int length = (operateCode.length() + protocolValue.length() + parameter.length() + option.length() + value.length()
                      + antNum.length() + readPower.length() + writePower.length() + zoneCode.length()  ) / 2;


        String strLength = String.format("%02X",length);
//
        command.append(strLength);
        command.append(operateCode);
        command.append(protocolValue);
        command.append(parameter);
        command.append(option);
        command.append(value);
        command.append(antNum);
        command.append(readPower);
        command.append(writePower);
        command.append(zoneCode);

        String  crcStr = mCrcModel.getCrcStr(HexUtil.toByteArray(command.toString())) ;
        command.append(crcStr);

        String resultCode = "failed";

        try {
            resultCode = mBleInterface.sendUhfCommand(command.toString());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (   resultCode.equals(RESULT_FAIL))
            return READER_ERR.MT_CMD_FAILED_ERR;

        return READER_ERR.MT_OK_ERR;
    }

    @Override
    public void Str2Hex(String buf, int len, byte[] hexbuf) {
        super.Str2Hex(buf, len, hexbuf);
    }

    @Override
    public void Str2Binary(String buf, int len, byte[] binarybuf) {
        super.Str2Binary(buf, len, binarybuf);
    }

    @Override
    public READER_ERR AsyncStartReading(int[] ants, int antcnt, int option) {
        return super.AsyncStartReading(ants, antcnt, option);
    }

    @Override
    public READER_ERR AsyncStopReading() {

        String sendCommand = "FF0EAA4D6F64756C6574656368AA49F3BB0391";


        String resultCode = "failed";

        try {
            resultCode = mBleInterface.sendUhfCommand(sendCommand);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (   resultCode.equals(RESULT_FAIL))
            return READER_ERR.MT_CMD_FAILED_ERR;

        return READER_ERR.MT_OK_ERR;

    }

    @Override
    public READER_ERR AsyncGetTagCount(int[] tagcnt) {
        return super.AsyncGetTagCount(tagcnt);
    }


    /**
     * 重载异步获取标签的方法
     * @param tagcnt
     * @param taginfos
     * @return
     */
    public READER_ERR AsyncGetTagCount(int[] tagcnt, TAGINFO[] taginfos) {


        mIvnTagList.clear();
        mIvnCount = 0;

        //开启快速盘点命令
        String sendCommand = "FF13AA4D6F64756C6574656368AA48001700800375BB4D30";
        String resultCode = "failed";

        try {
            resultCode = mBleInterface.sendUhfCommand(sendCommand);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (resultCode.equals(RESULT_FAIL))
            return READER_ERR.MT_CMD_FAILED_ERR;

        tagcnt[0] = 2;
        //获取盘点到的标签信息
        return getInvTagQuick(taginfos);

    }



    /**
     * 获取一轮快速盘点中的标签信息
     */
    private READER_ERR getInvTagQuick(TAGINFO[] taginfos){

        //发送获取标签命令
        StringBuilder command = new StringBuilder(COMMAND_HEADER);
        String operateCode = "29";
        String metadataFlag = "0015";
        String readOption = "00";
        int length = ( metadataFlag.length() + readOption.length() ) / 2;
        String strLength = String.format("%02X",length);

        command.append(strLength);
        command.append(operateCode);
        command.append(metadataFlag);
        command.append(readOption);

        String  crcStr = mCrcModel.getCrcStr(HexUtil.toByteArray(command.toString())) ;
        command.append(crcStr);

        String resultCode = "failed";

        try {
//            resultCode = mBleInterface.sendUhfCommand(command.toString());
            resultCode = mBleInterface.getUhfTagData();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (   resultCode.equals(RESULT_FAIL))
            return READER_ERR.MT_CMD_FAILED_ERR;


        //解析获取到的标签
        int relLen = resultCode.length();
        if (relLen < 24) return READER_ERR.MT_CMD_FAILED_ERR;
        String relStatus =  resultCode.substring(6,10) ;
        if (!relStatus.equals("0000"))
            return READER_ERR.MT_CMD_FAILED_ERR;
        String tagTotalInfo = resultCode.substring(18, relLen -4);
        Log.d(TAG,"the total info is " + tagTotalInfo);
        int tagCount = Integer.parseInt(resultCode.substring(16,18),16);

        int tagTotLen = tagTotalInfo.length();
        int beginIndex = 0;

        while (beginIndex < tagTotLen){
            TAGINFO tfs = new TAGINFO();
            tfs.ReadCnt = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex,beginIndex+2));
            tfs.RSSI = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+2,beginIndex+4));
            tfs.AntennaID = (byte)HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+4,beginIndex+6));
            tfs.TimeStamp = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+6,beginIndex+14));
            int epcLen = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+14,beginIndex+18)) /8 * 2;
            int tagLen = epcLen - 8;
            tfs.PC = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+18,beginIndex+22));
            tfs.EpcId = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+22,beginIndex+22+tagLen));
            tfs.CRC = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+22+tagLen,beginIndex+22+tagLen+4));
            mIvnTagList.add(tfs);
            beginIndex += 22 + epcLen;
            taginfos[mIvnCount] = tfs;
            mIvnCount++;
        }

        Log.d(TAG,"tag list size is " + mIvnTagList.size());

        return READER_ERR.MT_OK_ERR;


    }





    @Override
    public READER_ERR AsyncGetNextTag(TAGINFO TI) {
        return super.AsyncGetNextTag(TI);
    }

    @Override
    public READER_ERR GetSerialNumber(DeviceSerialNumber devsn) {
        return super.GetSerialNumber(devsn);
    }

    @Override
    public READER_ERR GetLastDetailError(ErrInfo ei) {
        return super.GetLastDetailError(ei);
    }

    @Override
    public READER_ERR ResetRfidModule() {
        return super.ResetRfidModule();
    }

    @Override
    public int DataTransportSend(byte[] data, int datalen, int timeout) {
        return super.DataTransportSend(data, datalen, timeout);
    }

    @Override
    public int DataTransportRecv(byte[] data, int datalen, int timeout) {
        return super.DataTransportRecv(data, datalen, timeout);
    }

    @Override
    public void addReadListener(ReadListener listener) {
        super.addReadListener(listener);
    }

    @Override
    public void removeReadListener(ReadListener listener) {
        super.removeReadListener(listener);
    }

    @Override
    public void addReadExceptionListener(ReadExceptionListener listener) {
        super.addReadExceptionListener(listener);
    }

    @Override
    public void removeReadExceptionListener(ReadExceptionListener listener) {
        super.removeReadExceptionListener(listener);
    }

    @Override
    public void addGpiTriggerListener(GpiTriggerListener listener) {
        super.addGpiTriggerListener(listener);
    }

    @Override
    public void removeGpiTriggerListener(GpiTriggerListener listener) {
        super.removeGpiTriggerListener(listener);
    }

    @Override
    public void addGpiTriggerBoundaryListener(GpiTriggerBoundaryListener listener) {
        super.addGpiTriggerBoundaryListener(listener);
    }

    @Override
    public void removeGpiTriggerBoundaryListener(GpiTriggerBoundaryListener listener) {
        super.removeGpiTriggerBoundaryListener(listener);
    }

    @Override
    public READER_ERR StartReading(int[] ants, int antcnt, BackReadOption pBRO) {
        return super.StartReading(ants, antcnt, pBRO);
    }

    @Override
    public READER_ERR StopReading() {
//        return super.StopReading();

        return READER_ERR.MT_OK_ERR;

    }
}
