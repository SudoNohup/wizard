package cs.utexas.wizard.translator;

import java.io.*;
import org.antlr.runtime.*;
import org.antlr.stringtemplate.*;
import org.antlr.stringtemplate.language.*;

public class Main {
    public static StringTemplateGroup templates;

    public static void main(String[] args) throws Exception {
	String templateFileName;
	int a = 0;
	if ( args.length<=1 ) {
		templateFileName = "templates/Java.stg";
	}
	else {
		templateFileName = args[a];
		a++;
	}
	templates = new StringTemplateGroup(new FileReader(templateFileName),
					    AngleBracketTemplateLexer.class);

	String src = "input";
	CharStream input = new ANTLRFileStream(src);
	CMinusLexer lexer = new CMinusLexer(input);
	CommonTokenStream tokens = new CommonTokenStream(lexer);
	CMinusParser parser = new CMinusParser(tokens);
	parser.setTemplateLib(templates);
	RuleReturnScope r = parser.program();
	System.out.println(r.getTemplate().toString());
    }
}
