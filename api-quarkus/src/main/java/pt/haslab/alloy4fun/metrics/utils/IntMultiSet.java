package pt.haslab.alloy4fun.metrics.utils;

import java.util.Collections;

public class IntMultiSet extends NumberMultiSet<Integer> {
	
	public Double avg() {
		return elems.entrySet().stream().flatMap(k -> Collections.nCopies(k.getValue(), k.getKey()).stream()).mapToInt(i -> i).average().getAsDouble();
	}

	public Integer sum() {
		return elems.entrySet().stream().map(k -> k.getKey()*k.getValue()).mapToInt(i -> i).sum();
	}

	public Integer min() {
		return elems.keySet().stream().mapToInt(i -> i).min().getAsInt();
	}

	public Integer max() {
		return elems.keySet().stream().mapToInt(i -> i).max().getAsInt();
	}

}
