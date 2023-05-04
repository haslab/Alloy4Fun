package pt.haslab.alloy4fun;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4viz.AlloyAtom;
import edu.mit.csail.sdg.alloy4viz.AlloyInstance;
import edu.mit.csail.sdg.alloy4viz.AlloyProjection;
import edu.mit.csail.sdg.alloy4viz.AlloyType;
import edu.mit.csail.sdg.alloy4viz.StaticInstanceReader;
import edu.mit.csail.sdg.alloy4viz.StaticProjector;
import edu.mit.csail.sdg.alloy4viz.VizState;
import edu.mit.csail.sdg.translator.A4Solution;

@Path("/getProjection")
public class AlloyGetProjection {
	private static Logger LOGGER = LoggerFactory.getLogger(AlloyGetProjection.class);
	
	@POST
	@Produces("text/json")
	public Response doGet(String body) throws IOException {
		String res = "";
		Request req;
		try {
			req = parseJSON(body);
			A4Solution sol = SolutionManager.getSol(req.uuid,req.index);
			File tempFile = File.createTempFile("a4f", "als");
			tempFile.deleteOnExit();
			LOGGER.info("Projecting "+req.type+" at "+req.index);
			sol.writeXML(tempFile.getAbsolutePath());
			AlloyInstance myInstance = StaticInstanceReader.parseInstance(tempFile.getAbsoluteFile(),0);
			
			JsonArrayBuilder jsonResponseBuilder = Json.createArrayBuilder();
			
			VizState myState = new VizState(myInstance);
			//clonamos o myState para obter um theme a usar abaixo para obter o originalName
			VizState theme= new VizState(myState);
			theme.useOriginalName(true);
			
			Map<AlloyType, AlloyAtom> map = new LinkedHashMap<AlloyType, AlloyAtom>();
			for (AlloyAtom alloy_atom : myState.getOriginalInstance().getAllAtoms()) {
				for (Object projectingType : req.type) {	
					String pt = (String) projectingType;
					if (alloy_atom.getVizName(theme, true).replace("$","").equals(pt))
						map.put(alloy_atom.getType(), alloy_atom);
				}   
			}
			
			AlloyProjection currentProjection = new AlloyProjection(map);
			AlloyInstance projected = StaticProjector.project(myInstance, currentProjection);
			LOGGER.debug(projected.toString());
			jsonResponseBuilder.add(AlloyGetInstances.instanceToJSONObject(projected));
			
			res = jsonResponseBuilder.build().toString();
			
			
		} catch (Err e) {
			LOGGER.error("Alloy errored during solution parsing.",e);
		}
		

		return Response.ok(res).build();
	}
	
	private Request parseJSON(String body) {

		JSONObject jo = new JSONObject(body);
		String uuid = jo.getString("sessionId");
		JSONArray typesArray = jo.getJSONArray("type");
		List<Object> types = typesArray.toList();
		int indx = jo.getInt("index");
	
		Request req = new Request(uuid,types,indx);

		return req;
	}
	
}

class Request {
   final String uuid;
   final List<Object> type;
   final int index;
   
   Request(String u, List<Object> t, int i) {
   		uuid = u;
   		type = t;
   		index = i;
   }
  
}