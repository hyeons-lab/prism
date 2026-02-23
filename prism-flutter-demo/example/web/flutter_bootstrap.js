{{flutter_js}}
{{flutter_build_config}}

_flutter.loader.load({
  onEntrypointLoaded: async function (engineInitializer) {
    // Use the HTML renderer so Flutter widgets (FPS chip, control panel)
    // composite correctly above the WebGPU HtmlElementView platform view.
    let appRunner = await engineInitializer.initializeEngine({
      renderer: "html",
    });
    await appRunner.runApp();
  },
});
