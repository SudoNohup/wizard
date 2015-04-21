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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import wasp.data.Dictionary;
import wasp.data.Example;
import wasp.data.Examples;
import wasp.data.Meaning;
import wasp.data.Terminal;
import wasp.main.Config;
import wasp.main.Parse;
import wasp.nl.NL;
import wasp.util.Arrays;
import wasp.util.FileWriter;

/**
 * The program for evaluating tactical generators.  Macroaveraging is used to obtain the average
 * statistics across all trials.
 * 
 * @author ywwong
 *
 */
public abstract class Evaluator {

	private static Logger logger = Logger.getLogger(Evaluator.class.getName());
	
	private static Examples read(PrintWriter out, Examples gold, String filename)
	throws IOException, SAXException, ParserConfigurationException {
		Examples examples = new Examples();
		examples.read(filename);
		out.println("file "+filename);
		int size = examples.size();
		for (int i = 0; i < size; ++i) {
			Example ex = examples.getNth(i);
			out.println("example "+ex.id);
			out.println("correct translation:");
			out.println(normalize(gold.get(ex.id).E()));
			Parse[] parses = ex.getSortedParses();
			for (int j = 0; j < parses.length; ++j) {
				out.println("parse "+j+":");
				String E = normalize(parses[j].toStr());
				out.println(E);
			}
		}
		return examples;
	}
	
	protected static String normalize(Meaning m) {
		return Config.getMRLGrammar().combine(m.syms);
	}
	
	protected static String normalize(String str) {
		return normalize(new NL().tokenize(str));
	}
	
	protected static String normalize(Terminal[] E) {
		StringBuffer sb = new StringBuffer();
		for (short i = 1; i < E.length-1; ++i) {
			if (i > 1)
				sb.append(' ');
			sb.append(Dictionary.term(E[i].getId()));
		}
		return sb.toString();
	}
	
	/**
	 * Evaluates the given generated sentences using a particular evaluation metric.  The results are
	 * written to the specified character stream.
	 * 
	 * @param out the character stream to write to.
	 * @param gold the gold standard to compare against.
	 * @param examples the test examples to evaluate; there is a set of test examples for each trial.
	 * @throws IOException if an I/O error occurs.
	 */
	protected abstract void evaluate(PrintWriter out, Examples gold, Examples[] examples)
	throws IOException;
	
	/**
	 * The main program for evaluating the performance of NL generators.  This program takes the following 
	 * command-line arguments:
	 * <p>
	 * <blockquote><code><b>java wasp.main.generate.Evaluator</b> <u>config-file</u>
	 * <u>output-file</u> <u>input-file</u> ...</code></blockquote>
	 * <p>
	 * <ul>
	 * <li><code><u>config-file</u></code> - the configuration file that contains the current 
	 * settings.</li>
	 * <li><code><u>output-file</u></code> - the output text file for storing the evaluation results.</li>
	 * <li><code><u>input-file</u></code> - the input XML file that contains the generated sentences to
	 * evaluate.</li>
	 * </ul>
	 * <p>
	 * If there are more than one input XML file, then each file represents a separate trial, and
	 * macroaveraging will be used to obtain the average statistics across all trials.  Log messages are
	 * sent to the standard error stream, which can be captured for detailed error analysis.
	 * 
	 * @param args the command-line arguments.
	 * @throws IOException if an I/O error occurs.
	 * @throws SAXException if the XML parser throws a <code>SAXException</code> while parsing.
	 * @throws ParserConfigurationException if an XML parser cannot be created which satisfies the 
	 * requested configuration.
	 */
	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		if (args.length < 3) {
			System.err.println("Usage: java wasp.main.generate.Evaluator config-file output-file input-file ...");
			System.err.println();
			System.err.println("config-file - the configuration file that contains the current settings.");
			System.err.println("output-file - the output text file for storing the evaluation results.");
			System.err.println("input-file - the input XML file that contains the generated sentences to evaluate.");
			System.exit(1);
		}
		String configFilename = args[0];
		String outputFilename = args[1];
		String[] inputFilenames = (String[]) Arrays.subarray(args, 2, args.length);
		
		Config.read(configFilename);
		logger.info("Evaluation starts");
		NL.useNL = Config.getTargetNL();
		PrintWriter out = new PrintWriter(new BufferedWriter(FileWriter.createNew(outputFilename)));
		Examples gold = Config.readCorpus();
		Examples[] examples = new Examples[inputFilenames.length];
		for (int i = 0; i < inputFilenames.length; ++i) {
			logger.fine("evaluate "+inputFilenames[i]);
			examples[i] = read(out, gold, inputFilenames[i]);
		}
		new BasicStats().evaluate(out, gold, examples);
		new MTEval().evaluate(out, gold, examples);
		out.close();
		logger.info("Evaluation ends");
	}
	
}
