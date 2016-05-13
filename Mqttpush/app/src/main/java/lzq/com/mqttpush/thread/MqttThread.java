package lzq.com.mqttpush.thread;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

/**
 * Created by zhiqiang on 2016/5/12.
 *
 * 这个类使用自己定义的心跳包，因为Mqtt框架原生支持的心跳规则没有断开重连机制.... ，而且某些机型貌似不能使用这个原生的心跳机制，我的三星4.4就不行了，不知道为什么（无奈）
 * 可能随着以后这个框架的升级会解决这个问题，到时，我们可以直接使用原生的心跳机制，不用自己去写了......
 */
public class MqttThread extends Thread implements MqttCallback {

    private static final String TAG = "MqttThread";
    private Context context;
    private MqttClient mqttClient;
    private MqttConfig config;
    private Handler handler;
    private MqttThreadCallBack callBack;
    private volatile boolean keepAlive = true; // 是否保持长连接（就是不断发心跳）
    private ConnectivityManager connectivityManager;

    private CountDownLatch countDownLatch;//用来阻塞进程的类
    public MqttThread(Context context, MqttConfig config) {
        this.context = context.getApplicationContext();
        this.config = config;
        handler = new Handler(context.getMainLooper());
        setName("MqttThread");
    }

    @Override
    public void run() {
        super.run();
        try {
            InitConnect(); // 初始化连接
            while(keepAlive){
                sleep(30*1000);//心跳间隔，写死是30s，推荐是1分钟，以后根据实际情况加长（注意，不能更短了，会十分耗电）
                if(!isNetWorkAvailable()){
                    countDownLatch = new CountDownLatch(1);
                    disconnect();//无网络时 ，断开连接，等待有网
                    countDownLatch.await();//这一步会阻塞知道有网
                    countDownLatch = null;
                    InitConnect(); // 重新初始化连接
                }
                sendHeartbeat();
            }
        } catch (MqttError error) {
            responeError(error);
        } catch (InterruptedException e) {
           //线程被正常停止了，不作任何处理
        }finally {
            try {
                disconnect();//被调用了 finish 这个方法，就需要停止线程，并且断掉连接
            } catch (MqttError error) {
                //responeError(error); 线程要被终结，这里的error ，只打印出来，不需要外部处理
                Log.e(TAG,"error:"+error.getMessage()+" ***extra message:"+error.getExtraMsg());
            }
        }

    }

    /**
     * 通知网络可用，唤起阻塞
     */
    public void notifyWhenfNetworkAvailable(){
        if(countDownLatch != null ) {
            countDownLatch.countDown();
        }
    }

    private void InitConnect() throws MqttError {
        initMqttCilent(config); //不管有没有旧的cilent，直接新建一个
        if(isNetWorkAvailable()) {
            Connect();//连接
            subscribeTopic(config.getTopicList());//订阅相关主题
        }
    }

    private void disconnect() throws MqttError {
        //stopKeepAlive();
        if(mqttClient != null){
            if(!mqttClient.isConnected()){
                return;
            }
            try {
                mqttClient.disconnect();
            } catch (MqttException e) {
                throw new MqttError(e,"mqttClient disconnect false");
            }
        }
    }

    /**
     * 不在需要保持长连接 (基本没用)
     */
    public void stopKeepAlive() {
        keepAlive = false;
    }

    private void initMqttCilent(MqttConfig config) throws MqttError {
        String UrlFormat = config.getProtocol()+"://%s:%s";
        String Url = String.format(Locale.US, UrlFormat, config.getServiceIP(), config.getPort());
        try {
            mqttClient = new MqttClient(Url,config.getDeviceID(),new MqttDefaultFilePersistence(context.getCacheDir().getAbsolutePath()));
            mqttClient.setCallback(this);
        } catch (MqttException e) {
            throw new MqttError(e,"initMqttCilent false");
        }
    }

    /**
     * 连接服务器
     * @throws MqttError
     */
    private void Connect() throws MqttError {
        if(mqttClient == null){
            initMqttCilent(config);
        }
        if(mqttClient.isConnected()){
            return;
        }
        try {
            mqttClient.connect(config.getMqttConnectOptions());
        } catch (MqttException e) {
            throw new MqttError(e,"MqttCilent connect false");
        }
    }
    private void subscribeTopic(List<MqttConfig.Topic> topiclist) throws MqttError {
        for(MqttConfig.Topic topic:topiclist){
            subscribeTopic(topic.getName(),topic.getQos());
        }
    }
    /**
     * 订阅相关推送
     * @param topic 推送主题
     * @throws MqttError
     */
    private void subscribeTopic(String topic,int qos) throws MqttError {
        if(mqttClient == null){
            throw new MqttError(new MqttException(0),"it have not init the mqtt cilent before call subscribeTopic!");
        }
        try {
            mqttClient.subscribe(topic, qos);
        } catch (MqttException e) {
            throw new MqttError(e,"subscribeTopic:"+topic+" occur error");
        }
    }

    private void public2Topic(String topic,String message) throws MqttError {
        if(mqttClient == null){
            throw new MqttError(new MqttException(0),"it have not init the mqtt cilent before call public2Topic!");
        }
        if(!mqttClient.isConnected()){
            return;
        }
        try {
            mqttClient.publish(topic, message.getBytes(), config.getQos(), config.isRetained());
        } catch (MqttException e) {
            throw new MqttError(e,"public2Topic:"+topic+" occur error");
        }
    }

    /**
     * 当需要自己发送心跳保持tcp 长连接时可以调用这个方法（MqttCilent 自己会发心跳，但是貌似某些奇葩机型上会不发）
     */
    private synchronized void sendHeartbeat() throws MqttError {
        public2Topic(config.getKeepAliveTopic(),"");
    }

    public void setCallBack(MqttThreadCallBack callBack) {
        this.callBack = callBack;
    }

    private void responeError(final MqttError mqttError){
        if(callBack == null){
            return;
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                callBack.Error(mqttError);
            }
        });
    }

    @Override
    public void connectionLost(Throwable cause) {
        if(isNetWorkAvailable()){
            try {
                InitConnect();
            } catch (MqttError mqttError) {
                responeError(mqttError);
            }
        }else{
            try {
                disconnect();
            } catch (MqttError error) {
                responeError(error);
            }
        }
    }

    public void finish(){
        this.interrupt();
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    private boolean isNetWorkAvailable(){
        if(connectivityManager == null) {
            connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if(networkInfo == null){
            return false;
        }
        return networkInfo.isConnected();
    }
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        if(callBack == null){
            return;
        }
        handler.post(new messageArrivedRunnable(topic,message){
            @Override
            public void run() {
                callBack.messageArrived(this.topic,this.message);
            }
        });
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        //TODO:暂时不处理
    }

    public static abstract class messageArrivedRunnable implements Runnable {
        public String topic;
        public MqttMessage message;

        public messageArrivedRunnable(String topic, MqttMessage message) {
            this.topic = topic;
            this.message = message;
        }
    }

    public interface MqttThreadCallBack{
        public void messageArrived(String topic, MqttMessage message);
        public void Error(MqttError error);
    }
}
