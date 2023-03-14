package pt.haslab.alloy4fun.datamodel;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.json.JSONObject;

public class A4FNavigation extends A4FEntry {

	public final String model_entry;
	public final int operation;
	public final int index;
	public final LocalDateTime time;


	public A4FNavigation(JSONObject obj) {
		super(obj);
		this.time = strToTime(obj.getString("time"));
		this.model_entry = obj.getString("model_id");
		this.operation = obj.getInt("operation");
		this.index = obj.getInt("instIndex");
	}

	public static LocalDateTime strToTime(String strtime) {
		LocalDateTime res;
		try {
			DateTimeFormatter format = DateTimeFormatter.ofPattern("M/d/yyyy, h:mm:ss a");
			res = LocalDateTime.from(format.parse(strtime));
		} catch (DateTimeException ex) {
			DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-M-d HH:mm:ss");
			res = LocalDateTime.from(format.parse(strtime));
		}
		return res;
	}
}
