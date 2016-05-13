package lzq.com.mqttpush.thread;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/5/12.
 */
public class MqttConfig {
    private String DeviceID;
    private String ServiceIP;
    private String Port;
    private MqttConnectOptions mqttConnectOptions;
    private int Qos=1;
    private boolean Retained = false;
    private String Protocol = MqttProtocol.PROTOCOL_TCP;
    private String KeepAliveTopic = "keepalive";
    private List<Topic> topicList; //需要订阅的topic 的数组

    public MqttConfig(String serviceIP, String port,String DeviceID) {
        ServiceIP = serviceIP;
        Port = port;
        this.DeviceID = DeviceID;
        topicList = new ArrayList<Topic>();
    }

    public String getServiceIP() {
        return ServiceIP;
    }

    public String getPort() {
        return Port;
    }

    public String getDeviceID() {
        return DeviceID;
    }

    public MqttConnectOptions getMqttConnectOptions() {
        if(mqttConnectOptions == null){
            mqttConnectOptions = new MqttConnectOptions();
        }
        return mqttConnectOptions;
    }

    public void setMqttConnectOptions(MqttConnectOptions mqttConnectOptions) {
        this.mqttConnectOptions = mqttConnectOptions;
    }

    public int getQos() {
        return Qos;
    }

    public void setQos(int qos) {
        Qos = qos;
    }

    public String getProtocol() {
        return Protocol;
    }

    public void setTopics(String ... topics){
        topicList.clear();
        for(String topic:topics){
            topicList.add(new Topic(topic,Qos));
        }
    }

    public void setProtocol(@MqttProtocol String protocol) {
        Protocol = protocol;
    }

    public boolean isRetained() {
        return Retained;
    }

    public void setRetained(boolean retained) {
        Retained = retained;
    }

    public String getKeepAliveTopic() {
        return KeepAliveTopic;
    }

    public void setKeepAliveTopic(String keepAliveTopic) {
        KeepAliveTopic = keepAliveTopic;
    }

    public List<Topic> getTopicList() {
        return topicList;
    }

    public static class Topic{
        private String Name;
        private int Qos;

        public Topic(String name, int qos) {
            Name = name;
            Qos = qos;
        }

        public String getName() {
            return Name;
        }

        public int getQos() {
            return Qos;
        }
    }
}
