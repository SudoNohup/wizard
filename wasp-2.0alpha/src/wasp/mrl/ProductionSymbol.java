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

import wasp.data.Dictionary;
import wasp.data.Symbol;
import wasp.data.Terminal;
import wasp.util.Arrays;
import wasp.util.Int;

/**
 * Symbols for encapsulating production rules in an MRL grammar.  These symbols are used in MR parse 
 * trees and word alignments.
 * 
 * @author ywwong
 *
 */
public class ProductionSymbol extends Symbol {

	private Production prod;
	
	/**
	 * Creates a new symbol that encapsulates the specified production.
	 * 
	 * @param prod an interned production of the MRL grammar.
	 */
	public ProductionSymbol(Production prod) {
		this.prod = prod.intern();
	}
	
	private ProductionSymbol(Production prod, boolean dummy) {
		this.prod = prod;
	}
	
	public boolean equals(Object o) {
		return o instanceof ProductionSymbol && prod.equals(((ProductionSymbol) o).prod);
	}

	public boolean matches(Symbol sym) {
		return false;
	}
	
	public int hashCode() {
		return prod.hashCode();
	}

	public Object copy() {
		return new ProductionSymbol(prod, true);
	}

	/**
	 * Returns the production encapsulated in this symbol.
	 * 
	 * @return the production encapsulated in this symbol.
	 */
	public Production getProduction() {
		return prod;
	}

	///
	/// String representations
	///
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("*p:");
		sb.append(prod.toString().replaceAll("\\\\", "\\\\\\\\").replaceAll("_", "\\\\_").replace(' ', '_'));
		return sb.toString();
	}

	public String toConcise() {
		StringBuffer sb = new StringBuffer();
		Symbol[] rhs = prod.getRhs();
		for (short i = 0; i < rhs.length; ++i)
			if (rhs[i] instanceof Terminal) {
				String token = rhs[i].toString();
				if (Character.isLetterOrDigit(token.charAt(0))) {
					if (sb.length() > 0)
						sb.append('_');
					sb.append(token);
				}
			}
		if (sb.length() == 0)
			return Dictionary.nonterm(prod.getLhs().getId());
		return sb.toString();
	}
	
	public static Symbol read(String token) {
		if (!token.startsWith("*p:"))
			return null;
		StringBuffer sb = new StringBuffer();
		for (int i = 3; i < token.length(); ++i) {
			char c = token.charAt(i);
			if (c == '\\')
				sb.append(token.charAt(++i));
			else if (c == '_')
				sb.append(' ');
			else
				sb.append(c);
		}
		Production.readOrig = false;
		return new ProductionSymbol(Production.read(Arrays.tokenize(sb.toString()), new Int(0)));
	}
	
}
