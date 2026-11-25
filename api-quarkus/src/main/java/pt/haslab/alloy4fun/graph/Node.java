package pt.haslab.alloy4fun.graph;

public class Node {
	
	public final String sat;
	public final String label;
	public int weight;

	public Node(String sat, String label) {
		this.sat = sat;
		this.label = label;
		this.weight = 0;
	}

	public void increase() {
		weight++;
	}
	

}
