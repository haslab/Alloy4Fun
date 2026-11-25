package pt.haslab.alloy4fun.metrics.utils;

import java.util.Collections;

public class DoubleMultiSet extends NumberMultiSet<Double> {

	
	public Double avg() {
		return elems.entrySet().stream().flatMap(k -> Collections.nCopies(k.getValue(), k.getKey()).stream()).mapToDouble(i -> i).average().getAsDouble();
	}

	public Double sum() {
		return elems.entrySet().stream().map(k -> k.getKey()*k.getValue()).mapToDouble(i -> i).sum();
	}

	public Double min() {
		return elems.keySet().stream().mapToDouble(i -> i).min().getAsDouble();
	}

	public Double max() {
		return elems.keySet().stream().mapToDouble(i -> i).max().getAsDouble();
	}
}
