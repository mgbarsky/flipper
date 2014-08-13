package correlations;
/**
 * Keeps track for all the parameters of the program and its current state
 */
import java.io.*;
import java.util.*;

public class State {
	private List <String> keyCombinations;
	private List <String> keyCombinations1;
	public String transactionDelimiter;
	
	public int [] minSupps;
	public int hierarchyDepth;
	
	public List  <Integer> maxTransactionWidthPerLevel=new ArrayList <Integer>();
	public Map <String,List<String>> hierarchyByItemID=new HashMap <String,List<String>>();
	
	public double pos;
	public double neg;
	
	private double [] freqs;
	
	private String transactionsFileName;
	
	public List <Cell> twoItemsets=new ArrayList <Cell> ();
	public List <Cell> oneItemsets=new ArrayList <Cell> ();
	
	
	public State(String tDelimiter,
			double posThreshold, double negThreshold)	{
		transactionDelimiter=tDelimiter;		
		pos=posThreshold;
		neg=negThreshold;
	}
	
	public boolean setInputFiles(String folderName,
			String transFileName, String dictFileName, 
			String dictionaryDelimiter, String supportsFileName)	{
		//category dictionaries
		int minHierarchy=0;
		try		{
		    FileInputStream dfstream = new FileInputStream(folderName+System.getProperty("file.separator")
					+dictFileName);
		    DataInputStream ddis = new DataInputStream(dfstream);
		    BufferedReader dictin= new BufferedReader(new InputStreamReader(ddis));
		
		    String dictLine;
		    while((dictLine=dictin.readLine())!=null)    {
		    	StringTokenizer st=new StringTokenizer(dictLine,dictionaryDelimiter);
		    	String prodKey=st.nextToken();
		    	
		    	List <String> categories=new ArrayList<String>();
		    	
		    	int levelCount=0;
		    	while(st.hasMoreTokens())  	{
		    		categories.add(st.nextToken());
		    		levelCount++;
		    	}
		    	if(minHierarchy==0 || levelCount<minHierarchy)
		    		minHierarchy=levelCount;
		    	hierarchyByItemID.put(prodKey, categories);
		    }
		    dictin.close();
		
		    hierarchyDepth=minHierarchy;
		}
		catch (IOException e)		{
			System.out.println("Error opening dictionary file "+dictFileName+". " + e.getMessage());
			return false;
		}
	
		try		{
			FileInputStream supfstream = new FileInputStream(folderName+System.getProperty("file.separator")
					+supportsFileName);
		    DataInputStream supdis = new DataInputStream(supfstream);
		    BufferedReader supin= new BufferedReader(new InputStreamReader(supdis));
		    String supLine;
		    if((supLine=supin.readLine())!=null)		    {
		    	freqs=new double[hierarchyDepth];
		    	StringTokenizer st=new StringTokenizer(supLine,General.COMMA_SEPARATOR);
		    	if(st.countTokens()!=hierarchyDepth)		    	{
		    		System.out.println("Please provide minimum support for each hierarchy level (total "+hierarchyDepth+" levels.)");
			    	return false;
		    	}
		    	for(int f=0;f<hierarchyDepth;f++)		    	{
		    		freqs[f]=Double.parseDouble(st.nextToken());
		    	}
		    }
		    else   {
		    	System.out.println("Please provide minimum support for each hierarchy level (total "+hierarchyDepth+" levels.)");
		    	return false;
		    }
		    supin.close();
		}
	    catch (IOException e)	{
			System.out.println("Error opening min supports file "+supportsFileName+". " + e.getMessage());
			return false;
		}
	
	    for(int i=0;i<hierarchyDepth;i++)	{
			maxTransactionWidthPerLevel.add(new Integer(0));
		}
	   
	    transactionsFileName=folderName+System.getProperty("file.separator")+transFileName;
		int totalTransactions=0;
		try		{
			FileInputStream tfstream = new FileInputStream(folderName+System.getProperty("file.separator")+transFileName);
			
		    DataInputStream tis = new DataInputStream(tfstream);
		    BufferedReader in= new BufferedReader(new InputStreamReader(tis));
		    
		    String transLine;
		    while((transLine=in.readLine())!=null)    {
		    	StringTokenizer st=new StringTokenizer(transLine,transactionDelimiter);
		    	List <Map<String,Object>>differentItems=new ArrayList <Map<String,Object>>();
		    	for(int i=0;i<hierarchyDepth;i++)	{
		    		differentItems.add(new HashMap <String,Object>());
				}			    	
		    	
		    	while(st.hasMoreTokens())    	{
		    		String itemID=st.nextToken();
		    		for(int i=0;i<hierarchyDepth;i++)  		{
		    			String catString=General.getCategoryString(hierarchyByItemID, itemID, i);
		    			differentItems.get(i).put(catString, null);
		    		}
		    	}
		    	
		    	for(int i=0;i<hierarchyDepth;i++)    	{
		    		if(differentItems.get(i).size()>maxTransactionWidthPerLevel.get(i))
		    			maxTransactionWidthPerLevel.set(i, differentItems.get(i).size());
		    	}
		    	totalTransactions++;
		    }
		
		    in.close();
		}
		catch (IOException e)	{
			System.out.println("Error opening transactions file "+transFileName+". " + e.getMessage());
			return false;
		}
		
		minSupps=new int [hierarchyDepth];
		for(int f=0;f<hierarchyDepth;f++)    	{
			minSupps[f]=(int)(freqs[f]*totalTransactions);
    	}
		
		
		for(int h=0;h<hierarchyDepth;h++)
			oneItemsets.add(new Cell(h, 1, this,1));
		for(int h=0;h<hierarchyDepth;h++)
			twoItemsets.add(new Cell(h, 2, this));
		
		System.out.println("dataset info: ");
		System.out.println("DATASET "+transFileName+" with categories "+dictFileName+" contains:");
		System.out.println(totalTransactions+" total transactions.");
		for(int t=0;t<hierarchyDepth;t++)
			System.out.println(maxTransactionWidthPerLevel.get(t)+" max items  per transaction in level "+t);
		System.out.println(hierarchyDepth+" depth of category hierarchies.");
		for(int s=0;s<hierarchyDepth;s++)
			System.out.println("MIN SUPPORT COUNT for level "+s+" ="+minSupps[s]);
		return true;
	}
	
	private double getAverageKulcFromTwoItemsets(String combinedKey, int currHierarchy)	{
		double sum=0;
		String [] keys=General.splitCombinedKey(combinedKey,
				General.DIFFERENT_CATEGORIES_SEPARATOR);
		int totalKeys=keys.length;
		int totalTwoItemsets=0;
		
		Cell twoCell=twoItemsets.get(currHierarchy);
		for(int i=0;i<totalKeys-1;i++)		{
			String key1=keys[i];
			for(int j=i+1;j<totalKeys;j++)	{
				String key2=keys[j];
				String twoKeys=key1+General.DIFFERENT_CATEGORIES_SEPARATOR+key2;
				if(!twoCell.candidates.containsKey(twoKeys))	{
					System.out.println("Logical error. No key combination "
							+twoKeys +" in candidates for hierarchy "+currHierarchy);
					System.exit(1);
				}
				sum+=twoCell.candidates.get(twoKeys).kulc;
				totalTwoItemsets++;
			}
		}
		return sum/totalTwoItemsets;
	}
	
	public double maxMin(String combinedKey,int h)	{
		List <String> allKeysSorted=General.splitCombinedKeyIntoSortedList(combinedKey,
				General.DIFFERENT_CATEGORIES_SEPARATOR);
		if(allKeysSorted.size()<=2 )
			return pos;
		keyCombinations1=new ArrayList<String>();
		generateAllDifferentSubsets1(allKeysSorted, 2);
		
		//min of 1 -itemset
		Map <String,Info> ones=oneItemsets.get(h).candidates;
		int minOne=ones.get(allKeysSorted.get(0)).count;
		for(int i=1;i<allKeysSorted.size();i++)	{
			int next=ones.get(allKeysSorted.get(i)).count;
			if(next<minOne)
				minOne=next;
		}
		
		Map <String,Info> twos=twoItemsets.get(h).candidates;
		int maxTwo=twos.get(keyCombinations1.get(0)).count;
		for(int i=1;i<keyCombinations1.size();i++)	{
			int next=twos.get(keyCombinations1.get(i)).count;
			if(next>maxTwo)
				maxTwo=next;
		}
		return ((double)maxTwo)/((double)minOne);
	}
	
	public double getAverageKulc(String combinedKey, Cell leftCell)	{
		double sum=0;
		List <String> allKeysSorted=General.splitCombinedKeyIntoSortedList(combinedKey,
				General.DIFFERENT_CATEGORIES_SEPARATOR);
		if(allKeysSorted.size()<=2 || leftCell==null)
			return pos;
		keyCombinations1=new ArrayList<String>();
		generateAllDifferentSubsets1(allKeysSorted, allKeysSorted.size()-1);
		
		
		for(int i=0;i<keyCombinations1.size();i++)		{
			if(leftCell.candidates.containsKey(keyCombinations1.get(i)))
				sum+=leftCell.candidates.get(keyCombinations1.get(i)).kulc;
			else
				sum+=getAverageKulcFromTwoItemsets(keyCombinations1.get(i),leftCell.hierarchyLevel);
		}
		return sum/keyCombinations.size();
	}
	
	public boolean candidateBreaksThePath( String parentkey, double averageKulc, Cell parentCell)
	{
		if(averageKulc<this.pos)	{
			if(parentCell.candidates.get(parentkey).is(Info.NEGATIVE))
				return true;
		}
		return false;
	}	
	
	private boolean isValidCombination( Cell leftCell,String combinedKey)	{
		if(!General.allFromDifferentCategories(combinedKey))
			return false;
		List <String>sortedKeys=General.splitCombinedKeyIntoSortedList(combinedKey, General.DIFFERENT_CATEGORIES_SEPARATOR);
		
		keyCombinations1=new ArrayList <String>();
		generateAllDifferentSubsets1(sortedKeys, leftCell.numberOfItems);
		for(int i=0;i<keyCombinations1.size();i++)	{
			String k_minus_1_subset=keyCombinations1.get(i);
			if(!leftCell.candidates.containsKey(k_minus_1_subset))
				return false;
		}
		return true;
	}
	
	public boolean countCandidatesForCeiling (Cell cell, Cell parentCell,
			Cell leftCell, Cell leftParentCell)	{
		Cell oneCell = oneItemsets.get(cell.hierarchyLevel);
		Cell oneParentCell = oneItemsets.get(cell.hierarchyLevel);
		
		try		{
			FileInputStream tfstream = new FileInputStream(transactionsFileName);
		    DataInputStream tis = new DataInputStream(tfstream);
		    BufferedReader in= new BufferedReader(new InputStreamReader(tis));
		
			String transLine;
			
			while((transLine=in.readLine())!=null)	{
				StringTokenizer st=new StringTokenizer(transLine, transactionDelimiter);					
				
				Map <String, Object> transmap1=new HashMap <String, Object>();
				Map <String, Object> transmap2=new HashMap <String, Object>();
				while(st.hasMoreTokens())	{
					String itemKey=st.nextToken();
					String itemCat1=General.getCategoryString(hierarchyByItemID,
							itemKey,cell.hierarchyLevel);	
					
					if(oneCell.candsFromBottomPruning.contains(itemCat1))
							transmap1.put(itemCat1, null);											
					
					String itemCat2=General.getCategoryString(hierarchyByItemID,
								itemKey,parentCell.hierarchyLevel);	
					
					if(oneParentCell.candsFromBottomPruning.contains(itemCat2))
							transmap2.put(itemCat2, null);							
				}
				
				List <String>sortedIDs1=new ArrayList<String>();
				List <String>sortedIDs2=new ArrayList<String>();
				
				Iterator <String> it1=transmap1.keySet().iterator();
				while(it1.hasNext())	{
					String key=it1.next();
					sortedIDs1.add(key);
				}
				Collections.sort(sortedIDs1);
				
				Iterator <String> it2=transmap2.keySet().iterator();
				while(it2.hasNext())	{
					String key=it2.next();
					sortedIDs2.add(key);
				}
				Collections.sort(sortedIDs2);				
				
				//generate all k-length sorted keys for child cell and add count for each of them				
				keyCombinations=new ArrayList <String>();
				generateAllDifferentSubsets(sortedIDs2, parentCell.numberOfItems);
				for(int i=0;i<keyCombinations.size();i++)	{
					String combinedKey=keyCombinations.get(i);
					
					if(isValidCombination(leftParentCell,combinedKey))	{
						if(!parentCell.candidates.containsKey(combinedKey))
							parentCell.candidates.put(combinedKey, new Info(combinedKey));						
						(parentCell.candidates.get(combinedKey).count)++;
					}
				}
				
				keyCombinations=new ArrayList <String>();
				generateAllDifferentSubsets(sortedIDs1, cell.numberOfItems);
				for(int i=0;i<keyCombinations.size();i++)	{
					String combinedKey=keyCombinations.get(i);
					
					if(isValidCombination(leftCell,combinedKey))	{
						if(!cell.candidates.containsKey(combinedKey))
							cell.candidates.put(combinedKey, new Info(combinedKey));						
						(cell.candidates.get(combinedKey).count)++;
					}
				}
			}
			
			in.close();
		}
		catch (IOException e)	{
			System.out.println("Error reading transactions file: " + e.getMessage());
			return false;
		}
		
		//after counting we can remove all where count is 0 and where count < minSupp
		//they cannot be extended to the left or downwards	
		List <String> removeNonFrequentParent=new ArrayList <String>();
		
		//compute kulc for parent cell		
		Iterator <String> it=parentCell.candidates.keySet().iterator();
		Map <String,Info> oneCandidates=oneItemsets.get(parentCell.hierarchyLevel).candidates;
		while(it.hasNext())		{
			String key=it.next();
			Info info=parentCell.candidates.get(key);
			
			if(info.count>=this.minSupps[parentCell.hierarchyLevel])	{
				String [] allDifferentKeys=General.splitCombinedKey(key,
						General.DIFFERENT_CATEGORIES_SEPARATOR);
				
				for(int p=0;p<allDifferentKeys.length;p++)	{
					String singleKey=allDifferentKeys[p];					
					info.kulc+=(double)info.count/(double)oneCandidates.get(singleKey).count;
				}
				info.kulc=info.kulc/allDifferentKeys.length;				
				
				if (info.kulc>=this.pos)	{
					info.setFlag(Info.POSITIVE, true);
				} else if(info.kulc<=neg)	{
					info.setFlag(Info.NEGATIVE, true);
				}
				else	{
					info.setFlag(Info.BREAKS_THE_FLIPS, true);
				}
				parentCell.candidates.put(key, info);
			}
			else	{
				removeNonFrequentParent.add(key);
			}
		}
		
		//compute kulc in child cell and set all the flags
		it=cell.candidates.keySet().iterator();
		oneCandidates=oneItemsets.get(cell.hierarchyLevel).candidates;
		List <String> removeNonFrequent=new ArrayList <String>();
		Map <String,Object> allParentsWithFrequentChildren=new HashMap <String,Object>();
		while(it.hasNext())	{
			String key=it.next();			
			String parentKey=General.getParentCategory(key);
			
			Info info=cell.candidates.get(key);			
			if(info.count>=this.minSupps[cell.hierarchyLevel])	{
				String [] allDifferentKeys=General.splitCombinedKey(key,
						General.DIFFERENT_CATEGORIES_SEPARATOR);
				
				for(int p=0;p<allDifferentKeys.length;p++)	{
					String singleKey=allDifferentKeys[p];
					info.kulc+=(double)info.count/(double)oneCandidates.get(singleKey).count;
				}
				info.kulc=info.kulc/allDifferentKeys.length;
				
				if (info.kulc>=this.pos) {
					info.setFlag(Info.POSITIVE, true);	
				}
				
				else if(info.kulc<=neg)	{				
					info.setFlag(Info.NEGATIVE, true);	
				}
				else	{
					info.setFlag(Info.BREAKS_THE_FLIPS, true);	
				}
				cell.candidates.put(key, info);
				allParentsWithFrequentChildren.put(parentKey, null);	
			}
			else
				removeNonFrequent.add(key);
		}		
		
		Iterator <String> parit=parentCell.candidates.keySet().iterator();
		while(parit.hasNext())	{
			String currParkey=parit.next();
			if( !allParentsWithFrequentChildren.containsKey(currParkey))
				removeNonFrequentParent.add(currParkey);
		}		
		
		for(int i=0;i<removeNonFrequentParent.size();i++)	{
			parentCell.candidates.remove(removeNonFrequentParent.get(i));
		}
		
		Iterator <String> iter=cell.candidates.keySet().iterator();
		while(iter.hasNext())	{
			String currKey=iter.next();
			String parCategory=General.getParentCategory(currKey);
			if( !parentCell.candidates.containsKey(parCategory))
				removeNonFrequent.add(currKey);
		}
		
		for(int i=0;i<removeNonFrequent.size();i++)	{
			cell.candidates.remove(removeNonFrequent.get(i));
		}
		return true;
	}
	
	//Just for FREQUENCY_PRUNING
	public boolean countCandidates (Cell cell,  Cell leftCell, Cell parentCell)
	{
		try	{
			FileInputStream tfstream = new FileInputStream(transactionsFileName);
		    DataInputStream tis = new DataInputStream(tfstream);
		    BufferedReader in= new BufferedReader(new InputStreamReader(tis));
		
			String transLine;			
			
			while((transLine=in.readLine())!=null)		{
				StringTokenizer st=new StringTokenizer(transLine, transactionDelimiter);		
				
				Map <String, Object> transmap=new HashMap <String, Object>();
				
				while(st.hasMoreTokens())	{
					String itemKey=st.nextToken();
					String itemCat=General.getCategoryString(hierarchyByItemID,
							itemKey,cell.hierarchyLevel);
					
					transmap.put(itemCat, null);
				}
				
				List <String>sortedIDs=new ArrayList<String>();
				
				Iterator <String> it1=transmap.keySet().iterator();
				while(it1.hasNext())	{
					String key=it1.next();
					sortedIDs.add(key);
				}
				Collections.sort(sortedIDs);
				
				//generate all k length sorted keys for child cell and add count for each of them
				keyCombinations=new ArrayList <String>();
				generateAllDifferentSubsets(sortedIDs, cell.numberOfItems);
				for(int i=0;i<keyCombinations.size();i++)	{
					String combinedKey=keyCombinations.get(i);
					
					if(isValidCombination(leftCell, combinedKey))	{
						if(cell.candidates.containsKey(combinedKey))	{
							(cell.candidates.get(combinedKey).count)++;
						}
						else {
							Info newInfo=new Info(combinedKey);
							newInfo.count=1;
							cell.candidates.put(combinedKey, newInfo);
						}						
					}
				}
			}			
			in.close();			
		}
		catch (IOException e)	{
			System.out.println("Error reading transactions file: " + e.getMessage());
			return false;
		}
		
		//compute kulc in child cell and set all the flags
		Iterator <String> it=cell.candidates.keySet().iterator();
		Map <String,Info> oneCandidates=oneItemsets.get(cell.hierarchyLevel).candidates;
		List <String> keystoremove=new ArrayList <String>();
		
		while(it.hasNext())	{
			String key=it.next();
			String parentKey=General.getParentCategory(key);
			Info info=cell.candidates.get(key);
			
			String [] allDifferentKeys=General.splitCombinedKey(key,
					General.DIFFERENT_CATEGORIES_SEPARATOR);
			
			for(int p=0;p<allDifferentKeys.length;p++)	{
				String singleKey=allDifferentKeys[p];
				info.kulc+=(double)info.count/(double)oneCandidates.get(singleKey).count;
			}
			info.kulc=info.kulc/allDifferentKeys.length;
			
			if(info.count>=minSupps[cell.hierarchyLevel])	{
				if (info.kulc>=pos)	{
					if(parentCell!=null) {
						if(parentCell.candidates.containsKey(parentKey))	{
							if(parentCell.candidates.get(parentKey).kulc<=neg)
								info.setFlag(Info.POSITIVE, true);
							else
								info.setFlag(Info.BREAKS_THE_FLIPS, true);
						}
					} else info.setFlag(Info.BREAKS_THE_FLIPS, true);
				}
				else if(info.kulc<=neg)	{				
					if(parentCell!=null) {
						if(parentCell.candidates.containsKey(parentKey)) {
							if(parentCell.candidates.get(parentKey).kulc>=pos)
								info.setFlag(Info.NEGATIVE, true);
							else
								info.setFlag(Info.BREAKS_THE_FLIPS, true);
						}
					} else info.setFlag(Info.BREAKS_THE_FLIPS, true);
				}
				else {
					info.setFlag(Info.BREAKS_THE_FLIPS, true);
				}			
			}
			cell.candidates.put(key, info);					
		}		
		
		for(int a=0;a<keystoremove.size();a++)	{
			cell.candidates.remove(keystoremove.get(a));
		}		
		return true;
	}
	
	public boolean countCandidates(Cell cell,  Cell leftCell, List <Cell> parentRow) {
		Cell parentCell=parentRow.get(cell.numberOfItems);
		Cell oneCell = oneItemsets.get(cell.hierarchyLevel);
		
		try	{
			FileInputStream tfstream = new FileInputStream(transactionsFileName);
		    DataInputStream tis = new DataInputStream(tfstream);
		    BufferedReader in= new BufferedReader(new InputStreamReader(tis));
		
			String transLine;
			
			while((transLine=in.readLine())!=null)	{
				StringTokenizer st=new StringTokenizer(transLine, transactionDelimiter);
				
				Map <String, Object> transmap=new HashMap <String, Object>();
				
				while(st.hasMoreTokens())	{
					String itemKey=st.nextToken();
					String itemCat=General.getCategoryString(hierarchyByItemID,
							itemKey,cell.hierarchyLevel);					
					if(oneCell.candsFromBottomPruning.contains(itemCat))
						transmap.put(itemCat, null);					
				}
				
				List <String>sortedIDs=new ArrayList<String>();
				
				Iterator <String> it1=transmap.keySet().iterator();
				while(it1.hasNext())	{
					String key=it1.next();
					sortedIDs.add(key);
				}
				Collections.sort(sortedIDs);
				
				//generate all k length sorted keys for child cell and add count for each of them
				keyCombinations=new ArrayList <String>();
				generateAllDifferentSubsets(sortedIDs, cell.numberOfItems);
				for(int i=0;i<keyCombinations.size();i++)	{
					String combinedKey=keyCombinations.get(i);
					String parentCat=General.getParentCategory(combinedKey);
					
					if(isValidCombination(leftCell, combinedKey))	{						
						if(cell.candidates.containsKey(combinedKey)) {
							(cell.candidates.get(combinedKey).count)++;
						}
						else {
							Info newInfo=new Info(combinedKey);
							newInfo.count=1;
							cell.candidates.put(combinedKey, newInfo);
						}						
					}
				}
			}
			
			in.close();			
		}
		catch (IOException e) {
			System.out.println("Error reading transactions file: " + e.getMessage());
			return false;
		}
		
		//compute kulc in child cell and set all the flags
		Iterator <String> it=cell.candidates.keySet().iterator();
		Map <String,Info> oneCandidates=oneItemsets.get(cell.hierarchyLevel).candidates;
		List <String> keystoremove=new ArrayList <String>();
		
		while(it.hasNext())	{
			String key=it.next();
			String parentKey=General.getParentCategory(key);
			Info info=cell.candidates.get(key);
			
			String [] allDifferentKeys=General.splitCombinedKey(key,
					General.DIFFERENT_CATEGORIES_SEPARATOR);
			
			for(int p=0;p<allDifferentKeys.length;p++)	{
				String singleKey=allDifferentKeys[p];
				info.kulc+=(double)info.count/(double)oneCandidates.get(singleKey).count;
			}
			info.kulc=info.kulc/allDifferentKeys.length;
			
			if(info.count>=minSupps[cell.hierarchyLevel])	{
				if (info.kulc>=pos)	{
					if(parentCell.candidates.containsKey(parentKey))	{
						if(parentCell.candidates.get(parentKey).kulc<=neg)
							info.setFlag(Info.POSITIVE, true);
						else
							info.setFlag(Info.BREAKS_THE_FLIPS, true);
					}
					else						
						keystoremove.add(key);				
				}
				else if(info.kulc<=neg)
				{				
					if(parentCell.candidates.containsKey(parentKey))	{
						if(parentCell.candidates.get(parentKey).kulc>=pos)
							info.setFlag(Info.NEGATIVE, true);
						else
							info.setFlag(Info.BREAKS_THE_FLIPS, true);
					}
					else						
						keystoremove.add(key);						
				}
				else {
					info.setFlag(Info.BREAKS_THE_FLIPS, true);
				}			
				cell.candidates.put(key, info);
				if(info.is(Info.BREAKS_THE_FLIPS) && !General.hasSupersets(parentKey, parentRow, cell.numberOfItems+1))
					keystoremove.add(key);
			}
			else
				keystoremove.add(key);			
		}		
		
		for(int a=0;a<keystoremove.size();a++)	{
			cell.candidates.remove(keystoremove.get(a));
		}		
		
		return true;
	}
	
	private String getParentForKey(Info current, String endString, int h,List <Cell> columnK)
	{
		if(h<0)
			return endString;
		String parentkey=General.getParentCategory(current.itemsKeys);
		if(columnK.get(h).candidates.containsKey(parentkey))	{
			Info parentInfo=columnK.get(h).candidates.get(parentkey);
			if(parentInfo.count>=minSupps[columnK.get(h).hierarchyLevel])	{
				String sign="0";
				if(parentInfo.kulc>=pos)
					sign="(+) ";
				if(parentInfo.kulc<=neg)
					sign="(-) ";
				if(!sign.equals("0"))	{
					if((parentInfo.kulc>=pos && current.kulc<=neg)
							||
							(parentInfo.kulc<=neg && current.kulc>=pos))	{
						String currPath=General.getIndent(h)+sign+General.getBottomChildCategoryForPrint(parentkey,parentInfo.kulc,parentInfo.count)+System.getProperty("line.separator")+endString;
						return getParentForKey(parentInfo, currPath, h-1, columnK);
					}
					else
						return null;
				}
				else
					return null;
			}
			else
				return null;
		}
		
		return null;
	}
	
	public void printAllKItemsets (int k, List <List<Cell>> table)	{
		List <Cell> columnK=new LinkedList <Cell>();
		for(int i=0;i<hierarchyDepth;i++)	{
			columnK.add(table.get(i).get(k));
		}
		
		Cell bottomCell=columnK.get(hierarchyDepth-1);
		Iterator <Info> it=bottomCell.candidates.values().iterator();
		while(it.hasNext())	{
			Info currItemset=it.next();
			String sign="0";
			if(currItemset.count>=minSupps[bottomCell.hierarchyLevel])	{
				System.out.println ("Level 2: "+currItemset.itemsKeys +": " + currItemset.kulc);
				String parentKey=General.getParentCategory(currItemset.itemsKeys);
				//get corresponding itemset
				if(columnK.get(1).candidates.containsKey(parentKey))	{
					Info parentInfo=columnK.get(1).candidates.get(parentKey);
					System.out.println ("Level 1: "+parentInfo.itemsKeys +": " + parentInfo.kulc);
				}
				
				//now grandparent
				parentKey=General.getParentCategory(parentKey);
				//get corresponding itemset
				if(columnK.get(0).candidates.containsKey(parentKey))	{
					Info grandparentInfo=columnK.get(0).candidates.get(parentKey);
					System.out.println ("Level 0: "+grandparentInfo.itemsKeys +": " + grandparentInfo.kulc);
				}
			}
		}
	}
	
	public void printFlippingPathsForKItemsets(int k, List <List<Cell>> table)	{
		List <Cell> columnK=new LinkedList <Cell>();
		for(int i=0;i<hierarchyDepth;i++)	{
			columnK.add(table.get(i).get(k));
		}
		
		Cell bottomCell=columnK.get(hierarchyDepth-1);
		Iterator <Info> it=bottomCell.candidates.values().iterator();
		while(it.hasNext())	{
			Info currItemset=it.next();
			String sign="0";
			if(currItemset.count>=minSupps[bottomCell.hierarchyLevel])	{
				if(currItemset.kulc>=pos)
					sign="(+) ";
				if(currItemset.kulc<=neg)
					sign="(-) ";
				if(!sign.equals("0"))
				{
					String endString=General.getIndent(hierarchyDepth-1)+sign
						 +General.getBottomChildCategoryForPrint(currItemset.itemsKeys,currItemset.kulc,currItemset.count)+System.getProperty("line.separator");
					String path=getParentForKey(currItemset,endString,hierarchyDepth-2,columnK);
					if(path!=null)
						System.out.println(path);
				}
			}
		}
	}	

	public boolean generateAllF1()	{
		try	{
			FileInputStream tfstream = new FileInputStream(transactionsFileName);
		    DataInputStream tis = new DataInputStream(tfstream);
		    BufferedReader in= new BufferedReader(new InputStreamReader(tis));
			String transLine;
			
			while((transLine=in.readLine())!=null)	{
				StringTokenizer st=new StringTokenizer(transLine, transactionDelimiter);
				
				List <Map <String, Object>> transmaps=new LinkedList <Map <String, Object>>();
				for(int h=0;h<hierarchyDepth;h++)	{
					transmaps.add(new HashMap <String,Object>());
				}
				
				while(st.hasMoreTokens())	{
					String itemKey=st.nextToken();
					for(int h=0;h<hierarchyDepth;h++)	{
						String itemCat=General.getCategoryString(hierarchyByItemID,
								itemKey,h);
						transmaps.get(h).put(itemCat, null);
					}
				}
				
				for(int h=0;h<hierarchyDepth;h++)	{
					Iterator <String> it=transmaps.get(h).keySet().iterator();
					while(it.hasNext())		{
						String key=it.next();						
												
						if(oneItemsets.get(h).candidates.containsKey(key))	{
							(oneItemsets.get(h)).candidates.get(key).count++;
						}
						else	{
							(oneItemsets.get(h)).candidates.put(key, new Info(key));
							(oneItemsets.get(h)).candidates.get(key).count =1;
						}						
					}
				}
			}
			
			in.close();			
		}
		catch (IOException e)	{
			System.out.println("Error reading transactions file: " + e.getMessage());
			return false;
		}
		
		for(int h=0;h<hierarchyDepth;h++)	{
			Map <String,Info> currmap=oneItemsets.get(h).candidates;
			List <String>removeKeys=new LinkedList <String>();
			
			Iterator <String> it=currmap.keySet().iterator();
			while(it.hasNext())	{
				String key=it.next();
				Info info=currmap.get(key);
				if(info.count<minSupps[h])
					removeKeys.add(key);
			}
			
			for(int i=0;i<removeKeys.size();i++)
				oneItemsets.get(h).candidates.remove(removeKeys.get(i));
			
			it=oneItemsets.get(h).candidates.keySet().iterator();
			while(it.hasNext())	{
				oneItemsets.get(h).sortedKeys.add(it.next());
			}
			Collections.sort(oneItemsets.get(h).sortedKeys);
		}
		return true;		
	}
	
	public boolean pruneRowFinal( List <Cell> currentRow, List <Cell> parentRow) {
		int totalColumns=Math.min(currentRow.size(), parentRow.size());
		
		for(int c=totalColumns-1;c>2;c--)	{
			Cell cell=currentRow.get(c);
			Cell parentCell=parentRow.get(cell.numberOfItems);
			List <String> removeKeys=new ArrayList <String>();
			List <String> removeParentKeys=new ArrayList <String>();
			
			Map <String,Object> hasFlippingChildren=new HashMap <String,Object>();
			
			Iterator <String> iter=cell.candidates.keySet().iterator();
			
			while(iter.hasNext())	{
				String combinedKey=iter.next();
				String parentCat=General.getParentCategory(combinedKey);
				if(parentCell.candidates.containsKey(parentCat))	{
					if(parentCell.candidates.get(parentCat).is(Info.BREAKS_THE_FLIPS))	{
						parentCell.candidates.remove(parentCat);
					}
					
					if(parentCell.candidates.containsKey(parentCat))	{
						if(cell.candidates.get(combinedKey).is(Info.BREAKS_THE_FLIPS))	{
							removeKeys.add(combinedKey);
						}
						
						if((parentCell.candidates.get(parentCat).kulc>=pos
								&& cell.candidates.get(combinedKey).kulc<=neg) ||
								(parentCell.candidates.get(parentCat).kulc<=neg
								&& cell.candidates.get(combinedKey).kulc>=pos))	{
							hasFlippingChildren.put(parentCat, null);
						}
						else
							removeKeys.add(combinedKey);
					}
					else
						removeKeys.add(combinedKey);
				}
				else {
					removeKeys.add(combinedKey);
				}
			}
			
			iter=parentCell.candidates.keySet().iterator();
			while(iter.hasNext()) {
				String currparentcat=iter.next();
				if(!hasFlippingChildren.containsKey(currparentcat))
					removeParentKeys.add(currparentcat);
			}			
			
			for(int b=0;b<removeKeys.size();b++)
				cell.candidates.remove(removeKeys.get(b));
			
			for(int b=0;b<removeParentKeys.size();b++)
				parentCell.candidates.remove(removeParentKeys.get(b));
		}
		return true;
	}	
	
	public boolean stopCriteria(Cell cell, Cell parentCell)	{
		
		if(cell.allNonPositive() && parentCell.allNonPositive())
			return true;
				
		return false;
	}
	
	public void printOneItemsets ()
	{		
		Map <String,Info> currmap=oneItemsets.get(hierarchyDepth-1).candidates;			
					
		Iterator <String> it=currmap.keySet().iterator();
		while(it.hasNext())	{
			String key=it.next();
			Info info=currmap.get(key);
			System.out.println ("h="+(hierarchyDepth-1)+ ": "+ key +": "+info);
			
			//next level up
			String parentKey = General.getParentCategoryOfSingleKey(key);
			info = oneItemsets.get(hierarchyDepth-2).candidates.get(parentKey);
			System.out.println ("h="+(hierarchyDepth-2)+ ": "+ parentKey +": "+info);			
		}		
	}
	public boolean generateAllTwoItemsets()
	{
		//generate candidates from 1-itemsets
				
		for(int h=0;h<hierarchyDepth;h++)	{
			List <String> currsortedkeys=oneItemsets.get(h).sortedKeys;
				
			for(int i=0;i<currsortedkeys.size()-1;i++)	{
				for(int j=i+1;j<currsortedkeys.size();j++)	{					
					if(General.fromTwoDifferentCategories(currsortedkeys.get(i),currsortedkeys.get(j)))
					{
						String combinedkey =currsortedkeys.get(i)+General.DIFFERENT_CATEGORIES_SEPARATOR +
							currsortedkeys.get(j);
						if(h>0)	{
							String parentKey=General.getParentCategory(combinedkey);
							if(twoItemsets.get(h-1).candidates.containsKey(parentKey))	{
								Info newInfo=new Info(combinedkey);
								twoItemsets.get(h).candidates.put(combinedkey, newInfo);
							}
						}
						else {
							Info newInfo=new Info(combinedkey);
							twoItemsets.get(h).candidates.put(combinedkey, newInfo);
						}
					}					
				}
			}
		}
		
		//hash transactions
		try	{
			FileInputStream tfstream = new FileInputStream(transactionsFileName);
		    DataInputStream tis = new DataInputStream(tfstream);
		    BufferedReader in= new BufferedReader(new InputStreamReader(tis));
			String transLine;
			
			while((transLine=in.readLine())!=null)	{
				StringTokenizer st=new StringTokenizer(transLine, transactionDelimiter);
				
				List <Map <String, Object>> transmaps=new LinkedList <Map <String, Object>>();
				List <List<String>> sortedLists=new LinkedList<List<String>>();
				for(int h=0;h<hierarchyDepth;h++)	{
					transmaps.add(new HashMap <String,Object>());
					sortedLists.add(new ArrayList <String>());
				}
				
				while(st.hasMoreTokens()) {
					String itemKey=st.nextToken();
					for(int h=0;h<hierarchyDepth;h++)	{
						String itemCat=General.getCategoryString(hierarchyByItemID,
								itemKey,h);
						transmaps.get(h).put(itemCat, null);
					}
				}
				
				for(int h=0;h<hierarchyDepth;h++)	{
					Iterator <String> it=transmaps.get(h).keySet().iterator();
					while(it.hasNext())	{
						sortedLists.get(h).add(it.next());
					}
					Collections.sort(sortedLists.get(h));
				}
				
				for(int h=0;h<hierarchyDepth;h++)	{
					List <String> currKeysList=sortedLists.get(h);
					for(int i=0;i<currKeysList.size()-1;i++)	{
						for(int j=i+1;j<currKeysList.size();j++)	{
							String key12=currKeysList.get(i)
								+General.DIFFERENT_CATEGORIES_SEPARATOR+
								currKeysList.get(j);
							
							if(twoItemsets.get(h).candidates.containsKey(key12))	{
								Info currinfo=twoItemsets.get(h).candidates.get(key12);
								currinfo.count++;
								twoItemsets.get(h).candidates.put(key12, currinfo);
							}
						}
					}
				}					
			}			
			in.close();
		}
		catch (IOException e)	{
			System.out.println("Error reading transactions file: " + e.getMessage());
			return false;
		}
		//remove non-frequent items which are not pos and not neg
		//compute kulc, and set flags
		
		for(int h=0;h<hierarchyDepth;h++)	{
			Cell curOneCell = oneItemsets.get(h);
			Cell curTwoCell = twoItemsets.get(h);
			Cell parentOneCell = null;
			Cell parentTwoCell = null;
			if(h>0) {
				parentOneCell = oneItemsets.get(h-1);
				parentTwoCell = twoItemsets.get(h-1);
			}
			ArrayList <String>removekeys=new ArrayList <String>();
			
			for(Info valueIt:curTwoCell.candidates.values())	{
				Info currInfo=valueIt;
				String key12=currInfo.itemsKeys;
				
				String [] keys=General.splitCombinedKey(key12, General.DIFFERENT_CATEGORIES_SEPARATOR);
				currInfo.kulc=((double)currInfo.count/(double)curOneCell.candidates.get(keys[0]).count+
						(double)currInfo.count/(double)curOneCell.candidates.get(keys[1]).count)/2;	
				
				//add to the candidate sorted lists only those which are frequent
				if(currInfo.count>=minSupps[h])		{			
					if(currInfo.kulc<=neg)	{
						currInfo.setFlag(Info.NEGATIVE, true);
					}
					else if(currInfo.kulc>=pos)	{
						currInfo.setFlag(Info.POSITIVE, true);
					}
					
					curTwoCell.candidates.put(key12, currInfo);
					
					if (h >0) {
						String parentKey=General.getParentCategory(key12);
						if(!parentTwoCell.candidates.containsKey(parentKey))
							removekeys.add(key12);					
					}
					
					if(curOneCell.maxKulc.containsKey(keys[0])){
						if(curOneCell.maxKulc.get(keys[0])<currInfo.kulc) curOneCell.maxKulc.put(keys[0],currInfo.kulc);
					}else curOneCell.maxKulc.put(keys[0],currInfo.kulc);

					if(curOneCell.maxKulc.containsKey(keys[1])){
						if(curOneCell.maxKulc.get(keys[1])<currInfo.kulc) curOneCell.maxKulc.put(keys[1],currInfo.kulc);
					}else curOneCell.maxKulc.put(keys[1],currInfo.kulc);					
				}
				else
					removekeys.add(key12);
			}
			for(String key:removekeys)
				curTwoCell.candidates.remove(key);
			
			//Apply BOTTOM PRUNING which is used only for FULL PRUNING
			if(h>0){
				removekeys=new ArrayList <String>();
				
				//add 1-itemset for oneItemsets.get(h).candsFromBottomPruning: 
				//To be used for countCandidates and countCandidatesForCeiling
				for(String key:curOneCell.candidates.keySet()) {
					String parentKey = General.getParentCategory(key);
					if(curOneCell.maxKulc.containsKey(key) && parentOneCell.maxKulc.containsKey(parentKey) && !(curOneCell.maxKulc.get(key)<pos && parentOneCell.maxKulc.get(parentKey)<pos))
						curOneCell.candsFromBottomPruning.add(key);
				}
				
				for(String key12:curTwoCell.candidates.keySet()) {
					String [] curKeys=General.splitCombinedKey(key12, General.DIFFERENT_CATEGORIES_SEPARATOR);
					String [] parKeys=General.splitCombinedKey(General.getParentCategory(key12), General.DIFFERENT_CATEGORIES_SEPARATOR);

					if((curOneCell.maxKulc.get(curKeys[0])<pos && parentOneCell.maxKulc.get(parKeys[0])<pos)
							|| (curOneCell.maxKulc.get(curKeys[1])<pos && parentOneCell.maxKulc.get(parKeys[1])<pos))
						removekeys.add(key12);
				}
				for(String key:removekeys)
					curTwoCell.candidates.remove(key);
			}
		}		
		return true;
	}	
	
	//these generate recursively all possible k-combinations of keys provided as a list
	private void generateMoreSubsets( List <String> set,
			String [] subset, int subsetSize, int nextIndex) {
	    if (subsetSize == subset.length)    {
	    	keyCombinations.add(General.combineKey(subset,General.DIFFERENT_CATEGORIES_SEPARATOR));
	    }
	    else   {
	        for (int j = nextIndex; j < set.size(); j++)  {
	            subset[subsetSize] = set.get(j);
	            generateMoreSubsets(set, subset, subsetSize + 1, j + 1);
	        }
	    }
	}
	
	private void generateAllDifferentSubsets(List <String> set, int k) {
	    String [] subset = new String[k];
	    generateMoreSubsets(set, subset, 0, 0);
	}
	
	private void generateMoreSubsets1( List <String> set,
			String [] subset, int subsetSize, int nextIndex) {
	    if (subsetSize == subset.length)   {
	    	keyCombinations1.add(General.combineKey(subset,General.DIFFERENT_CATEGORIES_SEPARATOR));
	    }
	    else   {
	        for (int j = nextIndex; j < set.size(); j++)  {
	            subset[subsetSize] = set.get(j);
	            generateMoreSubsets1(set, subset, subsetSize + 1, j + 1);
	        }
	    }
	}
	
	private void generateAllDifferentSubsets1(List <String> set, int k) {
	    String [] subset = new String[k];
	    generateMoreSubsets1(set, subset, 0, 0);
	}
}
