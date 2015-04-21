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

import wasp.data.Node;
import wasp.data.Nonterminal;
import wasp.data.Symbol;
import wasp.data.Terminal;
import wasp.data.Variable;
import wasp.main.generate.LogLinearModel;
import wasp.math.Math;
import wasp.mrl.Denotation;
import wasp.mrl.Production;
import wasp.util.Arrays;
import wasp.util.Double;
import wasp.util.Int;
import wasp.util.Short;

/**
 * Production rules in a synchronous context-free grammar (SCFG) or a lambda-SCFG.
 * 
 * @author ywwong
 *
 */
public class Rule {

	/** The LHS nonterminal. */
	private Nonterminal lhs;
	/** The NL string on the RHS. */
	private Symbol[] E;
	/** Size of word gaps to the left of each RHS NL symbol. */
	private short[] gaps;
	/** The MRL string on the RHS. */
	private Symbol[] F;
	/** The corresponding production in the MRL grammar. */
	private Production prod;
	/** The maximum variable ID that appears in this rule. */
	private short maxVarId;
	/** The number of unique free variables that appear in this rule. */
	private short nfvars;
	/** A list of valid variable types for this production. */
	private Denotation varTypes;
	/** Indicates if this rule is an initial rule. */
	private boolean init;
	/** Indicates if this rule is active. */
	private boolean active;
	/** The weight of this rule in a translation model. */
	private double weight;
	/** The outer score of this rule. */
	private double outer;
	/** The absolute frequency of this rule. */
	private int count;
	/** The score components of this rule for NL generation. */
	private LogLinearModel.Scores scores;

	/** The position index of the last nonterminal in the NL string. */
	private short lastArg;
	/** The number of non-zero word gaps in this rule. */
	private short ngaps;
	/** Hash code of this rule. */
	private int hash;

	/** Used to expedite the computation of derivation scores. */
	public int ruleId;
	/** Used to expedite the computation of MRL language model scores. */
	public int partialRuleId;

	/**
	 * Creates a new SCFG rule with the specified arguments, assuming the corresponding MRL production
	 * already exists in the MRL grammar.
	 * 
	 * @param lhs the LHS nonterminal.
	 * @param E the NL string on the RHS.
	 * @param gaps size of word gaps to the right of each NL symbol.
	 * @param F the MRL string on the RHS.
	 * @param init indicates if this rule is an initial rule.
	 */
	public Rule(Nonterminal lhs, Symbol[] E, short[] gaps, Symbol[] F, boolean init) {
		this.lhs = lhs;
		this.E = E;
		this.gaps = gaps;
		this.F = F;
		Symbol[] rhs = (Symbol[]) Arrays.copy(F);
		for (short i = 0; i < rhs.length; ++i) {
			rhs[i].removeIndex();
			if (rhs[i] instanceof Nonterminal)
				((Nonterminal) rhs[i]).removeArgs();
		}
		// assuming that an interned copy of the new production already exists...
		prod = new Production(lhs, rhs, false, false).intern();
		setMaxVarId();
		setNumFreeVars();
		varTypes = prod.getVarTypes().uncut(prod.getVars(), maxVarId);
		this.init = init;
		active = true;
		weight = 0;
		outer = Double.NEGATIVE_INFINITY;
		count = 0;
		scores = new LogLinearModel.Scores();
		ruleId = 0;
		partialRuleId = 0;
		init();
	}

	/**
	 * Creates a new SCFG rule with the specified arguments.
	 * 
	 * @param prod an interned copy of the corresponding MRL production.
	 * @param E the NL string on the RHS.
	 * @param gaps size of word gaps to the right of each NL symbol.
	 * @param F the MRL string on the RHS.
	 * @param init indicates if this rule is an initial rule.
	 */
	public Rule(Production prod, Symbol[] E, short[] gaps, Symbol[] F, boolean init) {
		lhs = prod.getLhs();
		this.E = E;
		this.gaps = gaps;
		this.F = F;
		this.prod = prod;
		setMaxVarId();
		setNumFreeVars();
		varTypes = prod.getVarTypes().uncut(prod.getVars(), maxVarId);
		this.init = init;
		active = true;
		weight = 0;
		outer = Double.NEGATIVE_INFINITY;
		count = 0;
		scores = new LogLinearModel.Scores();
		ruleId = 0;
		partialRuleId = 0;
		init();
	}
	
	/**
	 * Creates a new dummy rule with the specified RHS nonterminal.
	 *   
	 * @param rhs the RHS nonterminal.
	 */
	public Rule(Nonterminal rhs) {
		lhs = null;
		E = new Symbol[1];
		E[0] = (Nonterminal) rhs.copy();
		E[0].setIndex((short) 1);
		gaps = new short[2];
		F = new Symbol[1];
		F[0] = (Nonterminal) rhs.copy();
		F[0].setIndex((short) 1);
		prod = new Production(rhs);
		maxVarId = 0;
		nfvars = 0;
		varTypes = new Denotation((short) 0);
		init = false;
		active = true;
		weight = 0;
		outer = Double.NEGATIVE_INFINITY;
		count = 0;
		scores = new LogLinearModel.Scores();
		ruleId = 0;  // not used anyway
		partialRuleId = 0;  // must be zero
		init();
	}

	/**
	 * Creates a specialization of a rule by replacing the wildcards on the RHS with the specified
	 * terminal symbol.  The terminal symbol must be compatible with the wildcards.  Otherwise, a
	 * <code>RuntimeException</code> is thrown.
	 * 
	 * @param rule the rule to specialize.
	 * @param term the replacement terminal symbol.
	 * @throws RuntimeException if specialization fails.
	 */
	public Rule(Rule rule, Terminal term) {
		if (!rule.isWildcard() || !rule.E[0].matches(term))
			throw new RuntimeException();
		
		lhs = rule.lhs;
		E = new Symbol[1];
		E[0] = (Symbol) term.copy();
		gaps = new short[2];
		F = new Symbol[1];
		F[0] = (Symbol) term.copy();
		prod = new Production(rule.getProduction(), term);
		maxVarId = rule.maxVarId;
		nfvars = rule.nfvars;
		varTypes = rule.varTypes;
		init = false;
		active = true;
		weight = 0;
		outer = Double.NEGATIVE_INFINITY;
		count = 0;
		scores = new LogLinearModel.Scores();
		ruleId = 0;
		partialRuleId = 0;
		init();
	}
	
	private void setMaxVarId() {
		maxVarId = 0;
		for (short i = 0; i < lhs.countArgs(); ++i)
			if (maxVarId < lhs.getArg(i).getVarId())
				maxVarId = lhs.getArg(i).getVarId();
		for (short i = 0; i < F.length; ++i)
			if (F[i] instanceof Variable) {
				if (maxVarId < ((Variable) F[i]).getVarId())
					maxVarId = ((Variable) F[i]).getVarId();
			} else if (F[i] instanceof Nonterminal)
				for (short j = 0; j < ((Nonterminal) F[i]).countArgs(); ++j)
					if (maxVarId < ((Nonterminal) F[i]).getArg(j).getVarId())
						maxVarId = ((Nonterminal) F[i]).getArg(j).getVarId();
	}
	
	private void setNumFreeVars() {
		boolean[] fvars = new boolean[maxVarId+1];
		for (short i = 0; i < F.length; ++i)
			if (F[i] instanceof Variable)
				fvars[((Variable) F[i]).getVarId()] = true;
			else if (F[i] instanceof Nonterminal)
				for (short j = 0; j < ((Nonterminal) F[i]).countArgs(); ++j)
					fvars[((Nonterminal) F[i]).getArg(j).getVarId()] = true;
		for (short i = 0; i < lhs.countArgs(); ++i)
			fvars[lhs.getArg(i).getVarId()] = false;
		nfvars = (short) Arrays.count(fvars);
	}
	
	private void init() {
		for (short i = 0; i < E.length; ++i)
			if (E[i] instanceof Terminal)
				--scores.wp;
		lastArg = (short) Arrays.findLastInstance(E, Nonterminal.class, 0);
		ngaps = 0;
		for (short i = 0; i < gaps.length; ++i)
			if (gaps[i] > 0)
				++ngaps;
		hash = 1;
		hash = 31*hash + getLhsId();
		hash = 31*hash + Arrays.hashCode(E);
		hash = 31*hash + Arrays.hashCode(gaps);
		hash = 31*hash + Arrays.hashCode(F);
	}
	
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o instanceof Rule) {
			Rule r = (Rule) o;
			return ((lhs==null) ? (r.lhs==null) : lhs.equals(r.lhs)) && Arrays.equal(E, r.E)
			&& Arrays.equal(gaps, r.gaps) && Arrays.equal(F, r.F);
		}
		return false;
	}
	
	public int hashCode() {
		return hash;
	}
	
	/**
	 * Returns the LHS nonterminal of this rule.  <code>null</code> is returned if this is a dummy rule.
	 * 
	 * @return the LHS nonterminal of this rule.
	 */
	public Nonterminal getLhs() {
		return lhs;
	}
	
	/**
	 * Returns the ID of the LHS nonterminal of this rule.  <code>-1</code> is returned if this is a dummy
	 * rule.
	 * 
	 * @return the ID of the LHS nonterminal of this rule.
	 */
	public int getLhsId() {
		return (lhs==null) ? -1 : lhs.getId();
	}
	
	public short countLhsArgs() {
		return (lhs==null) ? 0 : lhs.countArgs();
	}
	
	public Symbol[] getE() {
		return E;
	}
	
	/**
	 * Returns the specified symbol in the NL string.
	 * 
	 * @param i a position index.
	 * @return the <code>i</code>-th symbol in the NL string.
	 */
	public Symbol getE(short i) {
		return E[i];
	}
	
	public short[] getGaps() {
		return gaps;
	}
	
	/**
	 * Returns the size of word gap to the <i>left</i> of the specified NL symbol.
	 * 
	 * @param i a position index.
	 * @return the size of word gap to the <i>left</i> of the <code>i</code>-th NL symbol.
	 */
	public short getGap(short i) {
		return gaps[i];
	}
	
	public Symbol[] getF() {
		return F;
	}
	
	/**
	 * Returns the specified symbol in the MRL string.
	 * 
	 * @param i a position index.
	 * @return the <code>i</code>-th symbol in the MRL string.
	 */
	public Symbol getF(short i) {
		return F[i];
	}
	
	public short lengthE() {
		return (short) E.length;
	}
	
	public short lengthF() {
		return (short) F.length;
	}
	
	public Production getProduction() {
		return prod;
	}
	
	public short getMaxVarId() {
		return maxVarId;
	}
	
	public Denotation getVarTypes() {
		return varTypes;
	}
	
	/**
	 * Indicates if this rule is an initial rule.
	 * 
	 * @return <code>true</code> if this rule is an initial rule; <code>false</code> otherwise.
	 */
	public boolean isInit() {
		return init;
	}
	
	/**
	 * Indicates if this rule is active.
	 * 
	 * @return <code>true</code> if this rule is active; <code>false</code> otherwise.
	 */
	public boolean isActive() {
		return active;
	}
	
	/**
	 * Deactivates this rule.  Only rules that are not initial rules can be deactivated.
	 */
	public void deactivate() {
		if (!init)
			active = false;
	}
	
	/**
	 * Indicates if this rule requires any arguments.
	 * 
	 * @return <code>true</code> if this rule requires some arguments; <code>false</code> otherwise.
	 */
	public boolean hasArgs() {
		return prod.hasArgs();
	}
	
	/**
	 * Returns the number of arguments that this rule requires.
	 * 
	 * @return the number of arguments that this rule requires.
	 */
	public short countArgs() {
		return prod.countArgs();
	}
	
	/**
	 * Indicates if the specified position index corresponds to the last nonterminal in the NL string of
	 * this rule.
	 * 
	 * @param i a position index.
	 * @return <code>true</code> if <code>i</code> corresponds to the last nonterminal in the NL string of
	 * this rule; <code>false</code> otherwise.
	 */
	public boolean isLastArg(short i) {
		return i == lastArg;
	}
	
	/**
	 * Returns the number of non-zero word gaps in this rule.
	 * 
	 * @return the number of non-zero word gaps in this rule.
	 */
	public short countGaps() {
		return ngaps;
	}
	
	/**
	 * Returns the weight of this rule in a translation model.
	 * 
	 * @return the weight of this rule.
	 */
	public double getWeight() {
		return weight;
	}
	
	/**
	 * Returns the number of unique free variables that appear in this rule.
	 * 
	 * @return the number of unique free variables that appear in this rule.
	 */
	public short countFreeVars() {
		return nfvars;
	}
	
	/**
	 * Assigns a weight to this rule in a translation model.
	 * 
	 * @param weight the weight to assign.
	 */
	public void setWeight(double weight) {
		this.weight = weight;
		scores.tm = weight;
	}
	
	/**
	 * Returns the score components of this rule for NL generation.
	 * 
	 * @return the score components of this rule.
	 */
	public LogLinearModel.Scores getScores() {
		return scores;
	}
	
	/**
	 * Indicates if this rule is a unary rule (i.e. RHS consists of a single nonterminal).
	 * 
	 * @return <code>true</code> if this rule is unary; <code>false</code> otherwise.
	 */
	public boolean isUnary() {
		return E.length == 1 && E[0] instanceof Nonterminal && prod.isUnary();
	}
	
	/**
	 * Indicates if the RHS of this rule is only a wildcard terminal.
	 * 
	 * @return <code>true</code> if the RHS of this rule is only a wildcard terminal; <code>false</code> 
	 * otherwise.
	 */
	public boolean isWildcard() {
		return E.length == 1 && E[0] instanceof Terminal && ((Terminal) E[0]).isWildcard()
		&& prod.isWildcard();
	}
	
	/**
	 * Indicates if this rule is a dummy.
	 * 
	 * @return <code>true</code> if this rule is a dummy; <code>false</code> otherwise.
	 */
	public boolean isDummy() {
		return lhs == null;
	}
	
	/**
	 * Indicates if the first symbol of the RHS NL string is a possible match for the given symbol.
	 * This method always returns <code>true</code> if the first symbol of the RHS NL string is a
	 * nonterminal.
	 * 
	 * @param sym a symbol.
	 * @return <code>true</code> if the first symbol of the RHS NL string is a possible match for the
	 * given symbol; <code>false</code> otherwise.
	 */
	public boolean possibleMatchE(Symbol sym) {
		return E[0] instanceof Nonterminal || E[0].matches(sym);
	}
	
	/**
	 * Indicates if the first symbol of the RHS MRL string is a possible match for the given symbol.
	 * This method always returns <code>true</code> if the first symbol of the RHS MRL string is a
	 * nonterminal.
	 * 
	 * @param sym a symbol.
	 * @return <code>true</code> if the first symbol of the RHS MRL string is a possible match for the
	 * given symbol; <code>false</code> otherwise.
	 */
	public boolean possibleMatchF(Symbol sym) {
		return F[0] instanceof Nonterminal || F[0].matches(sym);
	}
	
	/**
	 * Returns the specified nonterminal node in the MR parse tree of this rule.
	 * 
	 * @param index the nonterminal index.
	 * @return the nonterminal node in the MR parse tree of this rule with the specified index.
	 */
	public Node getArgNode(short index) {
		return prod.getArgNodes()[index-1];
	}

	///
	/// Parameter estimation
	///
	
	public double getOuterScore() {
		return outer;
	}
	
	public void addOuterScore(double z) {
		outer = Math.logAdd(outer, z);
	}
	
	public void resetOuterScore() {
		outer = Double.NEGATIVE_INFINITY;
	}
	
	public int getCount() {
		return count;
	}
	
	public void addCount(int c) {
		count += c;
	}
	
	public void addCount() {
		++count;
	}
	
	///
	/// String representations
	///
	
	/**
	 * Returns the string representation of this rule.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(lhs);
		sb.append(" -> ({");
		for (short i = 0; i < E.length; ++i) {
			if (gaps[i] > 0) {
				sb.append(" *g:");
				sb.append(gaps[i]);
			}
			sb.append(' ');
			sb.append(E[i]);
		}
		if (gaps[E.length] > 0) {
			sb.append(" *g:");
			sb.append(gaps[E.length]);
		}
		sb.append(" })({");
		for (short i = 0; i < F.length; ++i) {
			sb.append(' ');
			sb.append(F[i]);
		}
		sb.append(" })");
		return sb.toString();
	}
	
	/** Indicates if all rules subsequently read by the <code>read</code> method are initial rules. */
	public static boolean readInit = false;

	/**
	 * Returns an SCFG rule that part of the given line of text represents.  Beginning with the token 
	 * at the specified index, this method finds the shortest substring of the line that is the string 
	 * representation of an SCFG rule.  If such a substring exists, then the rule that it represents is 
	 * returned, and the <code>index</code> argument is set to the token index immediately after the end 
	 * of the substring.  Otherwise, <code>null</code> is returned and the <code>index</code> argument 
	 * remains unchanged.
	 * 
	 * @param line a line of text containing the string representation of an SCFG rule.
	 * @param index the beginning token index to consider; it is also an <i>output</i> variable for 
	 * the token index immediately after the end of the consumed substring.
	 * @return the SCFG rule that the substring <code>line[index, i)</code> represents, for the 
	 * smallest <code>i</code> possible; <code>null</code> if no such <code>i</code> exists.
	 */
	public static Rule read(String[] line, Int index) {
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
		Terminal.readWords = true;
		ArrayList list1 = new ArrayList();
		ArrayList list2 = new ArrayList();
		list2.add(new Short(0));
		for (++i; i < line.length && !line[i].equals("})({"); ++i)
			if (line[i].startsWith("*g:")) {
				list2.remove(list2.size()-1);
				list2.add(new Short(Short.parseShort(line[i].substring(3))));
			} else {
				Symbol sym = Symbol.read(line[i]);
				if (sym == null)
					return null;
				list1.add(sym);
				list2.add(new Short(0));
			}
		Terminal.readWords = false;
		ArrayList list3 = new ArrayList();
		for (++i; i < line.length && !line[i].equals("})"); ++i) {
			Symbol sym = Symbol.read(line[i]);
			if (sym == null)
				return null;
			list3.add(sym);
		}
		if (i >= line.length)
			return null;
		Symbol[] E = (Symbol[]) list1.toArray(new Symbol[0]);
		short[] gaps = Arrays.toShortArray(list2);
		Symbol[] F = (Symbol[]) list3.toArray(new Symbol[0]);
		index.val = i+1;
		return new Rule(lhs, E, gaps, F, readInit);
	}
	
}
