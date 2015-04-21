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

import wasp.data.Nonterminal;
import wasp.data.Symbol;
import wasp.data.Terminal;
import wasp.util.Arrays;
import wasp.util.Int;

/**
 * The MRL part of an SCFG or lambda-SCFG rule.
 * 
 * @author ywwong
 *
 */
public class PartialRule {
	
	protected Nonterminal lhs;
	protected Symbol[] rhs;
	protected short nargs;
	
	public PartialRule(Rule rule) {
		lhs = rule.getLhs();
		rhs = rule.getF();
		nargs = rule.countArgs();
	}
	
	protected PartialRule(Nonterminal lhs, Symbol[] rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
		nargs = (short) Arrays.countInstances(rhs, Nonterminal.class);
	}
	
	public boolean equals(Object o) {
		if (o == this)
			return true;
		return o instanceof PartialRule && lhs.equals(((PartialRule) o).lhs)
		&& Arrays.equal(rhs, ((PartialRule) o).rhs);
	}
	
	public int hashCode() {
		int hash = 1;
		hash = 31*hash + lhs.hashCode();
		hash = 31*hash + Arrays.hashCode(rhs);
		return hash;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(lhs);
		sb.append(" -> ({");
		for (short i = 0; i < rhs.length; ++i) {
			sb.append(' ');
			sb.append(rhs[i]);
		}
		sb.append(" })");
		return sb.toString();
	}
	
	public short countArgs() {
		return nargs;
	}
	
	public static PartialRule read(String[] line, Int index) {
		int i = index.val;
		Nonterminal lhs;
		if (i >= line.length || (lhs = (Nonterminal) Nonterminal.read(line[i])) == null)
			return null;
		++i;
		if (i == line.length || !line[i].equals("->"))
			return null;
		++i;
		if (i == line.length || !line[i].equals("({"))
			return null;
		Terminal.readWords = false;
		ArrayList list = new ArrayList();
		for (++i; i < line.length && !line[i].equals("})"); ++i) {
			Symbol sym = Symbol.read(line[i]);
			if (sym == null)
				return null;
			list.add(sym);
		}
		if (i >= line.length)
			return null;
		Symbol[] rhs = (Symbol[]) list.toArray(new Symbol[0]);
		index.val = i+1;
		return new PartialRule(lhs, rhs);
	}
	
}