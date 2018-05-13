package cn.xpleaf.rpc.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * RPC服务注解，标注在服务实现类在即可通过minidubbo发布服务
 *
 * @author yeyonghao
 */
@Target(value = ElementType.TYPE)        // 自定义注解的使用范围，ElementType.TYPE表示自定义的注解可以用在类或接口上
@Retention(RetentionPolicy.RUNTIME)    // 注解的可见范围，RetentionPolicy.RUNTIME表示自定义注解在虚拟机运行期间也可见
@Component                            // 让spring可以扫描到此注解
public @interface RPCService {

    // 自定义注解的参数类型为类型对象，使用该注解时，需要传入实现类的接口对象
    Class<?> value();
}
