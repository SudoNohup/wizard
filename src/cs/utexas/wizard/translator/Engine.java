package cs.utexas.wizard.translator;

import edu.stanford.nlp.sempre.Builder;
import edu.stanford.nlp.sempre.Dataset;
import edu.stanford.nlp.sempre.Learner;
import edu.stanford.nlp.sempre.Master;

public class Engine {

	public static void main(String[] args) {
		Builder builder = new Builder();
		builder.build();

		Dataset dataset = new Dataset();
		dataset.read();

		Learner learner = new Learner(builder.parser, builder.params, dataset);
		learner.learn();

		Master master = new Master(builder);
		master.runInteractivePrompt();
	}
}
