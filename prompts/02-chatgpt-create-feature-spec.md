# Prompt 02 — ChatGPT Create Feature / Unit Spec

Use this in ChatGPT after your Project Brain files already exist.

> Inside Claude Code, `/brain-spec` does the same job with the context files already loaded.

```txt
Act as my senior prompt engineer and spec writer.

I am using this workflow:
- ChatGPT writes the feature/unit spec
- Claude Code implements the spec
- The context files are the source of truth

Here is the project context:

[PASTE RELEVANT CONTEXT FILES OR SUMMARIES]

I want to create the next unit spec for:

[DESCRIBE THE FEATURE, SECTION, ASSET, OR TASK]

Create a complete unit spec using this structure:

# Unit Spec: [Unit Name]

## Status
- Status: Ready
- Owner: Claude Code
- Created: [DATE]

## Goal

## Why This Matters

## Scope
### In Scope
### Out of Scope

## Relevant Context Files

## Implementation Details
### Files / Areas to Create or Modify
### Behavior / Requirements
### Design / UX / Content Requirements
### Data / State / Storage Requirements
### Error / Empty / Edge States

## Acceptance Criteria

## Checks to Run

## Progress Tracker Update Required

## Decision Log Update Required

Rules:
- Make the unit small enough for Claude Code to complete in one focused session.
- Be explicit about what Claude must not touch.
- Include acceptance criteria that are easy to verify.
- If the request is too broad, split it into smaller unit specs and tell me the recommended order.
```
