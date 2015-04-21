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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import wasp.domain.GeoGapModel;
import wasp.domain.RoboCupCLangGapModel;
import wasp.main.Config;
import wasp.util.FileWriter;
import wasp.util.TokenReader;

/**
 * This model assigns weights to words generated from word gaps.  The formulation of the word-gap model 
 * is specific to the translation probability model and the application domain.
 * 
 * @author ywwong
 *
 */
public abstract class GapModel {

	/**
	 * Creates and returns a new word-gap model suitable for the current domain and translation
	 * probability model.  If there is no specific model that matches, then a basic word-gap model is
	 * returned by default.  
	 * 
	 * @return a new word-gap model suitable for the current domain and translation probability model.
	 */
	public static GapModel createNew() throws IOException {
		String prob = Config.get(Config.TRANSLATION_PROB_MODEL);
		if (prob.equals("pscfg") || prob.equals("pscfg-relative-freq"))
			return new PSCFGGapModel();
		else if (prob.equals("relative-freq"))
			return new BasicGapModel();
		else if (prob.equals("maxent")) {
			String mrl = Config.getMRL();
			if (mrl.equals("geo-funql") || mrl.equals("geo-prolog"))
				return new GeoGapModel();
			else if (mrl.equals("robocup-clang"))
				return new RoboCupCLangGapModel();
		}
		return new BasicGapModel();
	}
	
	private static final String GAP_MODEL = "gap-model";
	
	/**
	 * Reads the model parameters.  Model parameters are read from a file called <code>gap-model</code>
	 * in the directory specified in the configuration file (via the key <code>Config.MODEL_DIR</code>).
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public abstract void read() throws IOException;
	
	protected static TokenReader getReader() throws IOException {
		File file = new File(Config.getModelDir(), GAP_MODEL);
		return new TokenReader(new BufferedReader(new FileReader(file)));
	}
	
	/**
	 * Writes the model parameters to a file called <code>gap-model</code> in the directory specified
	 * in the configuration file (via the key <code>Config.MODEL_DIR</code>).
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public abstract void write() throws IOException;
	
	protected static PrintWriter getWriter() throws IOException {
		File file = new File(Config.getModelDir(), GAP_MODEL);
		return new PrintWriter(new BufferedWriter(FileWriter.createNew(file)));
	}
	
}
