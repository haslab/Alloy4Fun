package pt.haslab.alloy4fun.metrics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.haslab.alloy4fun.datamodel.A4FDatabase;

public class MetricRunner {

	private static Logger LOGGER = LoggerFactory.getLogger(MetricRunner.class);

	private final static boolean REEXECUTE = true; 
	
	private static Class<?> catalog;
	private static String catalog_name; 

	private static ModelStats stats;
	private static A4FDatabase a4f;
	
	public static void main(String[] args) throws ClassNotFoundException, JSONException, IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		final boolean all = args.length > 3;
		final String model_json = args[0];
		final String link_json = all?args[1]:null;
		final String instance_json = all?args[2]:null;
		final String nav_json = all?args[3]:null;
		final String original_id = args[all?4:1];
		final Class<?> catalog = Class.forName(args[all?5:2]);
		run(original_id,model_json,link_json,instance_json,nav_json,catalog);
	}
	
	public static ModelStats run(String model_id, String model_json, String link_json, String instance_json, String nav_json, Class<?> catalog) throws JSONException, IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, SecurityException {
		String jline;
		a4f = new A4FDatabase(model_id);

		BufferedReader file = new BufferedReader(new FileReader(model_json));
		while ((jline = file.readLine()) != null)
			a4f.addModel(new JSONObject(jline));
		file.close();
		
		if (a4f.getModel(model_id) == null) {
			LOGGER.error("Model not found in database: "+model_id);
			throw new IllegalArgumentException("Model not found in database: "+model_id);
		}
		
		if (link_json != null) {
			file = new BufferedReader(new FileReader(link_json));
			while ((jline = file.readLine()) != null)
				a4f.addLink(new JSONObject(jline));
			file.close();
		}

		if (instance_json != null) { 
			file = new BufferedReader(new FileReader(instance_json));
			while ((jline = file.readLine()) != null)
				a4f.addInstance(new JSONObject(jline));
			file.close();
		}
		
		a4f.processRoot(REEXECUTE);

		MetricRunner.catalog = catalog;
		MetricSuite suite = MetricRunner.getCatalog().getAnnotation(MetricSuite.class);
		if (suite == null) {
			LOGGER.error("Invalid catalog annotations.");
			throw new IllegalArgumentException("Invalid catalog annotations.");
		}
		MetricRunner.catalog_name = suite.description().isEmpty()?catalog.getName():suite.description();
		
    	LOGGER.debug(String.format("Creating derivation tree (re-execution = %s)",REEXECUTE));

		if (REEXECUTE)
			for (String id: a4f.serverErrors())
				a4f.models().remove(id);

		LOGGER.info("Challenge labels: "+a4f.challengeLabels());
		
		stats = new ModelStats(model_id, a4f);

    	LOGGER.info("Processing metrics");
		stats.processMetrics(catalog.getMethods());
		LOGGER.info("Done.");
    	
		return stats;
	}
	
	public static String getCatalogName() {
		return catalog_name;
	}
	
	public static ModelStats getStats() {
		return stats;
	}
	
	/*
	 * Normalize error/warning messages removing indentifiers and locations.
	 */
	public static String normUpMessages(String string) {
		return string.replaceAll("name \".*?\"", "name _").replaceAll("\\s\\s.*?\\n", "  _\n")
				.replaceAll("\\{...*?\\}", "{_}").replaceAll("<...*?>", "<_>")
				.replaceAll("filename=.*?als", "filename=_.als").replaceAll("line\\s.*?\\s", "line _ ")
				.replaceAll("column\\s.*?\\s", "column _ ").replaceAll("parameters\\sare\\s.*?:", "parameters are _:")
				.replaceAll("field\\s.*?\\s<:\\s.*?(\\s|$)", "_ ").replaceAll("fun\\s.*?/.*?(\\s|$)", "_ ")
				.replaceAll("sig\\s.*?/.*?(\\s|$)", "_ ").replaceAll("pred\\s.*?/.*?(\\s|$)", "_ ")
				.replaceAll("to\\spred\\s.*?\\.", "to pred _.").replaceAll("to\\sfun\\s.*?\\.", "to fun _.")
				.replaceAll("side\\sis\\s.*?\\(type", "side is _ (type").replaceAll("^.*?is\\salready", "_ is already")
				.replaceAll("\\^.*?\\sis redundant", "^_ is redundant").replaceAll("\\s\\(.*?\\)", "(_)");
	}

	static Class<?> getCatalog() {
		return catalog;
	}

    
}
