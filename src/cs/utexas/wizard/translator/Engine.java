package cs.utexas.wizard.translator;

import java.util.Arrays;
import java.util.List;

import edu.stanford.nlp.sempre.Builder;
import edu.stanford.nlp.sempre.Derivation;
import edu.stanford.nlp.sempre.Example;
import edu.stanford.nlp.sempre.LanguageAnalyzer;
import fig.basic.LogInfo;

public class Engine {

	public static void main(String[] args) {

		String gPath = "./grammar/wizard.grammar";

		Builder builder = new Builder();
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

		List<String> queryList = Arrays.asList(testSet);
		// query = "two plus one";
		int succ = 0;
		for (String query : queryList) {
			LogInfo.logs("Parse query----------%s", query);
			Example.Builder b = new Example.Builder();
			b.setUtterance(query);
			Example ex = b.createExample();

			ex.preprocess(LanguageAnalyzer.getSingleton());

			// Parse!
			builder.parser.parse(builder.params, ex, false);
			LogInfo.logs("Derivation: %s", ex.predDerivations);
			if (!ex.predDerivations.isEmpty()) {
				succ++;

				for (Derivation dev : ex.predDerivations) {
					LogInfo.log("Dev result-----------: " + dev.getValue());
				}
			}
		}
		
		LogInfo.log("Success: " + succ + "/" + queryList.size());
	}
	
	final static String[] testSet = { "m mod n",
			"m mod n and let r be the remainder", "r is 0", "r is not 0",
			"n is the answer", "if r is 0", "if r is not 0",
			"if r is 0, n is the answer", "continue to step 3",
			"if r is not 0, continue to step 3.", "m = n",
			"m = n and n = r.", "Go back to step 1" };
}
