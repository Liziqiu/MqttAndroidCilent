package lzq.com.mqttpush.thread;

import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * Created by Administrator on 2016/5/12.
 */
public class MqttError extends MqttException {
    private String extraMsg = "";

    public MqttError(int reasonCode) {
        super(reasonCode);
    }

    public MqttError(Throwable cause) {
        super(cause);
    }

    public MqttError(int reason, Throwable cause) {
        super(reason, cause);
    }
    public MqttError(MqttException e, String extraMsg){
        super(e.getReasonCode(),e.getCause());
        this.extraMsg = extraMsg;
    }

    public String getExtraMsg() {
        return extraMsg;
    }
}
