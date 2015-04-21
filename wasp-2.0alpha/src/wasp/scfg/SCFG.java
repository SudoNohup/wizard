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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import wasp.data.Nonterminal;
import wasp.data.Symbol;
import wasp.data.Terminal;
import wasp.main.Config;
import wasp.mrl.Production;
import wasp.nl.NL;
import wasp.util.Arrays;
import wasp.util.Double;
import wasp.util.FileWriter;
import wasp.util.Int;
import wasp.util.Matrices;
import wasp.util.Numberer;
import wasp.util.TokenReader;

/**
 * Synchronous context-free grammars (SCFGs) and lambda-SCFGs.
 * 
 * @author ywwong
 *
 */
public class SCFG {

	private ArrayList[] byLhs;
	private Rule[][] _byLhs;
	private Numberer rules;
	private HashMap ties;
	private int ninit;
	
	private boolean[][] Elc;
	private boolean[][] _ElcTrans;
	private boolean[][] Flc;
	private boolean[][] _FlcTrans;

	private Numberer partial;
	private int npartial;

	public SCFG() {
		int nlhs = countNonterms();
		byLhs = new ArrayList[nlhs];
		_byLhs = new Rule[nlhs][];
		for (int i = 0; i < byLhs.length; ++i)
			byLhs[i] = new ArrayList();
		rules = new Numberer();
		ties = new HashMap();
		ninit = 0;

		Elc = new boolean[nlhs][nlhs];
		_ElcTrans = null;
		Flc = new boolean[nlhs][nlhs];
		_FlcTrans = null;

		partial = new Numberer(1);
		npartial = 1;  // 0 is reserved for the dummy rule
	}

	/**
	 * Returns the start symbol of this grammar.
	 * 
	 * @return the start symbol of this grammar.
	 */
	public Nonterminal getStart() {
		return Config.getMRLGrammar().getStart();
	}
	
	/**
	 * Returns the number of nonterminal symbols in this grammar.
	 * 
	 * @return the number of nonterminal symbols in this grammar.
	 */
	public int countNonterms() {
		return Config.getMRLGrammar().countNonterms();
	}
	
	/**
	 * Returns an array containing all rules in this grammar with the specified LHS nonterminal.  
	 * An empty array is returned if there are no such rules in this grammar.
	 * 
	 * @param lhs the ID of an LHS nonterminal.
	 * @return an array containing all rules in this grammar with the specified LHS nonterminal.
	 */
	public Rule[] getRules(int lhs) {
		if (_byLhs[lhs] == null)
			_byLhs[lhs] = (Rule[]) byLhs[lhs].toArray(new Rule[0]);
		return _byLhs[lhs];
	}
	
	/**
	 * Returns an array containing all rules in this grammar.  An empty array is returned if there
	 * are no rules in this grammar.
	 * 
	 * @return an array containing all rules in this grammar.
	 */
	public Rule[] getRules() {
		int nr = rules.getNextId();
		Rule[] a = new Rule[nr];
		for (int i = 0; i < nr; ++i)
			a[i] = (Rule) rules.getObj(i);
		return a;
	}
	
	/**
	 * Returns the number of rules in this grammar.
	 * 
	 * @return the number of rules in this grammar.
	 */
	public int countRules() {
		return rules.getNextId();
	}

	/**
	 * Returns the number of initial rules in this grammar.
	 * 
	 * @return the number of initial rules in this grammar.
	 */
	public int countInitRules() {
		return ninit;
	}
	
	/**
	 * Returns the ID of the specified rule.  <code>-1</code> is returned if there is no such rule in
	 * this grammar.
	 *  
	 * @param rule the rule to look for.
	 * @return the ID of the <code>rule</code> argument.
	 */
	public int getId(Rule rule) {
		return rules.getId(rule, false);
	}
	
	/**
	 * Returns the rule with the specified ID.  <code>null</code> is returned if there is no such rule
	 * in this grammar.
	 * 
	 * @param id a rule ID.
	 * @return the rule with the specified ID.
	 */
	public Rule getRule(int id) {
		return (Rule) rules.getObj(id);
	}
	
	/**
	 * Indicates if the specified rule is in this grammar.
	 * 
	 * @param rule the rule to look for.
	 * @return <code>true</code> if the <code>rule</code> argument is in this grammar; <code>false</code>
	 * otherwise.
	 */
	public boolean containsRule(Rule rule) {
		return rules.getId(rule, false) >= 0;
	}
	
	/**
	 * Returns an interned copy of the specified rule.  If none exists, then the given rule is returned
	 * instead.
	 * 
	 * @param rule the rule to look for.
	 * @return an interned copy of the <code>rule</code> argument.
	 */
	public Rule intern(Rule rule) {
		int id = getId(rule);
		return (id < 0) ? rule : getRule(id);
	}

	/**
	 * Returns an interned copy of the rule that the specified rule is tied to (e.g. <code>State -&gt; 
	 * Virginia, stateid('virginia')</code> could be tied to <code>State -&gt; Texas, 
	 * stateid('texas')</code>).
	 * 
	 * @param rule a rule.
	 * @return an interned copy of the rule that the <code>rule</code> argument is tied to.
	 */
	public Rule tied(Rule rule) {
		Int tid;
		if ((tid = (Int) ties.get(rule)) != null)
			return getRule(tid.val);
		// wildcard rules
		if (rule.lengthE() == 1 && rule.lengthF() == 1) {
			Symbol e = rule.getE((short) 0);
			Symbol f = rule.getF((short) 0);
			if (e instanceof Terminal && e.equals(f)) {
				Terminal t = (Terminal) e;
				if (t.isNum()) {
					Rule r = createWildcardRule(rule.getLhs(), Terminal.wildcardNum());
					int rid = getId(r);
					if (rid >= 0)
						return getRule(rid);
				}
				if (t.isUnum()) {
					Rule r = createWildcardRule(rule.getLhs(), Terminal.wildcardUnum());
					int rid = getId(r);
					if (rid >= 0)
						return getRule(rid);
				}
				if (t.isIdent()) {
					Rule r = createWildcardRule(rule.getLhs(), Terminal.wildcardIdent());
					int rid = getId(r);
					if (rid >= 0)
						return getRule(rid);
				}
			}
		}
		return intern(rule);
	}

	private static Rule createWildcardRule(Nonterminal lhs, Terminal wildcard) {
		Symbol[] E = new Symbol[1];
		E[0] = (Symbol) wildcard.copy();
		E[0].setIndex((short) 1);
		Symbol[] F = new Symbol[1];
		F[0] = (Symbol) wildcard.copy();
		F[0].setIndex((short) 1);
		return new Rule(lhs, E, new short[2], F, false);
	}

	/**
	 * Specifies the rule that the specified rule is tied to  (e.g. <code>State -&gt; Virginia, 
	 * stateid('virginia')</code> could be tied to <code>State -&gt; Texas, stateid('texas')</code>).
	 * 
	 * @param rule a rule.
	 * @param tied the rule that the <code>rule</code> argument is tied to.
	 */
	public void addTie(Rule rule, Rule tied) {
		addRule(rule);
		addRule(tied);
		ties.put(rule, new Int(getId(tied)));
	}
	
	/**
	 * Adds a new rule to this grammar.  This method returns <code>true</code> if the specified rule is
	 * successfully added to this grammar.  If the rule is already in the grammar, then 
	 * <code>false</code> is returned.
	 * 
	 * @param rule the rule to add.
	 * @return <code>true</code> if the specified rule is successfully added to this grammar; 
	 * <code>false</code> otherwise.
	 */
	public boolean addRule(Rule rule) {
		if (rules.addObj(rule)) {
			byLhs[rule.getLhs().getId()].add(rule);
			_byLhs[rule.getLhs().getId()] = null;
			if (rule.isInit())
				++ninit;
			if (rule.getE((short) 0) instanceof Nonterminal) {
				Elc[rule.getLhs().getId()][rule.getE((short) 0).getId()] = true;
				_ElcTrans = null;
			}
			if (rule.lengthF() > 0 && rule.getF((short) 0) instanceof Nonterminal) {
				Flc[rule.getLhs().getId()][rule.getF((short) 0).getId()] = true;
				_FlcTrans = null;
			}
			return true;
		}
		return false;
	}

	/**
	 * Indicates if the nonterminal <code>n2</code> is a <i>left corner</i> of the nonterminal
	 * <code>n1</code>, with respect to the NL grammar.  If a nonterminal is a left corner of another,
	 * then it is possible for the latter nonterminal to generate the former on the left fringe of some 
	 * parse tree.
	 * 
	 * @param n1 a nonterminal ID.
	 * @param n2 a nonterminal ID.
	 * @return <code>true</code> if the nonterminal <code>n2</code> is a left corner of the nonterminal
	 * <code>n1</code> with respect to the NL grammar; <code>false</code> otherwise.
	 */
	public boolean isLeftCornerForE(int n1, int n2) {
		if (_ElcTrans == null)
			_ElcTrans = Matrices.reflexiveTransitive(Elc);
		return _ElcTrans[n1][n2];
	}
	
	/**
	 * Indicates if the nonterminal <code>n2</code> is a <i>left corner</i> of the nonterminal
	 * <code>n1</code>, with respect to the MRL grammar.  If a nonterminal is a left corner of another, 
	 * then it is possible for the latter nonterminal to generate the former on the left fringe of some 
	 * parse tree.
	 * 
	 * @param n1 a nonterminal ID.
	 * @param n2 a nonterminal ID.
	 * @return <code>true</code> if the nonterminal <code>n2</code> is a left corner of the nonterminal
	 * <code>n1</code> with respect to the MRL grammar; <code>false</code> otherwise.
	 */
	public boolean isLeftCornerForF(int n1, int n2) {
		if (_FlcTrans == null)
			_FlcTrans = Matrices.reflexiveTransitive(Flc);
		return _FlcTrans[n1][n2];
	}
	
	public int getPartialRuleId(PartialRule rule, boolean add) {
		int id = partial.getId(rule, add);
		if (add)
			npartial = partial.getNextId();
		return id;
	}
	
	public PartialRule getPartialRule(int id) {
		return (PartialRule) partial.getObj(id);
	}
	
	public int countPartialRules() {
		return npartial;
	}
	
	///
	/// Parameter estimation
	///
	
	public void resetOuterScores() {
		int nr = rules.getNextId();
		for (int i = 0; i < nr; ++i)
			((Rule) rules.getObj(i)).resetOuterScore();
	}
	
	///
	/// File I/O
	///
	
	private static final String SCFG_RULES = "scfg-rules";
	
	/**
	 * Adds initial rules to this grammar.  Some of these initial rules are automatically created based on
	 * the MRL grammar (e.g. the unary rules).  Others are read from a file that is NL-specific.  The
	 * prefix of the file's pathname is specified in the configuration file (via the key
	 * <code>Config.SCFG_INIT</code>).  Suppose the prefix is <i>&lt;prefix&gt;</i>.  Then the file's
	 * pathname takes the form of <i>&lt;prefix&gt;</i><code>-</code><i>&lt;lang&gt;</i>, where
	 * <i>&lt;lang&gt;</i> is the current NL (i.e.&nbsp;the value of <code>NL.useNL</code>).  If this
	 * file does not exist, then <i>&lt;prefix&gt;</i> is the file to read.  SCFG rules are read from this
	 * file.  If this file contains something that is not a valid string representation of a rule, then a
	 * <code>RuntimeException</code> is thrown.  All initial rules have zero weights.
	 * 
	 * @throws IOException if an I/O error occurs.
	 * @throws RuntimeException if the file contains something that is not a valid string representation
	 * of a rule.
	 */
	public void readInit() throws IOException {
		String prefix = Config.get(Config.SCFG_INIT);
		File inFile = new File(prefix+"-"+NL.useNL);
		if (!inFile.exists())
			inFile = new File(prefix);
		TokenReader in = new TokenReader(new BufferedReader(new FileReader(inFile)));
		Rule.readInit = true;
		String[] line;
		while ((line = in.readLine()) != null) {
			Int index = new Int(0);
			Rule rule = Rule.read(line, index);
			Rule tied = rule;
			if (index.val < line.length && line[index.val].equals("tied-to")) {
				++index.val;
				tied = Rule.read(line, index);
				addTie(rule, tied);
			}
			if (index.val < line.length)
				throw new RuntimeException();
			addRule(rule);
			rule.ruleId = getId(tied);
			rule.partialRuleId = getPartialRuleId(new PartialRule(tied), true);
		}
		in.close();
		addDefaultInit();
	}
	
	private void addDefaultInit() {
		Production[] prods = Config.getMRLGrammar().getProductions();
		for (int i = 0; i < prods.length; ++i)
			if (prods[i].isUnary() || prods[i].isWildcard()) {
				Symbol[] E = new Symbol[1];
				E[0] = (Symbol) prods[i].getRhs((short) 0).copy();
				E[0].setIndex((short) 1);
				Symbol[] F = (Symbol[]) Arrays.copy(prods[i].getRhs());
				F[0].setIndex((short) 1);
				Rule rule = new Rule(prods[i], E, new short[2], F, true);
				addRule(rule);
				rule.ruleId = getId(rule);
				rule.partialRuleId = getPartialRuleId(new PartialRule(rule), true);
			}
	}

	/**
	 * Adds rules to this grammar.  Rules are read from a file called <code>scfg-rules</code> in the 
	 * directory specified in the configuration file (via the key <code>Config.MODEL_DIR</code>).  If 
	 * this file contains something that is not a valid string representation of a rule, then a 
	 * <code>RuntimeException</code> is thrown.
	 * 
	 * @throws IOException if an I/O error occurs.
	 * @throws RuntimeException if the file contains something that is not a valid string representation
	 * of a rule.
	 */
	public void read() throws IOException {
		read(new File(Config.getModelDir(), SCFG_RULES));
	}
	
	public void read(File file) throws IOException {
		TokenReader in = new TokenReader(new BufferedReader(new FileReader(file)));
		Rule.readInit = false;
		String[] line;
		while ((line = in.readLine()) != null) {
			Int index = new Int(0);
			Rule rule = Rule.read(line, index);
			Rule tied = rule;
			if (index.val < line.length && line[index.val].equals("tied-to")) {
				++index.val;
				tied = Rule.read(line, index);
				addTie(rule, tied);
			}
			if (index.val < line.length && line[index.val].equals("weight")) {
				rule.setWeight(Double.parseDouble(line[index.val+1]));
				index.val += 2;
			}
			if (index.val < line.length && line[index.val].equals("phr-prob-f|e")) {
				rule.getScores().PFE = Double.parseDouble(line[index.val+1]);
				index.val += 2;
			}
			if (index.val < line.length && line[index.val].equals("phr-prob-e|f")) {
				rule.getScores().PEF = Double.parseDouble(line[index.val+1]);
				index.val += 2;
			}
			if (index.val < line.length && line[index.val].equals("lex-weight-f|e")) {
				rule.getScores().PwFE = Double.parseDouble(line[index.val+1]);
				index.val += 2;
			}
			if (index.val < line.length && line[index.val].equals("lex-weight-e|f")) {
				rule.getScores().PwEF = Double.parseDouble(line[index.val+1]);
				index.val += 2;
			}
			if (index.val < line.length)
				throw new RuntimeException();
			addRule(rule);
			rule.ruleId = getId(tied);
			rule.partialRuleId = getPartialRuleId(new PartialRule(tied), true);
		}
		in.close();
	}

	/**
	 * Writes all active rules in this grammar to a file called <code>scfg-rules</code> in the directory
	 * specified in the configuration file (via the key <code>Config.MODEL_DIR</code>). 
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public void write() throws IOException {
		File file = new File(Config.getModelDir(), SCFG_RULES);
		PrintWriter out = new PrintWriter(new BufferedWriter(FileWriter.createNew(file)));
		Rule[] rules = getRules();
		for (int i = 0; i < rules.length; ++i)
			if (rules[i].isActive()) {
				out.print(rules[i]);
				Rule tied = tied(rules[i]);
				if (tied != rules[i]) {
					out.print(" tied-to ");
					out.print(tied);
				}
				if (rules[i].getWeight() != 0) {
					out.print(" weight ");
					out.print(rules[i].getWeight());
				}
				if (rules[i].getScores().PFE != 0) {
					out.print(" phr-prob-f|e ");
					out.print(rules[i].getScores().PFE);
				}
				if (rules[i].getScores().PEF != 0) {
					out.print(" phr-prob-e|f ");
					out.print(rules[i].getScores().PEF);
				}
				if (rules[i].getScores().PwFE != 0) {
					out.print(" lex-weight-f|e ");
					out.print(rules[i].getScores().PwFE);
				}
				if (rules[i].getScores().PwEF != 0) {
					out.print(" lex-weight-e|f ");
					out.print(rules[i].getScores().PwEF);
				}
				out.println();
			}
		out.close();
	}
	
}
