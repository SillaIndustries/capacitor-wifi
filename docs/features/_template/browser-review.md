# Browser Review Guide

Keep this guide current while its milestone is under review. After acceptance, retain the completed guide as dated milestone evidence outside the default reading path. Create the next guide from the behavior actually available at its checkpoint.

Status: Not ready

Current milestone: <Milestone ID and name>

Last updated: YYYY-MM-DD

## What You Can Review

Describe the user-visible outcome available in this milestone in one short paragraph.

Not available yet:

- List later or excluded behavior that could otherwise look broken or missing.

## Before You Start

List only what the reviewer needs, such as:

1. Start the local application with `<command>`.
2. Open `<address>`.
3. Sign in with a suitable test account, if required.
4. Prepare or select `<simple test data>`.

## Review Steps

### 1. <First user workflow>

1. Use interface labels exactly as they appear.
2. Perform one understandable action at a time.

Expected result:

- Describe what the reviewer should see or feel in plain language.

### 2. <Important failure or recovery workflow>

1. Trigger the relevant condition safely.
2. Follow the visible recovery choices.

Expected result:

- Describe the safe and understandable outcome.

### 3. Keyboard and narrow-screen review

Include this section only when relevant to the milestone.

1. Complete the core flow using the keyboard.
2. Repeat it at the named narrow-screen size.

Expected result:

- Focus is visible, controls remain understandable, and the workflow remains usable.

## Record the Review

Review date:

Reviewer:

Review phase: `Implementation review` or `Final validation`

Outcome: `Passed`, `Changes requested`, or `Blocked`

Findings:

- None recorded.

Detailed engineering coverage: [Verification](verification.md)

Passing an implementation review starts the mandatory hardening review. Only a passing final validation can support definitive milestone acceptance.
