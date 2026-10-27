package com.sky.context;

// 存放上下文类
public class BaseContext {
    // ThreadLocal 是 Java 中用于线程本地变量的工具类。
    // 它的核心作用是：为每个线程提供独立的变量副本，避免多线程之间的数据共享和竞争

    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }

}
