package pt.haslab.alloy4fun.metrics;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import pt.haslab.alloy4fun.datamodel.A4FDatabase;
import pt.haslab.alloy4fun.datamodel.A4FModel;
import pt.haslab.alloy4fun.graph.Node;
import pt.haslab.alloy4fun.metrics.MetricMethod.GROUPBY;

public class ModelStats {

	/* Id of the root model under analysis. */
	public final String root_id;
	/* Timestamp of the generation of this statistics. */
	public final LocalDateTime timestamp;
	/* Scalar statistics, from rule name to value. */
	private final Map<String, Double> scalarStats = new HashMap<>();
	/* Indexed statistics, from rule to index to value. */
	private final Map<String, Map<Object, Double>> indexedStats = new HashMap<>();
	/* Indexed grouped statistics, from rule to index to group to value. */
	private final Map<String, Map<Object, Map<Object, Double>>> indexedGroupedStats = new HashMap<>();
	/*
	 * Indexed grouped statistics per challenge, from rule to challenge to index to
	 * group to value.
	 */
	private final Map<String, Map<String, Map<Object, Map<Object, Double>>>> challengeIndexedGroupedStats = new HashMap<>();
	/*
	 * The Alloy4Fun database under analysis (assumes all entries from root model)
	 */
	private final A4FDatabase a4f;
	/* The processed root model. */
	public final A4FModel root;
	/* The descriptions of the executed metrics. */
	private final Map<String, String> metric_desc = new HashMap<>();
	
	public ModelStats(String id, A4FDatabase a4f)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, SecurityException {
		this.root_id = id;
		this.timestamp = LocalDateTime.now();
		this.a4f = a4f;
		this.root = a4f.getModel(id);
	}

	/* The creation date of the challenge (i.e., of the root model). */
	public LocalDate getChallengeDate() {
		return root.time.toLocalDate();
	}

	/*-----------------------------------------------------------*/

	/*
	 * Total number of registered server errors found. If re-execution disabled,
	 * will be empty.
	 */
	public int getTotalServerErrors() {
		return getA4f().server_errors().size();
	}

	/*
	 * Total number of solutions for execution entries found. If re-execution
	 * disabled, will be empty.
	 */
	public int getTotalSolutions() {
		return getA4f().solutions().size();
	}

	/*
	 * Total number of entries where an error state was registered, but re-execution
	 * was successful. Should only be != 0 for legacy models. If re-execution
	 * disabled, will be empty.
	 */
	public int getTotalInconsistentRes() {
		return getA4f().inconsistentRes().size();
	}

	/*
	 * Total number of entries where an error message was registered, but
	 * re-execution was successful. Should only be != 0 for legacy models. If
	 * re-execution disabled, will be empty.
	 */
	public int getTotalInconsistentMsg() {
		return getA4f().inconsistentMsg().size();
	}

	/*
	 * Total number of re-executions that timed out. If re-execution disabled, will
	 * be empty.
	 */
	public int getTotalTimeouts() {
		return getA4f().timeouts().size();
	}

	/*-----------------------------------------------------------*/

	/* Retrieve the description of an applied metric. */
	public String getMetricDesc(String metric) {
		return metric_desc.get(metric);
	}

	/*
	 * Processes all metrics given the metric rule annotations of provided methods.
	 */
	public void processMetrics(Method[] methods)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Map<List<Class<?>>, List<Method>> mts = new HashMap<>();
		for (Method method : methods) {
			MetricMethod annos = method.getAnnotation(MetricMethod.class);
			if (annos != null) {
				metric_desc.put(annos.rule(), annos.description());
				List<Class<?>> anns = Arrays.stream(method.getParameterAnnotations()).map(k -> k[0].annotationType())
						.collect(Collectors.toList());
				List<Method> typeMethods = mts.computeIfAbsent(anns, k -> new ArrayList<Method>());
				typeMethods.add(method);
			}
		}
		Map<Method, List<Object[]>> metrics = new HashMap<>();
		for (List<Class<?>> paramTypes : mts.keySet())
			processMetric(metrics, paramTypes, mts.get(paramTypes), new ArrayList<>());

		aggregateMetrics(metrics);
		normalizeIndices();
	}

	/* Normalizes the indices of all statistics, filling missing indices with 0s. */
	private void normalizeIndices() {

		for (String rule : indexedStats.keySet()) {
			List<?> norms = normalizeIndices(indexedStats.get(rule).keySet());
			for (Object idx : norms)
				indexedStats.get(rule).putIfAbsent(idx, (double) 0);
		}

		for (String rule : indexedGroupedStats.keySet()) {
			List<?> norms = normalizeIndices(indexedGroupedStats.get(rule).keySet());
			for (Object idx : norms)
				indexedGroupedStats.get(rule).putIfAbsent(idx, new HashMap<>());
		}

		for (String rule : challengeIndexedGroupedStats.keySet()) {
			for (String chal : challengeIndexedGroupedStats.get(rule).keySet()) {
				List<?> norms = normalizeIndices(challengeIndexedGroupedStats.get(rule).get(chal).keySet());
				for (Object idx : norms)
					challengeIndexedGroupedStats.get(rule).get(chal).putIfAbsent(idx, new HashMap<>());
			}
		}

	}

	/* Normalizes a set of indices, filling missing indices with 0s. */
	private <T> List<T> normalizeIndices(Set<T> idxs) {

		List<T> norm_idsx;
		Iterator<T> it = idxs.iterator();
		T x = it.next();
		if (x instanceof Integer) {
			List<Integer> aux = new ArrayList<>();
			for (Integer i = Collections.min((Set<Integer>) idxs); i <= Collections.max((Set<Integer>) idxs); i++)
				aux.add(i);
			norm_idsx = (List<T>) aux;
		} else if (x instanceof LocalDate) {
			List<LocalDate> aux = new ArrayList<>();
			for (LocalDate i = getChallengeDate(); i.isBefore(LocalDate.now())
					|| i.equals(LocalDate.now()); i = i.plusDays(1))
				aux.add(i);
			norm_idsx = (List<T>) aux;
		} else if (x instanceof String && idxs.contains(getA4f().challengeLabels().get(0))) {
			norm_idsx = (List<T>) getA4f().challengeLabels();
			List<T> aux = new ArrayList<T>(idxs);
			aux.removeAll(norm_idsx);
			norm_idsx.addAll(aux);
		} else {
			norm_idsx = new ArrayList<T>(idxs);
		}

		return norm_idsx;
	}

	/*
	 * Calculates the metrics of a given type of arguments, calculating the
	 * Cartesian product of all possible arguments of that type.
	 */
	// TODO: this will repeat iteration if permutation of identical parameters
	private void processMetric(Map<Method, List<Object[]>> metrics, List<Class<?>> paramType, List<Method> methods,
			List<Object> arguments) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (paramType.isEmpty()) {
			for (Method m : methods) {
				Object[] res = (Object[]) m.invoke(null, arguments.toArray());
				if (res != null)
					metrics.computeIfAbsent(m, k -> new ArrayList<Object[]>()).add(res);
			}
		} else {
			Iterator<?> it = iteratorParam(paramType.get(0));
			while (it.hasNext()) {
				List<Object> extArguments = new ArrayList<>(arguments);
				extArguments.add(it.next());
				List<Class<?>> redParamTypes = paramType.subList(1, paramType.size());
				processMetric(metrics, redParamTypes, methods, extArguments);
			}
		}
	}

	/* Returns a database iterator given a metric rule annotation. */
	private Iterator<?> iteratorParam(Class<?> param) {
		if (param == ForAllSolutions.class)
			return getA4f().solutions().keySet().iterator();
		else if (param == ForAllErrors.class)
			return getA4f().errors().keySet().iterator();
		else if (param == ForAllWarnings.class)
			return getA4f().warnings().keySet().iterator();
		else if (param == ForAllModels.class)
			return getA4f().models().values().iterator();
		else if (param == ForAllExecutions.class)
			return getA4f().executions().values().iterator();
		else if (param == ForAllShares.class)
			return getA4f().shares().values().iterator();
		else if (param == ForAllSessions.class)
			return root.children().iterator();
		else if (param == ForAllCommands.class)
			return getA4f().challengeLabels().iterator();
		else if (param == ForAllInstances.class)
			return getA4f().instances().values().iterator();
		else if (param == ForAllLinks.class)
			return getA4f().links().values().iterator();
		else if (param == A4FDB.class)
			return Arrays.asList(getA4f()).iterator();

		return null;
	}

	private double agg(GROUPBY groupby, List<Double> vals) {
		double res;
		switch (groupby) {
		case COUNT:
			res = vals.stream().count();
			break;
		case SUM:
			res = vals.stream().mapToDouble(i -> i).sum();
			break;
		case AVG:
			res = vals.stream().mapToDouble(i -> i).average().getAsDouble();
			break;
		case MIN:
			res = vals.stream().mapToDouble(i -> i).min().getAsDouble();
			break;
		case MAX:
			res = vals.stream().mapToDouble(i -> i).max().getAsDouble();
			break;
		default:
			res = 0;
			break;
		}
		;
		return res;
	}

	private void aggregateMetrics(Map<Method, List<Object[]>> rawMetrics) {

		for (Method rule : rawMetrics.keySet()) {
			GROUPBY group = rule.getAnnotation(MetricMethod.class).groupby();
			String rulename = rule.getAnnotation(MetricMethod.class).rule();
			if (rawMetrics.get(rule).isEmpty())
				scalarStats.put(rulename, (double) 0);
			else {
				if (rawMetrics.get(rule).get(0).length == 1 && group != GROUPBY.COUNT) {
					List<Double> res = new ArrayList<>();
					for (Object[] i : rawMetrics.get(rule))
						res.add(toDouble(i[0]));
					scalarStats.put(rulename, agg(group, res));
				} else if (rawMetrics.get(rule).get(0).length == 1 && group == GROUPBY.COUNT) {
					Map<Object, Double> res = new TreeMap<>();
					for (Object[] i : rawMetrics.get(rule))
						res.compute(i[0], (x, v) -> (v == null) ? 1 : v + 1);

					if (res.keySet().size() == 1)
						scalarStats.put(rulename, res.values().iterator().next());
					else
						indexedStats.computeIfAbsent(rulename, x -> new TreeMap<Object, Double>()).putAll(res);
				} else if (rawMetrics.get(rule).get(0).length == 2 && group != GROUPBY.COUNT) {
					Map<Object, List<Double>> res = new TreeMap<>();
					for (Object[] i : rawMetrics.get(rule))
						res.computeIfAbsent(i[0], x -> new ArrayList<>()).add((Double) i[1]);

					for (Entry<Object, List<Double>> rs : res.entrySet())
						indexedStats.computeIfAbsent(rulename, x -> new TreeMap<Object, Double>()).put(rs.getKey(),
								agg(group, rs.getValue()));

				} else if (rawMetrics.get(rule).get(0).length == 2 && group == GROUPBY.COUNT) {
					Map<Object, Map<Object, Double>> res = new TreeMap<>();
					for (Object[] i : rawMetrics.get(rule)) {
						Map<Object, Double> res2 = res.computeIfAbsent(i[0], x -> new TreeMap<>());
						res2.compute(i[1].toString(), (x, v) -> (v == null) ? 1 : v + 1);
					}
					indexedGroupedStats.put(rulename, res);

				} else if (rawMetrics.get(rule).get(0).length == 3 && group == GROUPBY.COUNT) {
					Map<String, Map<Object, Map<Object, Double>>> res3 = new TreeMap<>();
					for (Object[] i : rawMetrics.get(rule)) {
						Map<Object, Map<Object, Double>> res2 = res3.computeIfAbsent(i[0].toString(),
								x -> new TreeMap<>());
						Map<Object, Double> res1 = res2.computeIfAbsent(i[1], x -> new TreeMap<>());
						res1.compute(i[2], (x, v) -> (v == null) ? 1 : v + 1);
					}
					challengeIndexedGroupedStats.put(rulename, res3);

				} else {
					System.out.println("* Unsupported rule shape: " + rulename);
				}
			}
		}
	}

	private Double toDouble(Object object) {
		if (object instanceof Double)
			return (Double) object;
		if (object instanceof Integer)
			return ((Integer) object).doubleValue();
		throw new RuntimeException("Can't convert value to double.");
	}

	public Set<String> scalarMetrics() {
		return scalarStats.keySet();
	}

	public Set<String> indexedMetrics() {
		return indexedStats.keySet();
	}

	public Set<String> indexedGroupedMetrics() {
		return indexedGroupedStats.keySet();
	}

	public Set<String> challengeIndexedGroupedMetrics() {
		return challengeIndexedGroupedStats.keySet();
	}

	public Double scalarStats(String metric_name) {
		return scalarStats.get(metric_name);
	}

	public Map<Object, Double> indexedStats(String metric_name) {
		return indexedStats.get(metric_name);
	}

	public Map<Object, Map<Object, Double>> indexedGroupedStats(String metric_name) {
		return indexedGroupedStats.get(metric_name);
	}

	public Map<String, Map<Object, Map<Object, Double>>> challengeIndexedGroupedStats(String metric_name) {
		return challengeIndexedGroupedStats.get(metric_name);
	}

	public Map<String, Map<String, Node>> graphNodes() {
		return getA4f().nodes;
	}

	public Map<String, Map<String, Entry<String, Integer>>> graphEdges() {
		return getA4f().edges;
	}

	public A4FDatabase getA4f() {
		return a4f;
	}
}
