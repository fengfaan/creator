## ADDED Requirements

### Requirement: Get configuration value
The system SHALL provide an API endpoint `GET /api/v1/settings/{key}` that returns the value associated with the given configuration key from the SQLite database.

#### Scenario: Get existing config
- **WHEN** a valid config key is requested that exists in the database
- **THEN** the API returns 200 with the config key, value, and metadata

#### Scenario: Get non-existent config
- **WHEN** a config key is requested that does not exist
- **THEN** the API returns 200 with a null value (config is optional)

### Requirement: Set configuration value
The system SHALL provide an API endpoint `POST /api/v1/settings` that accepts a JSON body with `key` and `value`, and persists the configuration to SQLite. If the key already exists, the value SHALL be updated.

#### Scenario: Set new config
- **WHEN** a new key-value pair is provided
- **THEN** the config is inserted into the database and the API returns 200

#### Scenario: Update existing config
- **WHEN** a key that already exists is provided with a new value
- **THEN** the existing value is updated and the API returns 200

### Requirement: List all configurations
The system SHALL provide an API endpoint `GET /api/v1/settings` that returns all configuration items.

#### Scenario: List all configs
- **WHEN** the endpoint is called
- **THEN** the API returns 200 with an array of all config items including key, value, and updatedAt timestamp

### Requirement: SQLite schema initialization
The system SHALL automatically create the SQLite database and required tables on first startup if they do not exist.

#### Scenario: First startup creates tables
- **WHEN** the application starts and config.db does not exist
- **THEN** the SQLite file is created at `~/.ai-publisher/config.db` with the settings table

#### Scenario: Existing database is preserved
- **WHEN** the application starts and config.db already exists
- **THEN** no schema changes are made, existing data is preserved
