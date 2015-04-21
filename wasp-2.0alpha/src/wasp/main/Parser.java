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
import wasp.data.Terminal;
import wasp.main.parse.ParseModel;
import wasp.nl.NL;

/**
 * The abstract class for semantic parsers.
 *  
 * @author ywwong
 *
 */
public abstract class Parser {

	private static Logger logger = Logger.getLogger(Parser.class.getName());
	
	/**
	 * Indicates if this parser supports only batch processing.
	 * 
	 * @return <code>true</code> if this parser supports only batch processing; <code>false</code>
	 * otherwise.
	 */
	public abstract boolean batch();
	
	/**
	 * Finds the <i>K</i> top-scoring parses of the given NL sentence.  The value of <i>K</i> is specified
	 * in the configuration file (via the key <code>Config.KBEST</code>).  Use <i>K</i> = 1 for Viterbi
	 * decoding.  This method returns <code>null</code> if this parser supports only batch processing.
	 * 
	 * @param E the input NL sentence.
	 * @return the <i>K</i> top-scoring parses of <code>E</code>.
	 */
	public Iterator parse(Terminal[] E) throws IOException {
		return null;
	}
	
	/**
	 * Processes the given list of NL sentences as a batch, and finds the <i>K</i> top-scoring parses of
	 * each sentence.  The value of <i>K</i> is specified in the configuration file (via the key
	 * <code>Config.KBEST</code>).  This method returns <code>null</code> if this parser does not support
	 * batch processing.
	 * 
	 * @param E a list of input NL sentences.
	 * @return a list of top-scoring parses for each element of <code>E</code>.
	 */
	public Iterator[] parse(Terminal[][] E) throws IOException {
		return null;
	}
	
	/**
	 * Finds the <i>K</i> top-scoring parses of the given NL sentence which are consistent with the given
	 * meaning representation.  The value of <i>K</i> is specified in the configuration file (via the
	 * key <code>Config.KBEST</code>).  This method returns <code>null</code> if this parser supports
	 * only batch processing, or if this parser does not support this operation.
	 * 
	 * @param E the input NL sentence.
	 * @param F the meaning representation of <code>E</code>.
	 * @return the <i>K</i> top-scoring parses of <code>E</code> which are consistent with <code>F</code>.
	 * @throws IOException if an I/O error occurs.
	 */
	public Iterator parse(Terminal[] E, Meaning F) throws IOException {
		return null;
	}
	
	/**
	 * Processes the given list of NL sentences as a batch, and finds the <i>K</i> top-scoring parses of
	 * each sentence which are consistent with its corresponding meaning representations.  The value of
	 * <i>K</i> is specified in the configuration file (via the key <code>Config.KBEST</code>).  This 
	 * method returns <code>null</code> if this parser does not support batch processing, or if this
	 * parser does not support this operation.
	 * 
	 * @param E a list of input NL sentences.
	 * @param F a list of meaning representations for each element of <code>E</code>.
	 * @return a list of top-scoring parses for each element of <code>E</code> which are consistent with
	 * the corresponding MR in <code>F</code>.
	 * @throws IOException if an I/O error occurs.
	 */
	public Iterator[] parse(Terminal[][] E, Meaning[] F) throws IOException {
		return null;
	}
	
	/**
	 * The main program for parsing (i.e.&nbsp;translation from NL into MRL).  This program takes the following
	 * command-line arguments:
	 * <p>
	 * <blockquote><code><b>java wasp.main.Parser</b> [-oracle] <u>config-file</u> <u>model-dir</u> <u>mask-file</u>
	 * <u>output-file</u></code></blockquote>
	 * <p>
	 * <ul>
	 * <li><code><u>config-file</u></code> - the configuration file that contains the current 
	 * settings.</li>
	 * <li><code><u>model-dir</u></code> - the directory that contains the learned parsing model.</li>
	 * <li><code><u>mask-file</u></code> - the example mask that specifies the test set.</li>
	 * <li><code><u>output-file</u></code> - the output XML file for storing the automatically-generated
	 * parses. </li>
	 * </ul>
	 * <p>
	 * <b>Options:</b>
	 * <p>
	 * <ul>
	 * <li><code>-oracle</code> - returns only parses that are consistent with correct MRs.</li>
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
		if (args.length < 4 || args.length > 5) {
			System.err.println("Usage: java wasp.main.Parser [-oracle] config-file model-dir mask-file output-file");
			System.err.println();
			System.err.println("config-file - the configuration file that contains the current settings.");
			System.err.println("model-dir - the directory that contains the learned parsing model.");
			System.err.println("mask-file - the example mask that specifies the test set.");
			System.err.println("output-file - the output XML file for storing the automatically-generated parses.");
			System.err.println();
			System.err.println("Options:");
			System.err.println("-oracle - returns only parses that are consistent with correct MRs.");
			System.exit(1);
		}
		boolean oracle = false;
		int index = 0;
		if (args[index].equals("-oracle")) {
			oracle = true;
			++index;
		}
		String configFilename = args[index++];
		String modelDir = args[index++];
		String maskFilename = args[index++];
		String outputFilename = args[index++];
		
		Config.read(configFilename);
		Config.setModelDir(modelDir);
		Examples examples = Config.readCorpus();
		ExampleMask mask = new ExampleMask();
		mask.read(maskFilename);
		examples = mask.apply(examples);
		ParseModel model = ParseModel.createNew();
		model.read();
		Parser parser = model.getParser();
		logger.info("Parsing all input sentences");
		NL.useNL = Config.getSourceNL();
		if (parser.batch()) {
			Terminal[][] E = new Terminal[examples.size()][];
			Meaning[] F = new Meaning[examples.size()];
			for (int i = 0; i < E.length; ++i) {
				Example ex = examples.getNth(i);
				logger.fine("example "+ex.id);
				E[i] = ex.E();
				F[i] = ex.F;
			}
			Iterator[] P = (oracle) ? parser.parse(E, F) : parser.parse(E);
			for (int i = 0; i < E.length; ++i) {
				Example ex = examples.getNth(i);
				while (P[i].hasNext())
					ex.parses.add(P[i].next());
			}
		} else
			for (Iterator it = examples.iterator(); it.hasNext();) {
				Example ex = (Example) it.next();
				logger.fine("example "+ex.id);
				Iterator P = (oracle) ? parser.parse(ex.E(), ex.F) : parser.parse(ex.E());
				while (P.hasNext())
					ex.parses.add(P.next());
			}
		logger.info("All input sentences have been processed");
		examples.write(outputFilename);
	}
	
}
