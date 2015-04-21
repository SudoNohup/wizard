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
package wasp.scfg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import wasp.data.Node;
import wasp.util.Arrays;
import wasp.util.DisjointSets;
import wasp.util.Short;
import wasp.util.ShortSymbol;

/**
 * The class for undirected graphs.  These graphs are used in lambda-WASP for conjunct regrouping.
 * 
 * @author ywwong
 *
 */
public class Graph {

	private static class Edge implements Comparable {
		public short v1, v2;
		public double weight;
		public Edge(short v1, short v2, double weight) {
			this.v1 = v1;
			this.v2 = v2;
			this.weight = weight;
		}
		public int compareTo(Object o) {
			Edge e = (Edge) o;
			if (weight < e.weight)
				return -1;
			else if (weight > e.weight)
				return 1;
			else if (v1 < e.v1)
				return -1;
			else if (v1 > e.v1)
				return 1;
			else if (v2 < e.v2)
				return -1;
			else if (v2 > e.v2)
				return 1;
			else
				return 0;
		}
	}
	
	private short n;
	private ArrayList edges;
	private DisjointSets scomp;
	private short[][] comps;
	private short[] roots;
	private DisjointSets stree;
	private Node[] trees;
	
	public Graph(short n) {
		this.n = n;
		edges = new ArrayList();
		scomp = new DisjointSets(n);
		comps = null;
		roots = null;
		stree = new DisjointSets(n);
		trees = null;
	}
	
	public void addEdge(short v1, short v2, double weight) {
		edges.add(new Edge(v1, v2, weight));
		scomp.union(v1, v2);
	}
	
	public void findComponents() {
		TreeMap map = new TreeMap();
		for (short i = 0; i < n; ++i) {
			Short rep = new Short(scomp.findSet(i));
			ArrayList list = (ArrayList) map.get(rep);
			if (list == null) {
				list = new ArrayList();
				map.put(rep, list);
			}
			list.add(new Short(i));
		}
		comps = new short[map.size()][];
		short c = 0;
		for (Iterator it = map.values().iterator(); it.hasNext(); ++c)
			comps[c] = Arrays.toShortArray((ArrayList) it.next());
	}
	
	public short countComponents() {
		return (short) comps.length;
	}
	
	public short[] getComponent(short c) {
		return comps[c];
	}
	
	public void setTreeRoot(short c, short root) {
		if (roots == null)
			roots = new short[comps.length];
		roots[c] = root;
	}
	
	public void findMinSpanForest() {
		// Kruskal's algorithm
		boolean[][] A = new boolean[n][n];
		Edge[] E = (Edge[]) edges.toArray(new Edge[0]);
		Arrays.sort(E);
		for (short i = 0; i < E.length; ++i)
			if (!stree.isSameComponent(E[i].v1, E[i].v2)) {
				A[E[i].v1][E[i].v2] = true;
				A[E[i].v2][E[i].v1] = true;
				stree.union(E[i].v1, E[i].v2);
			}
		// make trees
		trees = new Node[comps.length];
		for (short i = 0; i < trees.length; ++i)
			trees[i] = makeTree(A, roots[i], (short) -1);
	}
	
	private Node makeTree(boolean[][] A, short i, short prev) {
		Node n = new Node(new ShortSymbol(i));
		for (short j = 0; j < A[i].length; ++j)
			if (j != prev && A[i][j])
				n.addChild(makeTree(A, j, i));
		return n;
	}
	
	public Node getMinSpanTree(short c) {
		return trees[c];
	}
	
}
