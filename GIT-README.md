# Real Estate Management System

## Table of contents

- [1. Team responsibilities](#1-team-responsibilities)
- [2. Team agreements](#2-team-agreements)
- [3. Required software and versions](#3-required-software-and-versions)
- [4. First-time setup ](#4-first-time-setup)
- [5. Plan work with GitHub Issues](#5-plan-work-with-github-issues)
- [6. Daily workflow — every task](#6-daily-workflow--every-task)
  - [A. Start from current main](#a-start-from-current-main)
  - [B. Implement and commit](#b-implement-and-commit)
  - [C. Incorporate teammates' changes](#c-incorporate-teammates-changes)
  - [D. Push and open a pull request](#d-push-and-open-a-pull-request)
  - [E. Review and merge](#e-review-and-merge)
  - [F. Update after merging](#f-update-after-merging)
- [7. Resolve a merge conflict](#7-resolve-a-merge-conflict)
- [8. Database and shared-file coordination](#8-database-and-shared-file-coordination)
- [9. Quick troubleshooting](#9-quick-troubleshooting)

## 1. Team responsibilities

| Owner | Area | Main classes | First feature |
| --- | --- | --- | --- |
| Lavinia | Property & Listing Management | Proprietate, Oferta, ImgOferta, Zona, Agent, AgentZona | Create a property and listing |
| Iasmina | Client & Viewing Management | Persoana, Cerere, CerereZona, Interes, Vizionare | Register a client and schedule a viewing |
| Silviu | Transaction & Contract Management | Tranzactie (Rezultat), PropunerePret, AgentRezultat, Contract | Start a transaction and record a price proposal |

Ownership means coordinating changes; teammates can contribute to any area.
Before changing shared classes such as Persoana or Agent, agree with the other owners.

## 2. Team agreements

- One repository, one Spring Boot application, and one shared `main` branch.
- One short-lived branch per task; no permanent branch per person.
- All changes after initial setup go through a pull request (PR).
- One teammate reviews each PR before it is merged.
- Keep PRs small enough to understand and test.
- Use Romanian domain names consistently, matching the diagrams. 
- Use BigDecimal for money, enums for states, and Integer for initial domain IDs.
- Business operations protect state transitions; avoid unrestricted state setters.
- Keep main buildable. Do not merge code that breaks another area's existing behavior.

## 3. Required software and versions

| Tool | Agreement |
| --- | --- |
| Git | Install a recent supported version |
| Java | JDK 25, matching the supplied course example |
| Spring Boot | Use the exact version committed in pom.xml (4.1.1)|
| Maven | Use the committed Maven Wrapper |
| IDE | IntelliJ IDEA |
| PostgreSQL | Add when persistence is implemented; |

Check your installation:

```powershell
git --version
java -version
```
The IDE Project SDK, language level, and Maven runner JDK must match the chosen Java version. After cloning, also check:

```powershell
.\mvnw.cmd -version
```

Maven Wrapper chooses Maven, not necessarily your Java installation. Check the Java version printed by this command.

Configure your Git identity once, using your own details.

```powershell
git config --global user.name "Your Name"
git config --global user.email "your-email@example.com"
```

Authenticate through your IDE, GitHub Desktop, Git Credential Manager, or SSH. Do not put tokens in repository URLs or source files.

## 4. First-time setup 

Clone it once:

```powershell
git clone https://github.com/sara-bellum/msd-REmanagement.git
cd msd-REmanagement
.\mvnw.cmd -version
.\mvnw.cmd clean verify
```

Open the cloned folder as a Maven project in your IDE. Do not create a separate unrelated project and copy its files over the repository.

Run the application:

```powershell
.\mvnw.cmd spring-boot:run
```

Use Ctrl+C to stop it. Starting successfully does not imply a web page exists; the initial domain project may have no controllers or frontend.

Database configuration becomes necessary when database dependencies and persistence are introduced.

## 5. Plan work with GitHub Issues

Create one issue per small feature or fix. Include:

- What the user should be able to do.
- Business rules and expected failures.
- Shared classes or interfaces affected.
- A short acceptance checklist.

Assign one owner. Use a simple board: Todo → In Progress → In Review → Done.

Example issue: **Create a listing for a registered owner**

- Existing owner and zone are required.
- Listing price must be positive.
- Initial state is NOUA.
- Tests cover successful creation and rejection of invalid prices.

## 6. Daily workflow — every task

### A. Start from current main

First check that your working tree is clean. Commit work on its task branch before switching. If it is not ready to commit, use your IDE's stash feature and remember to restore it later.

```powershell
git status
git switch main
git pull --ff-only origin main
git switch -c feature/12-create-listing
```

Use the real issue number. Suggested branch names:

- feature/12-create-listing
- feature/18-schedule-viewing
- fix/24-prevent-double-booking
- docs/30-update-setup

### B. Implement and commit

Make one coherent change. Run relevant tests, then inspect and stage the intended files.

```powershell
.\mvnw.cmd test
git status
git diff
git add src/main/java/ro/agentie/domain/listing/Oferta.java
git add src/test/java/ro/agentie/domain/listing/OfertaTest.java
git diff --cached
git commit -m "feat: create listings with a positive price"
```

The paths above are examples; use your actual paths. Prefer explicit paths to avoid including unrelated files. Multiple commits on a task branch are fine.

### C. Incorporate teammates' changes

For a branch that has been open while main changed, first commit your current changes, then:

```powershell
git fetch origin
git merge origin/main
.\mvnw.cmd clean verify
```

This team uses merges to update task branches, avoiding history rewrites and force pushes. Resolve conflicts before proceeding.

### D. Push and open a pull request

```powershell
git push -u origin feature/12-create-listing
```

On GitHub, create a PR with **base: main** and **compare: your task branch**. Request one teammate as reviewer. Open it as a draft if it is incomplete.

Suggested PR description:

```markdown
## What changed
Creates listings for existing owners and zones.

## Validation
- Creation test passes.
- Zero and negative prices are rejected.
- Maven clean verify passes.

## Shared changes
List modified shared classes, or write "None".

Closes #12
```

Push later fixes to the same branch; the PR updates automatically. After the first push, `git push` is enough.

### E. Review and merge

Reviewer checks:

- Does the change satisfy the issue?
- Are business rules enforced in the appropriate place?
- Are shared methods and relationships still compatible?
- Do meaningful tests pass, with no unexpected skips?
- Are credentials, build output, and unrelated files excluded?

After approval and successful checks, squash merge the PR into main and delete the remote task branch. If substantive changes are pushed after approval, ask the reviewer to review again.

### F. Update after merging

With a clean working tree:

```powershell
git switch main
git pull --ff-only origin main
git fetch --prune
```

Optionally delete the local task branch. After squash merging, Git may report it as unmerged because its original commits are not on main. Keep it if unsure; delete it through the IDE only after confirming its changes are in main. Start the next task from updated main on a new branch.

## 7. Resolve a merge conflict

A conflict means Git needs help combining changes; it does not mean anyone did something wrong.

1. Run `git status` to identify conflicting files.
2. Open the IDE merge editor or inspect the conflict markers.
3. Read both changes and produce the correct combined code. Do not blindly accept one side for a shared class.
4. Remove all conflict markers and save.
5. Stage each resolved file and finish the merge.

```powershell
git add path/to/resolved-file.java
git commit
.\mvnw.cmd clean verify
git push
```

If you need to cancel an unfinished merge, run `git merge --abort`. Then coordinate with the teammate who changed the same area. Do not use reset --hard or force push as routine conflict fixes.


## 8. Database and shared-file coordination

- Discuss changes to shared entities and public interfaces before coding.
- Mention affected owners in the issue or PR.
- Coordinate pom.xml dependency changes and avoid unrelated formatting changes.
- When PostgreSQL is introduced, we use versioned Flyway migrations rather than manually maintaining different schemas.
- Coordinate migration numbers so two branches do not introduce the same version. Do not edit a migration already applied to shared environments; add a new one.
- Keep database passwords and personal data out of commits. Use synthetic sample data.
- Agree on listing, viewing, transaction, and contract state transitions before implementing cross-domain workflows.

## 9. Quick troubleshooting

| Problem | What to check |
| --- | --- |
| Java compilation or class-version error | java -version, Maven's Java version, IDE SDK, and pom.xml |
| mvnw.cmd not found | Run from the folder containing pom.xml; verify Wrapper files are committed |
| GitHub push rejected | Verify branch and permissions; fetch and merge the appropriate remote branch, then retry without forcing |
| Cannot switch branches | Check git status; commit or stash current changes first |
| Tests are not running | Check skip settings, test naming, and test-runner configuration |
| Port already in use | Stop the other running application or configure a local port |
| Lombok-generated methods appear missing | Reload Maven and check annotation processing/IDE support |
| Code works only on one machine | Compare Java versions, configuration, dependencies, and sample data |
