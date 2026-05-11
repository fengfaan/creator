## ADDED Requirements

### Requirement: AI generation endpoint

The backend SHALL expose `POST /api/v1/ai/generate` for AI-assisted writing.

#### Scenario: Generate outline
- **WHEN** the client posts action `outline` with title/content
- **THEN** the backend SHALL call the configured OpenAI-compatible chat completions endpoint
- **AND** return generated text in `data.text`

#### Scenario: Polish content
- **WHEN** the client posts action `polish`
- **THEN** the backend SHALL request a polished Chinese rewrite based on the current draft

#### Scenario: Continue content
- **WHEN** the client posts action `continue`
- **THEN** the backend SHALL request continuation text based on the current draft

#### Scenario: Missing API key
- **WHEN** `ai_api_key` is not configured
- **THEN** the backend SHALL return a non-200 API response with a readable message

### Requirement: AI settings UI

The frontend SHALL provide an in-app settings panel for AI connection settings.

#### Scenario: Save AI settings
- **WHEN** the user saves API Key, Base URL, and model
- **THEN** the frontend SHALL persist them through `/api/v1/settings`

### Requirement: Real AI panel requests

The frontend AI panel SHALL call the backend instead of using local mock output.

#### Scenario: Run supported action
- **WHEN** the user chooses outline, polish, or continue
- **THEN** the panel SHALL post the current title/content/action to `/api/v1/ai/generate`
- **AND** display the returned text for insertion
