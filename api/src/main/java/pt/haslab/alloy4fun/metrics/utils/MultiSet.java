package pt.haslab.alloy4fun.metrics.utils;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MultiSet<T> {

    Map<T,Integer> elems;
    
    public static <Q> MultiSet<Q> factory(Class<Q> t) {
    	if (t.equals(Integer.class))
    		return (MultiSet<Q>) new IntMultiSet();
    	else if (t.equals(Double.class))
    		return (MultiSet<Q>) new DoubleMultiSet();
    	return new MultiSet<>();
    }
    
    public MultiSet() {
    	elems = new HashMap<>();
    }
    
    public void add(T elem) {
    	elems.merge(elem, 1, (i,j)->i+1);
    }
    
    public boolean remove(T elem) {
    	if (elems.getOrDefault(elem, 0) < 1)
    		return false;
    	else {
    		elems.computeIfPresent(elem, (i,j) -> j-1);
    		return true;
    	}
    }
    
    public int count(T elem) {
    	return elems.getOrDefault(elem, 0);
    }

    public Set<T> elems() {
    	return elems.keySet();
    }
    
    public int size() {
    	return elems.values().stream().mapToInt(i->i).sum();
    }
    
    public void merge(MultiSet<T> mset) {
    	mset.elems.forEach((key,value) -> elems.merge(key, value, (a,b) -> a+b));
    }

    public String toString() {
    	if (singleton()) return elems.keySet().iterator().next().toString();
    	StringBuilder sb = new StringBuilder();
    	for (T e : elems.keySet())
    		sb.append(e +"->"+elems.get(e)+", ");
    	return sb.toString();
    }

	public void clear() {
		elems.clear();
	}

	public boolean singleton() {
		return elems.size() == 1 && elems.values().iterator().next() == 1;
	}

	public Double avg() {
		throw new UnsupportedOperationException();
	}

	public T sum() {
		throw new UnsupportedOperationException();
	}

	public T min() {
		throw new UnsupportedOperationException();
	}

	public T max() {
		throw new UnsupportedOperationException();
	}
}