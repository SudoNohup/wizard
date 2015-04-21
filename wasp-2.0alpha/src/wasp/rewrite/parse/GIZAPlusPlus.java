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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.logging.Logger;

import wasp.data.Dictionary;
import wasp.data.Example;
import wasp.data.Examples;
import wasp.data.Symbol;
import wasp.data.Terminal;
import wasp.main.Config;
import wasp.main.TranslationModel;
import wasp.mrl.MRLVocabulary;
import wasp.mrl.Production;
import wasp.scfg.Rule;
import wasp.scfg.SCFG;
import wasp.util.Arrays;
import wasp.util.InputStreamWriter;
import wasp.util.Int;
import wasp.util.Numberer;
import wasp.util.RadixMap;

/**
 * An F-to-E translation model based on IBM Model 4.  This is the only translation model that the Rewrite
 * decoder supports.  Training of this model requires the GIZA++ toolkit, which can be found in
 * <a href="http://www.fjoch.com/GIZA++.html" target="_new">http://www.fjoch.com/GIZA++.html</a>.
 * 
 * @author ywwong
 *
 */
public class GIZAPlusPlus extends TranslationModel {

	private static Logger logger = Logger.getLogger(GIZAPlusPlus.class.getName());
	
	private static final int NUM_WORD_CLASSES = 50;
	
	private static class Settings {
	    public String[][] config = {
	            {"hmmiterations", "0"},
	            {"model1iterations", "5"},
	            {"model2iterations", "5"},
	            {"model3iterations", "3"},
	            {"model4iterations", "3"},
	            {"model5iterations", "3"},
	            {"pegging", "1"},
	            {"compactadtable", "0"}
	    };
	    public File mkclsFile;
	    public File gizappFile;
	    public File zeroFertFile;
	    public String prefix;
	    public File srcSentFile;
	    public File tarSentFile;
	    public File sentFile;
	    public File configFile;
	    public File srcVocabFile;
	    public File tarVocabFile;
	    public File srcClassesFile;
	    public File tarClassesFile;
	    public File tTableFile;
	    public File tiTableFile;
	    public File nTableFile;
	    public File p0TableFile;
	    public File d3TableFile;
	    public File d4TableFile;
	    public File zeroFile;
	    public Settings() {
	    	mkclsFile = new File(Config.get(Config.MKCLS_EXEC));
	        gizappFile = new File(Config.get(Config.GIZAPP_EXEC));
	        zeroFertFile = new File(Config.get(Config.ZEROFERT_EXEC));
	        prefix = "giza++";
	        String modelDir = Config.getModelDir();
	        String suffix = ".final";
	        srcVocabFile = new File(modelDir, prefix+".src.vcb");
	        tarVocabFile = new File(modelDir, prefix+".tar.vcb");
	        srcClassesFile = new File(srcVocabFile.getPath()+".classes");
	        tarClassesFile = new File(tarVocabFile.getPath()+".classes");
	        tTableFile = new File(modelDir, prefix+".t3"+suffix);
	        tiTableFile = new File(modelDir, prefix+".ti"+suffix);
	        nTableFile = new File(modelDir, prefix+".n3"+suffix);
	        p0TableFile = new File(modelDir, prefix+".p0_3"+suffix);
	        d3TableFile = new File(modelDir, prefix+".d3"+suffix);
	        d4TableFile = new File(modelDir, prefix+".d4"+suffix);
	        zeroFile = new File(modelDir, prefix+".zero");
	    }
	    public void createTempFiles() throws IOException {
	    	srcSentFile = File.createTempFile(prefix, ".src.snt");
	    	tarSentFile = File.createTempFile(prefix, ".tar.snt");
	        sentFile = File.createTempFile(prefix, ".snt");
	        configFile = File.createTempFile(prefix, ".cfg");
	        srcSentFile.deleteOnExit();
	        tarSentFile.deleteOnExit();
	        sentFile.deleteOnExit();
	        configFile.deleteOnExit();
	    }
	}

	private static class Vocabulary extends Numberer {
		private static final int FIRST_ID = 2;
		private RadixMap counts;
		public Vocabulary() {
			super(FIRST_ID);
			counts = new RadixMap();
		}
		public int getId(Object o, boolean add) {
			int id = super.getId(o, add);
			Int count = (Int) counts.get(id);
			if (count == null)
				counts.put(id, new Int(1));
			else
				++count.val;
			return id;
		}
		public void write(PrintWriter out) {
			for (int i = FIRST_ID; i < getNextId(); ++i) {
				out.print(i);
				out.print(' ');
				out.print(getObj(i));
				out.print(' ');
				out.println(counts.get(i));
			}
		}
	}
	
	private Settings s;
	private MRLVocabulary vocab;

	/**
	 * Constructs a new f-to-e translation model based on IBM Model 4.
	 * 
	 * @param vocab a mapping between MRL productions and their string representations.
	 */
	public GIZAPlusPlus(MRLVocabulary vocab) {
		s = new Settings();
		this.vocab = vocab;
	}
	
	/**
	 * Checks if the model files are present.  No actual reading of the files is involved.
	 * The files should be in the directory specified in the configuration file (via the
	 * key <code>Config.MODEL_DIR</code>).
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public void read() throws IOException {
		new FileInputStream(s.srcVocabFile).close();
		new FileInputStream(s.tarVocabFile).close();
		new FileInputStream(s.zeroFertFile).close();
		new FileInputStream(s.srcClassesFile).close();
		new FileInputStream(s.tarClassesFile).close();
		new FileInputStream(s.tTableFile).close();
		new FileInputStream(s.tiTableFile).close();
		new FileInputStream(s.nTableFile).close();
		new FileInputStream(s.p0TableFile).close();
		new FileInputStream(s.d3TableFile).close();
		new FileInputStream(s.d4TableFile).close();
	}

	public File getSrcVocabFile() {
		return s.srcVocabFile;
	}
	
	public File getTarVocabFile() {
		return s.tarVocabFile;
	}
	
	public File getSrcClassesFile() {
		return s.srcClassesFile;
	}
	
	public File getTarClassesFile() {
		return s.tarClassesFile;
	}
	
	public File getTTableFile() {
		return s.tTableFile;
	}
	
	public File getTiTableFile() {
		return s.tiTableFile;
	}
	
	public File getNTableFile() {
		return s.nTableFile;
	}
	
	public File getP0TableFile() {
		return s.p0TableFile;
	}
	
	public File getD3TableFile() {
		return s.d3TableFile;
	}
	
	public File getD4TableFile() {
		return s.d4TableFile;
	}
	
	public File getZeroFile() {
		return s.zeroFile;
	}
	
	public void train(Examples examples, boolean full) throws IOException {
		logger.info("Training IBM Model 4 with GIZA++");
		s.createTempFiles();
		writeInput(examples);
		runMkcls();
		runGiza();
		runZeroFert();
		logger.info("IBM Model 4 has been trained");
	}

	private void writeInput(Examples examples) throws IOException {
		PrintWriter srcSentOut = new PrintWriter(new BufferedWriter(new FileWriter(s.srcSentFile)));
		PrintWriter tarSentOut = new PrintWriter(new BufferedWriter(new FileWriter(s.tarSentFile)));
        PrintWriter sentOut = new PrintWriter(new BufferedWriter(new FileWriter(s.sentFile)));
        PrintWriter srcVocabOut = new PrintWriter(new BufferedWriter(new FileWriter(s.srcVocabFile)));
        PrintWriter tarVocabOut = new PrintWriter(new BufferedWriter(new FileWriter(s.tarVocabFile)));
        Vocabulary srcVocab = new Vocabulary();
        Vocabulary tarVocab = new Vocabulary();
        // a sentence pair for each training example
        for (Iterator it = examples.iterator(); it.hasNext();) {
            Example ex = (Example) it.next();
            Terminal[] E = ex.E();
            Production[] F = ex.F.lprods;
            sentOut.println('1');
            // source is F
            for (short j = 0; j < F.length; ++j)
            	if (!F[j].isUnary()) {
	            	String token = vocab.token(F[j]);
	            	srcSentOut.print(token);
	            	srcSentOut.print(' ');
	                int sid = srcVocab.getId(token, true);
	                sentOut.print(sid);
	                sentOut.print(' ');
	            }
            srcSentOut.println();
            sentOut.println();
            // target is E
            for (short j = 0; j < E.length; ++j)
            	if (!E[j].isBoundary()) {
            		String token = Dictionary.term(E[j].getId());
            		tarSentOut.print(token);
            		tarSentOut.print(' ');
	                int tid = tarVocab.getId(token, true);
	                sentOut.print(tid);
	                sentOut.print(' ');
	            }
            tarSentOut.println();
            sentOut.println();
        }
        // a sentence pair for each initial SCFG rule 
		SCFG gram = new SCFG();
		gram.readInit();
        Rule[] rules = gram.getRules();
        for (int i = 0; i < rules.length; ++i)
        	if (rules[i].countArgs() == 0 && !rules[i].isWildcard()) {
        		Symbol[] E = rules[i].getE();
        		Production F = rules[i].getProduction();
	        	sentOut.println('1');
	        	// source is F
	        	String token = vocab.token(F);
	        	srcSentOut.println(token);
	        	int sid = srcVocab.getId(token, true);
	        	sentOut.println(sid);
	        	// target is E
	        	for (short j = 0; j < E.length; ++j)
	        		if (!((Terminal) E[j]).isBoundary()) {
	            		token = Dictionary.term(E[j].getId());
	            		tarSentOut.print(token);
	            		tarSentOut.print(' ');
		                int tid = tarVocab.getId(token, true);
		                sentOut.print(tid);
		                sentOut.print(' ');
	        		}
	        	tarSentOut.println();
	        	sentOut.println();
        	}
        // sentence pairs for numbers and CLang identifiers
        for (Iterator it = examples.iterator(); it.hasNext();) {
            Example ex = (Example) it.next();
            Terminal[] E = ex.E();
            Production[] F = ex.F.lprods;
            for (short j = 0; j < F.length; ++j)
            	if (F[j].isWildcardMatch())
            		for (short k = 0; k < E.length; ++k)
            			if (E[k].equals(F[j].getRhs((short) 0))) {
				            sentOut.println('1');
				            // source is F
				            String token = vocab.token(F[j]);
				            srcSentOut.println(token);
				            int sid = srcVocab.getId(token, true);
				            sentOut.println(sid);
				            // target is E
				            token = Dictionary.term(E[k].getId());
				            tarSentOut.println(token);
				            int tid = tarVocab.getId(token, true);
				            sentOut.println(tid);
				            break;
			            }
        }
        srcVocab.write(srcVocabOut);
        tarVocab.write(tarVocabOut);
        srcSentOut.close();
        tarSentOut.close();
        sentOut.close();
        srcVocabOut.close();
        tarVocabOut.close();
	}
	
	private void runMkcls() throws IOException {
		// mkcls -m1 -pgiza++00000.src.snt -cNUM_WORD_CLASSES -Vgiza++.src.vcb.classes opt
		String[] cmdarray = new String[6];
		cmdarray[0] = s.mkclsFile.getPath();
		cmdarray[1] = "-m1";
		cmdarray[2] = "-p"+s.srcSentFile.getPath();
		cmdarray[3] = "-c"+NUM_WORD_CLASSES;
		cmdarray[4] = "-V"+s.srcClassesFile.getPath();
		cmdarray[5] = "opt";
		exec(cmdarray);
		// mkcls -m1 -pgiza++00000.tar.snt -cNUM_WORD_CLASSES -Vgiza++.tar.vcb.classes opt
		cmdarray[2] = "-p"+s.tarSentFile.getPath();
		cmdarray[4] = "-V"+s.tarClassesFile.getPath();
		exec(cmdarray);
	}
	
	private static void exec(String[] cmdarray) throws IOException {
		try {
			Process proc = Runtime.getRuntime().exec(cmdarray);
			new InputStreamWriter(proc.getInputStream(), System.err).start();
			new InputStreamWriter(proc.getErrorStream(), System.err).start();
			int exitVal = proc.waitFor();
			if (exitVal != 0) {
				logger.severe(cmdarray[0]+" terminates abnormally");
				throw new RuntimeException();
			}
		} catch (InterruptedException e) {}
	}
	
	private void runGiza() throws IOException {
		PrintWriter configOut = new PrintWriter(new BufferedWriter(new FileWriter(s.configFile)));
		String[][] config = {
				{"s", s.srcVocabFile.getPath()},
				{"t", s.tarVocabFile.getPath()},
				{"c", s.sentFile.getPath()},
				{"o", s.prefix},
				{"outputpath", Config.getModelDir()}
		};
		config = (String[][]) Arrays.concat(s.config, config);
		for (int i = 0; i < config.length; ++i)
			configOut.println(config[i][0]+" "+config[i][1]);
		configOut.close();
		// GIZA++ giza++00000.cfg
		String[] cmdarray = new String[2];
		cmdarray[0] = s.gizappFile.getPath();
		cmdarray[1] = s.configFile.getPath();
		exec(cmdarray);
	}
	
	private void runZeroFert() throws IOException {
		// ZeroFert.perl giza++00000.src.vcb giza++.n3.final > giza++.zero
		String[] cmdarray = new String[3];
		cmdarray[0] = s.zeroFertFile.getPath();
		cmdarray[1] = s.srcVocabFile.getPath();
		cmdarray[2] = s.nTableFile.getPath();
		exec(cmdarray, s.zeroFile);
	}
	
	private static void exec(String[] cmdarray, File outFile) throws IOException {
		try {
			Process proc = Runtime.getRuntime().exec(cmdarray);
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
			Thread outThread = new InputStreamWriter(proc.getInputStream(), out, false, true);
			outThread.start();
			new InputStreamWriter(proc.getErrorStream(), System.err).start();
			int exitVal = proc.waitFor();
			if (exitVal != 0) {
				logger.severe(cmdarray[0]+" terminates abnormally");
				throw new RuntimeException();
			}
			outThread.join();
		} catch (InterruptedException e) {}
	}
	
}
