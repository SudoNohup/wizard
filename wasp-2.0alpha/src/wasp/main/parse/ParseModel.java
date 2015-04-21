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
package wasp.main.parse;

import java.io.IOException;

import wasp.data.Examples;
import wasp.main.Config;
import wasp.main.Parser;
import wasp.pharaoh.parse.PharaohModel;
import wasp.rewrite.parse.RewriteModel;

/**
 * The abstract class for parsing models.
 * 
 * @author ywwong
 *
 */
public abstract class ParseModel {

	/**
	 * Creates and returns a new semantic parsing model of the type specified in the configuration
	 * file (via the key <code>Config.PARSE_MODEL</code>).  Three types of parsing models are
	 * recognized: <code>direct</code> for the direct parsing model of WASP; <code>pharaoh</code>
	 * for the Pharaoh-based parsing model; and <code>rewrite</code> for the Rewrite-based parsing
	 * model.  <code>null</code> is returned if the given model type is not recognized.
	 * 
	 * @return a new semantic parsing model of the specified type.
	 * @throws IOException if an I/O error occurs.
	 */
	public static ParseModel createNew() throws IOException {
		String type = Config.getParseModel();
		if (type.equals("direct"))
			return new DirectModel();
		else if (type.equals("pharaoh"))
			return new PharaohModel();
		else if (type.equals("rewrite"))
			return new RewriteModel();
		return null;
	}
	
	/**
	 * Creates and returns a new parser based on this parsing model.
	 * 
	 * @return a new parser based on this parsing model.
	 * @throws IOException if an I/O error occurs.
	 */
	public abstract Parser getParser() throws IOException;
	
	/**
	 * Trains this parsing model using the specified training examples.
	 * 
	 * @param examples a set of training examples.
	 * @param full indicates if more time should be spent on training to allow better results.
	 * @throws IOException if an I/O error occurs.
	 */
	public abstract void train(Examples examples, boolean full) throws IOException;

	/**
	 * Trains this parsing model using the specified training examples with more time spent in
	 * order to obtain a better model.
	 * 
	 * @param examples a set of training examples.
	 * @throws IOException if an I/O error occurs.
	 */
	public void train(Examples examples) throws IOException {
		train(examples, true);
	}
	
	/**
	 * Retrieves a previously-trained semantic parser from the directory specified in the
	 * configuration file (via the key <code>Config.MODEL_DIR</code>).
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public abstract void read() throws IOException;
	
}
