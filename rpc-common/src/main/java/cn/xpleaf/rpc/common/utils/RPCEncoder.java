package cn.xpleaf.rpc.common.utils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPCEncoder继承自Netty中的MessageToByteEncoder类，
 * 并重写抽象方法encode(ChannelHandlerContext ctx, Object msg, ByteBuf out)
 * 它负责将Object类型的POJO对象编码为byte数组，然后写入到ByteBuf中
 *
 * @author yeyonghao
 */
public class RPCEncoder extends MessageToByteEncoder<Object> {

    // log4j日志记录
    private Logger logger = LoggerFactory.getLogger(RPCEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        // 直接生成序列化对象
        // 需要注意的是，使用protostuff序列化时，不需要知道pojo对象的具体类型也可以进行序列化的
        // 在反序列化时，只要提供序列化后的字节数组和原来pojo对象的类型即可完成反序列化
        logger.info("准备序列化对象...");
        byte[] array = SerializationUtil.serialize(msg);
        logger.info("序列化对象完毕，准备将其写入到ByteBuf中...");
        out.writeBytes(array);
    }

}
