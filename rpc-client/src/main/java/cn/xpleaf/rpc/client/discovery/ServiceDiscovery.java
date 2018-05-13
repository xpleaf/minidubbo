package cn.xpleaf.rpc.client.discovery;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务发现类，用于向zookeeper中查询服务提供者的地址（host:port） 暂时的设计思路是，每执行一次调用都会进行查询，而不是像dubbo那样
 * 会将interfaceName和服务地址缓存起来，后面会实现这一点
 *
 * 另外，显然我这里都是使用zookeeper较为原生的API，原因很简单，当初入手zookeeper API时就是先从原生的学起，之后就直接应用在minidubbo上，
 * 在我的另外一个项目中[分布式爬虫系统]，使用的是较为高层次的API，即curator，如果有兴趣，可以参考一下使用方式：https://github.com/xpleaf/ispider
 *
 * @author yeyonghao
 */
public class ServiceDiscovery {

    // zookeeper中保存服务信息的父节点
    private final String parentNode = "/minidubbo";
    // zookeeper的地址，由spring构造ServiceDiscovery对象时传入
    private String registryAddress;
    // 连接zookeeper的超时时间
    private int sessionTimeout = 2000;
    // 连接zookeeper的客户端
    private ZooKeeper zkClient = null;
    // 用来确保zookeeper连接成功后才进行后续的操作
    private CountDownLatch latch = new CountDownLatch(1);
    // log4j日志记录
    private Logger logger = LoggerFactory.getLogger(ServiceDiscovery.class);

    /**
     * 构造方法
     *
     * @param registryAddress zookeeper的地址，格式为 host:port
     */
    public ServiceDiscovery(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    /**
     * 发现服务方法 根据接口名称向zookeeper查询服务提供者的地址
     *
     * @param interfaceName 接口名称
     * @return serverAddress服务提供者的地址，格式为 host:port 如果不存在，则返回null
     */
    public String discoverService(String interfaceName) {
        // 如果zkClient为null，则连接未建立，先建立连接
        if (this.zkClient == null) {
            logger.info("未连接zookeeper，准备建立连接...");
            connectServer();
        }
        // 构建需要查询的节点的完整名称
        String node = parentNode + "/" + interfaceName;
        // 获取该节点所对应的服务提供者地址
        logger.info("zookeeper连接建立完毕，准备获取服务提供者地址[{}]...", node);
        String serverAddress = getServerAddress(node);
        logger.info("服务提供者地址获取完毕[{}]...", serverAddress);
        // 返回结果
        return serverAddress;
    }

    /**
     * 建立连接
     */
    private void connectServer() {
        try {
            zkClient = new ZooKeeper(registryAddress, sessionTimeout, new Watcher() {

                // 注册监听事件，连接成功后会调用process方法
                // 此时再调用latch的countDown方法使CountDownLatch计数器减1
                // 因为构造CountDownLatch对象时设置的值为1，减1后变为0，所以执行该方法后latch.await()将会中断
                // 从而确保连接成功后才会执行后续zookeeper的相关操作
                @Override
                public void process(WatchedEvent event) {
                    // 如果状态为已连接，则使用CountDownLatch计数器减1
                    if (event.getState() == Event.KeeperState.SyncConnected) {
                        latch.countDown();
                    }
                }
            });
            latch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取对应接口名的服务地址
     *
     * @param node 接口名对应的完整节点名称
     * @return serverAddress，如果为null，说明不存在该节点的服务提供者
     */
    private String getServerAddress(String node) {
        String serverAddress = null;
        try {
            // 先获取接口名节点的子节点，子节点下是服务器的列表
            // 需要注意的是，如果不存在该节点，会有异常，此时下面的代码就不会执行
            List<String> children = zkClient.getChildren(node, false);
            // 获取服务提供者列表的第一个，暂时先不考虑负载均衡的问题
            String firstChildren = children.get(0);
            // 构建该服务提供者的完整节点名称
            String firstChildrenNode = node + "/" + firstChildren;
            // 获取服务提供者节点的数据，得到serverAddress的byte数组
            byte[] serverAddressByte = zkClient.getData(firstChildrenNode, false, null);
            // 将byte数组转换为字符串，同时赋值给serverAddress
            serverAddress = new String(serverAddressByte);
        } catch (Exception e) {
            logger.error("节点[{}]不存在，无法获取服务提供者地址...", node);
            logger.error(e.getMessage());
        }

        return serverAddress;
    }

    /*
    public static void main(String[] args) throws Exception {
        ServiceDiscovery serviceDiscovery = new ServiceDiscovery("localhost:2181");
        serviceDiscovery.connectServer();
        String serverAddress = serviceDiscovery.getServerAddress("/minidubbo/cn.xpleaf.service.UserService");
        System.out.println("serverAddress: " + serverAddress);
    }
    */

}
