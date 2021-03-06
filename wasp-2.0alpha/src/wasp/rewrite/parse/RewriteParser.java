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
package wasp.rewrite.parse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import wasp.data.Dictionary;
import wasp.data.Nonterminal;
import wasp.data.Symbol;
import wasp.data.Terminal;
import wasp.main.Config;
import wasp.main.Parse;
import wasp.main.Parser;
import wasp.math.Math;
import wasp.mrl.MRLParser;
import wasp.mrl.MRLVocabulary;
import wasp.mrl.Production;
import wasp.util.Arrays;
import wasp.util.Double;
import wasp.util.InputStreamWriter;
import wasp.util.Int;

/**
 * A semantic parser based on IBM Model 4/Rewrite.  It uses the Rewrite decoder, which can be found in
 * <a href="http://www.isi.edu/publications/licensed-sw/rewrite-decoder/index.html" 
 * target="_new">http://www.isi.edu/publications/licensed-sw/rewrite-decoder/index.html</a>. 
 * 
 * @author ywwong
 *
 */
public class RewriteParser extends Parser {

	private static Logger logger = Logger.getLogger(RewriteParser.class.getName());
	
	/** The port number that the underlying Rewrite decoding server uses.  It should be between
	 * 1024 and 65535. */
	private static final int PORT_NUMBER = 59999;
	
	private static class Settings {
		public String[][] tmConfig = {
		};
		public String[][] decConfig = {
				{"Greedy2", "true"}
		};
		public File rewriteFile;
		public File tmConfigFile;
		public File decConfigFile;
		public File serverLockPrefix;
		public File serverLockFile;
		public int serverPID;
		public Settings() {
			rewriteFile = new File(Config.get(Config.REWRITE_EXEC));
			serverPID = -1;
		}
		public void createTempFiles() throws IOException {
			String prefix = "rewrite";
			tmConfigFile = File.createTempFile(prefix, ".tm.cfg");
			decConfigFile = File.createTempFile(prefix, ".dec.cfg");
			serverLockPrefix = File.createTempFile(prefix, "");
			tmConfigFile.deleteOnExit();
			decConfigFile.deleteOnExit();
			serverLockPrefix.deleteOnExit();
		}
	}
	
	private Settings s;
	private MRLVocabulary vocab;
	private Process server;
	private BufferedReader sin;
	
	/**
	 * Constructs a new semantic parser based on the given translation model (IBM Model 4) and language
	 * model (n-gram).  It starts the Rewrite decoding server, which the <code>parse</code> method
	 * uses to obtain MRL translations.
	 * 
	 * @param tm the underlying translation model based on IBM Model 4.
	 * @param lm the underlying language model for the target MRL based on the n-gram model.
	 * @throws IOException if an I/O error occurs.
	 */
	public RewriteParser(GIZAPlusPlus tm, CMUCamBinaryModel lm, MRLVocabulary vocab) throws IOException {
		s = new Settings();
		s.createTempFiles();
		this.vocab = vocab;
		logger.info("Starting the Rewrite server");
		writeTMConfig(tm);
		writeDecConfig(lm);
		runRewrite();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				// assumes UNIX operating system: Rewrite is available only on Linux or SunOS
				logger.info("Killing the Rewrite server (PID "+s.serverPID+")");
				try {
					String[] cmdarray = new String[2];
					cmdarray[0] = "kill";
					cmdarray[1] = Int.toString(s.serverPID);
					exec(cmdarray);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	private static void exec(String[] cmdarray) throws IOException {
		try {
			Process proc = Runtime.getRuntime().exec(cmdarray);
			new InputStreamWriter(proc.getInputStream(), System.err).start();
			new InputStreamWriter(proc.getErrorStream(), System.err).start();
			proc.waitFor();
		} catch (InterruptedException e) {}
	}
	
	private void writeTMConfig(GIZAPlusPlus tm) throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(s.tmConfigFile)));
		String[][] config = {
				{"TM_RawDataDir", Config.getModelDir()},
				{"TTable", tm.getTTableFile().getPath()},
				{"InverseTTable", tm.getTiTableFile().getPath()},
				{"NTable", tm.getNTableFile().getPath()},
				{"D3Table", tm.getD3TableFile().getPath()},
				{"D4Table", tm.getD4TableFile().getPath()},
				{"PZero", tm.getP0TableFile().getPath()},
				{"Source.vcb", tm.getSrcVocabFile().getPath()},
				{"Target.vcb", tm.getTarVocabFile().getPath()},
				{"Source.classes", tm.getSrcClassesFile().getPath()},
				{"Target.classes", tm.getTarClassesFile().getPath()},
				{"FZeroWords", tm.getZeroFile().getPath()}
		};
		config = (String[][]) Arrays.concat(config, s.tmConfig);
		for (int i = 0; i < config.length; ++i)
			out.println(config[i][0]+" = "+config[i][1]);
		out.close();
	}

	private void writeDecConfig(CMUCamBinaryModel lm) throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(s.decConfigFile)));
		String[][] config = {
				{"LanguageModelFile", lm.getBinaryFile().getPath()},
				{"TranslationModelConfigFile", s.tmConfigFile.getPath()},
				{"Port", Int.toString(PORT_NUMBER)},
				{"ServerLockFile", s.serverLockPrefix.getPath()}
		};
		config = (String[][]) Arrays.concat(config, s.decConfig);
		for (int i = 0; i < config.length; ++i)
			out.println(config[i][0]+" = "+config[i][1]);
		out.close();
	}
	
	private void runRewrite() throws IOException {
		String[] cmdarray = new String[4];
		cmdarray[0] = s.rewriteFile.getPath();
		cmdarray[1] = "--server";
		cmdarray[2] = "--print-all=true";
		cmdarray[3] = "--config="+s.decConfigFile.getPath();
		execServer(cmdarray);
		// find server lock file
		File[] files = s.serverLockPrefix.getParentFile().listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				String path = pathname.getPath();
				return path.startsWith(s.serverLockPrefix.getPath()) && path.endsWith(".lock");
			}
		});
		if (files.length == 0) {
			logger.severe("No server lock file is found");
			System.exit(1);
		}
		if (files.length > 1) {
			logger.severe("Multiple server lock files are found; remove them first");
			System.exit(1);
		}
		// find server PID
		String path = files[0].getPath();
		int dot1 = path.indexOf('.', s.serverLockPrefix.getPath().length());
		int dot2 = path.lastIndexOf('.');
		s.serverPID = Int.parseInt(path.substring(dot1+1, dot2));
		if (s.serverPID < 0) {
			logger.severe("Server PID less than zero: "+s.serverPID);
			System.exit(1);
		}
		files[0].deleteOnExit();
	}

	private void execServer(String[] cmdarray) throws IOException {
		try {
			server = Runtime.getRuntime().exec(cmdarray);
			sin = new BufferedReader(new InputStreamReader(server.getInputStream()));
			Thread errThread = new InputStreamWriter(server.getErrorStream(), System.err);
			errThread.setDaemon(true);
			errThread.start();
			int exitVal = server.waitFor();
			if (exitVal != 0) {
				logger.severe(cmdarray[0]+" terminates abnormally");
				System.exit(1);
			}
		} catch (InterruptedException e) {}
	}
	
	private static class ParseIterator implements Iterator {
		private Parse parse;
		public ParseIterator(Parse parse) {
			this.parse = parse;
		}
		public boolean hasNext() {
			return parse != null;
		}
		public Object next() {
			Parse p = parse;
			parse = null;
			return p;
		}
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	public boolean batch() {
		return false;
	}
	
	public Iterator parse(Terminal[] E) throws IOException {
		Socket socket = new Socket("127.0.0.1", PORT_NUMBER);
		PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out.println("<?xml version=\"1.0\" encoding=\"latin-1\"?>");
		out.println("<doc>");
		out.println("<s id=\"1\">");
		short len = 0;
		for (short i = 0; i < E.length; ++i)
			if (!E[i].isBoundary()) {
				out.print(Dictionary.term(E[i].getId()));
				out.print(' ');
				++len;
			}
		out.println();
		out.println("</s>");
		out.println("</doc>");
		out.write(4);  // part of the Rewrite protocol
		out.flush();
		String s = in.readLine();
		// greedy2 (0.000000 sec.):(A: 8.628147e-18 LM: 6.516801e-05 TM: 2.241332e-26)
		int Acolon = s.indexOf("A: ");
		int space = s.indexOf(' ', Acolon+3);
		double score = Math.log(Double.parseDouble(s.substring(Acolon+3, space)));
		// original sentence
		in.readLine();
		// MRL parse
		ArrayList prods = new ArrayList();
		String[] line = Arrays.tokenize(in.readLine());
		for (short i = 0; i < line.length; ++i)
			prods.add(vocab.prod(line[i]));
		Symbol[] syms = toSyms(prods, new Int(0));
		Parse parse = null;
		if (syms != null && new MRLParser(Config.getMRLGrammar()).parse(syms) != null)
			parse = new Parse(Config.getMRLGrammar().combine(syms), score);
		// consume all output
		while ((s = in.readLine()) != null)
			;
		// add comment - alignment information
		StringBuffer sb = new StringBuffer();
		while ((s = sin.readLine()) != null)
			if (s.startsWith("Alignment")) {
				sb.append(s);
				sb.append('\n');
				for (short i = 1; i < len; ++i) {
					sb.append(sin.readLine());
					sb.append('\n');
				}
				break;
			}
		if (parse != null)
			parse.comment = sb.toString();
		out.close();
		in.close();
		socket.close();
		return new ParseIterator(parse);
	}

	private Symbol[] toSyms(ArrayList prods, Int index) {
		if (index.val == prods.size())
			return null;
		Production prod = (Production) prods.get(index.val++);
		if (prod == null)
			return null;
		Symbol[] syms = (Symbol[]) Arrays.copy(prod.getRhs());
		for (short i = 0; i < syms.length; ++i)
			if (syms[i] instanceof Nonterminal) {
				Symbol[] arg = toSyms(prods, index);
				if (arg == null)
					return null;
				syms = (Symbol[]) Arrays.replace(syms, i, arg);
			}
		return syms;
	}
	
}
