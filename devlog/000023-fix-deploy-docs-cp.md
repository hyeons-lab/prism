# 000023 — fix/deploy-docs-cp

**Agent:** Claude (claude-opus-4-7) @ prism branch fix/deploy-docs-cp

## Intent

Fix the `Deploy Docs` workflow that has been failing on every push to `main`
since `composeResources/` started appearing in the WASM distribution. Run
`24959988342` (sha `ccda5e8`) is the most recent failure.

## What Changed

- 2026-05-08T19:18-0700 `.github/workflows/deploy-docs.yml` — change
  `Stage docs site` to use `cp -R productionExecutable/. _site/wasm/` so the
  new `composeResources/` subdirectory copies recursively instead of being
  skipped with a non-zero exit.
- 2026-05-08T19:18-0700 `devlog/plans/000023-01-deploy-docs-cp-fix.md` —
  plan documenting the failure and the chosen fix.

## Decisions

- 2026-05-08T19:18-0700 Recursive copy of the entire `productionExecutable/`
  tree (option 1 in the plan) over filtering out `composeResources/` (option 2).
  Reasoning: the step's intent is "overlay freshly built WASM artifacts",
  and the docs HTML may eventually depend on Compose resources. Filtering
  risks silently dropping a needed file with no real benefit; recursion is
  the conservative match for the original intent.
- 2026-05-08T19:18-0700 Use `cp -R src/.` rather than `cp -r src/*`. The
  trailing `/.` form includes hidden entries and avoids glob expansion
  edge cases on otherwise-empty directories.

## Issues

None encountered. Cannot trigger the workflow from a PR (it's `push: main`-only),
so verification has to happen on the post-merge run.

## Commits

- HEAD — fix: copy WASM artifacts recursively in deploy-docs
