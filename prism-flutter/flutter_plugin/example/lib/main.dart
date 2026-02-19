import 'dart:async';

import 'package:flutter/material.dart';
import 'package:prism_flutter/prism_flutter.dart';

void main() {
  runApp(const PrismDemoApp());
}

class PrismDemoApp extends StatelessWidget {
  const PrismDemoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Prism Flutter Demo',
      theme: ThemeData(
        colorSchemeSeed: Colors.blue,
        useMaterial3: true,
      ),
      home: const PrismDemoPage(),
    );
  }
}

class PrismDemoPage extends StatefulWidget {
  const PrismDemoPage({super.key});

  @override
  State<PrismDemoPage> createState() => _PrismDemoPageState();
}

class _PrismDemoPageState extends State<PrismDemoPage> {
  final _engine = PrismEngine();
  double _rotationSpeed = 45.0;
  bool _isPaused = false;

  @override
  void dispose() {
    unawaited(_engine.shutdown());
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Prism 3D Demo')),
      body: Column(
        children: [
          // 3D render view
          Expanded(
            child: PrismRenderView(engine: _engine),
          ),
          // Controls
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              children: [
                // Rotation speed slider
                Row(
                  children: [
                    const Text('Speed'),
                    Expanded(
                      child: Slider(
                        value: _rotationSpeed,
                        min: 0,
                        max: 360,
                        onChanged: (value) {
                          setState(() => _rotationSpeed = value);
                          _engine.setRotationSpeed(value);
                        },
                      ),
                    ),
                    Text('${_rotationSpeed.toInt()}°/s'),
                  ],
                ),
                // Color buttons + pause
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    _colorButton('Blue', 0.3, 0.5, 0.9),
                    _colorButton('Red', 0.9, 0.2, 0.2),
                    _colorButton('Green', 0.2, 0.8, 0.3),
                    _colorButton('Gold', 0.9, 0.7, 0.1),
                    FilledButton.icon(
                      onPressed: () {
                        setState(() => _isPaused = !_isPaused);
                        _engine.togglePause();
                      },
                      icon: Icon(_isPaused ? Icons.play_arrow : Icons.pause),
                      label: Text(_isPaused ? 'Resume' : 'Pause'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _colorButton(String label, double r, double g, double b) {
    return FilledButton(
      onPressed: () => _engine.setCubeColor(r, g, b),
      style: FilledButton.styleFrom(
        backgroundColor: Color.fromRGBO(
          (r * 255).toInt(),
          (g * 255).toInt(),
          (b * 255).toInt(),
          1.0,
        ),
      ),
      child: Text(label, style: const TextStyle(color: Colors.white)),
    );
  }
}
