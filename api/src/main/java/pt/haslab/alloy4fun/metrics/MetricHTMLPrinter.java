package pt.haslab.alloy4fun.metrics;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONException;

import pt.haslab.alloy4fun.datamodel.A4FModel;
import pt.haslab.alloy4fun.datamodel.A4FShare;
import pt.haslab.alloy4fun.datamodel.A4FExecution;
import pt.haslab.alloy4fun.datamodel.A4FExecution.RESULT;

public class MetricHTMLPrinter {
	
	private final static Map<String,String> colors = Stream.of(new String[][] {
		  { "unsolved", "#f0e878" }, 
		  { RESULT.SAT.toString(), "#f0e878" }, 
		  { "solved", "#93e388" }, 
		  { RESULT.UNSAT.toString(), "#93e388" }, 
		  { RESULT.ERROR.toString(), "#f06779" }, 
		  { "warning", "ffffff" }, 
		  { "model share", "#6c79ad" }, 
		  { "instance share", "#6179ad" }, 
		}).collect(Collectors.toMap(data -> data[0], data -> data[1]));

	
	private static void printDerivTree(A4FModel obj, int indent, FileWriter fw) throws IOException {
		 
		String cls;
		if (obj instanceof A4FExecution) {
			if (((A4FExecution) obj).result() == RESULT.ERROR)
				cls = "list-group-item-danger";
			else if (((A4FExecution) obj).result() == RESULT.UNSAT)
				cls = "list-group-item-success";
			else
				cls = "list-group-item-warning";
		}
		else
			cls = "list-group-item-info";
		
		String ico = obj.children().isEmpty()?"glyphicon-minus":"glyphicon-chevron-right";
		
		fw.write("<a href=\"#item-"+obj.id+"\" class=\"more list-group-item "+cls+"\" data-toggle=\"collapse\" style=\"padding-left:"+indent*10+"px\">\n" + 
				"    <i id=\"icon-"+obj.id+"\" class=\"glyphicon "+ico+"\"></i>"+printShortObj(obj)+"</a>\n" + 
						"  <div class=\"list-group collapse show\" id=\"item-"+obj.id+"\">");
		for (A4FModel entry : obj.children()) {
			printDerivTree(entry, indent+1, fw);
		}
		fw.write("</div>\n");
		
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

	public static void main(String[] args) throws SecurityException, IOException, ClassNotFoundException, JSONException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		final boolean all = args.length > 3;
		final String model_json = args[0];
		final String link_json = all?args[1]:null;
		final String instance_json = all?args[2]:null;
		final String original_id = args[all?3:1];
		final Class<?> catalog = Class.forName(args[all?4:2]);
		ModelStats stats = MetricRunner.run(original_id,model_json,link_json,instance_json,catalog);

		FileWriter fw = new FileWriter(original_id+".html");
		
		fw.write("<head>\n" + 
				"  <meta charset=\"UTF-8\">\n" + 
				"  <meta name=\"description\" content=\"Free Web tutorials\">\n" + 
				"  <meta name=\"keywords\" content=\"HTML, CSS, JavaScript\">\n" + 
				"  <meta name=\"author\" content=\"John Doe\">\n" + 
				"  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
				"  <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/css/bootstrap.min.css\">\n" + 
				"  <link rel=\"stylesheet\" href=\"metrics.css\">\n" +
				"</head>\n" +
				"<body>\n" +
				"<script src=\"https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.6.0/Chart.min.js\"></script>\n");
		
		fw.write("<h1>"+original_id+" ("+LocalDate.now()+")</h1>\n");
		
		fw.write("<h2>Meta-data</h2>\n");
		fw.write("<dl>");		
		fw.write("<dt>Challenge name</dt><dd>"+stats.getA4f().getModule_name()+"</dd>\n");
		fw.write("<dt>Creation time</dt><dd>"+stats.getChallengeDate()+"</dd>\n");
		fw.write("<dt># sub-challenges</dt><dd>"+stats.getA4f().challengeLabels().size()+"</dd>\n");
		fw.write("<dt>Metric catalog</dt><dd>"+MetricRunner.getCatalogName()+"</dd>\n");
		fw.write("</dl>");		
		fw.write("<h2>Overall metric statistics</h2>\n");
		fw.write("Overall statistics for this challenge. Note that for challenges correctness is unsatisfiability. Totals may contain executions of commands other than the sub-challenges, if declared by the user.");
		fw.write("<dl>");		
//		fw.write("<dt>Total solutions</dt><dd>"+stats.getTotalSolutions()+"</d>\n");
//		fw.write("<dt>Timed out re-executions</dt><dd>"+stats.getTotalTimeouts()+"</d>\n");
//		fw.write("<dt>Rejected due to server errors</dt><dd>"+stats.getTotalServerErrors()+"</d>\n");
//		fw.write("<dt>Inconsistent result reports</dt><dd>"+stats.getTotalInconsistentRes()+"</d>\n");
//		fw.write("<dt>Inconsistent message reports</dt><dd>"+stats.getTotalInconsistentMsg()+"</d>\n");
		fw.write("</dl>");		
		
		fw.write("<h2>Scalar metrics</h2>\n");
		fw.write("<dl>");		
		for (String metric_name : stats.scalarMetrics())
			fw.write("<dt data-toggle=\"tooltip\" title=\""+stats.getMetricDesc(metric_name)+"\">"+metric_name+"</dt><dd>"+stats.scalarStats(metric_name)+"</dd>\n");
		fw.write("</dl>");		

		fw.write("<h2>Overall metrics</h2>\n");
		for (String metric_name : stats.indexedMetrics()) {
			fw.write("<h3>"+metric_name+"</h3>\n");
			fw.write("<p>"+stats.getMetricDesc(metric_name)+"</p>\n");

			fw.write("<canvas id=\""+metric_name.replaceAll("[^A-Za-z]", "")+"\" width=\"1400\"></canvas>");
			fw.write("<script>\n" + 
					"  var chart = new Chart("+metric_name.replaceAll("[^A-Za-z]", "")+", {\n" + 
					"   type: 'bar',\n" + 
					"   data: {\n" + 
					"      labels: [");
			
			for (Object c : stats.indexedStats(metric_name).keySet())
				fw.write("'"+c.toString().replace("\n", " ").replace("\\", "\\\\").replace("'", "\\'")+"',");

			fw.write("]," + 
					"      datasets: [\n");
			
			fw.write("{backgroundColor: '#6c79ad', data: [");
			
			int max_len = 0;
			for (Object c : stats.indexedStats(metric_name).keySet()) {
				max_len = Math.max(max_len, c.toString().length());
				fw.write(stats.indexedStats(metric_name).getOrDefault(c,(double) 0)+",");
			}

			fw.write("]},");

			fw.write("]");
				
				fw.write("},\n" + 
				"   options: {\n" + 
				"      responsive: false,\n" + 
				"      legend: {\n" + 
				"         display: false\n" + 
				"      },\n" + 
				"      scales: {\n" + 
				"         xAxes: [{\n" + 
				"                ticks: {\n" + 
				"                    autoSkip: "+(max_len>20?false:true)+",\n" + 
				"                    minRotation:"+ (max_len>20?90:0) +",\n" + 
				"                    maxRotation:"+ (max_len>20?90:20) +",\n" + 
				"                }\n" + 
				"            }],\n" + 
				"      }\n" + 
				"   }\n" + 
				"});\n" + 
				"</script>");
		
		}		
		
		fw.write("<h2>Overall metrics, classified</h2>\n");
		for (String metric_name : stats.indexedGroupedMetrics()) {
			fw.write("<h3>"+metric_name+"</h3>\n");
			fw.write("<p>"+stats.getMetricDesc(metric_name)+"</p>\n");
			fw.write(printClassifiedBars(stats.indexedGroupedStats(metric_name), metric_name));
		}
		
		fw.write("<h2>Metrics by sub-challenge, classified</h2>\n");
		for (String metric_name : stats.challengeIndexedGroupedMetrics()) {
			fw.write("<h3>"+metric_name+"</h3>\n");
			fw.write("<p>"+stats.getMetricDesc(metric_name.toString())+"</p>\n");
			for (Object challenge_name : stats.challengeIndexedGroupedStats(metric_name).keySet()) {
				try {
					if (stats.challengeIndexedGroupedStats(metric_name).get(challenge_name).size() > 0) {
						fw.write("<h4>"+challenge_name+"</h4>\n");
						fw.write(printClassifiedBars(stats.challengeIndexedGroupedStats(metric_name).get(challenge_name), metric_name+challenge_name));
					}
				} catch(Exception e) {
					
				}
			}
		}
		

		fw.write("<h2>Derivation tree</h2>\n" +
		"<div class=\"just-padding\">\n" + 
		"<div class=\"list-group list-group-root well\">\n");
		printDerivTree(stats.root,0,fw);
		fw.write("</div>\n" + 
				"</div>");

		fw.write(
				"<script src=\"https://code.jquery.com/jquery-3.3.1.slim.min.js\" integrity=\"sha384-q8i/X+965DzO0rT7abK41JStQIAqVgRVzpbzo5smXKp4YfRvH+8abtTE1Pi6jizo\" crossorigin=\"anonymous\"></script>\n"
						+ "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js\" integrity=\"sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1\" crossorigin=\"anonymous\"></script>\n"
						+ "<script src=\"https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js\" integrity=\"sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM\" crossorigin=\"anonymous\"></script>\n");

		fw.write("<script>\n" + 
				"  $(\".more\").click(function() {  \n" + 
				"  \n" + 
				"  icon = $(this).find(\"i\");\n" + 
				"  icon.toggleClass(\"glyphicon-chevron-right glyphicon-chevron-up\");\n" + 
				"  });\n" + 
				"</script>\n" + 
				"");

		fw.write("</body>");
		fw.close();
		
		System.out.println("Done");
		System.exit(0);
	}

	private static String printClassifiedBars(Map<Object,Map<Object,Double>> vals, String name) {
		StringBuilder sb = new StringBuilder();
		Set<Object> labels = new HashSet<>();
		for (Map<Object,Double> m : vals.values())
			labels.addAll(m.keySet());

		
		sb.append("<canvas id=\""+name.replaceAll("[^A-Za-z0-9]", "")+"\" ></canvas>");
		sb.append("<script>\n" + 
				"  var chart = new Chart("+name.replaceAll("[^A-Za-z0-9]", "")+", {\n" + 
				"   type: 'bar',\n" + 
				"   indexLabel: '#percent%',\n"+
				"   data: {\n" + 
				"      labels: [");
		
		for (Object c : vals.keySet())
			sb.append("'"+c+"',");
		
		sb.append("]," + 
				"      datasets: [\n");

		for (Object lbl : labels) {
			sb.append("{label: '"+lbl+"', backgroundColor: '"+colors.get(lbl.toString())+"', data: [");
		
			for (Object c : vals.keySet()) {
				Map<Object, Double> mc = vals.getOrDefault(c,new HashMap<Object,Double>());
				Double mv = mc.getOrDefault(lbl,(double) 0);
				sb.append(mv+",");
			}

			sb.append("]},");
		}

		sb.append("]");
			
		sb.append("},\n" + 
			"   options: {\n" + 
			"	tooltips: {\n" + 
			"      enabled: true,\n" + 
			"      mode: 'single',\n" + 
			"      callbacks: {\n" + 
			"        label: function(value, context) {\n" + 
			"          if (value == 0) return '';\n" + 
			"          let dataArr = context.datasets;\n" + 
			"          let sum = 0;\n" + 
			"          dataArr.map(data => {\n" + 
			"              sum += data.data[value.index];\n" + 
			"          });\n" + 
			"          return context.datasets[value.datasetIndex].label +\": \"+value.yLabel +' ('+Math.round(1000 / sum * value.yLabel) / 10 + '%)';\n" + 
			"        }\n" + 
			"      }\n" + 
			"    },"+
			"      responsive: true,\n" + 
			"	   maintainAspectRatio: false,\n" + 
			"      legend: {\n" + 
			"         position: 'right'\n" + 
			"      },\n" + 
			"      scales: {\n" + 
			"         xAxes: [{\n" + 
			"            maxBarThickness: 100, stacked: true\n" + 
			"         }],\n" + 
			"         yAxes: [{\n" + 
			"            beginAtZero:true, stacked: true\n" + 
			"         }]\n" + 
			"      }\n" + 
			"   }\n" + 
			"});\n" + 
			"</script>");	
		
		return sb.toString();
	}
}
