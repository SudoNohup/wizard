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

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import wasp.data.Anaphor;
import wasp.data.Node;
import wasp.data.Nonterminal;
import wasp.data.Symbol;
import wasp.data.Terminal;
import wasp.data.Variable;
import wasp.data.VariableAssignment;
import wasp.main.Config;
import wasp.util.Arrays;
import wasp.util.Int;

/**
 * Production rules of MRL grammars.
 * 
 * @author ywwong
 *
 */
public class Production {

	private static final Logger logger = Logger.getLogger(Production.class.getName());
	static {
		logger.setLevel(Level.INFO);
	}
	
	/** The LHS nonterminal. */
	private Nonterminal lhs;
	/** The RHS string. */
	private Symbol[] rhs;
	/** Indicates if this production is in the original, unambiguous MRL grammar. */
	private boolean orig;
	
	/** Indicates if this production is an associative-commutative operator. */
	private boolean ac;
	/** For associative-commutative operators: the first RHS symbol to repeat when there are more than
	 * two arguments. */
	private short repeatFirst;
	/** For associative-commutative operators: the position of the second RHS nonterminal, which is
	 * also the last RHS symbol to repeat when there are more than two arguments. */
	private short repeatLast;
	
	/** RHS nonterminals in left-to-right order. */
	private Nonterminal[] args;
	/** RHS variables in left-to-right order. */
	private Variable[] vars;
	/** A list of valid variable types for this production.  Not used for parsing; the actual variable
	 * types used for parsing are stored in synchronous grammar rules. */
	private Denotation varTypes;
	/** Parse tree of the RHS string based on the original, unambiguous MRL grammar. */
	private Node parse;
	/** Nonterminal nodes on the frontier of the RHS parse tree. */
	private Node[] argNodes;
	/** Hash code of this production. */
	private int hash;
	
	/**
	 * Creates a new production with the specified LHS and RHS.
	 *   
	 * @param lhs the LHS nonterminal.
	 * @param rhs the RHS string.
	 * @param ac indicates if this production is an associative-commutative (AC) operator.
	 * @param orig indicates if this production is part of the original, unambiguous MRL grammar.
	 * @throws RuntimeException if <code>ac</code> is <code>true</code>, but this production cannot be an
	 * AC operator.
	 */
	public Production(Nonterminal lhs, Symbol[] rhs, boolean ac, boolean orig) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.orig = orig;
		setHash();

		this.ac = ac;
		repeatFirst = repeatLast = -1;
		args = (Nonterminal[]) Arrays.subseq(rhs, Nonterminal.class);
		vars = (Variable[]) Arrays.subseq(rhs, Variable.class);
		varTypes = new Denotation((short) vars.length);  // default value
		parse = new Node(new ProductionSymbol(this));
		for (short i = 0; i < rhs.length; ++i)
			if (rhs[i] instanceof Nonterminal)
				parse.addChild(new Node(rhs[i]));
		argNodes = parse.getDescends(Nonterminal.class);
		if (ac) {
			if (args.length != 2 || !args[0].equals(args[1]))
				throw new RuntimeException();
			for (short i = 0; i < rhs.length; ++i)
				if (rhs[i] instanceof Nonterminal) {
					if (repeatFirst < 0)
						repeatFirst = (short) (i+1);
					else
						repeatLast = i;
				}
		}
	}

	/**
	 * Creates a new dummy production with the specified RHS nonterminal.
	 *   
	 * @param rhs the RHS nonterminal.
	 */
	public Production(Nonterminal rhs) {
		lhs = null;
		this.rhs = new Symbol[1];
		this.rhs[0] = rhs;
		orig = false;
		setHash();
		
		ac = false;
		repeatFirst = repeatLast = -1;
		args = new Nonterminal[1];
		args[0] = rhs;
		vars = new Variable[0];
		varTypes = new Denotation((short) 0);
		parse = new Node(new ProductionSymbol(this));
		parse.addChild(new Node(this.rhs[0]));
		argNodes = parse.getDescends(Nonterminal.class);
	}

	/**
	 * Creates a new dummy production with the specified nonterminal.
	 * 
	 * @param nonterm a nonterminal symbol.
	 * @param n the length of the RHS string.
	 */
	public Production(Nonterminal nonterm, short n) {
		lhs = nonterm;
		rhs = new Symbol[n];
		for (short i = 0; i < n; ++i)
			rhs[i] = (Nonterminal) nonterm.copy();
		orig = false;
		setHash();
		
		ac = false;
		repeatFirst = repeatLast = -1;
		args = new Nonterminal[n];
		for (short i = 0; i < n; ++i)
			args[i] = (Nonterminal) rhs[i];
		vars = new Variable[0];
		varTypes = new Denotation((short) 0);
		parse = new Node(new ProductionSymbol(this));
		for (short i = 0; i < n; ++i)
			parse.addChild(new Node(rhs[i]));
		argNodes = parse.getDescends(Nonterminal.class);
	}
	
	/**
	 * Creates a version of the given associative-commutative (AC) operator with the specified number of
	 * arguments.  The resulting production will have the <code>ac</code> bit turned off.
	 * 
	 * @param prod the AC operator.
	 * @param nargs the number of arguments.
	 * @throws RuntimeException if the given production is not an AC operator, or if the number of
	 * arguments is less than two.
	 */
	public Production(Production prod, short nargs) {
		if (!prod.isAC())
			throw new RuntimeException();
		
		lhs = prod.lhs;
		rhs = prod.repeatRhs(nargs);
		orig = false;
		setHash();
		
		ac = false;
		repeatFirst = repeatLast = -1;
		args = (Nonterminal[]) Arrays.subseq(rhs, Nonterminal.class);
		// assuming AC operators don't take variables
		vars = new Variable[0];
		varTypes = prod.varTypes;
		parse = new Node(new ProductionSymbol(prod));
		for (short i = 0; i < rhs.length; ++i)
			if (rhs[i] instanceof Nonterminal)
				parse.addChild(new Node(rhs[i]));
		argNodes = parse.getDescends(Nonterminal.class);
	}
	
	/**
	 * Creates a version of the given production in which the RHS wildcard is replaced with a terminal
	 * symbol.  A <code>RuntimeException</code> is thrown if the production does not contain any wildcard
	 * symbols, or if the wildcard symbol does not match the terminal symbol.
	 * 
	 * @param prod the production to modify.
	 * @param term a terminal symbol.
	 * @throws RuntimeException if substitution fails.
	 */
	public Production(Production prod, Terminal term) {
		if (!prod.isWildcard() || !prod.rhs[0].matches(term))
			throw new RuntimeException();
		
		lhs = prod.lhs;
		rhs = new Symbol[1];
		rhs[0] = (Symbol) term.copy();
		orig = false;
		setHash();
		
		ac = false;
		repeatFirst = repeatLast = -1;
		args = new Nonterminal[0];
		vars = new Variable[0];
		varTypes = prod.varTypes;
		parse = new Node(new ProductionSymbol(this));
		argNodes = new Node[0];
	}
	
	/**
	 * Creates a version of the given production in which an RHS variable is replaced with another one.  A
	 * <code>RuntimeException</code> is thrown if there is no variable at the specified position.
	 * 
	 * @param prod the production to modify.
	 * @param index the position index of the variable to be replaced.
	 * @param var a new variable.
	 * @throws RuntimeException if substitution fails.
	 */
	public Production(Production prod, short index, Variable var) {
		if (!(prod.rhs[index] instanceof Variable))
			throw new RuntimeException();
		
		lhs = prod.lhs;
		rhs = (Symbol[]) prod.rhs.clone();
		rhs[index] = var;
		orig = false;
		setHash();
		
		// assuming AC operators don't take variables
		ac = false;
		repeatFirst = repeatLast = -1;
		args = prod.args;
		vars = (Variable[]) Arrays.subseq(rhs, Variable.class);
		varTypes = prod.varTypes;
		// production is in the original MRL grammar
		parse = new Node(new ProductionSymbol(this));
		for (short i = 0; i < rhs.length; ++i)
			if (rhs[i] instanceof Nonterminal)
				parse.addChild(new Node(rhs[i]));
		argNodes = parse.getDescends(Nonterminal.class);
	}
	
	/**
	 * Creates a version of the given production in which all variable indices and nonterminal
	 * arguments are removed.
	 * 
	 * @param prod the production to modify.
	 */
	public Production(Production prod) {
		// remove variables in the LHS nonterminal, if any
		lhs = new Nonterminal(prod.lhs.getId());
		rhs = (Symbol[]) prod.rhs.clone();
		for (short i = 0; i < rhs.length; ++i)
			if (rhs[i] instanceof Variable)
				rhs[i] = new Variable();
			else if (rhs[i] instanceof Nonterminal)
				// doesn't have variables in it
				;
		orig = false;
		setHash();
		
		ac = prod.ac;
		repeatFirst = prod.repeatFirst;
		repeatLast = prod.repeatLast;
		args = prod.args;
		vars = (Variable[]) Arrays.subseq(rhs, Variable.class);
		varTypes = prod.varTypes;
		// production is in the original MRL grammar
		parse = new Node(new ProductionSymbol(this));
		for (short i = 0; i < rhs.length; ++i)
			if (rhs[i] instanceof Nonterminal)
				parse.addChild(new Node(rhs[i]));
		argNodes = parse.getDescends(Nonterminal.class);
	}
	
	/**
	 * Creates a version of the given production in which an RHS nonterminal is rewritten by another
	 * production.  A <code>RuntimeException</code> is thrown if the production used for rewriting has a
	 * wrong LHS nonterminal or if it contains a wildcard symbol.
	 *  
	 * @param prod the production to modify.
	 * @param arg the production used for rewriting.
	 * @param argIndex the index of the nonterminal to be rewritten.
	 * @throws RuntimeException if rewriting fails.
	 */
	public Production(Production prod, Production arg, short argIndex) {
		if (!arg.getLhs().matches(prod.args[argIndex]) || arg.isWildcard())
			throw new RuntimeException();
		
		Production p1 = ((ProductionSymbol) prod.argNodes[argIndex].getParent().getSymbol()).getProduction();
		Production p2 = ((ProductionSymbol) arg.parse.getSymbol()).getProduction();
		if (p1.isAC() && p1.matches(p2)) {
			// remove redundant AC operator
			lhs = prod.lhs;
			short i = (short) Arrays.findInstance(prod.rhs, Nonterminal.class, argIndex);
			short len1 = (short) Arrays.findInstance(p2.rhs, Nonterminal.class, 0);
			short len2 = (short) (p2.rhs.length-p2.repeatLast-1);
			Symbol[] argRhs = (Symbol[]) Arrays.subarray(arg.rhs, len1, arg.rhs.length-len2);
			rhs = (Symbol[]) Arrays.replace(prod.rhs, i, argRhs);
			orig = false;
			setHash();
			
			ac = false;
			repeatFirst = repeatLast = -1;
			args = (Nonterminal[]) Arrays.subseq(rhs, Nonterminal.class);
			vars = (Variable[]) Arrays.subseq(rhs, Variable.class);
			short varIndex = (short) Arrays.countInstances(prod.rhs, 0, i, Variable.class);
			varTypes = prod.varTypes.multiply(arg.varTypes, varIndex);
			//logger.finest(prod+" + "+arg+" @"+argIndex);
			//logger.finest(prod.varTypes+" + "+arg.varTypes);
			//logger.finest("  "+varTypes);
			parse = prod.parse.deepCopy();
			Node[] n = parse.getDescends(Nonterminal.class);
			n[argIndex].getParent().replaceChildWithChildren(n[argIndex], arg.parse.deepCopy());
			argNodes = parse.getDescends(Nonterminal.class);
		} else {
			// no redundant AC operator here
			lhs = prod.lhs;
			short i = (short) Arrays.findInstance(prod.rhs, Nonterminal.class, argIndex);
			rhs = (Symbol[]) Arrays.replace(prod.rhs, i, arg.rhs);
			orig = false;
			setHash();
	
			ac = false;
			repeatFirst = repeatLast = -1;
			args = (Nonterminal[]) Arrays.subseq(rhs, Nonterminal.class);
			vars = (Variable[]) Arrays.subseq(rhs, Variable.class);
			short varIndex = (short) Arrays.countInstances(prod.rhs, 0, i, Variable.class);
			varTypes = prod.varTypes.multiply(arg.varTypes, varIndex);
			//logger.finest(prod+" + "+arg+" @"+argIndex);
			//logger.finest(prod.varTypes+" + "+arg.varTypes);
			//logger.finest("  "+varTypes);
			parse = prod.parse.deepCopy();
			Node[] n = parse.getDescends(Nonterminal.class);
			Node argp = arg.parse.deepCopy();
			argp.setSymbol(new ProductionSymbol(new Production(p2, new Variable[0])));
			n[argIndex].getParent().replaceChild(n[argIndex], argp);
			argNodes = parse.getDescends(Nonterminal.class);
		}
	}

	/**
	 * Creates a version of the given production with the given bound variables.
	 * 
	 * @param prod the production to modify.
	 * @param vargs a list of bound variables.
	 */
	public Production(Production prod, Variable[] vargs) {
		// the list of bound variables are stored in the LHS nonterminal
		lhs = new Nonterminal(prod.lhs.getId());
		for (short i = 0; i < vargs.length; ++i)
			lhs.addArg(vargs[i]);
		rhs = (Symbol[]) prod.rhs.clone();
		orig = false;
		setHash();
		
		ac = prod.ac;
		repeatFirst = prod.repeatFirst;
		repeatLast = prod.repeatLast;
		args = prod.args;
		vars = prod.vars;
		varTypes = prod.varTypes;
		parse = prod.parse.deepCopy();
		if (prod.isBase())
			// base case to avoid infinite loops
			parse.setSymbol(new ProductionSymbol(this));
		else {
			Production p = ((ProductionSymbol) parse.getSymbol()).getProduction();
			p = new Production(p, vargs);
			parse.setSymbol(new ProductionSymbol(p));
		}
		argNodes = parse.getDescends(Nonterminal.class);
	}
	
	/**
	 * Creates a version of the given production in which all RHS variables are replaced based on the
	 * given variable assignment.
	 * 
	 * @param prod the production to modify.
	 * @param assign the variable assignment.
	 * @throws RuntimeException if <code>assign</code> does not cover all RHS variables in this
	 * production.
	 */
	public Production(Production prod, VariableAssignment assign) {
		// may have variables in it
		lhs = new Nonterminal(prod.lhs, assign);
		rhs = (Symbol[]) prod.rhs.clone();
		for (short i = 0; i < rhs.length; ++i)
			if (rhs[i] instanceof Variable) {
				rhs[i] = assign.get((Variable) rhs[i]);
				if (rhs[i] == null)
					throw new RuntimeException();
			} else if (rhs[i] instanceof Nonterminal)
				// doesn't have variables in it
				;
		orig = false;
		setHash();
		
		ac = prod.ac;
		repeatFirst = prod.repeatFirst;
		repeatLast = prod.repeatLast;
		args = prod.args;
		vars = (Variable[]) Arrays.subseq(rhs, Variable.class);
		varTypes = prod.varTypes;
		parse = prod.parse.deepCopy();
		if (prod.isBase())
			// base case to avoid infinite loops
			parse.setSymbol(new ProductionSymbol(this));
		else {
			Node[] descends = parse.getDescends(ProductionSymbol.class);
			for (short i = 0; i < descends.length; ++i) {
				Production p = ((ProductionSymbol) descends[i].getSymbol()).getProduction();
				p = new Production(p, assign);
				descends[i].setSymbol(new ProductionSymbol(p));
			}
		}
		argNodes = parse.getDescends(Nonterminal.class);
	}

	private boolean isBase() {
		return equals(((ProductionSymbol) parse.getSymbol()).getProduction());
	}
	
	private void setHash() {
		hash = 1;
		hash = 31*hash + getLhsId();
		hash = 31*hash + Arrays.hashCode(rhs);
	}
	
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o instanceof Production) {
			Production p = (Production) o;
			return ((lhs==null) ? (p.lhs==null) : lhs.equals(p.lhs)) && Arrays.equal(rhs, p.rhs)
			&& ac == p.ac;
		}
		return false;
	}
	
	public boolean matches(Production prod) {
		if (prod == this)
			return true;
		if ((lhs==null) ? (prod.lhs!=null) : !lhs.matches(prod.lhs))
			return false;
		if (rhs.length != prod.rhs.length)
			return false;
		for (short i = 0; i < rhs.length; ++i)
			if (!rhs[i].matches(prod.rhs[i]))
				return false;
		if (ac != prod.ac)
			return false;
		return true;
	}
	
	public VariableAssignment matchesVar(Production prod) {
		// assuming matches(prod) == true
		VariableAssignment a = new VariableAssignment();
		for (short i = 0; i < vars.length; ++i)
			if (!a.put(vars[i], prod.vars[i]))
				return null;
		return a;
	}
	
	public int hashCode() {
		return hash;
	}

	/**
	 * Returns the LHS nonterminal of this production.  <code>null</code> is returned if this is a dummy
	 * production.
	 * 
	 * @return the LHS nonterminal of this production.
	 */
	public Nonterminal getLhs() {
		return lhs;
	}
	
	/**
	 * Returns the ID of the LHS nonterminal of this production.  <code>-1</code> is returned if this is a
	 * dummy production.
	 * 
	 * @return the ID of the LHS nonterminal of this production.
	 */
	public int getLhsId() {
		return (lhs==null) ? -1 : lhs.getId();
	}
	
	public Symbol[] getRhs() {
		return rhs;
	}
	
	public Symbol getRhs(short i) {
		return rhs[i];
	}
	
	public short length() {
		return (short) rhs.length;
	}
	
	public Denotation getVarTypes() {
		return varTypes;
	}
	
	public void setVarTypes(Denotation varTypes) {
		this.varTypes = varTypes;
	}
	
	/**
	 * Indicates if this production is in the original, unambiguous MRL grammar.
	 * 
	 * @return <code>true</code> if this production is in the original, unambiguous MRL grammar; 
	 * <code>false</code> otherwise.
	 */
	public boolean isOrig() {
		return orig;
	}
	
	/**
	 * Indicates if this production is an associative-commutative (AC) operator.
	 * 
	 * @return <code>true</code> if this production is an AC operator; <code>false</code> otherwise.
	 */
	public boolean isAC() {
		return ac;
	}

	/**
	 * Indicates if repetition of RHS symbols is allowed after the <code>i</code>-th symbol.
	 * 
	 * @param i a position index.
	 * @return <code>true</code> if repetition is allowed; <code>false</code> otherwise.
	 */
	public boolean allowsRepeat(short i) {
		return i == repeatLast;
	}
	
	/**
	 * Returns the position of the first RHS symbol to repeat when the production has more than two
	 * arguments.
	 * 
	 * @return the position index of the first RHS symbol to repeat.
	 */
	public short repeatDot() {
		return repeatFirst;
	}
	
	/**
	 * Returns the RHS string when this production has more than two arguments.
	 * 
	 * @param nargs the number of arguments.
	 * @return the RHS string of this production.
	 */
	public Symbol[] repeatRhs(short nargs) {
		if (!ac)
			return rhs;
		Symbol[] r = new Symbol[rhs.length+(repeatLast-repeatFirst+1)*(nargs-2)];
		short i = 0;
		for (short j = 0; j <= repeatLast; ++j)
			r[i++] = rhs[j];
		for (short j = 2; j < nargs; ++j)
			for (short k = repeatFirst; k <= repeatLast; ++k)
				r[i++] = rhs[k];
		for (short j = (short) (repeatLast+1); j < rhs.length; ++j)
			r[i++] = rhs[j];
		return r;
	}
	
	/**
	 * Returns an array containing all nonterminal symbols in this production's RHS, listed in
	 * left-to-right order.  An empty array is returned if there is no nonterminal in the RHS.
	 * 
	 * @return an array containing all nnonterminal symbols in this production's RHS.
	 */
	public Nonterminal[] getArgs() {
		return args;
	}
	
	/**
	 * Returns the number of nonterminal symbols in this production's RHS.
	 * 
	 * @return the number of nonterminal symbols in this production's RHS.
	 */
	public short countArgs() {
		return (short) args.length;
	}
	
	/**
	 * Indicates if this production has any nonterminal symbols on the RHS.
	 * 
	 * @return <code>true</code> if this production has nonterminal symbols on the RHS;
	 * <code>false</code> otherwise.
	 */
	public boolean hasArgs() {
		return args.length > 0;
	}
	
	public Variable[] getVars() {
		return vars;
	}
	
	public short countVars() {
		return (short) vars.length;
	}
	
	/**
	 * Returns the parse tree of the RHS string of this production based on the original, unambiguous MRL
	 * grammar.  For every node <i>n</i> in the parse tree, if <i>n</i> has a child with a nonterminal
	 * label, then there is a nonterminal in the production of <i>n</i> that has not yet been rewritten.
	 * 
	 * @return the parse tree of the RHS string of this production.
	 */
	public Node getParse() {
		return parse;
	}
	
	/**
	 * Returns the nonterminal nodes on the frontier of the RHS parse tree.
	 * 
	 * @return an array containing the nonterminal nodes on the frontier of the RHS parse tree.
	 */
	public Node[] getArgNodes() {
		return argNodes;
	}
	
	/**
	 * Indicates if this production is a unary production (i.e. RHS consists of a single nonterminal).
	 * 
	 * @return <code>true</code> if this production is unary; <code>false</code> otherwise.
	 */
	public boolean isUnary() {
		return rhs.length == 1 && rhs[0] instanceof Nonterminal;
	}
	
	/**
	 * Indicates if the RHS of this production is only a wildcard terminal.
	 * 
	 * @return <code>true</code> if the RHS of this production is only a wildcard terminal;
	 * <code>false</code> otherwise.
	 */
	public boolean isWildcard() {
		return rhs.length == 1 && rhs[0] instanceof Terminal && ((Terminal) rhs[0]).isWildcard();
	}
	
	public boolean isWildcardMatch() {
		return rhs.length == 1 && rhs[0] instanceof Terminal && ((Terminal) rhs[0]).isWildcardMatch();
	}
	
	/**
	 * Indicates if the RHS of this production is only an <code>Anaphor</code> symbol.
	 * 
	 * @see wasp.data.Anaphor
	 * @return <code>true</code> if the RHS of this production is only an <code>Anaphor</code> symbol;
	 * <code>false</code> otherwise.
	 */
	public boolean isAnaphor() {
		return rhs.length == 1 && rhs[0] instanceof Anaphor;
	}
	
	/**
	 * Indicates if this production is a dummy.
	 * 
	 * @return <code>true</code> if this production is a dummy; <code>false</code> otherwise.
	 */
	public boolean isDummy() {
		return lhs == null;
	}

	/**
	 * Indicates if the first symbol of the RHS string is a possible match for the given symbol.
	 * This method always returns <code>true</code> if the first symbol of the RHS string is a
	 * nonterminal.
	 * 
	 * @param sym a symbol.
	 * @return <code>true</code> if the first symbol of the RHS string is a possible match for the
	 * given symbol; <code>false</code> otherwise.
	 */
	public boolean possibleMatch(Symbol sym) {
		return rhs[0] instanceof Nonterminal || rhs[0].matches(sym);
	}
	
	/**
	 * Returns an interned copy of this production.  If none exists, then this production is returned
	 * instead.
	 * 
	 * @return an interned copy of this production.
	 */
	public Production intern() {
		return Config.getMRLGrammar().intern(this);
	}
	
	///
	/// String representations
	///
	
	/**
	 * Returns the string representation of this production.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(lhs);
		if (ac)
			sb.append(" ->ac ({");
		else
			sb.append(" -> ({");
		for (short i = 0; i < rhs.length; ++i) {
			sb.append(' ');
			sb.append(rhs[i]);
		}
		sb.append(" })");
		return sb.toString();
	}

	public static boolean readOrig = false;
	
	/**
	 * Returns a production that part of the given line of text represents.  Beginning with the token at 
	 * the specified index, this method finds the shortest substring of the line that is the string 
	 * representation of a production.  If such a substring exists, then the production that it 
	 * represents is returned, and the <code>index</code> argument is set to the token index immediately 
	 * after the end of the substring.  Otherwise, <code>null</code> is returned and the 
	 * <code>index</code> argument remains unchanged.
	 * <p>
	 * This method is for reading productions in the original, unambiguous MRL grammar.  For other
	 * productions, use the <code>readParse</code> method.
	 * 
	 * @param line a line of text containing the string representation of a production.
	 * @param index the beginning token index to consider; it is also an <i>output</i> variable for 
	 * the token index immediately after the end of the consumed substring.
	 * @return the production that the substring <code>line[index, i)</code> represents, for the 
	 * smallest <code>i</code> possible; <code>null</code> if no such <code>i</code> exists.
	 */
	public static Production read(String[] line, Int index) {
		int i = index.val;
		Nonterminal lhs;
		boolean ac = false;
		if (i >= line.length || (lhs = (Nonterminal) Nonterminal.read(line[i])) == null)
			return null;
		++i;
		if (i == line.length || !(line[i].equals("->") || line[i].equals("->ac")))
			return null;
		ac = line[i].equals("->ac");
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
		if (i == line.length)
			return null;
		Symbol[] rhs = (Symbol[]) list.toArray(new Symbol[0]);
		index.val = i+1;
		return new Production(lhs, rhs, ac, readOrig);
	}
	
	/**
	 * Returns a production given the string representation of a parse tree.  It is similar to the
	 * <code>read</code> method, except that the input is the string representation of the parse tree
	 * of the production's RHS string, based on the original, unambiguous MRL grammar.
	 * 
	 * @param line a line of text containing the string representation of a parse tree.
	 * @param index the beginning token index to consider; it is also an <i>output</i> variable for 
	 * the token index immediately after the end of the consumed substring.
	 * @return the production that corresponds to the string representation of a parse tree; 
	 * <code>null</code> if none exists.
	 */
	public static Production readParse(String[] line, Int index) {
		Node.readSyn = false;
		Node parse = Node.read(line, index);
		if (parse == null)
			return null;
		return readParse(parse);
	}
	
	private static Production readParse(Node parse) {
		Production prod = ((ProductionSymbol) parse.getSymbol()).getProduction();
		prod.setVarTypes(new Production(prod).intern().getVarTypes());
		short nc = parse.countChildren();
		if (prod.isAC())
			prod = new Production(prod, nc);
		for (short i = (short) (nc-1); i >= 0; --i) {
			Node child = parse.getChild(i);
			if (child.getSymbol() instanceof ProductionSymbol)
				prod = new Production(prod, readParse(child), i);
		}
		return prod;
	}
	
}
