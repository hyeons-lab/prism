(async () => {
  const script = [...document.querySelectorAll('script[data-module]')].pop();
  const moduleUrl = script?.dataset.module || 'prism-flutter.mjs';

  try {
    const mod = await import(`./${moduleUrl}`);
    if (typeof mod.default === "function") {
      await mod.default();
    }
    // Pin every exported function to window so Dart @JS() bindings resolve.
    for (const [name, value] of Object.entries(mod)) {
      if (typeof value === "function") window[name] = value;
    }
    window.dispatchEvent(new CustomEvent("prism-wasm-ready"));
  } catch (e) {
    console.error("Prism WASM load error:", e);
    window.dispatchEvent(new CustomEvent("prism-wasm-error", { detail: String(e) }));
  }
})();
