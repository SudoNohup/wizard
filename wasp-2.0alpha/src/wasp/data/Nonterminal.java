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

import java.util.StringTokenizer;

import wasp.util.Arrays;
import wasp.util.Short;

/**
 * The class for nonterminal symbols.
 * 
 * @author ywwong
 *
 */
public class Nonterminal extends Symbol {

	private short index;
	private Variable[] args;
	
	public Nonterminal(String str) {
		super(Dictionary.nonterm(str));
		index = 0;
		args = null;
	}
	
	public Nonterminal(int id) {
		super(id);
		index = 0;
		args = null;
	}
	
	public Nonterminal(Nonterminal n, VariableAssignment assign) {
		this(n.id, n.index, null);
		if (n.args != null) {
			args = new Variable[n.args.length];
			for (short i = 0; i < n.args.length; ++i) {
				args[i] = assign.get(n.args[i]);
				if (args[i] == null)
					throw new RuntimeException();
			}
		}
	}
	
	private Nonterminal(int id, short index, Variable[] args) {
		super(id);
		this.index = index;
		this.args = args;
	}

	public boolean equals(Object o) {
		if (o instanceof Nonterminal) {
			Nonterminal n = (Nonterminal) o;
			if (id != n.id || index != n.index)
				return false;
			return (args==null) ? (n.args==null) : Arrays.equal(args, n.args);
		}
		return false;
	}
	
	public boolean matches(Symbol sym) {
		return sym instanceof Nonterminal && id == sym.id;
	}
	
	public int hashCode() {
		return id+1259;
	}
	
	public Object copy() {
		return new Nonterminal(id, index, (args==null) ? null : (Variable[]) args.clone());
	}

	/**
	 * Returns the index of this symbol which indicates its association with other symbols in an SCFG 
	 * rule.
	 */
	public short getIndex() {
		return index;
	}
	
	/**
	 * Assigns an index to this symbol which indicates its association with other symbols in an SCFG 
	 * rule.
	 */
	public void setIndex(short index) {
		this.index = index;
	}

	public void removeIndex() {
		index = 0;
	}
	
	/**
	 * Indicates if this symbol can associate with other symbols in an SCFG rule.
	 */
	public boolean isIndexable() {
		return true;
	}

	public short countArgs() {
		return (args==null) ? 0 : (short) args.length;
	}
	
	public Variable[] getArgs() {
		return (args==null) ? new Variable[0] : args;
	}
	
	public Variable getArg(short i) {
		return args[i];
	}
	
	public void addArg(Variable var) {
		if (args == null) {
			args = new Variable[1];
			args[0] = var;
		} else
			args = (Variable[]) Arrays.append(args, var);
	}
	
	public void setArgs(Variable[] vars) {
		if (vars == null || vars.length == 0)
			args = null;
		else
			args = (Variable[]) vars.clone();
	}
	
	public void removeArgs() {
		args = null;
	}
	
	///
	/// String representations
	///
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("*n:");
		sb.append(Dictionary.nonterm(id));
		if (index > 0) {
			sb.append('#');
			sb.append(index);
		}
		if (args != null) {
			sb.append('(');
			for (short i = 0; i < args.length; ++i) {
				if (i > 0)
					sb.append(',');
				sb.append(args[i]);
			}
			sb.append(')');
		}
		return sb.toString();
	}
	
	public static Symbol read(String token) {
		if (!token.startsWith("*n:"))
			return null;
		token = token.substring(3);
		int pound = token.indexOf('#');
		int lparen = token.indexOf('(');
		int rparen = token.indexOf(')');
		String argStr = null;
		if (lparen > 0 && rparen > lparen) {
			argStr = token.substring(lparen+1, rparen);
			token = token.substring(0, lparen);
		}
		String indexStr = null;
		if (pound > 0) {
			indexStr = token.substring(pound+1);
			token = token.substring(0, pound);
		}
		Nonterminal n = new Nonterminal(token);
		if (indexStr != null)
			n.setIndex(Short.parseShort(indexStr));
		if (argStr != null) {
			StringTokenizer toks = new StringTokenizer(argStr, ",");
			while (toks.hasMoreTokens())
				n.addArg((Variable) Variable.read(toks.nextToken()));
		}
		return n;
	}
	
}
