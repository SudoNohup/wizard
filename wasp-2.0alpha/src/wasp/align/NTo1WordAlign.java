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

import wasp.data.Node;
import wasp.data.Symbol;
import wasp.util.Arrays;

/**
 * Word alignments where each NL word is linked to at most one MRL production.  Synchronous grammar
 * rules are extracted from these alignments.
 * 
 * @author ywwong
 *
 */
public class NTo1WordAlign extends WordAlign {

	private Link[] a;
	private Link[] heads;
	private short[] ferts;
	
	private NTo1WordAlign(Symbol[] E, Node[] F, double score, double[] wE, double[] wF) {
		super(E, F, score, wE, wF);
		a = new Link[E.length];
		heads = new Link[F.length];
		ferts = new short[F.length];
	}
	
	public NTo1WordAlign(Symbol[] E, Node[] F, double score) {
		this(E, F, score, null, null);
	}
	
	public NTo1WordAlign(Symbol[] E, Node[] F) {
		this(E, F, 0);
	}
	
	protected WordAlign createNew(Symbol[] E, Node[] F, double score, double[] wE, double[] wF) {
		return new NTo1WordAlign(E, F, score, wE, wF);
	}
	
	protected WordAlign createNew(Symbol[] E, Node[] F, double score) {
		return new NTo1WordAlign(E, F, score);
	}
	
	public boolean equals(Object o) {
		if (o instanceof NTo1WordAlign) {
			NTo1WordAlign align = (NTo1WordAlign) o;
			return E == align.E && F == align.F && Arrays.equal(a, align.a);
		}
		return false;
	}
	
	public int hashCode() {
		return Arrays.hashCode(a);
	}
	
	public boolean isLinked(short e, short f) {
		return a[e] != null && a[e].f == f;
	}
	
	public Link getFirstLinkFromE(short e) {
		return a[e];
	}
	
	public short countLinksFromE(short e) {
		return (short) ((a[e]==null) ? 0 : 1);
	}
	
	public Link getFirstLinkFromF(short f) {
		return heads[f];
	}
	
	public short countLinksFromF(short f) {
		return ferts[f];
	}
	
	/**
	 * Adds a new link from an NL word to an MR parse node (<code>f</code>).  If the NL word is already 
	 * linked to another MR parse node (<code>f'</code>), then no new link will be added, unless
	 * <code>f</code> is an ancestor of <code>f'</code> in the MR parse tree, in which case the new link
	 * will replace the old link.
	 */
	protected void addNewLink(Link link) {
		if (a[link.e] != null)
			return;
		a[link.e] = link;
		++ferts[link.f];
		if (heads[link.f] == null) {
			link.back = null;
			link.next = null;
			heads[link.f] = link;
		} else if (heads[link.f].e > link.e) {
			link.back = null;
			link.next = heads[link.f];
			heads[link.f].back = link;
			heads[link.f] = link;
		} else {
			Link back = heads[link.f];
			while (back.next != null && back.next.e < link.e)
				back = back.next;
			link.back = back;
			link.next = back.next;
			back.next = link;
			if (link.next != null)
				link.next.back = link;
		}
	}
	
	public void removeLink(Link link) {
		if (a[link.e] == null || a[link.e].f != link.f)
			return;
		a[link.e] = null;
		--ferts[link.f];
		if (heads[link.f].e == link.e) {
			heads[link.f] = heads[link.f].next;
			if (heads[link.f] != null)
				heads[link.f].back = null;
		} else {
			Link back = heads[link.f];
			while (back.next.e != link.e)
				back = back.next;
			Link next = back.next.next;
			if (next != null)
				next.back = back;
			back.next = next;
		}
	}
	
	public void removeLinksFromE(short e) {
		if (a[e] != null)
			removeLink(a[e]);
	}
	
	public void removeLinksFromF(short f) {
		Link link = heads[f];
		while (link != null) {
			removeLink(link);
			link = link.next;
		}
	}
	
}
