import 'package:flutter/material.dart';
import 'package:prism_flutter/prism_flutter.dart';

void main() {
  runApp(const PrismExampleApp());
}

class PrismExampleApp extends StatelessWidget {
  const PrismExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Prism Engine Demo',
      theme: ThemeData(
        colorSchemeSeed: Colors.deepPurple,
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
  late PrismEngine engine;
  bool isInitialized = false;

  @override
  void initState() {
    super.initState();
    engine = PrismEngine();
    _initEngine();
  }

  Future<void> _initEngine() async {
    await engine.initialize(appName: 'Flutter Demo');
    setState(() {
      isInitialized = true;
    });
  }

  @override
  void dispose() {
    engine.shutdown();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Prism 3D Engine'),
      ),
      body: Column(
        children: [
          Expanded(
            child: isInitialized
                ? PrismRenderView(engine: engine)
                : const Center(child: CircularProgressIndicator()),
          ),
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                ElevatedButton(
                  onPressed: () {
                    engine.setClearColor(0.39, 0.58, 0.93, 1.0);
                  },
                  child: const Text('Blue'),
                ),
                ElevatedButton(
                  onPressed: () {
                    engine.setClearColor(0.2, 0.2, 0.2, 1.0);
                  },
                  child: const Text('Dark'),
                ),
                ElevatedButton(
                  onPressed: () {
                    engine.setClearColor(0.1, 0.6, 0.3, 1.0);
                  },
                  child: const Text('Green'),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
