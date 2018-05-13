package cn.xpleaf.rpc.common.utils;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPCDecoder继承自Netty中的MessageToMessageDecoder类，
 * 并重写抽象方法decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out)
 * 首先从数据报msg（数据类型取决于继承MessageToMessageDecoder时填写的泛型类型）中获取需要解码的byte数组
 * 然后调用使用序列化工具类将其反序列化（解码）为Object对象 将解码后的对象加入到解码列表out中，这样就完成了解码操作
 *
 * @author yeyonghao
 */
public class RPCDecoder extends MessageToMessageDecoder<ByteBuf> {

    // 需要反序列对象所属的类型
    private Class<?> genericClass;
    // log4j日志记录
    private Logger logger = LoggerFactory.getLogger(RPCDecoder.class);

    // 构造方法，传入需要反序列化对象的类型
    public RPCDecoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        // ByteBuf的长度
        int length = msg.readableBytes();
        // 构建length长度的字节数组
        byte[] array = new byte[length];
        // 将ByteBuf数据复制到字节数组中
        msg.readBytes(array);
        // 反序列化对象
        logger.info("准备反序列化对象...");
        Object obj = SerializationUtil.deserialize(array, this.genericClass);
        // 添加到反序列化对象结果列表
        logger.info("反序列化对象完毕，准备将其添加到反序列化对象结果列表...");
        out.add(obj);
    }

}
