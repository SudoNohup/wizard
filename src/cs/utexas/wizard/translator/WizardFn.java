package cs.utexas.wizard.translator;

public class WizardFn {
	public static String returnStmt(String x) {
		return "(return " + x + ")";
	}

	public static String condExprEq(String lhs, String rhs) {
		return "(" + lhs + "==" + rhs + ")";
	}

	public static String condExprNe(String lhs, String rhs) {
		return "(" + lhs + "!=" + rhs + ")";
	}

	public static String ifStmt(String expr) {
		return "if" + expr;
	}

	public static String gotoStmt(String expr) {
		return "goto " + expr;
	}

	public static String assignStmt(String lhs, String rhs) {
		return "(" + lhs + "=" + rhs + ")";
	}

	public static String aExprMod(String lhs, String rhs) {
		return "(" + lhs + "%" + rhs + ")";
	}
	
	public static String andStmt(String lhs, String rhs) {
		return "(" + lhs + ";" + rhs + ")";
	}
}
