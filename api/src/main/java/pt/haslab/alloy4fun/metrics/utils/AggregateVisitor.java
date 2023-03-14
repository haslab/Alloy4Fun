package pt.haslab.alloy4fun.metrics.utils;
import java.util.Set;
import java.util.function.BinaryOperator;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.ExprBinary;
import edu.mit.csail.sdg.ast.ExprCall;
import edu.mit.csail.sdg.ast.ExprConstant;
import edu.mit.csail.sdg.ast.ExprITE;
import edu.mit.csail.sdg.ast.ExprLet;
import edu.mit.csail.sdg.ast.ExprList;
import edu.mit.csail.sdg.ast.ExprQt;
import edu.mit.csail.sdg.ast.ExprUnary;
import edu.mit.csail.sdg.ast.ExprVar;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.VisitReturn;
import edu.mit.csail.sdg.ast.Sig.Field;

public class AggregateVisitor<T> extends VisitReturn<T> {
	
	BinaryOperator<T> aggregator;
	T stop;
	Set<String> relevants;
	protected boolean counting = false;

	public AggregateVisitor(BinaryOperator<T> aggregator,T stop, Set<String> relevants) {
		this.stop = stop;
		this.aggregator = aggregator;
		this.relevants = relevants;
	}

	@Override
	public T visit(ExprBinary x) throws Err {
		return aggregator.apply(x.left.accept(this), x.right.accept(this));
	}

	@Override
	public T visit(ExprList x) throws Err {
		return x.args.stream().map(y -> y.accept(this)).reduce(stop, aggregator);
	}

	@Override
	public T visit(ExprCall x) throws Err {
		if (relevants.contains(x.fun.label)) counting = true;
		T res = x.fun.getBody().accept(this);
		counting = false;
		return res;
	}

	@Override
	public T visit(ExprConstant x) throws Err {
		return stop;
	}

	@Override
	public T visit(ExprITE x) throws Err {
		return aggregator.apply(x.cond.accept(this),aggregator.apply(x.left.accept(this),x.right.accept(this)));
	}

	@Override
	public T visit(ExprLet x) throws Err {
		return x.sub.accept(this);
	}

	@Override
	public T visit(ExprQt x) throws Err {
		T ds = x.decls.stream().map(y -> y.expr.accept(this)).reduce(stop, aggregator);
		return aggregator.apply(ds,x.sub.accept(this));
	}

	@Override
	public T visit(ExprUnary x) throws Err {
		return x.sub.accept(this);
	}

	@Override
	public T visit(ExprVar x) throws Err {
		return stop;
	}

	@Override
	public T visit(Sig x) throws Err {
		return stop;
	}

	@Override
	public T visit(Field x) throws Err {
		return stop;
	}

}
