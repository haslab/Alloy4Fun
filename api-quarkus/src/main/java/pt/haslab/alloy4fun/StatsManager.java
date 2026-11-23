package pt.haslab.alloy4fun;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import edu.mit.csail.sdg.alloy4.Err;
import org.bson.Document;
import org.jboss.logging.Logger;
import org.json.JSONObject;
import pt.haslab.alloy4fun.datamodel.*;
import pt.haslab.alloy4fun.metrics.ModelStats;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

import static com.mongodb.client.model.Filters.eq;

final class StatsManager {
    private static final Logger LOGGER = Logger.getLogger(StatsManager.class);
    private static final int TIMEOUT_SECONDS = 600;
    private static final int MAXAGE = Integer.getInteger("alloy4fun.stats.maxage", 1000 * 60 * 60 * 24);
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);
    private static final ConcurrentMap<String, Future<ModelStats>> RUNNING = new ConcurrentHashMap<>();
    private static final Map<String, LocalDateTime> LAST = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService CACHE_CLEANER = Executors.newSingleThreadScheduledExecutor();
    private static volatile boolean cacheCleanerStarted;
    private StatsManager() {}

    static ModelStats getStats(String modelId, Class<?> catalog)
            throws InterruptedException, ExecutionException, TimeoutException {
        startCacheCleaner();
        Future<ModelStats> future = RUNNING.computeIfAbsent(modelId,
                id -> EXECUTOR.submit(() -> calculate(id, catalog)));
        try {
            ModelStats stats = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            LAST.put(modelId, LocalDateTime.now());
            return stats;
        } catch (ExecutionException error) {
            if (error.getCause() instanceof IllegalArgumentException missingRoot)
                throw missingRoot;
            throw error;
        }
    }

    private static synchronized void startCacheCleaner() {
        if (cacheCleanerStarted)
            return;
        cacheCleanerStarted = true;
        CACHE_CLEANER.scheduleAtFixedRate(() -> {
            LOGGER.info("Cleaning up stat caches.");
            RUNNING.clear();
            LAST.clear();
        }, MAXAGE, MAXAGE, TimeUnit.MILLISECONDS);
    }

    private static ModelStats calculate(String modelId, Class<?> catalog) throws Exception {
        String host = setting("alloy4fun.mongo.host", "MONGO_HOST", "127.0.0.1");
        int port = Integer.parseInt(setting("alloy4fun.mongo.port", "MONGO_PORT", "3001"));
        String databaseName = setting("alloy4fun.mongo.db", "MONGO_DB", "meteor");
        try (MongoClient client = MongoClients.create("mongodb://" + host + ":" + port)) {
            MongoDatabase database = client.getDatabase(databaseName);
            A4FDatabase a4f = new A4FDatabase(modelId);
            for (Document document : database.getCollection("Model").find(eq("original", modelId)))
                a4f.addModel(A4FModel.fromJSON(new JSONObject(document.toJson())));
            for (Document document : database.getCollection("Link").find())
                a4f.addLink(new A4FLink(new JSONObject(document.toJson())));
            for (Document document : database.getCollection("Instance").find())
                a4f.addInstance(new A4FInstance(new JSONObject(document.toJson())));
            for (Document document : database.getCollection("Navigation").find())
                a4f.addNavigation(new A4FNavigation(new JSONObject(document.toJson())));
            a4f.processRoot(false);
            ModelStats stats = new ModelStats(modelId, a4f);
            stats.processMetrics(catalog.getMethods());
            return stats;
        }
    }

    private static String setting(String property, String environment, String fallback) {
        return System.getProperty(property, System.getenv().getOrDefault(environment, fallback));
    }
}
