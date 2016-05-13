package lzq.com.mqttpush;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import lzq.com.mqttpush.common.Utils;
import lzq.com.mqttpush.thread.MqttConfig;
import lzq.com.mqttpush.thread.MqttError;
import lzq.com.mqttpush.thread.MqttProtocol;
import lzq.com.mqttpush.thread.MqttThread;

/**
 * Created by zhiqiang on 2016/5/12.
 *
 * 启动 mqttThread 这个线程，就可以实现监听推送，当有推送时，会回调本类的方法messageArrived（。。。）
 * 当有错误时会回调 Error（。。），请在该方法下实现错误的处理
 */
public class PushService extends Service implements MqttThread.MqttThreadCallBack {
    private NetworkStatusListener networkStatusListener;
    private MqttThread mqttThread;
    private MqttConfig mqttConfig;

    public static final String LINTENER = "linsener";

    //具体需要的配置
    private static final String ServiceIP = "172.16.32.237";  //服务器IP
    private static final String ServiceProt = "1883";  //服务器端口
    private static final String DeviceID = "android_test_01";//客户端 ID
    private String[] topics = {"zhuti"};   //需要订阅的主题


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        networkStatusListener = new NetworkStatusListener();
        IntentFilter filter = new IntentFilter();

        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        this.registerReceiver(networkStatusListener, filter);
        generateMqttThread();
    }

    private void generateMqttThread(){
        mqttConfig = new MqttConfig(ServiceIP,ServiceProt,DeviceID);
        mqttConfig.setTopics(topics);

        mqttConfig.setProtocol(MqttProtocol.PROTOCOL_TCP);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        mqttConfig.setMqttConnectOptions(options);
        mqttThread = new MqttThread(this,mqttConfig);
        mqttThread.setCallBack(this);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null){
            return super.onStartCommand(intent, flags, startId);
        }
        if(LINTENER.equals(intent.getAction())){
            if(mqttThread.getState() == Thread.State.NEW) {
                mqttThread.start();
            }else if(mqttThread.getState() == Thread.State.TERMINATED){
                generateMqttThread();
                mqttThread.start();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mqttThread.finish();
        unregisterReceiver(networkStatusListener);
        networkStatusListener = null;
    }

    /**
     * 当收到消息时，会被回调
     * @param topic 消息的主题（跟你订阅的一致）
     * @param message
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        Log.d("zhiqiang","Arrived topic:"+topic+" Message:"+new String(message.getPayload()));
    }

    /**
     * 当推送进程发送错误时，会被回调
     * @param error
     */
    @Override
    public void Error(MqttError error) {
        //具体在这里处理错误信息，reason code 具体含义参考MqttException里的注释
        Log.e("zhiqiang","Push error:"+error.getMessage()+". Extra:"+error.getExtraMsg()+".  Reason code:"+ error.getReasonCode());
    }

    class NetworkStatusListener extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if(Utils.isNetWorkAvailable(context)){
                mqttThread.notifyWhenfNetworkAvailable();
            }
        }
    }
}
