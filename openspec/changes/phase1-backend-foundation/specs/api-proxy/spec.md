## ADDED Requirements

### Requirement: Vite development proxy
The frontend Vite dev server SHALL be configured to proxy all requests starting with `/api` and `/ws` to the Spring Boot backend at `http://localhost:8080`.

#### Scenario: API request proxying
- **WHEN** the frontend makes a request to `/api/v1/files` during development
- **THEN** the request is forwarded to `http://localhost:8080/api/v1/files` and the response is returned to the frontend

#### Scenario: WebSocket proxying
- **WHEN** the frontend establishes a WebSocket connection to `/ws/logs`
- **THEN** the connection is proxied to `ws://localhost:8080/ws/logs` with WebSocket upgrade support

### Requirement: Frontend API client module
The frontend SHALL have a centralized API client module that handles all HTTP requests to the backend, including base URL configuration and error handling.

#### Scenario: Successful API call
- **WHEN** a component calls an API method (e.g., fetchFiles())
- **THEN** the request is sent to the backend and the parsed response data is returned

#### Scenario: API error handling
- **WHEN** the backend returns a non-200 response
- **THEN** the API client throws a structured error with code and message

### Requirement: Replace mock data with API calls
The frontend components SHALL use real API calls instead of hardcoded mock data. This includes FileSidebar (file tree), EditorPanel (file content), and TopBar (model selection).

#### Scenario: File list loads from backend
- **WHEN** the application starts
- **THEN** the FileSidebar displays files fetched from `GET /api/v1/files`

#### Scenario: File content loads from backend
- **WHEN** a file is selected in the sidebar
- **THEN** the EditorPanel loads content from `GET /api/v1/files/content?path=...`

#### Scenario: File content saves to backend
- **WHEN** the user edits content (with debounce)
- **THEN** the content is saved via `POST /api/v1/files/save`

#### Scenario: Model selection persists to backend
- **WHEN** the user selects a different AI model
- **THEN** the selection is saved via `POST /api/v1/settings`
