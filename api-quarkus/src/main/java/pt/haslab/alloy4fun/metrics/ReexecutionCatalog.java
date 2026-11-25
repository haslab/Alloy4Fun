package pt.haslab.alloy4fun.metrics;

import edu.mit.csail.sdg.translator.A4Solution;

@MetricSuite(description = "Simple online metrics")
public class ReexecutionCatalog extends BasicCatalog {

	@MetricMethod(rule = "Total solutions", description = "The total number of solutions found.", group = 4)
	public static Object[] totalSols(@ForAllSolutions A4Solution mdl) {
		return new Object[] { "sol" };
	}

}
