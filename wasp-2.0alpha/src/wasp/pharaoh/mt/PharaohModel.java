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
package wasp.pharaoh.mt;

import java.io.IOException;

import wasp.data.Examples;
import wasp.main.Config;
import wasp.main.Translator;
import wasp.main.mt.MTModel;
import wasp.nl.NL;
import wasp.nl.NgramModel;
import wasp.nl.SRINgramModel;

/**
 * An MT model based on Pharaoh.
 * 
 * @author ywwong
 *
 */
public class PharaohModel extends MTModel {

	private PhraseTranslationModel tm;
	private NgramModel lm;
	
	/**
	 * Constructs a new MT model based on Pharaoh.
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public PharaohModel() throws IOException {
		lm = new SRINgramModel();
		tm = new PhraseTranslationModel(lm);
	}
	
	public Translator getTranslator() throws IOException {
		return new PharaohTranslator(tm);
	}

	public void train(Examples examples) throws IOException {
		NL.useNL = Config.getTargetNL();
		lm.train(examples);
		tm.train(examples);
	}

	public void read() throws IOException {
		tm.read();
		lm.read();
	}

}
