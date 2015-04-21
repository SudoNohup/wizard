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

/**
 * The hyperarc data structure used in <i>k</i>-best generation.
 * 
 * @author ywwong
 *
 */
public class Hyperarc {

	public Item tail1;  // null if arity is 0 (i.e., head is a source vertex)
	public Item tail2;  // null if arity is 1
	public Item head;
	
	public Hyperarc(Item tail1, Item tail2, Item head) {
		this.tail1 = tail1;
		this.tail2 = tail2;
		this.head = head;
	}
	
	public Hyperarc(Item tail, Item head) {
		tail1 = tail;
		tail2 = null;
		this.head = head;
	}
	
	public Hyperarc(Item head) {
		tail1 = null;
		tail2 = null;
		this.head = head;
	}
	
	public int arity() {
		if (tail1 == null)
			return 0;
		else if (tail2 == null)
			return 1;
		else
			return 2;
	}
	
}
