# Netty

Q： netty 创建的线程数  

A： N==0 || N == null时， 系统设置了 "io.netty.eventLoopThreads", 则为设置只，否则为 CPU 核心数的2倍

```java
protected MultithreadEventLoopGroup(int nThreads, Executor executor, Object... args) {        
    super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor, args);    
}    
static {        
    DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt("io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));    
}
```



### InboundHandler & outBoundHandler的调用

假设handlerAdapter的添加顺序如下：    head -> h1 -> h2 -> h3 -> h4 -> h5 -> h6 -> tail。其中head, tail是netty自动添加的，h1, h2, h3为inboundHandlerAdapter ，h4, h5, h6为outBoundHandlerAdapter(省略了h2, h3, h5, h6相似的代码）

默认情况下执行顺序是 head -> inBound 顺序 -> tail -> outBound倒序，所以为 log的输出顺序为 h1 -> h2 -> h3 -> h6 -> h5 -> h4 （下面也是指输出顺序）

如果代码按①②的顺序执行，则h1 -> h6 -> h5 -> h4 -> h2 -> h3

如果注释代码按②①顺序执行，则h1 -> h2 -> h3 -> h6 -> h5 -> h4

如果①②放到h2中，则为 h1 -> h2 -> h6 -> h5 -> h4 -> h3 

如果注释代码按②①顺序执行，则h1 -> h2 -> h3 -> h6 -> h5 -> h4

**Note：**①处的代码 ```super.channelRead(ctx, "S1");```是调用的关键，它使得inboundHandler能够链式调用，并将处理过的data继续向下执行（将 字符串S1 往下传递，那么下一个inboundHandler拿到的就是字符串S1）

如果按②③在h2中顺序执行代码，则h1 -> h2 -> h3. 分析：调用链为h1(输出h1) -> h2(输出h2) -> h3(输出h3，h3调用完毕，h2继续执行ctx......，从h2开始往回找，没有处理outBound的，调用完毕) 

```java
new ServerBootstrap().group(boss, worker).channel(NioServerSocketChannel.class)
    .childHandler(new ChannelInitializer<NioSocketChannel>() {
        @Override
        protected void initChannel(NioSocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(new StringDecoder());
            pipeline.addLast("h1", new ChannelInboundHandlerAdapter(){
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

 					log.info("h1");                   
                    ch.writeAndFlush(ctx.alloc().buffer().writeBytes("s".getBytes(StandardCharsets.UTF_8))); // 这行代码会去调用outBound的调用链  ······①   
                    ctx.writeAndFlush(ctx.alloc().buffer().writeBytes("Scan".getBytes(StandardCharsets.UTF_8))); // 当前调用链开始往前找 ······③

                    super.channelRead(ctx, "S1"); // 这行方法会去触发下一个inBound的调用链 ···②
                }
            });
            pipeline.addLast("h4", new ChannelOutboundHandlerAdapter(){
                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                    log.info("h4");
                    super.write(ctx, msg, promise);
                }
            });
        }
    })
    .bind(8080);
```

