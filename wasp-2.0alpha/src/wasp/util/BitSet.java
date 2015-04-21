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
package wasp.util;

/**
 * An implementation of bit vectors.  Unlike <code>java.util.BitSet</code>, the vectors never grow in
 * size.
 * 
 * @author ywwong
 *
 */
public class BitSet implements Comparable, Copyable {

	private static final short[] CARD = new short[Short.MAX_VALUE+1];
	private static int BYTE_MASK = 0;
	private static final int[] BIG_END_MASKS = new int[8];
	private static final int[] SMALL_END_MASKS = new int[8];
	static {
		for (int i = 0; i <= Short.MAX_VALUE; ++i) {
			int j = i;
			while (j != 0) {
				if ((j&1) != 0)
					++CARD[i];
				j >>= 1;
			}
		}
		for (int i = 0; i < 8; ++i)
			BYTE_MASK |= (1<<i);
		for (int i = 0; i < 8; ++i)
			for (int j = i; j < 8; ++j)
				BIG_END_MASKS[i] |= (1<<j);
		for (int i = 0; i < 8; ++i)
			for (int j = 0; j <= i; ++j)
				SMALL_END_MASKS[i] |= (1<<j);
	}
			
    private byte[] data;
    private short length;
    private short card;
    
    public BitSet(short length) {
        data = null;
        this.length = length;
        card = 0;
    }
    
    private BitSet() {}
    
    public boolean equals(Object o) {
        if (o instanceof BitSet) {
            BitSet set = (BitSet) o;
            if (length != set.length)
            	return false;
            if (card != set.card)
            	return false;
            return data == null || Arrays.equal(data, set.data);
        }
        return false;
    }
    
    public int hashCode() {
        if (data == null)
            return 0;
        else {
        	long h = 1234;
            for (short i = (short) (data.length-1); i >= 0; --i)
                 h ^= data[i] * (i+1);
            return (int) ((h>>32) ^ h);
        }
    }
    
    public int compareTo(Object o) {
    	BitSet set = (BitSet) o;
    	if (length < set.length)
    		return -1;
    	else if (length > set.length)
    		return 1;
    	else if (card < set.card)
    		return -1;
    	else if (card > set.card)
    		return 1;
    	else if (card == 0)
    		return 0;
    	else
    		return Arrays.compare(data, set.data);
    }
    
    public Object copy() {
        BitSet copy = new BitSet();
        copy.data = (data==null) ? null : (byte[]) data.clone();
        copy.length = length;
        copy.card = card;
        return copy;
    }
    
    public boolean get(short index) {
        if (data == null)
            return false;
        else
            return (data[index>>3] & (1<<(index&7))) != 0;   
    }
    
    public short getShort(short first, short last) {
    	if (data == null)
    		return 0;
    	int firstB = (first>>3);
    	int firstb = (first&7);
    	int lastB = (last>>3);
    	int lastb = (last&7);
    	int value;
    	if (firstB == lastB) {
    		value = (data[firstB]&(SMALL_END_MASKS[lastb]&BIG_END_MASKS[firstb]));
    		value >>= firstb;
    	} else {
	    	value = (data[lastB]&SMALL_END_MASKS[lastb]);
	    	for (int i = lastB-1; i > firstB; --i)
	    		value = (value<<8) | (data[i]&BYTE_MASK);
	    	value = (value<<(8-firstb)) | ((data[firstB]&BIG_END_MASKS[firstb])>>firstb);
    	}
    	return (short) value;
    }
    
    public boolean and(short first, short last) {
    	for (short i = first; i <= last; ++i)
    		if (!get(i))
    			return false;
    	return true;
    }
    
    public boolean or(short first, short last) {
    	for (short i = first; i <= last; ++i)
    		if (get(i))
    			return true;
    	return false;
    }
    
    public void set(short index, boolean value) {
    	int i = index>>3;
    	int mask = 1<<(index&7);
        if (value) {
            if (data == null)
                data = new byte[((length-1)>>3)+1];
            if ((data[i]&mask) == 0) {
            	data[i] |= mask;
            	++card;
            }
        } else {
            if (data == null)
                return;
            if ((data[i]&mask) != 0) {
                data[i] &= ~mask;
            	--card;
            }
            if (card == 0)
                data = null;
        }
    }
    
    public void setAll(short first, short last, boolean value) {
    	for (short i = first; i <= last; ++i)
    		set(i, value);
    }
    
    public void setAll(boolean value) {
    	if (value) {
            if (data == null)
                data = new byte[((length-1)>>3)+1];
    		for (short i = 0; i < (length>>3); ++i)
    			data[i] = ~((byte) 0);
    		for (short i = 0; i < (length&7); ++i)
    			data[data.length-1] |= (1<<i);
    		card = length;
    	} else {
    		data = null;
    		card = 0;
    	}
    }
    
    public void setShort(short first, short last, short value) {
    	short c = CARD[value];
    	short oldc = CARD[getShort(first, last)];
    	if (card+(c-oldc) == 0) {
    		data = null;
    		card = 0;
    	} else {
    		if (data == null)
                data = new byte[((length-1)>>3)+1];
        	int firstB = (first>>3);
        	int firstb = (first&7);
        	int lastB = (last>>3);
        	int lastb = (last&7);
        	if (firstB == lastB) {
        		data[firstB] &= (SMALL_END_MASKS[lastb]^BIG_END_MASKS[firstb]);
        		data[firstB] |= (value<<firstb);
        	} else {
        		int v = value;
        		data[firstB] &= ~BIG_END_MASKS[firstb];
        		data[firstB] |= ((v&SMALL_END_MASKS[8-firstb])<<firstb);
        		v >>= 8-firstb;
        		for (int i = firstB+1; i < lastB; ++i) {
        			data[i] = (byte) (v&BYTE_MASK);
        			v >>= 8;
        		}
        		data[lastB] &= ~SMALL_END_MASKS[lastb];
        		data[lastB] |= (v&SMALL_END_MASKS[lastb]);
        	}
    		card += c-oldc;
    	}
    }
    
    public boolean isSubsetOf(BitSet set) {
        if (length != set.length)
            return false;
        if (data == null)
            return true;
        if (set.data == null)
            return false;
        if (card > set.card)
        	return false;
        for (short i = 0; i < data.length; ++i)
            if ((data[i] & ~set.data[i]) != 0)
                return false;
        return true;
    }
    
    /**
     * Finds the intersection of this bit vector and the specified bit vector.  This operation is
     * non-destructive.
     *  
     * @param set a bit vector.
     * @return the intersection of this bit vector and the <code>set</code> argument.
     */
    public BitSet intersect(BitSet set) {
        if (length != set.length)
            return null;
        if (data == null || set.data == null)
            return new BitSet(length);
        byte[] d = new byte[data.length];
        short card = 0;
        for (short i = 0; i < data.length; ++i) {
            d[i] = (byte) (data[i] & set.data[i]);
            card += CARD[d[i]&BYTE_MASK];
        }
        BitSet s = new BitSet();
        s.data = (card==0) ? null : d;
        s.length = length;
        s.card = card;
        return s;
    }
    
    /**
     * Finds the union of this bit vector and the specified bit vector.  This operation is
     * non-destructive.
     * 
     * @param set a bit vector.
     * @return the union of this bit vector and the <code>set</code> argument.
     */
    public BitSet union(BitSet set) {
        if (length != set.length)
            return null;
        if (set.data == null)
            return (BitSet) copy();
        if (data == null)
        	return (BitSet) set.copy();
        byte[] d = new byte[data.length];
        short card = 0;
        for (short i = 0; i < data.length; ++i) {
            d[i] = (byte) (data[i] | set.data[i]);
            card += CARD[d[i]&BYTE_MASK];
        }
        BitSet s = new BitSet();
        s.data = d;
        s.length = length;
        s.card = card;
        return s;
    }
    
    public BitSet andNot(BitSet set) {
    	if (length != set.length)
    		return null;
    	if (data == null || set.data == null)
    		return (BitSet) copy();
        byte[] d = new byte[data.length];
        short card = 0;
        for (short i = 0; i < data.length; ++i) {
            d[i] = (byte) (data[i] & ~set.data[i]);
            card += CARD[d[i]&BYTE_MASK];
        }
        BitSet s = new BitSet();
        s.data = (card==0) ? null : d;
        s.length = length;
        s.card = card;
        return s;
    }
    
    public short length() {
    	return length;
    }
    
    public short cardinality() {
    	return card;
    }
    
    public boolean isEmpty() {
        return data == null;
    }
    
    public boolean isFull() {
    	return card == length;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append('{');
        boolean first = true;
        for (short i = 0; i < length; ++i)
            if (get(i)) {
                if (first)
                    first = false;
                else
                    sb.append(", ");
                short to = (short) (i+1);
                for (; to < length; ++to)
                	if (!get(to))
                		break;
        		if (to == i+1)
        			sb.append(i);
        		else {
        			sb.append(i);
        			sb.append('-');
        			sb.append(to-1);
        		}
        		i = to;
            }
        sb.append('}');
        return sb.toString();
    }
    
}
