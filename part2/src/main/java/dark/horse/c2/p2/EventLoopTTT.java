package dark.horse.c2.p2;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.NettyRuntime;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class EventLoopTTT {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup(2);

        for (int i = 0; i < 4; i++) {
            log.info(group.next().toString());
        }

        for (int i = 0; i < 4; i++) {
            group.next().submit(() -> {
                log.info("in thread 1");
                log.info(String.valueOf(Thread.currentThread().getName()));
            });
        }

        log.info("main");


        // 定时任务
        group.next().scheduleAtFixedRate(() -> {
            log.info("scheduled...");
        }, 2, 1, TimeUnit.SECONDS);





    }
}
