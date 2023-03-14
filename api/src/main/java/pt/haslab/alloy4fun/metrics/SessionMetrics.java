package pt.haslab.alloy4fun.metrics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pt.haslab.alloy4fun.datamodel.A4FExecution;
import pt.haslab.alloy4fun.datamodel.A4FModel;
import pt.haslab.alloy4fun.datamodel.A4FShare;
import pt.haslab.alloy4fun.datamodel.A4FExecution.RESULT;
import pt.haslab.alloy4fun.metrics.utils.MultiSet;

public class SessionMetrics {
	
    final MultiSet<String> sat_cmds = new MultiSet<>();
    final MultiSet<String> unsat_cmds = new MultiSet<>();
    int shared_mdls = 0;
    int shared_insts = 0;
    int depth = 0;
    
	/* The cached session metrics, starting at a given entry. */
	private static Map<A4FModel,SessionMetrics> cacheSessionMetrics = new HashMap<A4FModel,SessionMetrics>();

    private SessionMetrics(A4FModel d) {
    	depth++;
    	if (d instanceof A4FExecution) {
            if (((A4FExecution) d).result() == RESULT.SAT) {
            	sat_cmds.add(((A4FExecution) d).cmd_name);
            }
            else if (((A4FExecution) d).result() == RESULT.UNSAT) {
            	unsat_cmds.add(((A4FExecution) d).cmd_name);
            }
    	} else if (d instanceof A4FShare) {
        	if (!((A4FShare) d).instance_share)
            	shared_mdls++;
        	else 
            	shared_insts++;
    	}
	}
    
    /* Calculates the metrics for a session starting given entry. */
    public static SessionMetrics sessionMetrics(A4FModel d) {
    	if (cacheSessionMetrics.containsKey(d))
    		return cacheSessionMetrics.get(d);
    	
    	SessionMetrics res = SessionMetrics.processSessions(d);
    	cacheSessionMetrics.put(d, res);
    	
    	return res;
    }
    
    /* Recursively calculates the metrics at a starting model entry. */
    private static SessionMetrics processSessions(A4FModel d) {
     	SessionMetrics res = new SessionMetrics(d);
    	for (A4FModel c : d.children) {
    		SessionMetrics ms = processSessions(c);
    		res.merge(ms);
    	}
    	return res;
    }

	void merge(SessionMetrics m) {
        this.sat_cmds.merge(m.sat_cmds);
        this.unsat_cmds.merge(m.unsat_cmds);
        this.shared_mdls+=m.shared_mdls;
        this.shared_insts+=m.shared_insts;
        this.depth=Math.max(this.depth, m.depth+1);
    }
	
	Set<String> unsatCommands() {
		return new HashSet<String>(unsat_cmds.elems());
	}

	Set<String> satCommands() {
		return new HashSet<String>(sat_cmds.elems());
	}
	
	int numSatExecs() {
		return sat_cmds.size();
	}

	int numUnsatExecs() {
		return unsat_cmds.size();
	}

}
