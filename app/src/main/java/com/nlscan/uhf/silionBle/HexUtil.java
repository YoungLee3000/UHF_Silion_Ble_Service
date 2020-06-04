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
	 * 将16进制字符串转成二进制字节数据，目前此函数只支持长度不超过600个字符的16进制字符串的转换。
	 * @param buf 16进制字符串(长度必须是2的倍数，因为两个16进制字符转换为一个二进制字节，分别代表一个字节的高四位和低四位):例(0x):0A02
	 * @return 二进制字节数组
	 */
	public static byte[] Str2Hex(String hexStr) 
	{
		if(TextUtils.isEmpty(hexStr))
			return null;
		
		try {
			String chex = "0123456789ABCDEF";
			int len = hexStr.length();
			byte[] hexbuf = new byte[len/2];
		    for (int i = 0; i < len; i += 2)
		    {
		      byte hnx = (byte)chex.indexOf(hexStr.toUpperCase().substring(i, i + 1));
		      byte lnx = 0;
		      if (hexStr.length() > i + 1) {
		        lnx = (byte)chex.indexOf(hexStr.toUpperCase().substring(i + 1, i + 2));
		      }
		      hexbuf[(i / 2)] = ((byte)(hnx << 4 & 0xFF | lnx & 0xFF));
		    }
			
			return hexbuf;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
