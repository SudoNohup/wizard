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
package wasp.main.mt;

import java.io.IOException;

import wasp.data.Examples;
import wasp.main.Config;
import wasp.main.Translator;
import wasp.pharaoh.mt.PharaohModel;

/**
 * The abstract class for machine translators.
 * 
 * @author ywwong
 *
 */
public abstract class MTModel {

	/**
	 * Creates and returns a new MT model.  Two types of MT models are recognized:
	 * <code>interlingual</code> for interlingual MT; and <code>pharaoh</code> for direct translation 
	 * using Pharaoh.  <code>null</code> is returned if the given model type is not recognized. 
	 * 
	 * @return a new MT model of the specified type.
	 * @throws IOException if an I/O error occurs.
	 */
	public static MTModel createNew() throws IOException {
		String type = Config.get(Config.MT_MODEL);
		if (type.equals("interlingual"))
			return new InterlingualModel();
		else if (type.equals("pharaoh"))
			return new PharaohModel();
		return null;
	}
	
	/**
	 * Creates and returns a new translator based on this MT model.
	 * 
	 * @return a new translator based on this MT model.
	 * @throws IOException if an I/O error occurs.
	 */
	public abstract Translator getTranslator() throws IOException;
	
	/**
	 * Trains this MT model using the specified training examples.
	 * 
	 * @param examples a set of training examples
	 * @throws IOException if an I/O error occurs.
	 */
	public abstract void train(Examples examples) throws IOException;
	
	/**
	 * Retrieves a previously-trained MT model from the directory specified in the configuration file (via
	 * the key <code>Config.MODEL_DIR</code>).
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public abstract void read() throws IOException;
	
}
