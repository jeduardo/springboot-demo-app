# Dependabot Auto-Merge Design

## Goal

Automatically merge low-risk Dependabot dependency updates in this repository once the repository's required checks pass.

## Design

Add `.github/workflows/dependabot-auto-merge.yml` using `pull_request_target` for pull request `opened`, `reopened`, and `synchronize` events. The job will run only when the pull request author is `dependabot[bot]`.

The job will fetch Dependabot metadata with `dependabot/fetch-metadata@v3`. It will enable GitHub's auto-merge using squash merging only for semver patch and minor updates. Major updates will require normal manual review. The workflow will grant only `contents: write` and `pull-requests: write` permissions, and will use the event's GitHub token and pull request URL.

## Behavior and safety

- Auto-merge is requested, not forced; GitHub will wait for required checks and branch protections.
- Non-Dependabot pull requests do not run the job.
- Major version updates are not auto-merged.
- No repository application code or runtime behavior changes.

## Verification

Validate the workflow YAML structure and inspect the final diff. Run the existing test suite to confirm the repository remains healthy.
