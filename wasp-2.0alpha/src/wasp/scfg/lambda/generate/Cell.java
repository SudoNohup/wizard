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

import java.util.HashMap;

import wasp.main.Config;
import wasp.math.Math;
import wasp.util.Int;

/**
 * Cells for holding chart items used in the lambda-SCFG-based generator.
 * 
 * @author ywwong
 *
 */
public class Cell {

	private static final int PRUNE_K = Int.parseInt(Config.get(Config.SCFG_LAMBDA_PRUNE_K));
	
	private Item key;
	private HashMap map;
	
	public Cell(Item key) {
		this.key = key;
		map = null;
	}
	
	public boolean equals(Object o) {
		if (o instanceof Cell) {
			Item k = ((Cell) o).key;
			return key.set.equals(k.set) && key.args.length == k.args.length && key.dot == k.dot;
		}
		return false;
	}
	
	public int hashCode() {
		int hash = 1;
		hash = 31*hash + key.set.hashCode();
		hash = 31*hash + key.args.length;
		hash = 31*hash + key.dot;
		return hash;
	}
	
	public boolean isComplete() {
		return key.isComplete();
	}
	
	public HashMap getMap() {
		if (map == null)
			map = new HashMap();
		return map;
	}
	
	public void prune() {
		Item[] a = (Item[]) map.values().toArray(new Item[0]);
		if (a.length <= PRUNE_K)
			return;
		double min = select(a, 0, a.length-1, PRUNE_K).D[0].weight;
		for (int k = 0; k < a.length; ++k)
			if (a[k].D[0].weight < min)
				a[k].deactivate();
	}
	
	private Item select(Item[] items, int p, int r, int i) {
		if (p == r)
			return items[p];
		int q = partition(items, p, r);
		int k = q-p+1;
		if (i <= k)
			return select(items, p, q, i);
		else
			return select(items, q+1, r, i-k);
	}
	
	private int partition(Item[] items, int p, int r) {
		// randomize to avoid worst-case input
		int rand = Math.random(p, r);
		Item tmp = items[p];
		items[p] = items[rand];
		items[rand] = tmp;
		// partition
		double w = items[p].D[0].weight;
		int i = p-1;
		int j = r+1;
		while (true) {
			do {
				--j;
			} while (items[j].D[0].weight < w);
			do {
				++i;
			} while (items[i].D[0].weight > w);
			if (i < j) {
				tmp = items[i];
				items[i] = items[j];
				items[j] = tmp;
			} else
				return j;
		}
	}
	
}
