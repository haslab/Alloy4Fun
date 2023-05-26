package pt.haslab.alloy4fun.metrics;

import java.util.Set;
import java.time.LocalDate;
import java.util.List;

import static pt.haslab.alloy4fun.datamodel.A4FExecution.RESULT.*;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.ast.ExprQt;
import pt.haslab.alloy4fun.datamodel.A4FModel;
import pt.haslab.alloy4fun.datamodel.A4FNavigation;
import pt.haslab.alloy4fun.datamodel.A4FShare;
import pt.haslab.alloy4fun.datamodel.A4FDatabase;
import pt.haslab.alloy4fun.datamodel.A4FExecution;
import pt.haslab.alloy4fun.metrics.MetricMethod.GROUPBY;
import pt.haslab.alloy4fun.metrics.utils.AggregateVisitor;

@MetricSuite(description = "Example metric catalog")
public class BasicCatalog {

	@MetricMethod(rule = "Total sessions", description = "The number of sessions.", group = 0)
	public static Object[] totalSessions(@ForAllSessions A4FModel ses) {
		return new Object[] { "session" };
	}
	
	@MetricMethod(rule = "Longest session", groupby = GROUPBY.MAX, description = "The length of the longest session.", group = 1)
	public static Object[] longestSession(@A4FDB A4FDatabase db, @ForAllSessions A4FModel ses) {
		SessionMetrics ms = db.sessionMetrics(ses);
		return new Object[] { ms.depth };
	}
	
	@MetricMethod(rule = "Average session", groupby = GROUPBY.AVG, description = "The average session length.", group = 1)
	public static Object[] avgSession(@A4FDB A4FDatabase db, @ForAllSessions A4FModel ses) {
		SessionMetrics ms = db.sessionMetrics(ses);
		return new Object[] { ms.depth };
	}
	
	@MetricMethod(rule = "Sessions all solved", description = "Number of sessions where all challenges were solved at some point.", group = 3)
	public static Object[] allUnsatSession(@A4FDB A4FDatabase db, @ForAllSessions A4FModel ses) {
		SessionMetrics ms = db.sessionMetrics(ses);
		List<String> lbls = db.challengeLabels();
		lbls.removeAll(ms.unsat_cmds.elems());
		
		if (lbls.isEmpty())
			return new Object[] { "all" };
		else
			return null;
	}
	
	@MetricMethod(rule = "Average length all solved", groupby = GROUPBY.AVG, description = "Average length of sessions where all challenges were solved at some point.", group = 3)
	public static Object[] avgUnsatSession(@A4FDB A4FDatabase db, @ForAllSessions A4FModel ses) {
		SessionMetrics ms = db.sessionMetrics(ses);
		List<String> lbls = db.challengeLabels();
		lbls.removeAll(ms.unsat_cmds.elems());
		
		if (lbls.isEmpty())
			return new Object[] { ms.depth };
		else
			return null;
	}

	@MetricMethod(rule = "Average % unsatisfiable", groupby = GROUPBY.AVG, description = "The average sat/unsat ratio within sessions.", group = 2)
	public static Object[] rationCorrectSession(@A4FDB A4FDatabase db, @ForAllSessions A4FModel ses) {
		SessionMetrics ms = db.sessionMetrics(ses);
		if (ms.numSatExecs() + ms.numUnsatExecs() == 0)
			return null;
		return new Object[] { (Double.valueOf(ms.numUnsatExecs()) / (ms.numSatExecs() + ms.numUnsatExecs())) };
	}
	
	/* ------------- */


	@MetricMethod(rule = "Total executions", description = "The total number of execution entries.", group = 4)
	public static Object[] totalExecs(@ForAllExecutions A4FExecution mdl) {
		return new Object[] { "exec" };
	}

	@MetricMethod(rule = "Challenge executions", description = "The total number of (non-errored) challenge executions.", group = 5)
	public static Object[] challengeExecs(@A4FDB A4FDatabase db, @ForAllExecutions A4FExecution mdl) {
		List<String> lbls = db.challengeLabels();
		if (lbls.contains(mdl.cmd_name))
			return new Object[] { "all" };
		else
			return null;
	}
	
	@MetricMethod(rule = "Sat executions", description = "The number of satisfiable executions.", group = 7)
	public static Object[] correctExecs(@ForAllExecutions A4FExecution exe) {
		if (exe.result() == SAT)
			return new Object[] { exe.result() };
		else
			return null;
	}

	@MetricMethod(rule = "Unsat executions", description = "The number of unsatisfiable executions.", group = 7)
	public static Object[] incorrectExecs(@ForAllExecutions A4FExecution exe) {
		if (exe.result() == UNSAT)
			return new Object[] { exe.result() };
		else
			return null;
	}
	
	@MetricMethod(rule = "Run executions", description = "The number of (non-errored) executions from run commands.", group = 8)
	public static Object[] runExecs(@ForAllExecutions A4FExecution exe) {
		if (exe.cmd_check != null && !exe.cmd_check)
			return new Object[] { exe.result() };
		else
			return null;
	}
	
	@MetricMethod(rule = "Error executions", description = "The number of errored executions.", group = 9)
	public static Object[] erroredExecs(@ForAllExecutions A4FExecution exe) {
		if (exe.result() == ERROR)
			return new Object[] { exe.result() };
		else
			return null;
	}
	
	@MetricMethod(rule = "Compile-time errors", description = "The number of executed models that with compilation-time errors.", group = 10)
	public static Object[] compileErrors(@A4FDB A4FDatabase db, @ForAllErrors Err err) {
		A4FModel entry = db.models().get(db.errors().get(err));
		if (entry instanceof A4FExecution && ((A4FExecution) entry).command() == null)
			return new Object[] { "exec" };
		else
			return null;
	}

	@MetricMethod(rule = "Run-time errors", description = "The number of executed models with run-time errors.", group = 10)
	public static Object[] runErrors(@A4FDB A4FDatabase db, @ForAllErrors Err err) {
		A4FModel entry = db.models().get(db.errors().get(err));
		if (entry instanceof A4FExecution && ((A4FExecution) entry).command() != null)
			return new Object[] { "exec" };
		else
			return null;
	}

	@MetricMethod(rule = "Total warnings", description = "Total number of warnings (a model may have multiple warnings).", group = 11)
	public static Object[] correctWarning(@A4FDB A4FDatabase db) {
		return new Object[] { db.warnings().size() };
	}
	
	@MetricMethod(rule = "Unsat executions w/ warnings", description = "The number of unsatisfiable executions that produced warning messages.", group = 12)
	public static Object[] correctWarning(@ForAllExecutions A4FExecution exe) {
		if (exe.result() != UNSAT || exe.wns.isEmpty())
			return null;
		return new Object[] { exe.result() };
	}

	@MetricMethod(rule = "Sat executions w/ warnings", description = "The number of satisfiable executions that produced warning messages.", group = 12)
	public static Object[] incorrectWarning(@ForAllExecutions A4FExecution exe) {
		if (exe.result() != SAT || exe.wns.isEmpty())
			return null;
		return new Object[] { exe.result() };
	}

	@MetricMethod(rule = "Error executions w/ warnings", description = "The number of errored executions that produced warning messages.", group = 12)
	public static Object[] erroredWarning(@ForAllExecutions A4FExecution exe) {
		if (exe.result() != ERROR || exe.wns.isEmpty())
			return null;
		return new Object[] { exe.result() };
	}
	
	/* ------------- */

	@MetricMethod(rule = "Total shares", description = "The total number of share entries.", group = 13)
	public static Object[] totalShares(@ForAllShares A4FShare mdl) {
		return new Object[] { "share" };
	}
	
	@MetricMethod(rule = "Shared models", description = "The number of models shares (excluding instances).", group = 14)
	public static Object[] sharedModels(@ForAllShares A4FShare sha) {
		if (!sha.instance_share)
			return new Object[] { sha.instance_share };
		else
			return null;
	}

	@MetricMethod(rule = "Shared instances", description = "The number of instance shares.", group = 14)
	public static Object[] sharedInstances(@ForAllShares A4FShare sha) {
		if (sha.instance_share)
			return new Object[] { sha.instance_share };
		else
			return null;
	}

	@MetricMethod(rule = "Number of shared sessions", description = "The number of sessions for which an entry has been shared.", group = 12)
	public static Object[] sharedSessions(@A4FDB A4FDatabase db, @ForAllSessions A4FModel ses) {
		SessionMetrics ms = db.sessionMetrics(ses);
		if (ms.shared_mdls > 0 || ms.shared_insts > 0)
			return new Object[] { "shared" };
		else
			return null;
	}
	
	@MetricMethod(rule = "Number of iterations", groupby = GROUPBY.SUM, description = "The total number of navigation operations.", group = 15)
	public static Object[] totalNav(@A4FDB A4FDatabase db, @ForAllExecutions A4FExecution ses) {
		List<A4FNavigation> ms = db.getNavigation(ses.id);
		return new Object[] { ms.size() };
	}

	@MetricMethod(rule = "Average iterations", groupby = GROUPBY.AVG, description = "Average number of navigation operations by satisfiable command.", group = 15)
	public static Object[] avgNav(@A4FDB A4FDatabase db, @ForAllExecutions A4FExecution ses) {
		List<A4FNavigation> ms = db.getNavigation(ses.id);
		if (ses.result() == SAT)
			return new Object[] { ms.size() };
		else
			return null;
	}

	/* ------------- */

	@MetricMethod(rule = "Sessions by length", description = "The number of sessions with a certain length.", group = 0)
	public static Integer[] sessionLength(@A4FDB A4FDatabase db, @ForAllSessions A4FModel ses) {
		SessionMetrics ms = db.sessionMetrics(ses);
		return new Integer[] { ms.depth };
	}

	@MetricMethod(rule = "Sessions by # solved challenges", description = "The number of sessions with a certain number solved challenges.", group = 0)
	public static Object[] sessionChallenge(@A4FDB A4FDatabase db, @ForAllSessions A4FModel ses) {
		Set<String> us = db.sessionMetrics(ses).unsatCommands();
		List<String> lbls = db.challengeLabels();
		lbls.retainAll(us);
		return new Object[] { lbls.size() };
	}

	@MetricMethod(rule = "Errors by type", description = "The number of errors by normalized message.", group = 1)
	public static Object[] errorMessages(@ForAllErrors Err err) {
		return new Object[] { MetricRunner.normUpMessages(err.msg) };
	}

	@MetricMethod(rule = "Warnings by type", description = "The number of warnings by normalized message.", group = 1)
	public static Object[] warningMessages(@ForAllWarnings ErrorWarning err) {
		return new Object[] { MetricRunner.normUpMessages(err.msg) };
	}
	
	/* ------------- */
	
	@MetricMethod(rule = "Entries over time", description = "The number of model entries by date, classified by type and result.", group = 0)
	public static Object[] resultsTime(@ForAllModels A4FModel entry) {
		LocalDate date = entry.time.toLocalDate();
		if (entry instanceof A4FExecution)
			return new Object[] { date, ((A4FExecution) entry).result() };
		else
			return new Object[] { date, "SHARE" };
	}

	@MetricMethod(rule = "Execution results by command", description = "The number of executions by command, classified by result. Errored executions are not considered.", group = 2)
	public static Object[] execsChallenge(@ForAllExecutions A4FExecution entry) {
		if (entry.result() == ERROR)
			return null;
		return new Object[] { entry.cmd_name, entry.result() };
	}

	@MetricMethod(rule = "Session results by command", description = "The number of sessions in which a certain command has been correctly solved.", group = 1)
	public static String[] sessionChallenge(@A4FDB A4FDatabase db, @ForAllSessions A4FModel ses, @ForAllCommands String chl) {
		SessionMetrics ms = db.sessionMetrics(ses);
		return new String[] { chl, ms.unsat_cmds.count(chl) > 0 ? "solved" : "unsolved" };
	}
	
	/* ------------- */

	@MetricMethod(rule = "Size in 10s of nodes", description = "The number of executions, for a particular challenge, by the size of a command AST in tens of nodes, grouped by result. Considers the complete model (not just the challenge predicate).")
	public static Object[] nodeSize10(@A4FDB A4FDatabase db, @ForAllExecutions A4FExecution exe) {
		if (!db.challengeLabels().contains(exe.cmd_name))
			return null;
		AggregateVisitor<Integer> qnt = new AggregateVisitor<Integer>((k, l) -> k + l + 1, 1,
				db.challengPreds()) {
		};
		return new Object[] { exe.cmd_name, exe.command().formula.accept(qnt) / 10, exe.result() };
	}
	
	@MetricMethod(rule = "Number of quantified variables", description = "The number of executions, for a particular challenge, by the number of occurring first-order quantifiers, grouped by result. Each declared variable counts once. Considers the complete model (not just the challenge predicate).")
	public static Object[] numQuants(@A4FDB A4FDatabase db, @ForAllExecutions A4FExecution entry) {
		if (!db.challengeLabels().contains(entry.cmd_name))
			return null;
		AggregateVisitor<Integer> qnt = new AggregateVisitor<Integer>(Integer::sum, 0, db.challengPreds()) {

			@Override
			public Integer visit(ExprQt x) {
				return (counting?x.count():0) + super.visit(x);
			}

		};

		return new Object[] { entry.cmd_name, entry.command().formula.accept(qnt), entry.result() };

	}
}
