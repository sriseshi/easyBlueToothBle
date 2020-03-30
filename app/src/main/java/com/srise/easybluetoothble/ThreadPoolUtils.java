package com.srise.easybluetoothble;

import android.os.Handler;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolUtils {
    public static final String TAG = "ThreadPoolUtils";

    public static final int POOL_SIZE = 2;
    static ThreadPoolExecutor sPool;

    static Handler sHandler = new Handler();


    static {
        sPool = new ThreadPoolExecutor(POOL_SIZE, POOL_SIZE,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
        sPool.setThreadFactory(new MagnetThreadFactory());
    }

    public static void run(Runnable runnable) {
        sPool.execute(runnable);
    }

    public static <V> Future<V> submit(Callable<V> callable) {
        return sPool.submit(callable);
    }

    public static ExecutorService getThreadPool() {
        return sPool;
    }

    private static class MagnetThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        MagnetThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = "Magnet:pool-" +
                    poolNumber.getAndIncrement() +
                    "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }


    public static void runOnUi(Runnable runnable) {
        sHandler.post(runnable);
    }

    public static void runOnUiDelay(Runnable runnable, long time) {
        sHandler.postDelayed(runnable, time);
    }

    public static void removeTask(Runnable runnable) {
        sHandler.removeCallbacks(runnable);
    }

    public static void removeAllTask() {
        sHandler.removeCallbacksAndMessages(null);
    }
}