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
import wasp.util.Matrices;

/**
 * Word alignments with many-to-many links.
 * 
 * @author ywwong
 *
 */
public class NToNWordAlign extends WordAlign {

	private boolean[][] a;
	private Link[] headsE;
	private short[] fertsE;
	private Link[] headsF;
	private short[] fertsF;

	private NToNWordAlign(Symbol[] E, Node[] F, double score, double[] wE, double[] wF) {
		super(E, F, score, wE, wF);
		a = new boolean[E.length][F.length];
		headsE = new Link[E.length];
		fertsE = new short[E.length];
		headsF = new Link[F.length];
		fertsF = new short[F.length];
	}
	
	public NToNWordAlign(Symbol[] E, Node[] F, double score) {
		this(E, F, score, null, null);
	}
	
	public NToNWordAlign(Symbol[] E, Node[] F) {
		this(E, F, 0);
	}
	
	protected WordAlign createNew(Symbol[] E, Node[] F, double score, double[] wE, double[] wF) {
		return new NToNWordAlign(E, F, score, wE, wF);
	}
	
	protected WordAlign createNew(Symbol[] E, Node[] F, double score) {
		return new NToNWordAlign(E, F, score);
	}
	
	public boolean equals(Object o) {
		if (o instanceof NToNWordAlign) {
			NToNWordAlign align = (NToNWordAlign) o;
			return E == align.E && F == align.F && Matrices.equal(a, align.a);
		}
		return false;
	}
	
	public int hashCode() {
		return Matrices.hashCode(a);
	}
	
	public boolean isLinked(short e, short f) {
		return a[e][f];
	}
	
	public Link getFirstLinkFromE(short e) {
		return headsE[e];
	}
	
	public short countLinksFromE(short e) {
		return fertsE[e];
	}
	
	public Link getFirstLinkFromF(short f) {
		return headsF[f];
	}
	
	public short countLinksFromF(short f) {
		return fertsF[f];
	}
	
	protected void addNewLink(Link link) {
		if (a[link.e][link.f])
			return;
		a[link.e][link.f] = true;
		++fertsE[link.e];
		++fertsF[link.f];
		if (headsE[link.e] == null) {
			link.back = null;
			link.next = null;
			headsE[link.e] = link;
		} else if (headsE[link.e].f > link.f) {
			link.back = null;
			link.next = headsE[link.e];
			headsE[link.e].back = link;
			headsE[link.e] = link;
		} else {
			Link back = headsE[link.e];
			while (back.next != null && back.next.f < link.f)
				back = back.next;
			link.back = back;
			link.next = back.next;
			back.next = link;
			if (link.next != null)
				link.next.back = link;
		}
		link = (Link) link.clone();
		if (headsF[link.f] == null) {
			link.back = null;
			link.next = null;
			headsF[link.f] = link;
		} else if (headsF[link.f].e > link.e) {
			link.back = null;
			link.next = headsF[link.f];
			headsF[link.f].back = link;
			headsF[link.f] = link;
		} else {
			Link back = headsF[link.f];
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
		if (!a[link.e][link.f])
			return;
		a[link.e][link.f] = false;
		--fertsE[link.e];
		--fertsF[link.f];
		if (headsE[link.e].f == link.f) {
			headsE[link.e] = headsE[link.e].next;
			if (headsE[link.e] != null)
				headsE[link.e].back = null;
		} else {
			Link back = headsE[link.e];
			while (back.next.f != link.f)
				back = back.next;
			Link next = back.next.next;
			if (next != null)
				next.back = back;
			back.next = next;
		}
		if (headsF[link.f].e == link.e) {
			headsF[link.f] = headsF[link.f].next;
			if (headsF[link.f] != null)
				headsF[link.f].back = null;
		} else {
			Link back = headsF[link.f];
			while (back.next.e != link.e)
				back = back.next;
			Link next = back.next.next;
			if (next != null)
				next.back = back;
			back.next = next;
		}
	}
	
	public void removeLinksFromE(short e) {
		Link link = headsE[e];
		while (link != null) {
			removeLink(link);
			link = link.next;
		}
	}
	
	public void removeLinksFromF(short f) {
		Link link = headsF[f];
		while (link != null) {
			removeLink(link);
			link = link.next;
		}
	}
	
}
