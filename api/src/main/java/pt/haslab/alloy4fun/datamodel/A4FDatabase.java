package pt.haslab.alloy4fun.datamodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.ExprCall;
import edu.mit.csail.sdg.ast.ExprConstant;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.ast.VisitQuery;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;
import pt.haslab.alloy4fun.datamodel.A4FExecution.RESULT;
import pt.haslab.alloy4fun.graph.Node;
import pt.haslab.alloy4fun.metrics.SessionMetrics;
import pt.haslab.alloy4fun.metrics.utils.NormalizeExpr;
import pt.haslab.alloy4fun.metrics.utils.PrintExpr;

public class A4FDatabase {

	private static final long REEXECUTION_TIMEOUT = 60;
	private final Map<String,A4FModel> models = new HashMap<>();
	private final Map<String,A4FShare> shares = new HashMap<>();
	private final Map<String,A4FExecution> executions = new HashMap<>();
	private final Map<String,A4FLink> links = new HashMap<>();
	private final Map<String,A4FInstance> instances = new HashMap<>();
	private final Map<String,List<A4FNavigation>> navigations = new HashMap<>();
	private final Map<A4Solution,String> solutions = new HashMap<>();
	private final Map<Err,String> errors = new IdentityHashMap<>();
	private final Map<ErrorWarning,String> warnings = new HashMap<>();
	private final List<String> server_errors = new ArrayList<>();
	private final List<String> inconsistent_res = new ArrayList<>();
	private final List<String> inconsistent_msg = new ArrayList<>();
	private final List<String> timeouts = new ArrayList<>();	
	private String model_id;
	private String module_name;
	private boolean reexecuted = false;
	private List<String> challenges;
	private Map<String,String> preds;
	
	private Map<String,Map<String,String>> normalized = new TreeMap<>();
	public final Map<String,Map<String,Node>> nodes = new TreeMap<>();
	public final Map<String,Map<String,Map<String,Integer>>> edges = new TreeMap<>();

	private static Logger LOGGER = LoggerFactory.getLogger(A4FDatabase.class);

	/* The names of the predicates to be filled in the challenges (actually, currently empty preds in root). */
    public Set<String> challengPreds() {
		return new HashSet<String>(preds.keySet());
    }
	
    /* The labels of the commands considered challenges, check commands defined in the root model. */
	public List<String> challengeLabels() {
		return new ArrayList<>(challenges);
	}
	
	public A4FDatabase(String model_id) {
		this.model_id = model_id;
	}

	public void addModel(JSONObject obj) {
		if (obj.has("original") && obj.getString("original").equals(model_id)) {
			A4FModel mdl = A4FModel.fromJSON(obj);
			models.put(mdl.id,mdl);
			if (mdl instanceof A4FExecution)
				executions.put(mdl.id,(A4FExecution) mdl);
			else
				shares.put(mdl.id,(A4FShare) mdl);
			// when model database provided as tree
			if (obj.has("children")) {
				JSONArray arr = obj.getJSONArray("children");
				for (int i = 0; i < arr.length(); i++)
					addModel(arr.getJSONObject(i));
			}
		}		
	}

	public void addModel(A4FModel model) {
		if (model.root_entry.equals(model_id)) {
			models.put(model.id,model);
			if (model instanceof A4FExecution)
				executions.put(model.id,(A4FExecution) model);
			else
				shares.put(model.id,(A4FShare) model);
		}			
	}

	public void addLink(JSONObject obj) {
		if (models.containsKey(obj.getString("model_id"))) {
			links.put(obj.getString("_id"),new A4FLink(obj));
		}		
	}

	public void addLink(A4FLink link) {
		links.put(link.id,link);
	}
	
	public void addNavigation(A4FNavigation nav) {
		navigations.computeIfAbsent(nav.model_entry,x->new ArrayList<>()).add(nav);
	}

	public List<A4FNavigation> getNavigation(String mdl) {
		return navigations.computeIfAbsent(mdl,x->new ArrayList<>());
	}

	public void addInstance(JSONObject obj) {
		if (models.containsKey(obj.getString("model_id"))) {
			instances.put(obj.getString("_id"),new A4FInstance(obj));
		}		
	}
	
	public void addInstance(A4FInstance inst) {
		instances.put(inst.id,inst);
	}

	public A4FModel getModel(String model_id) {
		return models.get(model_id);
	}
	
	public void calculateGraph() {
		for (String chl : challengeLabels()) {
			for (A4FExecution mdl : executions.values()) {
				if (normalized.containsKey(chl) && normalized.get(chl).containsKey(mdl.id)) {
					String mdl_norm = normalized.get(chl).get(mdl.id);
					nodes.computeIfAbsent(chl, x -> new HashMap<>()).computeIfAbsent(mdl_norm, x -> new Node(mdl.result().toString(), mdl_norm)).increase();
					for (A4FModel cld : mdl.childrenCmd(chl)) {
						String cld_norm = normalized.get(chl).get(cld.id);
						if (cld_norm != null) {
							edges.computeIfAbsent(chl, x -> new HashMap<>())
								.computeIfAbsent(mdl_norm, x -> new HashMap<>())
								.compute(cld_norm, (x,y) -> y==null?1:y+1);
						}
					}
				}
			}
		}
	}
	
	public A4FModel calculateDerivTree(String id, boolean reexecute) {
		if (!models.containsKey(id)) {
			System.out.println("Could not find parent model "+id);
			return null;
		}
		if (id.equals(root().id))
			return root();

		A4FModel mdl = models.get(id);
		
		A4FModel parent = mdl.parent;

		if (parent != null)
			return mdl;
		
		try {
			parent = calculateDerivTree(mdl.parent_entry,reexecute);

			mdl.setParent(parent);
		} catch (Exception e) {
			System.out.println("Problems with derivationOf of "+id);
		}
		
		String model = mdl.code;
		
		if (extractSecrets(model).isEmpty()) {
			List<String> secrets = extractSecrets(models.get(mdl.root_entry).code);
			for (String secret : secrets)
				model += secret;
		}
		
		final List<ErrorWarning> wns = new ArrayList<>();
		final A4Reporter rep = new A4Reporter() {
			public void warning(ErrorWarning msg) {
				super.warning(msg);
				wns.add(msg);
			};
		};
	
		try {
			final CompModule wrl = CompUtil.parseEverything_fromString(rep, model);
			for (ErrorWarning e : wns) {
				warnings.put(e,id);
				mdl.addWarning(e);
			}
			

			if (mdl instanceof A4FExecution) {
				
				for (Func f : wrl.getAllFunc()) {
					if (((A4FExecution) mdl).cmd_name != null && (((A4FExecution) mdl).cmd_name).equals(preds.get(f.label))) {
						String ast = f.getBody().accept(new NormalizeExpr()).accept(new PrintExpr()).toString();
						if (f.getBody() == null)
							ast = ExprConstant.TRUE.toString();
						normalized.computeIfAbsent(preds.get(f.label), x -> new HashMap<String,String>()).put(mdl.id,ast);
					}
				}
				
				final Command cmd = wrl.getAllCommands().get(((A4FExecution) mdl).cmd_index);
				Err err = null;
				executions.get(id).setCommand(cmd);
				if (reexecute) {
					ExecutorService executor = Executors.newCachedThreadPool();
					Callable<A4Solution> task = new Callable<A4Solution>() {
					   public A4Solution call() {
					      return TranslateAlloyToKodkod.execute_command(rep, wrl.getAllReachableSigs(), cmd, new A4Options());
					   }
					};
					Future<A4Solution> future = executor.submit(task);
					A4Solution sol = null;
					try {
						sol = future.get(REEXECUTION_TIMEOUT, TimeUnit.SECONDS); 
						solutions.put(sol, id);
					} catch (TimeoutException e) {
						timeouts.add(id);
						LOGGER.warn("Timed out during execution: "+mdl.id);
					} catch (InterruptedException e) {
					} catch (ExecutionException e) {
						if (e.getCause() instanceof Err) {
							// Alloy runtime errors
							err = (Err) e.getCause();
							mdl.error = err;
							errors.put(err, id);
						}
					} finally {
					   future.cancel(true);
					   executor.shutdownNow();
					}
					if (((A4FExecution) mdl).result() == RESULT.ERROR && sol != null) {
						for (ErrorWarning w : mdl.wns)
							warnings.remove(w);
						errors.remove(err);
						executions.remove(id);
						solutions.remove(sol);
						server_errors.add(id);
						LOGGER.warn("Server error, disregarded during analysis: "+((A4FExecution) mdl).msg+" ("+mdl.id+")");
					} else if (((A4FExecution) mdl).result() == RESULT.ERROR && err == null) {
						LOGGER.warn("Had error result registered but ran ok: "+((A4FExecution) mdl).msg.replace("\n", " ")+" ("+mdl.id+")");
						inconsistent_res.add(id);
					} else if (((A4FExecution) mdl).msg != null && wns.isEmpty() && err == null) {
						LOGGER.warn("Had error message registered but ran ok: "+((A4FExecution) mdl).msg.replace("\n", " ")+" ("+mdl.id+")");
						inconsistent_msg.add(id);
					}
				}
				
			
			}

		} catch (Err e) {
			if (errors.containsKey(e)) {
				System.out.println("alarm");
				e.printStackTrace();
			}
			errors.put(e, id);
			mdl.error = e;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mdl;
	}

	private static List<String> extractSecrets(String code) {
	    String tag = Pattern.quote(secretTag)+"\\s*?\\n";
	    String paragraphKeywords = "sig|fact|assert|check|fun|pred|run";
	    String pgd = "(?:(?:var|one|abstract|lone|some)\\s+)*"+paragraphKeywords;
	    String cmnts = "(?:(?:/\\*(?:.|\\n)*?\\*/\\s*)|(?://.*\\n)|(?:--.*\\n))";
	    String exp = "(?:("+tag+"\\s*"+ cmnts +"*?\\s*(?:"+pgd+")(?:.|\\n)*?)(?:"+tag+"\\s*)?(?:"+ cmnts +"*?\\s*(?:"+pgd+")\\s+|$))";

	    List<String> mts = new ArrayList<String>();
	    Matcher m = Pattern.compile(exp).matcher(code);
	    int where = 0;
	    while (m.find(where)) {
	    	mts.add(m.group(1));
	    	where = m.end(1);
	    }
	    
	    return mts;
	}
	
	private final static String secretTag = "//SECRET";


	public List<String> serverErrors() {
		if (!reexecuted)
			throw new IllegalStateException("Models not re-executed.");
		return server_errors;
	}

	public Map<String, A4FModel> models() {
		return models;
	}

	public Map<String, A4FExecution> executions() {
		return executions;
	}

	public Map<String, A4FShare> shares() {
		return shares;
	}

	public Map<String, A4FInstance> instances() {
		return instances;
	}

	public Map<String, A4FLink> links() {
		return links;
	}
	
	public Map<String, List<A4FNavigation>> navigations() {
		return navigations;
	}

	public List<String> timeouts() {
		if (!reexecuted)
			throw new IllegalStateException("Models not re-executed.");
		return timeouts;
	}

	public List<String> inconsistentMsg() {
		if (!reexecuted)
			throw new IllegalStateException("Models not re-executed.");
		return inconsistent_msg;
	}

	public List<String> inconsistentRes() {
		if (!reexecuted)
			throw new IllegalStateException("Models not re-executed.");
		return inconsistent_res;
	}

	public Map<A4Solution, String> solutions() {
		if (!reexecuted)
			throw new IllegalStateException("Models not re-executed.");
		return solutions;
	}

	public List<String> server_errors() {
		if (!reexecuted)
			throw new IllegalStateException("Models not re-executed.");
		return server_errors;
	}

	public Map<Err, String> errors() {
		// if !reexecute, will only contain compilation-time errors
		return errors;
	}

	public Map<ErrorWarning, String> warnings() {
		return warnings;
	}
	
	public A4FModel root() {
		return models.get(model_id);
	}

	/* The metrics for a session starting given entry. */
	// TODO: make this accessible from the session itself
	public SessionMetrics sessionMetrics(A4FModel start) {
		return SessionMetrics.sessionMetrics(start);
	}

	public void processRoot(boolean reexecute) {
		this.reexecuted = reexecute;
		CompModule or = CompUtil.parseEverything_fromString(new A4Reporter(),models.get(model_id).code);
		challenges = or.getAllCommands().stream().filter(c -> c.check).map(c -> c.label).collect(Collectors.toList());
		preds = new HashMap<>();
		
		VisitQuery<String> query = new VisitQuery<String>() {
			@Override
			public String visit(ExprCall x) throws Err {
				if (x.fun.getBody().isSame(ExprConstant.TRUE)) {
					return x.fun.label;
				}
				return super.visit(x);
			}

		};
		
		for (Command c : or.getAllCommands().stream().filter(c -> c.check).collect(Collectors.toList())) {
			String prd = c.formula.accept(query);
			preds.put(prd,c.label);
		}
		module_name = or.getModelName();
		
		for (String id : models().keySet())
			calculateDerivTree(id,reexecute);
		
		System.out.println(normalized.keySet());
		
//		// merge sessions
//		Map<String, Map<String,Session>> merge = new TreeMap<>();
//		Comparator<String> comparator = new Comparator<String>() {
//		    public int compare(String o1, String o2) {
//		    	int c = ((Character) o2.charAt(0)).compareTo(((Character) o1.charAt(0)));
//		        if (c != 0) return c;
//		        else return (Integer.valueOf(o1.substring(1))).compareTo(Integer.valueOf(o2.substring(1)));
//		    }
//		};
//		for (A4FModel m : root().children()) {
//			Session s = new Session(m);
//			getSession(m,s);
//			merge.computeIfAbsent(m.id.split("-")[1], x -> new TreeMap<String,Session>(comparator)).put(m.id.split("-")[2], s);
//		}
		
//		StringBuilder sb = new StringBuilder();
//		for (String p : merge.keySet()) {
//			sb.append(p);
//			for (String c : merge.get(p).keySet()) {
//				Session s = merge.get(p).get(c);
//				sb.append("\t");
//				sb.append(s.frst_dt == null?"--":s.frst_dt.toLocalTime());
//				sb.append("\t");
//				sb.append(s.r.size());
//				sb.append("\t");
//				sb.append(s.is_solved);
//				sb.append("\t");
//				sb.append(s.last_dt == null?"--":s.last_dt.toLocalTime());
//				sb.append("\t");
//				sb.append(s.has_fork);
//			}
//			sb.append("\n");
//		}
//		System.out.println(sb.toString());
		
		calculateGraph();

	}
	
	
//	private void getSession (A4FModel m, Session s) {
//		if (s.is_solved && m.time.compareTo(s.last_dt) > 0) {
//			System.out.println("Already solved: "+m.id);
//		}
//		if (!m.id.equals(s.origin)) {
//			s.r.add(m);
//			if (m.time.compareTo(s.last_dt) > 0)
//				s.last_dt = m.time;
//		}
//		if (m.children().size()>1)
//			s.has_fork = true;
//		if ((m instanceof A4FExecution) && ((A4FExecution) m).result() == RESULT.UNSAT) 
//			s.is_solved = true;
//
//		for (A4FModel c : m.children())
//			getSession(c,s);
//	}
//	
//	private class Session {
//
//		public Session(A4FModel m) {
//			origin = m.id;
//			if (m.children.size() > 0) {
//				frst_dt = m.children.get(0).time;
//				last_dt = m.time;
//			}
//			for (A4FModel c : m.children) 
//				if (c.time.compareTo(frst_dt) < 0)
//					frst_dt = c.time;
//		}
//		LocalDateTime last_dt;
//		LocalDateTime frst_dt;
//		List<A4FModel> r = new ArrayList<>(); 
//		boolean has_fork = false;
//		boolean is_solved = false;
//		
//		String origin;
//	}
//	
	public String getModule_name() {
		return module_name;
	}


	
}
