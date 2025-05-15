# JWT Auth Template

A basic JWT authentication and Google/Naver OAuth integration template built with a Spring (Java) backend and React (TypeScript) frontend. Use this to quickly implement secure and versatile authentication methods.

## Features
  
- [x] **JWT Authentication**: Secure user authentication using JSON Web Tokens.
- [x] **Google OAuth Integration**: Seamless login via Google accounts.
- [x] **Naver OAuth Integration**: Seamless login via Naver accounts.
- [x] **Responsive UI**: User interface designed to work well on various devices and screen sizes.
- [ ] **Refresh Token Support**: Enables silent token renewal without re-login. *(In development; currently only Access Token is issued.)*
- [ ] **Redis integration**: Manage refresh tokens using Redis. *(Planned)*

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
        }
        ```
    * **Response**: On successful account creation, returns an HTTP status code `201 Created`.

    * **Error Responses**:
        * `400 Bad Request`: If the request body is invalid or required fields are missing.
        * `409 Conflict`: If attempting to create an account with an email that is already in use.

* **Login**: `POST /v1/auth/login`
    * **Description**: Authenticates a registered user using email and password to obtain an access token and user's information.
    * **Request**: Send a JSON object containing the user's email and password in the request body.
        ```json
        {
          "email": "user@example.com",
          "password": "your_password"
        }
        ```
    * **Response**: On successful authentication, returns an HTTP status code `200 OK` along with a JSON object containing the access token and user's information.
        ```json
        {
          "name": "leun",
          "image": "/image.jpg",
          "token": "eyJhbGciOiJIUzI1Ni...",
        }
        ```
    * **Error Responses**:
        * `400 Bad Request`: If the request body is invalid or required fields are missing.
        * `401 Unauthorized`: If the email or password does not match.
          
* **OAuth Login Flow**:
    * **Description**: This section describes the common Server-Side Authorization Code Flow used for both Google and Naver social logins in this application. The process involves the frontend obtaining an authorization code from the OAuth provider and sending it to the backend for final authentication and token issuance.
    * **Process**:
        1.  **Initiate Login**: The user clicks the Google or Naver login button on the frontend, initiating the redirect to the respective OAuth provider's (Google or Naver) authentication and consent page.
        2.  **Authentication & Redirect**: The user authenticates with the OAuth provider and grants the application permission to access their information. The provider then redirects the user's browser back to the configured frontend callback URL, including an authorization `code` (and `state` for Naver) in the URL parameters.
        3.  **Frontend Sends Code to Backend**: The frontend (running at the callback URL) extracts the `code` (and verifies the `state` for Naver) from the URL parameters. It then sends this `code` via a `POST` request to the backend's specific OAuth login endpoint.
            * For **Google**: `POST /v1/auth/google/login`
            * For **Naver**: `POST /v1/auth/naver/login`
            The `code` (and potentially `state`) are expected in the request body.
        4.  **Backend Processing**: The backend receives the `code` (and verifies `state` for Naver). It then uses the code to securely interact with the respective OAuth provider's API to exchange the `code` for tokens and fetch user information. Based on this information, it handles user registration (if new) or login (if existing), linking the account to the provider. Finally, it generates your application's own Access Token and Refresh Token (JWTs) for the authenticated user.
        5.  **Complete Login**: The backend returns the generated application JWTs to the frontend. The frontend receives these tokens, stores them (e.g., in local storage or `AuthContext`), and updates the user's authentication state, completing the login process and typically navigating the user to a logged-in area.

* **Accessing Protected Routes**:
    * **Description**: Explains how to access API endpoints that require authentication using the Access Token obtained from login.
    * **Method**: Include an `Authorization` HTTP header in your API requests with the value formatted as `Bearer [YOUR_ACCESS_TOKEN]`. Replace `[YOUR_ACCESS_TOKEN]` with the actual Access Token string received from the login response.
        ```
        Authorization: Bearer eyJhbGciOiJIUzI1Ni...
        ```
    * **Error Responses**:
        * `401 Unauthorized`: If the `Authorization` header is missing, or the included token is invalid or expired.
        * `403 Forbidden`: If the user is authenticated but lacks the necessary permissions to access the resource or function.

* **Get Current User Information**: `GET /v1/user/profile`
    * **Description**: Retrieves the basic information of the currently authenticated user.
    * **Request**: Send the request with a valid Access Token included in the `Authorization: Bearer [YOUR_ACCESS_TOKEN]` header. The request body is typically empty.
    * **Response**: If the Access Token is valid, returns an HTTP status code `200 OK` along with a JSON object containing the current authenticated user's information.
        ```json
        {
          "email": "user@example.com",
          "name": "User Name",
          "image": "/image.jpg"
        }
        ```
    * **Error Responses**:
        * `401 Unauthorized`: If the request is made without a valid Access Token.

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
    ```bash
    cd server
    ```
2.  Create a `.env` file in this directory (`src/main/resources/`) and add your environment variables:
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
    ```bash
    cd client
    ```
2.  Create a `.env` file in this directory (`client/`) and add your environment variables:
    ```
    # Google OAuth Credentials
    VITE_GOOGLE_CLIENT_ID=[YOUR_GOOGLE_CLIENT_ID]
    VITE_GOOGLE_REDIRECT_URI=[YOUR_GOOGLE_REDIRECT_URI]
    
    # Naver OAuth Credentials
    VITE_NAVER_CLIENT_ID=[YOUR_NAVER_CLIENT_ID]
    VITE_NAVER_REDIRECT_URI=[YOUR_NAVER_REDIRECT_URI]
    
    ```

### Important

**⚠️ The `.env` file contains sensitive information like credentials and secrets. **Make sure to add `.env` to your `.gitignore` file** located in the `server` and `client` directory to prevent it from being committed to your Git repository.**

### Running the Application

1.  Start the Backend Server:
    ```bash
    cd server
    ./gradlew bootRun
    ```
2.  Start the Frontend Development Server:
    ```bash
    cd client
    npm run dev
    ```
3.  Access the application in your browser at `http://localhost:3000`

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.
