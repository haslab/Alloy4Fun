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

	private final static boolean REEXECUTE = false; 
	
	private static Class<?> catalog;
	private static String catalog_name; 

	private static ModelStats stats;
	private static A4FDatabase a4f;
	
	public static void main(String[] args) throws ClassNotFoundException, JSONException, IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		final boolean all = args.length > 3;
		final String model_json = args[0];
		final String link_json = all?args[1]:null;
		final String instance_json = all?args[2]:null;
		final String original_id = args[all?3:1];
		final Class<?> catalog = Class.forName(args[all?4:2]);
		run(original_id,model_json,link_json,instance_json,catalog);
	}
	
	public static ModelStats run(String model_id, String model_json, String link_json, String instance_json, Class<?> catalog) throws JSONException, IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, SecurityException {
		String jline;
		a4f = new A4FDatabase(model_id);

		BufferedReader file = new BufferedReader(new FileReader(model_json));
		while ((jline = file.readLine()) != null)
			a4f.addModel(new JSONObject(jline));
		file.close();
		
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
		a4f.processRoot();

		MetricRunner.catalog = catalog;
		MetricSuite suite = MetricRunner.getCatalog().getAnnotation(MetricSuite.class);
		if (suite == null)
			throw new IllegalArgumentException("Invalid catalog annotations.");
		MetricRunner.catalog_name = suite.description().isEmpty()?catalog.getName():suite.description();
		
    	System.out.println("* Creating derivation tree (includes execution)...");

    	
		if (REEXECUTE)
			for (String id: a4f.serverErrors())
				a4f.models().remove(id);
		
		stats = new ModelStats(model_id, a4f);
		LOGGER.info(stats.toString());

		if (REEXECUTE) {
			System.out.println("Solutions: "+stats.getTotalSolutions());
			System.out.println("Timeouts: "+stats.getTotalTimeouts());
			System.out.println("Rejected: "+stats.getTotalServerErrors());
			System.out.println("Inconsistent msg: "+stats.getTotalInconsistentMsg());
			System.out.println("Inconsistent res: "+stats.getTotalInconsistentRes());
		}
		
    	System.out.println("* Processing metrics...");
		stats.processMetrics(catalog.getMethods());
		System.out.println("* Done.");
    	
		return stats;
	}
	
	public static String getCatalogName() {
		return catalog_name;
	}
	
	public static ModelStats getStats() {
		LOGGER.info(stats.toString());
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
