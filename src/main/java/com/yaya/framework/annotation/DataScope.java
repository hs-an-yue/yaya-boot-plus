package com.yaya.framework.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限注解，只有添加了注解DataScope的mapper层函数才可以对数据进行授权
 * 数据权限 {@link com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor}
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DataScope {

    /*
     * SQL语句中如果部门表起了别名,这里要设置部门表别名
     * 如果不设置别名那位空
     */
    String deptAlias() default "";

    /*
     * 我们的数据权限说白了就是在WHERE后面拼接条件，指向部门表信息的列的属性名是什么，这里就设置成什么
     */
    String deptIdColumnName() default "dept_id";

    /*
     * SQL语句中如果用户表起了别名,这里要设置用户表别名
     * 在多表联查时，设置给哪个表添加权限，通过别名定位
     */
    String userAlias() default "";

    /*
     * 我们的数据权限说白了就是在WHERE后面拼接条件，指向部门表信息的列的属性名是什么，这里就设置成什么
     */
    String userIdColumnName() default "create_by";

}
