package pt.haslab.alloy4fun.utils;


import java.util.List;
import java.util.Map;

import pt.haslab.alloy4fun.datamodel.A4FInstance;
import pt.haslab.alloy4fun.datamodel.A4FModel;
import pt.haslab.alloy4fun.datamodel.A4FNavigation;
import pt.haslab.alloy4fun.datamodel.A4FLink;

import java.util.ArrayList;
import java.util.HashMap;

public class StatsRequest {
	public final String model;
	public final Map<String,A4FModel> models = new HashMap<>();
	public final List<A4FLink> links = new ArrayList<>();
	public final List<A4FInstance> instances = new ArrayList<>();
	public final List<A4FNavigation> navigations = new ArrayList<>();
	
	public StatsRequest(String model) {
		this.model = model;
	}
}
