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

/**
 * Special symbols for dealing with anaphora.  These symbols can be useful as in the following:
 * <blockquote>
 * <p>
 * <i>If player 5 has the ball then <b><u>he</u></b> should pass to player 8 or 11.</i>
 * <p>
 * <code>((bowner (player our {5})) (do <b><u>(player our {5})</u></b> (pass (player our {8
 * 11}))))</code>
 * </blockquote>
 * <p>
 * Here the meaning of the pronoun <i>he</i> depends on the meaning of the previous NP <i>player 5</i>.
 * Both phrases refer to player 5 in our team.
 * <p>
 * The <code>Anaphor</code> symbols allow WASP to handle such kind of coreferences.  Like terminal
 * symbols, <code>Anaphor</code> symbols appear on the RHS of a rule.  If an <code>Anaphor</code>
 * symbol appears on the RHS of a rule, it has to be the only symbol there.  All <code>Anaphor</code>
 * symbols are typed, and the types correspond to the nonterminals in the MRL grammar.  If there is an
 * <code>Anaphor</code> of type <i>X</i> in an MR parse tree, then it will be expanded into the MR that
 * corresponds to the closest preceding NL phrase of type <i>X</i> (an NL phrase is of type <i>X</i> if
 * the root of its parse tree is labeled with <i>X</i>).  If there is no such NL phrase, then the
 * <code>Anaphor</code> is said to be <i>unresolved</i>, and the MR will be considered invalid.
 * <p>
 * For training, the above MR would be transformed into:
 * <blockquote>
 * <p>
 * <code>((bowner (player our {5})) (do <b><u>*^:Player</u></b> (pass (player our {8 11}))))</code>
 * </blockquote>
 * <p>
 * Here <code>*^:Player</code> is an <code>Anaphor</code> symbol of type <code>Player</code>.
 * <p>
 * There is code that automatically replaces certain MR expressions with <code>Anaphor</code> symbols
 * for training.  See <tt>GIZAPlusPlus.createAnaphora()</tt> for more detail.
 *  
 * @see wasp.align.GIZAPlusPlus#createAnaphora()
 * @author ywwong
 *
 */
public class Anaphor extends Symbol {

	private Nonterminal type;
	
	public Anaphor(Nonterminal nonterm) {
		this.type = nonterm;
	}
	
	public boolean equals(Object o) {
		return o instanceof Anaphor && type.equals(((Anaphor) o).type);
	}

	public int hashCode() {
		return type.getId()+1279;
	}

	public Object copy() {
		return new Anaphor(type);
	}

	public boolean matches(Symbol sym) {
		return equals(sym);
	}

	public int getId() {
		return type.getId();
	}
	
	public Nonterminal getType() {
		return type;
	}
	
	///
	/// String representations
	///
	
	public String toString() {
		String token = type.toString();
		return "*^:"+token.substring(3);
	}

	public static Symbol read(String token) {
		if (!token.startsWith("*^:"))
			return null;
		Nonterminal type = (Nonterminal) Nonterminal.read("*n:"+token.substring(3));
		return new Anaphor(type);
	}
	
}
