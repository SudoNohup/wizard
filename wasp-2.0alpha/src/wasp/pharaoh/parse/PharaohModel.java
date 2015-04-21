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
package wasp.pharaoh.parse;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import wasp.data.Example;
import wasp.data.Examples;
import wasp.main.Config;
import wasp.main.Parser;
import wasp.main.parse.ParseModel;
import wasp.mrl.MRLVocabulary;
import wasp.mrl.Production;
import wasp.mrl.SRINgramModel;
import wasp.scfg.Rule;
import wasp.scfg.SCFG;

/**
 * A semantic parsing model based on Pharaoh.
 * 
 * @author ywwong
 *
 */
public class PharaohModel extends ParseModel {

	private MRLVocabulary vocab;
	private PhraseTranslationModel tm;
	private SRINgramModel lm;
	
	/**
	 * Constructs a new semantic parsing model based on Pharaoh.
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public PharaohModel() throws IOException {
		vocab = new MRLVocabulary();
		lm = new SRINgramModel(vocab);
		tm = new PhraseTranslationModel(lm, vocab);
	}
	
	public Parser getParser() throws IOException {
		return new PharaohParser(tm, vocab);
	}

	private static String MRL_VOCAB = "mrl-vocab";
	
	public void train(Examples examples, boolean full) throws IOException {
		setVocab(examples);
		vocab.write(new File(Config.getModelDir(), MRL_VOCAB));
		lm.train(examples);
		tm.train(examples);
	}

	private void setVocab(Examples examples) throws IOException {
		for (Iterator it = examples.iterator(); it.hasNext();) {
			Example ex = (Example) it.next();
			for (short j = 0; j < ex.F.lprods.length; ++j) {
				Production prod = ex.F.lprods[j];
				if (!prod.isUnary())
					vocab.add(prod, token(prod));
			}
		}
		SCFG gram = new SCFG();
		gram.readInit();
        Rule[] rules = gram.getRules();
        for (int i = 0; i < rules.length; ++i)
        	if (rules[i].countArgs() == 0 && !rules[i].isWildcard()) {
        		Production prod = rules[i].getProduction();
        		vocab.add(prod, token(prod));
        	}
	}

	private String token(Production prod) {
		if (prod.isWildcardMatch())
			return prod.getRhs((short) 0).toString();
		else
			return prod.toString().replaceAll("\\W|_", "").toLowerCase();
	}
	
	public void read() throws IOException {
		vocab.read(new File(Config.getModelDir(), MRL_VOCAB));
		tm.read();
		lm.read();
	}

}
