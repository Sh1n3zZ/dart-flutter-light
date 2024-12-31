package dk.cachet.light;

import androidx.annotation.NonNull;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.MethodChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LightPlugin
 */
public class LightPlugin implements FlutterPlugin, EventChannel.StreamHandler {
    private SensorEventListener sensorEventListener = null;
    private SensorManager sensorManager = null;
    private Sensor sensor = null;
    private EventChannel eventChannel = null;
    private static final String STEP_COUNT_CHANNEL_NAME =
            "light.eventChannel";
    private static final String METHOD_CHANNEL_NAME = "light.methodChannel";
    private MethodChannel methodChannel;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPlugin.FlutterPluginBinding flutterPluginBinding) {
        /// Init sensor manager
        Context context = flutterPluginBinding.getApplicationContext();
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        
        // 获取所有照度传感器
        List<Sensor> lightSensors = sensorManager.getSensorList(Sensor.TYPE_LIGHT);
        if (!lightSensors.isEmpty()) {
            // 默认使用第一个传感器
            sensor = lightSensors.get(0);
        }

        /// Init event channel
        BinaryMessenger binaryMessenger = flutterPluginBinding.getBinaryMessenger();
        eventChannel = new EventChannel(binaryMessenger, STEP_COUNT_CHANNEL_NAME);
        eventChannel.setStreamHandler(this);

        // 初始化 method channel 用于获取传感器列表和切换传感器
        methodChannel = new MethodChannel(binaryMessenger, METHOD_CHANNEL_NAME);
        methodChannel.setMethodCallHandler(
            (call, result) -> {
                switch (call.method) {
                    case "getLightSensors":
                        List<Map<String, Object>> sensorsList = new ArrayList<>();
                        for (Sensor s : lightSensors) {
                            Map<String, Object> sensorInfo = new HashMap<>();
                            sensorInfo.put("name", s.getName());
                            sensorInfo.put("vendor", s.getVendor());
                            sensorInfo.put("id", s.getId());
                            sensorsList.add(sensorInfo);
                        }
                        result.success(sensorsList);
                        break;
                    case "setLightSensor":
                        int sensorId = call.argument("sensorId");
                        for (Sensor s : lightSensors) {
                            if (s.getId() == sensorId) {
                                // 如果有正在监听的传感器，先取消监听
                                if (sensorEventListener != null) {
                                    sensorManager.unregisterListener(sensorEventListener);
                                }
                                sensor = s;
                                // 如果当前正在监听事件，重新注册新的传感器
                                if (sensorEventListener != null) {
                                    sensorManager.registerListener(sensorEventListener, sensor, 
                                        SensorManager.SENSOR_DELAY_NORMAL);
                                }
                                result.success(true);
                                return;
                            }
                        }
                        result.error("SENSOR_NOT_FOUND", "找不到指定的传感器", null);
                        break;
                    default:
                        result.notImplemented();
                }
            }
        );
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        /// Cancel the handling of stream data
        eventChannel.setStreamHandler(null);
        onCancel(null);
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        /// Set up the event sensor for the light sensor
        sensorEventListener = createSensorEventListener(events);
        sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onCancel(Object arguments) {
        /// Finish listening to events
        sensorManager.unregisterListener(sensorEventListener);
    }
    
    SensorEventListener createSensorEventListener(final EventChannel.EventSink events) {
        return new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                /// Do nothing
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                /// Extract lux value and send it to Flutter via the event sink
                int lux = (int) event.values[0];
                events.success(lux);
            }
        };
    }
}
