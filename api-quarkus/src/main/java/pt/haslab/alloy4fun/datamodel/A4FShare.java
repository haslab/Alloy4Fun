package pt.haslab.alloy4fun.datamodel;

import org.json.JSONObject;

public class A4FShare extends A4FModel {

	public final Boolean instance_share;
	public final JSONObject theme;

	A4FShare(JSONObject obj) {
		super(obj);
		instance_share = obj.has("sat");
		theme = obj.getJSONObject("theme");
	}

}
