## ADDED Requirements

### Requirement: File tree listing
The system SHALL provide an API endpoint `GET /api/v1/files` that returns the file tree under `~/.ai-publisher/articles/`, including folders and `.md` files with their names, paths, and types (file/folder).

#### Scenario: List files in empty directory
- **WHEN** the articles directory exists but is empty
- **THEN** the API returns a 200 response with an empty array

#### Scenario: List files with nested structure
- **WHEN** the articles directory contains folders and .md files
- **THEN** the API returns a tree structure with each item having `id`, `name`, `path`, `type` (file/folder), and `children` (for folders)

### Requirement: Read file content
The system SHALL provide an API endpoint `GET /api/v1/files/content` that accepts a file path query parameter and returns the content of the specified `.md` file.

#### Scenario: Read existing file
- **WHEN** a valid .md file path is provided
- **THEN** the API returns 200 with the file content as plain text

#### Scenario: Read non-existent file
- **WHEN** a non-existent file path is provided
- **THEN** the API returns 404 with an error message

#### Scenario: Read file outside articles directory
- **WHEN** a path traversal attempt is made (e.g. `../../etc/passwd`)
- **THEN** the API returns 403 with an error message

### Requirement: Save file content
The system SHALL provide an API endpoint `POST /api/v1/files/save` that accepts a JSON body with `path` and `content`, and writes the content to the specified `.md` file. If the file does not exist, it SHALL be created.

#### Scenario: Save to existing file
- **WHEN** a valid path and content are provided for an existing file
- **THEN** the file is overwritten with the new content and the API returns 200

#### Scenario: Save to new file
- **WHEN** a valid path is provided for a non-existent file
- **THEN** the file is created with the given content and the API returns 201

#### Scenario: Save with invalid path
- **WHEN** the path contains traversal characters or is outside articles directory
- **THEN** the API returns 403 with an error message

### Requirement: Create new document
The system SHALL provide an API endpoint `POST /api/v1/files/create` that accepts a JSON body with `name` and optional `folder`, and creates a new empty `.md` file in the specified location.

#### Scenario: Create document in root
- **WHEN** a name is provided without a folder
- **THEN** a new .md file is created in the articles root directory

#### Scenario: Create document in subfolder
- **WHEN** a name and folder path are provided
- **THEN** a new .md file is created in the specified subfolder

### Requirement: Delete file
The system SHALL provide an API endpoint `DELETE /api/v1/files` that accepts a file path query parameter and deletes the specified `.md` file.

#### Scenario: Delete existing file
- **WHEN** a valid file path is provided
- **THEN** the file is deleted and the API returns 200

#### Scenario: Delete non-existent file
- **WHEN** a non-existent file path is provided
- **THEN** the API returns 404

### Requirement: Rename file
The system SHALL provide an API endpoint `POST /api/v1/files/rename` that accepts `oldPath` and `newName`, and renames the file.

#### Scenario: Rename to valid name
- **WHEN** a valid old path and new name are provided
- **THEN** the file is renamed and the API returns 200 with the new path

#### Scenario: Rename to existing name
- **WHEN** the new name already exists in the same directory
- **THEN** the API returns 409 with an error message

### Requirement: Data directory initialization
The system SHALL automatically create the `~/.ai-publisher/` directory structure on first startup, including `articles/` and `assets/` subdirectories.

#### Scenario: First startup
- **WHEN** the application starts and `~/.ai-publisher/` does not exist
- **THEN** the directory structure is created automatically

#### Scenario: Subsequent startup
- **WHEN** the application starts and `~/.ai-publisher/` already exists
- **THEN** no action is taken, existing files are preserved
