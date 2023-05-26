package pt.haslab.alloy4fun.metrics;

import java.util.Arrays;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

import static pt.haslab.alloy4fun.datamodel.A4FExecution.RESULT.*;

import java.util.ArrayList;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Decl;
import edu.mit.csail.sdg.ast.ExprBinary;
import edu.mit.csail.sdg.ast.ExprCall;
import edu.mit.csail.sdg.ast.ExprConstant;
import edu.mit.csail.sdg.ast.ExprHasName;
import edu.mit.csail.sdg.ast.ExprITE;
import edu.mit.csail.sdg.ast.ExprLet;
import edu.mit.csail.sdg.ast.ExprList;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprQt;
import edu.mit.csail.sdg.ast.ExprQt.Op;
import edu.mit.csail.sdg.ast.VisitReturn;
import edu.mit.csail.sdg.ast.ExprUnary;
import edu.mit.csail.sdg.ast.ExprVar;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.ast.Browsable;
import edu.mit.csail.sdg.ast.Command;
import pt.haslab.alloy4fun.datamodel.A4FModel;
import pt.haslab.alloy4fun.datamodel.A4FShare;
import pt.haslab.alloy4fun.datamodel.A4FDatabase;
import pt.haslab.alloy4fun.datamodel.A4FExecution;
import pt.haslab.alloy4fun.metrics.MetricMethod.GROUPBY;
import pt.haslab.alloy4fun.metrics.utils.AggregateVisitor;

@MetricSuite(description = "Example metric catalog")
public class MetricCatalog extends BasicCatalog {

	// scalar metrics

	@MetricMethod(rule = "Shared errored models", description = "Counts the number of shared models that had errors.")
	public static Object[] sharedErrors(@A4FDB A4FDatabase db, @ForAllErrors Err err) {

		A4FModel entry = db.models().get(db.errors().get(err));
		if (entry instanceof A4FShare)
			return new Object[] { "shared" };
		else
			return null;
	}

	@MetricMethod(rule = "Shares with warnings", description = "Counts the number of incorrect executions that produced warning messages.")
	public static Object[] shareWarning(@ForAllShares A4FShare sha) {
		if (sha.wns.isEmpty())
			return null;
		return new Object[] { "share" };
	}
	
	@MetricMethod(rule = "Last steps", groupby = GROUPBY.MAX, description = "The size of the longest session.")
	public static Object[] lastStepsSession(@A4FDB A4FDatabase db, @ForAllSessions A4FModel ses) {
    	lastSteps(db, ses.id,ses,new HashMap<>());
		SessionMetrics ms = db.sessionMetrics(ses);
		return new Object[] { ms.depth };
	}
	
	static void lastSteps(A4FDatabase db, String ses, A4FModel m, Map<String,A4FExecution> lastunsats) {
		if (m instanceof A4FExecution) {
			String ex = ((A4FExecution) m).cmd_name;
            if (((A4FExecution) m).result() == UNSAT) {
            	
            	String rep = "%s\t%s\t%s\t%s\t%s\t%s\t%s";
            	A4FExecution prev = lastunsats.get(ex);
            	String pre_res;
        		VisitReturn<Map<String,Expr>> v = new VisitReturn<Map<String,Expr>>() {
					@Override
					public Map<String,Expr> visit(ExprCall x) throws Err {
						Map<String,Expr> ans = new HashMap<>();
						ans.put(x.fun.label,x.fun.getBody());
						ans.putAll(x.fun.getBody().accept(this));
        		    	return ans;
        		    }

					@Override
					public Map<String,Expr> visit(ExprBinary x) throws Err {
						Map<String,Expr> ans = new HashMap<>();
				        ans.putAll(x.right.accept(this));
				        ans.putAll(x.left.accept(this));
				        return ans;							
					}

					@Override
					public Map<String,Expr> visit(ExprList x) throws Err {
						Map<String,Expr> ans = new HashMap<>();
						for (Browsable y : x.getSubnodes()) {
							ans.putAll(((Expr) y).accept(this));
						}
						return ans;
					}

					@Override
					public Map<String,Expr> visit(ExprConstant x) throws Err {
						Map<String,Expr> ans = new HashMap<>();
        		    	return ans;
					}

					@Override
					public Map<String,Expr> visit(ExprITE x) throws Err {
						Map<String,Expr> ans = new HashMap<>();
						ans.putAll(x.cond.accept(this));
						ans.putAll(x.left.accept(this));
						ans.putAll(x.right.accept(this));
        		    	return ans;
					}

					@Override
					public Map<String,Expr> visit(ExprLet x) throws Err {
						Map<String,Expr> ans = new HashMap<>();
						ans.putAll(x.expr.accept(this));
						ans.putAll(x.sub.accept(this));
        		    	return ans;
					}

					@Override
					public Map<String,Expr> visit(ExprQt x) throws Err {
						Map<String,Expr> ans = new HashMap<>();
						for (Decl d : x.decls) {
							ans.putAll(d.expr.accept(this));
						}
						ans.putAll(x.sub.accept(this));
        		    	return ans;
					}

					@Override
					public Map<String,Expr> visit(ExprUnary x) throws Err {
						Map<String,Expr> ans = new HashMap<>();
						ans.putAll(x.sub.accept(this));
        		    	return ans;
					}

					@Override
					public Map<String,Expr> visit(ExprVar x) throws Err {
						Map<String,Expr> ans = new HashMap<>();
        		    	return ans;
					}

					@Override
					public Map<String,Expr> visit(Sig x) throws Err {
						Map<String,Expr> ans = new HashMap<>();
        		    	return ans;
					}

					@Override
					public Map<String,Expr> visit(Field x) throws Err {
						Map<String,Expr> ans = new HashMap<>();
        		    	return ans;
					}
        		};
        		Map<String,Expr> post = ((A4FExecution) m).command().formula.accept(v);
        		Map<String,Expr> pre = null;

        		if (prev != null) {
	        		pre_res = prev.result().toString();
	        		pre = prev.command().formula.accept(v);
	        		
	            } else {
	            	pre_res = "FIRST";
	            	CompModule or = CompUtil.parseEverything_fromString(new A4Reporter(),db.root().code);
	        		Command cmd = null;
	            	for (Command c : or.getAllCommands())
	        			if (c.label.equals(ex))
	        				cmd = c;
	        		pre = cmd.formula.accept(v);

            	}
        		List<Expr> diffs1 = new ArrayList<Expr>();
        		List<Expr> diffs2 = new ArrayList<Expr>();
        		for (String s : pre.keySet()) {
        			if (post.get(s) == null)
        				diffs1.add(pre.get(s));
        			else if (!pre.get(s).toString().equals(post.get(s).toString())) {
        				diffs1.add(pre.get(s));
        				diffs2.add(post.get(s));
        			}
        		}
        		if (diffs1.size() == 1)
        			System.out.println(String.format(rep,m.root_entry,ses,m.id,ex,pre_res,diffs1.get(0),diffs2.get(0)));
        		else if (diffs1.size() > 1)
        			System.out.println(String.format(rep,m.root_entry,ses,m.id,ex,pre_res,"Several relevant predicates changed.",""));
        		else
        			System.out.println(String.format(rep,m.root_entry,ses,m.id,ex,pre_res,"No relevant predicates changed.",""));
            }
            if (((A4FExecution) m).command() == null) {
            	// Errors are ignored, command could not be identified; so diffs are from one well-typed to another well-typed
            	if (((A4FExecution) m).result() != ERROR)
					System.out.println(m.id+": empty command?");
            }
            else 
            	lastunsats.put(ex,(A4FExecution) m);
		}
		for (A4FModel m1 : m.children() ) {
			lastSteps(db, ses,m1,lastunsats);
		}
		if (m instanceof A4FExecution) {
			String ex = ((A4FExecution) m).cmd_name;
			lastunsats.remove(ex);
		}
	}

	// overall metrics


	@MetricMethod(rule = "Models by number of warnings", description = "The number of entries with a certain number of warning messages.")
	public static Object[] numWarningsByEntry(@ForAllModels A4FModel entry) {
		return new Object[] { entry.wns.size() };
	}

	// overall metrics classified
	


	// metrics by sub-challenge classified
	@MetricMethod(rule = "Max nested quantifier level", description = "The number of executions, for a particular challenge, by the maximum nested level of first-order quantifiers, grouped by result. Each declaration (possibly with multiple variables) counts once.")
	public static Object[] quantifierDepth(@A4FDB A4FDatabase db, @ForAllExecutions A4FExecution entry) {
		if (!db.challengeLabels().contains(entry.cmd_name))
			return null;
		AggregateVisitor<Integer> qnt = new AggregateVisitor<Integer>(Integer::max, 0, db.challengPreds()) {

			@Override
			public Integer visit(ExprQt x) throws Err {
				return (counting?x.decls.size():0) + super.visit(x);
			}

		};
		try {
			return new Object[] { entry.cmd_name, entry.command().formula.accept(qnt), entry.result() };
		} catch (Exception e) {
			throw e;
		}
	}



	@MetricMethod(rule = "Number of lone/one quantifiers", description = "The number of executions, for a particular challenge, by the number of occurring lone/one quantifiers, grouped by result. Each declared variable is counts once.")
	public static Object[] numLOneQuants(@A4FDB A4FDatabase db, @ForAllExecutions A4FExecution exe) {
		if (!db.challengeLabels().contains(exe.cmd_name))
			return null;
		AggregateVisitor<Integer> qnt = new AggregateVisitor<Integer>(Integer::sum, 0, db.challengPreds()) {

			@Override
			public Integer visit(ExprQt exp) throws Err {
				return (counting&&(exp.op==Op.LONE||exp.op==Op.ONE)?exp.count():0) + super.visit(exp);
			}

		};

		return new Object[] { exe.cmd_name, exe.command().formula.accept(qnt), exe.result() };

	}

	@MetricMethod(rule = "Number multiplicity arrows", description = "The number of executions, for a particular challenge, by the number of occurring multiplicity arrow tests, grouped by result.")
	public static Object[] numMultArrows(@A4FDB A4FDatabase db, @ForAllExecutions A4FExecution entry) {
		if (!db.challengeLabels().contains(entry.cmd_name))
			return null;
		AggregateVisitor<Integer> qnt = new AggregateVisitor<Integer>(Integer::sum, 0, db.challengPreds()) {

			@Override
			public Integer visit(ExprBinary x) {
				int dlt = 0;
				switch (x.op) {
				case ANY_ARROW_LONE:
				case ANY_ARROW_ONE:
				case ANY_ARROW_SOME:
				case LONE_ARROW_ANY:
				case LONE_ARROW_LONE:
				case LONE_ARROW_ONE:
				case ONE_ARROW_ANY:
				case ONE_ARROW_LONE:
				case ONE_ARROW_ONE:
				case ONE_ARROW_SOME:
				case SOME_ARROW_ANY:
				case SOME_ARROW_LONE:
				case LONE_ARROW_SOME:
				case SOME_ARROW_ONE:
				case SOME_ARROW_SOME:
					dlt = 1;
				default:
					break;
				}
				return (counting?dlt:0) + super.visit(x);
			}

		};

		return new Object[] { entry.cmd_name, entry.command().formula.accept(qnt), entry.result() };

	}

	@MetricMethod(rule = "Number of disj quantifications", description = "The number of executions, for a particular challenge, by the number of occurring disjoint first-order quantifications, grouped by result.")
	public static Object[] numDisjQuants(@A4FDB A4FDatabase db, @ForAllExecutions A4FExecution entry) {
		if (!db.challengeLabels().contains(entry.cmd_name))
			return null;
		AggregateVisitor<Integer> qnt = new AggregateVisitor<Integer>(Integer::sum, 0, db.challengPreds()) {

			@Override
			public Integer visit(ExprQt x) throws Err {
				int qn = 0;
				for (Decl d : x.decls)
					if (d.disjoint != null)
						qn++;
				return (counting?qn:0) + super.visit(x);
			}

		};

		return new Object[] { entry.cmd_name, entry.command().formula.accept(qnt), entry.result() };
	}

	@MetricMethod(rule = "Max nested temporal operator level", description = "The number of executions, for a particular challenge, by the maximum nested level temporal operators, grouped by result.")
	public static Object[] nestedTemporalLevel(@A4FDB A4FDatabase db, @ForAllExecutions A4FExecution entry) {
		if (!db.challengeLabels().contains(entry.cmd_name))
			return null;
		AggregateVisitor<Integer> qnt = new AggregateVisitor<Integer>(Integer::max, 0, db.challengPreds()) {

			@Override
			public Integer visit(ExprBinary x) throws Err {
				int dlt = 0;
				if (Arrays.asList(ExprBinary.Op.SINCE, ExprBinary.Op.UNTIL, ExprBinary.Op.RELEASES,
						ExprBinary.Op.TRIGGERED).contains(x.op))
					dlt = 1;
				return (counting?dlt:0) + super.visit(x);
			}

			@Override
			public Integer visit(ExprUnary x) throws Err {
				int dlt = 0;
				if (Arrays.asList(ExprUnary.Op.AFTER, ExprUnary.Op.ALWAYS, ExprUnary.Op.EVENTUALLY, ExprUnary.Op.BEFORE,
						ExprUnary.Op.HISTORICALLY, ExprUnary.Op.ONCE, ExprUnary.Op.PRIME).contains(x.op))
					dlt = 1;
				return (counting?dlt:0) + super.visit(x);
			}

		};

		return new Object[] { entry.cmd_name, entry.command().formula.accept(qnt), entry.result() };

	}

	@MetricMethod(rule = "Number of binary temporal operators", description = "The number of executions, for a particular challenge, by the number of occurring binary temporal operators, grouped by result..")
	public static Object[] numBinaryTemp(@A4FDB A4FDatabase db, @ForAllExecutions A4FExecution entry) {
		if (!db.challengeLabels().contains(entry.cmd_name))
			return null;
		AggregateVisitor<Integer> qnt = new AggregateVisitor<Integer>(Integer::sum, 0, db.challengPreds()) {

			@Override
			public Integer visit(ExprBinary x) throws Err {
				int dlt = 0;
				if (Arrays.asList(ExprBinary.Op.SINCE, ExprBinary.Op.UNTIL, ExprBinary.Op.RELEASES,
						ExprBinary.Op.TRIGGERED).contains(x.op))
					dlt = 1;
				return (counting?dlt:0) + super.visit(x);
			}

		};

		return new Object[] { entry.cmd_name, entry.command().formula.accept(qnt), entry.result() };

	}

	@MetricMethod(rule = "Number of inverted navigation", description = "The number of executions, for a particular challenge, by the number of occurring expressions of the shape \"r.x\".")
	public static Object[] invertedNav(@A4FDB A4FDatabase db, @ForAllExecutions A4FExecution exe) {
		if (!db.challengeLabels().contains(exe.cmd_name))
			return null;
		AggregateVisitor<Integer> qnt = new AggregateVisitor<Integer>(Integer::sum, 0, db.challengPreds()) {

			@Override
			public Integer visit(ExprBinary x) throws Err {
				int dlt = 0;
				if (x.op == ExprBinary.Op.JOIN)
					if (x.right.deNOP() instanceof ExprVar && x.left.deNOP() instanceof ExprHasName)
						dlt = 1;
				return (counting?dlt:0) + super.visit(x);
			}

		};

		return new Object[] { exe.cmd_name, exe.command().formula.accept(qnt), exe.result() };
	}

	@MetricMethod(rule = "Number of reversed relation navigation", description = "The number of executions, for a particular challenge, by the number of occurring expressions of the shape \"x.~r\".")
	public static Object[] reverseNav(@A4FDB A4FDatabase db, @ForAllExecutions A4FExecution exe) {
		if (!db.challengeLabels().contains(exe.cmd_name))
			return null;
		AggregateVisitor<Integer> qnt = new AggregateVisitor<Integer>(Integer::sum, 0, db.challengPreds()) {

			@Override
			public Integer visit(ExprBinary x) throws Err {
				int dlt = 0;
				if (x.op == ExprBinary.Op.JOIN)
					if (x.left.deNOP() instanceof ExprVar && x.right.deNOP() instanceof ExprUnary
							&& ((ExprUnary) x.right.deNOP()).op == ExprUnary.Op.TRANSPOSE)
						dlt = 1;
				return (counting?dlt:0) + super.visit(x);
			}

		};

		return new Object[] { exe.cmd_name, exe.command().formula.accept(qnt), exe.result() };
	}



}
