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
package wasp.rewrite.generate;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import wasp.data.Example;
import wasp.data.Examples;
import wasp.main.Config;
import wasp.nl.NLModel;
import wasp.util.Arrays;
import wasp.util.InputStreamWriter;
import wasp.util.Short;

/**
 * The n-gram NL language model trained using the CMU-Cambridge Statistical Language Modeling Toolkit.
 * The language model is stored in the binary format, which is the only format that works with the 
 * Rewrite decoder.  The CMU-Cambridge SLM toolkit can be found in
 * <a href="http://mi.eng.cam.ac.uk/~prc14/toolkit.html" 
 * target="_new">http://mi.eng.cam.ac.uk/~prc14/toolkit.html</a>.
 * 
 * @author ywwong
 *
 */
public class CMUCamBinaryModel extends NLModel {

	private static Logger logger = Logger.getLogger(CMUCamBinaryModel.class.getName());
	
	private static final short GT_DISC_RANGE_1 = 1;
	private static final short GT_DISC_RANGE_N = 4;
	
	private short N;
	
	private class Settings {
		public String[] text2wfreqOptions = {
		};
		public String[] wfreq2vocabOptions = {
				"-gt", "1"
		};
		public String[] text2idngramOptions = {
				"-n", Short.toString(N)
		};
		public String[] idngram2lmOptions = {
				"-n", Short.toString(N),
				"-vocab_type", "1"
		};
	    public File binDir;
	    public File text2wfreqFile;
	    public File wfreq2vocabFile;
	    public File text2idngramFile;
	    public File idngram2lmFile;
	    public File tmpDir;
	    public String prefix;
	    public File textFile;
	    public File wfreqFile;
	    public File vocabFile;
	    public File idngramFile;
	    public File ccsFile;
	    public Settings() {
	    	binDir = new File(Config.get(Config.CMU_CAM_TOOLKIT_DIR));
	    	text2wfreqFile = new File(binDir, "text2wfreq");
	    	wfreq2vocabFile = new File(binDir, "wfreq2vocab");
	    	text2idngramFile = new File(binDir, "text2idngram");
	    	idngram2lmFile = new File(binDir, "idngram2lm");
	        tmpDir = new File(System.getProperty("java.io.tmpdir"));
	        prefix = "ngram";
	    }
	    public void createTempFiles() throws IOException {
	    	textFile = File.createTempFile(prefix, ".text.gz", tmpDir);
	    	wfreqFile = File.createTempFile(prefix, ".wfreq.gz", tmpDir);
	    	vocabFile = File.createTempFile(prefix, ".vocab.gz", tmpDir);
	    	idngramFile = File.createTempFile(prefix, ".idngram.gz", tmpDir);
	    	ccsFile = File.createTempFile(prefix, ".ccs", tmpDir);
	    	textFile.deleteOnExit();
	    	wfreqFile.deleteOnExit();
	    	vocabFile.deleteOnExit();
	    	idngramFile.deleteOnExit();
	    	ccsFile.deleteOnExit();
	    }
	}

	private static final String SENT_BEGIN = "<s>";
	private static final String SENT_END = "</s>";
	
	private Settings s;

	public CMUCamBinaryModel() throws IOException {
		N = Short.parseShort(Config.get(Config.NGRAM_N));
		s = new Settings();
	}

	private static final String BINARY_MODEL = "ngram-model.binlm";
	
	/**
	 * Checks if the binary model file is present.  No actual reading of the model file is involved.
	 * The model file should be in the directory specified in the configuration file (via the key
	 * <code>Config.MODEL_DIR</code>).
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public void read() throws IOException {
		new FileInputStream(getBinaryFile()).close();
	}

	/**
	 * Returns the pathname of the binary model file.
	 * 
	 * @return the pathname of the binary model file.
	 */
	public File getBinaryFile() {
		return new File(Config.getModelDir(), BINARY_MODEL);
	}
	
	public void train(Examples examples) throws IOException {
		logger.info("Training the N-gram language model for NL");
		s.createTempFiles();
		writeText(examples);
		writeCCS();
		// zcat .text.gz | text2wfreq | gzip > .wfreq.gz
		exec(cmd(s.text2wfreqFile, s.text2wfreqOptions), s.textFile, s.wfreqFile);
		// zcat .wfreq.gz | wfreq2vocab | gzip > .vocab.gz
		exec(cmd(s.wfreq2vocabFile, s.wfreq2vocabOptions), s.wfreqFile, s.vocabFile);
		// zcat .text.gz | text2idngram -vocab .vocab.gz -temp /tmp | gzip > .idngram.gz
		String[] args = new String[4];
		args[0] = "-vocab";
		args[1] = s.vocabFile.getPath();
		args[2] = "-temp";
		args[3] = s.tmpDir.getPath();
		exec(cmd(s.text2idngramFile, (String[]) Arrays.concat(args, s.text2idngramOptions)),
				s.textFile, s.idngramFile);
		// idngram2lm -idngram .idngram.gz -vocab .vocab.gz -context .ccs -arpa .arpa.gz
		args = new String[10+N];
		args[0] = "-idngram";
		args[1] = s.idngramFile.getPath();
		args[2] = "-vocab";
		args[3] = s.vocabFile.getPath();
		args[4] = "-context";
		args[5] = s.ccsFile.getPath();
		args[6] = "-binary";
		args[7] = getBinaryFile().getPath();
		args[8] = "-good_turing";
		args[9] = "-disc_ranges";
		args[10] = Short.toString(GT_DISC_RANGE_1);
		for (short i = 11; i < args.length; ++i)
			args[i] = Short.toString(GT_DISC_RANGE_N);
		exec(cmd(s.idngram2lmFile, (String[]) Arrays.concat(args, s.idngram2lmOptions)));
		logger.info("The N-gram language model for NL has been trained");
	}

	private void writeText(Examples examples) throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream
				(new FileOutputStream(s.textFile)))));
		for (Iterator it = examples.iterator(); it.hasNext();) {
			Example ex = (Example) it.next();
			out.print(SENT_BEGIN);
			for (short i = 0; i < ex.E().length; ++i)
				if (!ex.E()[i].isBoundary())
					out.print(" "+token(ex.E()[i]));
			out.println(" "+SENT_END);
		}
		out.close();
	}
	
	private void writeCCS() throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(s.ccsFile)));
		out.println(SENT_BEGIN);
		out.println(SENT_END);
		out.close();
	}
	
	private static String[] cmd(File execFile, String[] args) {
		return (String[]) Arrays.insert(args, 0, execFile.getPath());
	}
	
	private static void exec(String[] cmdarray, File inFile, File outFile) throws IOException {
		try {
			Process proc = Runtime.getRuntime().exec(cmdarray);
			BufferedInputStream in =
				new BufferedInputStream(new GZIPInputStream(new FileInputStream(inFile)));
			BufferedOutputStream out =
				new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(outFile)));
			new InputStreamWriter(in, proc.getOutputStream(), true, true).start();
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
