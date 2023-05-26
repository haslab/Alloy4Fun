package pt.haslab.alloy4fun.metrics;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;

import pt.haslab.alloy4fun.datamodel.A4FModel;
import pt.haslab.alloy4fun.datamodel.A4FShare;
import pt.haslab.alloy4fun.datamodel.A4FExecution;
import pt.haslab.alloy4fun.datamodel.A4FExecution.RESULT;

public class MetricTextPrinter {
	
	private static void printDerivTree(A4FModel obj, int indent, FileWriter fw) throws IOException {
		 
		fw.write(new String(new char[indent]).replace("\0", "  ") +printShortObj(obj)+"\n");
		for (A4FModel entry : obj.children())
			printDerivTree(entry, indent+1, fw);
		
	}
	
	private static String printShortObj(A4FModel obj) {
		StringBuilder sb = new StringBuilder();
		sb.append(obj.id);
		sb.append(", ");
		sb.append(obj.time);
		sb.append(" (");
		if (obj instanceof A4FShare)
			if (((A4FShare) obj).instance_share)
				sb.append("instance share");
			else
				sb.append("model share");
		else if (obj instanceof A4FExecution) {
			if (((A4FExecution) obj).result() == RESULT.SAT || ((A4FExecution) obj).result() == RESULT.UNSAT) {
				sb.append(((A4FExecution) obj).cmd_name);
				sb.append(",");
				sb.append(((A4FExecution) obj).result() == RESULT.SAT?"incorrect":"correct");
				sb.append(",");
				sb.append(((A4FExecution) obj).cmd_check?"check":"run");
				sb.append(",");
			}
			if (((A4FExecution) obj).msg != null)
				sb.append(((A4FExecution) obj).msg.replace("\n", " "));
		}
		sb.append(")");
		return sb.toString();
	}
	
	private static String printClassifiedBars(Map<Object, Map<Object, Double>> vals, String name) {
		StringBuilder sb = new StringBuilder();
		Set<Object> labels = new HashSet<>();
		for (Map<Object,Double> m : vals.values())
			labels.addAll(m.keySet());

		sb.append("m\t");
		for (Object lbl : labels) {
			sb.append(lbl+"\t");
		}
		sb.append("\n");

		for (Object c : vals.keySet()) {
			sb.append(c.toString().replace("\n", " ")+"\t");
			for (Object lbl : labels) {
				Map<Object, Double> mc = vals.getOrDefault(c,new HashMap<Object,Double>());
				Double mv = mc.getOrDefault(lbl,(double) 0);
				sb.append(mv+"\t");
			}
			sb.append("\n");
		}

		return sb.toString();
	}

	public static void main(String[] args) throws SecurityException, IOException, ClassNotFoundException, JSONException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		final boolean all = args.length > 3;
		final String model_json = args[0];
		final String link_json = all?args[1]:null;
		final String instance_json = all?args[2]:null;
		final String nav_json = all?args[3]:null;
		final String original_id = args[all?4:1];
		final Class<?> catalog = Class.forName(args[all?5:2]);
		ModelStats stats = MetricRunner.run(original_id,model_json,link_json,instance_json,nav_json,catalog);

		FileWriter fw = new FileWriter(original_id+".txt");
		
		fw.write(original_id+" ("+LocalDate.now()+")\n");
		
		fw.write("\n* Meta-data\n");
		fw.write("Challenge name: "+stats.getA4f().getModule_name()+"\n");
		fw.write("Creation time: "+stats.getChallengeDate()+"\n");
		fw.write("# sub-challenges: "+stats.getA4f().challengeLabels().size()+"\n");
		fw.write("Metric catalog: "+MetricRunner.getCatalogName()+"\n");
		
		fw.write("\n* Scalar metrics\n");
		for (String metric_name : stats.scalarMetrics())
			fw.write(metric_name+"\t"+stats.scalarStats(metric_name)+"\n");

		fw.write("\n* Overall metrics\n");
		for (String metric_name : stats.indexedMetrics()) {
			fw.write("\n"+metric_name+"\n");

			for (Object c : stats.indexedStats(metric_name).keySet()) {
				fw.write(c.toString().replace("\n", " ")+"\t"+stats.indexedStats(metric_name).getOrDefault(c,(double) 0)+"\n");
			}
			
		}
		
		fw.write("\n* Overall metrics, classified\n");
		for (String metric_name : stats.indexedGroupedMetrics()) {
			fw.write("\n"+metric_name+"\n");
			fw.write(printClassifiedBars(stats.indexedGroupedStats(metric_name), metric_name));
		}
		
		fw.write("\n* Metrics by sub-challenge, classified\n");
		for (String metric_name : stats.challengeIndexedGroupedMetrics()) {
			fw.write("\n"+metric_name+"\n");
			for (Object challenge_name : stats.challengeIndexedGroupedStats(metric_name).keySet()) {
				if (stats.challengeIndexedGroupedStats(metric_name).get(challenge_name).size() > 0) {
					fw.write("\n"+challenge_name+"\n");
					fw.write(printClassifiedBars(stats.challengeIndexedGroupedStats(metric_name).get(challenge_name), metric_name+challenge_name));
				}
			}
		}

		fw.write("\n* Derivation tree\n");
		printDerivTree(stats.root,0,fw);
		fw.close();
		
		System.out.println("Done");
		System.exit(0);
	}


}
