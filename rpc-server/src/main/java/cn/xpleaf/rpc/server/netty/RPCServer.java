package cn.xpleaf.rpc.server.netty;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import cn.xpleaf.rpc.common.pojo.RPCRequest;
import cn.xpleaf.rpc.common.utils.RPCDecoder;
import cn.xpleaf.rpc.common.utils.RPCEncoder;
import cn.xpleaf.rpc.server.annotation.RPCService;
import cn.xpleaf.rpc.server.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * RPCServer主要完成下面几个功能：
 * 1.将需要发布的服务保存到一个map中
 * 2.启动netty服务端程序
 * 3.向zookeeper注册需要发布的服务
 *
 * @author yeyonghao
 */
public class RPCServer implements ApplicationContextAware, InitializingBean {

    // 用来保存用户服务实现类对象，key为实现类的接口名称，value为实现类对象
    private Map<String, Object> serviceBeanMap = new HashMap<>();
    // 运行netty程序的服务端地址，用于绑定到netty服务端程序中
    private String serverAddress;
    // 向zookeeper注册的注册类对象
    private ServiceRegistry serviceRegistry;
    // log4j日志记录
    private Logger logger = LoggerFactory.getLogger(RPCServer.class);

    /**
     * RPCServer的构造方法
     *
     * @param serverAddress   服务提供者的地址信息，格式为 host:port
     * @param serviceRegistry zookeeper服务注册类对象
     */
    public RPCServer(String serverAddress, ServiceRegistry serviceRegistry) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * 由于本类实现了ApplicationContextAware接口，spring在构造本类对象时会调用setApplicationContext方法
     * 在该方法中，通过注解获取标注了RPCService的用户服务实现类，然后将其接口和实现类对象保存到serviceBeanMap中
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 通过spring获取到标注了RPCService注解的map，map的key为bean的名称，map的value为bean的实例对象
        // a Map of bean names with corresponding bean instances
        // 所以，如果你的服务实现类标注了RPCService注解，但其不在spring的扫描范围内，也是没有办法获取到的
        logger.info("准备扫描获取标注了RPCService的用户服务实现类...");
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(RPCService.class);
        // 将beansWithAnnotation中的values值，即bean的实现对象保存到serviceBeanMap中
        logger.info("标注了RPCService的用户服务实现类扫描完毕，准备将其接口和实现类对象保存到容器中...");
        for (Object serviceBean : beansWithAnnotation.values()) {
            // 获取实现类对象的接口名称，思路是，实现类中标注了RPCService注解，同时其参数为实现类的接口类型
            // 说明：实际上可以通过serviceBean.getClass().getInterfaces()的方式来获取其接口名称的
            // 只是如果是实现多个接口的情况下需要进行判断，这点后面再做具体的实现
            String interfaceName = serviceBean.getClass().getAnnotation(RPCService.class).value().getName();

            // 保存到serviceBeanMap中
            serviceBeanMap.put(interfaceName, serviceBean);
        }
        logger.info("用户服务接口和实现类对象保存完毕...");

    }

    /**
     * 由于本类实现了InitializingBean接口，spring在构造完所有对象之后会调用afterPropertiesSet方法
     * 在该方法中，将服务注册到zookeeper，同时启动netty服务端程序，该方法中主要是netty框架的代码
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {

        logger.info("准备构建RPC服务端，监听来自RPC客户端的请求...");

        // 配置服务端NIO线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // 添加编码器，RPC服务端需要解码的是RPCRequest对象，因为需要接收客户端发送过来的请求
                            ch.pipeline().addLast(new RPCDecoder(RPCRequest.class));
                            // 添加解码器
                            ch.pipeline().addLast(new RPCEncoder());
                            // 添加业务处理handler
                            ch.pipeline().addLast(new RPCServerHandler(serviceBeanMap));
                        }
                    });

            // 解析serverAddress中的host和port
            String[] array = serverAddress.split(":");
            String host = array[0];
            int port = Integer.valueOf(array[1]);

            // 绑定端口，同步等待成功，该方法是同步阻塞的，绑定成功后返回一个ChannelFuture
            logger.info("准备绑定服务提供者地址和端口[{}:{}]", host, port);
            ChannelFuture f = b.bind(host, port).sync();

            // 向zookeeper注册
            logger.info("绑定服务提供者地址和端口成功，准备向zookeeper注册服务...");
            for (String interfaceName : serviceBeanMap.keySet()) {
                serviceRegistry.registerService(serverAddress, interfaceName);
            }

            // 等待服务端监听端口关闭，阻塞，等待服务端链路关闭之后main函数才退出
            logger.info("向zookeeper注册服务成功，正在监听来自RPC客户端的请求连接...");
            f.channel().closeFuture().sync();
        } finally {
            // 优雅退出，释放线程池资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }

}
