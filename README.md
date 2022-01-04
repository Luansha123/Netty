# Netty

Q： netty 创建的线程数         new ServerBootstrap().group(new NioEventLoopGroup( N* ))

A： N==0 || N == null时， 系统设置了 "io.netty.eventLoopThreads", 则为设置只，否则为 CPU 核心数的2倍

```java
    protected MultithreadEventLoopGroup(int nThreads, Executor executor, Object... args) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor, args);
    }
    static {
        DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt(
                "io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));
    }
```

P63 - eventLoop的切换