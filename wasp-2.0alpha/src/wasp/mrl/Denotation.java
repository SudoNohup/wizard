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
package wasp.mrl;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.TreeSet;

import wasp.data.Variable;
import wasp.main.Config;
import wasp.math.Math;
import wasp.util.Arrays;
import wasp.util.BitSet;
import wasp.util.Numberer;

/**
 * The class for keeping track of entity types that can possibly be denoted by logical variables.
 *  
 * @author ywwong
 *
 */
public class Denotation {

	private static final short NUM_TYPES = Config.getMRLGrammar().countTypes();
	private static final short WIDTH = (short) Math.ceil(Math.log2(NUM_TYPES+1));
	private static final Numberer TYPES = new Numberer(1);
	
	private BitSet[] tuples;
	private int hash;

	public Denotation(short dim) {
		tuples = new BitSet[1];
		tuples[0] = new BitSet((short) (dim*WIDTH));
		hash = Arrays.hashCode(tuples);
	}
	
	private Denotation(BitSet[] tuples) {
		this.tuples = tuples;
		hash = Arrays.hashCode(tuples);
	}
	
	public boolean equals(Object o) {
		return o instanceof Denotation && Arrays.equal(tuples, ((Denotation) o).tuples);
	}
	
	public int hashCode() {
		return hash;
	}
	
	public boolean isEmpty() {
		return tuples.length == 0;
	}
	
	public short getDim() {
		return (short) (tuples[0].length()/WIDTH);
	}
	
	public Denotation multiply(Denotation d, short index) {
		if (tuples.length == 0)
			return this;
		if (d.tuples.length == 0)
			return d;
		BitSet[] array = new BitSet[tuples.length*d.tuples.length];
		short len1 = tuples[0].length();
		short len2 = d.tuples[0].length();
		for (int i = 0, j = 0; j < tuples.length; ++j)
			for (int k = 0; k < d.tuples.length; ++k) {
				array[i] = new BitSet((short) (len1+len2));
				short l = 0;
				for (short m = 0; m < index*WIDTH; ++m)
					array[i].set(l++, tuples[j].get(m));
				for (short m = 0; m < len2; ++m)
					array[i].set(l++, d.tuples[k].get(m));
				for (short m = (short) (index*WIDTH); m < len1; ++m)
					array[i].set(l++, tuples[j].get(m));
				++i;
			}
		return new Denotation(array);
	}
	
	public Denotation cut(Variable[] vars) {
		if (tuples.length == 0)
			return this;
		TreeSet set = new TreeSet();
		short len = (short) (vars.length*WIDTH);
		for (int i = 0; i < tuples.length; ++i) {
			BitSet s = new BitSet(len);
			if (!tuples[i].isEmpty())
				for (short j = 0, k = 0; j < vars.length; ++j, k += WIDTH) {
					short first = (short) ((vars[j].getVarId()-1)*WIDTH);
					short last = (short) (first+WIDTH-1);
					short val = tuples[i].getShort(first, last);
					s.setShort(k, (short) (k+WIDTH-1), val);
				}
			set.add(s);
		}
		return new Denotation((BitSet[]) set.toArray(new BitSet[set.size()]));
	}
	
	public Denotation uncut(Variable[] vars, short dim) {
		if (tuples.length == 0)
			return this;
		TreeSet set = new TreeSet();
		short len = (short) (dim*WIDTH);
		NEXT: for (int i = 0; i < tuples.length; ++i) {
			BitSet s = new BitSet(len);
			if (!tuples[i].isEmpty())
				for (short j = 0, k = 0; j < vars.length; ++j, k += WIDTH) {
					short first = (short) ((vars[j].getVarId()-1)*WIDTH);
					short last = (short) (first+WIDTH-1);
					short val1 = s.getShort(first, last);
					short val2 = tuples[i].getShort(k, (short) (k+WIDTH-1));
					if (val1 != 0 && val2 != 0 && val1 != val2)
						continue NEXT;
					s.setShort(first, last, ((val1==0) ? val2 : val1));
				}
			set.add(s);
		}
		return new Denotation((BitSet[]) set.toArray(new BitSet[set.size()]));
	}
	
	public Denotation intersect(Denotation d) {
		if (tuples.length == 0)
			return this;
		if (d.tuples.length == 0)
			return d;
		TreeSet set = new TreeSet();
		short len = tuples[0].length();
		for (int i = 0; i < tuples.length; ++i)
			NEXT: for (int j = 0; j < d.tuples.length; ++j) {
				if (tuples[i].isEmpty()) {
					set.add(d.tuples[j]);
					continue NEXT;
				}
				if (d.tuples[j].isEmpty()) {
					set.add(tuples[i]);
					continue NEXT;
				}
				BitSet s = new BitSet(len);
				for (short k = 0, l = (short) (WIDTH-1); k < len; k += WIDTH, l += WIDTH) {
					short val1 = tuples[i].getShort(k, l);
					short val2 = d.tuples[j].getShort(k, l);
					if (val1 != 0 && val2 != 0 && val1 != val2)
						continue NEXT;
					s.setShort(k, l, ((val1==0) ? val2 : val1));
				}
				set.add(s);
			}
		return new Denotation((BitSet[]) set.toArray(new BitSet[set.size()]));
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append('{');
		for (int i = 0; i < tuples.length; ++i) {
			if (i > 0)
				sb.append(',');
			sb.append('(');
			for (short j = 0; j < tuples[i].length(); j += WIDTH) {
				if (j > 0)
					sb.append(',');
				short val = tuples[i].getShort(j, (short) (j+WIDTH-1));
				if (val == 0)
					sb.append('_');
				else
					sb.append(TYPES.getObj(val));
			}
			sb.append(')');
		}
		sb.append('}');
		return sb.toString();
	}
	
	public static Denotation read(String token) {
		StringTokenizer tokenizer = new StringTokenizer(token, "{(,)}", true);
		ArrayList list = new ArrayList();
		while (tokenizer.hasMoreTokens())
			list.add(tokenizer.nextToken());
		String[] array = (String[]) list.toArray(new String[0]);
		if (!array[0].equals("{") || !array[array.length-1].equals("}"))
			return null;
		if (array.length == 2)
			return new Denotation(new BitSet[0]);
		int lparen = Arrays.indexOf(array, "(");
		int rparen = Arrays.indexOf(array, ")");
		if (lparen < 0 || rparen < 0)
			return null;
		short dim = (short) ((rparen-lparen)/2);
		int ntuples = (array.length-1)/((dim+1)*2);
		if (ntuples*(dim+1)*2+1 != array.length)
			return null;
		BitSet[] tuples = new BitSet[ntuples];
		for (int i = 1, j = 0; j < ntuples; ++j) {
			if (j > 0)
				if (!array[i++].equals(","))
					return null;
			if (!array[i++].equals("("))
				return null;
			tuples[j] = new BitSet((short) (dim*WIDTH));
			for (short k = 0; k < dim; ++k) {
				if (k > 0)
					if (!array[i++].equals(","))
						return null;
				String type = array[i++];
				if (type.equals("_"))
					continue;
				short val = (short) TYPES.getId(type, true);
				if (val > NUM_TYPES)
					return null;
				tuples[j].setShort((short) (k*WIDTH), (short) (((k+1)*WIDTH)-1), val);
			}
			if (!array[i++].equals(")"))
				return null;
		}
		Arrays.sort(tuples);
		return new Denotation(tuples);
	}
	
}
