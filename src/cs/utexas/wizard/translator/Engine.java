package cs.utexas.wizard.translator;

import java.util.LinkedList;
import java.util.List;

import edu.stanford.nlp.sempre.Builder;
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

		// reture stmt.
		String query1 = "n is the answer";
		// equal: n == 0
		String query2 = "n is 0";
		
		// if-stmt
		String query3 = "if n is 0, n is the answer";
		
		List<String> queryList = new LinkedList<String>();
		queryList.add(query1);
		queryList.add(query2);
		queryList.add(query3);
		// query = "two plus one";
		for (String query : queryList) {
			Example.Builder b = new Example.Builder();
			b.setUtterance(query);
			Example ex = b.createExample();

			ex.preprocess(LanguageAnalyzer.getSingleton());

			// Parse!
			builder.parser.parse(builder.params, ex, false);
			LogInfo.logs("Derivation: %s", ex.predDerivations);
		}
	}
}
