package pt.haslab.alloy4fun.metrics;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MetricMethod {

	String rule() default "";
	String description() default "";
	int group() default 999;
	GROUPBY groupby() default GROUPBY.COUNT;
	
	enum GROUPBY { SUM, COUNT, AVG, MAX, MIN };

}
