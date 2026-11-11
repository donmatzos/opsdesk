# Deploy rollback

Use this when a deploy to dev or production went wrong: the `deploy-dev` job failed its smoke test, a release behaves badly, or users report errors that began right after a rollout.

## Symptoms
- The pipeline's deploy step or the post-deploy smoke test failed (`tools/smoke-test.sh` reports which check broke).
- Error rate or latency changes right after a new image tag was rolled out.
- `helm history opsdesk` shows a new revision that is `failed` or `pending-upgrade`.

## First checks
- Which revision is live? `helm history opsdesk -n <namespace>` lists revisions with the image tag in the description; compare with the last revision that was `deployed` and healthy.
- Did the pipeline already roll back? The `deploy-dev` job (GitHub Actions), the GitLab job `deploy-dev`, the Jenkins stage "Deploy dev" and the AWX template all roll back by themselves when the smoke test fails, unless it was the first release.
- Check the Route answers: `oc get route opsdesk -n <namespace>` and then `tools/smoke-test.sh https://<host> 180`.

## Mitigation
- Roll back to the previous revision: `helm rollback opsdesk -n <namespace> --wait`; to pick a revision, `helm rollback opsdesk <revision> -n <namespace> --wait`.
- Confirm with `tools/smoke-test.sh https://<host> 180`, then `oc get pods -n <namespace>` shows every pod ready.
- Stop the bad tag from coming back: revert the commit on `main`, or fix forward in a branch, before the next deploy runs.
- Where AWX runs the deploy, launch the job template again with the previous image tag instead of rolling back by hand.

## Escalation
- The rollback itself fails or the smoke test still fails afterwards: open a SEV1 incident (`sev1-incident`) and page the service owner.
- If a first release fails there is nothing to roll back to: fix the cause (see `pod-not-ready`) and deploy again.

## Related
- `pod-not-ready`, `http-errors`, `sev1-incident`
- Write a postmortem for any rollback in production (`postmortem-template`).
