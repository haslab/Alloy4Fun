package pt.haslab.alloy4fun.datamodel;

import org.json.JSONObject;

public class A4FEntry {

	public final String id;
	
	A4FEntry (JSONObject obj) {
		this.id = obj.getString("_id");
	}
}
