# Internal Allium Examples

This directory is the primary example set for the curriculum. The course no
longer requires a clone of Patina or patina-mct; those are useful remote
case studies, not prerequisites.

| Example | Use it for | Notes |
|---------|------------|-------|
| [`library-lending.allium`](library-lending.allium) | First syntax pass, Session 3 slide deck | Small state machine with a temporal observation. |
| [`access-grant-lifecycle.allium`](access-grant-lifecycle.allium) | Authority decisions, deny-by-default, Session 2 solution | Also appears as the worked solution for Exercise 02. |
| [`pinned-installer.allium`](pinned-installer.allium) + [`code/pinned-installer.sh`](code/pinned-installer.sh) | Distill-from-code demo | Shows platform gating, checksum refusal, and PATH notice behavior. |
| [`support-ticket-routing.allium`](support-ticket-routing.allium) | Larger product-map style reading | Shows safe customer projection vs operations-facing inspection. |
| [`ci/github-actions-allium.yml`](ci/github-actions-allium.yml) | CI integration example | Copy and adapt; it intentionally checks examples and solutions, not flawed starters. |

Run the examples as a bundle:

```bash
allium check examples
for spec in examples/*.allium; do allium analyse "$spec"; done
```

## Optional remote production examples

- `patina/layer/allium/mother/*.allium` — production lifecycle specs and
  obligation-plan drift checks.
- `patina-mct/layer/allium/mct-product-map.allium` — large product-map style
  spec with many decision-log entries and safe-projection contracts.

Treat those as comparative reading after the internal examples are familiar.
