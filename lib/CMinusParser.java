package cs.utexas.wizard.translator;
// $ANTLR 3.5.2 CMinus.g 2015-04-06 11:14:16

import org.antlr.stringtemplate.*;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

import org.antlr.stringtemplate.*;
import org.antlr.stringtemplate.language.*;
import java.util.HashMap;
@SuppressWarnings("all")
public class CMinusParser extends Parser {
	public static final String[] tokenNames = new String[] {
		"<invalid>", "<EOR>", "<DOWN>", "<UP>", "ID", "INT", "WS", "'%'", "'('", 
		"')'", "'*'", "'+'", "','", "'-'", "'/'", "';'", "'<'", "'='", "'=='", 
		"'char'", "'else'", "'for'", "'if'", "'int'", "'while'", "'{'", "'}'"
	};
	public static final int EOF=-1;
	public static final int T__7=7;
	public static final int T__8=8;
	public static final int T__9=9;
	public static final int T__10=10;
	public static final int T__11=11;
	public static final int T__12=12;
	public static final int T__13=13;
	public static final int T__14=14;
	public static final int T__15=15;
	public static final int T__16=16;
	public static final int T__17=17;
	public static final int T__18=18;
	public static final int T__19=19;
	public static final int T__20=20;
	public static final int T__21=21;
	public static final int T__22=22;
	public static final int T__23=23;
	public static final int T__24=24;
	public static final int T__25=25;
	public static final int T__26=26;
	public static final int ID=4;
	public static final int INT=5;
	public static final int WS=6;

	// delegates
	public Parser[] getDelegates() {
		return new Parser[] {};
	}

	// delegators

	protected static class slist_scope {
		List locals;
		List stats;
	}
	protected Stack<slist_scope> slist_stack = new Stack<slist_scope>();


	public CMinusParser(TokenStream input) {
		this(input, new RecognizerSharedState());
	}
	public CMinusParser(TokenStream input, RecognizerSharedState state) {
		super(input, state);
	}

	protected StringTemplateGroup templateLib =
	  new StringTemplateGroup("CMinusParserTemplates", AngleBracketTemplateLexer.class);

	public void setTemplateLib(StringTemplateGroup templateLib) {
	  this.templateLib = templateLib;
	}
	public StringTemplateGroup getTemplateLib() {
	  return templateLib;
	}
	/** allows convenient multi-value initialization:
	 *  "new STAttrMap().put(...).put(...)"
	 */
	@SuppressWarnings("serial")
	public static class STAttrMap extends HashMap<String, Object> {
		public STAttrMap put(String attrName, Object value) {
			super.put(attrName, value);
			return this;
		}
	}
	@Override public String[] getTokenNames() { return CMinusParser.tokenNames; }
	@Override public String getGrammarFileName() { return "CMinus.g"; }


	protected static class program_scope {
		List globals;
		List functions;
	}
	protected Stack<program_scope> program_stack = new Stack<program_scope>();

	public static class program_return extends ParserRuleReturnScope {
		public StringTemplate st;
		public Object getTemplate() { return st; }
		public String toString() { return st==null?null:st.toString(); }
	};


	// $ANTLR start "program"
	// CMinus.g:20:1: program : ( declaration )+ -> program(globals=$program::globalsfunctions=$program::functions);
	public final CMinusParser.program_return program() throws RecognitionException {
		program_stack.push(new program_scope());
		CMinusParser.program_return retval = new CMinusParser.program_return();
		retval.start = input.LT(1);


		  program_stack.peek().globals = new ArrayList();
		  program_stack.peek().functions = new ArrayList();

		try {
			// CMinus.g:29:5: ( ( declaration )+ -> program(globals=$program::globalsfunctions=$program::functions))
			// CMinus.g:29:9: ( declaration )+
			{
			// CMinus.g:29:9: ( declaration )+
			int cnt1=0;
			loop1:
			while (true) {
				int alt1=2;
				int LA1_0 = input.LA(1);
				if ( (LA1_0==ID||LA1_0==19||LA1_0==23) ) {
					alt1=1;
				}

				switch (alt1) {
				case 1 :
					// CMinus.g:29:9: declaration
					{
					pushFollow(FOLLOW_declaration_in_program48);
					declaration();
					state._fsp--;

					}
					break;

				default :
					if ( cnt1 >= 1 ) break loop1;
					EarlyExitException eee = new EarlyExitException(1, input);
					throw eee;
				}
				cnt1++;
			}

			// TEMPLATE REWRITE
			// 30:9: -> program(globals=$program::globalsfunctions=$program::functions)
			{
				retval.st = templateLib.getInstanceOf("program",new STAttrMap().put("globals", program_stack.peek().globals).put("functions", program_stack.peek().functions));
			}



			}

			retval.stop = input.LT(-1);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
			program_stack.pop();
		}
		return retval;
	}
	// $ANTLR end "program"


	public static class declaration_return extends ParserRuleReturnScope {
		public StringTemplate st;
		public Object getTemplate() { return st; }
		public String toString() { return st==null?null:st.toString(); }
	};


	// $ANTLR start "declaration"
	// CMinus.g:33:1: declaration : ( variable |f= function );
	public final CMinusParser.declaration_return declaration() throws RecognitionException {
		CMinusParser.declaration_return retval = new CMinusParser.declaration_return();
		retval.start = input.LT(1);

		ParserRuleReturnScope f =null;
		ParserRuleReturnScope variable1 =null;

		try {
			// CMinus.g:34:5: ( variable |f= function )
			int alt2=2;
			switch ( input.LA(1) ) {
			case 23:
				{
				int LA2_1 = input.LA(2);
				if ( (LA2_1==ID) ) {
					int LA2_4 = input.LA(3);
					if ( (LA2_4==15) ) {
						alt2=1;
					}
					else if ( (LA2_4==8) ) {
						alt2=2;
					}

					else {
						int nvaeMark = input.mark();
						try {
							for (int nvaeConsume = 0; nvaeConsume < 3 - 1; nvaeConsume++) {
								input.consume();
							}
							NoViableAltException nvae =
								new NoViableAltException("", 2, 4, input);
							throw nvae;
						} finally {
							input.rewind(nvaeMark);
						}
					}

				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 2, 1, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case 19:
				{
				int LA2_2 = input.LA(2);
				if ( (LA2_2==ID) ) {
					int LA2_4 = input.LA(3);
					if ( (LA2_4==15) ) {
						alt2=1;
					}
					else if ( (LA2_4==8) ) {
						alt2=2;
					}

					else {
						int nvaeMark = input.mark();
						try {
							for (int nvaeConsume = 0; nvaeConsume < 3 - 1; nvaeConsume++) {
								input.consume();
							}
							NoViableAltException nvae =
								new NoViableAltException("", 2, 4, input);
							throw nvae;
						} finally {
							input.rewind(nvaeMark);
						}
					}

				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 2, 2, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case ID:
				{
				int LA2_3 = input.LA(2);
				if ( (LA2_3==ID) ) {
					int LA2_4 = input.LA(3);
					if ( (LA2_4==15) ) {
						alt2=1;
					}
					else if ( (LA2_4==8) ) {
						alt2=2;
					}

					else {
						int nvaeMark = input.mark();
						try {
							for (int nvaeConsume = 0; nvaeConsume < 3 - 1; nvaeConsume++) {
								input.consume();
							}
							NoViableAltException nvae =
								new NoViableAltException("", 2, 4, input);
							throw nvae;
						} finally {
							input.rewind(nvaeMark);
						}
					}

				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 2, 3, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 2, 0, input);
				throw nvae;
			}
			switch (alt2) {
				case 1 :
					// CMinus.g:34:9: variable
					{
					pushFollow(FOLLOW_variable_in_declaration89);
					variable1=variable();
					state._fsp--;

					program_stack.peek().globals.add((variable1!=null?((StringTemplate)variable1.getTemplate()):null));
					}
					break;
				case 2 :
					// CMinus.g:35:9: f= function
					{
					pushFollow(FOLLOW_function_in_declaration105);
					f=function();
					state._fsp--;

					program_stack.peek().functions.add((f!=null?((StringTemplate)f.getTemplate()):null));
					}
					break;

			}
			retval.stop = input.LT(-1);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "declaration"


	public static class variable_return extends ParserRuleReturnScope {
		public StringTemplate st;
		public Object getTemplate() { return st; }
		public String toString() { return st==null?null:st.toString(); }
	};


	// $ANTLR start "variable"
	// CMinus.g:41:1: variable : type declarator ';' -> {$function.size()>0 && $function::name==null}? globalVariable(type=$type.stname=$declarator.st) -> variable(type=$type.stname=$declarator.st);
	public final CMinusParser.variable_return variable() throws RecognitionException {
		CMinusParser.variable_return retval = new CMinusParser.variable_return();
		retval.start = input.LT(1);

		ParserRuleReturnScope type2 =null;
		ParserRuleReturnScope declarator3 =null;

		try {
			// CMinus.g:42:5: ( type declarator ';' -> {$function.size()>0 && $function::name==null}? globalVariable(type=$type.stname=$declarator.st) -> variable(type=$type.stname=$declarator.st))
			// CMinus.g:42:9: type declarator ';'
			{
			pushFollow(FOLLOW_type_in_variable129);
			type2=type();
			state._fsp--;

			pushFollow(FOLLOW_declarator_in_variable131);
			declarator3=declarator();
			state._fsp--;

			match(input,15,FOLLOW_15_in_variable133); 
			// TEMPLATE REWRITE
			// 43:9: -> {$function.size()>0 && $function::name==null}? globalVariable(type=$type.stname=$declarator.st)
			if (function_stack.size()>0 && function_stack.peek().name==null) {
				retval.st = templateLib.getInstanceOf("globalVariable",new STAttrMap().put("type", (type2!=null?((StringTemplate)type2.getTemplate()):null)).put("name", (declarator3!=null?((StringTemplate)declarator3.getTemplate()):null)));
			}

			else // 45:9: -> variable(type=$type.stname=$declarator.st)
			{
				retval.st = templateLib.getInstanceOf("variable",new STAttrMap().put("type", (type2!=null?((StringTemplate)type2.getTemplate()):null)).put("name", (declarator3!=null?((StringTemplate)declarator3.getTemplate()):null)));
			}



			}

			retval.stop = input.LT(-1);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "variable"


	public static class declarator_return extends ParserRuleReturnScope {
		public StringTemplate st;
		public Object getTemplate() { return st; }
		public String toString() { return st==null?null:st.toString(); }
	};


	// $ANTLR start "declarator"
	// CMinus.g:48:1: declarator : ID -> {new StringTemplate($ID.text)};
	public final CMinusParser.declarator_return declarator() throws RecognitionException {
		CMinusParser.declarator_return retval = new CMinusParser.declarator_return();
		retval.start = input.LT(1);

		Token ID4=null;

		try {
			// CMinus.g:49:5: ( ID -> {new StringTemplate($ID.text)})
			// CMinus.g:49:9: ID
			{
			ID4=(Token)match(input,ID,FOLLOW_ID_in_declarator207); 
			// TEMPLATE REWRITE
			// 49:12: -> {new StringTemplate($ID.text)}
			{
				retval.st = new StringTemplate((ID4!=null?ID4.getText():null));
			}



			}

			retval.stop = input.LT(-1);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "declarator"


	protected static class function_scope {
		String name;
	}
	protected Stack<function_scope> function_stack = new Stack<function_scope>();

	public static class function_return extends ParserRuleReturnScope {
		public StringTemplate st;
		public Object getTemplate() { return st; }
		public String toString() { return st==null?null:st.toString(); }
	};


	// $ANTLR start "function"
	// CMinus.g:52:1: function : type ID '(' (p+= formalParameter ( ',' p+= formalParameter )* )? ')' block -> function(type=$type.stname=$function::namelocals=$slist::localsstats=$slist::statsargs=$p);
	public final CMinusParser.function_return function() throws RecognitionException {
		slist_stack.push(new slist_scope());
		function_stack.push(new function_scope());
		CMinusParser.function_return retval = new CMinusParser.function_return();
		retval.start = input.LT(1);

		Token ID5=null;
		List<Object> list_p=null;
		ParserRuleReturnScope type6 =null;
		RuleReturnScope p = null;

		  slist_stack.peek().locals = new ArrayList();
		  slist_stack.peek().stats = new ArrayList();

		try {
			// CMinus.g:61:5: ( type ID '(' (p+= formalParameter ( ',' p+= formalParameter )* )? ')' block -> function(type=$type.stname=$function::namelocals=$slist::localsstats=$slist::statsargs=$p))
			// CMinus.g:61:9: type ID '(' (p+= formalParameter ( ',' p+= formalParameter )* )? ')' block
			{
			pushFollow(FOLLOW_type_in_function244);
			type6=type();
			state._fsp--;

			ID5=(Token)match(input,ID,FOLLOW_ID_in_function246); 
			function_stack.peek().name =(ID5!=null?ID5.getText():null);
			match(input,8,FOLLOW_8_in_function258); 
			// CMinus.g:62:13: (p+= formalParameter ( ',' p+= formalParameter )* )?
			int alt4=2;
			int LA4_0 = input.LA(1);
			if ( (LA4_0==ID||LA4_0==19||LA4_0==23) ) {
				alt4=1;
			}
			switch (alt4) {
				case 1 :
					// CMinus.g:62:15: p+= formalParameter ( ',' p+= formalParameter )*
					{
					pushFollow(FOLLOW_formalParameter_in_function264);
					p=formalParameter();
					state._fsp--;

					if (list_p==null) list_p=new ArrayList<Object>();
					list_p.add(p.getTemplate());
					// CMinus.g:62:34: ( ',' p+= formalParameter )*
					loop3:
					while (true) {
						int alt3=2;
						int LA3_0 = input.LA(1);
						if ( (LA3_0==12) ) {
							alt3=1;
						}

						switch (alt3) {
						case 1 :
							// CMinus.g:62:36: ',' p+= formalParameter
							{
							match(input,12,FOLLOW_12_in_function268); 
							pushFollow(FOLLOW_formalParameter_in_function272);
							p=formalParameter();
							state._fsp--;

							if (list_p==null) list_p=new ArrayList<Object>();
							list_p.add(p.getTemplate());
							}
							break;

						default :
							break loop3;
						}
					}

					}
					break;

			}

			match(input,9,FOLLOW_9_in_function280); 
			pushFollow(FOLLOW_block_in_function290);
			block();
			state._fsp--;

			// TEMPLATE REWRITE
			// 64:9: -> function(type=$type.stname=$function::namelocals=$slist::localsstats=$slist::statsargs=$p)
			{
				retval.st = templateLib.getInstanceOf("function",new STAttrMap().put("type", (type6!=null?((StringTemplate)type6.getTemplate()):null)).put("name", function_stack.peek().name).put("locals", slist_stack.peek().locals).put("stats", slist_stack.peek().stats).put("args", list_p));
			}



			}

			retval.stop = input.LT(-1);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
			slist_stack.pop();
			function_stack.pop();
		}
		return retval;
	}
	// $ANTLR end "function"


	public static class formalParameter_return extends ParserRuleReturnScope {
		public StringTemplate st;
		public Object getTemplate() { return st; }
		public String toString() { return st==null?null:st.toString(); }
	};


	// $ANTLR start "formalParameter"
	// CMinus.g:70:1: formalParameter : type declarator -> parameter(type=$type.stname=$declarator.st);
	public final CMinusParser.formalParameter_return formalParameter() throws RecognitionException {
		CMinusParser.formalParameter_return retval = new CMinusParser.formalParameter_return();
		retval.start = input.LT(1);

		ParserRuleReturnScope type7 =null;
		ParserRuleReturnScope declarator8 =null;

		try {
			// CMinus.g:71:5: ( type declarator -> parameter(type=$type.stname=$declarator.st))
			// CMinus.g:71:9: type declarator
			{
			pushFollow(FOLLOW_type_in_formalParameter406);
			type7=type();
			state._fsp--;

			pushFollow(FOLLOW_declarator_in_formalParameter408);
			declarator8=declarator();
			state._fsp--;

			// TEMPLATE REWRITE
			// 72:9: -> parameter(type=$type.stname=$declarator.st)
			{
				retval.st = templateLib.getInstanceOf("parameter",new STAttrMap().put("type", (type7!=null?((StringTemplate)type7.getTemplate()):null)).put("name", (declarator8!=null?((StringTemplate)declarator8.getTemplate()):null)));
			}



			}

			retval.stop = input.LT(-1);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "formalParameter"


	public static class type_return extends ParserRuleReturnScope {
		public StringTemplate st;
		public Object getTemplate() { return st; }
		public String toString() { return st==null?null:st.toString(); }
	};


	// $ANTLR start "type"
	// CMinus.g:75:1: type : ( 'int' -> type_int(| 'char' -> type_char(| ID -> type_user_object(name=$ID.text));
	public final CMinusParser.type_return type() throws RecognitionException {
		CMinusParser.type_return retval = new CMinusParser.type_return();
		retval.start = input.LT(1);

		Token ID9=null;

		try {
			// CMinus.g:76:5: ( 'int' -> type_int(| 'char' -> type_char(| ID -> type_user_object(name=$ID.text))
			int alt5=3;
			switch ( input.LA(1) ) {
			case 23:
				{
				alt5=1;
				}
				break;
			case 19:
				{
				alt5=2;
				}
				break;
			case ID:
				{
				alt5=3;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 5, 0, input);
				throw nvae;
			}
			switch (alt5) {
				case 1 :
					// CMinus.g:76:9: 'int'
					{
					match(input,23,FOLLOW_23_in_type449); 
					// TEMPLATE REWRITE
					// 76:16: -> type_int(
					{
						retval.st = templateLib.getInstanceOf("type_int");
					}



					}
					break;
				case 2 :
					// CMinus.g:77:9: 'char'
					{
					match(input,19,FOLLOW_19_in_type466); 
					// TEMPLATE REWRITE
					// 77:16: -> type_char(
					{
						retval.st = templateLib.getInstanceOf("type_char");
					}



					}
					break;
				case 3 :
					// CMinus.g:78:9: ID
					{
					ID9=(Token)match(input,ID,FOLLOW_ID_in_type482); 
					// TEMPLATE REWRITE
					// 78:16: -> type_user_object(name=$ID.text)
					{
						retval.st = templateLib.getInstanceOf("type_user_object",new STAttrMap().put("name", (ID9!=null?ID9.getText():null)));
					}



					}
					break;

			}
			retval.stop = input.LT(-1);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "type"


	public static class block_return extends ParserRuleReturnScope {
		public StringTemplate st;
		public Object getTemplate() { return st; }
		public String toString() { return st==null?null:st.toString(); }
	};


	// $ANTLR start "block"
	// CMinus.g:81:1: block : '{' ( variable )* ( stat )* '}' ;
	public final CMinusParser.block_return block() throws RecognitionException {
		CMinusParser.block_return retval = new CMinusParser.block_return();
		retval.start = input.LT(1);

		ParserRuleReturnScope variable10 =null;
		ParserRuleReturnScope stat11 =null;

		try {
			// CMinus.g:82:5: ( '{' ( variable )* ( stat )* '}' )
			// CMinus.g:82:8: '{' ( variable )* ( stat )* '}'
			{
			match(input,25,FOLLOW_25_in_block513); 
			// CMinus.g:83:8: ( variable )*
			loop6:
			while (true) {
				int alt6=2;
				int LA6_0 = input.LA(1);
				if ( (LA6_0==ID) ) {
					int LA6_2 = input.LA(2);
					if ( (LA6_2==ID) ) {
						alt6=1;
					}

				}
				else if ( (LA6_0==19||LA6_0==23) ) {
					alt6=1;
				}

				switch (alt6) {
				case 1 :
					// CMinus.g:83:10: variable
					{
					pushFollow(FOLLOW_variable_in_block524);
					variable10=variable();
					state._fsp--;

					slist_stack.peek().locals.add((variable10!=null?((StringTemplate)variable10.getTemplate()):null));
					}
					break;

				default :
					break loop6;
				}
			}

			// CMinus.g:84:8: ( stat )*
			loop7:
			while (true) {
				int alt7=2;
				int LA7_0 = input.LA(1);
				if ( ((LA7_0 >= ID && LA7_0 <= INT)||LA7_0==8||LA7_0==15||(LA7_0 >= 20 && LA7_0 <= 22)||(LA7_0 >= 24 && LA7_0 <= 25)) ) {
					alt7=1;
				}

				switch (alt7) {
				case 1 :
					// CMinus.g:84:10: stat
					{
					pushFollow(FOLLOW_stat_in_block540);
					stat11=stat();
					state._fsp--;

					slist_stack.peek().stats.add((stat11!=null?((StringTemplate)stat11.getTemplate()):null));
					}
					break;

				default :
					break loop7;
				}
			}

			match(input,26,FOLLOW_26_in_block553); 
			}

			retval.stop = input.LT(-1);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "block"


	public static class stat_return extends ParserRuleReturnScope {
		public StringTemplate st;
		public Object getTemplate() { return st; }
		public String toString() { return st==null?null:st.toString(); }
	};


	// $ANTLR start "stat"
	// CMinus.g:88:1: stat : ( forStat -> {$forStat.st}| whileStat -> {$whileStat.st}| expr ';' -> statement(expr=$expr.st)| ifStat -> {$ifStat.st}| elseIfStat -> {$elseIfStat.st}| elseStat -> {$elseStat.st}| block -> statementList(locals=$slist::localsstats=$slist::stats)| assignStat ';' -> {$assignStat.st}| ';' -> {new StringTemplate(\";\")});
	public final CMinusParser.stat_return stat() throws RecognitionException {
		slist_stack.push(new slist_scope());

		CMinusParser.stat_return retval = new CMinusParser.stat_return();
		retval.start = input.LT(1);

		ParserRuleReturnScope forStat12 =null;
		ParserRuleReturnScope whileStat13 =null;
		ParserRuleReturnScope expr14 =null;
		ParserRuleReturnScope ifStat15 =null;
		ParserRuleReturnScope elseIfStat16 =null;
		ParserRuleReturnScope elseStat17 =null;
		ParserRuleReturnScope assignStat18 =null;


		  slist_stack.peek().locals = new ArrayList();
		  slist_stack.peek().stats = new ArrayList();

		try {
			// CMinus.g:94:5: ( forStat -> {$forStat.st}| whileStat -> {$whileStat.st}| expr ';' -> statement(expr=$expr.st)| ifStat -> {$ifStat.st}| elseIfStat -> {$elseIfStat.st}| elseStat -> {$elseStat.st}| block -> statementList(locals=$slist::localsstats=$slist::stats)| assignStat ';' -> {$assignStat.st}| ';' -> {new StringTemplate(\";\")})
			int alt8=9;
			switch ( input.LA(1) ) {
			case 21:
				{
				alt8=1;
				}
				break;
			case 24:
				{
				alt8=2;
				}
				break;
			case ID:
				{
				int LA8_3 = input.LA(2);
				if ( (LA8_3==17) ) {
					alt8=8;
				}
				else if ( (LA8_3==7||(LA8_3 >= 10 && LA8_3 <= 11)||(LA8_3 >= 13 && LA8_3 <= 16)||LA8_3==18) ) {
					alt8=3;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 8, 3, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case INT:
			case 8:
				{
				alt8=3;
				}
				break;
			case 22:
				{
				alt8=4;
				}
				break;
			case 20:
				{
				int LA8_6 = input.LA(2);
				if ( (LA8_6==22) ) {
					alt8=5;
				}
				else if ( (LA8_6==25) ) {
					alt8=6;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 8, 6, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case 25:
				{
				alt8=7;
				}
				break;
			case 15:
				{
				alt8=9;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 8, 0, input);
				throw nvae;
			}
			switch (alt8) {
				case 1 :
					// CMinus.g:94:7: forStat
					{
					pushFollow(FOLLOW_forStat_in_stat580);
					forStat12=forStat();
					state._fsp--;

					// TEMPLATE REWRITE
					// 94:15: -> {$forStat.st}
					{
						retval.st = (forStat12!=null?((StringTemplate)forStat12.getTemplate()):null);
					}



					}
					break;
				case 2 :
					// CMinus.g:95:7: whileStat
					{
					pushFollow(FOLLOW_whileStat_in_stat592);
					whileStat13=whileStat();
					state._fsp--;

					// TEMPLATE REWRITE
					// 95:17: -> {$whileStat.st}
					{
						retval.st = (whileStat13!=null?((StringTemplate)whileStat13.getTemplate()):null);
					}



					}
					break;
				case 3 :
					// CMinus.g:96:7: expr ';'
					{
					pushFollow(FOLLOW_expr_in_stat604);
					expr14=expr();
					state._fsp--;

					match(input,15,FOLLOW_15_in_stat606); 
					// TEMPLATE REWRITE
					// 96:16: -> statement(expr=$expr.st)
					{
						retval.st = templateLib.getInstanceOf("statement",new STAttrMap().put("expr", (expr14!=null?((StringTemplate)expr14.getTemplate()):null)));
					}



					}
					break;
				case 4 :
					// CMinus.g:98:7: ifStat
					{
					pushFollow(FOLLOW_ifStat_in_stat628);
					ifStat15=ifStat();
					state._fsp--;

					// TEMPLATE REWRITE
					// 98:14: -> {$ifStat.st}
					{
						retval.st = (ifStat15!=null?((StringTemplate)ifStat15.getTemplate()):null);
					}



					}
					break;
				case 5 :
					// CMinus.g:99:7: elseIfStat
					{
					pushFollow(FOLLOW_elseIfStat_in_stat640);
					elseIfStat16=elseIfStat();
					state._fsp--;

					// TEMPLATE REWRITE
					// 99:18: -> {$elseIfStat.st}
					{
						retval.st = (elseIfStat16!=null?((StringTemplate)elseIfStat16.getTemplate()):null);
					}



					}
					break;
				case 6 :
					// CMinus.g:100:7: elseStat
					{
					pushFollow(FOLLOW_elseStat_in_stat652);
					elseStat17=elseStat();
					state._fsp--;

					// TEMPLATE REWRITE
					// 100:16: -> {$elseStat.st}
					{
						retval.st = (elseStat17!=null?((StringTemplate)elseStat17.getTemplate()):null);
					}



					}
					break;
				case 7 :
					// CMinus.g:101:7: block
					{
					pushFollow(FOLLOW_block_in_stat664);
					block();
					state._fsp--;

					// TEMPLATE REWRITE
					// 101:13: -> statementList(locals=$slist::localsstats=$slist::stats)
					{
						retval.st = templateLib.getInstanceOf("statementList",new STAttrMap().put("locals", slist_stack.peek().locals).put("stats", slist_stack.peek().stats));
					}



					}
					break;
				case 8 :
					// CMinus.g:102:7: assignStat ';'
					{
					pushFollow(FOLLOW_assignStat_in_stat686);
					assignStat18=assignStat();
					state._fsp--;

					match(input,15,FOLLOW_15_in_stat688); 
					// TEMPLATE REWRITE
					// 102:22: -> {$assignStat.st}
					{
						retval.st = (assignStat18!=null?((StringTemplate)assignStat18.getTemplate()):null);
					}



					}
					break;
				case 9 :
					// CMinus.g:103:7: ';'
					{
					match(input,15,FOLLOW_15_in_stat700); 
					// TEMPLATE REWRITE
					// 103:11: -> {new StringTemplate(\";\")}
					{
						retval.st = new StringTemplate(";");
					}



					}
					break;

			}
			retval.stop = input.LT(-1);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
			slist_stack.pop();

		}
		return retval;
	}
	// $ANTLR end "stat"


	public static class forStat_return extends ParserRuleReturnScope {
		public StringTemplate st;
		public Object getTemplate() { return st; }
		public String toString() { return st==null?null:st.toString(); }
	};


	// $ANTLR start "forStat"
	// CMinus.g:106:1: forStat : 'for' '(' e1= assignStat ';' e2= expr ';' e3= assignStat ')' block -> forLoop(e1=$e1.ste2=$e2.ste3=$e3.stlocals=$slist::localsstats=$slist::stats);
	public final CMinusParser.forStat_return forStat() throws RecognitionException {
		slist_stack.push(new slist_scope());

		CMinusParser.forStat_return retval = new CMinusParser.forStat_return();
		retval.start = input.LT(1);

		ParserRuleReturnScope e1 =null;
		ParserRuleReturnScope e2 =null;
		ParserRuleReturnScope e3 =null;


		  slist_stack.peek().locals = new ArrayList();
		  slist_stack.peek().stats = new ArrayList();

		try {
			// CMinus.g:112:5: ( 'for' '(' e1= assignStat ';' e2= expr ';' e3= assignStat ')' block -> forLoop(e1=$e1.ste2=$e2.ste3=$e3.stlocals=$slist::localsstats=$slist::stats))
			// CMinus.g:112:9: 'for' '(' e1= assignStat ';' e2= expr ';' e3= assignStat ')' block
			{
			match(input,21,FOLLOW_21_in_forStat733); 
			match(input,8,FOLLOW_8_in_forStat735); 
			pushFollow(FOLLOW_assignStat_in_forStat739);
			e1=assignStat();
			state._fsp--;

			match(input,15,FOLLOW_15_in_forStat741); 
			pushFollow(FOLLOW_expr_in_forStat745);
			e2=expr();
			state._fsp--;

			match(input,15,FOLLOW_15_in_forStat747); 
			pushFollow(FOLLOW_assignStat_in_forStat751);
			e3=assignStat();
			state._fsp--;

			match(input,9,FOLLOW_9_in_forStat753); 
			pushFollow(FOLLOW_block_in_forStat755);
			block();
			state._fsp--;

			// TEMPLATE REWRITE
			// 113:9: -> forLoop(e1=$e1.ste2=$e2.ste3=$e3.stlocals=$slist::localsstats=$slist::stats)
			{
				retval.st = templateLib.getInstanceOf("forLoop",new STAttrMap().put("e1", (e1!=null?((StringTemplate)e1.getTemplate()):null)).put("e2", (e2!=null?((StringTemplate)e2.getTemplate()):null)).put("e3", (e3!=null?((StringTemplate)e3.getTemplate()):null)).put("locals", slist_stack.peek().locals).put("stats", slist_stack.peek().stats));
			}



			}

			retval.stop = input.LT(-1);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
			slist_stack.pop();

		}
		return retval;
	}
	// $ANTLR end "forStat"


	public static class whileStat_return extends ParserRuleReturnScope {
		public StringTemplate st;
		public Object getTemplate() { return st; }
		public String toString() { return st==null?null:st.toString(); }
	};


	// $ANTLR start "whileStat"
	// CMinus.g:117:1: whileStat : 'while' '(' e1= expr ')' block -> whileLoop(e1=$e1.stlocals=$slist::localsstats=$slist::stats);
	public final CMinusParser.whileStat_return whileStat() throws RecognitionException {
		slist_stack.push(new slist_scope());

		CMinusParser.whileStat_return retval = new CMinusParser.whileStat_return();
		retval.start = input.LT(1);

		ParserRuleReturnScope e1 =null;


		  slist_stack.peek().locals = new ArrayList();
		  slist_stack.peek().stats = new ArrayList();

		try {
			// CMinus.g:123:5: ( 'while' '(' e1= expr ')' block -> whileLoop(e1=$e1.stlocals=$slist::localsstats=$slist::stats))
			// CMinus.g:123:9: 'while' '(' e1= expr ')' block
			{
			match(input,24,FOLLOW_24_in_whileStat838); 
			match(input,8,FOLLOW_8_in_whileStat840); 
			pushFollow(FOLLOW_expr_in_whileStat844);
			e1=expr();
			state._fsp--;

			match(input,9,FOLLOW_9_in_whileStat846); 
			pushFollow(FOLLOW_block_in_whileStat848);
			block();
			state._fsp--;

			// TEMPLATE REWRITE
			// 124:9: -> whileLoop(e1=$e1.stlocals=$slist::localsstats=$slist::stats)
			{
				retval.st = templateLib.getInstanceOf("whileLoop",new STAttrMap().put("e1", (e1!=null?((StringTemplate)e1.getTemplate()):null)).put("locals", slist_stack.peek().locals).put("stats", slist_stack.peek().stats));
			}



			}

			retval.stop = input.LT(-1);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
			slist_stack.pop();

		}
		return retval;
	}
	// $ANTLR end "whileStat"


	public static class ifStat_return extends ParserRuleReturnScope {
		public StringTemplate st;
		public Object getTemplate() { return st; }
		public String toString() { return st==null?null:st.toString(); }
	};


	// $ANTLR start "ifStat"
	// CMinus.g:128:1: ifStat : 'if' '(' e1= expr ')' block -> ifClause(e1=$e1.stlocals=$slist::localsstats=$slist::stats);
	public final CMinusParser.ifStat_return ifStat() throws RecognitionException {
		slist_stack.push(new slist_scope());

		CMinusParser.ifStat_return retval = new CMinusParser.ifStat_return();
		retval.start = input.LT(1);

		ParserRuleReturnScope e1 =null;


		  slist_stack.peek().locals = new ArrayList();
		  slist_stack.peek().stats = new ArrayList();

		try {
			// CMinus.g:134:5: ( 'if' '(' e1= expr ')' block -> ifClause(e1=$e1.stlocals=$slist::localsstats=$slist::stats))
			// CMinus.g:134:9: 'if' '(' e1= expr ')' block
			{
			match(input,22,FOLLOW_22_in_ifStat925); 
			match(input,8,FOLLOW_8_in_ifStat927); 
			pushFollow(FOLLOW_expr_in_ifStat931);
			e1=expr();
			state._fsp--;

			match(input,9,FOLLOW_9_in_ifStat933); 
			pushFollow(FOLLOW_block_in_ifStat935);
			block();
			state._fsp--;

			// TEMPLATE REWRITE
			// 135:9: -> ifClause(e1=$e1.stlocals=$slist::localsstats=$slist::stats)
			{
				retval.st = templateLib.getInstanceOf("ifClause",new STAttrMap().put("e1", (e1!=null?((StringTemplate)e1.getTemplate()):null)).put("locals", slist_stack.peek().locals).put("stats", slist_stack.peek().stats));
			}



			}

			retval.stop = input.LT(-1);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
			slist_stack.pop();

		}
		return retval;
	}
	// $ANTLR end "ifStat"


	public static class elseIfStat_return extends ParserRuleReturnScope {
		public StringTemplate st;
		public Object getTemplate() { return st; }
		public String toString() { return st==null?null:st.toString(); }
	};


	// $ANTLR start "elseIfStat"
	// CMinus.g:139:1: elseIfStat : 'else' 'if' '(' e1= expr ')' block -> elseIfClause(e1=$e1.stlocals=$slist::localsstats=$slist::stats);
	public final CMinusParser.elseIfStat_return elseIfStat() throws RecognitionException {
		slist_stack.push(new slist_scope());

		CMinusParser.elseIfStat_return retval = new CMinusParser.elseIfStat_return();
		retval.start = input.LT(1);

		ParserRuleReturnScope e1 =null;


		  slist_stack.peek().locals = new ArrayList();
		  slist_stack.peek().stats = new ArrayList();

		try {
			// CMinus.g:145:5: ( 'else' 'if' '(' e1= expr ')' block -> elseIfClause(e1=$e1.stlocals=$slist::localsstats=$slist::stats))
			// CMinus.g:145:9: 'else' 'if' '(' e1= expr ')' block
			{
			match(input,20,FOLLOW_20_in_elseIfStat1012); 
			match(input,22,FOLLOW_22_in_elseIfStat1014); 
			match(input,8,FOLLOW_8_in_elseIfStat1016); 
			pushFollow(FOLLOW_expr_in_elseIfStat1020);
			e1=expr();
			state._fsp--;

			match(input,9,FOLLOW_9_in_elseIfStat1022); 
			pushFollow(FOLLOW_block_in_elseIfStat1024);
			block();
			state._fsp--;

			// TEMPLATE REWRITE
			// 146:9: -> elseIfClause(e1=$e1.stlocals=$slist::localsstats=$slist::stats)
			{
				retval.st = templateLib.getInstanceOf("elseIfClause",new STAttrMap().put("e1", (e1!=null?((StringTemplate)e1.getTemplate()):null)).put("locals", slist_stack.peek().locals).put("stats", slist_stack.peek().stats));
			}



			}

			retval.stop = input.LT(-1);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
			slist_stack.pop();

		}
		return retval;
	}
	// $ANTLR end "elseIfStat"


	public static class elseStat_return extends ParserRuleReturnScope {
		public StringTemplate st;
		public Object getTemplate() { return st; }
		public String toString() { return st==null?null:st.toString(); }
	};


	// $ANTLR start "elseStat"
	// CMinus.g:150:1: elseStat : 'else' block -> elseClause(locals=$slist::localsstats=$slist::stats);
	public final CMinusParser.elseStat_return elseStat() throws RecognitionException {
		slist_stack.push(new slist_scope());

		CMinusParser.elseStat_return retval = new CMinusParser.elseStat_return();
		retval.start = input.LT(1);


		  slist_stack.peek().locals = new ArrayList();
		  slist_stack.peek().stats = new ArrayList();

		try {
			// CMinus.g:156:5: ( 'else' block -> elseClause(locals=$slist::localsstats=$slist::stats))
			// CMinus.g:156:9: 'else' block
			{
			match(input,20,FOLLOW_20_in_elseStat1101); 
			pushFollow(FOLLOW_block_in_elseStat1103);
			block();
			state._fsp--;

			// TEMPLATE REWRITE
			// 157:9: -> elseClause(locals=$slist::localsstats=$slist::stats)
			{
				retval.st = templateLib.getInstanceOf("elseClause",new STAttrMap().put("locals", slist_stack.peek().locals).put("stats", slist_stack.peek().stats));
			}



			}

			retval.stop = input.LT(-1);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
			slist_stack.pop();

		}
		return retval;
	}
	// $ANTLR end "elseStat"


	public static class assignStat_return extends ParserRuleReturnScope {
		public StringTemplate st;
		public Object getTemplate() { return st; }
		public String toString() { return st==null?null:st.toString(); }
	};


	// $ANTLR start "assignStat"
	// CMinus.g:163:1: assignStat : ID '=' expr -> assign(lhs=$ID.textrhs=$expr.st);
	public final CMinusParser.assignStat_return assignStat() throws RecognitionException {
		CMinusParser.assignStat_return retval = new CMinusParser.assignStat_return();
		retval.start = input.LT(1);

		Token ID19=null;
		ParserRuleReturnScope expr20 =null;

		try {
			// CMinus.g:164:5: ( ID '=' expr -> assign(lhs=$ID.textrhs=$expr.st))
			// CMinus.g:164:9: ID '=' expr
			{
			ID19=(Token)match(input,ID,FOLLOW_ID_in_assignStat1147); 
			match(input,17,FOLLOW_17_in_assignStat1149); 
			pushFollow(FOLLOW_expr_in_assignStat1151);
			expr20=expr();
			state._fsp--;

			// TEMPLATE REWRITE
			// 164:21: -> assign(lhs=$ID.textrhs=$expr.st)
			{
				retval.st = templateLib.getInstanceOf("assign",new STAttrMap().put("lhs", (ID19!=null?ID19.getText():null)).put("rhs", (expr20!=null?((StringTemplate)expr20.getTemplate()):null)));
			}



			}

			retval.stop = input.LT(-1);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "assignStat"


	public static class expr_return extends ParserRuleReturnScope {
		public StringTemplate st;
		public Object getTemplate() { return st; }
		public String toString() { return st==null?null:st.toString(); }
	};


	// $ANTLR start "expr"
	// CMinus.g:167:1: expr : condExpr -> {$condExpr.st};
	public final CMinusParser.expr_return expr() throws RecognitionException {
		CMinusParser.expr_return retval = new CMinusParser.expr_return();
		retval.start = input.LT(1);

		ParserRuleReturnScope condExpr21 =null;

		try {
			// CMinus.g:167:5: ( condExpr -> {$condExpr.st})
			// CMinus.g:167:9: condExpr
			{
			pushFollow(FOLLOW_condExpr_in_expr1179);
			condExpr21=condExpr();
			state._fsp--;

			// TEMPLATE REWRITE
			// 167:18: -> {$condExpr.st}
			{
				retval.st = (condExpr21!=null?((StringTemplate)condExpr21.getTemplate()):null);
			}



			}

			retval.stop = input.LT(-1);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "expr"


	public static class condExpr_return extends ParserRuleReturnScope {
		public StringTemplate st;
		public Object getTemplate() { return st; }
		public String toString() { return st==null?null:st.toString(); }
	};


	// $ANTLR start "condExpr"
	// CMinus.g:170:1: condExpr : a= aexpr ( ( '==' b= aexpr -> equals(left=$a.stright=$b.st)| '<' b= aexpr -> lessThan(left=$a.stright=$b.st)) | -> {$a.st}) ;
	public final CMinusParser.condExpr_return condExpr() throws RecognitionException {
		CMinusParser.condExpr_return retval = new CMinusParser.condExpr_return();
		retval.start = input.LT(1);

		ParserRuleReturnScope a =null;
		ParserRuleReturnScope b =null;

		try {
			// CMinus.g:171:5: (a= aexpr ( ( '==' b= aexpr -> equals(left=$a.stright=$b.st)| '<' b= aexpr -> lessThan(left=$a.stright=$b.st)) | -> {$a.st}) )
			// CMinus.g:171:9: a= aexpr ( ( '==' b= aexpr -> equals(left=$a.stright=$b.st)| '<' b= aexpr -> lessThan(left=$a.stright=$b.st)) | -> {$a.st})
			{
			pushFollow(FOLLOW_aexpr_in_condExpr1204);
			a=aexpr();
			state._fsp--;

			// CMinus.g:172:9: ( ( '==' b= aexpr -> equals(left=$a.stright=$b.st)| '<' b= aexpr -> lessThan(left=$a.stright=$b.st)) | -> {$a.st})
			int alt10=2;
			int LA10_0 = input.LA(1);
			if ( (LA10_0==16||LA10_0==18) ) {
				alt10=1;
			}
			else if ( (LA10_0==9||LA10_0==15) ) {
				alt10=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 10, 0, input);
				throw nvae;
			}

			switch (alt10) {
				case 1 :
					// CMinus.g:172:13: ( '==' b= aexpr -> equals(left=$a.stright=$b.st)| '<' b= aexpr -> lessThan(left=$a.stright=$b.st))
					{
					// CMinus.g:172:13: ( '==' b= aexpr -> equals(left=$a.stright=$b.st)| '<' b= aexpr -> lessThan(left=$a.stright=$b.st))
					int alt9=2;
					int LA9_0 = input.LA(1);
					if ( (LA9_0==18) ) {
						alt9=1;
					}
					else if ( (LA9_0==16) ) {
						alt9=2;
					}

					else {
						NoViableAltException nvae =
							new NoViableAltException("", 9, 0, input);
						throw nvae;
					}

					switch (alt9) {
						case 1 :
							// CMinus.g:172:16: '==' b= aexpr
							{
							match(input,18,FOLLOW_18_in_condExpr1221); 
							pushFollow(FOLLOW_aexpr_in_condExpr1225);
							b=aexpr();
							state._fsp--;

							// TEMPLATE REWRITE
							// 172:29: -> equals(left=$a.stright=$b.st)
							{
								retval.st = templateLib.getInstanceOf("equals",new STAttrMap().put("left", (a!=null?((StringTemplate)a.getTemplate()):null)).put("right", (b!=null?((StringTemplate)b.getTemplate()):null)));
							}



							}
							break;
						case 2 :
							// CMinus.g:173:16: '<' b= aexpr
							{
							match(input,16,FOLLOW_16_in_condExpr1255); 
							pushFollow(FOLLOW_aexpr_in_condExpr1259);
							b=aexpr();
							state._fsp--;

							// TEMPLATE REWRITE
							// 173:30: -> lessThan(left=$a.stright=$b.st)
							{
								retval.st = templateLib.getInstanceOf("lessThan",new STAttrMap().put("left", (a!=null?((StringTemplate)a.getTemplate()):null)).put("right", (b!=null?((StringTemplate)b.getTemplate()):null)));
							}



							}
							break;

					}

					}
					break;
				case 2 :
					// CMinus.g:175:13: 
					{
					// TEMPLATE REWRITE
					// 175:13: -> {$a.st}
					{
						retval.st = (a!=null?((StringTemplate)a.getTemplate()):null);
					}



					}
					break;

			}

			}

			retval.stop = input.LT(-1);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "condExpr"


	public static class aexpr_return extends ParserRuleReturnScope {
		public StringTemplate st;
		public Object getTemplate() { return st; }
		public String toString() { return st==null?null:st.toString(); }
	};


	// $ANTLR start "aexpr"
	// CMinus.g:179:1: aexpr : (a= atom -> {$a.st}) ( '+' b= atom -> add(left=$aexpr.stright=$b.st))* ( '-' b= atom -> minus(left=$aexpr.stright=$b.st))* ( '*' b= atom -> mult(left=$aexpr.stright=$b.st))* ( '/' b= atom -> div(left=$aexpr.stright=$b.st))* ( '%' b= atom -> mod(left=$aexpr.stright=$b.st))* ;
	public final CMinusParser.aexpr_return aexpr() throws RecognitionException {
		CMinusParser.aexpr_return retval = new CMinusParser.aexpr_return();
		retval.start = input.LT(1);

		ParserRuleReturnScope a =null;
		ParserRuleReturnScope b =null;

		try {
			// CMinus.g:180:5: ( (a= atom -> {$a.st}) ( '+' b= atom -> add(left=$aexpr.stright=$b.st))* ( '-' b= atom -> minus(left=$aexpr.stright=$b.st))* ( '*' b= atom -> mult(left=$aexpr.stright=$b.st))* ( '/' b= atom -> div(left=$aexpr.stright=$b.st))* ( '%' b= atom -> mod(left=$aexpr.stright=$b.st))* )
			// CMinus.g:180:9: (a= atom -> {$a.st}) ( '+' b= atom -> add(left=$aexpr.stright=$b.st))* ( '-' b= atom -> minus(left=$aexpr.stright=$b.st))* ( '*' b= atom -> mult(left=$aexpr.stright=$b.st))* ( '/' b= atom -> div(left=$aexpr.stright=$b.st))* ( '%' b= atom -> mod(left=$aexpr.stright=$b.st))*
			{
			// CMinus.g:180:9: (a= atom -> {$a.st})
			// CMinus.g:180:10: a= atom
			{
			pushFollow(FOLLOW_atom_in_aexpr1337);
			a=atom();
			state._fsp--;

			// TEMPLATE REWRITE
			// 180:17: -> {$a.st}
			{
				retval.st = (a!=null?((StringTemplate)a.getTemplate()):null);
			}



			}

			// CMinus.g:181:9: ( '+' b= atom -> add(left=$aexpr.stright=$b.st))*
			loop11:
			while (true) {
				int alt11=2;
				int LA11_0 = input.LA(1);
				if ( (LA11_0==11) ) {
					alt11=1;
				}

				switch (alt11) {
				case 1 :
					// CMinus.g:181:11: '+' b= atom
					{
					match(input,11,FOLLOW_11_in_aexpr1354); 
					pushFollow(FOLLOW_atom_in_aexpr1358);
					b=atom();
					state._fsp--;

					// TEMPLATE REWRITE
					// 181:22: -> add(left=$aexpr.stright=$b.st)
					{
						retval.st = templateLib.getInstanceOf("add",new STAttrMap().put("left", retval.st).put("right", (b!=null?((StringTemplate)b.getTemplate()):null)));
					}



					}
					break;

				default :
					break loop11;
				}
			}

			// CMinus.g:182:9: ( '-' b= atom -> minus(left=$aexpr.stright=$b.st))*
			loop12:
			while (true) {
				int alt12=2;
				int LA12_0 = input.LA(1);
				if ( (LA12_0==13) ) {
					alt12=1;
				}

				switch (alt12) {
				case 1 :
					// CMinus.g:182:11: '-' b= atom
					{
					match(input,13,FOLLOW_13_in_aexpr1387); 
					pushFollow(FOLLOW_atom_in_aexpr1391);
					b=atom();
					state._fsp--;

					// TEMPLATE REWRITE
					// 182:22: -> minus(left=$aexpr.stright=$b.st)
					{
						retval.st = templateLib.getInstanceOf("minus",new STAttrMap().put("left", retval.st).put("right", (b!=null?((StringTemplate)b.getTemplate()):null)));
					}



					}
					break;

				default :
					break loop12;
				}
			}

			// CMinus.g:183:9: ( '*' b= atom -> mult(left=$aexpr.stright=$b.st))*
			loop13:
			while (true) {
				int alt13=2;
				int LA13_0 = input.LA(1);
				if ( (LA13_0==10) ) {
					alt13=1;
				}

				switch (alt13) {
				case 1 :
					// CMinus.g:183:11: '*' b= atom
					{
					match(input,10,FOLLOW_10_in_aexpr1420); 
					pushFollow(FOLLOW_atom_in_aexpr1424);
					b=atom();
					state._fsp--;

					// TEMPLATE REWRITE
					// 183:22: -> mult(left=$aexpr.stright=$b.st)
					{
						retval.st = templateLib.getInstanceOf("mult",new STAttrMap().put("left", retval.st).put("right", (b!=null?((StringTemplate)b.getTemplate()):null)));
					}



					}
					break;

				default :
					break loop13;
				}
			}

			// CMinus.g:184:9: ( '/' b= atom -> div(left=$aexpr.stright=$b.st))*
			loop14:
			while (true) {
				int alt14=2;
				int LA14_0 = input.LA(1);
				if ( (LA14_0==14) ) {
					alt14=1;
				}

				switch (alt14) {
				case 1 :
					// CMinus.g:184:11: '/' b= atom
					{
					match(input,14,FOLLOW_14_in_aexpr1453); 
					pushFollow(FOLLOW_atom_in_aexpr1457);
					b=atom();
					state._fsp--;

					// TEMPLATE REWRITE
					// 184:22: -> div(left=$aexpr.stright=$b.st)
					{
						retval.st = templateLib.getInstanceOf("div",new STAttrMap().put("left", retval.st).put("right", (b!=null?((StringTemplate)b.getTemplate()):null)));
					}



					}
					break;

				default :
					break loop14;
				}
			}

			// CMinus.g:185:9: ( '%' b= atom -> mod(left=$aexpr.stright=$b.st))*
			loop15:
			while (true) {
				int alt15=2;
				int LA15_0 = input.LA(1);
				if ( (LA15_0==7) ) {
					alt15=1;
				}

				switch (alt15) {
				case 1 :
					// CMinus.g:185:11: '%' b= atom
					{
					match(input,7,FOLLOW_7_in_aexpr1486); 
					pushFollow(FOLLOW_atom_in_aexpr1490);
					b=atom();
					state._fsp--;

					// TEMPLATE REWRITE
					// 185:22: -> mod(left=$aexpr.stright=$b.st)
					{
						retval.st = templateLib.getInstanceOf("mod",new STAttrMap().put("left", retval.st).put("right", (b!=null?((StringTemplate)b.getTemplate()):null)));
					}



					}
					break;

				default :
					break loop15;
				}
			}

			}

			retval.stop = input.LT(-1);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "aexpr"


	public static class atom_return extends ParserRuleReturnScope {
		public StringTemplate st;
		public Object getTemplate() { return st; }
		public String toString() { return st==null?null:st.toString(); }
	};


	// $ANTLR start "atom"
	// CMinus.g:188:1: atom : ( ID -> refVar(id=$ID.text)| INT -> iconst(value=$INT.text)| '(' expr ')' -> {$expr.st});
	public final CMinusParser.atom_return atom() throws RecognitionException {
		CMinusParser.atom_return retval = new CMinusParser.atom_return();
		retval.start = input.LT(1);

		Token ID22=null;
		Token INT23=null;
		ParserRuleReturnScope expr24 =null;

		try {
			// CMinus.g:189:5: ( ID -> refVar(id=$ID.text)| INT -> iconst(value=$INT.text)| '(' expr ')' -> {$expr.st})
			int alt16=3;
			switch ( input.LA(1) ) {
			case ID:
				{
				alt16=1;
				}
				break;
			case INT:
				{
				alt16=2;
				}
				break;
			case 8:
				{
				alt16=3;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 16, 0, input);
				throw nvae;
			}
			switch (alt16) {
				case 1 :
					// CMinus.g:189:7: ID
					{
					ID22=(Token)match(input,ID,FOLLOW_ID_in_atom1524); 
					// TEMPLATE REWRITE
					// 189:10: -> refVar(id=$ID.text)
					{
						retval.st = templateLib.getInstanceOf("refVar",new STAttrMap().put("id", (ID22!=null?ID22.getText():null)));
					}



					}
					break;
				case 2 :
					// CMinus.g:190:7: INT
					{
					INT23=(Token)match(input,INT,FOLLOW_INT_in_atom1541); 
					// TEMPLATE REWRITE
					// 190:11: -> iconst(value=$INT.text)
					{
						retval.st = templateLib.getInstanceOf("iconst",new STAttrMap().put("value", (INT23!=null?INT23.getText():null)));
					}



					}
					break;
				case 3 :
					// CMinus.g:191:7: '(' expr ')'
					{
					match(input,8,FOLLOW_8_in_atom1558); 
					pushFollow(FOLLOW_expr_in_atom1560);
					expr24=expr();
					state._fsp--;

					match(input,9,FOLLOW_9_in_atom1562); 
					// TEMPLATE REWRITE
					// 191:20: -> {$expr.st}
					{
						retval.st = (expr24!=null?((StringTemplate)expr24.getTemplate()):null);
					}



					}
					break;

			}
			retval.stop = input.LT(-1);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "atom"

	// Delegated rules



	public static final BitSet FOLLOW_declaration_in_program48 = new BitSet(new long[]{0x0000000000880012L});
	public static final BitSet FOLLOW_variable_in_declaration89 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_function_in_declaration105 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_type_in_variable129 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_declarator_in_variable131 = new BitSet(new long[]{0x0000000000008000L});
	public static final BitSet FOLLOW_15_in_variable133 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_ID_in_declarator207 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_type_in_function244 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_ID_in_function246 = new BitSet(new long[]{0x0000000000000100L});
	public static final BitSet FOLLOW_8_in_function258 = new BitSet(new long[]{0x0000000000880210L});
	public static final BitSet FOLLOW_formalParameter_in_function264 = new BitSet(new long[]{0x0000000000001200L});
	public static final BitSet FOLLOW_12_in_function268 = new BitSet(new long[]{0x0000000000880010L});
	public static final BitSet FOLLOW_formalParameter_in_function272 = new BitSet(new long[]{0x0000000000001200L});
	public static final BitSet FOLLOW_9_in_function280 = new BitSet(new long[]{0x0000000002000000L});
	public static final BitSet FOLLOW_block_in_function290 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_type_in_formalParameter406 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_declarator_in_formalParameter408 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_23_in_type449 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_19_in_type466 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_ID_in_type482 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_25_in_block513 = new BitSet(new long[]{0x0000000007F88130L});
	public static final BitSet FOLLOW_variable_in_block524 = new BitSet(new long[]{0x0000000007F88130L});
	public static final BitSet FOLLOW_stat_in_block540 = new BitSet(new long[]{0x0000000007708130L});
	public static final BitSet FOLLOW_26_in_block553 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_forStat_in_stat580 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_whileStat_in_stat592 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_expr_in_stat604 = new BitSet(new long[]{0x0000000000008000L});
	public static final BitSet FOLLOW_15_in_stat606 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_ifStat_in_stat628 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_elseIfStat_in_stat640 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_elseStat_in_stat652 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_block_in_stat664 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_assignStat_in_stat686 = new BitSet(new long[]{0x0000000000008000L});
	public static final BitSet FOLLOW_15_in_stat688 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_15_in_stat700 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_21_in_forStat733 = new BitSet(new long[]{0x0000000000000100L});
	public static final BitSet FOLLOW_8_in_forStat735 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_assignStat_in_forStat739 = new BitSet(new long[]{0x0000000000008000L});
	public static final BitSet FOLLOW_15_in_forStat741 = new BitSet(new long[]{0x0000000000000130L});
	public static final BitSet FOLLOW_expr_in_forStat745 = new BitSet(new long[]{0x0000000000008000L});
	public static final BitSet FOLLOW_15_in_forStat747 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_assignStat_in_forStat751 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_9_in_forStat753 = new BitSet(new long[]{0x0000000002000000L});
	public static final BitSet FOLLOW_block_in_forStat755 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_24_in_whileStat838 = new BitSet(new long[]{0x0000000000000100L});
	public static final BitSet FOLLOW_8_in_whileStat840 = new BitSet(new long[]{0x0000000000000130L});
	public static final BitSet FOLLOW_expr_in_whileStat844 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_9_in_whileStat846 = new BitSet(new long[]{0x0000000002000000L});
	public static final BitSet FOLLOW_block_in_whileStat848 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_22_in_ifStat925 = new BitSet(new long[]{0x0000000000000100L});
	public static final BitSet FOLLOW_8_in_ifStat927 = new BitSet(new long[]{0x0000000000000130L});
	public static final BitSet FOLLOW_expr_in_ifStat931 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_9_in_ifStat933 = new BitSet(new long[]{0x0000000002000000L});
	public static final BitSet FOLLOW_block_in_ifStat935 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_20_in_elseIfStat1012 = new BitSet(new long[]{0x0000000000400000L});
	public static final BitSet FOLLOW_22_in_elseIfStat1014 = new BitSet(new long[]{0x0000000000000100L});
	public static final BitSet FOLLOW_8_in_elseIfStat1016 = new BitSet(new long[]{0x0000000000000130L});
	public static final BitSet FOLLOW_expr_in_elseIfStat1020 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_9_in_elseIfStat1022 = new BitSet(new long[]{0x0000000002000000L});
	public static final BitSet FOLLOW_block_in_elseIfStat1024 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_20_in_elseStat1101 = new BitSet(new long[]{0x0000000002000000L});
	public static final BitSet FOLLOW_block_in_elseStat1103 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_ID_in_assignStat1147 = new BitSet(new long[]{0x0000000000020000L});
	public static final BitSet FOLLOW_17_in_assignStat1149 = new BitSet(new long[]{0x0000000000000130L});
	public static final BitSet FOLLOW_expr_in_assignStat1151 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_condExpr_in_expr1179 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_aexpr_in_condExpr1204 = new BitSet(new long[]{0x0000000000050002L});
	public static final BitSet FOLLOW_18_in_condExpr1221 = new BitSet(new long[]{0x0000000000000130L});
	public static final BitSet FOLLOW_aexpr_in_condExpr1225 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_16_in_condExpr1255 = new BitSet(new long[]{0x0000000000000130L});
	public static final BitSet FOLLOW_aexpr_in_condExpr1259 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_atom_in_aexpr1337 = new BitSet(new long[]{0x0000000000006C82L});
	public static final BitSet FOLLOW_11_in_aexpr1354 = new BitSet(new long[]{0x0000000000000130L});
	public static final BitSet FOLLOW_atom_in_aexpr1358 = new BitSet(new long[]{0x0000000000006C82L});
	public static final BitSet FOLLOW_13_in_aexpr1387 = new BitSet(new long[]{0x0000000000000130L});
	public static final BitSet FOLLOW_atom_in_aexpr1391 = new BitSet(new long[]{0x0000000000006482L});
	public static final BitSet FOLLOW_10_in_aexpr1420 = new BitSet(new long[]{0x0000000000000130L});
	public static final BitSet FOLLOW_atom_in_aexpr1424 = new BitSet(new long[]{0x0000000000004482L});
	public static final BitSet FOLLOW_14_in_aexpr1453 = new BitSet(new long[]{0x0000000000000130L});
	public static final BitSet FOLLOW_atom_in_aexpr1457 = new BitSet(new long[]{0x0000000000004082L});
	public static final BitSet FOLLOW_7_in_aexpr1486 = new BitSet(new long[]{0x0000000000000130L});
	public static final BitSet FOLLOW_atom_in_aexpr1490 = new BitSet(new long[]{0x0000000000000082L});
	public static final BitSet FOLLOW_ID_in_atom1524 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INT_in_atom1541 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_8_in_atom1558 = new BitSet(new long[]{0x0000000000000130L});
	public static final BitSet FOLLOW_expr_in_atom1560 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_9_in_atom1562 = new BitSet(new long[]{0x0000000000000002L});
}
