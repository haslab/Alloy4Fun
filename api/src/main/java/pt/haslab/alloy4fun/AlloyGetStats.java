package pt.haslab.alloy4fun;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.csail.sdg.alloy4.Err;
import pt.haslab.alloy4fun.graph.Node;
import pt.haslab.alloy4fun.metrics.MetricMethod;
import pt.haslab.alloy4fun.metrics.MetricSuite;
import pt.haslab.alloy4fun.metrics.ModelStats;
import pt.haslab.alloy4fun.metrics.OnlineCatalog;
import pt.haslab.alloy4fun.utils.StatsRequest;

@Path("/getStats")
public class AlloyGetStats {

	private static Logger LOGGER = LoggerFactory.getLogger(AlloyGetStats.class);
	private String catalog_name;
	
	@POST
	@Produces("text/json")
	public Response doGet(String body) throws IOException, Err, IllegalAccessException, IllegalArgumentException, InvocationTargetException, SecurityException, InterruptedException, ExecutionException {
		StatsRequest req = parseJSON(body);
		LOGGER.info("Received a stats request for session: "+req.model);

		
		Class<?> catalog = OnlineCatalog.class;
		MetricSuite suite = catalog.getAnnotation(MetricSuite.class);
		if (suite == null)
			throw new IllegalArgumentException("Invalid catalog annotations.");
		catalog_name = suite.description().isEmpty()?catalog.getName():suite.description();
		
		ModelStats stats;
		try {
			stats = StatsManager.getStats(req.model,catalog);
		} catch (TimeoutException e) {
			LOGGER.info("Timed out.");
			return Response.status(500	, "Stats calculation timed out").build();
		}

		LOGGER.info("Responding with solutions.");
		return Response.ok(statsToJson(stats)).build();
	}

	private String statsToJson(ModelStats stats) {

		JsonObjectBuilder statsJSON = Json.createObjectBuilder();

		statsJSON.add("model", stats.root_id);
		statsJSON.add("time", stats.timestamp.toString());
		statsJSON.add("name", stats.getA4f().getModule_name());
		statsJSON.add("catalog", catalog_name);
		statsJSON.add("nchallenges", stats.getA4f().challengeLabels().size());

		JsonArrayBuilder metricArray = Json.createArrayBuilder();

		Method[] methods = OnlineCatalog.class.getMethods();
        Arrays.sort(methods, new Comparator<Method>() {
            @Override
            public int compare(Method o1, Method o2) {
            	MetricMethod or1 = o1.getAnnotation(MetricMethod.class);
            	MetricMethod or2 = o2.getAnnotation(MetricMethod.class);
                // nulls last
                if (or1 != null && or2 != null) {
                    return or1.group() - or2.group();
                } else
                if (or1 != null && or2 == null) {
                    return -1;
                } else
                if (or1 == null && or2 != null) {
                    return 1;
                }
                return o1.getName().compareTo(o2.getName());
            }
        });
		
		for (Method rule : methods) {
			if (rule.getAnnotation(MetricMethod.class) != null) {
				String rulename = rule.getAnnotation(MetricMethod.class).rule();
				if (stats.scalarStats(rulename) != null) {
					JsonObjectBuilder metricDict = Json.createObjectBuilder();
					metricDict.add("name", rulename);
					metricDict.add("desc", stats.getMetricDesc(rulename));
					metricDict.add("value", stats.scalarStats(rulename));
					metricArray.add(metricDict);
				}
			}
		}
		
		statsJSON.add("scalars", metricArray);

		metricArray = Json.createArrayBuilder();
		
		for (Method rule : methods) {
			if (rule.getAnnotation(MetricMethod.class) != null) {
				String rulename = rule.getAnnotation(MetricMethod.class).rule();
				if (stats.indexedStats(rulename) != null) {
					JsonArrayBuilder valsJSON = Json.createArrayBuilder();
					JsonArrayBuilder idxsJSON = Json.createArrayBuilder();
					JsonObjectBuilder metricDict = Json.createObjectBuilder();
					for (Entry<Object, Double> j : stats.indexedStats(rulename).entrySet()) {
						idxsJSON.add(j.getKey().toString());
						valsJSON.add(j.getValue());
					}
					metricDict.add("name", rulename);
					metricDict.add("desc", stats.getMetricDesc(rulename));
					metricDict.add("indices", idxsJSON);
					metricDict.add("values", valsJSON);
					metricArray.add(metricDict);
				}
			}
		}
		statsJSON.add("bars", metricArray);

		metricArray = Json.createArrayBuilder();
		
		for (Method rule : methods) {
			if (rule.getAnnotation(MetricMethod.class) != null) {
				String rulename = rule.getAnnotation(MetricMethod.class).rule();
				if (stats.indexedGroupedStats(rulename) != null) {
					JsonArrayBuilder idxsJSON = Json.createArrayBuilder();
					JsonObjectBuilder metricDict = Json.createObjectBuilder();
					Map<Object,JsonArrayBuilder> series = new HashMap<Object,JsonArrayBuilder>();
					Set<Object> all_series = stats.indexedGroupedStats(rulename).values().stream()
									.map(x -> x.keySet())
									.flatMap(x -> x.stream())
									.collect(Collectors.toSet());
					
					for (Entry<Object, Map<Object, Double>> j : stats.indexedGroupedStats(rulename).entrySet()) {
						idxsJSON.add(j.getKey().toString());

						
						for (Object s : all_series) {
							Double val = stats.indexedGroupedStats(rulename).get(j.getKey()).get(s);
							if (val == null) val = (double) 0;
							JsonArrayBuilder ser = series.computeIfAbsent(s, x -> Json.createArrayBuilder());
							ser.add(val);
						}
					}
					JsonArrayBuilder valsJSON = Json.createArrayBuilder();
					for (Entry<Object,JsonArrayBuilder> i : series.entrySet()) {
						JsonObjectBuilder ser = Json.createObjectBuilder();
						ser.add("series",i.getKey().toString());
						ser.add("values",i.getValue());
						valsJSON.add(ser);
					}
					metricDict.add("name", rulename);
					metricDict.add("desc", stats.getMetricDesc(rulename));
					metricDict.add("indices", idxsJSON);
					metricDict.add("series", valsJSON);
					metricArray.add(metricDict);
				}
			}
		}
		statsJSON.add("classified", metricArray);
		
		metricArray = Json.createArrayBuilder();
		for (Method rule : methods) {
			if (rule.getAnnotation(MetricMethod.class) != null) {
				String rulename = rule.getAnnotation(MetricMethod.class).rule();
				if (stats.challengeIndexedGroupedStats(rulename) != null) {
					JsonObjectBuilder metricDict = Json.createObjectBuilder();
					JsonArrayBuilder challengeArray = Json.createArrayBuilder();
					for (Entry<String, Map<Object, Map<Object, Double>>> challenges : stats.challengeIndexedGroupedStats(rulename).entrySet()) {
						JsonObjectBuilder challengeDict = Json.createObjectBuilder();
						JsonArrayBuilder indicesArray = Json.createArrayBuilder();
						Map<Object,JsonArrayBuilder> flat_series = new HashMap<Object,JsonArrayBuilder>();
						
						Set<Object> all_series = challenges.getValue().values().stream()
								.map(x -> x.keySet())
								.flatMap(x -> x.stream())
								.collect(Collectors.toSet());
						
						for (Entry<Object, Map<Object, Double>> indices : stats.challengeIndexedGroupedStats(rulename).get(challenges.getKey()).entrySet()) {
							indicesArray.add(indices.getKey().toString());
							
							for (Object s : all_series) {
								Double val = stats.challengeIndexedGroupedStats(rulename).get(challenges.getKey()).get(indices.getKey()).get(s);
								if (val == null) val = (double) 0;
								JsonArrayBuilder ser = flat_series.computeIfAbsent(s, x -> Json.createArrayBuilder());
								ser.add(val);
							}
							
						}
						JsonArrayBuilder seriesArray = Json.createArrayBuilder();
						for (Entry<Object,JsonArrayBuilder> i : flat_series.entrySet()) {
							JsonObjectBuilder seriesDict = Json.createObjectBuilder();
							seriesDict.add("series",i.getKey().toString());
							seriesDict.add("values",i.getValue());
							seriesArray.add(seriesDict);
						}
						challengeDict.add("series", seriesArray);
						challengeDict.add("indices", indicesArray);
						challengeDict.add("challenge", challenges.getKey());
						challengeArray.add(challengeDict);
					}
					metricDict.add("name", rulename);
					metricDict.add("desc", stats.getMetricDesc(rulename));
					metricDict.add("challenges", challengeArray);
					metricArray.add(metricDict);
				}
			}
		}
		statsJSON.add("challenges", metricArray);
		Map<String,Integer> nodeIds = new HashMap<>();
		int nodeCtr = 0;
		JsonArrayBuilder graphArray = Json.createArrayBuilder();
		for (String cld : stats.graphNodes().keySet()) {
			JsonObjectBuilder graphDict = Json.createObjectBuilder();
			JsonArrayBuilder nodeArray = Json.createArrayBuilder();
			for (Node nd : stats.graphNodes().get(cld).values()) {
				nodeArray.add(toJson(nd,nodeCtr));
				nodeIds.put(nd.label, nodeCtr);
				nodeCtr++;
			}
			JsonArrayBuilder edgeArray = Json.createArrayBuilder();
			if (stats.graphEdges().get(cld) == null)
				System.out.println("** No edges for "+cld+"!");
			else
				for (Entry<String, Map<String, Integer>> nd : stats.graphEdges().get(cld).entrySet()) {
					for (Entry<String, Integer> to : nd.getValue().entrySet()) {
						JsonObjectBuilder edgeDict = Json.createObjectBuilder();
						edgeDict.add("from", nodeIds.get(nd.getKey()));
						edgeDict.add("to", nodeIds.get(to.getKey()));
						edgeDict.add("value", to.getValue());
						edgeArray.add(edgeDict);
					}
				}
			
			graphDict.add("challenge", cld);
			graphDict.add("nodes", nodeArray);
			graphDict.add("edges", edgeArray);
			
			graphArray.add(graphDict);
		}

		statsJSON.add("graphs", graphArray);
		
		return statsJSON.build().toString();
	}

	public JsonObject toJson(Node node, int nodeCtr) {
		JsonObjectBuilder nodeDict = Json.createObjectBuilder();
		nodeDict.add("id", nodeCtr);
		nodeDict.add("title", node.label);
		nodeDict.add("value", node.weight);
		nodeDict.add("group", node.sat);
		return nodeDict.build();
	}
	
	/**
	 * Parses the JSON instance requires.
	 * @param body the request body
	 * @return the parsed request
	 */
	static private StatsRequest parseJSON(String body) {
		JSONObject jo = new JSONObject(body);
		StatsRequest req = new StatsRequest(jo.getString("model"));
		return req;
	}
}
