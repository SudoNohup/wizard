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
package wasp.align;

import java.util.ArrayList;
import java.util.HashMap;

import wasp.data.Node;
import wasp.data.Nonterminal;
import wasp.data.Symbol;
import wasp.data.Terminal;
import wasp.mrl.Production;
import wasp.mrl.ProductionSymbol;
import wasp.util.Arrays;
import wasp.util.CombineElements;
import wasp.util.Mask;

/**
 * A sentence pair along with links from NL words to MRL productions. 
 *  
 * @author ywwong
 *
 */
public abstract class WordAlign implements Comparable {

	protected Symbol[] E;
	protected Node[] F;
	protected double score;
	
	// for calculating lexical weights
	public double[] wE;
	public double[] wF;

	/**
	 * Creates an empty word alignment based on the specified NL sentence and linearized MR parse.
	 * 
	 * @param E an NL sentence.
	 * @param F a linearized MR parse.
	 * @param score the alignment score.
	 */
	protected WordAlign(Symbol[] E, Node[] F, double score, double[] wE, double[] wF) {
		this.E = E;
		this.F = F;
		this.score = score;
		this.wE = wE;
		this.wF = wF;
	}

	public abstract boolean equals(Object o);
	
	public abstract int hashCode();
	
	/**
	 * Word alignments with higher scores are ranked higher.  This ordering is <i>not</i> consistent
	 * with equals because distinct word alignments with equal scores should not be treated as equal.
	 */
	public int compareTo(Object o) {
		if (score > ((WordAlign) o).score)
			return -1;
		else if (score < ((WordAlign) o).score)
			return 1;
		else
			return 0;
	}
	
	/**
	 * Returns the NL sentence.
	 * 
	 * @return the NL sentence.
	 */
	public Symbol[] getE() {
		return E;
	}
	
	/**
	 * Returns the specified word in the NL sentence.
	 * 
	 * @param i a word index.
	 * @return the <code>i</code>-th NL word.
	 */
	public Symbol getE(short i) {
		return E[i];
	}
	
	/**
	 * Returns the linearized MR parse.
	 * 
	 * @return the linearized MR parse.
	 */
	public Node[] getF() {
		return F;
	}
	
	/**
	 * Returns the specified node in the linearized MR parse.
	 * 
	 * @param i a node index.
	 * @return the <code>i</code>-th MR parse node.
	 */
	public Node getF(short i) {
		return F[i];
	}
	
	/**
	 * Returns the position of the given node in the linearized MR parse.
	 * 
	 * @param node a node in the linearized MR parse.
	 * @return the position of the given node.
	 */
	public short getF(Node node) {
		return (short) Arrays.indexOf(F, node);
	}
	
	/**
	 * Returns the length of the NL sentence.
	 * 
	 * @return the length of the NL sentence.
	 */
	public short lengthE() {
		return (short) E.length;
	}
	
	/**
	 * Returns the length of the linearized MR parse.
	 * 
	 * @return the length of the linearized MR parse.
	 */
	public short lengthF() {
		return (short) F.length;
	}
	
	/**
	 * Returns the score of this alignment.
	 * 
	 * @return the score of this alignment.
	 */
	public double getScore() {
		return score;
	}
	
	/**
	 * Assign a score to this alignment.
	 * 
	 * @param score the score to assign.
	 */
	public void setScore(double score) {
		this.score = score;
	}
	
	/**
	 * Indicates if the specified NL word is linked to the specified node in the MR parse.
	 * 
	 * @param e a word index.
	 * @param f a node index.
	 * @return <code>true</code> if the <code>e</code>-th NL word is linked to the <code>f</code>-th 
	 * node in the MR parse; <code>false</code> otherwise.
	 */
	public abstract boolean isLinked(short e, short f);
	
	/**
	 * Returns the first link from the specified NL word in this alignment, if any.
	 * 
	 * @param e a word index.
	 * @return the first link from the <code>e</code>-th NL word in this alignment; <code>null</code> if
	 * none exists.
	 */
	public abstract Link getFirstLinkFromE(short e);
	
	/**
	 * Returns the number of links from the specified NL word in this alignment.
	 * 
	 * @param e a word index.
	 * @return the number of links from the <code>e</code>-th NL word in this alignment.
	 */
	public abstract short countLinksFromE(short e);
	
	/**
	 * Indicates if the specified NL word is linked in this alignment.
	 * 
	 * @param e a word index.
	 * @return <code>true</code> if the <code>e</code>-th NL word is linked in this alignment;
	 * <code>false</code> otherwise.
	 */
	public boolean isLinkedFromE(short e) {
		return getFirstLinkFromE(e) != null;
	}
	
	/**
	 * Returns the first link from the specified MR parse node in this alignment, if any.
	 * 
	 * @param f a node index.
	 * @return the link from the <code>f</code>-th MR parse node in this alignment; <code>null</code> if 
	 * none exists.
	 */
	public abstract Link getFirstLinkFromF(short f);
	
	/**
	 * Returns the number of links from the specified MR parse node in this alignment.
	 * 
	 * @param f a node index.
	 * @return the number of links from the <code>f</code>-th MR parse node in this alignment.
	 */
	public abstract short countLinksFromF(short f);
	
	/**
	 * Indicates if the specified MR parse node is linked in this alignment.
	 * 
	 * @param f a node index.
	 * @return <code>true</code> if the <code>f</code>-th MR parse node is linked in this alignment;
	 * <code>false</code> otherwise.
	 */
	public boolean isLinkedFromF(short f) {
		return getFirstLinkFromF(f) != null;
	}
	
	/**
	 * Adds a new link to this alignment.
	 * 
	 * @param e the index of the word to link.
	 * @param f the index of the MR parse node to link.
	 * @param strength the strength of the new link.
	 */
	public void addLink(short e, short f, double strength) {
		addNewLink(new Link(this, e, f, strength));
	}
	
	/**
	 * Adds a new link to this alignment, with the link strength equal to one. 
	 * 
	 * @param e the index of the word to link.
	 * @param f the index of the MR parse node to link.
	 */
	public void addLink(short e, short f) {
		addNewLink(new Link(this, e, f, 1));
	}
	
	/**
	 * Adds a copy of the specified link to this alignment.
	 * 
	 * @param link the link to add.
	 */
	public void addLink(Link link) {
		addNewLink(new Link(this, link.e, link.f, link.strength));
	}

	/**
	 * Adds the specified link to this alignment.
	 * 
	 * @param link the link to add.
	 */
	protected abstract void addNewLink(Link link);
	
	/**
	 * Removes the specified link from this alignment.  If the link does not exist, then no links will
	 * be removed.
	 * 
	 * @param link the link to remove.
	 */
	public abstract void removeLink(Link link);
	
	/**
	 * Removes all links from the specified NL word.
	 * 
	 * @param e a word index.
	 */
	public abstract void removeLinksFromE(short e);
	
	/**
	 * Removes all links from the specified MR parse node.
	 * 
	 * @param f a node index.
	 */
	public abstract void removeLinksFromF(short f);
	
	/**
	 * Returns the string representation of this alignment.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (short i = 0; i < E.length; ++i) {
			if (i > 0)
				sb.append(' ');
			sb.append('(');
			sb.append(i);
			sb.append(')');
			sb.append(E[i]);
		}
		sb.append('\n');
		for (short i = 0; i < F.length; ++i) {
			if (i > 0)
				sb.append(' ');
			sb.append(((ProductionSymbol) F[i].getSymbol()).toConcise());
			sb.append(" ({");
			for (Link link = getFirstLinkFromF(i); link != null; link = link.next) {
				sb.append(' ');
				sb.append(link.e);
			}
			sb.append(" })");
		}
		return sb.toString();
	}
	
	///
	/// Transformations used in word-alignment models
	///
	
	protected abstract WordAlign createNew(Symbol[] E, Node[] F, double score, double[] wE, double[] wF);
	protected abstract WordAlign createNew(Symbol[] E, Node[] F, double score);
	
	/**
	 * Restores the words in the NL sentence that have been removed.
	 * 
	 * @param E the NL sentence with all removed words restored.
	 * @param Emask the mask that has been applied to the NL sentence.
	 * @return a new word alignment with all removed words restored.
	 */
	public WordAlign unmaskE(Terminal[] E, Mask Emask) {
		WordAlign align = createNew(E, F, score);
		for (short i = 0; i < lengthE(); ++i) {
			Link link = getFirstLinkFromE(i);
			while (link != null) {
				align.addLink(Emask.toLong(link.e), link.f, link.strength);
				link = link.next;
			}
		}
		return align;
	}
	
	/**
	 * Restores the MR parse nodes in a word alignment that have been removed.
	 * 
	 * @param F the linearized MR parse with all removed nodes restored.
	 * @param Fmask the mask that has been applied to the linearized MR parse.
	 * @return a new word alignment with all removed MR parse nodes restored.
	 */
	public WordAlign unmaskF(Node[] F, Mask Fmask) {
		WordAlign align = createNew(E, F, score);
		for (short i = 0; i < lengthE(); ++i) {
			Link link = getFirstLinkFromE(i);
			while (link != null) {
				align.addLink(link.e, Fmask.toLong(link.f), link.strength);
				link = link.next;
			}
		}
		return align;
	}
	
	/**
	 * Combines the specified words in the NL sentence.  All links to the original phrase are removed.
	 * This method returns a new word alignment in which the specified words are combined.  A new
	 * <code>Combine</code> object is appended to the list <code>Ecomb</code> so that the combination
	 * of words can be undone later.
	 * 
	 * @param from the index of the first word to combine.
	 * @param len the number of words to combine.
	 * @param Ecomb an <i>output</i> list of <code>Combine</code> objects for keeping track of the words
	 * that have been combined.
	 * @return a new word alignment in which the specified words are combined.
	 */
	public WordAlign combineE(short from, short len, ArrayList Ecomb) {
		Terminal[] E = (Terminal[]) Arrays.replace(this.E, from, from+len, combineTerms(from, len));
		WordAlign align = createNew(E, F, score);
		CombineElements comb = new CombineElements(from, len);
		Ecomb.add(comb);
		for (short i = 0; i < lengthE(); ++i) {
			Link link = getFirstLinkFromE(i);
			while (link != null) {
				if (i < from || i >= from+len)
					align.addLink(comb.toShort(i), link.f, link.strength);
				link = link.next;
			}
		}
		return align;
	}
	
	private Terminal combineTerms(short from, short len) {
		StringBuffer sb = new StringBuffer();
		for (short i = 0; i < len; ++i) {
			sb.append('_');
			sb.append(E[i+from]);
		}
		return new Terminal(sb.toString(), true);
	}
	
	/**
	 * Separates the NL words that have been combined using the <code>NToNWordAlign.combineE()</code>
	 * method.  All links to the combined words are replicated to cover all individual words.  This method 
	 * returns a new alignment in which the combined words are separated.
	 * 
	 * @param E the NL sentence with all combined words separated.
	 * @param Ecomb a list of <code>Combine</code> objects that keep track of the words that have been
	 * combined.
	 * @return a new word alignment in which the combined words are separated.
	 */
	public WordAlign separateE(Terminal[] E, ArrayList Ecomb) {
		if (Ecomb.isEmpty())
			return this;
		WordAlign align = createNew(E, F, score);
		for (short i = 0; i < lengthE(); ++i) {
			Link link = getFirstLinkFromE(i);
			while (link != null) {
				short[] e = new short[1];
				e[0] = i;
				for (short j = (short) (Ecomb.size()-1); j >= 0; --j) {
					CombineElements comb = (CombineElements) Ecomb.get(j);
					e = comb.toLong(e);
				}
				for (short j = 0; j < e.length; ++j)
					align.addLink(e[j], link.f, link.strength);
				link = link.next;
			}
		}
		return align;
	}
	
	///
	/// Transformations used in rule extractors
	///
	
	public void initw() {
		wE = new double[E.length];
		wF = new double[F.length];
	}
	
	public WordAlign mapF(Node[] F, HashMap Fmap) {
		double[] wF = new double[F.length];
		for (short i = 0; i < lengthF(); ++i) {
			Node n = (Node) Fmap.get(this.F[i]);
			if (n == null)
				continue;
			short f = (short) Arrays.indexOf(F, n);
			wF[f] = this.wF[i];
		}
		WordAlign align = createNew(E, F, score, wE, wF);
		for (short i = 0; i < lengthF(); ++i) {
			Node n = (Node) Fmap.get(this.F[i]);
			if (n == null)
				continue;
			short f = (short) Arrays.indexOf(F, n);
			Link link = getFirstLinkFromF(i);
			while (link != null) {
				align.addLink(link.e, f, link.strength);
				link = link.next;
			}
		}
		return align;
	}
	
	/**
	 * Replaces the specified phrase in the NL sentence with the given nonterminal.  All links to the 
	 * original phrase are removed.  This method returns a new alignment in which the specified phrase is 
	 * replaced.
	 * 
	 * @param from the beginning index of the phrase to replace.
	 * @param to the end index (exclusive) of the phrase to replace.
	 * @param replacement the replacement symbol.
	 * @return a new word alignment in which the specified phrase is replaced.
	 */
	public WordAlign replaceE(short from, short to, Nonterminal replacement) {
		Symbol[] E = this.E;
		if (E instanceof Terminal[])
			E = toSymbolArray(E);
		E = (Symbol[]) Arrays.replace(E, from, to, replacement);
		double[] wE = Arrays.replace(this.wE, from, to, 1);
		WordAlign align = createNew(E, F, score, wE, wF);
		for (short i = 0; i < lengthE(); ++i) {
			Link link = getFirstLinkFromE(i);
			while (link != null) {
				if (i < from)
					align.addLink(i, link.f, link.strength);
				else if (i >= to)
					align.addLink((short) (i-(to-from)+1), link.f, link.strength);
				link = link.next;
			}
		}
		return align;
	}
	
	private Symbol[] toSymbolArray(Symbol[] array) {
		Symbol[] a = new Symbol[array.length];
		for (short i = 0; i < array.length; ++i)
			a[i] = array[i];
		return a;
	}
	
	/**
	 * Combines the specified node in the MR parse tree with its parent.  All links to the specified node 
	 * are transferred to its parent.  This method returns a new alignment in which the specified node is 
	 * combined with its parent.  If the specified node has no parent, then the input alignment will be
	 * returned.
	 * 
	 * @param f the index of the MR parse node to combine with its parent.
	 * @return a new word alignment in which the specified node is combined with its parent. 
	 */
	public WordAlign combineF(short f) {
		if (!F[f].hasParent())
			return this;
		Node[] F = this.F[0].deepCopy().getDescends();
		short parent = (short) Arrays.indexOf(F, F[f].getParent());
		Production prod = ((ProductionSymbol) F[parent].getSymbol()).getProduction();
		Production arg = ((ProductionSymbol) F[f].getSymbol()).getProduction();
		short argIndex = F[parent].indexOf(F[f]);
		Node n = new Node(new ProductionSymbol(new Production(prod, arg, argIndex)));
		for (short i = 0; i < argIndex; ++i)
			n.addChild(F[parent].getChild(i).deepCopy());
		for (short i = 0; i < F[f].countChildren(); ++i)
			n.addChild(F[f].getChild(i).deepCopy());
		for (short i = (short) (argIndex+1); i < F[parent].countChildren(); ++i)
			n.addChild(F[parent].getChild(i).deepCopy());
		if (F[parent].hasParent()) {
			F[parent].getParent().replaceChild(F[parent], n);
			F = F[0].getDescends();
		} else
			F = n.getDescends();
		double[] wF = Arrays.remove(this.wF, f);
		wF[parent] *= this.wF[f];
		WordAlign align = createNew(E, F, score, wE, wF);
		for (short i = 0; i < lengthE(); ++i) {
			Link link = getFirstLinkFromE(i);
			while (link != null) {
				if (link.f < f)
					align.addLink(link.e, link.f, link.strength);
				else if (link.f == f)
					align.addLink(link.e, parent, link.strength);
				else
					align.addLink(link.e, (short) (link.f-1), link.strength);
				link = link.next;
			}
		}
		return align;
	}
	
}
