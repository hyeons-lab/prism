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
  double _metallic = 0.0;
  double _roughness = 0.5;
  double _envIntensity = 1.0;
  bool _isPaused = false;

  @override
  void dispose() {
    unawaited(_engine.shutdown());
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Prism PBR Demo')),
      body: Column(
        children: [
          // 3D render view — PBR sphere grid
          Expanded(
            child: PrismRenderView(engine: _engine),
          ),
          // PBR controls
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              children: [
                // Metallic slider
                Row(
                  children: [
                    const SizedBox(width: 80, child: Text('Metallic')),
                    Expanded(
                      child: Slider(
                        value: _metallic,
                        min: 0,
                        max: 1,
                        onChanged: (value) {
                          setState(() => _metallic = value);
                          _engine.setMetallic(value);
                        },
                      ),
                    ),
                    Text(_metallic.toStringAsFixed(2)),
                  ],
                ),
                // Roughness slider
                Row(
                  children: [
                    const SizedBox(width: 80, child: Text('Roughness')),
                    Expanded(
                      child: Slider(
                        value: _roughness,
                        min: 0,
                        max: 1,
                        onChanged: (value) {
                          setState(() => _roughness = value);
                          _engine.setRoughness(value);
                        },
                      ),
                    ),
                    Text(_roughness.toStringAsFixed(2)),
                  ],
                ),
                // Environment intensity slider
                Row(
                  children: [
                    const SizedBox(width: 80, child: Text('Env IBL')),
                    Expanded(
                      child: Slider(
                        value: _envIntensity,
                        min: 0,
                        max: 2,
                        onChanged: (value) {
                          setState(() => _envIntensity = value);
                          _engine.setEnvIntensity(value);
                        },
                      ),
                    ),
                    Text(_envIntensity.toStringAsFixed(2)),
                  ],
                ),
                // Pause button
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
          ),
        ],
      ),
    );
  }
}
