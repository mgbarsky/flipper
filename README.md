flipper
=======
<h1>Flipper</h1>
<p>This is a datamining app which extracts flipping correlations 
from market basket transactions with taxonomies.

<p>For this program, correlation between items is positive if they often occur together 
in the same observation (transaction).
The correlation is negative if they rarely occur together.
The same item (for example, skim milk or white bread) 
can be generalized to higher abstraction levels (milk, bread).
The higher abstraction groups encompass more items.
The correlation between milk and bread can be positive, while correlation between some specific sub-types (skim milk and white bread) can be negative. 
We say that correlation flips when moving from one abstraction level to another.</p>

<p>Flipping correlations can be used
to find items which were incorrectly assigned to the wrong
category; to find surprising non-trivial correlations to be
explained; to discover under-represented, or over-represented
combinations of items; or to discover correlations specific
for some sub-population.</p>

<p>For more details on flipping correlations see: <br>
<em>M. Barsky, S. Kim , T. Weninger, J. Han.</em> <br>
<strong>Mining flipping correlations from large datasets with taxonomies.</strong> <br>
International conference on very large databases, VLDB-2012</p>

<h2>To compile:</h2>
Create <em>bin</em> folder. From <em>src</em> folder:
<pre><code>
set path=<em>path to JDK</em>/bin
javac -cp . correlations/Flipper.java -d ../bin
</code></pre>

<h2>Program parameters</h2>

Specify command-line arguments in the following order:
<ol>
<li>Full path to the folder with input files</li>
<li>Name of the file with lines of transactions, each line is a separate transaction, each item is separated by a delimiter </li>
<li>Name of the dictionary file, where for each item there are at least 2 levels of abstraction. 
Format: item ID, followed by the highest abstraction level, and then by more specific levels.</li>
<li>Name of the supports file: 
for each hierarchy level - specify its min support.</li>
<li>Minimum positive threshold (from 0 to 1): all correlations above it are considered positive.</li>
<li>Maximum negative threshold: all correlations below it are considered negative.</li>
</ol>
The default delimiters for transactions and for the hierarchy are commas. 
If your input has different delimiters, these can be re-specified 
as optional parameters 7 (for transactions) and 8 (for hierarchies).

<h2>Sample usage</h2>
There is a folder sample_input which contains groceries dataset in the 'groceries' sub-folder. 
This is a small dataset of grocery purchases with about 10,000 transactions.
To run Flipper on this dataset: 
<pre><code>
java -Xmx512M -Xms512m correlations.Flipper ../sample_input/groceries input_groceries.txt itemhierarchy.csv supports.txt 0.12 0.1
</code></pre>

or use the jar-packaged app with the same arguments:
<pre><code>java -Xmx512M -Xms512m -jar FLIPPER.jar arguments</code></pre>
Sample run and sample outputs are recorded in file 'SAMPLE_RUN.txt'
<h2>Program output</h2>
Flipping correlations are printed to stdout in the following form:
<pre>(+) DRINKS together with PERFUMERY[kulc=0.34] [count=555] 
  (-) BEER together with COSMETICS[kulc=0.08] [count=23] 
    (+) CANNED BEER together with BABY COSMETICS[kulc=0.17] [count=2]</pre>
Each line includes items (the same items at different levels of abstraction), correlation signs, 
and numeric values for correlation (kulc) and for support (count). <br>   
The above example reflects a famous {beer, diaper} itemset, now in a
more highlighted form: by showing the negative correlation
between minimal generalizations of the items.
