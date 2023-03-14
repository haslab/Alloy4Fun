package pt.haslab.alloy4fun.metrics.utils;

public abstract class NumberMultiSet<T extends Number> extends MultiSet<T> {
	
	abstract public Double avg();

	abstract public T sum();

	abstract public T min();

	abstract public T max();
}
