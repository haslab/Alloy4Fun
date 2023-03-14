package pt.haslab.alloy4fun.datamodel;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.time.format.DateTimeFormatter;

import org.json.JSONObject;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorWarning;

abstract public class A4FModel extends A4FEntry {

	public final String parent_entry;
	public final String root_entry;
	public final LocalDateTime time;
	public final String id;
	public final String code;
	public Err error = null;
	public final Set<ErrorWarning> wns = new HashSet<>();
	A4FModel parent = null;
    public final List<A4FModel> children;

	static public A4FModel fromJSON(JSONObject obj) {
		if (obj.has("theme"))
			return new A4FShare(obj);
		else 
			return new A4FExecution(obj);
	}

	A4FModel(JSONObject obj) {
		super(obj);
		this.time = strToTime(obj.getString("time"));
		this.parent_entry = !obj.isNull("derivationOf") ? obj.getString("derivationOf") : null;
		this.root_entry = obj.getString("original");
		this.id = obj.getString("_id");
        this.children = new LinkedList<A4FModel>();
		this.code = obj.getString("code");
	}

    public void setParent(A4FModel parent) {
    	this.parent = parent;
        parent.children.add(this);
    }

	public List<A4FModel> children() {
		children.sort((c1,c2) -> c1.time.compareTo(c2.time));
		return children;
	}
	
	public void addWarning(ErrorWarning e) {
		wns.add(e);
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
	
	
	public List<A4FModel> childrenCmd(String chl) {
		List<A4FModel> mdls = new ArrayList<>();
		for (A4FModel c : children) {
			if (c instanceof A4FExecution && ((A4FExecution) c).cmd_name != null && ((A4FExecution) c).cmd_name.equals(chl)) 
				mdls.add(c);
			else
				mdls.addAll(c.childrenCmd(chl));
		}
		
		return mdls;
	}

	
}
