## ADDED Requirements

### Requirement: Platform format checks

The publish modal SHALL show format checks before publishing.

#### Scenario: Check title and body length
- **WHEN** the publish modal opens
- **THEN** it SHALL show title/body length checks for WeChat and Xiaohongshu

#### Scenario: Check prohibited placeholder rules
- **WHEN** the draft contains placeholder prohibited expressions
- **THEN** the modal SHALL flag the matching rule and term

### Requirement: Export content

The publish modal SHALL export platform-specific content.

#### Scenario: Copy WeChat HTML
- **WHEN** the user clicks copy WeChat HTML
- **THEN** the clipboard SHALL receive HTML converted from the draft

#### Scenario: Copy Xiaohongshu body
- **WHEN** the user clicks copy Xiaohongshu body
- **THEN** the clipboard SHALL receive plain text suited for Xiaohongshu

### Requirement: Semi-automatic publishing

The publish modal SHALL use a semi-automatic flow.

#### Scenario: Open platform backend
- **WHEN** the user clicks open backend
- **THEN** the application SHALL open the selected platform backend in a new tab
