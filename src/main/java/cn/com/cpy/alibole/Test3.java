package cn.com.cpy.alibole;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author chenpanyu
 * @version 1.0
 * @since 2018/10/17 20:01
 */
public class Test3 {

    // 模拟递归的次数
    private static ThreadLocal<Integer> local = new ThreadLocal<>();

    private AccessLimiterService limiterService = new AccessLimiterService();

    public static void main(String[] args) throws InterruptedException {
        Test3 test3 = new Test3();
        for (int i = 0; i < 2000; i++) {
            int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    local.set(2);
                    test3.writeSomething("1", "2");
                    local.remove();
                }
            }).start();
        }
        TimeUnit.SECONDS.sleep(60);
    }

    public String writeSomething(String key, String value) {
        try {
            if (limiterService.tryAcquire()) {
                // 模拟递归调用的代码
                if (local.get() != 0) {
                    local.set(local.get() - 1);
                    return writeSomething(key, value);
                }
                saveToDatabase(key, value);
                System.out.println(Thread.currentThread().getName() + "-success");
                return "success";
            }
            System.out.println(Thread.currentThread().getName() + "-failure");
            return "failure";
        } finally {
            limiterService.release();
        }
    }

    private void saveToDatabase(String key, String value) {
        if (ThreadLocalRandom.current().nextInt(2) == 0) {
            // 模拟处理耗时, 体现限流效果
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("saveToDatabase, key = " + key + ", value = " + value);
    }

    /**
     * 访问限流服务
     */
    public class AccessLimiterService {

        // 最大许可数
        private static final int MAX_PERMITS = 50;

        private Map<Thread, AccessLimiter> accessLimiterMap = new ConcurrentHashMap<>();

        /**
         * 尝试获取许可
         *
         * @return 是否获得
         */
        public synchronized boolean tryAcquire() {
            Thread current = Thread.currentThread();
            if (accessLimiterMap.containsKey(current)) {
                // 重入,获取limiter并重入次数加1
                accessLimiterMap.get(current).count.incrementAndGet();
                return true;
            } else {
                if (accessLimiterMap.size() < MAX_PERMITS) {
                    accessLimiterMap.put(current, new AccessLimiter(current));
                    return true;
                }
                return false;
            }
        }

        /**
         * 释放许可
         */
        public synchronized void release() {
            Thread current = Thread.currentThread();
            if (accessLimiterMap.containsKey(current)) {
                AccessLimiter limiter = accessLimiterMap.get(current);
                if (limiter.count.get() == 0) {
                    accessLimiterMap.remove(current);
                    return;
                }
                limiter.count.decrementAndGet();
            }
        }

        private class AccessLimiter {
            // 持有线程
            private final Thread handleThread;
            // 记载重入次数
            private final AtomicInteger count;

            public AccessLimiter(Thread handleThread) {
                this.handleThread = handleThread;
                this.count = new AtomicInteger(0);
            }
        }
    }
}
