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
import wasp.data.Meaning;
import wasp.main.generate.GenerateModel;
import wasp.nl.NL;

/**
 * The abstract class for tactical generators.
 *  
 * @author ywwong
 *
 */
public abstract class Generator {

	private static Logger logger = Logger.getLogger(Generator.class.getName());
	
	/**
	 * Indicates if this generator supports only batch processing.
	 * 
	 * @return <code>true</code> if this generator supports only batch processing; <code>false</code>
	 * otherwise.
	 */
	public abstract boolean batch();
	
	/**
	 * Finds the <i>K</i> top-scoring generated sentences given a meaning representation.  The value of
	 * <i>K</i> is specified in the configuration file (via the key <code>Config.KBEST</code>).  Use
	 * <i>K</i> = 1 for Viterbi decoding.  This method returns <code>null</code> if this generator 
	 * supports only batch processing.
	 * 
	 * @param F the input meaning representations.
	 * @return the <i>K</i> top-scoring derivations given <code>F</code>.
	 */
	public Iterator generate(Meaning F) throws IOException {
		return null;
	}
	
	/**
	 * Processes the given list of meaning representations as a batch, and finds the <i>K</i> top-scoring
	 * generated sentences of each MR.  The value of <i>K</i> is specified in the configuration file (via
	 * the key <code>Config.KBEST</code>).  This method is called only when this generator supports batch
	 * processing.
	 * 
	 * @param F a list of input meaning representations.
	 * @return a list of top-scoring derivations for each element of <code>F</code>.
	 */
	public Iterator[] generate(Meaning[] F) throws IOException {
		return null;
	}
	
	/**
	 * The main program for NL generation (i.e.&nbsp;translation from MRL into NL).  This program takes the
	 * following command-line arguments:
	 * <p>
	 * <blockquote><code><b>java wasp.main.Generator</b> <u>config-file</u> <u>model-dir</u>
	 * <u>mask-file</u> <u>output-file</u></code></blockquote>
	 * <p>
	 * <ul>
	 * <li><code><u>config-file</u></code> - the configuration file that contains the current 
	 * settings.</li>
	 * <li><code><u>model-dir</u></code> - the directory that contains the learned generation model.</li>
	 * <li><code><u>mask-file</u></code> - the example mask that specifies the test set.</li>
	 * <li><code><u>output-file</u></code> - the output XML file for storing the automatically-generated
	 * sentences. </li>
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
			System.err.println("Usage: java wasp.main.Parser config-file model-dir mask-file output-file");
			System.err.println();
			System.err.println("config-file - the configuration file that contains the current settings.");
			System.err.println("model-dir - the directory that contains the learned generation model.");
			System.err.println("mask-file - the example mask that specifies the test set.");
			System.err.println("output-file - the output XML file for storing the automatically-generated sentences.");
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
		GenerateModel model = GenerateModel.createNew();
		model.read();
		Generator generator = model.getGenerator();
		logger.info("Processing all input meaning representations");
		NL.useNL = Config.getTargetNL();
		if (generator.batch()) {
			Meaning[] F = new Meaning[examples.size()];
			for (int i = 0; i < F.length; ++i) {
				Example ex = examples.getNth(i);
				logger.fine("example "+ex.id);
				F[i] = ex.F;
			}
			Iterator[] R = generator.generate(F);
			for (int i = 0; i < F.length; ++i) {
				Example ex = examples.getNth(i);
				for (int j = 0; R[i].hasNext(); ++j) {
					Parse parse = (Parse) R[i].next();
					ex.parses.add(parse);
				}
			}
		} else
			for (Iterator it = examples.iterator(); it.hasNext();) {
				Example ex = (Example) it.next();
				logger.fine("example "+ex.id);
				int j = 0;
				for (Iterator jt = generator.generate(ex.F); jt.hasNext(); ++j) {
					Parse parse = (Parse) jt.next();
					ex.parses.add(parse);
				}
			}
		logger.info("All input meaning representations have been processed");
		examples.write(outputFilename);
	}
	
}
