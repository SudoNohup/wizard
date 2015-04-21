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
package wasp.main.generate;

import java.io.IOException;

import wasp.data.Examples;
import wasp.main.Config;
import wasp.main.Generator;
import wasp.pharaoh.generate.PharaohModel;
import wasp.rewrite.generate.RewriteModel;

/**
 * The abstract class for tactical generation models.
 * 
 * @author ywwong
 *
 */
public abstract class GenerateModel {

	/**
	 * Creates and returns a new generation model of the type specified in the configuration file (via
	 * the key <code>Config.GENERATE_MODEL</code>).  Four types of generation models are recognized:
	 * <code>log-linear</code> for the log-linear model with fixed component weights;
	 * <code>min-error-rate</code> for the log-linear model with minimum error-rate training; 
	 * <code>pharaoh</code> for the Pharaoh-based model; and <code>rewrite</code> for the
	 * Rewrite-based model.  <code>null</code> is returned if the given model type is not recognized. 
	 * 
	 * @return a new generation model of the specified type.
	 * @throws IOException if an I/O error occurs.
	 */
	public static GenerateModel createNew() throws IOException {
		String type = Config.getGenerateModel();
		if (type.equals("log-linear"))
			return new LogLinearModel();
		else if (type.equals("min-error-rate"))
			return new MinErrorRateModel();
		else if (type.equals("pharaoh"))
			return new PharaohModel();
		else if (type.equals("rewrite"))
			return new RewriteModel();
		return null;
	}
	
	/**
	 * Creates and returns a new NL generator based on this generation model.
	 * 
	 * @return a new NL generator based on this generation model.
	 * @throws IOException if an I/O error occurs.
	 */
	public abstract Generator getGenerator() throws IOException;
	
	/**
	 * Trains this NL generation model using the specified training examples.
	 * 
	 * @param examples a set of training examples
	 * @throws IOException if an I/O error occurs.
	 */
	public abstract void train(Examples examples) throws IOException;
	
	/**
	 * Retrieves a previously-trained NL generation model from the directory specified in the
	 * configuration file (via the key <code>Config.MODEL_DIR</code>).
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public abstract void read() throws IOException;
	
}
