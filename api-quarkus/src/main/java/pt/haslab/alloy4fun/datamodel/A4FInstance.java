package pt.haslab.alloy4fun.datamodel;

import org.json.JSONObject;

public class A4FInstance extends A4FEntry {

	public final String model_entry;
	public final JSONObject graph;

	public A4FInstance(JSONObject obj) {
		super(obj);
		this.model_entry = obj.getString("model_id");
		this.graph = obj.getJSONObject("graph");
	}
	
}
