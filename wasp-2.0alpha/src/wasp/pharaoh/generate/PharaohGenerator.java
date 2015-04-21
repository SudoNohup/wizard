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
package wasp.pharaoh.generate;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import wasp.data.Meaning;
import wasp.main.Config;
import wasp.main.Parse;
import wasp.main.Generator;
import wasp.mrl.Production;
import wasp.util.Arrays;
import wasp.util.Bool;
import wasp.util.Double;
import wasp.util.InputStreamWriter;
import wasp.util.TokenReader;

/**
 * A tactical generator based on Pharaoh.  It uses the Pharaoh decoder, which can be found in
 * <a href="http://www.isi.edu/publications/licensed-sw/pharaoh/" 
 * target="_new">http://www.isi.edu/publications/licensed-sw/pharaoh/</a>. 
 * 
 * @author ywwong
 *
 */
public class PharaohGenerator extends Generator {

	private static Logger logger = Logger.getLogger(PharaohGenerator.class.getName());
	
	/** Indicates if the generator accepts linearized MR parse-trees as input (a.k.a.&nbsp;the
	 * Pharaoh++ model). */
	private boolean USE_MRL_GRAMMAR;
	
	private static class Settings {
		public String[] options = {
				"-trace",
				"-verbose", "2"
		};
		public File pharaohFile;
		public File srcSentFile;
		public File tarSentFile;
		public Settings() {
			pharaohFile = new File(Config.get(Config.PHARAOH_EXEC));
		}
		public void createTempFiles() throws IOException {
			String prefix = "pharaoh";
			srcSentFile = File.createTempFile(prefix, ".src");
			tarSentFile = File.createTempFile(prefix, ".tar");
			srcSentFile.deleteOnExit();
			tarSentFile.deleteOnExit();
		};
	}
	
	private Settings s;
	private PhraseTranslationModel tm;
	
	/**
	 * Constructs a new NL generator based on the given Pharaoh phrase translation model.
	 * 
	 * @param tm the underlying Pharaoh phrase translation model.
	 * @throws IOException if an I/O error occurs.
	 */
	public PharaohGenerator(PhraseTranslationModel tm) throws IOException {
		USE_MRL_GRAMMAR = Bool.parseBool(Config.get(Config.PHARAOH_USE_MRL_GRAMMAR));
		s = new Settings();
		s.createTempFiles();
		this.tm = tm;
	}
	
	private static class GenIterator implements Iterator {
		private Parse gen;
		public GenIterator(Parse gen) {
			this.gen = gen;
		}
		public boolean hasNext() {
			return gen != null;
		}
		public Object next() {
			Parse g = gen;
			gen = null;
			return g;
		}
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	public boolean batch() {
		return true;
	}
	
	public Iterator[] generate(Meaning[] F) throws IOException {
		writePharaohInput(F);
		runPharaoh();
		return readPharaohOutput();
	}

	private void writePharaohInput(Meaning[] F) throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(s.srcSentFile)));
		for (int i = 0; i < F.length; ++i) {
			if (USE_MRL_GRAMMAR)
				for (short j = 0; j < F[i].lprods.length; ++j) {
					Production prod = F[i].lprods[j];
					if (!prod.isUnary() && !Config.getMRLGrammar().isZeroFertility(prod)) {
						out.print(PharaohModel.token(F[i].lprods[j]));
						out.print(' ');
					}
				}
			else
				for (short j = 0; j < F[i].syms.length; ++j) {
					out.print(PharaohModel.token(F[i].syms[j]));
					out.print(' ');
				}
			out.println();
		}
		out.close();
	}
	
	private void runPharaoh() throws IOException {
		String[] cmdarray = new String[3];
		cmdarray[0] = s.pharaohFile.getPath();
		cmdarray[1] = "-config";
		cmdarray[2] = tm.getIniFile().getPath();
		cmdarray = (String[]) Arrays.concat(cmdarray, s.options);
		exec(cmdarray, s.srcSentFile, s.tarSentFile);
	}
	
	private static void exec(String[] cmdarray, File inFile, File outFile) throws IOException {
		try {
			Process proc = Runtime.getRuntime().exec(cmdarray);
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(inFile));
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
			new InputStreamWriter(in, proc.getOutputStream(), true, true).start();
			Thread outThread = new InputStreamWriter(proc.getInputStream(), out, false, true);
			Thread errThread = new InputStreamWriter(proc.getErrorStream(), System.err);
			outThread.start();
			errThread.start();
			int exitVal = proc.waitFor();
			if (exitVal != 0) {
				logger.severe(cmdarray[0]+" terminates abnormally");
				throw new RuntimeException();
			}
			outThread.join();
			errThread.join();
		} catch (InterruptedException e) {}
	}
	
	private Iterator[] readPharaohOutput() throws IOException {
		TokenReader in = new TokenReader(new BufferedReader(new FileReader(s.tarSentFile)));
		ArrayList list = new ArrayList();
		String[] line;
		while ((line = in.readLine()) != null) {
			if (line.length > 0 && line[0].equals("BEST:")) {
				StringBuffer nl = new StringBuffer();
				StringBuffer comment = new StringBuffer();
				// BEST: this is |0.014086|0|1| a |0.188447|2|2| house |5.85786e-08|3|3| -22.5844
				for (short i = 1; i < line.length-1; ++i) {
					if (!(line[i].startsWith("|") && line[i].endsWith("|"))) {
						nl.append(line[i]);
						nl.append(' ');
					}
					comment.append(line[i]);
					comment.append(' ');
				}
				comment.append('\n');
				double score = Double.parseDouble(line[line.length-1]);
				Parse gen = new Parse(nl.toString(), score);
				gen.comment = comment.toString();
				list.add(new GenIterator(gen));
			}
		}
		in.close();
		return (Iterator[]) list.toArray(new Iterator[0]);
	}
	
}
