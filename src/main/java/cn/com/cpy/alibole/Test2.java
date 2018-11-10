package cn.com.cpy.alibole;

import java.util.concurrent.*;

/**
 * @author chenpanyu
 * @version 1.0
 * @since 2018/10/17 19:36
 */
public class Test2 {

    private ExecutorService executorService = Executors.newFixedThreadPool(50);

    private IFooService fooService = new FooService();

    public static void main(String[] args) throws InterruptedException {
        Test2 test2 = new Test2();
        for (int i = 0; i < 1000; i++) {
            int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        test2.sendAsync("chenpanyu-" + finalI);
                    } catch (InterruptedException e) {
                        System.out.println("线程中断，不再进行重试");
                    } catch (ExecutionException | TimeoutException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        TimeUnit.SECONDS.sleep(60);
        test2.executorService.shutdown();
    }

    private Object sendAsync(Object data) throws InterruptedException, TimeoutException, ExecutionException {
        System.out.println("sendAsync 发起调用, data = " + data);
        try {
            return sendAsync0(data, 3000);
        } catch (ExecutionException | TimeoutException e) {
            for (int i = 3; i > 0; i--) {
                TimeUnit.SECONDS.sleep(10);
                try {
                    return sendAsync0(data, 3000);
                } catch (ExecutionException | TimeoutException e1) {
                    if (i == 1) {
                        throw e1;
                    }
                }
            }
        }
        return null;
    }

    private Object sendAsync0(final Object data, long timout) throws InterruptedException, ExecutionException, TimeoutException {
        Future<Object> future = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    fooService.write(data);
                    return "success";
                } catch (Exception e) {
                    throw new Exception(Thread.currentThread().getName() + ", Error Msg = " + e.getMessage(), e);
                }
            }
        });
        return future.get(timout, TimeUnit.MILLISECONDS);
    }

    /**
     * 模拟服务接口
     */
    public interface IFooService {
        void write(Object data) throws Exception;
    }

    /**
     * 模拟服务接口实现
     */
    public class FooService implements IFooService {
        @Override
        public void write(Object data) throws Exception {
            if (ThreadLocalRandom.current().nextInt(2) == 0) {
                throw new Exception("write failure, data = " + data);
            }
            System.out.println("write success, data = " + data);
        }
    }
}
