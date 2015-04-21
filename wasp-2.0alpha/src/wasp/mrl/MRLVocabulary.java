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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import wasp.data.Nonterminal;
import wasp.data.Symbol;
import wasp.data.Terminal;
import wasp.main.Config;
import wasp.util.FileWriter;
import wasp.util.Int;
import wasp.util.TokenReader;

/**
 * This class provides a mapping between MRL productions and their string representations.  Each MRL
 * production has a unique string representation, which can be useful for certain external applications
 * (e.g. Pharaoh).
 * 
 * @author ywwong
 *
 */
public class MRLVocabulary {
	
	private static final Logger logger = Logger.getLogger(MRLVocabulary.class.getName());
	
	private HashMap prodToToken;
	private HashMap tokenToProd;
	
	/**
	 * Constructs a new mapping between MRL productions and their string representations.
	 */
	public MRLVocabulary() {
		prodToToken = new HashMap();
		tokenToProd = new HashMap();
	}
	
	/**
	 * Adds a new MRL production to this mapping.  The resulting mapping should be one-to-one.  A
	 * warning is given when the mapping is no longer one-to-one.
	 * 
	 * @param prod the MRL production to add.
	 * @param token the string representation of the given production.
	 */
	public void add(Production prod, String token) {
		prodToToken.put(prod, token);
		Production p = (Production) tokenToProd.get(token);
		if (p != null && !p.equals(prod))
			logger.warning("Token "+token+" used for both "+p+" and "+prod);
		tokenToProd.put(token, prod);
	}
	
	/**
	 * Returns the string representation of the given MRL production.
	 * 
	 * @param prod an MRL production.
	 * @return the string representation of <code>prod</code>.
	 */
	public String token(Production prod) {
		return (String) prodToToken.get(prod);
	}
	
	/**
	 * Returns the MRL production that the given string represents.
	 * 
	 * @param token the string representation of an MRL production.
	 * @return the MRL production that the given string represents.
	 */
	public Production prod(String token) {
		Production prod = (Production) tokenToProd.get(token);
		if (prod == null) {
			// any LHS nonterminal will do
			Nonterminal lhs = Config.getMRLGrammar().getStart();
			Symbol[] rhs = new Symbol[1];
			rhs[0] = new Terminal(token, true);
			prod = new Production(lhs, rhs, false, false);
		}
		return prod;
	}

	/**
	 * Retrieves a mapping from the specified file.
	 * 
	 * @param file the file to read.
	 * @throws IOException if an I/O error occurs.
	 */
	public void read(File file) throws IOException {
		TokenReader in = new TokenReader(new BufferedReader(new FileReader(file)));
		String[] line;
		while ((line = in.readLine()) != null) {
			String token = line[0];
			Production.readOrig = false;
			Production prod = Production.read(line, new Int(1)).intern();
			add(prod, token);
		}
		in.close();
	}
	
	/**
	 * Writes this mapping to the specified file.
	 * 
	 * @param file the file to write to.
	 * @throws IOException if an I/O error occurs.
	 */
	public void write(File file) throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(FileWriter.createNew(file)));
		for (Iterator it = tokenToProd.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			String token = (String) entry.getKey();
			Production prod = (Production) entry.getValue();
			out.print(token);
			out.print(' ');
			out.println(prod);
		}
		out.close();
	}
	
}