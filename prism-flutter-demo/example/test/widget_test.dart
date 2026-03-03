// ignore_for_file: avoid_redundant_argument_values
//
// Widget tests for the Prism Flutter demo app.
//
// _FpsChip and PrismDemoPage are tested through PrismDemoApp:
//  - TargetPlatformVariant.only(TargetPlatform.iOS) ensures PrismRenderView
//    returns SizedBox.shrink() (handle == 0 before initialize()) instead of
//    creating an AppKitView or AndroidView, which require a registered factory.
//  - On the test host, PrismEngine uses the FFI backend. _loadBindings() catches
//    the ArgumentError from the missing libprism and returns null, leaving
//    handle == 0 and isRendererReady == false — so the loading overlay stays.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:prism_flutter_example/main.dart';

void main() {
  testWidgets('fps chip shows 0 fps before first engine tick',
      (WidgetTester tester) async {
    await tester.pumpWidget(const PrismDemoApp());

    // _FpsChip renders fps.toStringAsFixed(0) + ' fps'; initial _fps == 0.0.
    expect(find.text('0 fps'), findsOneWidget);
  }, variant: TargetPlatformVariant.only(TargetPlatform.iOS));

  testWidgets('loading overlay is visible before engine initializes',
      (WidgetTester tester) async {
    await tester.pumpWidget(const PrismDemoApp());

    // The overlay (CircularProgressIndicator) is shown while _isInitialized == false.
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
  }, variant: TargetPlatformVariant.only(TargetPlatform.iOS));

  testWidgets('loading overlay disappears once isInitialized becomes true',
      (WidgetTester tester) async {
    await tester.pumpWidget(const PrismDemoApp());
    expect(find.byType(CircularProgressIndicator), findsOneWidget);

    // Advance a few frames via the Ticker. On the iOS target variant the FFI
    // engine has handle == 0 (no libprism in test binary), so isRendererReady
    // is false and the overlay persists — confirming the Ticker alone does not
    // prematurely hide it.
    await tester.pump(const Duration(milliseconds: 600));
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
  }, variant: TargetPlatformVariant.only(TargetPlatform.iOS));
}
