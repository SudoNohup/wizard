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
import wasp.mrl.Production;
import wasp.nl.NgramModel;
import wasp.scfg.Rule;
import wasp.scfg.SCFG;
import wasp.util.Arrays;
import wasp.util.Bool;
import wasp.util.FileWriter;
import wasp.util.InputStreamWriter;
import wasp.util.Int;

/**
 * An F-to-E translation model based on Pharaoh.  Training this model requires the Pharaoh training 
 * code, which can be found in <a href="http://www.statmt.org/wmt06/shared-task/baseline.html" 
 * target="_new">http://www.statmt.org/wmt06/shared-task/baseline.html</a>.
 * 
 * @author ywwong
 *
 */
public class PhraseTranslationModel extends TranslationModel {

	private static Logger logger = Logger.getLogger(PhraseTranslationModel.class.getName());
	
	/** The portion of the training set that is used for training the component models
	 * during minimum error-rate training.  The rest of the training set is set aside as the
	 * validation set. */
	private static final double MERT_TRAIN_SIZE = 0.6;
	
	/** Indicates if the generator accepts linearized MR parse-trees as input (a.k.a.&nbsp;the
	 * Pharaoh++ model). */
	private boolean USE_MRL_GRAMMAR;
	/** Indicates if minimum error-rate training is performed. */
	private boolean MIN_ERROR_RATE_TRAINING;
	
	private class Settings {
	    public String[] options = {
	    		"--alignment", "grow-diag-final",
	    		"--giza-option", "mh=0,m1=5,m2=5,m3=3,m4=3,m5=3"
	    };
	    public String[] mertParams = {
	    };
	    public int mertKBest = 100;
	    public String mertLambda = "d:1,0.5-1.5 lm:1,0.5-1.5 tm:0.3,0.25-0.75;0.2,0.25-0.75;0.2,0.25-0.75;0.3,0.25-0.75;0,-0.5-0.5 w:0,-0.5-0.5";
	    public File pharaohFile;
	    public File trainPhraseFile;
	    public File mertFile;
	    public File mertDir;
	    public File rootDir;
	    public File corpusDir;
	    public File lexicalDir;
	    public File modelDir;
	    public File extractFile;
	    public File iniFile;
	    public String src;  // f
	    public String tar;  // e
	    public File gizaf2eDir;
	    public File gizae2fDir;
	    public File corpusStem;
	    public File srcSentFile;
	    public File tarSentFile;
	    public File mertWorkDir;
	    public File mertDevSrcFile;
	    public File mertDevTarFile;
	    public File mertIniFile;
	    public Settings() {
			pharaohFile = new File(Config.get(Config.PHARAOH_EXEC));
	    	trainPhraseFile = new File(Config.get(Config.TRAIN_PHRASE_EXEC));
	    	mertFile = new File(Config.get(Config.MERT_EXEC));
	    	mertDir = mertFile.getParentFile();
	    	rootDir = new File(System.getProperty("java.io.tmpdir"));
	    	corpusDir = new File(rootDir, "corpus");
	    	modelDir = new File(Config.getModelDir());
	    	lexicalDir = modelDir;
	    	extractFile = new File(modelDir, "extract");
	    	iniFile = new File(modelDir, "pharaoh.ini");
	    	src = Config.getMRL();
	    	tar = Config.getTargetNL();
	    	gizaf2eDir = new File(rootDir, "giza."+src+"-"+tar);
	    	gizae2fDir = new File(rootDir, "giza."+tar+"-"+src);
	    	corpusStem = new File(corpusDir, "corpus");
	    	srcSentFile = new File(corpusDir, "corpus."+src);
	    	tarSentFile = new File(corpusDir, "corpus."+tar);
	    }
	    public void createTempFiles() throws IOException {
	    	if (MIN_ERROR_RATE_TRAINING) {
	    		mertWorkDir = new File(rootDir, "mert");
	    		String[] cmdarray = {"rm", "-Rf", mertWorkDir.getPath()};
	    		exec(cmdarray, null, false);
	    		mertWorkDir.mkdir();
	    		mertDevSrcFile = File.createTempFile("mert", "."+src);
	    		mertDevTarFile = File.createTempFile("mert", "."+tar);
		    	mertIniFile = new File(mertWorkDir, "pharaoh.ini");
		    	mertDevSrcFile.deleteOnExit();
		    	mertDevTarFile.deleteOnExit();
	    	}
	    }
	}

	private Settings s;
	private NgramModel lm;
	
	/**
	 * Constructs a new f-to-e phrase translation model based on Pharaoh.  It requires an n-gram
	 * language model for the target NL.
	 * 
	 * @param lm an n-gram language model for the target NL.
	 * @throws IOException if an I/O error occurs.
	 */
	public PhraseTranslationModel(NgramModel lm) throws IOException {
		USE_MRL_GRAMMAR = Bool.parseBool(Config.get(Config.PHARAOH_USE_MRL_GRAMMAR));
		MIN_ERROR_RATE_TRAINING = Bool.parseBool(Config.get(Config.PHARAOH_MIN_ERROR_RATE_TRAINING));
		s = new Settings();
		s.createTempFiles();
		this.lm = lm;
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
		boolean merTrained = false;
		if (MIN_ERROR_RATE_TRAINING) {
			Examples[] split = examples.split(MERT_TRAIN_SIZE);
			writeInput(split[0]);
			runTrainPhrase();
			writeInputMERT(split[1]);
			merTrained = runMERT();
		}
		writeInput(examples);
		runTrainPhrase();
		if (merTrained)
			copyIniFile();
		logger.info("The phrase translation model has been trained");
	}

	private void writeInput(Examples examples) throws IOException {
		PrintWriter srcSentOut = new PrintWriter(new BufferedWriter(FileWriter.createNew(s.srcSentFile)));
		PrintWriter tarSentOut = new PrintWriter(new BufferedWriter(FileWriter.createNew(s.tarSentFile)));
        // a sentence pair for each training example
        for (Iterator it = examples.iterator(); it.hasNext();) {
            Example ex = (Example) it.next();
            // source is F
            if (USE_MRL_GRAMMAR) {
	            Production[] F = ex.F.lprods;
	            for (short j = 0; j < F.length; ++j)
	            	if (!F[j].isUnary() && !Config.getMRLGrammar().isZeroFertility(F[j])) {
		            	srcSentOut.print(PharaohModel.token(F[j]));
		            	srcSentOut.print(' ');
		            }
            } else {
            	Symbol[] F = ex.F.syms;
            	for (short j = 0; j < F.length; ++j) {
            		srcSentOut.print(PharaohModel.token(F[j]));
            		srcSentOut.print(' ');
            	}
            }
            srcSentOut.println();
            // target is E
            Terminal[] E = ex.E();
            for (short j = 0; j < E.length; ++j)
            	if (!E[j].isBoundary()) {
            		tarSentOut.print(Dictionary.term(E[j].getId()));
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
	        	// source is F
        		if (USE_MRL_GRAMMAR) {
	        		Production F = rules[i].getProduction();
		        	srcSentOut.print(PharaohModel.token(F));
        		} else {
        			Symbol[] F = rules[i].getF();
        			for (short j = 0; j < F.length; ++j) {
        				srcSentOut.print(PharaohModel.token(F[j]));
        				srcSentOut.print(' ');
        			}
        		}
    			srcSentOut.println();
	        	// target is E
        		Symbol[] E = rules[i].getE();
	        	for (short j = 0; j < E.length; ++j)
	        		if (!((Terminal) E[j]).isBoundary()) {
	            		tarSentOut.print(Dictionary.term(E[j].getId()));
	            		tarSentOut.print(' ');
	        		}
	        	tarSentOut.println();
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
				            // source is F
				            srcSentOut.println(PharaohModel.token(F[j]));
				            // target is E
				            tarSentOut.println(Dictionary.term(E[k].getId()));
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
		exec(cmdarray, null, false);
	}
	
	private static boolean exec(String[] cmdarray, File dir, boolean returns) throws IOException {
		try {
			Process proc = Runtime.getRuntime().exec(cmdarray, null, dir);
			new InputStreamWriter(proc.getInputStream(), System.err).start();
			new InputStreamWriter(proc.getErrorStream(), System.err).start();
			int exitVal = proc.waitFor();
			if (exitVal == 0)
				return true;
			else {
				logger.severe(cmdarray[0]+" terminates abnormally");
				if (!returns)
					throw new RuntimeException();
			}
		} catch (InterruptedException e) {}
		return false;
	}
	
	private void writeInputMERT(Examples examples) throws IOException {
		PrintWriter srcSentOut = new PrintWriter(new BufferedWriter(FileWriter.createNew(s.mertDevSrcFile)));
		PrintWriter tarSentOut = new PrintWriter(new BufferedWriter(FileWriter.createNew(s.mertDevTarFile)));
        // a sentence pair for each training example
        for (Iterator it = examples.iterator(); it.hasNext();) {
            Example ex = (Example) it.next();
            // source is F
            if (USE_MRL_GRAMMAR) {
	            Production[] F = ex.F.lprods;
	            for (short j = 0; j < F.length; ++j)
	            	if (!F[j].isUnary() && !Config.getMRLGrammar().isZeroFertility(F[j])) {
		            	srcSentOut.print(PharaohModel.token(F[j]));
		            	srcSentOut.print(' ');
		            }
            } else {
            	Symbol[] F = ex.F.syms;
            	for (short j = 0; j < F.length; ++j) {
            		srcSentOut.print(PharaohModel.token(F[j]));
            		srcSentOut.print(' ');
            	}
            }
            srcSentOut.println();
            // target is E
            Terminal[] E = ex.E();
            for (short j = 0; j < E.length; ++j)
            	if (!E[j].isBoundary()) {
            		tarSentOut.print(Dictionary.term(E[j].getId()));
            		tarSentOut.print(' ');
	            }
            tarSentOut.println();
        }
        srcSentOut.close();
        tarSentOut.close();
	}
	
	private boolean runMERT() throws IOException {
		String[] cmdarray = new String[8];
		cmdarray[0] = s.mertFile.getPath();
		cmdarray[1] = s.mertWorkDir.getAbsolutePath();
		cmdarray[2] = s.mertDevSrcFile.getAbsolutePath();
		cmdarray[3] = s.mertDevTarFile.getAbsolutePath();
		cmdarray[4] = Int.toString(s.mertKBest);
		cmdarray[5] = s.pharaohFile.getAbsolutePath();
		String[] params = {"-f", s.iniFile.getAbsolutePath()};
		params = (String[]) Arrays.concat(s.mertParams, params);
		cmdarray[6] = Arrays.concat(params);
		cmdarray[7] = s.mertLambda;
		return exec(cmdarray, s.mertDir, true);
	}
	
	private void copyIniFile() throws IOException {
		String[] cmdarray = new String[3];
		cmdarray[0] = "cp";
		cmdarray[1] = s.mertIniFile.getAbsolutePath();
		cmdarray[2] = s.iniFile.getAbsolutePath();
		exec(cmdarray, null, false);
	}
	
}
