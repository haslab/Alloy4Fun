package pt.haslab.alloy4fun.datamodel;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

import org.json.JSONObject;

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
import pt.haslab.alloy4fun.metrics.SessionMetrics;
import pt.haslab.alloy4fun.metrics.utils.NormalizeExpr;
import pt.haslab.alloy4fun.metrics.utils.PrintExpr;
import pt.haslab.alloy4fun.graph.Node;

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
	public final Map<String,Map<String,Entry<String,Integer>>> edges = new TreeMap<>();

	
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
				String mdl_norm = normalized.get(chl).get(mdl.id);
				if (mdl_norm != null) {
					nodes.computeIfAbsent(chl, x -> new HashMap<>()).computeIfAbsent(mdl_norm, x -> new Node(mdl.result().toString(), mdl_norm)).increase();
					for (A4FModel cld : mdl.childrenCmd(chl)) {
						String cld_norm = normalized.get(chl).get(cld.id);
						if (cld_norm != null)
							edges.computeIfAbsent(chl, x -> new HashMap<>()).compute(mdl_norm, (x,y) -> y==null?new AbstractMap.SimpleEntry<>(cld_norm,1):new AbstractMap.SimpleEntry<>(cld_norm,y.getValue()+1));
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
		A4FModel mdl = models.get(id);

		A4FModel parent = mdl.parent;

		if (parent != null)
			return mdl;
		
		if(!mdl.id.equals(this.model_id)) {
			try {
				parent = calculateDerivTree(mdl.parent_entry,reexecute);

				mdl.setParent(parent);
			} catch (Exception e) {
				System.out.println("Problems with derivationOf of "+id);
			}
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
						System.out.println("Timed out: "+mdl.id);
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
						System.out.println("Server error, disregarded during analysis: "+((A4FExecution) mdl).msg+" ("+mdl.id+")");
					} else if (((A4FExecution) mdl).result() == RESULT.ERROR && err == null) {
						System.out.println("Had error result registered but ran ok: "+((A4FExecution) mdl).msg.replace("\n", " ")+" ("+mdl.id+")");
						inconsistent_res.add(id);
					} else if (((A4FExecution) mdl).msg != null && wns.isEmpty() && err == null) {
						System.out.println("Had error message registered but ran ok: "+((A4FExecution) mdl).msg.replace("\n", " ")+" ("+mdl.id+")");
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
	public SessionMetrics sessionMetrics(A4FModel start) {
		return SessionMetrics.sessionMetrics(start);
	}

	public void processRoot() {
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
			calculateDerivTree(id,true);
		
		calculateGraph();

	}
	
	public String getModule_name() {
		return module_name;
	}


	
}
