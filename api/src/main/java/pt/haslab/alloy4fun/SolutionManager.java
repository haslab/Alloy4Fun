package pt.haslab.alloy4fun;
import javax.ws.rs.core.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.translator.A4Solution;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class SolutionManager extends Application {
	
	private static int TIMEOUT = 600;

	private static Logger LOGGER = LoggerFactory.getLogger(AlloyGetInstances.class);
	
	private static Map<String, List<A4Solution>> answers = new HashMap<String, List<A4Solution>>();
	private static Map<String, Integer> count = new HashMap<String, Integer>();
	private static Map<String, Command> cmd = new HashMap<String, Command>();
	private static Map<String, Iterable<Func>> skolems = new HashMap<String, Iterable<Func>>();
	
	private static Map<String, ScheduledExecutorService> scheds = new HashMap<>();
    
	public static void remove(String sessionId) {
		answers.remove(sessionId);
		count.remove(sessionId);
		cmd.remove(sessionId);
		skolems.remove(sessionId);
		LOGGER.info("Closing session for "+sessionId+", now active: "+answers.size());
	    Runtime rt = Runtime.getRuntime();
	    long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
		LOGGER.debug("Memory used: " + usedMB+"mb.");
		LOGGER.debug("Number of active threads: " + Thread.activeCount());
		
		scheds.get(sessionId).shutdownNow();
	}

	public static void add(String sessionId, A4Solution ans, Command cm, Iterable<Func> skolem) {
		answers.put(sessionId,new ArrayList<A4Solution>());
		answers.get(sessionId).add(ans);
		count.put(sessionId,0);
		cmd.put(sessionId,cm);
		skolems.put(sessionId, skolem);
		LOGGER.info("Opening session for "+sessionId+", now active: "+answers.size()+", will live for "+TIMEOUT);
	    Runtime rt = Runtime.getRuntime();
	    long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
		LOGGER.debug("Memory used: " + usedMB+"mb.");
		LOGGER.debug("Number of active threads: " + Thread.activeCount());
		
		// session will be closed after 10min
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.schedule(new Runnable() {
			public void run() {
				SolutionManager.remove(sessionId);
			}
		}, TIMEOUT, TimeUnit.SECONDS);
		scheds.put(sessionId, scheduler);
	}

	public static A4Solution getSol(String sessionId) {
		List<A4Solution> sols = answers.get(sessionId);
		return sols.get(sols.size()-1);
	}

	public static A4Solution getSol(String sessionId, int n) {
		List<A4Solution> sols = answers.get(sessionId);
		return sols.get(n);
	}

	public static int getCnt(String sessionId) {
		return count.get(sessionId);
	}

	public static Iterable<Func> getSkolem(String sessionId) {
		return skolems.get(sessionId);
	}

	public static Command getCommand(String sessionId) {
		return cmd.get(sessionId);
	}

	public static void next(String sessionId) throws Err {
		answers.get(sessionId).add(getSol(sessionId).next());
		count.put(sessionId,count.get(sessionId)+1);
	}

	public static boolean alive(String sessionId) {
		return answers.containsKey(sessionId);
	}

	public SolutionManager() {
    	
    }
}

