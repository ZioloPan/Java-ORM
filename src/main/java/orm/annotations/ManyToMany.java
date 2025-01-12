package orm.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ManyToMany {
    String joinTable();
    String joinColumn();
    String inverseJoinColumn();
}
