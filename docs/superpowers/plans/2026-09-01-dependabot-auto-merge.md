# Dependabot Auto-Merge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a GitHub Actions workflow that requests squash auto-merge for Dependabot patch and minor updates after required checks pass.

**Architecture:** A `pull_request_target` workflow runs on Dependabot pull request lifecycle events. A single guarded job fetches Dependabot metadata and invokes `gh pr merge --auto --squash` only for semver patch/minor updates.

**Tech Stack:** GitHub Actions YAML, Dependabot metadata action v3, GitHub CLI.

---

### Task 1: Add the Dependabot auto-merge workflow

**Files:**
- Create: `.github/workflows/dependabot-auto-merge.yml`

- [ ] **Step 1: Create the workflow with the approved event and permission policy**

Create `.github/workflows/dependabot-auto-merge.yml` with this exact content:

```yaml
name: Dependabot auto-merge

on:
  pull_request_target:
    types:
      - opened
      - reopened
      - synchronize

permissions:
  contents: write
  pull-requests: write

jobs:
  auto-merge:
    if: github.event.pull_request.user.login == 'dependabot[bot]'
    runs-on: ubuntu-latest

    steps:
      - name: Get Dependabot metadata
        id: metadata
        uses: dependabot/fetch-metadata@v3

      - name: Enable auto-merge for patch and minor updates
        if: |
          steps.metadata.outputs.update-type == 'version-update:semver-patch' ||
          steps.metadata.outputs.update-type == 'version-update:semver-minor'
        env:
          GH_TOKEN: ${{ github.token }}
          PR_URL: ${{ github.event.pull_request.html_url }}
        run: gh pr merge --auto --squash "$PR_URL"
```

- [ ] **Step 2: Validate the workflow syntax and policy**

Run:

```bash
ruby -e 'require "yaml"; YAML.load_file(".github/workflows/dependabot-auto-merge.yml")'
rg -n "pull_request_target|dependabot\[bot\]|contents: write|pull-requests: write|semver-patch|semver-minor|gh pr merge --auto --squash" .github/workflows/dependabot-auto-merge.yml
```

Expected: YAML parsing succeeds, and the search shows the approved event, actor guard, permissions, patch/minor conditions, and squash auto-merge command.

- [ ] **Step 3: Run the repository test suite**

Run:

```bash
mvn test
```

Expected: Maven exits with status 0 and all existing tests pass. The workflow-only change is not expected to alter application tests.

- [ ] **Step 4: Review the scoped diff**

Run:

```bash
git diff --check
git diff -- .github/workflows/dependabot-auto-merge.yml
git status --short
```

Expected: no whitespace errors; the workflow diff contains only the intended new file; unrelated pre-existing user modifications remain untouched.

- [ ] **Step 5: Commit only the workflow**

```bash
git add .github/workflows/dependabot-auto-merge.yml
git commit -m "ci: auto-merge low-risk Dependabot updates"
```

Expected: a commit is created containing only `.github/workflows/dependabot-auto-merge.yml`.
