# Plan: fix Deploy Docs `cp` failure on `composeResources/`

## Thinking

The `Deploy Docs` workflow on `main` failed at the `Stage docs site` step
(run `24959988342`):

```
cp: -r not specified; omitting directory 'prism-demo-core/build/dist/wasmJs/productionExecutable/composeResources'
##[error]Process completed with exit code 1.
```

The current step uses a non-recursive copy:

```bash
cp prism-demo-core/build/dist/wasmJs/productionExecutable/* _site/wasm/
```

This worked when the WASM distribution was a flat set of files. The recent
WASM build now emits a `composeResources/` subdirectory inside
`productionExecutable/`, so plain `cp` skips it and exits non-zero.

Two reasonable options:

1. Add `-R` and use the `src/.` form to copy everything including any
   hidden files/subdirs into `_site/wasm/`:
   `cp -R prism-demo-core/build/dist/wasmJs/productionExecutable/. _site/wasm/`
2. Filter out `composeResources/` if the docs site doesn't need it.

Option 1 is the safest and matches the step's intent ("overlay freshly
built WASM artifacts"). The static demo HTML pages may eventually rely on
resources from that subdir; even if not, copying it is harmless and a few
KB. Option 2 risks dropping a file the demo needs without a clear payoff.

Going with option 1.

## Plan

1. Edit `.github/workflows/deploy-docs.yml`: change the bare `cp` to
   `cp -R …/productionExecutable/. _site/wasm/`.
2. Create devlog `devlog/000023-fix-deploy-docs-cp.md`.
3. Commit (`fix: copy WASM artifacts recursively in deploy-docs`).
4. Push and open a PR. Verify the next `Deploy Docs` run on `main` after
   merge succeeds (the workflow only triggers on `push` to `main`, not on
   PR branches).
