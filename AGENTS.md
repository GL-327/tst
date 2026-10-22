# Repository workflow

The user requested on 22 October 2026 that every completed mod update be pushed to
GitHub and Minecraft be launched afterward. Follow this preference for future updates
unless the user changes it.

- Preserve existing work and saves. Fetch GitHub before updating and reconcile changes.
- Run checks appropriate to the changes and package the new version before publishing.
- Commit relevant source, resource, tooling and documentation changes, then push the
  current branch to its configured GitHub remote. Never force-push as part of this workflow.
- After a successful update, launch the mod with Java 25 using
  `gradlew.bat runClient -PclientRunDir=run/rework-preview`. Keep an existing user game
  session intact; do not kill it or overwrite its world to launch the update.
- Confirm the actual push and game startup. If either fails, report the specific failure.
- Keep runtime logs, caches, saves and temporary screenshots/backups out of commits.
  Original source artwork may be committed when needed by the project.
