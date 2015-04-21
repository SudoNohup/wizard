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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import wasp.data.CorpusReader;
import wasp.data.Examples;
import wasp.mrl.MRLGrammar;
import wasp.util.Int;

/**
 * Defines all configuration settings for controlling the behavior of WASP-based programs.
 * 
 * @author ywwong
 *
 */
public class Config {

	// set up the loggers
	static {
		Logger root = Logger.getLogger("");
		Handler handler = new ConsoleHandler();
		handler.setLevel(Level.FINEST);
		handler.setFormatter(new Formatter() {
			public String format(LogRecord record) {
				return record.getMessage()+'\n';
			}
		});
		root.addHandler(handler);
		root.setLevel(Level.FINER);
	}
	
	/** The current configuration settings. */
	private static Config config;
	/** The current configuration file. */
	private static String configFilename;
	
	/** The property list that stores all key-value pairs specified in the configuration file. */
	private Properties props;
	/** The MRL grammar. */
	private static MRLGrammar mrlGram;
	/** The corpus reader. */
	private static CorpusReader creader;
	
	private Config() {}

	private void init() throws IOException {
		if (mrlGram == null) {
			mrlGram = MRLGrammar.createNew();
			mrlGram.read();
		}
		if (creader == null)
			creader = CorpusReader.createNew();
	}
	
	/**
	 * Returns the current value for the given key.  This method returns <code>null</code> if no such
	 * value is found.
	 * 
	 * @param key the key to look for.
	 * @return the current value for the given key.
	 */
	public static String get(String key) {
		return config.props.getProperty(key);
	}
	
	/**
	 * Associate the given key to the given value.
	 *  
	 * @param key the key.
	 * @param value the value.
	 */
	public static void set(String key, String value) {
		config.props.setProperty(key, value);
	}
	
	/**
	 * Returns the identifier for the source natural language.
	 * 
	 * @return the identifier for the source natural language.
	 * @see wasp.main.Config#NL_SOURCE
	 */
	public static String getSourceNL() {
		return get(NL_SOURCE);
	}
	
	/**
	 * Returns the identifier for the target natural language.
	 * 
	 * @return the identifier for the target natural language.
	 * @see wasp.main.Config#NL_TARGET
	 */
	public static String getTargetNL() {
		return get(NL_TARGET);
	}
	
	/**
	 * Returns the identifier for the current meaning-representation language.
	 * 
	 * @return the identifier for the current meaning-representation language.
	 * @see wasp.main.Config#MRL
	 */
	public static String getMRL() {
		return get(MRL);
	}

	/**
	 * Returns the current MRL grammar.
	 * 
	 * @return the current MRL grammar.
	 * @see wasp.main.Config#MRL_GRAMMAR
	 */
	public static MRLGrammar getMRLGrammar() {
		return mrlGram;
	}

	/**
	 * Returns the value of <i>k</i> for <i>k</i>-best parsing (during testing).  The actual number of 
	 * parses returned can be more if there are ties.
	 * 
	 * @return the value of <i>k</i> for <i>k</i>-best parsing.
	 * @see wasp.main.Config#KBEST
	 */
	public static int getKBest() {
		return Int.parseInt(get(KBEST));
	}
	
	/**
	 * Returns the name of the directory for storing learned parsing or generation models.
	 * 
	 * @return the name of the directory for storing learned models.
	 * @see wasp.main.Config#MODEL_DIR
	 */
	public static String getModelDir() {
		return get(MODEL_DIR);
	}
	
	/**
	 * Assigns a new directory for storing learned parsing or generation models.
	 * 
	 * @param modelDir the name of the directory for storing learned models.
	 */
	public static void setModelDir(String modelDir) {
		set(MODEL_DIR, modelDir);
	}

	/**
	 * Reads the current corpus file and returns a set of examples.
	 * 
	 * @return a set of examples.
	 * @throws IOException if an I/O error occurs.
	 */
	public static Examples readCorpus() throws IOException, SAXException, ParserConfigurationException {
		return creader.readCorpus();
	}
	
	/**
	 * Returns the type of the current parsing model.
	 * 
	 * @return the type of the current parsing model.
	 * @see wasp.main.Config#PARSE_MODEL
	 */
	public static String getParseModel() {
		return get(PARSE_MODEL);
	}
	
	/**
	 * Returns the type of the current generation model.
	 * 
	 * @return the type of the current generation model.
	 * @see wasp.main.Config#GENERATE_MODEL
	 */
	public static String getGenerateModel() {
		return get(GENERATE_MODEL);
	}
	
	/**
	 * Returns the type of the current word alignment model.
	 * 
	 * @return the type of the current word alignment model.
	 * @see wasp.main.Config#ALIGN_MODEL
	 */
	public static String getAlignModel() {
		return get(ALIGN_MODEL);
	}
	
	/**
	 * Returns the type of the current NL language model.
	 * 
	 * @return the type of the current NL language model.
	 * @see wasp.main.Config#NL_MODEL
	 */
	public static String getNLModel() {
		return get(NL_MODEL);
	}
	
	/**
	 * Returns the type of the current MRL language model.
	 * 
	 * @return the type of the current MRL language model.
	 * @see wasp.main.Config#MRL_MODEL
	 */
	public static String getMRLModel() {
		return get(MRL_MODEL);
	}
	
	/**
	 * Returns the type of the current translation model.
	 * 
	 * @return the type of the current translation model.
	 * @see wasp.main.Config#TRANSLATION_MODEL
	 */
	public static String getTranslationModel() {
		return get(TRANSLATION_MODEL);
	}

	///
	/// Key constants
	///
	
	/** The source natural language.  Recognized identifiers are: <code>en</code> for English, 
	 * <code>es</code> for Spanish, <code>ja</code> for Japanese, and <code>tr</code> for Turkish. */
	public static final String NL_SOURCE = "wasp.nl.source";
	
	/** The target natural language.  Recognized identifiers are: <code>en</code> for English, 
	 * <code>es</code> for Spanish, <code>ja</code> for Japanese, and <code>tr</code> for Turkish. */
	public static final String NL_TARGET = "wasp.nl.target";
	
	/** The meaning-representation langauge.  Recognized identifiers are: 
	 * <code>geo-funql</code> for the functional query language (FunQL) in the Geoquery domain,
	 * <code>geo-prolog</code> for the logical query language in the Geoquery domain, and
	 * <code>robocup-clang</code> for CLang, the coach language in the RoboCup domain. */
	public static final String MRL = "wasp.mrl";
	
	/** The name of the file that stores the formal MRL grammar. */
	public static final String MRL_GRAMMAR = "wasp.mrl.grammar";

	/** The value of <i>k</i> for <i>k</i>-best parsing.  The actual number of parses returned can be
	 * more if there are ties. */
	public static final String KBEST = "wasp.kbest";
	
	/** The name of the directory for storing learned parsing and generation models. */
	public static final String MODEL_DIR = "wasp.model.dir";
	
	/** The name of the corpus file. */
	public static final String CORPUS_FILE = "wasp.corpus.file";
	
	/** The current parsing model.  Recognized identifiers are: <code>direct</code> for the direct
	 * parsing model of WASP, <code>pharaoh</code> for the Pharaoh-based parsing model, and 
	 * <code>rewrite</code> for the Rewrite-based parsing model. */
	public static final String PARSE_MODEL = "wasp.parse.model";
	
	/** The current feature set for semantic parsing.  The feature set is specified using a
	 * comma-separated list of identifiers.  Recognized identifiers include: <code>rule-weight</code> for
	 * individual rule weights, <code>gap-weight</code> for word-gap-based features, and
	 * <code>two-level-rules</code> for two-level-rule features.  */
	public static final String PARSE_FEATURES = "wasp.parse.features";
	
	/** The current generation model.  Recognized identifiers are: <code>log-linear</code> for the
	 * log-linear model with fixed parameters, <code>min-error-rate</code> for the log-linear model with
	 * minimum error-rate training, <code>pharaoh</code> for the Pharaoh-based generation model, and 
	 * <code>rewrite</code> for the Rewrite-based generation model. */
	public static final String GENERATE_MODEL = "wasp.generate.model";
	
	/** The current machine translation model.  Recognized identifiers are: <code>interlingual</code> for
	 * interlingual MT, and <code>pharaoh</code> for direct MT using Pharaoh.  */
	public static final String MT_MODEL = "wasp.mt.model";
	
	/** The parameter for the e-to-f translation model component of the log-linear generation model. */
	public static final String LOG_LINEAR_WEIGHT_TM = "wasp.log-linear.weight.translation-model";
	
	/** The parameter for the e-to-f phrase-translation probability component of the log-linear
	 * generation model. */
	public static final String LOG_LINEAR_WEIGHT_PHR_PROB_FE = "wasp.log-linear.weight.phr-prob-f|e";
	
	/** The parameter for the f-to-e phrase-translation probability component of the log-linear 
	 * generation model. */
	public static final String LOG_LINEAR_WEIGHT_PHR_PROB_EF = "wasp.log-linear.weight.phr-prob-e|f";
	
	/** The parameter for the e-to-f lexical weight component of the log-linear generation model. */
	public static final String LOG_LINEAR_WEIGHT_LEX_WEIGHT_FE = "wasp.log-linear.weight.lex-weight-f|e";
	
	/** The parameter for the f-to-e lexical weight component of the log-linear generation model. */
	public static final String LOG_LINEAR_WEIGHT_LEX_WEIGHT_EF = "wasp.log-linear.weight.lex-weight-e|f";
	
	/** The parameter for the language model component of the log-linear generation model. */
	public static final String LOG_LINEAR_WEIGHT_LM = "wasp.log-linear.weight.nl-model";
	
	/** The parameter for the word penalty component of the log-linear generation model. */
	public static final String LOG_LINEAR_WEIGHT_WORD_PENALTY = "wasp.log-linear.weight.word-penalty";
	
	/** The number of folds used for minimum error-rate training. */
	public static final String MIN_ERROR_RATE_NUM_FOLDS = "wasp.min-error-rate.num-folds";
	
	/** The number of greedy searches performed during minimum error-rate training. */
	public static final String MIN_ERROR_RATE_NUM_GREEDY = "wasp.min-error-rate.num-greedy";
	
	/** The value of <i>k</i> for <i>k</i>-best parsing during minimum error-rate training. */
	public static final String MIN_ERROR_RATE_KBEST = "wasp.min-error-rate.kbest";

	/** The objective used for minimum error-rate training.  The only recognized identifier is:
	 * <code>bleu</code> for maximum-BLEU training.  */
	public static final String MIN_ERROR_RATE_OBJECTIVE = "wasp.min-error-rate.objective";
	
	/** The current word alignment model.  Recognized identifiers are: <code>giza++</code> for
	 * the GIZA++ implementation of IBM Model 5, and <code>gold-standard</code> for gold-standard word
	 * alignments. */
	public static final String ALIGN_MODEL = "wasp.align.model";
	
	/** The number of top-scoring word alignments for each training example from which synchronous
	 * grammar rules are extracted. */
	public static final String GIZAPP_KBEST = "wasp.giza++.kbest";

	/** The absolute pathname of the GIZA++ executable file. */
	public static final String GIZAPP_EXEC = "wasp.giza++.exec";
	
	/** The absolute pathname of the <code>mkcls</code> file included in the GIZA++ package. */
	public static final String MKCLS_EXEC = "wasp.mkcls.exec";
	
	/** The absolute pathname of the <code>ZeroFert.perl</code> script included in the Rewrite decoder 
	 * package. */
	public static final String ZEROFERT_EXEC = "wasp.zerofert.exec";
	
	/** The current NL language model.  The only recognized identifier is: <code>ngram</code>
	 * for the n-gram model. */
	public static final String NL_MODEL = "wasp.nl.model";
	
	/** The current MRL language model.  The only recognized identifier is: <code>ngram</code>
	 * for the n-gram model. */
	public static final String MRL_MODEL = "wasp.mrl.model";
	
	/** The size of <i>n</i> for <i>n</i>-gram models (either NL or MRL). */
	public static final String NGRAM_N = "wasp.ngram.n";
	
	/** The absolute pathname of the directory that contains the executables for the CMU-Cambridge
	 * Statistical Language Modeling Toolkit. */
	public static final String CMU_CAM_TOOLKIT_DIR = "wasp.cmu-cam-toolkit.dir";
	
	/** The absolute pathname of the directory that contains the executables for the SRILM Toolkit. */
	public static final String SRILM_DIR = "wasp.srilm.dir";
	
	/** The current translation model.  The only recognized identifier is: <code>scfg</code> for 
	 * synchronous context-free grammars. */
	public static final String TRANSLATION_MODEL = "wasp.translation.model";
	
	/** The current probabilistic model for the translation model.  Recognized identifiers are:
	 * <code>maxent</code> for maximum entropy, <code>pscfg</code> for probabilistic SCFG with
	 * inside-outside estimation, <code>pscfg-relative-freq</code> for probabilistic SCFG with 
	 * relative-frequency estimation, and <code>relative-freq</code> for the Pharaoh-style 
	 * relative-frequency model (only for tactical generation). */
	public static final String TRANSLATION_PROB_MODEL = "wasp.translation.prob.model";
	
	/** The number of top-scoring parses to consider during tactical generation, if the output
	 * scores need to be normalized as an additional reranking step (i.e.&nbsp;when
	 * <code>wasp.translation.prob.model</code> is either <code>maxent</code> or <code>pscfg</code>). */
	public static final String TRANSLATION_KBEST = "wasp.translation.kbest";
	
	/** The frequency in which Viterbi approximation occurs when training a maximum-entropy model. */ 
	public static final String MAXENT_VITERBI_APPROX_ITERATIONS = "wasp.maxent.viterbi-approx-iterations";

	/** The filename prefix for storing initial SCFG rules.
	 * @see wasp.scfg.SCFG#readInit() */
	public static final String SCFG_INIT = "wasp.scfg.init";

	/** A boolean parameter that indicates if word gaps are allowed in extracted SCFG rules. */
	public static final String SCFG_ALLOW_GAPS = "wasp.scfg.allow-gaps";
	
	/** The maximum length of an extracted MR string. */
	public static final String SCFG_MAX_MR_LENGTH = "wasp.scfg.max-mr-length";
	
	/** The maximum arity of an extracted SCFG rule. */
	public static final String SCFG_MAX_ARITY = "wasp.scfg.max-arity";
	
	/** The maximum number of items for each cell in a tactical generator based on lambda-SCFG. */
	public static final String SCFG_LAMBDA_PRUNE_K = "wasp.scfg.lambda.prune-k";
	
	/** A boolean parameter that indicates if the Rewrite-based generator uses the MRL grammar. */
	public static final String REWRITE_USE_MRL_GRAMMAR = "wasp.rewrite.use-mrl-grammar";
	
	/** The absolute pathname of the Rewrite executable file. */
	public static final String REWRITE_EXEC = "wasp.rewrite.exec";
	
	/** A boolean parameter that indicates if the Pharaoh-based generator uses the MRL grammar. */
	public static final String PHARAOH_USE_MRL_GRAMMAR = "wasp.pharaoh.use-mrl-grammar";
	
	/** A boolean parameter that indicates if the Pharaoh-based generator is
	 * minimum-error-rate-trained. */
	public static final String PHARAOH_MIN_ERROR_RATE_TRAINING = "wasp.pharaoh.min-error-rate-training";
	
	/** The absolute pathname of the Pharaoh decoder. */
	public static final String PHARAOH_EXEC = "wasp.pharaoh.exec";
	
	/** The absolute pathname of the Pharaoh training script, <code>train-phrase-model.perl</code>. */
	public static final String TRAIN_PHRASE_EXEC = "wasp.train-phrase.exec";

	/** The absolute pathname of the Pharaoh minimum error-rate training script,
	 * <code>minimum-error-rate-training.perl</code>. */
	public static final String MERT_EXEC = "wasp.mert.exec";
	
	/** The absolute pathname of the configuration file for the parser component of an interlingual
	 * translator. */
	public static final String INTERLINGUAL_PARSE_CONFIG = "wasp.interlingual.parse-config";
	
	/** The absolute pathname of the configuration file for the generator component of an interlingual
	 * translator. */
	public static final String INTERLINGUAL_GEN_CONFIG = "wasp.interlingual.generate-config";
	
	/** A boolean parameter that indicates if cross validation is used for training an interlingual
	 * translator. */
	public static final String INTERLINGUAL_CROSS_VALIDATE = "wasp.interlingual.cross-validate";
	
	/** A boolean parameter that indicates if the parser component of an interlingual translator should
	 * be trained. */
	public static final String INTERLINGUAL_TRAIN_PARSE_MODEL = "wasp.interlingual.train-parse";
	
	/** A boolean parameter that indicates if the generator component of an interlingual translator
	 * should be trained. */
	public static final String INTERLINGUAL_TRAIN_GENERATE_MODEL = "wasp.interlingual.train-generate";
	
	/** The number of folds used for training interlingual MT models when
	 * <code>wasp.interlingual.cross-validate</code> is <code>true</code>. */
	public static final String INTERLINGUAL_NUM_FOLDS = "wasp.interlingual.num-folds";
	
	/** The filename prefix for storing entity names in the Geoquery domain. */
	public static final String GEO_NAMES = "wasp.domain.geo.names";
	
	/** The name of the directory that contains the evaluation scripts for the Geoquery domain. */
	public static final String GEO_EVAL_DIR = "wasp.domain.geo.eval.dir";
	
	/** The absolute pathname of the SICSTUS executable file for running the Geoquery evaluation
	 * scripts. */
	public static final String SICSTUS_EXEC = "wasp.sicstus.exec";
	
	/** The absolute pathname of the MTEval evaluation script. */
	public static final String MTEVAL_EXEC = "wasp.mteval.exec";
	
	/** A boolean parameter that indicates if examples with no translations should be ignored when
	 * evaluating generators. */
	public static final String MTEVAL_IGNORE_EMPTY = "wasp.mteval.ignore-empty";
	
	///
	/// Configuration files
	///
	
	/**
	 * Reads the specified configuration file and updates the current configuration settings of WASP.
	 * 
	 * @param filename the name of the configuration file.
	 * @throws IOException if an I/O error occurs.
	 */
	public static void read(String filename) throws IOException {
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(filename));
		config = new Config();
		config.props = new Properties();
		config.props.load(in);
		config.print(System.err);
		config.init();
		configFilename = filename;
	}
	
	/**
	 * Returns the name of the current configuration file.
	 * 
	 * @return the name of the current configuration file.
	 */
	public static String getFilename() {
		return configFilename;
	}
	
	/**
	 * Prints the current configuration settings to the specified output stream.
	 * 
	 * @param out the output stream to write to.
	 * @throws IOException if an I/O error occurs.
	 */
	public void print(PrintStream out) throws IOException {
		for (Iterator it = props.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			out.println(entry.getKey()+"="+entry.getValue());
		}
	}
	
	/**
	 * Prints the current configuration settings to the specified character stream.
	 * 
	 * @param out the character stream to write to.
	 * @throws IOException if an I/O error occurs.
	 */
	public void print(PrintWriter out) throws IOException {
		for (Iterator it = props.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			out.println(entry.getKey()+"="+entry.getValue());
		}
	}
	
}
