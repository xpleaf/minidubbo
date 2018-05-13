package cn.xpleaf.rpc.client.proxy;

import cn.xpleaf.rpc.client.discovery.ServiceDiscovery;
import cn.xpleaf.rpc.client.netty.RPCClient;
import cn.xpleaf.rpc.common.pojo.RPCRequest;
import cn.xpleaf.rpc.common.pojo.RPCResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * 动态代理对象类，用于根据接口创建动态代理对象
 *
 * @author yeyonghao
 */
public class RPCProxy {

    // 用于发现服务的对象
    private ServiceDiscovery serviceDiscovery;

    // log4j日志记录
    private Logger logger = LoggerFactory.getLogger(RPCProxy.class);

    /**
     * 构造函数，传入ServiceDiscovery对象
     */
    public RPCProxy(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    /**
     * 获得动态代理对象的通用方法，实现思路是，该方法中，并不需要具体的实现类对象 因为在invoke方法中，并不会调用Method
     * method这个方法，只是获得其方法的名字 然后将其封装在Netty请求中，发送到Netty服务端中请求远程调用的结果
     *
     * @param interfaceClass 需要被代理的接口的类型对象
     * @return proxy 对应接口的代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<?> interfaceClass) {

        T proxy = (T) Proxy.newProxyInstance(RPCProxy.class.getClassLoader(), new Class<?>[]{interfaceClass},
                new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        logger.info("准备构建RPCRequest对象...");

                        // 构建RPCRequest对象
                        RPCRequest request = new RPCRequest();
                        // 设置requestId
                        request.setRequestId(UUID.randomUUID().toString());
                        // 设置接口名interfaceName
                        String interfaceName = method.getDeclaringClass().getName();
                        request.setInterfaceName(interfaceName);
                        /*
                        当时在将其整合到Spring-mvc失败时的调试信息
                        前后调试了近两个月才搞定，所以这部分调试信息就不删除了
                        String interfaceName1 = method.getDeclaringClass().getName();
                        String interfaceName2 = interfaceClass.getName();
                        System.out.println("接口名称是：" + interfaceClass.getName());
                        System.out.println("接口名称是1：" + method.getDeclaringClass().getName());
                        String proxyName = proxy.getClass().getName();
                        */
                        // 设置方法名methodName
                        request.setMethodName(method.getName());
                        // 设置参数类型parameterTypes
                        request.setParameterTypes(method.getParameterTypes());
                        // 设置参数列表parameters
                        request.setParameters(args);

                        logger.info("RPCRequest对象构建完毕，准备发现服务[{}]...", interfaceName);

                        // 发现服务，得到服务地址，格式为 host:port
                        String serverAddress = serviceDiscovery.discoverService(interfaceName);
                        // 如果服务不存在，null，否则就构建RPC客户端进行远程调用
                        if (serverAddress == null) {
                            logger.error("服务[{}]的提供者不存在，发现服务失败...", interfaceName);
                            return null;
                        } else {

                            logger.info("发现服务完毕，准备解析服务地址[{}]...", serverAddress);

                            // 解析服务地址
                            String[] array = serverAddress.split(":");
                            String host = array[0];
                            int port = Integer.valueOf(array[1]);

                            logger.info("服务地址解析完毕，准备构建RPC客户端...");

                            // 构建RPC客户端
                            RPCClient client = new RPCClient(host, port);

                            logger.info("RPC客户端构建完毕，准备向RPC服务端发送请求...");

                            // 向RPC服务端发送请求
                            RPCResponse response = client.sendRequest(request);

                            // 返回信息
                            if (response.isError()) {
                                // 如果进行远程调用时出现异常，[则抛出异常信息]--->直接返回null
                                // throw response.getError();
                                logger.error("[{}]远程过程调用出现异常，远程过程调用失败...", interfaceName);
                                return null;
                            } else {
                                // 如果没有异常，则返回调用的结果
                                logger.info("[{}]远程过程调用完毕，远程过程调用成功...", interfaceName);
                                return response.getResult();
                            }
                        }
                    }
                });

        return proxy;
    }

}
