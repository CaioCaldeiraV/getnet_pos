import 'package:flutter/services.dart';

class GetnetPos {
  static const MethodChannel _channel = const MethodChannel('getnet_pos');

  static Future<String> getMifareCardSN() async =>
      await _channel.invokeMethod('getMifare');
}
