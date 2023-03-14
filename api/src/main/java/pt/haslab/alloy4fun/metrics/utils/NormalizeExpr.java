package pt.haslab.alloy4fun.metrics.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public final class NormalizeExpr extends VisitReturn<Expr> {
	int vcounter = 0;
	Map<ExprHasName,ExprHasName> renames = new HashMap<>();

    /** {@inheritDoc} */
    @Override
    public Expr visit(ExprBinary x) throws Err {
        Expr a = visitThis(x.left);
        Expr b = visitThis(x.right);
        return x.op.make(null, null, a, b);
    }

    /** {@inheritDoc} */
    @Override
    public Expr visit(ExprQt x) throws Err {
    	List<Decl> ds = new ArrayList<>();
        for (Decl d : x.decls) {
        	Expr dd = visitThis(d.expr);
        	for (ExprHasName v : d.names) {
	        	Decl nd = dd.oneOf(""+vcounter++);
	        	renames.put(v, nd.get());
	        	ds.add(nd);
        	}
        }
        Expr a = visitThis(x.sub);
        for (Decl d : ds)
        	a = x.op.make(null, null, Arrays.asList(d), a);
        return a;
    }

    /** {@inheritDoc} */
    @Override
    public Expr visit(ExprUnary x) throws Err {
    	if (x.op==Op.NOOP)
    		return visitThis(x.sub);
        Expr a = visitThis(x.sub);
        return x.op.make(null, a);
    }

    /** {@inheritDoc} */
    @Override
    public Expr visit(ExprList x) {
        List<Expr> args = new ArrayList<>();
        for (Expr y : x.args)
        	args.add(visitThis(y));
		return ExprList.make(null, null, x.op, args);
    }

    /** {@inheritDoc} */
    @Override
    public Expr visit(ExprCall x) {
        List<Expr> args = new ArrayList<>();
        for (Expr y : x.args)
        	args.add(visitThis(y));
        return x.fun.call(args.toArray(new Expr[args.size()]));
    }

    /** {@inheritDoc} */
    @Override
    public Expr visit(ExprConstant x) {
        return x;
    }

    /** {@inheritDoc} */
    @Override
    public Expr visit(ExprITE x) {
    	Expr a = visitThis(x.cond);
    	Expr b = visitThis(x.left);
    	Expr c = visitThis(x.right);
    	return ExprITE.make(null, a, b, c);
    }

    /** {@inheritDoc} */
    @Override
    public Expr visit(ExprLet x) {
    	Expr a = visitThis(x.expr);
    	Expr b = visitThis(x.sub);
    	ExprVar nv = ExprVar.make(null, ""+vcounter++);
    	renames.put(x.var, nv);
    	return ExprLet.make(null, nv, a, b);
    }

    /** {@inheritDoc} */
    @Override
    public Expr visit(ExprVar x) {
        return renames.get(x);
    }

    /** {@inheritDoc} */
    @Override
    public Expr visit(Sig x) {
        return x;
    }

    /** {@inheritDoc} */
    @Override
    public Expr visit(Field x) {
        return x;
    }
}
