package org.klong;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class Producer {

    private static InetSocketAddress addr = new InetSocketAddress("localhost", 8888);

    private static final LinkedBlockingDeque<Integer> QUEUE = new LinkedBlockingDeque<>(100);

    private static final ExecutorService pool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws InterruptedException {
        Runtime.getRuntime().addShutdownHook(new Thread(pool::shutdownNow));
        ((ThreadPoolExecutor) pool).setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        int i = 0;
        while (++i <= 10) {
            pool.submit(Producer::tcpWrite);
        }
        int j = 0;
        while (true) {
            QUEUE.put(j++);
        }
    }

    private static void tcpWrite() {
        try (Socket socket = new Socket()) {
            socket.connect(addr);
            if (socket.isConnected()) {
                System.out.println(Thread.currentThread().getName()+" connected successfully.");
            }
            OutputStream outputStream = socket.getOutputStream();
            while (socket.isConnected()) {
                Integer take = QUEUE.take();
                try {
                    outputStream.write((take + "\n").getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                } catch (IOException ioException) {
                    // consume fail, produce again.
                    QUEUE.putFirst(take);
                    throw ioException;
                }
                TimeUnit.MILLISECONDS.sleep(500);
            }
        } catch (IOException e) {
            System.out.println(Thread.currentThread().getName() + " connect fail, retry after 10s.");
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException ignore) {
                System.out.println(Thread.currentThread().getName() + " retry canceled.");
            }
            pool.submit(Producer::tcpWrite);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
