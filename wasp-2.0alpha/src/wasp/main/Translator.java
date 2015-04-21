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
import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import wasp.data.Example;
import wasp.data.ExampleMask;
import wasp.data.Examples;
import wasp.data.Terminal;
import wasp.main.mt.MTModel;
import wasp.nl.NL;

/**
 * The abstract class for machine translators.
 *  
 * @author ywwong
 *
 */
public abstract class Translator {

	private static Logger logger = Logger.getLogger(Translator.class.getName());
	
	/**
	 * Indicates if this translator supports only batch processing.
	 * 
	 * @return <code>true</code> if this translator supports only batch processing; <code>false</code>
	 * otherwise.
	 */
	public abstract boolean batch();
	
	/**
	 * Finds the <i>K</i> top-scoring translations given an NL sentence.  The value of <i>K</i> is
	 * specified in the configuration file (via the key <code>Config.KBEST</code>).  Use <i>K</i> =
	 * 1 for Viterbi decoding.  This method returns <code>null</code> if this translator supports
	 * only batch processing.
	 * <p>
	 * The source NL is specified in the configuration file via the key <code>Config.NL_SOURCE</code>.
	 * The target NL is the default NL.
	 * 
	 * @param E the input NL sentence broken into words.
	 * @return the <i>K</i> top-scoring translations given <code>E</code>.
	 */
	public Iterator translate(Terminal[] E) throws IOException {
		return null;
	}
	
	/**
	 * Processes the given list of NL sentences as a batch, and finds the <i>K</i> top-scoring
	 * translations of each sentence.  The value of <i>K</i> is specified in the configuration
	 * file (via the key <code>Config.KBEST</code>).  This method should be called only when this
	 * translator supports batch processing.
	 * <p>
	 * The source NL is specified in the configuration file via the key <code>Config.NL_SOURCE</code>.
	 * The target NL is the default NL.
	 * 
	 * @param E a list of input NL sentences.
	 * @return a list of top-scoring translations for each element of <code>E</code>.
	 */
	public Iterator[] translate(Terminal[][] E) throws IOException {
		return null;
	}
	
	/**
	 * The main program for NL-to-NL machine translation.  This program takes the following command-line
	 * arguments:
	 * <p>
	 * <blockquote><code><b>java wasp.main.Translator</b> <u>config-file</u> <u>model-dir</u>
	 * <u>mask-file</u> <u>output-file</u></code></blockquote>
	 * <p>
	 * <ul>
	 * <li><code><u>config-file</u></code> - the configuration file that contains the current 
	 * settings.</li>
	 * <li><code><u>model-dir</u></code> - the directory that contains the learned translation model.</li>
	 * <li><code><u>mask-file</u></code> - the example mask that specifies the test set.</li>
	 * <li><code><u>output-file</u></code> - the output XML file for storing the automatic translations.</li>
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
		if (args.length != 4) {
			System.err.println("Usage: java wasp.main.Translator config-file model-dir mask-file output-file");
			System.err.println();
			System.err.println("config-file - the configuration file that contains the current settings.");
			System.err.println("model-dir - the directory that contains the learned translation model.");
			System.err.println("mask-file - the example mask that specifies the test set.");
			System.err.println("output-file - the output XML file for storing the automatic translations.");
			System.exit(1);
		}
		String configFilename = args[0];
		String modelDir = args[1];
		String maskFilename = args[2];
		String outputFilename = args[3];
		
		Config.read(configFilename);
		Config.setModelDir(modelDir);
		Examples examples = Config.readCorpus();
		ExampleMask mask = new ExampleMask();
		mask.read(maskFilename);
		examples = mask.apply(examples);
		MTModel model = MTModel.createNew();
		model.read();
		Translator translator = model.getTranslator();
		logger.info("Translating all input sentences");
		NL.useNL = Config.getSourceNL();
		if (translator.batch()) {
			Terminal[][] E = new Terminal[examples.size()][];
			for (int i = 0; i < E.length; ++i) {
				Example ex = examples.getNth(i);
				logger.fine("example "+ex.id);
				E[i] = ex.E();
			}
			Iterator[] T = translator.translate(E);
			for (int i = 0; i < E.length; ++i) {
				Example ex = examples.getNth(i);
				for (int j = 0; T[i].hasNext(); ++j) {
					Parse parse = (Parse) T[i].next();
					ex.parses.add(parse);
				}
			}
		} else
			for (Iterator it = examples.iterator(); it.hasNext();) {
				Example ex = (Example) it.next();
				logger.fine("example "+ex.id);
				for (Iterator jt = translator.translate(ex.E()); jt.hasNext();)
					ex.parses.add(jt.next());
			}
		logger.info("All input sentences have been processed");
		examples.write(outputFilename);
	}
	
}
