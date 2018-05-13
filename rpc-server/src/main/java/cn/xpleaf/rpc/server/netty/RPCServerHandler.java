package cn.xpleaf.rpc.server.netty;

import java.lang.reflect.Method;
import java.util.Map;

import cn.xpleaf.rpc.common.pojo.RPCRequest;
import cn.xpleaf.rpc.common.pojo.RPCResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPCServerHandler主要用于处理用户的请求，并返回响应结果
 * 主要是在Netty的模板代码（ChannelInboundHandlerAdapter）中嵌入反射调用方法的代码，并封装结果
 *
 * @author yeyonghao
 */
public class RPCServerHandler extends ChannelInboundHandlerAdapter {

    // 用来保存用户服务实现类对象，key为实现类的接口名称，value为实现类对象
    Map<String, Object> serviceBeanMap = null;
    // log4j日志记录
    Logger logger = LoggerFactory.getLogger(RPCServerHandler.class);

    /**
     * 构造方法传入保存了key-value为interfaceName-bean的map
     *
     * @param serviceBeanMap
     */
    public RPCServerHandler(Map<String, Object> serviceBeanMap) {
        this.serviceBeanMap = serviceBeanMap;
    }

    /**
     * 接收消息，处理消息，返回结果
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        logger.info("接收到来自RPC客户端的连接请求...");

        // 接收到的对象的类型为RPCRequest
        RPCRequest request = (RPCRequest) msg;
        RPCResponse response = new RPCResponse();
        // 设置requestId
        response.setRequestId(response.getRequestId());
        try {
            logger.info("准备调用handle方法处理request请求对象...");
            // 调用handle方法处理request
            Object result = handleRequest(request);
            // 设置返回结果
            response.setResult(result);
        } catch (Throwable e) {
            // 如果有异常，则设置异常信息
            response.setError(e);
        }

        logger.info("请求处理完毕，准备回写response对象...");
        ctx.writeAndFlush(response);
    }

    /**
     * 对request进行处理，其实就是通过反射进行调用的过程
     *
     * @param request
     * @return
     * @throws Throwable
     */
    public Object handleRequest(RPCRequest request) throws Throwable {
        // 拿到类名
        String interfaceName = request.getInterfaceName();

        // 根据接口名拿到其实现类对象
        Object serviceBean = serviceBeanMap.get(interfaceName);

        // 拿到要调用的方法名、参数类型、参数值
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();

        // 拿到接口类对象
        Class<?> clazz = Class.forName(interfaceName);

        // 拿到实现类对象的指定方法
        Method method = clazz.getMethod(methodName, parameterTypes);

        // 通过反射调用方法
        logger.info("准备通过反射调用方法[{}]...", interfaceName);
        Object result = method.invoke(serviceBean, parameters);

        logger.info("通过反射调用方法完毕...");
        // 返回结果
        return result;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
