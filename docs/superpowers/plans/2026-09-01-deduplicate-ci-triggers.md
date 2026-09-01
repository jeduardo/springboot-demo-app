# Deduplicate CI Triggers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Run the test workflow once for each feature-branch update while preserving validation for pull requests and pushes to `main`.

**Architecture:** Restrict the workflow's `push` event to `main` and its `pull_request` event to pull requests targeting `main`. GitHub's `pull_request` `synchronize` event then owns feature-branch validation, while the `push` event covers direct pushes and merged commits on `main`.

**Tech Stack:** GitHub Actions YAML, Ruby/Psych configuration assertions, GitHub CLI

---

### Task 1: Scope Workflow Events to Main

**Files:**
- Modify: `.github/workflows/test.yml:3-5`

- [ ] **Step 1: Run a failing trigger assertion**

Run:

```bash
ruby -e '
  require "yaml"
  workflow = YAML.safe_load(File.read(".github/workflows/test.yml"), aliases: true)
  events = workflow["on"] || workflow[true]
  abort "push must target only main" unless events.dig("push", "branches") == ["main"]
  abort "pull_request must target only main" unless events.dig("pull_request", "branches") == ["main"]
'
```

Expected: FAIL with `push must target only main` because both event mappings are currently unfiltered.

- [ ] **Step 2: Restrict both event mappings**

Replace the trigger block in `.github/workflows/test.yml` with:

```yaml
on:
  push:
    branches:
      - main
  pull_request:
    branches:
      - main
```

- [ ] **Step 3: Run the trigger assertion again**

Run:

```bash
ruby -e '
  require "yaml"
  workflow = YAML.safe_load(File.read(".github/workflows/test.yml"), aliases: true)
  events = workflow["on"] || workflow[true]
  abort "push must target only main" unless events.dig("push", "branches") == ["main"]
  abort "pull_request must target only main" unless events.dig("pull_request", "branches") == ["main"]
  puts "workflow-triggers-ok"
'
```

Expected: PASS with `workflow-triggers-ok`.

- [ ] **Step 4: Verify the project and workflow diff**

Run:

```bash
mvn --batch-mode clean verify
git diff --check
git diff -- .github/workflows/test.yml
```

Expected: Maven reports `BUILD SUCCESS`, the whitespace check emits no output, and the diff contains only the event filters.

- [ ] **Step 5: Commit the workflow change**

```bash
git add .github/workflows/test.yml
git commit -m "ci: avoid duplicate pull request runs"
```

- [ ] **Step 6: Push and verify one workflow run**

Run:

```bash
git push
head_sha=$(git rev-parse HEAD)
gh run list --commit "$head_sha" --workflow "Run tests" --json databaseId,event,status,conclusion
```

Expected: exactly one run for the new commit, with event `pull_request`; no `push` run exists for the feature branch.
