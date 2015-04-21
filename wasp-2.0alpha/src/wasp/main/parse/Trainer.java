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

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import wasp.data.ExampleMask;
import wasp.data.Examples;
import wasp.main.Config;
import wasp.main.TranslationModel;
import wasp.nl.NL;

/**
 * The command-line interface to the trainer of semantic parsers.
 * 
 * @author ywwong
 *
 */
public class Trainer {

	private Trainer() {}
	
	/**
	 * The main program for training semantic parsing models.  This program takes the following
	 * command-line arguments:
	 * <p>
	 * <blockquote><code><b>java wasp.main.parse.Trainer</b> [extract-only] <u>config-file</u>
	 * <u>model-dir</u> <u>mask-file</u></code></blockquote>
	 * <p>
	 * <ul>
	 * <li><code><u>config-file</u></code> - the configuration file that contains the current 
	 * settings.</li>
	 * <li><code><u>model-dir</u></code> - the directory for storing the learned semantic parsing
	 * model.</li>
	 * <li><code><u>mask-file</u></code> - the example mask that specifies the training set.</li>
	 * </ul>
	 * <p>
	 * <b>Options:</b>
	 * <p>
	 * <ul>
	 * <li><code>-extract-only</code> - extracts rules only, no parameter estimation.</li>
	 * </ul>
	 * <p>
	 * Log messages are sent to the standard error stream, which can be captured for detailed error
	 * analysis.
	 * 
	 * @param args the command-line arguments.
	 * @throws IOException if an I/O error occurs.
	 * @throws SAXException if the XML parser throws a <code>SAXException</code> while parsing.
	 * @throws ParserConfigurationException if an XML parser cannot be created which satisfies the 
	 * requested configuration.
	 */
	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		if (args.length < 3 || args.length > 4) {
			System.err.println("Usage: java wasp.main.parse.Trainer [-extract-only] config-file model-dir mask-file");
			System.err.println();
			System.err.println("config-file - the configuration file that contains the current settings.");
			System.err.println("model-dir - the directory for storing the learned semantic parsing model.");
			System.err.println("mask-file - the example mask that specifies the training set.");
			System.err.println();
			System.err.println("Options:");
			System.err.println("-extract-only - extracts rules only, no parameter estimation.");
			System.exit(1);
		}
		int index = 0;
		if (args[index].equals("-extract-only")) {
			TranslationModel.extractOnly = true;
			++index;
		}
		String configFilename = args[index++];
		String modelDir = args[index++];
		String maskFilename = args[index++];
		
		Config.read(configFilename);
		Config.setModelDir(modelDir);
		Examples examples = Config.readCorpus();
		ExampleMask mask = new ExampleMask();
		mask.read(maskFilename);
		examples = mask.apply(examples);
		NL.useNL = Config.getSourceNL();
		ParseModel.createNew().train(examples);
	}
	
}
