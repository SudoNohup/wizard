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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import wasp.align.WordAlignModel;
import wasp.data.Examples;
import wasp.main.Config;
import wasp.main.TranslationModel;
import wasp.nl.BasicGapModel;
import wasp.nl.GapModel;
import wasp.nl.PSCFGGapModel;
import wasp.scfg.parse.Maxent;
import wasp.scfg.parse.PSCFG;
import wasp.scfg.parse.PSCFGRelativeFreq;
import wasp.scfg.parse.features.ParseFeatures;

/**
 * Translation models based on synchronous context-free grammars (SCFGs) or lambda-SCFGs.
 * 
 * @author ywwong
 *
 */
public class SCFGModel extends TranslationModel {

	private static final Logger logger = Logger.getLogger(SCFGModel.class.getName());
	static {
		logger.setLevel(Level.FINE);
	}

	public SCFG gram;
	public GapModel gm;
	
	// Feature functions for parsing
	public ParseFeatures pf;
	
	public SCFGModel() throws IOException {
		gram = new SCFG();
		gm = GapModel.createNew();
		pf = ParseFeatures.createNew(this);
	}
	
	public void train(Examples examples, boolean full) throws IOException {
		gram.readInit();
		WordAlignModel.createNew().train(examples);
		new RuleExtractor().extract(gram, gm, examples);
		if (!extractOnly) {
			String prob = Config.get(Config.TRANSLATION_PROB_MODEL);
			if (prob.equals("maxent")) {
				new Maxent(this).estimate(examples, full);
				((BasicGapModel) gm).setFillerWeights();
			} else if (prob.equals("pscfg")) {
				new PSCFG(this).estimate(examples, full);
				((PSCFGGapModel) gm).setFillerWeights();
			} else if (prob.equals("pscfg-relative-freq")) {
				new PSCFGRelativeFreq(this).estimate(examples);
				((PSCFGGapModel) gm).setFillerWeights();
			}
		}
		Config.getMRLGrammar().writeMore();
		gram.write();
		gm.write();
		for (int i = 0; i < pf.all.length; ++i)
			pf.all[i].write();
	}
	
	public void read() throws IOException {
		Config.getMRLGrammar().readMore();
		gram.read();
		gm.read();
		for (int i = 0; i < pf.all.length; ++i)
			pf.all[i].read();
	}
	
}