package jabaclass.user.common.apidocs;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorSpec {
	Class<? extends Enum<?>> value();
	String constant();
	String name() default "";
	String summary() default "";
	String description() default "";
}