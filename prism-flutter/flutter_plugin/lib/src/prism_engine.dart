import 'package:flutter/services.dart';

class PrismEngine {
  static const MethodChannel _channel = MethodChannel('engine.prism.flutter/engine');

  bool _isInitialized = false;
  bool get isInitialized => _isInitialized;

  Future<bool> initialize({String appName = 'Prism App', int targetFps = 60}) async {
    final result = await _channel.invokeMethod<bool>('initialize', {
      'appName': appName,
      'targetFps': targetFps,
    });
    _isInitialized = result ?? false;
    return _isInitialized;
  }

  Future<void> shutdown() async {
    await _channel.invokeMethod('shutdown');
    _isInitialized = false;
  }

  Future<void> resize(int width, int height) async {
    await _channel.invokeMethod('resize', {
      'width': width,
      'height': height,
    });
  }

  Future<void> setClearColor(double r, double g, double b, double a) async {
    await _channel.invokeMethod('setClearColor', {
      'r': r,
      'g': g,
      'b': b,
      'a': a,
    });
  }
}
