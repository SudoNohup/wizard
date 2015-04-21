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
package wasp.pharaoh.parse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
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
import wasp.mrl.SRINgramModel;
import wasp.scfg.Rule;
import wasp.scfg.SCFG;
import wasp.util.Arrays;
import wasp.util.FileWriter;
import wasp.util.InputStreamWriter;

/**
 * An E-to-F translation model based on Pharaoh.  Training this model requires the Pharaoh training code, 
 * which can be found in <a href="http://www.statmt.org/wmt06/shared-task/baseline.html" 
 * target="_new">http://www.statmt.org/wmt06/shared-task/baseline.html</a>.
 * 
 * @author ywwong
 *
 */
public class PhraseTranslationModel extends TranslationModel {

	private static Logger logger = Logger.getLogger(PhraseTranslationModel.class.getName());
	
	private static class Settings {
	    public String[] options = {
	    		"--alignment", "grow-diag-final",
	    		"--giza-option", "mh=0,m1=5,m2=5,m3=3,m4=3,m5=3"
	    };
	    public File trainPhraseFile;
	    public File rootDir;
	    public File corpusDir;
	    public File lexicalDir;
	    public File modelDir;
	    public File extractFile;
	    public File iniFile;
	    public String src;  // e
	    public String tar;  // f
	    public File gizaf2eDir;
	    public File gizae2fDir;
	    public File corpusStem;
	    public File srcSentFile;
	    public File tarSentFile;
	    public Settings() {
	    	trainPhraseFile = new File(Config.get(Config.TRAIN_PHRASE_EXEC));
	    	rootDir = new File(System.getProperty("java.io.tmpdir"));
	    	corpusDir = new File(rootDir, "corpus");
	    	modelDir = new File(Config.getModelDir());
	    	lexicalDir = modelDir;
	    	extractFile = new File(modelDir, "extract");
	    	iniFile = new File(modelDir, "pharaoh.ini");
	    	src = Config.getSourceNL();
	    	tar = Config.getMRL();
	    	gizaf2eDir = new File(rootDir, "giza."+src+"-"+tar);
	    	gizae2fDir = new File(rootDir, "giza."+tar+"-"+src);
	    	corpusStem = new File(corpusDir, "corpus");
	    	srcSentFile = new File(corpusDir, "corpus."+src);
	    	tarSentFile = new File(corpusDir, "corpus."+tar);
	    }
	}

	private Settings s;
	private SRINgramModel lm;
	private MRLVocabulary vocab;
	
	/**
	 * Constructs a new e-to-f phrase translation model based on Pharaoh.  It requires an n-gram
	 * language model for the target MRL.
	 * 
	 * @param lm an n-gram language model for the target MRL.
	 * @throws IOException if an I/O error occurs.
	 */
	public PhraseTranslationModel(SRINgramModel lm, MRLVocabulary vocab) {
		s = new Settings();
		this.lm = lm;
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
		new FileInputStream(s.iniFile).close();
	}

	/**
	 * Returns the pathname of the configuration file for the Pharaoh decoder.
	 * 
	 * @return the pathname of the configuration file for the Pharaoh decoder.
	 */
	public File getIniFile() {
		return s.iniFile;
	}
	
	public void train(Examples examples, boolean full) throws IOException {
		logger.info("Training the phrase translation model");
		writeInput(examples);
		runTrainPhrase();
		logger.info("The phrase translation model has been trained");
	}

	private void writeInput(Examples examples) throws IOException {
		PrintWriter srcSentOut = new PrintWriter(new BufferedWriter(FileWriter.createNew(s.srcSentFile)));
		PrintWriter tarSentOut = new PrintWriter(new BufferedWriter(FileWriter.createNew(s.tarSentFile)));
        // a sentence pair for each training example
        for (Iterator it = examples.iterator(); it.hasNext();) {
            Example ex = (Example) it.next();
            Terminal[] E = ex.E();
            Production[] F = ex.F.lprods;
            // source is E
            for (short j = 0; j < E.length; ++j)
            	if (!E[j].isBoundary()) {
            		String token = Dictionary.term(E[j].getId());
            		srcSentOut.print(token);
            		srcSentOut.print(' ');
	            }
            srcSentOut.println();
            // target is F
            for (short j = 0; j < F.length; ++j)
            	if (!F[j].isUnary()) {
	            	String token = vocab.token(F[j]);
	            	tarSentOut.print(token);
	            	tarSentOut.print(' ');
	            }
            tarSentOut.println();
        }
        // a sentence pair for each initial SCFG rule 
		SCFG gram = new SCFG();
		gram.readInit();
        Rule[] rules = gram.getRules();
        for (int i = 0; i < rules.length; ++i)
        	if (rules[i].countArgs() == 0 && !rules[i].isWildcard()) {
        		Symbol[] E = rules[i].getE();
        		Production F = rules[i].getProduction();
	        	// source is E
	        	for (short j = 0; j < E.length; ++j)
	        		if (!((Terminal) E[j]).isBoundary()) {
	            		String token = Dictionary.term(E[j].getId());
	            		srcSentOut.print(token);
	            		srcSentOut.print(' ');
	        		}
	        	srcSentOut.println();
	        	// target is F
	        	String token = vocab.token(F);
	        	tarSentOut.println(token);
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
				            // source is E
				            String token = Dictionary.term(E[k].getId());
				            srcSentOut.println(token);
				            // target is F
				            token = vocab.token(F[j]);
				            tarSentOut.println(token);
				            break;
			            }
        }
        srcSentOut.close();
        tarSentOut.close();
	}
	
	private void runTrainPhrase() throws IOException {
		// train-phrase-model.perl --root-dir ... --f ... --e ... --corpus ... --lm ...
		// --corpus-dir ... --lexical-dir ... --model-dir ... --extract-file ...
		// --giza-f2e ... --giza-e2f ...
		String[] cmdarray = new String[23];
		cmdarray[0] = s.trainPhraseFile.getPath();
		cmdarray[1] = "--root-dir";
		cmdarray[2] = s.rootDir.getPath();
		cmdarray[3] = "--f";
		cmdarray[4] = s.src;
		cmdarray[5] = "--e";
		cmdarray[6] = s.tar;
		cmdarray[7] = "--corpus";
		cmdarray[8] = s.corpusStem.getPath();
		cmdarray[9] = "--lm";
		cmdarray[10] = lm.getModelFile().getAbsolutePath();
		cmdarray[11] = "--corpus-dir";
		cmdarray[12] = s.corpusDir.getPath();
		cmdarray[13] = "--lexical-dir";
		cmdarray[14] = s.lexicalDir.getPath();
		cmdarray[15] = "--model-dir";
		cmdarray[16] = s.modelDir.getAbsolutePath();
		cmdarray[17] = "--extract-file";
		cmdarray[18] = s.extractFile.getPath();
		cmdarray[19] = "--giza-f2e";
		cmdarray[20] = s.gizaf2eDir.getPath();
		cmdarray[21] = "--giza-e2f";
		cmdarray[22] = s.gizae2fDir.getPath();
		cmdarray = (String[]) Arrays.concat(cmdarray, s.options);
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
	
}
