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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import wasp.data.Anaphor;
import wasp.data.Examples;
import wasp.data.Nonterminal;
import wasp.data.Symbol;
import wasp.domain.GeoFunqlGrammar;
import wasp.domain.GeoPrologGrammar;
import wasp.domain.RoboCupCLangGrammar;
import wasp.main.Config;
import wasp.util.FileWriter;
import wasp.util.Int;
import wasp.util.Matrices;
import wasp.util.Numberer;
import wasp.util.TokenReader;

/**
 * The abstract class for MRL grammars.
 *  
 * @author ywwong
 *
 */
public abstract class MRLGrammar {

	private static final Logger logger = Logger.getLogger(MRLGrammar.class.getName());
	static {
		logger.setLevel(Level.INFO);
	}
	
	private ArrayList[] byLhs;
	private Production[][] _byLhs;
	private Numberer numberer;
	private int norig;
	
	private HashSet zeroFert;
	private HashSet anaphorOK;
	private boolean[][] lc;
	private boolean[][] _lcTrans;
	
	protected MRLGrammar() {
		int nlhs = countNonterms();
		byLhs = new ArrayList[nlhs];
		_byLhs = new Production[nlhs][];
		for (int i = 0; i < byLhs.length; ++i)
			byLhs[i] = new ArrayList();
		numberer = new Numberer();
		norig = 0;

		zeroFert = new HashSet();
		anaphorOK = new HashSet();
		lc = new boolean[nlhs][nlhs];
		_lcTrans = null;
	}

	/**
	 * Creates and returns a new MRL grammar as specified in the configuration file (via the key
	 * <code>Config.MRL</code>).  Currently, two MRL identifiers are recognized: <code>geo-funql</code> 
	 * for the functional query language in the Geoquery domain, <code>geo-prolog</code> for the Prolog
	 * query language in the Geoquery domain, and <code>robocup-clang</code> for CLang in the RoboCup
	 * domain.  <code>null</code> is returned if the given MRL identifier is not recognized.
	 * 
	 * @return a new grammar of the MRL specified in the configuration file.
	 */
	public static MRLGrammar createNew() throws IOException {
		String mrl = Config.getMRL();
		if (mrl.equals("geo-funql"))
			return new GeoFunqlGrammar();
		else if (mrl.equals("geo-prolog"))
			return new GeoPrologGrammar();
		else if (mrl.equals("robocup-clang"))
			return new RoboCupCLangGrammar();
		return null;
	}
	
	/**
	 * Returns the start symbol of this grammar.
	 * 
	 * @return the start symbol of this grammar.
	 */
	public abstract Nonterminal getStart();
	
	/**
	 * Returns the number of nonterminal symbols in this grammar.
	 * 
	 * @return the number of nonterminal symbols in this grammar.
	 */
	public abstract int countNonterms();
	
	/**
	 * Returns the number of variable types in this grammar.
	 * 
	 * @return the number of variable types in this grammar.
	 */
	public abstract short countTypes();
	
	/**
	 * Returns an array containing all productions in this grammar with the specified LHS nonterminal.  
	 * An empty array is returned if there are no such productions in this grammar.
	 * 
	 * @param lhs the ID of an LHS nonterminal.
	 * @return an array containing all productions in this grammar with the specified LHS nonterminal.
	 */
	public Production[] getProductions(int lhs) {
		if (_byLhs[lhs] == null)
			_byLhs[lhs] = (Production[]) byLhs[lhs].toArray(new Production[0]);
		return _byLhs[lhs];
	}
	
	/**
	 * Returns an array containing all productions in this grammar.  An empty array is returned if there
	 * are no productions in this grammar.
	 * 
	 * @return an array containing all productions in this grammar.
	 */
	public Production[] getProductions() {
		int np = numberer.getNextId();
		Production[] a = new Production[np];
		for (int i = 0; i < np; ++i)
			a[i] = (Production) numberer.getObj(i);
		return a;
	}
	
	/**
	 * Returns the number of productions in this grammar.
	 * 
	 * @return the number of productions in this grammar.
	 */
	public int countProductions() {
		return numberer.getNextId();
	}

	/**
	 * Returns the number of productions in this grammar that are in the original, unambiguous MRL grammar.
	 * 
	 * @return the number of productions in this grammar that are in the original, unambiguous MRL grammar.
	 */
	public int countOrigProductions() {
		return norig;
	}
	
	/**
	 * Returns the ID of the specified production.  <code>-1</code> is returned if there is no such
	 * production in this grammar.
	 *  
	 * @param prod the production to look for.
	 * @return the ID of the <code>prod</code> argument.
	 */
	public int getId(Production prod) {
		return numberer.getId(prod, false);
	}
	
	/**
	 * Returns the production with the specified ID.  <code>null</code> is returned if there is no such
	 * production in this grammar.
	 * 
	 * @param id a production ID.
	 * @return the production with the specified ID.
	 */
	public Production getProduction(int id) {
		return (Production) numberer.getObj(id);
	}
	
	/**
	 * Returns an interned copy of the specified production.  If none exists, then the given production
	 * is returned instead.
	 * 
	 * @param prod the production to look for.
	 * @return an interned copy of the <code>prod</code> argument.
	 */
	public Production intern(Production prod) {
		int id = getId(prod);
		return (id < 0) ? prod : getProduction(id);
	}

	/**
	 * Adds a new production to this grammar.  This method returns <code>true</code> if the specified
	 * production is successfully added to this grammar.  If the production is already in the grammar, 
	 * then <code>false</code> is returned.
	 * 
	 * @param prod the production to add.
	 * @return <code>true</code> if the specified production is successfully added to this grammar; 
	 * <code>false</code> otherwise.
	 * @throws RuntimeException if the added production is associative-commutative (AC) but a non-AC
	 * version of it already exists in the grammar, or vice versa.
	 */
	public boolean addProduction(Production prod) {
		if (numberer.addObj(prod)) {
			byLhs[prod.getLhs().getId()].add(prod);
			_byLhs[prod.getLhs().getId()] = null;
			if (prod.isOrig())
				++norig;
			if (prod.length() > 0) {
				Symbol sym = prod.getRhs((short) 0);
				if (prod.isAnaphor())
					anaphorOK.add(((Anaphor) sym).getType());
				if (sym instanceof Nonterminal) {
					lc[prod.getLhs().getId()][sym.getId()] = true;
					_lcTrans = null;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Indicates if the specified production is forced to have zero fertility in a word alignment.
	 * 
	 * @param prod a production in this grammar.
	 * @return <code>true</code> if the <code>prod</code> argument is forced to have zero fertility in a 
	 * word alignment; <code>false</code> otherwise.
	 */
	public boolean isZeroFertility(Production prod) {
		return zeroFert.contains(new Production(prod));
	}
	
	/**
	 * Indicates if expressions of the specified type can be referred to using an <code>Anaphor</code>
	 * symbol.
	 * 
	 * @see wasp.data.Anaphor
	 * @param type a nonterminal.
	 * @return <code>true</code> if expressions of the specified type can be referred to using an
	 * <code>Anaphor</code> symbol; <code>false</code> otherwise.
	 */
	public boolean isAnaphorOK(Nonterminal type) {
		return anaphorOK.contains(type);
	}
	
	/**
	 * Indicates if the nonterminal <code>n2</code> is a <i>left corner</i> of the nonterminal 
	 * <code>n1</code>.   If a nonterminal is a left corner of another, then it is possible for the 
	 * latter nonterminal to generate the former on the left fringe of some parse tree.
	 * 
	 * @param n1 a nonterminal ID.
	 * @param n2 a nonterminal ID.
	 * @return <code>true</code> if the nonterminal <code>n2</code> is a left corner of the nonterminal
	 * <code>n1</code>; <code>false</code> otherwise.
	 */
	public boolean isLeftCorner(int n1, int n2) {
		if (_lcTrans == null)
			_lcTrans = Matrices.reflexiveTransitive(lc);
		return _lcTrans[n1][n2];
	}
	
	///
	/// Tokenizer
	///
	
	/**
	 * Tokenizes the given meaning representation into symbols.
	 * 
	 * @param str a meaning representation.
	 * @return an array of symbols that make up the specified meaning representation.
	 */
	public abstract Symbol[] tokenize(String str);

	/**
	 * Combines the given symbols to create a new meaning representation.
	 * 
	 * @param syms an array of symbols that make up the new meaning representation.
	 * @return the meaning representation made up of the given array of symbols.
	 */
	public abstract String combine(Symbol[] syms);
	
	///
	/// Evaluator
	///
	
	/**
	 * Evaluates the correctness of the automatically-generated MR translations of the specified examples.
	 * These MR translations are stored as strings in the <code>Parse</code> data structure, and can be
	 * retrieved using the <code>Parse.toStr()</code> method.  This method returns a list of Boolean 
	 * arrays such that the <code>i</code>-th element of the <code>j</code>-th array is true if and only 
	 * if the <code>i</code>-th top-scoring parse (i.e. the <code>i</code>-th element of the sorted list 
	 * of <code>Parse</code>s given by the <code>Example.getSortedParses()</code> method) of the 
	 * <code>j</code>-th example is correct.
	 * 
	 * @param examples the set of examples to evaluate.
	 * @param gold examples with gold-standard translations in the <code>Example.F</code> field.
	 * @return a two-dimensional Boolean array, <code>B</code>, such that <code>B[j][i]</code> is true
	 * if and only if the <code>i</code>-th top-scoring parse of the <code>j</code>-th example in the
	 * <code>examples</code> argument is correct.
	 * @throws IOException if an I/O error occurs.
	 */
	public abstract boolean[][] evaluate(Examples examples, Examples gold) throws IOException;
	
	///
	/// File I/O
	///
	
	private static final String MORE_MRL = "more-mrl-productions";
	
	/**
	 * Adds productions to this grammar so that the resulting grammar is unambiguous.  Productions are
	 * read from the file specified in the configuration file (via the key 
	 * <code>Config.MRL_GRAMMAR</code>).  If this file contains something that is not a valid string 
	 * representation of a production, then a <code>RuntimeException</code> is thrown.
	 * 
	 * @throws IOException if an I/O error occurs.
	 * @throws RuntimeException if the specified file contains something that is not a valid string 
	 * representation of a production.
	 */
	public void read() throws IOException {
		String filename = Config.get(Config.MRL_GRAMMAR);
		TokenReader in = new TokenReader(new BufferedReader(new FileReader(filename)));
		String[] line;
		int lineno = 0;
		while ((line = in.readLine()) != null) {
			++lineno;
			Int index = new Int(0);
			Production.readOrig = true;
			Production prod = Production.read(line, index);
			// there shouldn't be any words linked to AC operators
			if (prod.isAC())
				zeroFert.add(prod);
			if (index.val < line.length && line[index.val].equals("var-types")) {
				Denotation varTypes = Denotation.read(line[index.val+1]);
				if (varTypes == null) {
					logger.severe("Invalid variable types in MRL grammar (line "+lineno+" or earlier)");
					throw new RuntimeException();
				}
				if (varTypes.isEmpty() || varTypes.getDim() != prod.countVars()) {
					logger.severe("Invalid variable types in MRL grammar (line "+lineno+")");
					throw new RuntimeException();
				}
				prod.setVarTypes(varTypes);
				index.val += 2;
			}
			if (index.val < line.length && line[index.val].equals("zero-fertility")) {
				zeroFert.add(prod);
				++index.val;
			}
			readModifiers(prod, line, index);
			if (index.val < line.length) {
				logger.severe("Error in MRL grammar (line "+lineno+")");
				throw new RuntimeException();
			}
			addProduction(prod);
		}
		in.close();
	}
	
	/**
	 * Reads the domain-specific modifiers of the specified production from the given line of text,
	 * starting from the token at the specified index.
	 * 
	 * @param prod a production in this grammar.
	 * @param line a line of text containing the domain-specific modifiers of the <code>prod</code>
	 * argument. 
	 * @param index the beginning token index to consider; it is also an <i>output</i> variable for 
	 * the token index immediately after the end of the consumed substring.
	 */
	protected abstract void readModifiers(Production prod, String[] line, Int index);
	
	/**
	 * Adds productions to this grammar.  The resulting grammar is not necessarily unambiguous.  
	 * Productions are read from a file called <code>more-mrl-productions</code> in the directory
	 * specified in the configuration file (via the key <code>Config.MODEL_DIR</code>).  If this file
	 * contains something that is not a valid string representation of a production, then a 
	 * <code>RuntimeException</code> is thrown.  The <code>read</code> method needs to be called 
	 * <i>before</i> this method is called.
	 * 
	 * @throws IOException if an I/O error occurs.
	 * @throws RuntimeException if the specified file contains something that is not a valid string 
	 * representation of a production.
	 */
	public void readMore() throws IOException {
		readMore(new File(Config.getModelDir(), MORE_MRL));
	}
	
	public void readMore(File file) throws IOException {
		TokenReader in = new TokenReader(new BufferedReader(new FileReader(file)));
		String[] line;
		int lineno = 0;
		while ((line = in.readLine()) != null) {
			++lineno;
			Int index = new Int(0);
			Production prod = Production.readParse(line, index);
			if (prod.getVarTypes().isEmpty()) {
				logger.severe("Invalid variable types in extended MRL grammar (line "+lineno+")");
				throw new RuntimeException();
			}
			if (index.val < line.length) {
				logger.severe("Error in extended MRL grammar (line "+lineno+")");
				throw new RuntimeException();
			}
			addProduction(prod);
		}
		in.close();
	}
	
	/**
	 * Writes all productions in this grammar that are <i>not</i> in the original, unambiguous MRL grammar 
	 * to a file called <code>more-mrl-productions</code> in the directory specified in the configuration
	 * file (via the key <code>Config.MODEL_DIR</code>).
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public void writeMore() throws IOException {
		File file = new File(Config.getModelDir(), MORE_MRL);
		PrintWriter out = new PrintWriter(new BufferedWriter(FileWriter.createNew(file)));
		Production[] prods = getProductions();
		for (int i = 0; i < prods.length; ++i)
			if (!prods[i].isOrig())
				out.println(prods[i].getParse());
		out.close();
	}
	
}
