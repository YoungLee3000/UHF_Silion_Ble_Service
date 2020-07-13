package com.nlscan.uhf.silionBle;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.nlscan.android.uhf.UHFReader;
import com.nlscan.blecommservice.IBleInterface;
import com.nlscan.blecommservice.IUHFCallback;
import com.uhf.api.cls.BackReadOption;
import com.uhf.api.cls.ErrInfo;
import com.uhf.api.cls.GpiInfo_ST;
import com.uhf.api.cls.GpiTriggerBoundaryListener;
import com.uhf.api.cls.GpiTriggerListener;
import com.uhf.api.cls.ReadExceptionListener;
import com.uhf.api.cls.ReadListener;
import com.uhf.api.cls.Reader;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class BleReader extends Reader {


    private Context mContext;
    private static final String TAG = "UHFSilionBleService";

    private CrcModel mCrcModel = new CrcModel();


    private IBleInterface mBleInterface;
    private static final String COMMAND_HEADER = "FF";
    private static final String RESULT_FAIL = "failed";

    //存储标签信息的全局变量
    private LinkedList<TAGINFO> mIvnTagList = new LinkedList<>();
    private LinkedList<String> mBlueDataList = new LinkedList<>();
    private int mIvnCount = 0;
    private static int MAX_TAG = 10;


    //清除标签
    public void clearTagData(){
        mBlueDataList.clear();
        mIvnTagList.clear();
    }




    private IUHFCallback.Stub mUhfCallback = new IUHFCallback.Stub() {
        @Override
        public void onReceiveUhf(String data) throws RemoteException {
            mBlueDataList.add(data);
        }
    };


    public BleReader(IBleInterface iBleInterface) {
        this.mBleInterface = iBleInterface;
        try {
            if (this.mBleInterface !=  null)  this.mBleInterface.setUhfCallback(mUhfCallback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
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
        mBleInterface = null;
    }

    //获取标签，不启用匹配
    @Override
    public READER_ERR GetTagData(int ant, char bank, int address, int blkcnt, byte[] data, byte[] accesspasswd, short timeout) {
//        return super.GetTagData(ant, bank, address, blkcnt, data, accesspasswd, timeout);

        //读取单个标签
        StringBuilder command = new StringBuilder(COMMAND_HEADER);
        String operateCode = "28";
        String strTimeOut = String.format("%04X",timeout);
        String option = "00" ;
        String memBank = String.format("%02X",(int)bank);
        String readAddress = String.format("%08X",address);
        String wordCount = String.format("%02X",blkcnt);


        int length = (  strTimeOut.length() +   option.length() + memBank.length() +
                readAddress.length() + wordCount.length()) / 2;
        String strLength = String.format("%02X",length);

        command.append(strLength);
        command.append(operateCode);
        command.append(strTimeOut);
        command.append(option);
        command.append(memBank);
        command.append(readAddress);
        command.append(wordCount);

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

        String relStatus =  resultCode.substring(6,10) ;
        if (!relStatus.equals("0000"))
            return READER_ERR.MT_CMD_FAILED_ERR;

        int relLen =   Integer.parseInt(resultCode.substring(2,4),16) * 2 ;
        int dataLen = relLen - 2;
        byte[] readData =  HexUtil.toByteArray(resultCode.substring(12,12+dataLen));

        for (int i=0; i<data.length && i<readData.length ; i++){
            data[i] = readData[i];
        }

        return READER_ERR.MT_OK_ERR;


    }


    //写标签，不启用匹配
    @Override
    public READER_ERR WriteTagData(int ant, char bank, int address, byte[] data, int datalen, byte[] accesspasswd, short timeout) {

        //读取单个标签
        StringBuilder command = new StringBuilder(COMMAND_HEADER);
        String operateCode = "24";
        String strTimeOut = String.format("%04X",timeout);
        String option = "00" ;
        String readAddress = String.format("%08X",address);
        String memBank = String.format("%02X",(int)bank);
        String writeData = HexUtil.bytesToHexString(data);


        int length = (  strTimeOut.length() +   option.length() + memBank.length() +
                readAddress.length() + writeData.length()) / 2;
        String strLength = String.format("%02X",length);

        command.append(strLength);
        command.append(operateCode);
        command.append(strTimeOut);
        command.append(option);
        command.append(readAddress);
        command.append(memBank);
        command.append(writeData);

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

        //解析返回结果
        String relStatus =  resultCode.substring(6,10) ;
        if (!relStatus.equals("0000"))
            return READER_ERR.MT_CMD_FAILED_ERR;

        return READER_ERR.MT_OK_ERR;


    }

    @Override
    public READER_ERR WriteTagEpcEx(int ant, byte[] Epc, int epclen, byte[] accesspwd, short timeout) {
//        return super.WriteTagEpcEx(ant, Epc, epclen, accesspwd, timeout);

        //读取单个标签
        StringBuilder command = new StringBuilder(COMMAND_HEADER);
        String operateCode = "23";
        String strTimeOut = String.format("%04X",timeout);
        String option = "00" ;
        String rfu = "00";
        String writeData = HexUtil.bytesToHexString(Epc);


        int length = (  strTimeOut.length() +   option.length() +
                rfu.length() + writeData.length()) / 2;
        String strLength = String.format("%02X",length);

        command.append(strLength);
        command.append(operateCode);
        command.append(strTimeOut);
        command.append(option);
        command.append(rfu);
        command.append(writeData);

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

        //解析返回结果
        String relStatus =  resultCode.substring(6,10) ;
        if (!relStatus.equals("0000"))
            return READER_ERR.MT_CMD_FAILED_ERR;

        return READER_ERR.MT_OK_ERR;





    }

    @Override
    public READER_ERR TagInventory(int[] ants, int antcnt, short timeout, TAGINFO[] pTInfo, int[] tagcnt) {
        return super.TagInventory(ants, antcnt, timeout, pTInfo, tagcnt);
    }

    @Override
    public READER_ERR TagInventory_Raw(int[] ants, int antcnt, short timeout, int[] tagcnt) {


//        //发送多标签盘存命令
//        StringBuilder command = new StringBuilder(COMMAND_HEADER);
//        String operateCode = "22";
//        String option = "00";
//        String searchFlag = "0000";
//        String strTimeOut = String.format("%04X",timeout);
//
//        int length = ( option.length() + searchFlag.length() +
//                strTimeOut.length()) / 2;
//        String strLength = String.format("%02X",length);
//
//        command.append(strLength);
//        command.append(operateCode);
//        command.append(option);
//        command.append(searchFlag);
//        command.append(strTimeOut);
//
//        String  crcStr =     mCrcModel.getCrcStr(HexUtil.toByteArray(command.toString())) ;
//        command.append(crcStr);
//
//        String resultCode = "failed";
//
//        try {
//            resultCode = mBleInterface.sendUhfCommand(command.toString());
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }

        return getInvTagCount(tagcnt);
    }


    /**
     * 获取原始蓝牙返回的数据
     * @return
     */
    private String getOriginData(){
        int count = 0;
        StringBuilder sb = new StringBuilder("");
        while (count < MAX_TAG){
            String data = mBlueDataList.poll();
            if (data != null){
                Log.d(TAG,"list data is " + data);
                sb.append(data);
                sb.append(";");
            }
            count++;
        }
        String result = sb.toString();
        return  result.length() > 1 ?   result.substring(0,result.length()-1) : "";
    }


    /**
     * 获取返回的标签，包括快速模式与普通模式
     * @param tagcnt
     * @return
     */
    public READER_ERR getInvTagCount( int[] tagcnt){


        //发送获取标签命令
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

//        String resultCode = "failed";


//            mBleInterface.sendUhfCommand(command.toString());
        String resultCode = null;
        try {
            resultCode = mBleInterface.getUhfTagData();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
//              String resultCode = getOriginData();

            Log.d(TAG,"the result code is " +resultCode.substring(0,6));
            if ( resultCode == null ||  "".equals(resultCode))
                return READER_ERR.MT_CMD_FAILED_ERR;


            String [] tagArray = resultCode.split(";");


            if (tagArray.length < 1 ) return READER_ERR.MT_CMD_FAILED_ERR;

            if (tagArray[0].substring(4,6).equals("29")){

                decodeCommon(tagArray,tagcnt);
            }
            else if (tagArray[0].substring(4,6).equals("AA")){
                tagcnt[0] = tagArray.length;
                decodeQuickTag(tagArray);
            }






        Log.d(TAG,"tag list size is " + mIvnTagList.size());

        return READER_ERR.MT_OK_ERR;


    }







    @Override
    public READER_ERR TagInventory_BaseType(int[] ants, int antcnt, short timeout, byte[] outbuf, int[] tagcnt) {
        return super.TagInventory_BaseType(ants, antcnt, timeout, outbuf, tagcnt);
    }


    /**
     * 获取盘存标签，普通模式
     * @param TI
     * @return
     */
    @Override
    public READER_ERR GetNextTag(TAGINFO TI) {

        Log.d(TAG,"get next tag " );
        if ( mIvnTagList.size() > 0){
            TAGINFO tfs = mIvnTagList.poll();
            TI.ReadCnt = tfs.ReadCnt;
            TI.RSSI = tfs.RSSI;
            TI.AntennaID = tfs.AntennaID;
            TI.Frequency = tfs.Frequency;
            TI.TimeStamp = tfs.TimeStamp;
            TI.Res = tfs.Res;
            TI.PC = tfs.PC;
            TI.EpcId = tfs.EpcId;
            TI.CRC = tfs.CRC;
            Log.d(TAG,"TI is null " + (TI == null));
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
//        return super.LockTag(ant, lockobjects, locktypes, accesspasswd, timeout);

        //锁定标签
        StringBuilder command = new StringBuilder(COMMAND_HEADER);
        String operateCode = "25";
        String strTimeOut = String.format("%04X",timeout);
        String option = "00" ;
        String accessPass = HexUtil.bytesToHexString(accesspasswd).substring(0,8);
        String maskBits;
        String actionBits;
        switch (lockobjects){
            case 0:
                maskBits = "0200";
                break;
            case 1:
                maskBits = "0080";
                break;
            case 2:
                maskBits = "0020";
                break;
            case 3:
                maskBits = "0008";
                break;
            case 4:
                maskBits = "0002";
                break;
            default:
                maskBits = "0200";
                break;
        }

        actionBits = maskBits;






        int length = (  strTimeOut.length() +   option.length() +
                accessPass.length() + maskBits.length() + actionBits.length()) / 2;
        String strLength = String.format("%02X",length);

        command.append(strLength);
        command.append(operateCode);
        command.append(strTimeOut);
        command.append(option);
        command.append(accessPass);
        command.append(maskBits);
        command.append(actionBits);


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

        //解析返回结果
        String relStatus =  resultCode.substring(6,10) ;
        if (!relStatus.equals("0000"))
            return READER_ERR.MT_CMD_FAILED_ERR;

        return READER_ERR.MT_OK_ERR;


    }

    @Override
    public READER_ERR KillTag(int ant, byte[] killpasswd, short timeout) {
//        return super.KillTag(ant, killpasswd, timeout);

        //灭活标签
        StringBuilder command = new StringBuilder(COMMAND_HEADER);
        String operateCode = "26";
        String strTimeOut = String.format("%04X",timeout);
        String option = "00" ;
        String accessPass = HexUtil.bytesToHexString(killpasswd).substring(0,8);
        String rfu = "00";


        int length = (  strTimeOut.length() +   option.length() +
                accessPass.length() + rfu.length()) / 2;
        String strLength = String.format("%02X",length);

        command.append(strLength);
        command.append(operateCode);
        command.append(strTimeOut);
        command.append(option);
        command.append(accessPass);
        command.append(rfu);


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

        //解析返回结果
        String relStatus =  resultCode.substring(6,10) ;
        if (!relStatus.equals("0000"))
            return READER_ERR.MT_CMD_FAILED_ERR;

        return READER_ERR.MT_OK_ERR;


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


    /**
     * 获取模块电量
     * @param val
     * @return
     */
    public READER_ERR GetCharge(int[] val){

        String sendCommand = "7E013030303040574C535150573B03";
        String resultCode = "failed";

        try {
            resultCode = mBleInterface.sendUhfCommand(sendCommand);
        } catch (RemoteException e) {
            e.printStackTrace();
        }


        if (resultCode.equals("failed") )
            return READER_ERR.MT_CMD_FAILED_ERR;

        if (!resultCode.startsWith("02013030"))
            return READER_ERR.MT_CMD_FAILED_ERR;

        String chargeVal = resultCode.substring(26);

        if (chargeVal.length() == 12){
            val[0] = 100;
        }
        if (chargeVal.length() == 10){
            val[0] = Integer.parseInt(chargeVal.substring(1,2)) * 10 +
                    Integer.parseInt(chargeVal.substring(3,4));
        }
        if (chargeVal.length() == 8){
            val[0] = Integer.parseInt(chargeVal.substring(1,2));
        }

        Log.d(TAG,"the resultcode is " + resultCode);



        return READER_ERR.MT_OK_ERR;


    }

    /**
     * 获取参数值
     * @param key
     * @param val
     * @return
     */
    @Override
    public READER_ERR ParamGet(Mtr_Param key, Object val) {



        String resultCode = "failed";

        switch (key.value()){
            case 0://获取session

                break;
            case 1://获取Q值

            case 19://获取Target


                break;

            case 4: //获取天线读写功率

                String commandStr = "FF016103BDBE";

                try {
                    resultCode = mBleInterface.sendUhfCommand(commandStr);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                if (   resultCode.equals(RESULT_FAIL))
                    return READER_ERR.MT_CMD_FAILED_ERR;

                if (!resultCode.substring(6,10).equals("0000"))
                    return READER_ERR.MT_CMD_FAILED_ERR;

                Reader.AntPower power = new Reader.AntPower();

                power.readPower =  (short)Integer.parseInt(resultCode.substring(14,18),16);
                power.writePower =  (short)Integer.parseInt(resultCode.substring(18,22),16);
                ((AntPowerConf ) val).Powers[0] =  power ;
                break;
            case 5://读写器最大功率
                ((int[])val)[0] = 3000;
                break;

            case 15:
                break;
            default:
                return READER_ERR.MT_OK_ERR;

        }

        return READER_ERR.MT_OK_ERR;


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
                Log.d(TAG,"the powers len " + powerConf.Powers.length);
                operateCode = "91";
                option = "03";
                antNum = "01";
                readPower =  String.format("%04X",powerConf.Powers[0].readPower);
                writePower = String.format("%04X",powerConf.Powers[0].writePower);
                break;

            case 15://设置区域
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

        int length = ( protocolValue.length() + parameter.length() + option.length() + value.length()
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

        String sendCommand = "7EFE00023031";
        String resultCode = "failed";

        try {
            resultCode = mBleInterface.sendUhfCommand(sendCommand);
            mBleInterface.sendUhfCommand("7EFE00023131");
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Log.d(TAG,"the resultcode is " + resultCode);
//        if (resultCode.equals(RESULT_FAIL))
//            return READER_ERR.MT_CMD_FAILED_ERR;


        return READER_ERR.MT_OK_ERR;
    }


    /**
     * 重载开启快速盘点设置
     * @return
     */
    public READER_ERR AsyncStartReading(){
        String sendCommand = "7EFE00023031";
        String resultCode = "failed";

        try {
            resultCode = mBleInterface.sendUhfCommand(sendCommand);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Log.d(TAG,"the resultcode is " + resultCode);
//        if (resultCode.equals(RESULT_FAIL))
//            return READER_ERR.MT_CMD_FAILED_ERR;


        return READER_ERR.MT_OK_ERR;
    }



    @Override
    public READER_ERR AsyncStopReading() {

//        String sendCommand = "FF0EAA4D6F64756C6574656368AA49F3BB0391";
        String sendCommand = "7EFE00023130";


        String resultCode = "failed";

        try {
            resultCode = mBleInterface.sendUhfCommand(sendCommand);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

//        if (   resultCode.equals(RESULT_FAIL))
//            return READER_ERR.MT_CMD_FAILED_ERR;

        return READER_ERR.MT_OK_ERR;

    }

    @Override
    public READER_ERR AsyncGetTagCount(int[] tagcnt) {
        String resultCode = "failed";

        try {
            resultCode = mBleInterface.getUhfTagData();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Log.d(TAG,"the resultcode is " + resultCode);
        if (resultCode.equals(RESULT_FAIL))
            return READER_ERR.MT_CMD_FAILED_ERR;


        String []tagArray = resultCode.split(";");
        tagcnt[0] = tagArray.length;
        int tagCount = tagArray.length;



        //解析获取到的标签,每次只获取一个标签
        for (int i=0; i<tagCount; i++){

            int relLen = tagArray[i].length();
            if (relLen < 24) return READER_ERR.MT_CMD_FAILED_ERR;
            String relStatus =  tagArray[i].substring(6,10) ;
            if (!relStatus.equals("0000"))
                return READER_ERR.MT_CMD_FAILED_ERR;
            String tagTotalInfo = tagArray[i].substring(14, relLen -4);
            Log.d(TAG,"the total info is " + tagTotalInfo);


            int tagTotLen = tagTotalInfo.length();
            int beginIndex = 0;
            TAGINFO tfs = new TAGINFO();
            tfs.ReadCnt = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex,beginIndex+2));
            tfs.RSSI = HexUtil.parseSignedHex(tagTotalInfo.substring(beginIndex+2,beginIndex+4)) ;
            tfs.AntennaID = 1;
            tfs.Frequency = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+6,beginIndex+12));
            tfs.TimeStamp = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+12,beginIndex+20));
            tfs.Res   = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+20,beginIndex+24));
            int epcLen = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+24,beginIndex+26))  * 2;
            int tagLen = epcLen - 8;
            tfs.PC = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+26,beginIndex+30));
            tfs.EpcId = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+30,beginIndex+30+tagLen));
            tfs.CRC = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+30+tagLen,beginIndex+30+tagLen+4));
            mIvnTagList.add(tfs);
            Log.d(TAG,"tag list size is " + mIvnTagList.size());
        }


        return READER_ERR.MT_OK_ERR;
    }


    /**
     * 解析普通模式标签
     */
    private void decodeCommon(String[] tagArray, int[] tagcnt ){
        tagcnt[0] = 0;
        for (int i=0; i<tagArray.length; i++){
            //解析获取到的标签
            int relLen = tagArray[i].length();
            if (relLen < 24) return;
            String relStatus =  tagArray[i].substring(6,10) ;
            if (!relStatus.equals("0000"))
                return;
            String tagTotalInfo = tagArray[i].substring(18, relLen -4);
            Log.d(TAG,"the total info is " + tagTotalInfo);
            int tagCount = Integer.parseInt(tagArray[i].substring(16,18),16);

            int tagTotLen = tagTotalInfo.length();
            int beginIndex = 0;

            while (beginIndex < tagTotLen){
                TAGINFO tfs = new TAGINFO();
                tfs.ReadCnt = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex,beginIndex+2));
                tfs.RSSI = HexUtil.parseSignedHex(tagTotalInfo.substring(beginIndex+2,beginIndex+4)) ;
                tfs.AntennaID = 1;
                tfs.Frequency = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+6,beginIndex+12));
                tfs.TimeStamp = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+12,beginIndex+20));
                tfs.Res = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+20,beginIndex+24));
                int epcLen = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+28,beginIndex+32)) /8 * 2;
                int tagLen = epcLen - 8;
                tfs.PC = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+32,beginIndex+36));
                tfs.EpcId = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+36,beginIndex+36+tagLen));
                tfs.CRC = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+36+tagLen,beginIndex+36+tagLen+4));
                mIvnTagList.add(tfs);
                tagcnt[0]++;
                beginIndex += 32 + epcLen;
            }
        }
    }

    /**
     * 解析快速模式标签
     */
    private void decodeQuickTag(String[] tagArray){
        for (int i=0; i<tagArray.length; i++){

            int relLen = tagArray[i].length();
            if (relLen < 24) return ;
            String relStatus =  tagArray[i].substring(6,10) ;
            if (!relStatus.equals("0000"))
                return ;
            String tagTotalInfo = tagArray[i].substring(14, relLen -4);
            Log.d(TAG,"the total info is " + tagTotalInfo);


            int tagTotLen = tagTotalInfo.length();
            int beginIndex = 0;
            TAGINFO tfs = new TAGINFO();
            tfs.ReadCnt = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex,beginIndex+2));
            tfs.RSSI = HexUtil.parseSignedHex(tagTotalInfo.substring(beginIndex+2,beginIndex+4)) ;
            tfs.AntennaID = 1;
//            tfs.Frequency = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+6,beginIndex+12));
//            tfs.TimeStamp = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+12,beginIndex+20));
//            tfs.Res   = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+20,beginIndex+24));
            int epcLen = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+4,beginIndex+6))  * 2;
            int tagLen = epcLen - 8;
            tfs.PC = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+6,beginIndex+10));
            tfs.EpcId = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+10,beginIndex+10+tagLen));
            tfs.CRC = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+10+tagLen,beginIndex+10+tagLen+4));
            mIvnTagList.add(tfs);
            Log.d(TAG,"tag list size is " + mIvnTagList.size());
        }
    }


    /**
     * 重载异步获取标签的方法
     * @param tagcnt
     * @param taginfos
     * @return
     */
    public READER_ERR AsyncGetTagCount(int[] tagcnt, TAGINFO[] taginfos) {


        String resultCode = "failed";

        try {
            resultCode = mBleInterface.getUhfTagData();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Log.d(TAG,"the resultcode is " + resultCode);
        if (resultCode.equals(RESULT_FAIL))
            return READER_ERR.MT_CMD_FAILED_ERR;


        tagcnt[0] = 255;

//        mIvnTagList.clear();
//        mIvnCount = 0;
        //解析获取到的标签,每次只获取一个标签
        int relLen = resultCode.length();
        if (relLen < 24) return READER_ERR.MT_CMD_FAILED_ERR;
        String relStatus =  resultCode.substring(6,10) ;
        if (!relStatus.equals("0000"))
            return READER_ERR.MT_CMD_FAILED_ERR;
        String tagTotalInfo = resultCode.substring(14, relLen -4);
        Log.d(TAG,"the total info is " + tagTotalInfo);
//        int tagCount = Integer.parseInt(resultCode.substring(16,18),16);

        int tagTotLen = tagTotalInfo.length();
        int beginIndex = 0;

//        while (beginIndex < tagTotLen){
            TAGINFO tfs = new TAGINFO();
            tfs.ReadCnt = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex,beginIndex+2));
            tfs.RSSI = HexUtil.parseSignedHex(tagTotalInfo.substring(beginIndex+2,beginIndex+4)) ;
            tfs.AntennaID = (byte)HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+4,beginIndex+6));
            tfs.Frequency = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+6,beginIndex+12));
            tfs.TimeStamp = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+12,beginIndex+20));
            tfs.Res   = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+20,beginIndex+24));
            int epcLen = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex+24,beginIndex+26))  * 2;
            int tagLen = epcLen - 8;
            tfs.PC = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+26,beginIndex+30));
            tfs.EpcId = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+30,beginIndex+30+tagLen));
            tfs.CRC = HexUtil.toByteArray(tagTotalInfo.substring(beginIndex+30+tagLen,beginIndex+30+tagLen+4));
            mIvnTagList.add(tfs);
//            beginIndex += 22 + epcLen;
            taginfos[0] = tfs;
//            mIvnCount++;
//        }

        Log.d(TAG,"tag list size is " + mIvnTagList.size());

        return READER_ERR.MT_OK_ERR;


    }


    /**
     * 获取盘存标签，快速模式
     * @param TI
     * @return
     */
    @Override
    public READER_ERR AsyncGetNextTag(TAGINFO TI) {

        if (mIvnTagList.size() > 0){
            TAGINFO tfs = mIvnTagList.poll();
            TI.ReadCnt = tfs.ReadCnt;
            TI.RSSI = tfs.RSSI;
            TI.AntennaID = tfs.AntennaID;
            TI.Frequency = tfs.Frequency;
            TI.TimeStamp = tfs.TimeStamp;
            TI.Res = tfs.Res;
            TI.PC = tfs.PC;
            TI.EpcId = tfs.EpcId;
            TI.CRC = tfs.CRC;


            Log.d(TAG,"the TI is null "  + (TI ==null));
            return READER_ERR.MT_OK_ERR;
        }
        else{
            return READER_ERR.MT_CMD_FAILED_ERR;
        }
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

    /**
     * 设置普通盘点模式
     * @param timeout
     * @param delay
     * @return
     */
    public READER_ERR StartReading(int timeout, int delay){



        String sendCommand = "7EFE00063032";
        String strTimeOut = String.format("%04X",timeout);
        String strDelay = String.format("%04X",delay);
        sendCommand = sendCommand + strTimeOut + strDelay;
        String resultCode = "failed";

        try {
            resultCode = mBleInterface.sendUhfCommand(sendCommand);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Log.d(TAG,"the resultcode is " + resultCode);
//        if (resultCode.equals(RESULT_FAIL))
//            return READER_ERR.MT_CMD_FAILED_ERR;

        return READER_ERR.MT_OK_ERR;

    }


    /**
     * 开启盘点，无论快速或普通
     * @return
     */
    public READER_ERR StartReadingCommon(){

        String resultCode = "failed";

        try {

           resultCode = mBleInterface.sendUhfCommand("7EFE00023131");

        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Log.d(TAG,"the resultcode is " + resultCode);
//        if (resultCode.equals(RESULT_FAIL))
//            return READER_ERR.MT_CMD_FAILED_ERR;

        return READER_ERR.MT_OK_ERR;
    }


    /**
     * 结束普通盘点
     * @return
     */
    @Override
    public READER_ERR StopReading() {
         String sendCommand = "7EFE00023130";

        String resultCode = "failed";

        try {
            resultCode = mBleInterface.sendUhfCommand(sendCommand);

        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Log.d(TAG,"the resultcode is " + resultCode);

        return READER_ERR.MT_OK_ERR;

    }
}
