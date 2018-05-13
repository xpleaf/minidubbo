package cn.xpleaf.rpc.common.pojo;

/**
 * EchoResponse是server向client端发送数据的传输载体，将需要进行传输的pojo对象统一封装到EchoResponse对象中，
 * 这样会为编解码工作带来很大的方便性和统一性，同时也可以携带其它信息， 对于后面对程序进行扩展会有非常大的帮助
 *
 * @author yeyonghao
 */
public class RPCResponse {

    private String requestId;
    private Throwable error;
    private Object result;

    public boolean isError() {
        return error != null;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

}
