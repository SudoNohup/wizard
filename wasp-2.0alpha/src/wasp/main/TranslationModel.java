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
package wasp.main;

import java.io.IOException;

import wasp.data.Examples;
import wasp.scfg.SCFGModel;

/**
 * The abstract class for translation models.  Translation models are used in semantic parsing as well as
 * tactical generation (e.g. WASP<sup>-1</sup>).
 * 
 * @author ywwong
 *
 */
public abstract class TranslationModel {

	/**
	 * Creates and returns a new translation model of the type specified in the configuration file (via
	 * the key <code>Config.TRANSLATION_MODEL</code>).  Currently, one type of translation model is 
	 * recognized: <code>scfg</code> for synchronous context-free grammar.  <code>null</code> is returned 
	 * if the given model type is not recognized.
	 * 
	 * @return a new translation model of the specified type.
	 * @throws IOException if an I/O error occurs.
	 */
	public static TranslationModel createNew() throws IOException {
		String type = Config.getTranslationModel();
		if (type.equals("scfg")) {
			return new SCFGModel();
		}
		return null;
	}
	
	public static boolean extractOnly = false;
	
	/**
	 * Trains this translation model using the specified training examples.
	 * 
	 * @param examples a set of training examples.
	 * @param full indicates if more time should be spent on training to allow better results.
	 * @throws IOException if an I/O error occurs.
	 */
	public abstract void train(Examples examples, boolean full) throws IOException;

	/**
	 * Trains this translation model using the specified training examples with more time spent in
	 * order to obtain a better model.
	 * 
	 * @param examples a set of training examples.
	 * @throws IOException if an I/O error occurs.
	 */
	public void train(Examples examples) throws IOException {
		train(examples, true);
	}
	
	/**
	 * Retrieves a previously learned translation model from the directory specified in the configuration
	 * file (via the key <code>Config.MODEL_DIR</code>).
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public abstract void read() throws IOException;
	
}
