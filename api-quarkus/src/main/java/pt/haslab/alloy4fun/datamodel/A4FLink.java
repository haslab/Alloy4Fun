package pt.haslab.alloy4fun.datamodel;

import org.json.JSONObject;

public class A4FLink extends A4FEntry {

	public final String model_entry;
	public final boolean private_link;

	public A4FLink(JSONObject obj) {
		super(obj);
		this.model_entry = obj.getString("model_id");
		this.private_link = obj.getBoolean("private");
	}

}
