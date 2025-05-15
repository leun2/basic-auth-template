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
**⚠️ Important:** The `.env` file contains sensitive information like credentials and secrets. **Make sure to add `src/main/resources/.env` to your `.gitignore` file** located in the `server` directory to prevent it from being committed to your Git repository.

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

**⚠️  The `.env` file contains sensitive information like credentials and secrets. **Make sure to add `src/main/resources/.env` to your `.gitignore` file** located in the `server` directory to prevent it from being committed to your Git repository. **

### Running the Application

1.  Start the Backend Server:
    ```bash
    cd [FILL_IN_BACKEND_DIRECTORY_NAME]
    [FILL_IN_BACKEND_RUN_COMMAND] # 예: ./mvnw spring-boot:run 또는 java -jar target/[your-jar-file].jar
    ```
2.  Start the Frontend Development Server:
    ```bash
    cd [FILL_IN_FRONTEND_DIRECTORY_NAME]
    npm start # 또는 yarn start
    ```
3.  Access the application in your browser at `[FILL_IN_FRONTEND_APP_URL]` (usually `http://localhost:3000`).

## Usage

Describe how to use your application's authentication features.

* **User Registration**: `[FILL_IN_REGISTRATION_ENDPOINT]` (e.g., `POST /api/auth/register`)
* **Login**: `[FILL_IN_LOGIN_ENDPOINT]` (e.g., `POST /api/auth/login`) - Explains expected request body (username/password) and response (JWT token).
* **Accessing Protected Routes**: How to include the JWT in the request header (e.g., `Authorization: Bearer [YOUR_JWT]`).
* **Google OAuth Login Flow**: Steps for initiating Google login and handling the redirect.
* **Naver OAuth Login Flow**: Steps for initiating Naver login and handling the redirect.
* **[FILL_IN_OTHER_USAGE_DETAILS]**: 토큰 갱신, 로그아웃 등의 사용법을 설명하세요.

## Contributing

Contributions are welcome! If you'd like to contribute, please follow these steps:

1.  Fork the repository.
2.  Create a new branch (`git checkout -b feature/YourFeature`).
3.  Make your changes and commit them (`git commit -m 'Add some feature'`).
4.  Push to the branch (`git push origin feature/YourFeature`).
5.  Open a Pull Request.

## License

This project is licensed under the [FILL_IN_LICENSE_NAME] License - see the [LICENSE.md](LICENSE.md) file for details.

## Contact

Your Name - [Your Email Address]
Project Link: [https://github.com/leun2/jwt-auth-template](https://github.com/leun2/jwt-auth-template)

---

이 초안은 일반적인 README의 구성을 따르며, 당신의 레포지토리에 특화된 정보(디렉토리 이름, 명령어, 설정 변수 등)를 채워 넣을 수 있도록 안내합니다. 이 틀에 맞춰 내용을 추가하면 방문자들이 당신의 프로젝트를 이해하고 사용하는 데 큰 도움이 될 것입니다.
