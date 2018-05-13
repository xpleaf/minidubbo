package cn.xpleaf.rpc.client.netty;

import cn.xpleaf.rpc.common.pojo.RPCRequest;
import cn.xpleaf.rpc.common.pojo.RPCResponse;
import cn.xpleaf.rpc.common.utils.RPCDecoder;
import cn.xpleaf.rpc.common.utils.RPCEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC客户端，用于连接RPC服务端，向服务端发送请求
 * 主要是netty的模板代码
 *
 * @author yeyonghao
 */
public class RPCClient extends SimpleChannelInboundHandler<RPCResponse> {

    // RPC服务端的地址
    private String host;
    // RPC服务端的端口号
    private int port;
    // RPCResponse响应对象
    private RPCResponse response;
    // log4j日志记录
    private Logger logger = LoggerFactory.getLogger(RPCClient.class);

    /**
     * 构造方法
     *
     * @param host RPC服务端的地址
     * @param port RPC服务端的端口号
     */
    public RPCClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 向RPC服务端发送请求方法
     *
     * @param request RPC客户端向RPC服务端发送的request对象
     * @return
     */
    public RPCResponse sendRequest(RPCRequest request) throws Exception {

        // 配置客户端NIO线程组
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
                    // 设置TCP连接超时时间
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                    .handler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // 添加解码器，RPC客户端需要解码的是RPCResponse对象，因为需要接收服务端发送过来的响应
                            ch.pipeline().addLast(new RPCDecoder(RPCResponse.class));
                            // 添加编码器
                            ch.pipeline().addLast(new RPCEncoder());
                            // 添加业务处理handler，本类继承了SimpleChannelInboundHandler
                            // RPCClient继承了SimpleChannelInboundHandler，所以可以直接传入本类对象
                            ch.pipeline().addLast(RPCClient.this);
                        }
                    });
            // 发起异步连接操作（注意服务端是bind，客户端则需要connect）
            logger.info("准备发起异步连接操作[{}:{}]", host, port);
            ChannelFuture f = b.connect(host, port).sync();

            // 判断连接是否成功的代码
            // System.out.println(f.isSuccess());

            // 向RPC服务端发起请求
            logger.info("准备向RPC服务端发起请求...");
            f.channel().writeAndFlush(request);


            // 需要注意的是，如果没有接收到服务端返回数据，那么会一直停在这里等待
            // 等待客户端链路关闭
            logger.info("准备等待客户端链路关闭...");
            f.channel().closeFuture().sync();
        } finally {
            // 优雅退出，释放NIO线程组
            logger.info("优雅退出，释放NIO线程组...");
            group.shutdownGracefully();
        }

        return response;
    }

    /**
     * 读取RPC服务端的响应结果，并赋值给response对象
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RPCResponse msg) throws Exception {
        logger.info("从RPC服务端接收到响应...");
        this.response = msg;
        // 关闭与服务端的连接，这样就可以执行f.channel().closeFuture().sync();之后的代码，即优雅退出
        // 相当于是主动关闭连接
        ctx.close();
    }

    /*
    public static void main(String[] args) throws Exception {
        int port = 21881;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(port);
            } catch (NumberFormatException e) {
                // 采用默认值
            }
        }
        new RPCClient("localhost", port).sendRequest(new RPCRequest());
    }
    */
}
