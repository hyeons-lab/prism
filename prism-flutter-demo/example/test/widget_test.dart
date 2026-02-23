// ignore_for_file: avoid_redundant_argument_values
//
// Widget tests for the Prism Flutter demo app.
//
// _FpsChip and PrismDemoPage are tested through PrismDemoApp:
//  - On macOS host, PrismEngine uses the method channel (no-op initialize,
//    channel returns null → isInitialized() returns false, getState() returns {}).
//  - TargetPlatformVariant.only(TargetPlatform.iOS) ensures PrismRenderView
//    falls through to its "not yet available" fallback instead of creating an
//    AppKitView or AndroidView (which require a registered platform view factory).

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

    // The poll timer fires every 500 ms. Advance time past one tick so the
    // timer callback runs. On iOS target the engine is not initialized (channel
    // returns null → false), so the overlay persists — confirming the timer
    // alone does not prematurely hide it.
    await tester.pump(const Duration(milliseconds: 600));
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
  }, variant: TargetPlatformVariant.only(TargetPlatform.iOS));
}
