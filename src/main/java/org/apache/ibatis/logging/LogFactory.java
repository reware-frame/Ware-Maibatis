/*
 *    Copyright 2009-2013 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.logging;

import java.lang.reflect.Constructor;


/**
 * 日志工厂
 */
public final class LogFactory {

    /**
     * Marker to be used by logging implementations that support markers
     */
    // 给支持marker功能的logger使用(目前有slf4j, log4j2)
    public static final String MARKER = "MYBATIS";

    // 具体究竟用哪个日志框架，那个框架所对应logger的构造函数
    private static Constructor<? extends Log> logConstructor;

    /**
     * 单线程顺序检查有无相应的日志jar包，若有，则加载并退出
     */
    static {
        //slf4j
        tryImplementation(new Runnable() {
            @Override
            public void run() {
                useSlf4jLogging();
            }
        });
        //common logging
        tryImplementation(new Runnable() {
            @Override
            public void run() {
                useCommonsLogging();
            }
        });
        //log4j2
        tryImplementation(new Runnable() {
            @Override
            public void run() {
                useLog4J2Logging();
            }
        });
        //log4j
        tryImplementation(new Runnable() {
            @Override
            public void run() {
                useLog4JLogging();
            }
        });
        //jdk logging
        tryImplementation(new Runnable() {
            @Override
            public void run() {
                useJdkLogging();
            }
        });
        //没有日志
        tryImplementation(new Runnable() {
            @Override
            public void run() {
                useNoLogging();
            }
        });
    }

    // 单例模式，不得自己new实例
    private LogFactory() {
        // disable construction
    }

    // 根据传入的类来构建Log
    public static Log getLog(Class<?> aClass) {
        return getLog(aClass.getName());
    }

    // 根据传入的类名来构建Log
    public static Log getLog(String logger) {
        try {
            // 构造函数，参数必须是一个，为String型，指明logger的名称
            return logConstructor.newInstance(logger);
        } catch (Throwable t) {
            throw new LogException("Error creating logger for logger " + logger + ".  Cause: " + t, t);
        }
    }

    // 提供一个扩展功能，如果以上log都不满意，可以使用自定义的log
    public static synchronized void useCustomLogging(Class<? extends Log> clazz) {
        setImplementation(clazz);
    }

    public static synchronized void useSlf4jLogging() {
        setImplementation(org.apache.ibatis.logging.slf4j.Slf4jImpl.class);
    }

    public static synchronized void useCommonsLogging() {
        setImplementation(org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl.class);
    }

    public static synchronized void useLog4JLogging() {
        setImplementation(org.apache.ibatis.logging.log4j.Log4jImpl.class);
    }

    public static synchronized void useLog4J2Logging() {
        setImplementation(org.apache.ibatis.logging.log4j2.Log4j2Impl.class);
    }

    public static synchronized void useJdkLogging() {
        setImplementation(org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl.class);
    }

    // 这个没用到
    public static synchronized void useStdOutLogging() {
        setImplementation(org.apache.ibatis.logging.stdout.StdOutImpl.class);
    }

    public static synchronized void useNoLogging() {
        setImplementation(org.apache.ibatis.logging.nologging.NoLoggingImpl.class);
    }

    /**
     * 传入Runnable，但使用run方法，看似多线程实则单线程
     */
    private static void tryImplementation(Runnable runnable) {
        if (logConstructor == null) {
            try {
                // 这里调用的不是start,而是run！根本就没用多线程嘛！
                runnable.run();
            } catch (Throwable t) {
                // ignore
            }
        }
    }

    /**
     * 尝试加载预定的日志，通过构造函数反射寻找jar包，若存在则加载成功
     */
    private static void setImplementation(Class<? extends Log> implClass) {
        try {
            // String参数类型的构造函数
            Constructor<? extends Log> candidate = implClass.getConstructor(String.class);
            // 通过名称进行加载
            Log log = candidate.newInstance(LogFactory.class.getName());
            if (log.isDebugEnabled()) {
                log.debug("Logging initialized using '" + implClass + "' adapter.");
            }
            // 设置logConstructor,一旦设上，表明找到相应的log的jar包了，那后面别的log就不找了。
            logConstructor = candidate;
        } catch (Throwable t) {
            throw new LogException("Error setting Log implementation.  Cause: " + t, t);
        }
    }

}
