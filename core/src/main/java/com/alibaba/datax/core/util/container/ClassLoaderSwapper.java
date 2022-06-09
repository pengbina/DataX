package com.alibaba.datax.core.util.container;

/**
 * Created by jingxing on 14-8-29.
 *
 * 为避免jar冲突，比如hbase可能有多个版本的读写依赖jar包，JobContainer和TaskGroupContainer
 * 就需要脱离当前classLoader去加载这些jar包，执行完成后，又退回到原来classLoader上继续执行接下来的代码
 *
 * ClassLoaderSwapper 类加载器管理者
 * 描述：这个类主要配合LoadUtil使用，主要管理不同线程（即：Task任务）的类加载器。
 * 与LoadUtil配合使用的方式（这里说的是线程，但是官方文档说开了个进程，感觉不太合适）：
 *
 * // 初始化ClassLoaderSwapper
 * private ClassLoaderSwapper classLoaderSwapper
 * = ClassLoaderSwapper.newCurrentThreadClassLoaderSwapper();
 *
 * // 注入插件到ClassLoaderSwapper
 * classLoaderSwapper.setCurrentThreadClassLoader(
 * 	LoadUtil.getJarLoader(PluginType.READER, this.readerPluginName)
 * );
 */
public final class ClassLoaderSwapper {
    private ClassLoader storeClassLoader = null;

    private ClassLoaderSwapper() {
    }

    public static ClassLoaderSwapper newCurrentThreadClassLoaderSwapper() {
        return new ClassLoaderSwapper();
    }

    /**
     * 保存当前classLoader，并将当前线程的classLoader设置为所给classLoader
     *
     * @param
     * @return
     */
    public ClassLoader setCurrentThreadClassLoader(ClassLoader classLoader) {
        this.storeClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        return this.storeClassLoader;
    }

    /**
     * 将当前线程的类加载器设置为保存的类加载
     * @return
     */
    public ClassLoader restoreCurrentThreadClassLoader() {
        ClassLoader classLoader = Thread.currentThread()
                .getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.storeClassLoader);
        return classLoader;
    }
}
