package lzq.com.mqttpush.thread;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Administrator on 2016/5/12.
 */

@StringDef({MqttProtocol.PROTOCOL_LOCAL,MqttProtocol.PROTOCOL_SSL,MqttProtocol.PROTOCOL_TCP})
@Retention(RetentionPolicy.RUNTIME)
public @interface MqttProtocol {
    String PROTOCOL_TCP = "tcp";
    String PROTOCOL_SSL = "ssl";
    String PROTOCOL_LOCAL = "local";
}
