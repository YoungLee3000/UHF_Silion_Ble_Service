package com.nlscan.uhf.silionBle;

import android.text.TextUtils;

/**
 * 16进制数据转换工具
 * 
 * @author zch
 *
 */
public class HexUtil {

	/**
	 * 16进制数转成字节数组，如：字符N的16进制数是4e，则传入数组{'4','e'}，将被转成字段数组，再用new String(byte[]
	 * b)的方法可以打印出N来
	 * 
	 * @param data
	 * @return
	 */
	public static byte[] decodeHex(char[] data) {

		int len = data.length;

		if ((len & 0x01) != 0) {
			throw new RuntimeException("Odd number of characters.");
		}

		try {
			byte[] out = new byte[len >> 1];

			// two characters form the hex value.
			for (int i = 0, j = 0; j < len; i++) {
				int f = toDigit(data[j], j) << 4;
				j++;
				f = f | toDigit(data[j], j);
				j++;
				out[i] = (byte) (f & 0xFF);
			}

			return out;
		} catch (Exception e) {
		}

		return null;
	}

	/**
	 * 将十六进制字符转换成一个整数
	 *
	 * @param ch
	 *            十六进制char
	 * @param index
	 *            十六进制字符在字符数组中的位置
	 * @return 一个整数
	 * @throws Exception
	 *             当ch不是一个合法的十六进制字符时，抛出运行时异常
	 */
	protected static int toDigit(char ch, int index) throws Exception {
		int digit = Character.digit(ch, 16);
		if (digit == -1) {
			throw new Exception("Illegal hexadecimal character " + ch
					+ " at index " + index);
		}
		return digit;
	}

	public static boolean isValidHexString(String data) {
		String reg = "[0-9a-fA-F]*";
		if (!TextUtils.isEmpty(data))
			return data.matches(reg);
		return false;
	}


	/**
	 * 16进制字节转字符
	 * @param bArray
	 * @return
	 */
	public static final String bytesToHexString(byte[] bArray) {
		if(bArray == null || bArray.length <= 0)
			return null;
		StringBuffer sb = new StringBuffer(bArray.length);
		String sTemp;
		for (int i = 0; i < bArray.length; i++) {
			sTemp = Integer.toHexString(0xFF & bArray[i]);
			if (sTemp.length() < 2)
				sb.append(0);
			sb.append(sTemp);
		}
		return sb.toString();
	}
	



	/**
	 * 16进制的字符串表示转成字节数组
	 * @param hexString 16进制格式的字符串
	 * @return 转换后的字节数组
	 **/
	public static byte[] toByteArray(String hexString) {
		hexString = hexString.replaceAll(" ", "");
		final byte[] byteArray = new byte[hexString.length() / 2];
		int k = 0;
		for (int i = 0; i < byteArray.length; i++) {//因为是16进制，最多只会占用4位，转换成字节需要两个16进制的字符，高位在先
			byte high = (byte) (Character.digit(hexString.charAt(k), 16) & 0xff);
			byte low = (byte) (Character.digit(hexString.charAt(k + 1), 16) & 0xff);
			byteArray[i] = (byte) (high << 4 | low);
			k += 2;
		}
		return byteArray;
	}


	/**
	 * 16进制字符转整形数
	 * @param hexStr
	 * @return
	 */
	public static int hexStr2int(String hexStr){
		try {
			return Integer.parseInt(hexStr,16);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return 0;
	}



	//解析有符号16进制字符
	public static int parseSignedHex(String hexStr){
		byte[] bytes = toByteArray(hexStr);
		String binaryStr = bytes2BinaryStr(bytes);
		return parseSignedBinary(binaryStr);
	}



	//解析有符号2进制字符
	public static int parseSignedBinary(String binaryStr) {

		return binaryStr.charAt(0) == '0' ? Integer.parseInt(binaryStr,2) :
				-1* Integer.parseInt(binaryStr.substring(1),2);
	}



	//字节转二进制字符
	public static String bytes2BinaryStr(byte[] bArray){

		String outStr = "";
		int pos = 0;
		for(byte b:bArray){
			//高四位
			pos = (b&0xF0)>>4;
			outStr+=binaryArray[pos];
			//低四位
			pos=b&0x0F;
			outStr+=binaryArray[pos];
		}
		return outStr;

	}


	//二进制数组
	private static String[] binaryArray =
			{"0000","0001","0010","0011",
					"0100","0101","0110","0111",
					"1000","1001","1010","1011",
					"1100","1101","1110","1111"};






}
