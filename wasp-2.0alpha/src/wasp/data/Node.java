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
package wasp.data;

import java.util.ArrayList;

import wasp.util.Arrays;
import wasp.util.Int;

/**
 * The class for tree nodes.  These nodes form the basis of NL and MRL parse trees.
 * 
 * @author ywwong
 *
 */
public class Node {

	private static final int INC = 4;
	
	protected Symbol sym;
	protected Node parent;
	protected Node[] children;
	protected short nchildren;
	private int hash;
	
	public Node(Symbol sym) {
		this.sym = sym;
		parent = null;
		children = null;
		nchildren = 0;
		hash = sym.hashCode();
	}

	public Node(Symbol sym, short capacity) {
		this.sym = sym;
		parent = null;
		children = (capacity>0) ? new Node[capacity] : null;
		nchildren = 0;
		hash = sym.hashCode();
	}
	
	public boolean equals(Object o) {
		return o == this;
	}

	public boolean treeEquals(Object o) {
		if (o instanceof Node) {
			Node n = (Node) o;
			if (!sym.equals(n.sym) || nchildren != n.nchildren)
				return false;
			for (short i = 0; i < nchildren; ++i)
				if (!children[i].treeEquals(n.children[i]))
					return false;
			return true;
		}
		return false;
	}
	
	/**
	 * Creates and returns a copy of this node along with its descendants.
	 * 
	 * @return a copy of this node along with its descendants.
	 */
	public Node deepCopy() {
		Node n = new Node((Symbol) sym.copy(), nchildren);
		for (short i = 0; i < nchildren; ++i)
			n.addChild(children[i].deepCopy());
		return n;
	}
	
	/**
	 * Creates and returns a copy of this node.
	 * 
	 * @return a copy of this node.
	 */
	public Node shallowCopy() {
		return new Node((Symbol) sym.copy());
	}

	public Symbol getSymbol() {
		return sym;
	}
	
	public void setSymbol(Symbol sym) {
		int hash = this.sym.hashCode();
		this.sym = sym;
		updateAncestors(sym.hashCode()-hash);
	}
	
	public int getHash() {
		return hash;
	}
	
	public Node getParent() {
		return parent;
	}
	
	public boolean hasParent() {
		return parent != null;
	}
	
	/**
	 * Returns an array containing all children of this node.  A <code>Node</code> array is always
	 * returned, even when this node has no children.
	 * 
	 * @return an array containing all children of this node.
	 */
	public Node[] getChildren() {
		return (children==null) ? new Node[0] : children;
	}
	
	/**
	 * Returns the <code>i</code>-th child of this node.  If this child does not exist, then either
	 * <code>null</code> is returned, or an exception is thrown.
	 * 
	 * @param i an index.
	 * @return the <code>i</code>-th child of this node.
	 */
	public Node getChild(short i) {
		return children[i];
	}
	
	/**
	 * Indicates if this node has any children.
	 * 
	 * @return <code>true</code> if this node has a child; <code>false</code> otherwise.
	 */
	public boolean hasChildren() {
		return nchildren > 0;
	}
	
	/**
	 * Returns the number of children that this node has.
	 * 
	 * @return the number of children of this node.
	 */
	public short countChildren() {
		return nchildren;
	}
	
	/**
	 * Appends a child to the end of the children list of this node.
	 * 
	 * @param child the child node to add.
	 */
	public void addChild(Node child) {
		if (children == null)
			children = new Node[INC];
		if (nchildren == children.length)
			children = (Node[]) Arrays.resize(children, nchildren+INC);
		children[nchildren++] = child;
		child.parent = this;
		updateAncestors(child.hash);
	}
	
	private void updateAncestors(int hashInc) {
		Node n = this;
		while (n != null) {
			n.hash += hashInc;
			n = n.parent;
		}
	}
	
	/**
	 * Inserts a child to the front of the children list of this node.
	 * 
	 * @param child the child node to add.
	 */
	public void addChildToFront(Node child) {
		if (children == null)
			children = new Node[INC];
		if (nchildren == children.length)
			children = (Node[]) Arrays.resize(children, nchildren+INC);
		for (short i = (short)(nchildren-1); i >= 0; --i)
			children[i+1] = children[i];
		children[0] = child;
		++nchildren;
		child.parent = this;
		updateAncestors(child.hash);
	}
	
	/**
	 * Replaces a child with another in the children list of this node.
	 * 
	 * @param child the child node to replace.
	 * @param replacement the node that replaces <code>child</code>.
	 */
	public void replaceChild(Node child, Node replacement) {
		short i = indexOf(child);
		if (i >= 0) {
			children[i].parent = null;
			children[i] = replacement;
			replacement.parent = this;
			updateAncestors(replacement.hash-child.hash);
		}
	}
	
	public void replaceChildWithChildren(Node child, Node replacement) {
		short i = indexOf(child);
		if (i >= 0) {
			children[i].parent = null;
			Node[] c = (Node[]) Arrays.subarray(replacement.getChildren(), (short) 0, replacement.nchildren);
			children = (Node[]) Arrays.replace(children, i, c);
			nchildren += c.length-1;
			int h = 0;
			for (short j = 0; j < replacement.nchildren; ++j) {
				replacement.children[j].parent = this;
				h += replacement.children[j].hash;
			}
			replacement.nchildren = 0;
			updateAncestors(h-child.hash);
		}
	}
	
	/**
	 * Removes a child from the children list of this node.
	 * 
	 * @param child the child node to remove.
	 */
	public void removeChild(Node child) {
		short i = indexOf(child);
		if (i >= 0) {
			children[i].parent = null;
			for (; i < nchildren-1; ++i)
				children[i] = children[i+1];
			children[nchildren-1] = null;
			--nchildren;
			updateAncestors(-child.hash);
		}
	}

	/**
	 * Searches for the first occurence of <code>child</code> in the children list of this node.
	 * 
	 * @param child a child node.
	 * @return the index of the first occurence of <code>child</code> in the children list of this node;
	 * returns <code>-1</code> if the child is not found. 
	 */
	public short indexOf(Node child) {
		return (short) Arrays.indexOf(children, child, 0, nchildren);
	}
	
	/**
	 * Searches for the first occurence of this node in the children list of the parent node.
	 * 
	 * @return the index of the first occurence of this node in the children list of the parent node;
	 * returns <code>-1</code> if there is no parent node.
	 */
	public short getParentIndex() {
		if (parent == null)
			return -1;
		else
			return parent.indexOf(this);
	}
	
	/**
	 * Indicates if this node is a descendant of the specified node.
	 * 
	 * @param node a node.
	 * @return <code>true</code> if this node is a descendant of <code>node</code>; <code>false</code>
	 * otherwise.
	 */
	public boolean isDescendOf(Node node) {
		Node n = this;
		while (n != null) {
			if (n == node)
				return true;
			n = n.parent;
		}
		return false;
	}

	/**
	 * Finds the lowest common ancestor of this node and the specified node.
	 * 
	 * @param node a node with which this node has a common ancestor.
	 * @return the lowest common ancestor of this node and the given node; <code>null</code> if none
	 * exists.
	 */
	public Node lca(Node node) {
		// not efficient but easy to code
		for (Node n1 = this; n1 != null; n1 = n1.parent)
			for (Node n2 = node; n2 != null; n2 = n2.parent)
				if (n1 == n2)
					return n1;
		return null;
	}
	
	///
	/// Find descendants of various types
	///
	
	private static abstract class Predicate {
		public abstract boolean apply(Node node);
	}
	
	/**
	 * Returns an array containing all descendants of this node that satisfy the specified condition,
	 * listed in pre-order.
	 *  
	 * @param p a predicate.
	 * @return an array containing all descendants <code>d</code> of this node for which 
	 * <code>p.apply(d)</code> is true.
	 */
	private Node[] getDescends(Predicate p) {
		ArrayList list = new ArrayList();
		getDescends(p, list);
		return (Node[]) list.toArray(new Node[0]);
	}
	private void getDescends(Predicate p, ArrayList list) {
		boolean b = p.apply(this);
		if (b)
			list.add(this);
		for (short i = 0; i < nchildren; ++i)
			children[i].getDescends(p, list);
	}
	/**
	 * Returns the number of descendants of this node that satisfy the specified condition.
	 *  
	 * @param p a predicate.
	 * @return the number of descendants <code>d</code> of this node for which <code>p.apply(d)</code> 
	 * is true.
	 */
	private short countDescends(Predicate p) {
		boolean b = p.apply(this);
		short count = (short) ((b) ? 1 : 0);
		for (short i = 0; i < nchildren; ++i)
			count += children[i].countDescends(p);
		return count;
	}

	/**
	 * Returns an array containing all descendants of this node, listed in pre-order.
	 * 
	 * @return an array containing all descendants of this node.
	 */
	public Node[] getDescends() {
		return getDescends(_true);
	}
	/**
	 * Returns the number of descendants of this node.
	 * 
	 * @return the number of descendants of this node.
	 */
	public short countDescends() {
		return countDescends(_true);
	}
	private static Predicate _true = new Predicate() {
		public boolean apply(Node node) { return true; }
	};

	/**
	 * Returns an array containing all descendants of this node that are leaves (i.e.&nbsp;having no children).
	 * 
	 * @return an array containing all descendants of this node that are leaves.
	 */
	public Node[] getLeaves() {
		return getDescends(_isLeaf);
	}
	/**
	 * Returns the number of descendants of this node that are leaves (i.e.&nbsp;having no children).
	 * 
	 * @return the number of descendants of this node that are leaves.
	 */
	public short countLeaves() {
		return countDescends(_isLeaf);
	}
	private static Predicate _isLeaf = new Predicate() {
		public boolean apply(Node node) { return !node.hasChildren(); }
	};

	/**
	 * Returns an array containing all descendants of this node whose symbol is of the specified type.
	 * 
	 * @param type a subclass of <code>Symbol</code>.
	 * @return an array containing all descendants of this node whose symbol is of the specified type.
	 */
	public Node[] getDescends(Class type) {
		return getDescends(new TypePredicate(type));
	}
	/**
	 * Returns the number of descendants of this node whose symbol is of the specified type.
	 * 
	 * @param type a subclass of <code>Symbol</code>.
	 * @return the number of descendants of this node whose symbol is of the specified type.
	 */
	public short countDescends(Class type) {
		return countDescends(new TypePredicate(type));
	}
	private static class TypePredicate extends Predicate {
		private Class type;
		public TypePredicate(Class type) { this.type = type; }
		public boolean apply(Node node) { return type.isInstance(node.sym); }
	}
	
	///
	/// String representations
	///
	
	/**
	 * Returns the string representation of the tree rooted at this node.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (nchildren > 0)
			sb.append("( ");
		sb.append(sym);
		for (short i = 0; i < nchildren; ++i) {
			sb.append(' ');
			sb.append(children[i].toString());
		}
		if (nchildren > 0)
			sb.append(" )");
		return sb.toString();
	}
	
	/**
	 * Returns a pretty string representation of the tree rooted at this node.  Note that this
	 * representation cannot be used by the <code>read</code> method to recover the original tree.
	 * 
	 * @return a pretty string representation of the tree rooted at this node.
	 */
	public String toPrettyString() {
		StringBuffer sb = new StringBuffer();
		pretty(this, sb, 0, true);
		return sb.toString();
	}
	
	private static void pretty(Node node, StringBuffer sb, int offset, boolean firstLine) {
		if (!firstLine)
			sb.append('\n');
		for (int i = 0; i < offset; ++i)
			sb.append(' ');
		sb.append(node.sym);
		for (short i = 0; i < node.nchildren; ++i)
			pretty(node.children[i], sb, offset+2, false);
	}
	
	/** Indicates if all trees subsequently read are NL syntactic parses. */
	public static boolean readSyn = false;
	
	/**
	 * Returns a tree that part of the given line of text represents.  Beginning with the token at the 
	 * specified index, this method finds the shortest substring of the line that is the string 
	 * representation of a tree.  If such a substring exists, then the tree that it represents is
	 * returned, and the <code>index</code> argument is set to the token index immediately after the 
	 * end of the substring.  Otherwise, <code>null</code> is returned and the <code>index</code> 
	 * argument remains unchanged.
	 * 
	 * @param line a line of text containing the string representation of a tree.
	 * @param index the beginning token index to consider; it is also an <i>output</i> variable for 
	 * the token index immediately after the end of the consumed substring.
	 * @return the tree that the substring <code>line[index, i)</code> represents, for the smallest 
	 * <code>i</code> possible; <code>null</code> if no such <code>i</code> exists.
	 */
	public static Node read(String[] line, Int index) {
		int i = index.val;
		if (i >= line.length)
			return null;
		if (line[i].equals("(")) {
			++i;
			Symbol sym;
			if (readSyn)
				Terminal.readWords = false;
			if (i == line.length || (sym = Symbol.read(line[i])) == null)
				return null;
			Node n = new Node(sym);
			for (++i; i < line.length && !line[i].equals(")");) {
				Int idx = new Int(i);
				Node child = read(line, idx);
				if (child == null)
					return null;
				n.addChild(child);
				i = idx.val;
			}
			if (i == line.length)
				return null;
			index.val = i+1;
			return n;
		} else {
			if (readSyn)
				Terminal.readWords = true;
			Symbol sym = Symbol.read(line[i]);
			if (sym == null)
				return null;
			index.val = i+1;
			return new Node(sym);
		}
	}
	
}
