package pt.haslab.alloy4fun;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.alloy4viz.AlloyAtom;
import edu.mit.csail.sdg.alloy4viz.AlloyInstance;
import edu.mit.csail.sdg.alloy4viz.AlloyRelation;
import edu.mit.csail.sdg.alloy4viz.AlloySet;
import edu.mit.csail.sdg.alloy4viz.AlloyTuple;
import edu.mit.csail.sdg.alloy4viz.AlloyType;
import edu.mit.csail.sdg.alloy4viz.StaticInstanceReader;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;
import pt.haslab.alloy4fun.utils.InstancesRequest;

@Path("/getInstances")
public class AlloyGetInstances {

	private static Logger LOGGER = LoggerFactory.getLogger(AlloyGetInstances.class);
	
	@POST
	@Produces("text/json")
	public Response doGet(String body) throws IOException, Err {
		InstancesRequest req = parseJSON(body);
		LOGGER.info("Received request for session: "+req.sessionId);
		LOGGER.debug("Parent session: "+req.parentId);
		String res = "";
		List<ErrorWarning> warnings = new ArrayList<ErrorWarning>();

		// parent session open, close it
		if (RestApplication.alive(req.parentId)) {
			LOGGER.info("Found the parent session alive ("+req.parentId+").");
			// TODO: this does not shutdown the timeout thread waiting to remove, thread will remain until timeout
			RestApplication.remove(req.parentId);
		}

		// session open, recover solution object
		if (RestApplication.alive(req.sessionId)) { 
			LOGGER.info("Found the current session alive ("+req.sessionId+").");
			res = batchAdd(req,warnings);
		}
		// create new solving session
		else {
			LOGGER.info("Creating a new session ("+req.parentId+").");

			A4Reporter rep = new A4Reporter() {
				public void warning (ErrorWarning msg) {
					warnings.add(msg);
	   			}
			};
			CompModule world;

			try {
	            File tmpAls = File.createTempFile("alloy_heredoc", ".als");
	            tmpAls.deleteOnExit();
	            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tmpAls));
	            bos.write(req.model.getBytes());
	            bos.flush();
	            bos.close();
				world = CompUtil.parseEverything_fromFile(rep, null, tmpAls.getAbsolutePath());		
				tmpAls.deleteOnExit();
			} catch (Err e) {
				LOGGER.info("Alloy errored during model parsing: "+e.getMessage());
				JsonObjectBuilder instanceJSON = Json.createObjectBuilder();
				instanceJSON.add("alloy_error", true);
				instanceJSON.add("msg", e.msg);
				instanceJSON.add("line", e.pos.y);
				instanceJSON.add("column", e.pos.x);
				instanceJSON.add("line2", e.pos.y2);
				instanceJSON.add("column2", e.pos.x2);
				LOGGER.info("Responding with error message.");
				return Response.ok(instanceJSON.build().toString()).build();
			} catch (IOException e) {
				LOGGER.error("IO error during model parsing.",e);
				JsonObjectBuilder instanceJSON = Json.createObjectBuilder();
				instanceJSON.add("alloy_error", true);
				instanceJSON.add("msg", e.getMessage());
				LOGGER.info("Responding with error message.");
				return Response.ok(instanceJSON.build().toString()).build();
			}
			
			A4Options opt = new A4Options();
			opt.originalFilename = "alloy_heredoc.als";
			opt.solver = A4Options.SatSolver.SAT4J;
			Command command = world.getAllCommands().get(req.commandIndex);
			try {
				A4Solution ans = TranslateAlloyToKodkod.execute_command(rep, world.getAllReachableSigs(), command, opt);
				RestApplication.add(req.sessionId,ans,command,world.getAllFunc());

				res = batchAdd(req,warnings);

			} catch (Err e) {
				LOGGER.info("Alloy errored during solving: ",e.getMessage());
				JsonObjectBuilder instanceJSON = Json.createObjectBuilder();
				instanceJSON.add("alloy_error", true);
				instanceJSON.add("msg", e.msg);
				instanceJSON.add("line", e.pos.y);
				instanceJSON.add("column", e.pos.x);
				instanceJSON.add("line2", e.pos.y2);
				instanceJSON.add("column2", e.pos.x2);
				LOGGER.info("Responding with error message.");
				return Response.ok(instanceJSON.build().toString()).build();
			}
		}
		LOGGER.info("Responding with solutions.");
		return Response.ok(res).build();
	}

	static private String batchAdd(InstancesRequest req, List<ErrorWarning> warnings) throws Err {
		JsonArrayBuilder solsArrayJSON = Json.createArrayBuilder();
		A4Solution ans = RestApplication.getSol(req.sessionId);
		Command cmd = RestApplication.getCommand(req.sessionId);
		int cnt = RestApplication.getCnt(req.sessionId);
		Iterable<Func> skolems = RestApplication.getSkolem(req.sessionId);
		for (int n = 0; n < req.numberOfInstances && ans.satisfiable(); n++) {
			solsArrayJSON.add(answerToJson(req.sessionId, ans, skolems, warnings, cmd, cnt));
			RestApplication.next(req.sessionId);
			ans = RestApplication.getSol(req.sessionId);
			cnt = RestApplication.getCnt(req.sessionId);
		}
		if (!ans.satisfiable())
			solsArrayJSON.add(answerToJson(req.sessionId, ans, skolems, warnings, cmd, cnt));
		String res = solsArrayJSON.build().toString();

		return res;
	}

	/**
	 * Parses the JSON instance requires.
	 * @param body the request body
	 * @return the parsed request
	 */
	static private InstancesRequest parseJSON(String body) {
		JSONObject jo = new JSONObject(body);
		InstancesRequest req = new InstancesRequest();

		req.model = jo.getString("model");
		req.numberOfInstances = jo.getInt("numberOfInstances");
		req.commandIndex = jo.getInt("commandIndex");
		req.sessionId = jo.getString("sessionId");
		req.parentId = jo.getString("parentId");

		return req;
	}

	/**
	 * Converts an Alloy solution into JSON for the response. Also reports Alloy warning.
	 * @param sessionId
	 * @param answer
	 * @param warns
	 * @param cmd
	 * @param cnt
	 * @return
	 */
	static private JsonObject answerToJson(String sessionId, A4Solution answer, Iterable<Func> skolems, List<ErrorWarning> warns, Command cmd, int cnt) {
		JsonObjectBuilder instanceJSON = Json.createObjectBuilder();

		if (warns.size() > 0) {
			instanceJSON.add("warning_error", true);
			instanceJSON.add("msg", warns.get(0).msg);
			instanceJSON.add("line", warns.get(0).pos.y);
			instanceJSON.add("column", warns.get(0).pos.x);
			instanceJSON.add("line2", warns.get(0).pos.y2);
			instanceJSON.add("column2", warns.get(0).pos.x2);
		}

		instanceJSON.add("sessionId", sessionId.toString());
		instanceJSON.add("unsat", !answer.satisfiable());
		instanceJSON.add("check", cmd.check);
		instanceJSON.add("cmd_n", cmd.label);
		instanceJSON.add("cnt", cnt);
		instanceJSON.add("static", answer.getMaxTrace() < 0);

		if (answer.satisfiable()) {
			instanceJSON.add("loop", answer.getLoopState());

			JsonArrayBuilder traceJSON = Json.createArrayBuilder();

			try {
				File tempFile = File.createTempFile("a4f", "als");
				tempFile.deleteOnExit();
				answer.writeXML(tempFile.getAbsolutePath(),skolems);
				for (int i = 0; i < answer.getTraceLength(); i++) {
					AlloyInstance instance = StaticInstanceReader.parseInstance(tempFile.getAbsoluteFile(),i);
					traceJSON.add(instanceToJSONObject(instance));
				}
			} catch (Err e) {
				LOGGER.error("Alloy errored during solution parsing.",e);
				JsonObjectBuilder errorJSON = Json.createObjectBuilder();
				errorJSON.add("err", String.format("Evaluator error occurred: %s", e));
				return errorJSON.build();
			} catch (IOException e) {
				LOGGER.error("IO error during solution parsing.",e);
			}
			
			instanceJSON.add("instance", traceJSON);
		}

		return instanceJSON.build();
	}

	/**
	 * Converts a particular Alloy instance into JSON.
	 * @param instance the Alloy JSON
	 * @return the JSON representation of the instance
	 */
	static JsonObjectBuilder instanceToJSONObject(AlloyInstance instance) {
		
		JsonObjectBuilder stateJSON = Json.createObjectBuilder();
		JsonArrayBuilder typesJSON = Json.createArrayBuilder();
		JsonArrayBuilder setsJSON = Json.createArrayBuilder();
		JsonArrayBuilder relsJSON = Json.createArrayBuilder();

		for (AlloyType tp : instance.model.getTypes())
			typesJSON.add(sigToJSON(instance, tp));
		for (AlloySet st : instance.model.getSets())
			setsJSON.add(setToJSON(instance, st));
		for (AlloyRelation rl : instance.model.getRelations())
			relsJSON.add(relToJSON(instance, rl));

		stateJSON.add("types", typesJSON);
		stateJSON.add("sets", setsJSON);
		stateJSON.add("rels", relsJSON);
		
		return stateJSON;
	}

	static private JsonObjectBuilder sigToJSON(AlloyInstance answer, AlloyType signature) {
		JsonObjectBuilder atomJSON = Json.createObjectBuilder();
		atomJSON.add("name", signature.getName());
		AlloyType pr = answer.model.getSuperType(signature);
		atomJSON.add("parent", pr == null ? "null" : pr.toString());
		
		JsonArrayBuilder instancesJSON = Json.createArrayBuilder();
		for (AlloyAtom at : answer.type2atoms(signature)) {
			// test if most specific sig
			if (at.getType().equals(signature))
				instancesJSON.add(at.toString());
		}

    	atomJSON.add("atoms", instancesJSON);

    	return atomJSON;
	}
	
	static private JsonObjectBuilder setToJSON(AlloyInstance answer, AlloySet signature) {
		JsonObjectBuilder atomJSON = Json.createObjectBuilder();
		atomJSON.add("name", signature.getName());
		atomJSON.add("parent", signature.getType().getName());
		
		JsonArrayBuilder instancesJSON = Json.createArrayBuilder();
		for (AlloyAtom at : answer.set2atoms(signature))
			instancesJSON.add(at.toString());

    	atomJSON.add("atoms", instancesJSON);

    	return atomJSON;
	}
	
	static private JsonObjectBuilder relToJSON(AlloyInstance answer, AlloyRelation rel) {
		JsonObjectBuilder atomJSON = Json.createObjectBuilder();
		atomJSON.add("name", rel.getName());
		
		JsonArrayBuilder instancesJSON = Json.createArrayBuilder();
		for (AlloyTuple atts : answer.relation2tuples(rel)) {
			JsonArrayBuilder tuple = Json.createArrayBuilder();
			for (AlloyAtom at : atts.getAtoms())
				tuple.add(at.toString());
			instancesJSON.add(tuple);
		}

    	atomJSON.add("atoms", instancesJSON);

    	return atomJSON;
	}
}
