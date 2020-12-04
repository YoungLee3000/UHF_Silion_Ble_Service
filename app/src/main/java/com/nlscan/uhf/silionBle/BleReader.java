package com.nlscan.uhf.silionBle;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.nlscan.android.uhf.UHFReader;
import com.nlscan.blecommservice.IBleInterface;
import com.nlscan.blecommservice.IScanConfigCallback;
import com.nlscan.blecommservice.IUHFCallback;
import com.nlscan.uhf.silionBle.upgrade.Native;
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
import java.util.Timer;
import java.util.TimerTask;

public class BleReader extends Reader {


    private Context mContext;
    private static final String TAG = "UHFSilionBleService";

    private Native mCrcModel = new Native();


    private IBleInterface mBleInterface;
    private static final String COMMAND_HEADER = "FF";
    private static final String RESULT_FAIL = "failed";

    //存储标签信息的全局变量
    private LinkedList<TAGINFO> mIvnTagList = new LinkedList<>();
    private LinkedList<String> mBlueDataList = new LinkedList<>();
    private LinkedList<String> mImuDataList = new LinkedList<>();
    private int mIvnCount = 0;
    private static int MAX_TAG = 10;

    private Region_Conf mCurrentConf = Region_Conf.RG_NA;


    //清除标签
    public void clearTagData(){
        mBlueDataList.clear();
        mIvnTagList.clear();
    }



    //过滤器
    private TagFilter_ST m_tagFilter_st = new TagFilter_ST();




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

    public void  setmBleInterface(IBleInterface iBleInterface){
        this.mBleInterface = iBleInterface;
    }



    @Override
    public READER_ERR InitReader(String src, Reader_Type rtype) {

        return READER_ERR.MT_OK_ERR;

    }

    @Override
    public READER_ERR InitReader_Notype(String src, int rtype) {
        if (mBleInterface == null) return READER_ERR.MT_OK_ERR;
//        try {
//            return mBleInterface.isBleAccess() ?  READER_ERR.MT_OK_ERR : READER_ERR.MT_CMD_FAILED_ERR ;
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }

        String sendCommand = "FF00031D0C";
        String resultCode = "failed";

        try {
            resultCode = mBleInterface.sendUhfCommand(sendCommand);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if ("failed".equals(resultCode) )
             return READER_ERR.MT_CMD_FAILED_ERR;

        if (!resultCode.substring(6,10).equals("0000"))
            return READER_ERR.MT_CMD_FAILED_ERR;


        return READER_ERR.MT_OK_ERR;
    }



    @Override
    public READER_ERR GetHardwareDetails(HardwareDetails val) {
        return super.GetHardwareDetails(val);
    }

    @Override
    public void CloseReader() {
//        mBleInterface = null;
    }

    //获取标签，不启用匹配
    @Override
    public READER_ERR GetTagData(int ant, char bank, int address, int blkcnt, byte[] data, byte[] accesspasswd, short timeout) {
//        return super.GetTagData(ant, bank, address, blkcnt, data, accesspasswd, timeout);

        //读取单个标签
        StringBuilder command = new StringBuilder(COMMAND_HEADER);
        String operateCode = "28";
        String strTimeOut = String.format("%04X",timeout);
        String option =   String.format("%02X",m_tagFilter_st.bank);
        String memBank = String.format("%02X",(int)bank);
        String readAddress = String.format("%08X",address);
        String wordCount = String.format("%02X",blkcnt);

        String originPass =  HexUtil.bytesToHexString(accesspasswd);
        String accessPass = "";
        if (originPass.length() >= 8){
            accessPass = originPass.substring(0,8);
        }
        else{

            accessPass = originPass;
            for (int i=0; i< 8 - originPass.length(); i++){
                accessPass = "0" + accessPass;
            }

        }



        String selectAddress =  "";
        String selectLength = "";
        String selectData = "";
        if (m_tagFilter_st.isInvert == 0){
            selectAddress  =  String.format("%08X",m_tagFilter_st.startaddr);
            selectLength = String.format("%02X",m_tagFilter_st.flen);
            selectData = HexUtil.bytesToHexString(m_tagFilter_st.fdata);
        }
        else{
            option = "00";
            accessPass = "";
        }


        int length = (  strTimeOut.length() +   option.length() + memBank.length() +
                readAddress.length() + wordCount.length()  + accessPass.length()
                + selectAddress.length() + selectLength.length() + selectData.length()) / 2;
        String strLength = String.format("%02X",length);

        command.append(strLength);
        command.append(operateCode);
        command.append(strTimeOut);
        command.append(option);
        command.append(memBank);
        command.append(readAddress);
        command.append(wordCount);
        command.append(accessPass);
        command.append(selectAddress);
        command.append(selectLength);
        command.append(selectData);

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
        String option =   String.format("%02X",m_tagFilter_st.bank);
        String readAddress = String.format("%08X",address);
        String memBank = String.format("%02X",(int)bank);
        String writeData = HexUtil.bytesToHexString(data);

        String originPass =  HexUtil.bytesToHexString(accesspasswd);
        String accessPass = "";
        if (originPass.length() >= 8){
            accessPass = originPass.substring(0,8);
        }
        else{

            accessPass = originPass;
            for (int i=0; i< 8 - originPass.length(); i++){
                accessPass = "0" + accessPass;
            }


        }



        String selectAddress =  "";
        String selectLength = "";
        String selectData = "";
        if (m_tagFilter_st.isInvert == 0){
            selectAddress  =  String.format("%08X",m_tagFilter_st.startaddr);
            selectLength = String.format("%02X",m_tagFilter_st.flen);
            selectData = HexUtil.bytesToHexString(m_tagFilter_st.fdata);
        }
        else{
            option = "00";
            accessPass = "";
        }



        int length = (  strTimeOut.length() +   option.length() +
                readAddress.length() + memBank.length()
                + accessPass.length() + selectAddress.length()
                + selectLength.length() + selectData.length()
                + writeData.length()   ) / 2;
        String strLength = String.format("%02X",length);

        command.append(strLength);
        command.append(operateCode);
        command.append(strTimeOut);
        command.append(option);
        command.append(readAddress);
        command.append(memBank);
        command.append(accessPass);
        command.append(selectAddress);
        command.append(selectLength);
        command.append(selectData);
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

    //写EPC
    @Override
    public READER_ERR WriteTagEpcEx(int ant, byte[] Epc, int epclen, byte[] accesspwd, short timeout) {
//        return super.WriteTagEpcEx(ant, Epc, epclen, accesspwd, timeout);

        //读取单个标签
        StringBuilder command = new StringBuilder(COMMAND_HEADER);
        String operateCode = "23";
        String strTimeOut = String.format("%04X",timeout);
        String option = "04" ;
        String rfu = "00";
        String writeData = HexUtil.bytesToHexString(Epc);




        String originPass =  HexUtil.bytesToHexString(accesspwd);
        String accessPass = "";
        if (originPass.length() >= 8){
            accessPass = originPass.substring(0,8);
        }
        else{
            if (originPass.length() == 0){
                option = "00";
            }
            else {
                accessPass = originPass;
                for (int i=0; i< 8 - originPass.length(); i++){
                    accessPass = "0" + accessPass;
                }
            }

        }



        String selectAddress =  "";
        String selectLength = "";
        String selectData = "";
        if (m_tagFilter_st.isInvert == 0){
            selectAddress  =  String.format("%08X",m_tagFilter_st.startaddr);
            selectLength = String.format("%02X",m_tagFilter_st.flen);
            selectData = HexUtil.bytesToHexString(m_tagFilter_st.fdata);
            rfu = "";
        }
        else{
            if (originPass.length() > 0){
                option = "05";
            }
        }






        int length = (  strTimeOut.length() +   option.length() +
                rfu.length()

                + accessPass.length()
                + selectAddress.length()
                + selectLength.length()
                + selectData.length()
                + writeData.length()) / 2;
        String strLength = String.format("%02X",length);

        command.append(strLength);
        command.append(operateCode);
        command.append(strTimeOut);
        command.append(option);
        command.append(rfu);
        command.append(accessPass);
        command.append(selectAddress);
        command.append(selectLength);
        command.append(selectData);
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


    private static final int MAX_IMU = 5;
    public String getImuData(){
        if (mImuDataList.size() > 0 ){
            StringBuilder sb = new StringBuilder("");
            for(int i=0; i<MAX_IMU; i++){
                if (mImuDataList.size() ==0 ) break;
                String imuData = mImuDataList.poll();
                if (imuData.length() < 24) continue;


                String parseImu = "";
                for (int j=0; j<=20; j=j+4){
                    if (j <=8 ){
                        double acce = HexUtil.parseSignedHex(imuData.substring(j,j+4),0XFFFF) * 1.0 / 32767.0 * 16.0 * 9.8;
                        parseImu += String.format("%.8f",acce);
                    }
                    else{
                        double angule = HexUtil.parseSignedHex(imuData.substring(j,j+4),0XFFFF) * 1.0 / 32767.0 * 2000.0;
                        parseImu += String.format("%.8f",angule);
                    }

                    parseImu += ",";
                }
                Log.d(TAG,"the parse imu is " + parseImu);
                if (parseImu.length() > 1){
                    sb.append(parseImu.substring(0,parseImu.length()-1));
                    if(i != MAX_IMU-1 )sb.append("|");
                }

            }
            return sb.toString();

        }
        else {
            return null;
        }
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

        if (mBleInterface == null) return READER_ERR.MT_OK_ERR;
        String resultCode = null;
        try {
            resultCode = mBleInterface.getUhfTagData();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
//              String resultCode = getOriginData();


            if ( resultCode == null ||  "".equals(resultCode))
                return READER_ERR.MT_CMD_FAILED_ERR;
            Log.d(TAG,"the result code is " + resultCode);

            String [] tagArray = resultCode.split(";");


            if (tagArray.length < 1 ) return READER_ERR.MT_CMD_FAILED_ERR;

            for (int i=0; i<tagArray.length; i++){
                if (tagArray[i].substring(4,6).equals("29")){

                    decodeCommon(tagArray[i],tagcnt);
                }
                else if (tagArray[i].substring(4,6).equals("AA")){
                    decodeQuickTag(tagArray[i],tagcnt);
                }
                else{
                    mImuDataList.add(tagArray[i]);
                }
            }










        Log.d(TAG,"tag list size is " + mIvnTagList.size());

        return READER_ERR.MT_OK_ERR;


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
        String option =   String.format("%02X",m_tagFilter_st.bank);
        String originPass = HexUtil.bytesToHexString(accesspasswd);
        String accessPass = originPass;

        if (originPass.length() >= 8){
            accessPass = originPass.substring(0,8);
        }
        else{
            for (int i=0; i< 8 - originPass.length(); i++){
                accessPass = "0" + accessPass;
            }
        }


        String maskBits;
        String actionBits;
        switch (lockobjects){
            case 0:
                maskBits = "0080";
                break;
            case 1:
                maskBits = "0200";
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
                maskBits = "0020";
                break;
        }

        if (locktypes == 0){
            actionBits = "0000";
        }
        else{
            actionBits = maskBits;
        }



        if(m_tagFilter_st.flen > 254){
            return  READER_ERR.MT_CMD_FAILED_ERR;
        }

        String strAddress =  String.format("%08X",m_tagFilter_st.startaddr);
        String selectLength = String.format("%02X",m_tagFilter_st.flen);
        String selectData = HexUtil.bytesToHexString(m_tagFilter_st.fdata);

        if (m_tagFilter_st.isInvert != 0){
            strAddress = "";
            selectLength = "";
            selectData = "";
        }





        int length = (  strTimeOut.length() +   option.length() +
                accessPass.length() + maskBits.length() + actionBits.length()  + strAddress.length()
                + selectLength.length() + selectData.length()) / 2;
        String strLength = String.format("%02X",length);

        command.append(strLength);
        command.append(operateCode);
        command.append(strTimeOut);
        command.append(option);
        command.append(accessPass);
        command.append(maskBits);
        command.append(actionBits);
        command.append(strAddress);
        command.append(selectLength);
        command.append(selectData);


        String  crcStr =     mCrcModel.getCrcStr(HexUtil.toByteArray(command.toString())) ;
        command.append(crcStr);


        Log.d(TAG,"the command is " + command.toString());

        String resultCode = "failed";

        try {
            resultCode = mBleInterface.sendUhfCommand(command.toString());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Log.d(TAG,"the result code is " + resultCode);

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
        String option = m_tagFilter_st.bank == 4 ? String.format("%02X",1) :
                String.format("%02X",m_tagFilter_st.bank)  ;

        String rfu = "00";



        String originPass =  HexUtil.bytesToHexString(killpasswd);
        String accessPass = "";
        if (originPass.length() >= 8){
            accessPass = originPass.substring(0,8);
        }
        else{

            accessPass = originPass;
            for (int i=0; i< 8 - originPass.length(); i++){
                accessPass = "0" + accessPass;
            }


        }



        String selectAddress =  "";
        String selectLength = "";
        String selectData = "";
        if (m_tagFilter_st.isInvert == 0){
            selectAddress  =  String.format("%08X",m_tagFilter_st.startaddr);
            selectLength = String.format("%02X",m_tagFilter_st.flen);
            selectData = HexUtil.bytesToHexString(m_tagFilter_st.fdata);
        }
        else{
            option = "00";
        }





        int length = (  strTimeOut.length() +   option.length() +
                accessPass.length() + rfu.length()  +
                selectAddress.length()+
                selectLength.length() +
                selectData.length()
                ) / 2;
        String strLength = String.format("%02X",length);

        command.append(strLength);
        command.append(operateCode);
        command.append(strTimeOut);
        command.append(option);
        command.append(accessPass);
        command.append(rfu);
        command.append(selectAddress);
        command.append(selectLength);
        command.append(selectData);


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


    /**
     * 获取模块电量
     * @param val
     * @return
     */
    public READER_ERR GetCharge(int[] val){
        if (mBleInterface == null) return READER_ERR.MT_OK_ERR;

//        String sendCommand = "7E013030303040574C535150573B03";
        String sendCommand = "5DCC01010F000F7E013030303040574C535150573B0397B7";
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
        String commandStr ="";

        switch (key.value()){
            case 0://获取session

                break;
            case 1://获取Q值

            case 19://获取Target


                break;

            case 4: //获取天线读写功率

                commandStr = "FF016103BDBE";

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

            case 15://工作区域
                commandStr = "FF00671D68";

                try {
                    resultCode = mBleInterface.sendUhfCommand(commandStr);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                if (   resultCode.equals(RESULT_FAIL))
                    return READER_ERR.MT_CMD_FAILED_ERR;

                if (!resultCode.substring(6,10).equals("0000"))
                    return READER_ERR.MT_CMD_FAILED_ERR;

                String regionStr = resultCode.substring(10,12);
                Region_Conf conf = Region_Conf.RG_NA;
                int zoneCode = 1;
                switch (regionStr){
                    case "01"://北美
                        conf = Region_Conf.RG_NA;
                        break;
                    case "06"://中国1
                        conf = Region_Conf.RG_PRC;
                        break;
                    case "0A"://中国2
                        conf = Region_Conf.RG_PRC2;
                        break;
                    case  "08": //欧洲3
                        conf = Region_Conf.RG_EU3;
                        break;
                    case "FF": //全频
                        conf = Region_Conf.RG_OPEN;
                        break;

                }


                ((Region_Conf[])val)[0] = conf;

                break;


            case 16://获取频点
                HoptableData_ST hdst = new HoptableData_ST();
                switch (mCurrentConf){
                    case RG_NA:
                        hdst.htb  = new int[]{915750,915250,903250,926750,926250,904250,927250,920250,919250,909250,
                                918750,917750,905250,904750,925250,921750,914750,906750,913750,922250,
                                911250,911750,903750,908750,905750,912250,906250,917250,914250,907250,
                                918250,916250,910250,910750,907750,924750,909750,919750,916750,913250,
                                923750,908250,925750,912750,924250,921250,920750,922750,902750,923250};

                        hdst.lenhtb = 50;
                        break;
                    case RG_PRC:
                        hdst.htb  = new int[]{921375,922625,920875,923625,921125,920625,923125,921625,
                                922125,923875,921875,922875,924125,923375,924375,922375};

                        hdst.lenhtb = 16;

                        break;
                    case RG_PRC2:
                        hdst.htb  = new int[] {841375,842625,840875,843625,841125,840625,843125,841625,
                                842125,843875,841875,842875,844125,843375,844375,842375};
                        hdst.lenhtb = 16;
                        break;
                    case RG_EU3:
                        hdst.htb  = new int[]{865700,866300,866900,867500};
                        hdst.lenhtb = 4;
                        break;
                    case RG_OPEN:
                        hdst.htb  = new int[]{840000,850000,860000,870000,880000,890000,900000,
                                910000,920000,930000,940000,950000,960000};
//
                        hdst.lenhtb = 13;
                        break;
                }


                (( HoptableData_ST)val).htb = hdst.htb;
                (( HoptableData_ST)val).lenhtb = hdst.lenhtb;

                break;


            case 23: //获取温度值
                commandStr = "FF00721D7D";

                try {
                    resultCode = mBleInterface.sendUhfCommand(commandStr);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                if (   resultCode.equals(RESULT_FAIL))
                    return READER_ERR.MT_CMD_FAILED_ERR;

                if (!resultCode.substring(6,10).equals("0000"))
                    return READER_ERR.MT_CMD_FAILED_ERR;

                int temperature = HexUtil.parseSignedHex(resultCode.substring(10,12),0Xff);
                ((int[])val)[0] = temperature;


                break;



            default:
                return READER_ERR.MT_OK_ERR;

        }

        return READER_ERR.MT_OK_ERR;


    }

    @Override
    public READER_ERR ParamSet(Mtr_Param key, Object val) {
//        return super.ParamSet(key, val);

        if (mBleInterface == null) return READER_ERR.MT_OK_ERR;

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


        //---频点项---
        StringBuilder frequencyList = new StringBuilder();
        String frequencyStr = "";
        //---频点项---

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
                mCurrentConf = region_conf;
                switch (region_conf){
                    case RG_NA://北美
                        zoneCode = "01";
                        break;
                    case RG_PRC://中国1
                        zoneCode = "06";
                        break;
                    case RG_PRC2://中国2
                        zoneCode = "0A";
                        break;
                    case RG_EU3: //欧洲3
                        zoneCode = "08";
                        break;
                    case RG_OPEN: //全频
                        zoneCode = "FF";
                        break;

                }
                operateCode = "97";



                break;


//            case 16://设置频点
//                HoptableData_ST hoptableDat = (HoptableData_ST ) val;
//                operateCode = "95";
//
//                for (int i=0; i<hoptableDat.lenhtb; i++){
//                    String hoptHex =   String.format("%08X",hoptableDat.htb[i]) ;
//                    frequencyList.append(hoptHex);
//                }
//                frequencyStr = frequencyList.toString();
//
//
//                break;



            case 7:
                if (val == null){
                    m_tagFilter_st.isInvert = 1;
                    return READER_ERR.MT_OK_ERR;
                }
                TagFilter_ST tagFilter_st = (TagFilter_ST) val;
                m_tagFilter_st.bank = tagFilter_st.bank;
                if (tagFilter_st.bank == 1) m_tagFilter_st.bank = 4;
                m_tagFilter_st.fdata = tagFilter_st.fdata;
                m_tagFilter_st.startaddr = tagFilter_st.startaddr;
                m_tagFilter_st.flen = tagFilter_st.flen;
                m_tagFilter_st.isInvert = tagFilter_st.isInvert;

                return READER_ERR.MT_OK_ERR;

            default:
                return READER_ERR.MT_OK_ERR;

        }

        int length = ( protocolValue.length() + parameter.length() + option.length() + value.length()
                      + antNum.length() + readPower.length() + writePower.length() + zoneCode.length() +
                        frequencyStr.length()) / 2;


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
        command.append(frequencyStr);

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


    /**
     * 设置待机时间
     * @param value
     * @return
     */
    public boolean setStandbyTime(int value){
        if (mBleInterface == null) return false;
        String sendCommand =  "5DCC01011000107E013030303040" +
                HexUtil.stringtoHex ("WLSAPO" + value) + "3B03BB20";
        String resultCode = "failed";

        try {
            resultCode = mBleInterface.sendUhfCommand(sendCommand);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return  true;

    }



    /**
     * 设置待机时间
     * @param value
     * @return
     */
    public boolean setPromptMode(String value){
        if (mBleInterface == null) return false;
        String [] valArray = value.split(",");
        if (valArray.length < 2 ) return false;

        int mode = Integer.parseInt(valArray[0]);
        String modeCode =  "@GRVENA";
        switch (mode){
            case 1:
                modeCode = "@GRVENA";
                break;
            case 2:
                modeCode = "@GRLENA";
                break;
            case 3:
                modeCode = "@GRBENA";
                break;
        }


//        String sendCommand = "7E013030303040" +
//                HexUtil.stringtoHex (modeCode + valArray[1])  + "3B03";
        String resultCode = "failed";


//            resultCode = mBleInterface.sendUhfCommand(sendCommand);


        try {
            mBleInterface.setScanConfig(new IScanConfigCallback.Stub() {
                    @Override
                    public void onConfigCallback(final String str) throws RemoteException {

                    }}, modeCode + valArray[1]);
        } catch (RemoteException e) {
            e.printStackTrace();
        }


        return  true;

    }

    /**
     * 发送查找设备的指令
     */
    public void findDevice(){

        String modeCode = "7E013030303040424545504F4E323030304631303030543230563B03";

        try {
             mBleInterface.sendUhfCommand(modeCode);
        } catch (RemoteException e) {
            e.printStackTrace();
        }



    }






    /**
     * 设置为快速盘点
     * @return
     */
    public READER_ERR AsyncStartReading(){
        if (mBleInterface == null) return READER_ERR.MT_OK_ERR;
        String resultCode = "failed";

        String cmd1 = "FF13AA4D6F64756C6574656368AA48000300800378BBDBEC";
        String cmd2 = "FF0EAA4D6F64756C6574656368AA49F3BB0391";
        String cmd1Len = String.format("%02X",cmd1.length()/2);
        String cmd2Len = String.format("%02X",cmd2.length()/2);
        String sendCommand = "303100" + cmd1Len + cmd1 + cmd2Len + cmd2;
        sendCommand = "7EFE" + String.format("%04X",sendCommand.length()/2)  +  sendCommand ;

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
     * 停止快速盘点
     * @return
     */
    @Override
    public READER_ERR AsyncStopReading() {
        if (mBleInterface == null) return READER_ERR.MT_OK_ERR;

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




    /**
     * 解析普通模式标签
     */
    private void decodeCommon(String tagArray, int[] tagcnt ){
//        for (int i=0; i<tagArray.length; i++){
            //解析获取到的标签
            int relLen = tagArray.length();
//            if (relLen < 24) return;
            String relStatus =  tagArray.substring(6,10) ;
            if (!relStatus.equals("0000"))
                return;
            String tagTotalInfo = tagArray.substring(18, relLen -4);
            Log.d(TAG,"the total info is " + tagTotalInfo);
            if (tagTotalInfo == null || tagTotalInfo.length() <= 4){
                tagcnt[0]++;
                mIvnTagList.add(new TAGINFO());
                return;
            }

            int tagCount = Integer.parseInt(tagArray.substring(16,18),16);

            if (tagCount == 0){
                tagcnt[0]++;
                mIvnTagList.add(new TAGINFO());
                return;
            }

            int tagTotLen = tagTotalInfo.length();
            int beginIndex = 0;

            while (beginIndex < tagTotLen){
                TAGINFO tfs = new TAGINFO();
                tfs.ReadCnt = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex,beginIndex+2));
                tfs.RSSI = HexUtil.parseSignedHex(tagTotalInfo.substring(beginIndex+2,beginIndex+4),0xFF) ;
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
//        }
    }

    /**
     * 解析快速模式标签
     */
    private void decodeQuickTag(String tagArray, int[] tagcnt){
//        for (int i=0; i<tagArray.length; i++){

            int relLen = tagArray.length();
//            if (relLen < 24) return ;
            String relStatus =  tagArray.substring(6,10) ;
            if (!relStatus.equals("0000"))
                return;
            String tagTotalInfo = tagArray.substring(14, relLen -4);
            if (tagTotalInfo == null || tagTotalInfo.length() <= 4){
                mIvnTagList.add(new TAGINFO());
                return;
            }
            Log.d(TAG,"the total info is " + tagTotalInfo);


            int tagTotLen = tagTotalInfo.length();
            int beginIndex = 0;
            TAGINFO tfs = new TAGINFO();
            tfs.ReadCnt = HexUtil.hexStr2int(tagTotalInfo.substring(beginIndex,beginIndex+2));
            tfs.RSSI = HexUtil.parseSignedHex(tagTotalInfo.substring(beginIndex+2,beginIndex+4),0XFF) ;
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
            tagcnt[0]++;
//        }
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


        if (mBleInterface == null) return READER_ERR.MT_OK_ERR;

        if (timeout > 100 ) timeout = 100;
        if(timeout  < 20 ) timeout = 20;

        if (delay > 100 ) delay = 100;
        if(delay  < 0 ) delay = 0;

        String strTimeOut = String.format("%04X",timeout);
        String strDelay = String.format("%04X",delay);
        String resultCode = "failed";
        String cmd1 = "FF0522000000"  +  strTimeOut;
        cmd1 += mCrcModel.getCrcStr(HexUtil.toByteArray(cmd1));

        String cmd2 = "FF032900BF004B22";
        String cmd1Len = String.format("%02X",cmd1.length()/2);
        String cmd2Len = String.format("%02X",cmd2.length()/2);
        String sendCommand = "303200"  + strDelay +  cmd1Len + cmd1 + cmd2Len + cmd2;
        sendCommand = "7EFE" + String.format("%04X",sendCommand.length()/2) + sendCommand ;


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
     * 切换到bootload
     * @return
     */
    public UHFReader.READER_STATE doPowerOff(){
        String cmd = "FF00FC0130"  ;

        String resultCode = "failed";

        try {
            resultCode = mBleInterface.sendUhfCommand(cmd);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Log.d(TAG,"the resultcode is " + resultCode);
//        if (resultCode.equals(RESULT_FAIL))
//            return UHFReader.READER_STATE.CMD_FAILED_ERR;

        return UHFReader.READER_STATE.OK_ERR;

    }


    /**
     * 开启盘点，无论快速或普通
     * @return
     */
    public READER_ERR StartReadingCommon(){
        if (mBleInterface == null) return READER_ERR.MT_OK_ERR;

        String resultCode = "failed";

        try {

           resultCode = mBleInterface.sendUhfCommand("7EFE00023131");

        } catch (RemoteException e) {
            e.printStackTrace();
        }

        //开启IMU数据
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {

                    mBleInterface.sendUhfCommand("7EFD00020130");

                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        },500);

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

        if (mBleInterface == null) return READER_ERR.MT_OK_ERR;
         String sendCommand = "7EFE00023130";

        String resultCode = "failed";

        try {
            resultCode = mBleInterface.sendUhfCommand(sendCommand);

        } catch (RemoteException e) {
            e.printStackTrace();
        }


        //关闭IMU数据
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {

                    mBleInterface.sendUhfCommand("7EFD00020131");

                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        },500);



        Log.d(TAG,"the resultcode is " + resultCode);

        return READER_ERR.MT_OK_ERR;

    }
}
