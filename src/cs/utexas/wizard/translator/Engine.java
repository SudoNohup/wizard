package cs.utexas.wizard.translator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import edu.stanford.nlp.sempre.Builder;
import edu.stanford.nlp.sempre.Derivation;
import edu.stanford.nlp.sempre.Example;
import edu.stanford.nlp.sempre.LanguageAnalyzer;
import edu.stanford.nlp.sempre.StringValue;
import edu.stanford.nlp.sempre.Value;
import fig.basic.LogInfo;

public class Engine {

	Builder builder = new Builder();

	public static void main(String[] args) {
		new Engine().run();
	}

	private void run() {

		String gPath = "./grammar/wizard.grammar";

		builder.build();
		builder.grammar.read(gPath);

		// This is used for learning based.
		// Dataset dataset = new Dataset();
		// dataset.read();
		//
		// Learner learner = new Learner(builder.parser, builder.params,
		// dataset);
		// learner.learn();

		// This is used for interactive mode.
		// Master master = new Master(builder);
		// master.runInteractivePrompt();

		// Need to update the parser given that the grammar has changed.
		builder.parser = null;
		builder.buildUnspecified();

		testFromBuffer();

		testFromFile();

	}

	private void testFromFile() {
		String srcPath = "./corpus/gcd.txt";
		File src = new File(srcPath);
		try (BufferedReader br = new BufferedReader(new FileReader(src))) {
			String line;
			StringBuilder sb = new StringBuilder();
			while ((line = br.readLine()) != null) {
				// process the line.
				String num = line.substring(0, 1);
				String stmt = line.substring(3, line.length());
				sb.append("line " + num + "\n");
				if (stmt.contains("; ")) {
					String[] sub = stmt.split("; ");
					for (int i = 0; i < sub.length; i++) {
						String res = parse(sub[i]);
						sb.append(res);
					}
				} else if (stmt.contains(".")) {
					String[] sub = stmt.split("\\.");
					for (int i = 0; i < sub.length; i++) {
						String res = parse(sub[i]);
						sb.append(res);
					}
				}
			}
			LogInfo.log("Program: \n" + sb.toString());

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void testFromBuffer() {
		List<String> queryList = Arrays.asList(testSet);
		// query = "two plus one";
		int succ = 0;
		for (String query : queryList) {
			LogInfo.logs("Parse query----------%s", query);
			String res = parse(query);
			if (!res.equals("")) {
				succ++;
				LogInfo.log("Dev result-----------: " + res);
			}
		}

		LogInfo.log("Success: " + succ + "/" + queryList.size());
	}

	private String parse(String query) {
		StringBuilder sb = new StringBuilder();
		Example.Builder b = new Example.Builder();
		b.setUtterance(query);
		Example ex = b.createExample();

		ex.preprocess(LanguageAnalyzer.getSingleton());

		// Parse!
		builder.parser.parse(builder.params, ex, false);
		LogInfo.logs("Derivation: %s", ex.predDerivations);
		if (!ex.predDerivations.isEmpty()) {
			StringValue sv = (StringValue) pickLongest(ex.predDerivations);
			String dump = sv.value;
			sb.append(dump);
			sb.append("\n");
		}
		return sb.toString();
	}

	private Value pickLongest(List<Derivation> list) {
		int max = 0;
		Derivation can = null;
		for (Derivation dev : list) {
			int len = dev.getValue().toString().length();
			if (len > max) {
				max = len;
				can = dev;
			}
		}
		assert can.getValue() instanceof StringValue : can.getValue();
		return can.getValue();
	}

	final static String[] testSet = { "m mod n",
			"m mod n and let r be the remainder", "r is 0", "r is not 0",
			"n is the answer", "if r is 0", "if r is not 0",
			"if r is 0, n is the answer", "continue to step 3",
			"if r is not 0, continue to step 3.", "m = n", "m = n and n = r.",
			"Go back to step 1" };
}
