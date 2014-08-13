package correlations;
/**
 * Represents a single cell in the matrix of correlations
 * Matrix is a table: X-axis - number of items in the itemset
 * Y-axis - hierarchy level
 */
import java.util.*;

public class Cell {
	public int numberOfItems;
	public int hierarchyLevel;
	private State state;
	
	public List <String> sortedKeys=new ArrayList <String>();
	public Map <String,Info> candidates=new HashMap <String,Info>();
	public Map <String,Double> maxKulc;//Only for 1-itemsets 
	public Set <String> candsFromBottomPruning;//Only for 1-itemsets
	public int numCandidates;//Only for General.FREQUENCY_PRUNING case
	public Map <String,Object> RCandidates=new HashMap <String,Object> ();
	public List <Info> keysSortedBySupport=new ArrayList <Info>();
	
	public Cell(int currentH, int currrentk, State infoState) {
		hierarchyLevel=currentH;
		numberOfItems=currrentk;
		state=infoState;
	}

	//Only for 1-itemsets 
	public Cell(int currentH, int currrentk, State infoState, int one)	{
		hierarchyLevel=currentH;
		numberOfItems=currrentk;
		state=infoState;
		maxKulc=new HashMap <String,Double>();
		candsFromBottomPruning = new HashSet<String>();
	}

	public boolean maxMinNonPositive(Map <String,Info> ones) {
		Iterator <Info>iter=candidates.values().iterator();
		while(iter.hasNext())	{
			Info curr=iter.next();
			int maxCount=curr.count;
			
			String [] singlekeys=General.splitCombinedKey(curr.itemsKeys, 
					General.DIFFERENT_CATEGORIES_SEPARATOR);
			int requiredMin=(int)((double)maxCount/state.pos);
			for(int i=0;i<singlekeys.length;i++)	{
				if(ones.get(singlekeys[i]).count<requiredMin)
					return false;
			}
		}
		return true;
	}
	
	public boolean allNonPositive()	{
		Iterator <Info> it=candidates.values().iterator();
		while(it.hasNext())		{
			Info info=it.next();
			if(info.kulc>=state.pos)
				return false;
		}
		return true;
	}
	
	//Just for FREQUENCY_PRUNING
	public boolean countCandidates(Cell leftCell, Cell parentCell)	{
		return state.countCandidates(this, leftCell,parentCell);
	}
	
	public boolean countCandidates(Cell leftCell,   List <Cell> parentRow)	{
		Cell parentLeftCell=parentRow.get(this.numberOfItems-1);
		Cell parentCell=parentRow.get(this.numberOfItems);		
		
		if(parentLeftCell !=null && parentLeftCell.allNonPositive() && leftCell.allNonPositive())
			return false;

		if(parentCell.candidates.size()==0 || leftCell.candidates.size()==0)  //since there is no path down in the left column, there would not be in this too
			return false;

		if(state.maxTransactionWidthPerLevel.get(hierarchyLevel)<=numberOfItems)
			return false;
		
		return state.countCandidates(this, leftCell,parentRow);		
	}

	public boolean countCandidatesForCeiling(Cell parentCell, Cell leftCell, Cell parentLeftCell, Cell childLeftCell)
	{		
		if(state.maxTransactionWidthPerLevel.get(hierarchyLevel)<=numberOfItems)
			return false;
		
		return state.countCandidatesForCeiling(this, parentCell,leftCell,parentLeftCell);		
	}	
}
