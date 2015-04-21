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

import java.util.ArrayList;
import java.util.TreeMap;

import wasp.align.WordAlign;
import wasp.main.Config;
import wasp.main.Parse;
import wasp.nl.NL;
import wasp.util.Arrays;
import wasp.util.ComparablePair;
import wasp.util.Copyable;

/**
 * Examples consisting NL sentences and MRs.
 * 
 * @author ywwong
 *
 */
public class Example implements Copyable {

	/** The example ID. */
	public int id;
	/** The original sentences in various languages. */
	public TreeMap nlMap;
	/** Syntactically annotated sentences in various languages. */
	public TreeMap synMap;
	/** Syntactically and semantically annotated sentences in various languages. */
	public TreeMap augsynMap;
	/** The correct meaning representation in various MRLs. */
	public TreeMap mrlMap;
	/** The correct linearized MR parse in various MRLs. */
	public TreeMap mrlparseMap;
	
	/** The preprocessed, tokenized NL sentences in various languages. */
	public TreeMap E;
	/** The correct meaning representation. */
	public Meaning F;
	/** The correct linearized MR parse. */
	public ArrayList Fparse;
	/** The gold-standard word alignment between the sentence and the correct linearized MR parse. */
	public WordAlign EFalign;

	/** Word alignment between the sentence and the linearized MR parse, sorted in descending order of 
	 * alignment scores. */
	public ArrayList aligns;
	/** Automatically-generated parses, sorted in descending order of parse scores. */
	public ArrayList parses;
	
	public Example() {
		nlMap = new TreeMap();
		synMap = new TreeMap();
		augsynMap = new TreeMap();
		mrlMap = new TreeMap();
		mrlparseMap = new TreeMap();
		E = new TreeMap();
		F = null;
		Fparse = null;
		EFalign = null;
		aligns = new ArrayList();
		parses = new ArrayList();
	}
	
	/**
	 * Returns a shallow copy of this example.
	 */
	public Object copy() {
		Example ex = new Example();
		ex.id = id;
		ex.nlMap = nlMap;
		ex.synMap = synMap;
		ex.augsynMap = augsynMap;
		ex.mrlMap = mrlMap;
		ex.mrlparseMap = mrlparseMap;
		ex.E = E;
		ex.F = F;
		ex.Fparse = Fparse;
		ex.EFalign = EFalign;
		ex.aligns.addAll(aligns);
		ex.parses.addAll(parses);
		return ex;
	}
	
	public String nl() {
		return (String) nlMap.get(NL.useNL);
	}
	
	public String syn() {
		return (String) synMap.get(NL.useNL);
	}
	
	public String augsyn() {
		return (String) augsynMap.get(new ComparablePair(NL.useNL, Config.getMRL()));
	}

	/**
	 * Returns the preprocessed, tokenized NL sentence in the current language.
	 * 
	 * @return the preprocessed, tokenized NL sentence in the current language.
	 */
	public Terminal[] E() {
		return (Terminal[]) E.get(NL.useNL);
	}
	
	public Terminal[] E(String lang) {
		return (Terminal[]) E.get(lang);
	}
	
	public WordAlign[] getSortedAligns() {
		WordAlign[] array = (WordAlign[]) aligns.toArray(new WordAlign[0]);
		Arrays.sort(array);
		return array;
	}
	
	public Parse[] getSortedParses() {
		Parse[] array = (Parse[]) parses.toArray(new Parse[0]);
		Arrays.sort(array);
		return array;
	}
	
}
