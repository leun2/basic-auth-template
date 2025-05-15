# JWT Auth Template

A basic JWT authentication and Google/Naver OAuth integration template built with a Spring (Java) backend and React (TypeScript) frontend. Use this to quickly implement secure and versatile authentication methods.

## Features
  
- [x] **JWT Authentication**: Secure user authentication using JSON Web Tokens.
- [x] **Google OAuth Integration**: Seamless login via Google accounts.
- [x] **Naver OAuth Integration**: Seamless login via Naver accounts.
- [X] **Responsive UI**: User interface designed to work well on various devices and screen sizes.
- [ ] **Refresh Token**: Mechanism for issuing and using refresh tokens to obtain new access tokens without re-authentication. (In progress)
- [ ] **Redis Setup**: Set up Redis for refresh token management. (TBD)

## Tech Stack

This project is built with the following technologies.

### Frontend
- **React:** A core JavaScript library for building user interfaces.
- **TypeScript:** Used for enhanced code stability and developer productivity by adding static typing.
- **Vite:** A fast and modern frontend build tool and development server.
- **Material UI (MUI):** A component library based on Google Material Design for building consistent and stylish UIs.

### Backend
- **Spring Boot (Java):** The primary framework for Java-based backend application development.
- **Spring Security:** The core security framework handling user authentication and authorization.
- **Spring Data JPA:** Used to simplify database interactions using JPA.
- **MySQL:** The relational database management system (RDBMS) used for data storage and management.
- **JWT (JSON Web Token):** A token-based authentication method used for managing user sessions. (Implemented using Spring Security and JJWT).

## Usage

This section describes how to use the authentication and user-related features of the application. Please check the detailed explanations and required request/response formats for each endpoint.

* **User Registration**: `POST /v1/user`
    * **Description**: Creates a new user account. Register using email and password.
    * **Request**: Send a JSON object containing the user's email and password in the request body.
        ```json
        {
          "email": "user@example.com",
          "password": "your_strong_password"
          // Include additional information like username if required
        }
        ```
    * **Response**: On successful account creation, returns an HTTP status code `201 Created` along with the created user information or a success message.
        ```json
        // Example Response (returning created user info)
        // Status Code: 201 Created
        {
          "id": 123,
          "email": "user@example.com"
          // ...
        }
        // Example Response (returning success message)
        // Status Code: 201 Created
        // Empty body or {"message": "User registered successfully"}
        ```
    * **Error Responses**:
        * `400 Bad Request`: If the request body is invalid or required fields are missing.
        * `409 Conflict`: If attempting to create an account with an email that is already in use.

* **Login**: `POST /v1/auth/login`
    * **Description**: Authenticates a registered user using email and password to obtain an Access Token and Refresh Token.
    * **Request**: Send a JSON object containing the user's email and password in the request body.
        ```json
        {
          "email": "user@example.com",
          "password": "your_password"
        }
        ```
    * **Response**: On successful authentication, returns an HTTP status code `200 OK` along with a JSON object containing the Access Token and Refresh Token. These tokens are used for subsequent API calls that require authentication.
        ```json
        {
          "accessToken": "eyJhbGciOiJIUzI1Ni...",
          "refreshToken": "eyJhbGciOiJIUzI1Ni..."
          // Include additional token information like expiration time if needed
        }
        ```
    * **Error Responses**:
        * `400 Bad Request`: If the request body is invalid or required fields are missing.
        * `401 Unauthorized`: If the email or password does not match.

* **Accessing Protected Routes**:
    * **Description**: Explains how to access API endpoints that require authentication using the Access Token obtained from login.
    * **Method**: Include an `Authorization` HTTP header in your API requests with the value formatted as `Bearer [YOUR_ACCESS_TOKEN]`. Replace `[YOUR_ACCESS_TOKEN]` with the actual Access Token string received from the login response.
        ```
        Authorization: Bearer eyJhbGciOiJIUzI1Ni...
        ```
    * **Error Responses**:
        * `401 Unauthorized`: If the `Authorization` header is missing, or the included token is invalid or expired.
        * `403 Forbidden`: If the user is authenticated but lacks the necessary permissions to access the resource or function.

* **Google OAuth Login Flow**:
    * **Description**: Log in using a Google account. This process retrieves user information via Google authentication to link or create an account without the standard registration/login steps.
    * **Process**:
        1.  **Initiate Google Login from Frontend**: The user clicks the "Login with Google" button. The frontend redirects the user to the configured Google authentication URL. This URL includes `client_id`, `redirect_uri`, `scope`, etc.
        2.  **Google Authentication and Consent**: The user authenticates with their Google account on the Google login page and consents to the application accessing their information.
        3.  **Google Redirect**: After authentication and consent, Google redirects the user back to the configured `redirect_uri` (your frontend URL), including an authorization `code` in the URL parameters.
        4.  **Frontend -> Backend Code Transfer**: The frontend extracts the `code` value from the redirect URL and sends it to the backend's Google OAuth callback endpoint (e.g., `POST /v1/auth/google/callback`).
        5.  **Backend Processing**: The backend uses the received `code` to request user information (email, profile, etc.) from the Google API.
        6.  **Backend User Handling & JWT Issuance**: Based on the retrieved Google user information, the backend finds an existing user or creates/links a new one. Upon completion, it generates its own Access Token and Refresh Token for that user.
        7.  **Backend -> Frontend JWT Return**: The backend responds to the frontend with the generated Access Token and Refresh Token (similar response format to standard login).
        8.  **Frontend Processing**: The frontend stores the received tokens and handles the logged-in state.

* **Naver OAuth Login Flow**:
    * **Description**: Log in using a Naver account. The process is similar to the Google OAuth flow.
    * **Process**:
        1.  **Initiate Naver Login from Frontend**: The user clicks the "Naver Login" button. The frontend redirects the user to the configured Naver authentication URL (`client_id`, `redirect_uri`, `state`, etc. included).
        2.  **Naver Authentication and Consent**: The user authenticates with their Naver account and consents to the application accessing information.
        3.  **Naver Redirect**: After authentication, Naver redirects the user back to the configured `redirect_uri` (your frontend URL), including `code` and `state` values in the URL parameters.
        4.  **Frontend -> Backend Code Transfer**: The frontend extracts the `code` and `state` values and sends them to the backend's Naver OAuth callback endpoint (e.g., `POST /v1/auth/naver/callback`).
        5.  **Backend Processing**: The backend uses the received `code` to request user information from the Naver API.
        6.  **Backend User Handling & JWT Issuance**: Based on the retrieved Naver user information, the backend processes the user and generates Access Token and Refresh Token.
        7.  **Backend -> Frontend JWT Return**: The backend responds with the generated tokens to the frontend.
        8.  **Frontend Processing**: The frontend stores the received tokens and handles the logged-in state.

* **Token Refresh**: `POST /v1/auth/refresh`
    * **Description**: When the Access Token expires, use the Refresh Token to obtain a new pair of Access Token and Refresh Token. This allows maintaining the user's session without requiring re-login.
    * **Request**: Typically send the Refresh Token in the request body, or potentially in a specific header or cookie. The Access Token header might not be required for this specific request as the Access Token might be expired.
        ```json
        // Example Request Body (using Refresh Token)
        {
          "refreshToken": "[YOUR_REFRESH_TOKEN]"
        }
        ```
    * **Response**: If the Refresh Token is valid, returns an HTTP status code `200 OK` along with a JSON object containing the new Access Token and Refresh Token.
        ```json
        {
          "accessToken": "eyJhbGciOiJIUzI1Ni_new_access...",
          "refreshToken": "eyJhbGciOiJIUzI1Ni_new_refresh..."
        }
        ```
    * **Error Responses**:
        * `401 Unauthorized`: If the Refresh Token is invalid or expired.

* **Logout**: `POST /v1/auth/logout` (or `DELETE /v1/auth/logout`)
    * **Description**: Terminates the current user session and invalidates server-side tokens (Access Token, Refresh Token, etc.) associated with the user. Client-side stored tokens should also be removed.
    * **Request**: Send the request with a valid Access Token included in the `Authorization: Bearer [YOUR_ACCESS_TOKEN]` header.
    * **Response**: On successful logout, returns an HTTP status code `200 OK` along with a success message or status.
        ```json
        // Example Response
        // Status Code: 200 OK
        // Empty body or {"message": "Logged out successfully"}
        ```
    * **Error Responses**:
        * `401 Unauthorized`: If the request is made without a valid Access Token.

* **Get Current User Information**: `GET /v1/user/me`
    * **Description**: Retrieves the basic information of the currently authenticated user.
    * **Request**: Send the request with a valid Access Token included in the `Authorization: Bearer [YOUR_ACCESS_TOKEN]` header. The request body is typically empty.
    * **Response**: If the Access Token is valid, returns an HTTP status code `200 OK` along with a JSON object containing the current authenticated user's information.
        ```json
        {
          "id": 123,
          "email": "user@example.com",
          "name": "User Name" // May include linked social account info etc.
          // ...
        }
        ```
    * **Error Responses**:
        * `401 Unauthorized`: If the request is made without a valid Access Token.

---

**Note**: The descriptions above are based on common JWT and OAuth implementation patterns. The exact endpoint URLs, specific field names and formats in request/response bodies, and token management methods (e.g., whether Refresh Tokens are used, where they are stored, etc.) may vary depending on the actual backend code of your application. Please refer to the code for precise details.

## Getting Started

A guide for setting up and running the project in your local environment.

### Prerequisites

The following are required to build and run the project:

- Node.js (LTS version recommended)
- Java 17+
- Gradle

### Installation

Clone this repository:
    ```bash
    git clone https://github.com/leun2/jwt-auth-template.git
    cd jwt-auth-template
    ```

### Backend Configuration

1.  Navigate to the `server` directory.
2.  Create a `.env` file inside the `src/main/resources` directory.
3.  Set Up Environment Variables (.env)
    ```
    # JWT Secret Key
    JWT_SECRET=[YOUR_VERY_STRONG_SECRET_KEY]
    
    # JWT Expiration Time
    JWT_EXPIRATION=[FILL_IN_EXPIRATION]
    
    # Google OAuth Credentials
    GOOGLE_CLIENT_ID=[YOUR_GOOGLE_CLIENT_ID]
    GOOGLE_CLIENT_SECRET=[YOUR_GOOGLE_CLIENT_SECRET]
    GOOGLE_REDIRECT_URI=[YOUR_GOOGLE_REDIRECT_URI]
    
    # Naver OAuth Credentials
    NAVER_CLIENT_ID=[YOUR_NAVER_CLIENT_ID]
    NAVER_CLIENT_SECRET=[YOUR_NAVER_CLIENT_SECRET]
    NAVER_REDIRECT_URI=[YOUR_NAVER_REDIRECT_URI]
    
    # Database Configuration
    DATABASE_URL=[YOUR_DATABASE_URL]
    DATABASE_USERNAME=[YOUR_DATABASE_USERNAME]
    DATABASE_PASSWORD=[YOUR_DATABASE_PASSWORD]
    ```

### Frontend Configuration

1.  Navigate to the `client` directory.
2.  Create a `.env` file inside the `src` directory.
3.  Set Up Environment Variables (.env)
    ```
    # Client ID for OAuth
    VITE_GOOGLE_CLIENT_ID=[YOUR_FRONTEND_GOOGLE_CLIENT_ID]
    VITE_NAVER_CLIENT_ID=[YOUR_FRONTEND_NAVER_CLIENT_ID]
    
    # Redirect URI for NAVER
    VITE_NAVER_REDIRECT_URI=[YOUR_FRONTEND_NAVER_CLIENT_URI]
    ```

### Important

**⚠️ The `.env` file contains sensitive information like credentials and secrets. **Make sure to add `.env` to your `.gitignore` file** located in the `server` and `client` directory to prevent it from being committed to your Git repository.**

### Running the Application

1.  Start the Backend Server:
    ```bash
    cd server
    ./gradlew bootRun
    ```
    ```
2.  Start the Frontend Development Server:
    ```bash
    cd client
    npm run dev
    ```
3.  Access the application in your browser at `http://localhost:3000`

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.
