package correlations;

import java.util.*;

public class Info 
{
	public int count=0;
	
	public static int TOTAL_FLAGS=7;
	public static int POSITIVE=0;
	public static int NEGATIVE=1;
	public static int CANDIDATE_POSITIVE=2;
	public static int CANDIDATE_NEGATIVE=3;
	public static int HAS_FLIPPING_CHILDREN=4;
	public static int KULC_ZERO=5;
	public static int  BREAKS_THE_FLIPS=6;
	public double kulc;
	
	private BitSet flags=new BitSet(TOTAL_FLAGS);
	
	public String itemsKeys;
	
	public Info(String key)
	{
		itemsKeys=key;
	}
	
	public boolean is(int what)
	{
		return flags.get(what);
	}
	
	public void setFlag(int what, boolean value)
	{
		flags.set(what,value);
	}
	
	public String toString()
	{
		return this.count+":"+this.kulc;
	}	
}
