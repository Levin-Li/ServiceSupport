package com.levin.commons.utils;


import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.function.Consumer;

@Slf4j
@Data
@Accessors(chain = true)
public class AsyncHandler<T> {

    protected Executor defaultExecutor;

    protected String name;

    protected boolean isValid = true;

    /**
     * 是否忽略未做的任务
     */
    protected boolean skipNotProcessTask = false;

    /**
     * 自定义 同步任务执行器
     */
    protected Consumer<T> syncTaskExecutor;

    ////////////////////////////////////////////////////////

    protected final BlockingQueue<T> taskQueue;

    protected final Semaphore workerController;

    protected final StatHelper addTaskStatHelper = new StatHelper();

    protected final StatHelper doTaskStatHelper = new StatHelper();

    public AsyncHandler() {
        this(1, Integer.MAX_VALUE);
    }

    public AsyncHandler(int maxWorkers, int maxTasks) {

        this.taskQueue = new LinkedBlockingQueue(maxTasks);

        this.workerController = new Semaphore(maxWorkers);

    }

    public boolean addTask(T task) {
        try {
            return addTask(null, task);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 加入任务
     *
     * @param executor
     * @param task
     * @return
     * @throws InterruptedException
     */
    public boolean addTask(Executor executor, T task) throws InterruptedException {

        if (!isValid()) {
            taskQueue.clear();
            throw new IllegalStateException("handler is invalid");
        }

        boolean isOffer = taskQueue.offer(task, 7, TimeUnit.SECONDS);

        if (isOffer) {

            processQueue(executor);

            addTaskStatHelper.onAlarm(5, 100, 1.0, (scale, ratio) -> {
                log.info("异步处理器[{}]在{}秒内进入的任务速率为{}/秒，环比变化率{} ，目前队列数据包剩余数：{}"
                        , name, 5, ratio, scale, taskQueue.size());
            });

        }

        return isOffer;
    }

    protected void doQueueTask() {

        while (taskQueue.size() > 0
                && isValid()
                && !Thread.currentThread().isInterrupted()) {

            T task = null;

            //忽略中间的任务，只做最新的任务
            if (skipNotProcessTask) {

                T nextTask = taskQueue.poll();

                long skipCnt = -1;

                //只取最新的一个元素
                while (nextTask != null) {
                    task = nextTask;
                    nextTask = taskQueue.poll();
                    skipCnt++;
                }

                if (skipCnt > 0) {
                    log.info("队列处理器[{}] 跳过的任务数：{}", name, skipCnt);
                }

            } else {
                task = taskQueue.poll();
            }

            if (task != null) {

                doTaskStatHelper.onAlarm(isSkipNotProcessTask() ? 20 : 5, 100, 1.0, (scale, ratio) -> {
                    log.info("异步处理器[{}]在{}秒内完成的任务速率为{}/秒，环比变化率{} ，目前队列数据包剩余数：{}"
                            , name, 5, ratio, scale, taskQueue.size());
                });

                //同步处理任务
                if (syncTaskExecutor != null) {
                    syncTaskExecutor.accept(task);
                } else {
                    //同步处理任务
                    syncDoTask(task);
                }

            }

        }
    }

    protected void syncDoTask(T task) {
        if (task instanceof Runnable) {
            ((Runnable) task).run();
        } else {
            throw new IllegalArgumentException("task " + task.getClass() + " not supported");
        }
    }


    protected void processQueue(Executor executor) {

        //如果已经有人在处理，直接返回
        if (!workerController.tryAcquire()) {
            return;
        }

        try {

            if (executor == null) {
                executor = this.defaultExecutor;
            }

            executor.execute(worker);

        } finally {
            //释放
            workerController.release();
        }

    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "-" + name;
    }


    private final Runnable worker = () -> doQueueTask();

}