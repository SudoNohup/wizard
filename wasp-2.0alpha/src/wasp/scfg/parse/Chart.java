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
package wasp.scfg.parse;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
/*
import java.util.logging.Level;
import java.util.logging.Logger;
*/

import wasp.data.Nonterminal;
import wasp.data.Symbol;
import wasp.data.Terminal;
import wasp.scfg.SCFG;
import wasp.util.Double;
import wasp.util.Heap;

/**
 * An Earley chart for the SCFG- or lambda-SCFG-based semantic parser.
 * 
 * @author ywwong
 *
 */
public class Chart {

	/*
	private static Logger logger = Logger.getLogger(Chart.class.getName());
	static {
		logger.setLevel(Level.FINER);
	}
	*/
	
    private static final Comparator EARLIER_FIRST = new Comparator() {
        public int compare(Object o1, Object o2) {
            Item i1 = (Item) o1;
            Item i2 = (Item) o2;
            if (i1.start < i2.start)
                return -1;
            else if (i1.start > i2.start)
                return 1;
            else if (i1.timestamp > i2.timestamp)
                return -1;
            else if (i1.timestamp < i2.timestamp)
                return 1;
            else
                return 0;
        }
    };
	private static final Comparator LATER_FIRST = new Comparator() {
		public int compare(Object o1, Object o2) {
			Item i1 = (Item) o1;
			Item i2 = (Item) o2;
			if (i1.start > i2.start)
				return -1;
			else if (i1.start < i2.start)
				return 1;
			else if (i1.timestamp < i2.timestamp)
				return -1;
			else if (i1.timestamp > i2.timestamp)
				return 1;
			else
				return 0;
		}
	};
	private static final int INC = 64;
	
	/** The maximum number of top-scoring theories to keep for each cell.  <code>0</code> means no limit
	 * is imposed.  <code>1</code> is used for Viterbi decoding, <i>K</i> > 1 for <i>K</i>-best
	 * decoding. */
	private int kbest;
	/** Indicates if items with empty <code>m</code> field are ignored and never added to the chart. */
	private boolean ignoreEmpty;
	
	public short maxPos;
	public ArrayList[] sets;
	public ArrayList[][] toComps;
	public Heap[] comps;
	private BitSet[][] predicted;
	private HashMap[][][] intern;
	private int timestamp;
	
	public Chart(SCFG gram, Terminal[] s, int kbest, boolean ignoreEmpty) {
		this.kbest = kbest;
		this.ignoreEmpty = ignoreEmpty;
		maxPos = (short) s.length;
		int nlhs = gram.countNonterms();
		sets = new ArrayList[maxPos+1];
		toComps = new ArrayList[maxPos+1][nlhs];
		comps = new Heap[maxPos+1];
		predicted = new BitSet[maxPos+1][nlhs];
		intern = new HashMap[maxPos+1][][];
		for (short i = 0; i <= maxPos; ++i) {
			sets[i] = new ArrayList();
			comps[i] = new Heap(LATER_FIRST, INC);
			intern[i] = new HashMap[i+1][nlhs+1];
		}
		timestamp = 0;
	}
	
	public void addItem(Item item) {
		if (ignoreEmpty && item.cov != null && item.cov.isEmpty())
			return;
		//logger.finest(item.start+" "+item.current+" "+item.dot+" "+item.m+" "+item.rule);
		HashMap cell = cell(item);
		if (kbest == 0) {
			Item i = intern(cell, item);
			if (i == null) {
				addIntern(cell, item);
				add(item);
			} else
				i.combine(item);
		} else if (kbest == 1) {
			Item i = intern(cell, item);
			if (i == null) {
				addIntern(cell, item);
				add(item);
			} else if (i.inner < item.inner)
				i.replace(item);
		} else {
			Item[] a = internArray(cell, item);
			int i = 0;
			for (; i < kbest && a[i] != null && a[i].inner > item.inner; ++i)
				;
			if (i < kbest) {
				if (a[kbest-1] == null) {
					for (int j = kbest-1; j >= i+1; --j)
						a[j] = a[j-1];
					a[i] = item;
					add(item);
				} else {
					for (int j = kbest-1; j >= i+1; --j)
						a[j].replace(a[j-1]);
					a[i].replace(item);
				}
			}
		}
	}

	private HashMap cell(Item item) {
		int lhs = item.rule.getLhsId();
		HashMap cell = intern[item.current][item.start][lhs+1];
		if (cell == null)
			cell = intern[item.current][item.start][lhs+1] = new HashMap();
		return cell;
	}
	
	private Item intern(HashMap cell, Item item) {
		return (Item) cell.get(item);
	}

	private void addIntern(HashMap cell, Item item) {
		cell.put(item, item);
	}
	
	/**
	 * Returns an array containing all copies of the specified item that already exist in the given cell.
	 * Only the top <i>K</i> copies of an item are kept.  Unlike other <code>intern</code> methods in the
	 * <code>wasp</code> package, this method does <i>not</i> add the specified item to the cell.
	 * Instead, new items are later added to the returned array.
	 * 
	 * @param cell a cell.
	 * @param item an item.
	 * @return an array containing all copies of the given item that already exist in the given cell.
	 */
	private Item[] internArray(HashMap cell, Item item) {
		Item[] a = (Item[]) cell.get(item);
		if (a == null) {
			a = new Item[kbest];
			cell.put(item, a);
		}
		return a;
	}
	
	private void add(Item item) {
		item.timestamp = timestamp++;
		sets[item.current].add(item);
		if (item.dot == item.rule.lengthE())
			// item is complete
			comps[item.current].add(item);
		else {
			Symbol sym = item.rule.getE(item.dot);
			if (sym instanceof Nonterminal && item.current < maxPos) {
				// item is to be completed
				int n = sym.getId();
				if (toComps[item.current][n] == null)
					toComps[item.current][n] = new ArrayList();
				toComps[item.current][n].add(item);
			}
		}
	}
	
	public boolean isPredicted(short start, int lhs, short nslots) {
		if (predicted[start][lhs] == null)
			return false;
		return predicted[start][lhs].get(nslots);
	}
	
	public void predict(short start, int lhs, short nslots) {
		if (predicted[start][lhs] == null)
			predicted[start][lhs] = new BitSet();
		predicted[start][lhs].set(nslots);
	}
	
	public void resetOuterScores() {
		for (short i = 0; i <= maxPos; ++i)
			for (Iterator jt = sets[i].iterator(); jt.hasNext();) {
				Item item = (Item) jt.next();
				item.outer = Double.NEGATIVE_INFINITY;
			}
	}
	
	public Heap createReverseHeap() {
		return new Heap(EARLIER_FIRST, INC);
	}
	
}
