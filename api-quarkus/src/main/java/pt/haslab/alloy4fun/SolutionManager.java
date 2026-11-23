package pt.haslab.alloy4fun;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.translator.A4Solution;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

final class SolutionManager {
    private static final Logger LOGGER = Logger.getLogger(SolutionManager.class);
    private static final int TIMEOUT_SECONDS = 600;
    private static final Map<String, List<A4Solution>> answers = new HashMap<>();
    private static final Map<String, Integer> count = new HashMap<>();
    private static final Map<String, Command> commands = new HashMap<>();
    private static final Map<String, Iterable<Func>> skolems = new HashMap<>();
    private static final Map<String, ScheduledExecutorService> schedulers = new HashMap<>();

    private SolutionManager() {}

    static synchronized boolean alive(String sessionId) {
        return answers.containsKey(sessionId);
    }

    static synchronized void add(String sessionId, A4Solution answer, Command command, Iterable<Func> sessionSkolems) {
        LOGGER.infof("Opening session for %s, will live for %d", sessionId, TIMEOUT_SECONDS);
        answers.put(sessionId, new ArrayList<>(List.of(answer)));
        count.put(sessionId, 0);
        commands.put(sessionId, command);
        skolems.put(sessionId, sessionSkolems);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> remove(sessionId), TIMEOUT_SECONDS, TimeUnit.SECONDS);
        schedulers.put(sessionId, scheduler);
    }

    static synchronized void remove(String sessionId) {
        LOGGER.infof("Closing session for %s, now active: %d", sessionId, answers.size());
        answers.remove(sessionId);
        count.remove(sessionId);
        commands.remove(sessionId);
        skolems.remove(sessionId);
        ScheduledExecutorService scheduler = schedulers.remove(sessionId);
        if (scheduler != null) scheduler.shutdownNow();
    }

    static synchronized A4Solution getSol(String sessionId) {
        List<A4Solution> solutions = answers.get(sessionId);
        return solutions.get(solutions.size() - 1);
    }

    static synchronized A4Solution getSol(String sessionId, int index) {
        return answers.get(sessionId).get(index);
    }

    static synchronized int getCnt(String sessionId) { return count.get(sessionId); }
    static synchronized Command getCommand(String sessionId) { return commands.get(sessionId); }
    static synchronized Iterable<Func> getSkolem(String sessionId) { return skolems.get(sessionId); }

    static synchronized void next(String sessionId) throws Err {
        List<A4Solution> solutions = answers.get(sessionId);
        solutions.add(getSol(sessionId).next());
        count.put(sessionId, count.get(sessionId) + 1);
    }
}
