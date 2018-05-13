package cn.xpleaf.rpc.server.registry;

import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务注册类，用于将服务提供者的服务注册到zookeeper上
 *
 * @author yeyonghao
 */
public class ServiceRegistry {

    // zookeeper中保存服务信息的父节点
    private final String parentNode = "/minidubbo";
    // zookeeper中服务提供者的序列化名称
    private final String serverName = "server";
    // zookeeper的地址，由spring构造ServiceRegistry对象时传入
    private String registryAddress;
    // 连接zookeeper的超时时间
    private int sessionTimeout = 2000;
    // 连接zookeeper的客户端
    private ZooKeeper zkClient = null;
    // 用来确保zookeeper连接成功后才进行后续的操作
    private CountDownLatch latch = new CountDownLatch(1);
    // log4j日志记录
    private Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);

    /**
     * 构造方法
     *
     * @param registryAddress zookeeper的地址，格式为 host:port
     */
    public ServiceRegistry(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    /**
     * 向zookeeper注册服务
     *
     * @param serverAddress 服务提供者的地址，格式为 host:port
     * @param interfaceName 注册的服务，完整接口名称，如cn.xpleaf.service.UserService
     */
    public void registerService(String serverAddress, String interfaceName) {
        // 如果zkClient为null，则连接未建立，先建立连接
        if (this.zkClient == null) {
            logger.info("未连接zookeeper，准备建立连接...");
            connectServer();
        }
        logger.info("zookeeper连接建立成功，准备在zookeeper上创建相关节点...");
        // 先判断父节点是否存在，如果不存在，则先创建父节点
        if (!isExist(parentNode)) {
            logger.info("正在创建节点[{}]", parentNode);
            createPNode(parentNode, "");
        }
        // 先判断接口节点是否存在（即/minidubbo/interfacename），如果不存在，则先创建接口节点
        if (!isExist(parentNode + "/" + interfaceName)) {
            logger.info("正在创建节点[{}]", parentNode + "/" + interfaceName);
            createPNode(parentNode + "/" + interfaceName, "");
        }
        // 创建接口节点下的服务提供者节点（即/minidubbo/interfacename/provider00001）
        logger.info("正在创建节点[{}]", parentNode + "/" + interfaceName + "/" + serverName + "+序列号");
        createESNode(parentNode + "/" + interfaceName + "/" + serverName, serverAddress);
        logger.info("zookeeper上相关节点已经创建成功...");
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
     * 判断节点是否存在
     *
     * @return 存在返回true，不存在返回false
     * @throws Exception
     */
    private boolean isExist(String node) {
        Stat stat = null;
        try {
            stat = zkClient.exists(node, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stat == null ? false : true;
    }

    /**
     * 创建永久节点（父节点/minidubbo和其子节点即接口节点需要创建为此种类型）
     *
     * @param node 节点的名称，父节点为/minidubbo，接口接点则为/minidubbo/interfacename
     * @param data 节点的数据，可为空
     */
    private void createPNode(String node, String data) {
        try {
            zkClient.create(node, data.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建短暂序列化节点（服务提供者节点需要创建为此种类型）
     *
     * @param node 节点的名称，如/minidubbo/interfacename/server00001
     * @param data 节点的数据，为服务提供者的IP地址和端口号的格式化数据，如192.168.100.101:21881
     */
    private void createESNode(String node, String data) {
        try {
            zkClient.create(node, data.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    public static void main(String[] args) throws IOException {
        // ServiceRegistry serviceRegistry = new ServiceRegistry("localhost:2181");
        // serviceRegistry.connectServer();
		// serviceRegistry.registerService("192.168.1.102:21881", "cn.xpleaf.UserService");
		// System.in.read();
        Map<String, String> map = new HashMap<>();
        map.put("name", "xpleaf");
        System.out.println(map.entrySet());
    }
    */
}
