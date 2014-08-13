package correlations;
/**
 * This program implements a flipper algorithm which computes
 * positive and negative correlations between items in a market basket
 * (or any other group of items which may be considered as one transaction)
 * 
 * The novelty is that this program
 * 1. computes correlations based on a null-invariant correlation measure
 * 2. computes correlation for the same items at different levels of abstraction
 * 3. computes both positive and negative correlations, but leaves only those which change from positive to negative (and vice versa)
 * across abstraction levels (flipping correlations)
 * 
 * @author Marina Barsky
 * Copyright 2011 
 * Written by Marina Barsky (mgbarsky@gmail.com)
 * Released under the GPL
 * 
 * This is the implementation of the algorithm described in
 * M. Barsky, S. Kim , T. Weninger, J. Han. 
Mining flipping correlations from large datasets with taxonomies. 
International conference on very large databases, VLDB-2012

 * Input files: 
 * 1. file with lines of transactions, each line is a separate transaction, each item is separated by a delimiter 
 * 2. at least 2 levels of hierarchy for each item supplied as a dictionary file
 * 3. supports file: for each hierarchy level - its min support
 * Order of hierarchy must be: item id, followed by from highest level of abstraction to the lowest
 * Default delimiters for both inputs: comma
 * * See "SAMPLE_RUNS.txt" for example of how to run Flipper
 */
import java.util.*;

public class Flipper {	
	public static void main(String [] args)	{
		if(args.length<6)	{
			System.out.println("java -Xms512m -Xmx1024m Flipper <inputFolder> <inputFile> <dictionaryfilename> "
					+"<supportsfilename> <pos kulc> <neg kulc> "+
					"[transactions file delimiter] [dictionarydelimiter]");
			return;
		}
		
		//collect program arguments
		String inputfoldername=args[0];
		String inputfilename=args[1];
		String dictionaryfilename=args[2];
		
		String supportsfile=args[3];		
		
		double pos=Double.parseDouble(args[4]);
		double neg=Double.parseDouble(args[5]);				
		
		String tdelimiter=","; //can be supplied as parameter 6
		if(args.length>6)
			tdelimiter=args[6];
		
		String dictdelimiter=","; //can be supplied as parameter 7
		if(args.length>7)
			dictdelimiter=args[7];		
		
		long start=System.currentTimeMillis();
		int totalCandidates=0;
		
		//initialize state which keeps entire table of flipping correlations
		//ROWS in this table correspond to the levels of abstraction
		//COLUMNS IN THIS TABLE are number of items in the itemset
		State state=new State(tdelimiter, pos, neg);		
		
		//checks that the input is valid
		if(!state.setInputFiles(inputfoldername, inputfilename, dictionaryfilename, 
				dictdelimiter, supportsfile ))
			return;
		System.out.println((System.currentTimeMillis()-start)+" ms to collect db info.");		
		
		//generates frequent 1-itemsets
		if(!state.generateAllF1())
			return;
		System.out.println((System.currentTimeMillis()-start)+" ms to generate all F1.");			
		
		//generates frequent 2-itemsets. removes non-flipping
		if(!state.generateAllTwoItemsets())
			return;
		System.out.println((System.currentTimeMillis()-start)+" ms to generate all F2.");			
		
		boolean end=false;
		//one list per hierarchy level
		List <List<Cell>> table=new LinkedList <List<Cell>>();
		
		//we are going to process all h rows -one row per each hierarchy level
		for(int i=0;i<state.hierarchyDepth;i++)	{
			table.add(new LinkedList<Cell>());
			table.get(i).add(new Cell(i,0,state));
			table.get(i).add(new Cell(i,1,state));
			table.get(i).add(state.twoItemsets.get(i));			
		}		
		
		//compute top row of the table: for the highest level of abstraction
		for(int k=3;k<state.maxTransactionWidthPerLevel.get(1) && !end;k++ ) {
			Cell cell0k=new Cell(0, k, state);
			Cell cell1k=new Cell(1, k, state);
			
			Cell leftCell0k_1=table.get(0).get(k-1);
			Cell leftCell1k_1=table.get(1).get(k-1);			
			
			if(!end && !cell1k.countCandidatesForCeiling(cell0k, leftCell1k_1, 
					leftCell0k_1, null))				
				end=true;
					
			if(state.stopCriteria(cell1k, cell0k))
				end=true;			
			
			table.get(0).add(cell0k);
			table.get(1).add(cell1k);
			
			if(table.get(0).get(k).candidates.size()==0)
				end=true;
			if(table.get(1).get(k).candidates.size()==0)
				end=true;			
			System.out.println("processed "+k+"-itemsets for hierarchies 0 and 1");			
		}
		
		if(!state.pruneRowFinal(table.get(1), table.get(0)))
			return;
		
		int totalCols0=table.get(0).size();
		int totalCols1=table.get(1).size();
		
		for(int x=2;x<totalCols0;x++)	{
			totalCandidates+=table.get(0).get(x).candidates.size();			
		}
		
		for(int x=2;x<totalCols1;x++)	{			
			totalCandidates+=table.get(1).get(x).candidates.size();
		}
		System.out.println((System.currentTimeMillis()-start)+" ms to prune final ceiling");
		
		int minK=Math.min(table.get(0).size(),table.get(1).size());
		for(int k=2;k<minK;k++)	{			
				System.out.println("Remaining candidates for "+k+"-itemsets at level 0 = " 
						+table.get(0).get(k).candidates.size() );
				System.out.println("Remaining candidates for "+k+"-itemsets at level 1 = " 
						+table.get(1).get(k).candidates.size() );
			
		}		
		
		//here we are trying to expand promising 2-itemsets into 3-, 4- ... itemsets
		//but we stop when no more itemsets in the previous column
		boolean horizontalEnd=false;
		
		for(int h=2;h<state.hierarchyDepth;h++)	{
			horizontalEnd=false;
			for(int k=3;k<table.get(h-1).size() && !horizontalEnd;k++ ) {
				Cell cell_h_k=new Cell(h, k, state);
				Cell leftCell=table.get(h).get(k-1);
				Cell parentCell=table.get(h-1).get(k);

				if(!cell_h_k.countCandidates(leftCell,  table.get(h-1))) {
					horizontalEnd=true;
				}
				System.out.println((System.currentTimeMillis()-start)+" ms to generate "+k+","+h+"-itemsets.");									
				table.get(h).add(cell_h_k);
				
				if(!horizontalEnd && state.stopCriteria(cell_h_k, parentCell))
					horizontalEnd=true;
				System.out.println((System.currentTimeMillis()-start)+" ms to prune "+k+","+h+"-itemsets.");				
				if(!horizontalEnd && cell_h_k.candidates.size()==0)
					horizontalEnd=true;
				System.out.println("processed "+k+"-itemsets for hierarchy "+h);					
			}			
			
			state.pruneRowFinal( table.get(h), table.get(h-1));
			
			int totalCols=table.get(h).size();
			for(int x=2;x<totalCols;x++)	{				
				totalCandidates+=table.get(h).get(x).candidates.size();
			}
			System.out.println((System.currentTimeMillis()-start)+" ms to prune "+h+"-level.");
		}
		
		state.printFlippingPathsForKItemsets(2, table);
		
		System.out.println((System.currentTimeMillis()-start)+" ms total.");
		System.out.println("Total "+totalCandidates+" candidate itemsets.");
	}
}



