/*
 * Copyright 2006, 2007 Yuk Wah Wong.
 * 
 * This file is part of the WASP distribution.
 *
 * WASP is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * WASP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with WASP; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package wasp.scfg.lambda.generate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
/*
import java.util.logging.Level;
import java.util.logging.Logger;
*/

import wasp.data.Meaning;
import wasp.scfg.SCFG;
import wasp.util.Heap;

/**
 * A bottom-up chart for the lambda-SCFG-based generator.
 * 
 * @author ywwong
 *
 */
public class Chart {

	/*
	private static Logger logger = Logger.getLogger(Chart.class.getName());
	static {
		logger.setLevel(Level.FINEST);
	}
	*/
	
	private static final Comparator LATER_FIRST = new Comparator() {
		public int compare(Object o1, Object o2) {
			Item i1 = (Item) o1;
			Item i2 = (Item) o2;
			if (i1.timestamp < i2.timestamp)
				return -1;
			else if (i1.timestamp > i2.timestamp)
				return 1;
			else
				return 0;
		}
	};
	private static final int INC = 64;
	
	/** The maximum number of top-scoring theories to keep for each cell.  <code>1</code> is used for
	 * Viterbi decoding, <i>K</i> > 1 for <i>K</i>-best decoding. */
	public short kbest;

	public short maxPos;
	public ArrayList[][] toComps;
	public Heap[][] comps;
	public Item root;
	private HashMap[][][] intern;
	private int timestamp;
	
	public Chart(SCFG gram, Meaning F, short kbest) {
		this.kbest = kbest;
		maxPos = (short) (F.linear.length-1);
		int nlhs = gram.countNonterms();
		toComps = new ArrayList[maxPos+1][nlhs];
		comps = new Heap[maxPos+1][maxPos+1];
		root = null;
		intern = new HashMap[maxPos+1][maxPos+1][nlhs];
		for (short i = 0; i <= maxPos; ++i)
			for (short j = 0; j <= maxPos; ++j)
				comps[i][j] = new Heap(LATER_FIRST, INC);
		timestamp = 0;
	}
	
	public void addItem(Item item) {
		if (item.isRoot()) {
			if (root == null)
				root = item;
			else if (kbest == 1)
				root.max(item);
			else
				root.merge(item);
		} else {
			HashMap cell = cell(item);
			Item i = (Item) cell.get(item);
			if (i == null) {
				cell.put(item, item);
				addItemToTables(item);
			} else if (kbest == 1)
				i.max(item);
			else
				i.merge(item);
		}
	}
	
	private HashMap cell(Item item) {
		int root = item.root;
		int card = item.set.cardinality()-1;
		int lhs = item.rule.getLhsId();
		HashMap cells = intern[root][card][lhs];
		if (cells == null)
			cells = intern[root][card][lhs] = new HashMap();
		Cell c = new Cell(item);
		Cell cell = (Cell) cells.get(c);
		if (cell == null)
			cells.put(c, (cell = c));
		return cell.getMap();
	}
	
	private void addItemToTables(Item item) {
		item.timestamp = timestamp++;
		if (item.isComplete())
			// item is complete
			comps[item.root][item.set.cardinality()-1].add(item);
		else {
			// item is to be completed
			int n = item.args[item.dot].getId();
			short pos = item.pos[item.dot];
			if (toComps[pos][n] == null)
				toComps[pos][n] = new ArrayList();
			toComps[pos][n].add(item);
		}
	}
	
	public void prune(short root, short card) {
		for (int lhs = 0; lhs < intern[root][card].length; ++lhs) {
			HashMap cells = intern[root][card][lhs];
			if (cells != null)
				for (Iterator jt = cells.values().iterator(); jt.hasNext();) {
					Cell cell = (Cell) jt.next();
					if (cell.isComplete())
						cell.prune();
				}
		}
	}
	
}
