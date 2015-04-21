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
package wasp.nl;

import java.io.IOException;

import wasp.data.Dictionary;
import wasp.data.Examples;
import wasp.data.Terminal;
import wasp.main.Config;

/**
 * The abstract class for NL language models.
 * 
 * @author ywwong
 *
 */
public abstract class NLModel {

	/**
	 * Creates and returns a new NL language model of the type specified in the configuration file (via
	 * the key <code>Config.NL_MODEL</code>).  <code>null</code> is returned if the given model type is 
	 * not recognized.
	 * 
	 * @return a new NL language model of the specified type.
	 * @throws IOException if an I/O error occurs.
	 */
	public static NLModel createNew() throws IOException {
		String type = Config.getNLModel();
		if (type.equals("ngram"))
			return new SRINgramModel();
		return null;
	}
	
	/**
	 * Trains this NL language model using the specified training examples.
	 * 
	 * @param examples a set of training examples.
	 * @throws IOException if an I/O error occurs.
	 */
	public abstract void train(Examples examples) throws IOException;

	protected static String token(Terminal word) {
		return Dictionary.term(word.getId());
	}
	
	/**
	 * Retrieves a previously learned NL language model from the directory specified in the configuration
	 * file (via the key <code>Config.MODEL_DIR</code>).
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public abstract void read() throws IOException;
	
}
