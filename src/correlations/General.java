package correlations;
/**
 * Parsing chains of hierarchical product keys
 */
import java.util.*;

public class General 
{
	public static String CATEGORIES_PATH_SEPARATOR="^";
	public static String DIFFERENT_CATEGORIES_SEPARATOR=";";
	public static String COMMA_SEPARATOR=",";
	public static boolean debug=false;
	
	public static boolean prefixEquals(String [] parts1, String [] parts2)	{
		for(int i=0;i<parts1.length-1;i++)
		{
			if(!parts1[i].equals(parts2[i]))
				return false;
		}
		return true;
	}	
	
	public static boolean allFromDifferentCategories(String combinedkey){
		String [] keys=General.splitCombinedKey(combinedkey, General.DIFFERENT_CATEGORIES_SEPARATOR);
		for(int i=0;i<keys.length-1;i++){
			for(int j=i+1;j<keys.length;j++){
				String [] categories1=General.splitCombinedKey(keys[i],General.CATEGORIES_PATH_SEPARATOR);
				String [] categories2=General.splitCombinedKey(keys[j],General.CATEGORIES_PATH_SEPARATOR);
				
				if(categories1[0].equals(categories2[0]))
					return false;
			}
		}
		return true;
	}
	
	public static boolean fromTwoDifferentCategories(String key1,String key2) {
		String [] keys1=General.splitCombinedKey(key1, General.CATEGORIES_PATH_SEPARATOR);
		String [] keys2=General.splitCombinedKey(key2, General.CATEGORIES_PATH_SEPARATOR);
		
		if(keys1[0].equals(keys2[0]))
			return false;
		return true;
	}
	
	public static boolean hasSupersets( String parentkey , List <Cell> parentCellsRow,int startFrom )	{	
		for(int k=startFrom;k<parentCellsRow.size();k++) {
			if(General.partOfSomeKey(parentCellsRow.get(k).candidates,parentkey))
				return true;
		}
		return false;
	}
	
	private static boolean partOfSomeKey(Map <String,Info> candidates,String subsetKey) {
		Iterator <String> it=candidates.keySet().iterator();
		
		while(it.hasNext() ) {
			String key=it.next();
			if(key.indexOf(subsetKey)!=-1 && !candidates.get(key).is(Info.BREAKS_THE_FLIPS))
				return true;
		}
		return false;
	}
	
	public static String [] splitCombinedKeyKMinusOneAndOne(String key, String delimiter)	{
		String [] ret=new String[2];
		
		int lastoccurrence=key.lastIndexOf(delimiter);
		if(lastoccurrence==-1)
			return null;
		
		ret[0]=key.substring(0,lastoccurrence);
		ret[1]=key.substring(lastoccurrence+1);
		return ret;
	}
	
	public static List <String> splitCombinedKeyIntoSortedList(String key, String delimiter) {
		List <String> ret=new ArrayList <String>();
		int len=key.length();
		StringBuffer currKey=new StringBuffer("");
		char delChar=delimiter.charAt(0);
		for(int i=0;i<len;i++)	{
			if(key.charAt(i)==delChar)	{
				ret.add(currKey.toString());
				currKey=new StringBuffer("");
			}
			else
				currKey.append(key.charAt(i));
		}
		ret.add(currKey.toString());
		Collections.sort(ret);
		return ret;
	}	

	public static String [] splitCombinedKey(String key, String delimiter)	{
		String [] ret;
		int len=key.length();
		int delimCount=1;
		char del=delimiter.charAt(0);
		char [] keychars=key.toCharArray();
		
		for(int i=0;i<len;i++)	{
			if(keychars[i]==del)
				delimCount++;
		}
		
		ret=new String [delimCount];
		
		int index=0;
		StringBuffer sb=new StringBuffer("");
		for(int i=0;i<len;i++)	{
			if(keychars[i]==del) {
				ret[index]=sb.toString();
				index++;
				sb=new StringBuffer("");
			}
			else
				sb.append(keychars[i]);
		}
		ret[index]=sb.toString();
		return ret;
	}	

	public static String appendUpToKKeys(List <String> singleKeys, int startPos, int numItems)	{
		if(singleKeys.size()-startPos+1<numItems)
			return "";
		StringBuffer ret=new StringBuffer(singleKeys.get(startPos));
		ret.append(appendUpToKKeys(singleKeys,startPos+1,numItems));
		return ret.toString();
	}
	
	public static String getParentCategoryOfSingleKey(String catString)	{
		
		int len=catString.length();
		
		for(int i=len-1;i>=0;i--)	{
			if(catString.charAt(i)==General.CATEGORIES_PATH_SEPARATOR.charAt(0))
				return catString.substring(0, i);
		}
		return null;
	}
	
	public static String getParentCategory(String combinedKey)	{
		String [] singleKeys=splitCombinedKey(combinedKey, DIFFERENT_CATEGORIES_SEPARATOR);
		StringBuffer sb=new StringBuffer(getParentCategoryOfSingleKey(singleKeys[0]));
		for(int i=1;i<singleKeys.length;i++) {
			sb.append(DIFFERENT_CATEGORIES_SEPARATOR).append( getParentCategoryOfSingleKey(singleKeys[i]));
		}
		return sb.toString();
	}
	
	public static String getTopParentCategory(String catString)	{		
		int len=catString.length();
		
		for(int i=0;i<len;i++)	{
			if(catString.charAt(i)==General.CATEGORIES_PATH_SEPARATOR.charAt(0))
				return catString.substring(0, i);
		}
		return null;
	}
	
	public static String getBottomCategoryOfSingleKey(String key)	{
		int len=key.length();
		
		for(int i=len-1;i>=0;i--)	{
			if(key.charAt(i)==General.CATEGORIES_PATH_SEPARATOR.charAt(0))
				return key.substring(i+1);
		}
		return key;
	}
	
	public static String getBottomChildCategoryForPrint (String catString, double kulc, int count) {
		java.text.DecimalFormat df = new java.text.DecimalFormat("#.##");
		String [] singleKeys=splitCombinedKey(catString, DIFFERENT_CATEGORIES_SEPARATOR);
		String bottomOfSinglekey=getBottomCategoryOfSingleKey(singleKeys[0]);
		StringBuffer sb=new StringBuffer(bottomOfSinglekey.toUpperCase());
		for(int i=1;i<singleKeys.length;i++) {
			bottomOfSinglekey= getBottomCategoryOfSingleKey(singleKeys[i]);
			sb.append(" together with ").append(bottomOfSinglekey.toUpperCase());
		}
		sb.append("[kulc=").append(df.format(kulc)).append("] [count=").append(count).append("] ");
		return sb.toString();
	}
	
	public static String getIndent(int n) {
		String ret="";
		for(int i=0;i<n;i++)
			ret+="  ";		
		return ret;
	}
	
	public static String getCategoryString(Map <String,List<String>>dictionary, String key, int level)	{
		StringBuffer ret=new StringBuffer("");
		for(int i=0;i<=level;i++)	{
			List <String> categories=dictionary.get(key);
			String currcategory= categories.get(i);
			if(currcategory!=null)	{
				if(i==0)
					ret.append(currcategory);
				else
					ret.append(General.CATEGORIES_PATH_SEPARATOR).append(currcategory);
			}
			else
				return null;
				
		}
		return	ret.toString();
	}
	
	public static String combineKey(String [] subset, String delimiter)	{
		StringBuffer sb=new StringBuffer("");
		int i=0;
		for(;i<subset.length-1;i++)
			sb.append(subset[i]).append(delimiter);
		sb.append(subset[i]);
		return sb.toString();
	}
}
