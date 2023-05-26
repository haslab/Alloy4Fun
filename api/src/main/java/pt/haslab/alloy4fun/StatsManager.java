package pt.haslab.alloy4fun;

import static com.mongodb.client.model.Filters.eq;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.bson.Document;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;

import pt.haslab.alloy4fun.datamodel.A4FDatabase;
import pt.haslab.alloy4fun.datamodel.A4FInstance;
import pt.haslab.alloy4fun.datamodel.A4FLink;
import pt.haslab.alloy4fun.datamodel.A4FModel;
import pt.haslab.alloy4fun.datamodel.A4FNavigation;
import pt.haslab.alloy4fun.metrics.ModelStats;

@ApplicationPath("/")
public class StatsManager extends Application {
	
	private static int TIMEOUT = 600;
	private static int MAXAGE = 1000*60*60*24;

	private static Logger LOGGER = LoggerFactory.getLogger(StatsManager.class);
	
	private static Map<String,Future<ModelStats>> running = new HashMap<>();
	private static Map<String,LocalDateTime> last = new HashMap<>();

	private static ExecutorService executorService = Executors.newFixedThreadPool(4);
	
	private static Future<Void> cachecleaner;

	public static ModelStats getStats(String req, Class<?> catalog) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, SecurityException, InterruptedException, ExecutionException, TimeoutException {

		Future<ModelStats> res = running.computeIfAbsent(req, z -> executorService.submit(() -> {
			MongoClient mongoClient = new MongoClient("localhost", 3001);
			MongoDatabase database = mongoClient.getDatabase("meteor");
			FindIterable<Document> models = database.getCollection("Model").find(eq("original", req));
			FindIterable<Document> insts = database.getCollection("Instance").find();
			FindIterable<Document> links = database.getCollection("Link").find();
			FindIterable<Document> navs = database.getCollection("Navigation").find();
			
			A4FDatabase a4f = new A4FDatabase(req);

			for (Document model : models)
				a4f.addModel(A4FModel.fromJSON(new JSONObject(model.toJson())));
			
			for (Document link : links)
				a4f.addLink(new A4FLink(new JSONObject(link.toJson())));

			for (Document inst : insts)
				a4f.addInstance(new A4FInstance(new JSONObject(inst.toJson())));

			for (Document nav : navs)
				a4f.addNavigation(new A4FNavigation(new JSONObject(nav.toJson())));
			
			mongoClient.close();

			LOGGER.debug("# models: "+a4f.models().size());
			LOGGER.debug("# links: "+a4f.links().size());
			LOGGER.debug("# instances: "+a4f.instances().size());
			LOGGER.debug("# navigations: "+a4f.navigations().values().stream().mapToInt(x -> x.size()).sum());

			LOGGER.info("Running stats for "+req+".");
			a4f.processRoot(false);
			ModelStats stats = new ModelStats(req, a4f);
			stats.processMetrics(catalog.getMethods());
			last.put(req, LocalDateTime.now());
			return stats;
		}));
		
		if (cachecleaner == null) {
			cachecleaner = executorService.submit(() -> {
				while (true) {
					Thread.sleep(MAXAGE);
					LOGGER.info("Cleaning up stat caches.");
					running.clear();
					last.clear();
				}
			});
		}
		
    	return res.get(TIMEOUT,TimeUnit.MINUTES);
	}
	
	
	public StatsManager() {
    	
    }
}
