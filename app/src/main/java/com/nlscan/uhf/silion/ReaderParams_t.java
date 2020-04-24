package com.nlscan.uhf.silion;

import java.util.ArrayList;
import java.util.List;

/**
 * 参数类
 */
public class ReaderParams_t
{

	//save param
	public int opant;
	/**天线口数*/
	public int antportc;
	
	public List<String> invpro;
	public String opro;
	public int[] uants;
	public int readtime;
	public int sleep;

	public int checkant;
	public int[] rpow;
	public int[] wpow;
	
	public int region;
	public int[] frecys;
	public int frelen;
	
	public int session;
	public int qv;
	public int wmode;
	public int blf;
	public int maxlen;
	public int target;
	public int gen2code;
	public int gen2tari;
	
	public String fildata;
	public int filadr;
	public int filbank;
	public int filisinver;
	public int filenable;
	
	public int emdadr;
	public int emdbytec;
	public int emdbank;
	public int emdenable;
	
	public int antq;
	public int adataq;
	public int rhssi;
	public int invw;
	public int iso6bdeep;
	public int iso6bdel;
	public int iso6bblf;
	public int option;
	//other params
	
	public String password;
	public int optime;
	
	private static ReaderParams_t mInstance; 
	public static ReaderParams_t getInstance()
	{
		if(mInstance == null)
			mInstance = new ReaderParams_t();
		
		return mInstance;
	}
	
	private ReaderParams_t()
	{
		opant=1;
		antportc = 1;
		invpro=new ArrayList<String>();
		invpro.add("GEN2");
		uants=new int[1];
		uants[0]=1;
		sleep=0;
		readtime=50;
		optime=1000;
		opro="GEN2";
		checkant=1;
		rpow=new int[]{2700,2000,2000,2000};
		wpow=new int[]{2000,2000,2000,2000};
		region=1;
		frelen=0; 
		session=0; 
		qv=-1; 
		wmode=0; 
		blf=0; 
		maxlen=0; 
		target=0; 
		gen2code=2; 
		gen2tari=0; 
 
		fildata="";
		filadr=32;
		filbank=1;
		filisinver=0;
		filenable=0; 
		
		emdadr=0;
		emdbytec=0; 
		emdbank=1;
		emdenable=0; 
		 
		adataq=0; 
		rhssi=1; 
		invw=0; 
		iso6bdeep=0; 
		iso6bdel=0; 
		iso6bblf=0;
		option=0;
	}
}
