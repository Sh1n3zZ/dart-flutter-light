import 'dart:async';

import 'package:flutter/services.dart';
import 'dart:io' show Platform;

/// Custom Exception for the plugin,
/// thrown whenever the plugin is used on platforms other than Android
class LightException implements Exception {
  String cause;
  LightException(this.cause);
  @override
  String toString() => "$runtimeType - $cause";
}

class Light {
  static Light? _singleton;
  static const EventChannel _eventChannel =
      const EventChannel("light.eventChannel");
  static const MethodChannel _methodChannel =
      MethodChannel("light.methodChannel");

  /// Constructs a singleton instance of [Light].
  ///
  /// [Light] is designed to work as a singleton.
  factory Light() => _singleton ??= Light._();

  Light._();

  Stream<int>? _lightSensorStream;

  /// 获取所有可用的照度传感器列表
  Future<List<LightSensor>> getLightSensors() async {
    if (!Platform.isAndroid) {
      throw LightException('Light sensor API only available on Android.');
    }

    final List<dynamic> sensors =
        await _methodChannel.invokeMethod('getLightSensors');
    return sensors.map((sensor) => LightSensor.fromMap(sensor)).toList();
  }

  /// 设置要使用的照度传感器
  Future<bool> setLightSensor(int sensorId) async {
    if (!Platform.isAndroid) {
      throw LightException('Light sensor API only available on Android.');
    }

    return await _methodChannel.invokeMethod('setLightSensor', {
      'sensorId': sensorId,
    });
  }

  /// The stream of light events.
  /// Throws a [LightException] if device isn't on Android.
  Stream<int> get lightSensorStream {
    if (!Platform.isAndroid)
      throw LightException('Light sensor API only available on Android.');

    return _lightSensorStream ??=
        _eventChannel.receiveBroadcastStream().map((lux) => lux);
  }
}

/// 照度传感器信息类
class LightSensor {
  final String name;
  final String vendor;
  final int id;

  LightSensor({
    required this.name,
    required this.vendor,
    required this.id,
  });

  factory LightSensor.fromMap(Map<dynamic, dynamic> map) {
    return LightSensor(
      name: map['name'] as String,
      vendor: map['vendor'] as String,
      id: map['id'] as int,
    );
  }
}
