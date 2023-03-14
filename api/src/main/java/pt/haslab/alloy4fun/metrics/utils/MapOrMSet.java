package pt.haslab.alloy4fun.metrics.utils;

import java.util.HashMap;
import java.util.Map;

public class MapOrMSet<T> {
	public final Map<Object,MapOrMSet<T>> map = new HashMap<Object,MapOrMSet<T>>();
	public final MultiSet<T> obj;
	
	boolean leaf() { return map.isEmpty(); }
	
	// returns children or creates it
	MapOrMSet<T> children(Object c, Class<T> t) {
		return map.computeIfAbsent(c, k -> new MapOrMSet<T>(t));
	}

	public MapOrMSet<T> children(Object c) {
		return map.computeIfAbsent(c, k -> new MapOrMSet<T>());
	}

	public MapOrMSet() {
		this.obj = new MultiSet<T>();
	}

	public MapOrMSet(Class<T> obj) {
		this.obj = MultiSet.factory(obj);
	}
	
	public MapOrMSet(T i) {
		this.obj = new MultiSet<T>();
		this.obj.add(i);
	}

	public int level() {
		if (leaf()) return 0;
		else if (map.isEmpty()) return 1;
		else return 1 + map.values().iterator().next().level();
	}

	public MapOrMSet<T> min() {
		MapOrMSet<T> res = new MapOrMSet<T>();
		if (!leaf()) {
			for (Object c : map.keySet()) 
				res.map.put(c,map.get(c).min());
		}
		else {
			T f = obj.min();
			MapOrMSet<T> c = res.children("min",(Class<T>) f.getClass());
			c.obj.add(f);
			obj.clear();
		}
		return res;
	}

	public MapOrMSet<T> max() {
		MapOrMSet<T> res = new MapOrMSet<T>();
		if (!leaf()) {
			for (Object c : map.keySet()) 
				res.map.put(c,map.get(c).max());
		}
		else {
			T f = obj.max();
			MapOrMSet<T> c = res.children("max",(Class<T>) f.getClass());
			c.obj.add(f);
			obj.clear();
		}
		return res;
	}
	
	public MapOrMSet<Double> avg() {
		MapOrMSet<Double> res = new MapOrMSet<Double>(Double.class);
		if (!leaf()) {
			for (Object c : map.keySet()) 
				res.map.put(c,map.get(c).avg());
		}
		else {
			Double f = obj.avg();
			MapOrMSet<Double> c = res.children("avg",Double.class);
			c.obj.add(f);
			obj.clear();
		}
		return res;
	}
	
	public MapOrMSet<T> sum() {
		MapOrMSet<T> res = new MapOrMSet<T>();
		if (!leaf()) {
			for (Object c : map.keySet()) 
				res.map.put(c,map.get(c).sum());
		}
		else {
			T f = obj.sum();
			MapOrMSet<T> c = res.children("sum",(Class<T>) f.getClass());
			c.obj.add(f);
			obj.clear();
		}
		return res;
	}

	public MapOrMSet<Integer> count() {
		MapOrMSet<Integer> res = new MapOrMSet<Integer>(Integer.class);
		if (!leaf()) {
			for (Object c : map.keySet()) 
				res.map.put(c,map.get(c).count());
		}
		else {
			for (T o : obj.elems()) {
				MapOrMSet<Integer> c = res.children(o,Integer.class);
				c.obj.add(obj.count(o));
			}
			obj.clear();
		}
		return res;
	}
}