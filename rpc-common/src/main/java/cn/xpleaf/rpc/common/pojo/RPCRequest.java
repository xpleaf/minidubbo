package cn.xpleaf.rpc.common.pojo;

/**
 * RPCRequest是client向server端发送数据的传输载体，将需要进行传输的pojo对象统一封装到RPCRequest对象中，
 * 这样会为编解码工作带来很大的方便性和统一性，同时也可以携带其它信息， 对于后面对程序进行扩展会有非常大的帮助
 *
 * @author yeyonghao
 */
public class RPCRequest {

    // 请求的ID，为UUID
    private String requestId;
    // 接口名称
    private String interfaceName;
    // 调用的方法名称
    private String methodName;
    // 方法的参数类型
    private Class<?>[] parameterTypes;
    // 方法的参数值
    private Object[] parameters;

    /**
     * 上面几个数据就可以唯一地确定某一个类（接口）中的某一个具体的方法
     */
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

}
