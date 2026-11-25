package pt.haslab.alloy4fun.datamodel;

import org.json.JSONObject;

import edu.mit.csail.sdg.ast.Command;

public class A4FExecution extends A4FModel {

	private RESULT res;
	public String msg;
	public final String cmd_name;
	public final int cmd_index;
	public final Boolean cmd_check;
	private Command cmd;

	A4FExecution(JSONObject obj) {

		super(obj);
		if (obj.getInt("sat") == 0)
			res = RESULT.UNSAT;
		else if (obj.getInt("sat") == 1)
			res = RESULT.SAT;
		else if (obj.getInt("sat") == -1)
			res = RESULT.ERROR;
		else
			res = RESULT.UNKNOWN;

		msg = obj.has("msg") ? obj.getString("msg") : null;
		cmd_name = obj.has("cmd_n") ? obj.getString("cmd_n") : null;
		cmd_index = obj.has("cmd_i") ? obj.getInt("cmd_i") : -1;
		cmd_check = obj.has("cmd_c") ? obj.getBoolean("cmd_c") : null;
	}

	public enum RESULT {
		ERROR, UNSAT, SAT, UNKNOWN;
	}

	public void setCommand(Command cmd) {
		this.cmd = cmd;
	}
	
	public RESULT result() {
		return res;
	}
	
	public Command command() {
		return cmd;
	}


}
