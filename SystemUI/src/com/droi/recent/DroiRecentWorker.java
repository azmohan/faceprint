/*
 * Created by Droi Sean 20160420
 */

package com.droi.recent;

import java.lang.Override;
import java.lang.Runnable;
import java.lang.Thread;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class DroiRecentWorker{


    private static final ExecutorService sExecutor = Executors.newFixedThreadPool(5, new RecentThreadFactory());

    private static class RecentThreadFactory implements ThreadFactory{
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            return t;
        }
    }

    public static void post(Runnable r){
        sExecutor.execute(r);
    }

}