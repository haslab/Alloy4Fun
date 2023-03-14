package pt.haslab.alloy4fun.metrics.utils;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Decl;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprBinary;
import edu.mit.csail.sdg.ast.ExprCall;
import edu.mit.csail.sdg.ast.ExprConstant;
import edu.mit.csail.sdg.ast.ExprHasName;
import edu.mit.csail.sdg.ast.ExprITE;
import edu.mit.csail.sdg.ast.ExprLet;
import edu.mit.csail.sdg.ast.ExprList;
import edu.mit.csail.sdg.ast.ExprQt;
import edu.mit.csail.sdg.ast.ExprUnary;
import edu.mit.csail.sdg.ast.ExprUnary.Op;
import edu.mit.csail.sdg.ast.ExprVar;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.ast.VisitReturn;

/**
 * Immutable; this class rearranges the AST to promote as many clauses up to the
 * top level as possible (in order to get better precision unsat core results)
 */

public final class PrintExpr extends VisitReturn<StringBuilder> {

    /** {@inheritDoc} */
    @Override
    public StringBuilder visit(ExprBinary x) throws Err {
    	StringBuilder a = new StringBuilder();
    	a.append(visitThis(x.left));
    	a.append(" ");
    	a.append(x.op);
    	a.append(" ");
    	a.append(visitThis(x.right));
        return a;
    }

    /** {@inheritDoc} */
    @Override
    public StringBuilder visit(ExprQt x) throws Err {
    	StringBuilder a = new StringBuilder();
    	a.append("(");
    	a.append(x.op);
    	a.append(" ");
    	for (Decl d : x.decls) {
    		for (ExprHasName e : d.names) {
    			a.append(visitThis(e));
    			a.append(",");
    		}
        	a.deleteCharAt(a.length()-1);
    		a.append(":");
    		a.append(visitThis(d.expr));
			a.append(",");
    	}
    	a.deleteCharAt(a.length()-1);
    	a.append(" | ");
    	a.append(visitThis(x.sub));
    	a.append(")");
        return a;
    }

    /** {@inheritDoc} */
    @Override
    public StringBuilder visit(ExprUnary x) throws Err {
    	if (x.op==Op.NOOP || x.op==Op.ONEOF || x.op==Op.LONEOF || x.op==Op.SOMEOF)
    		return visitThis(x.sub);
    	StringBuilder a = new StringBuilder();
    	a.append(x.op);
    	a.append(" ");
    	a.append(visitThis(x.sub));
        return a;
    }

    /** {@inheritDoc} */
    @Override
    public StringBuilder visit(ExprList x) {
    	StringBuilder a = new StringBuilder();
    	a.append(x.op);
    	a.append("[");
    	for (Expr y : x.args) {
    		a.append(visitThis(y));
			a.append(",");
    	}
    	a.deleteCharAt(a.length()-1);
    	a.append("]");
    	return a;
    }

    /** {@inheritDoc} */
    @Override
    public StringBuilder visit(ExprCall x) {
    	StringBuilder a = new StringBuilder();
    	a.append(x.fun.label);
    	a.append("[");
    	for (Expr y : x.args) {
    		a.append(visitThis(y));
			a.append(",");
    	}
    	a.deleteCharAt(a.length()-1);
    	a.append("]");
        return a;
    }

    /** {@inheritDoc} */
    @Override
    public StringBuilder visit(ExprConstant x) {
    	StringBuilder a = new StringBuilder();
    	a.append(x);
        return a;
    }

    /** {@inheritDoc} */
    @Override
    public StringBuilder visit(ExprITE x) {
    	StringBuilder a = new StringBuilder();
    	a.append("(");
    	a.append(visitThis(x.cond));
    	a.append(" => ");
    	a.append(visitThis(x.left));
    	a.append(" , ");
    	a.append(visitThis(x.right));
    	a.append(" ) ");
    	return a;
    }

    /** {@inheritDoc} */
    @Override
    public StringBuilder visit(ExprLet x) {
    	StringBuilder a = new StringBuilder();
    	a.append("(");
    	a.append(visitThis(x.var));
    	a.append("=");
    	a.append(visitThis(x.expr));
    	a.append(" | ");
    	a.append(visitThis(x.sub));
    	a.append(" ) ");
    	return a;
    }

    /** {@inheritDoc} */
    @Override
    public StringBuilder visit(ExprVar x) {
    	StringBuilder a = new StringBuilder();
    	a.append(x.label);
        return a;
    }

    /** {@inheritDoc} */
    @Override
    public StringBuilder visit(Sig x) {
    	StringBuilder a = new StringBuilder();
    	String[] lbls = x.label.split("this/");
    	a.append(lbls[lbls.length-1]);
        return a;
    }

    /** {@inheritDoc} */
    @Override
    public StringBuilder visit(Field x) {
    	StringBuilder a = new StringBuilder();
    	String[] lbls = x.label.split("this/");
    	a.append(lbls[lbls.length-1]);
        return a;
    }
}
