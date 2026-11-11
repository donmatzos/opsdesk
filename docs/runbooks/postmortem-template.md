# Postmortem: <short title of the incident>

> Blameless: describe what happened and what the system and process allowed, not who made a mistake. Never repeat a credential value.
> Write it within five working days; copy this file to `docs/postmortems/<date>-<slug>.md`.

## Summary
Two or three sentences: what broke, for whom, how long, how it ended.

## Impact
Who and what was affected, for how long, and how many users or requests. Say "not measured" when it is.

## Timeline (UTC)
| Time | Event |
|---|---|
| hh:mm | first sign (alert, report, deploy) |
| hh:mm | detection and who was told |
| hh:mm | mitigation started / finished |
| hh:mm | resolved |

## Detection
How was it found: monitor, user, test, person. Could it have been found sooner?

## Root cause
Use "five whys": keep asking why until you reach something a process or tool can change.

## What went well
## What went badly
## Where we got lucky

## Action items
| Action | Owner | Due | Done |
|---|---|---|---|
| ... | name | date | no |

## Related
Links to the incident in opsdesk, the runbook that applied, pull requests and monitors.
