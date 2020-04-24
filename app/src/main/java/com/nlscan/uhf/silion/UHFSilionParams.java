package com.nlscan.uhf.silion;


public class UHFSilionParams {

	/**
	 * Gen2协议参数
	 */
	public final static class POTL_GEN2_SESSION{
		public final static String KEY = "POTL_GEN2_SESSION";
		/**Gen2协议*/
		public final static String PARAM_POTL_GEN2_SESSION = "PARAM_POTL_GEN2_SESSION";
	}
	
	/**
	 * Gen2协议参数Q值
	 */
	public final static class POTL_GEN2_Q{
		public final static String KEY = "POTL_GEN2_Q";
		/**Gen2协议参数Q值*/
		public final static String PARAM_POTL_GEN2_Q = "PARAM_POTL_GEN2_Q";
	}
	
	/**
	 * Gen2协议基带编码方式
	 */
	public final static class POTL_GEN2_TAGENCODING{
		public final static String KEY = "POTL_GEN2_TAGENCODING";
		/**Gen2协议基带编码方式*/
		public final static String PARAM_POTL_GEN2_TAGENCODING = "PARAM_POTL_GEN2_TAGENCODING";
	}

	/**
	 * 支持的最大EPC长度，单位为bit
	 */
	public final static class POTL_GEN2_MAXEPCLEN{
		public final static String KEY = "POTL_GEN2_MAXEPCLEN";
		/**支持的最大EPC长度 */
		public final static String PARAM_POTL_GEN2_MAXEPCLEN = "PARAM_POTL_GEN2_MAXEPCLEN";
	}
	
	/**
	 * 读写器发射功率
	 */
	public final static class RF_ANTPOWER{
		public final static String KEY = "RF_ANTPOWER";
		/**读写器发射功率,数据格式:天线ID, 读功率,写功率|天线ID, 读功率,写功率*/
		public final static String PARAM_RF_ANTPOWER = "PARAM_RF_ANTPOWER";
	}
	
	/**
	 * 读写器最大输出功率(单位为centi-dbm)(只读)
	 */
	public final static class RF_MAXPOWER{
		public final static String KEY = "RF_MAXPOWER";
		/**读写器最大输出功率 */
		public final static String PARAM_RF_MAXPOWER = "PARAM_RF_MAXPOWER";
	}
	
	/**
	 * 读写器最小输出功率(只读)
	 */
	public final static class RF_MINPOWER{
		public final static String KEY = "RF_MINPOWER";
		/**读写器最小输出功率 */
		public final static String PARAM_RF_MINPOWER = "PARAM_RF_MINPOWER";
	}
	
	/**
	 * 标签过滤器
	 */
	public final static class TAG_FILTER{
		public final static String KEY = "TAG_FILTER";
		public final static String PARAM_TAG_FILTER = "PARAM_TAG_FILTER";
	    /**移除过滤器*/
	    public final static String PARAM_CLEAR = "PARAM_CLEAR";
	}
	
	/**
	 * 在进行标签的盘存操作的同时可以读某个分区的数据
	 */
	public final static class TAG_EMBEDEDDATA{
		public final static String KEY = "TAG_EMBEDEDDATA";
		 public final static String PARAM_TAG_EMBEDEDDATA = "PARAM_TAG_EMBEDEDDATA";
	    
	    /**移除附加设置项*/
	    public final static String PARAM_CLEAR = "PARAM_CLEAR";
	}
	
	/**
	 * 设置盘存操作的协议（仅仅M6e架构的读写器支持的参数）
	 */
	public final static class TAG_INVPOTL{
		public final static String KEY = "TAG_INVPOTL";

		/**盘存操作的协议*/
	    public final static String PARAM_TAG_INVPOTL = "PARAM_TAG_INVPOTL";
	}
	
	/**
	 * 所有被检测到的天线（不是所有的天线都能被检测）(只读)
	 */
	public final static class READER_CONN_ANTS{
		public final static String KEY = "READER_CONN_ANTS";
		/**所有被检测到的天线*/
		public final static String PARAM_CONNECT_ANTS = "PARAM_CONNECT_ANTS";
	}
	
	/**
	 * 返回读写器的天线端口数(只读)
	 */
	public final static class READER_AVAILABLE_ANTPORTS{
		public final static String KEY = "READER_AVAILABLE_ANTPORTS";
		/**读写器的天线端口数(*/
		public final static String PARAM_READER_AVAILABLE_ANTPORTS = "PARAM_READER_AVAILABLE_ANTPORTS";
	}
	
	/**
	 * 发射功率前要检测天线是否连接
	 */
	public final static class READER_IS_CHK_ANT{
		public final static String KEY = "READER_IS_CHK_ANT";
		/**发射功率前要检测天线是否连接*/
		public final static String PARAM_READER_IS_CHK_ANT = "PARAM_READER_IS_CHK_ANT";
	}
	
	/**
	 * 读写器ip地址
	 */
	public final static class READER_IP{
	    
		public final static String KEY = "READER_IP";
		
		public final static String PARAM_READER_IP = "PARAM_READER_IP";
	}
	
	/**
	 * 读写器工作区域
	 */
	public final static class FREQUENCY_REGION{
		public final static String KEY = "FREQUENCY_REGION";
		/**读写器工作区域*/
		public final static String PARAM_FREQUENCY_REGION = "PARAM_FREQUENCY_REGION";
	}
	
	/**
	 * 读写器跳频表设置
	 */
	public final static class FREQUENCY_HOPTABLE{
		public final static String KEY = "FREQUENCY_HOPTABLE";
		/**用于存储频点*/
	    public final static String PARAM_HTB = "PARAM_HTB";
	    
	}
	
	/**
	 * Gen2协议后向链路速率
	 */
	public final static class POTL_GEN2_BLF{
		public final static String KEY = "POTL_GEN2_BLF";
		/**Gen2协议后向链路速率*/
	    public final static String PARAM_POTL_GEN2_BLF = "PARAM_POTL_GEN2_BLF";
	}
	
	/**
	 * Gen2协议写模式
	 */
	public final static class POTL_GEN2_WRITEMODE{
		public final static String KEY = "POTL_GEN2_WRITEMODE";
		/**Gen2协议写模式*/
	    public final static String PARAM_POTL_GEN2_WRITEMODE = "PARAM_POTL_GEN2_WRITEMODE";
	}
	
	/**
	 * Gen2协议目标
	 */
	public final static class POTL_GEN2_TARGET{
		public final static String KEY = "POTL_GEN2_TARGET";
		/**用于存储频点*/
	    public final static String PARAM_POTL_GEN2_TARGET = "PARAM_POTL_GEN2_TARGET";
	}
	
	/**
	 * 对于同一个标签，如果被不同的天线读到是否将做为多条标签数据<p>
	 * 0表示同一个标签不论都多少个天线读到都作为一条标签数据；<p>
	 * 1表示同一个标签被不同的天线读到将作为多条标签数据<p>
	 */
	public final static class TAGDATA_UNIQUEBYANT{
		public final static String KEY = "TAGDATA_UNIQUEBYANT";
		/**不同的天线读到是否将做为多条标签数据*/
	    public final static String PARAM_TAGDATA_UNIQUEBYANT = "PARAM_TAGDATA_UNIQUEBYANT";
	}
	
	/**
	 * Epc相同的标签如果在使用嵌入盘存读功能时，读出的其它bank数据不同，是否作为多条标签数据
	 */
	public final static class TAGDATA_UNIQUEBYEMDDATA{
		public final static String KEY = "TAGDATA_UNIQUEBYEMDDATA";
		/**Epc相同的标签如果在使用嵌入盘存读功能时，读出的其它bank数据不同，是否作为多条标签数据*/
	    public final static String PARAM_TAGDATA_UNIQUEBYEMDDATA = "PARAM_TAGDATA_UNIQUEBYEMDDATA";
	}
	
	/**
	 * 是否只记录最大rssi
	 */
	public final static class TAGDATA_RECORDHIGHESTRSSI{
		public final static String KEY = "TAGDATA_RECORDHIGHESTRSSI";
		/**是否只记录最大rssi*/
	    public final static String PARAM_TAGDATA_RECORDHIGHESTRSSI = "PARAM_TAGDATA_RECORDHIGHESTRSSI";
	}
	
	/**
	 * 跳频时间
	 */
	public final static class RF_HOPTIME{
		public final static String KEY = "RF_HOPTIME";
		/**跳频时间*/
	    public final static String PARAM_RF_HOPTIME = "PARAM_RF_HOPTIME";
	}
	
	/**
	 * 是否启用lbt
	 */
	public final static class RF_LBT_ENABLE{
		public final static String KEY = "RF_LBT_ENABLE";
		/**是否启用lbt*/
	    public final static String PARAM_RF_LBT_ENABLE = "PARAM_RF_LBT_ENABLE";
	}
	
	/**
	 * 180006b协议后向链路速率
	 */
	public final static class POTL_ISO180006B_BLF{
		public final static String KEY = "POTL_ISO180006B_BLF";
		/**180006b协议后向链路速率*/
	    public final static String PARAM_POTL_ISO180006B_BLF = "PARAM_POTL_ISO180006B_BLF";
	}
	
	/**
	 * Gen2协议Tari
	 */
	public final static class POTL_GEN2_TARI{
		public final static String KEY = "POTL_GEN2_TARI";
		/**Gen2协议Tari*/
	    public final static String PARAM_POTL_GEN2_TARI = "PARAM_POTL_GEN2_TARI";
	}
	
	/**天线数据*/
    public final static class ANTS {
    	public final static String KEY = "ANTS";
		/**天线集合*/
	    public final static String PARAM_ANTS_GROUP= "PARAM_ANTS_GROUP";
	    /**操作的天线*/
	    public final static String PARAM_OPERATE_ANTS = "PARAM_OPERATE_ANTS";
	    /**支持的天线最大数(只读)*/
	    public final static String PARAM_MAX_ANTS_COUNT = "PARAM_MAX_ANTS_COUNT";
    }
    
    /**读写器省电模式*/
    public final static class POWERSAVE_MODE {
    	public final static String KEY = "POWERSAVE_MODE";
    	/**省电级别为0-3,数字越大表示越省电，0为完全不省电*/
    	public final static String PARAM_POWERSAVE_MODE = "PARAM_POWERSAVE_MODE";
    }
    
    /**盘点超时时间ms*/
    public final static class INV_TIME_OUT {
    	public final static String KEY = "INV_TIME_OUT";
		/**盘点超时时间*/
	    public final static String PARAM_INV_TIME_OUT = "PARAM_INV_TIME_OUT";
	    /**默认值盘点超时时间ms*/
	    public final static long DEFAULT_INV_TIMEOUT = 50L;
    }
	
    /**盘点间隔时间*/
    public final static class INV_INTERVAL{
    	public final static String KEY = "INV_INTERVAL";
    	/**盘点间隔时间*/
	    public final static String PARAM_INV_INTERVAL_TIME = "PARAM_INV_INTERVAL_TIME";
	    /**默认值盘点间隔时间ms*/
	    public final static long DEFAULT_INV_INTERVAL_TIME = 0L;
    }
    
    /**快速模式*/
    public final static class INV_QUICK_MODE{
    	
    	public final static String KEY = "INV_QUICK_MODE";
    	/**快速模式 1:开启,0:关闭*/
	    public final static String PARAM_INV_QUICK_MODE = "PARAM_INV_QUICK_MODE";
    }
    
    /**温度*/
    public final static class TEMPTURE{
    	
    	public final static String KEY = "TEMPTURE";
    	/**温度*/
	    public final static String PARAM_TEMPTURE = "PARAM_TEMPTURE";
    }
    
    /**
     * 低电量时,读功率设置参数
     */
    public final static class LOWER_POWER{
    	
    	public final static String KEY = "LOWER_POWER";
    	/**是否开启低电量时,功率自动调节*/
    	public final static String PARAM_LOWER_POWER_DM_ENABLE = "PARAM_LOWER_POWER_DM_ENABLE";
    	/**低电量标准参数*/
    	public final static String PARAM_LOWER_POWER_LEVEL = "PARAM_LOWER_POWER_LEVEL";
    	/**读功率设置参数*/
	    public final static String PARAM_LOWER_POWER_READ_DBM = "PARAM_LOWER_POWER_DBM";
    }
}
