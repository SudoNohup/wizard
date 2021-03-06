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
package wasp.nl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import wasp.data.Example;
import wasp.data.Examples;
import wasp.main.Config;
import wasp.util.Arrays;
import wasp.util.InputStreamWriter;
import wasp.util.Int;
import wasp.util.Short;

/**
 * A version of the n-gram NL language model that uses the SRI Language Modeling Toolkit for training.
 * The toolkit can be found in <a href="http://www.speech.sri.com/projects/srilm/" 
 * target="_new">http://www.speech.sri.com/projects/srilm/</a>.  Learned models are written in the ARPA
 * format.
 * 
 * @see wasp.nl.NgramModel
 * @author ywwong
 *
 */
public class SRINgramModel extends NgramModel {

	private static Logger logger = Logger.getLogger(SRINgramModel.class.getName());
	
	private static final short VOCAB_GT = 1;
	private static final short GT_1_MAX = 1;
	private static final short GT_N_MAX = 4;
	
	private short N;
	
	private class Settings {
		public String[] ngramCountOptions = {
				"-debug",
				"1",
				"-order",
				Short.toString(N),
				"-unk"
		};
		public String[] ngramOptions = {
		};
	    public File binDir;
	    public File ngramCountFile;
	    public File ngramFile;
	    public File tmpDir;
	    public String prefix;
	    public File textFile;
	    public File vocabFile;
	    public Settings() {
	    	binDir = new File(Config.get(Config.SRILM_DIR));
	    	ngramCountFile = new File(binDir, "ngram-count");
	    	ngramFile = new File(binDir, "ngram");
	        tmpDir = new File(System.getProperty("java.io.tmpdir"));
	        prefix = "ngram";
	    }
	    public void createTempFiles() throws IOException {
	    	textFile = File.createTempFile(prefix, ".text.gz", tmpDir);
	    	vocabFile = File.createTempFile(prefix, ".vocab.gz", tmpDir);
	    	textFile.deleteOnExit();
	    	vocabFile.deleteOnExit();
	    }
	}

	private static final String UNKNOWN = "<unk>";
	private static final String SENT_BEGIN = "<s>";
	private static final String SENT_END = "</s>";
	
	private Settings s;

	public SRINgramModel() throws IOException {
		super(UNKNOWN, SENT_BEGIN, SENT_END);
		N = Short.parseShort(Config.get(Config.NGRAM_N));
		s = new Settings();
	}

	public void train(Examples examples) throws IOException {
		logger.info("Training the N-gram language model for NL");
		s.createTempFiles();
		writeText(examples);
		writeVocab(examples);
		String[] args = new String[14+(4*(N-2))];
		args[0] = "-text";
		args[1] = s.textFile.getPath();
		args[2] = "-vocab";
		args[3] = s.vocabFile.getPath();
		args[4] = "-lm";
		args[5] = getModelFile().getPath();
		args[6] = "-gt1min";
		args[7] = "1";
		args[8] = "-gt1max";
		args[9] = Short.toString(GT_1_MAX);
		for (short i = 2; i <= N; ++i) {
			args[10+(4*(i-2))] = "-gt"+i+"min";
			args[11+(4*(i-2))] = "1";
			args[12+(4*(i-2))] = "-gt"+i+"max";
			args[13+(4*(i-2))] = Short.toString(GT_N_MAX);
		}
		exec(cmd(s.ngramCountFile, (String[]) Arrays.concat(args, s.ngramCountOptions)));
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
	
	private void writeVocab(Examples examples) throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream
				(new FileOutputStream(s.vocabFile)))));
		HashMap counts = new HashMap();
		for (Iterator it = examples.iterator(); it.hasNext();) {
			Example ex = (Example) it.next();
			for (short i = 0; i < ex.E().length; ++i)
				if (!ex.E()[i].isBoundary()) {
					String token = token(ex.E()[i]);
					Int count = (Int) counts.get(token);
					if (count == null)
						counts.put(token, new Int(1));
					else
						++count.val;
				}
		}
		for (Iterator it = counts.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			String token = (String) entry.getKey();
			int count = ((Int) entry.getValue()).val;
			if (count > VOCAB_GT)
				out.println(token);
		}
		out.close();
	}
	
	private static String[] cmd(File execFile, String[] args) {
		return (String[]) Arrays.insert(args, 0, execFile.getPath());
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
