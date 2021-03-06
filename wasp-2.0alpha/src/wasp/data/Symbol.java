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

import wasp.util.Copyable;
import wasp.util.Int;

/**
 * The abstract class for all kinds of symbols.  There are two main kinds of symbols, terminals and
 * nonterminals.  These are building blocks for sentences, meaning representations, and synchronous
 * grammar rules.
 * 
 * @author ywwong
 *
 */
public abstract class Symbol implements Copyable {

	protected int id;
	
	protected Symbol() {
		id = 0;
	}
	
	protected Symbol(int id) {
		this.id = id;
	}
	
	/**
	 * Indicates if this symbol is equal to the specified object.  The precise definition of
	 * equality depends on particular types of symbols, but two symbols cannot be equal if they
	 * are of different types.
	 */
	public abstract boolean equals(Object o);

	/**
	 * Indicates if the string representation of this symbol is equal to the specified string.
	 * 
	 * @param str the string to compare.
	 * @return <code>true</code> if the the string representation of this symbol is equal to the
	 * <code>str</code> argument; <code>false</code> otherwise.
	 */
	public boolean stringEquals(String str) {
		return toString().equals(str);
	}
	
	/**
	 * Indicates if this symbol matches the specified symbol.  This method allows for a <i>softer</i>
	 * match than the <code>equals</code> method.  For example, given a wildcard and a symbol that it
	 * matches, <code>matches</code> would return <code>true</code> but <code>equals</code> would
	 * return <code>false</code>.
	 * 
	 * @param sym the symbol with which to compare.
	 * @return <code>true</code> if this symbol matches the <code>sym</code> argument; <code>false</code>
	 * otherwise.
	 */
	public abstract boolean matches(Symbol sym);
	
	/**
	 * Returns the hash code of this symbol. 
	 */
	public abstract int hashCode();

	/**
	 * Creates and returns a copy of this symbol.
	 * 
	 * @return a copy of this symbol.
	 */
	public abstract Object copy();

	/**
	 * Returns the ID of this symbol.  This ID can be used in equivalence tests.
	 * 
	 * @return the ID of this symbol.
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Returns the index of this symbol which indicates its association with other symbols in an SCFG
	 * rule.  By default, this method returns <code>0</code>, i.e. this symbol is not associated with any
	 * other symbols.  Subclasses may override this method to allow associations.
	 * 
	 * @return the index of this symbol.
	 */
	public short getIndex() {
		return 0;
	}

	/**
	 * Assigns an index to this symbol which indicates its association with other symbols in an SCFG
	 * rule.  By default, this method does nothing, i.e. this symbol cannot associate with any other
	 * symbols.  Subclasses may override this method to allow associations.
	 * 
	 * @param index the index to assign to this symbol.
	 */
	public void setIndex(short index) {}
	
	/**
	 * Removes the index of this symbol which indicates its association with other symbols in an SCFG
	 * rule.
	 */
	public void removeIndex() {}
	
	/**
	 * Indicates if this symbol can associate with other symbols in an SCFG rule.  By default, this
	 * method returns false, i.e. this symbol cannot associate with any other symbols.  Subclasses may
	 * override this method to allow associations.
	 *  
	 * @return <code>true</code> if this symbol can associate with other symbols in an SCFG rule;
	 * <code>false</code> otherwise.
	 */
	public boolean isIndexable() {
		return false;
	}
	
	///
	/// String representations
	///
	
	/**
	 * Returns the string representation of this symbol.
	 */
	public abstract String toString();
	
	/**
	 * Returns the symbol that the specified string token represents.  If the token is not a valid 
	 * string representation of any symbol, then <code>null</code> is returned.   
	 * 
	 * @param token the string representation of a symbol.
	 * @return the symbol that the <code>token</code> argument represents.
	 */
	public static Symbol read(String token) {
		Symbol sym;
		if ((sym = Anaphor.read(token)) != null)
			return sym;
		if ((sym = Nonterminal.read(token)) != null)
			return sym;
		if ((sym = Variable.read(token)) != null)
			return sym;
		if ((sym = wasp.mrl.ProductionSymbol.read(token)) != null)
			return sym;
		if ((sym = wasp.scfg.RuleSymbol.read(token)) != null)
			return sym;
		return Terminal.read(token);
	}
	
	/**
	 * Returns a symbol that part of the given line of text represents.  Beginning with the token at
	 * the specified index, this method finds the shortest substring of the line that is the string 
	 * representation of a symbol.  If such a substring exists, then the symbol that it represents is 
	 * returned, and the <code>index</code> argument is set to the token index immediately after the
	 * end of the substring.  Otherwise, <code>null</code> is returned and the <code>index</code> 
	 * argument remains unchanged.
	 * 
	 * @param line a line of text containing the string representation of a symbol.
	 * @param index the beginning token index to consider; it is also an <i>output</i> variable for 
	 * the token index immediately after the end of the consumed substring.
	 * @return the symbol that the substring <code>line[index, i)</code> represents, for the smallest
	 * <code>i</code> possible; <code>null</code> if no such <code>i</code> exists.
	 */
	public static Symbol read(String[] line, Int index) {
		Symbol sym = read(line[index.val]);
		if (sym == null)
			return null;
		else {
			++index.val;
			return sym;
		}
	}
	
}
