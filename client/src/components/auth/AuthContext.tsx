import React, { createContext, useContext, useState, ReactNode } from 'react';
import { jwtDecode, JwtPayload } from 'jwt-decode';
import { signIn, signInWithGoogle, signInWithNaver } from 'apis/authApi';

interface AuthSettings {
    language: string | null;
    country: string | null;
    timezone: string | null;
}

interface AuthState {
    isAuthenticated: boolean;
    name: string | null;
    token: string | null;
    settings: AuthSettings;
}

interface AuthContextType {
    authState: AuthState;
    login: (email: string, password: string) => Promise<boolean>;
    loginWithGoogle: (authCode: string) => Promise<boolean>;
    loginWithNaver: (authCode: string) => Promise<boolean>;
    logout: () => void;
}

interface AuthProviderProps {
    children: ReactNode;
}

interface SignInResponseData {
    token: string;
    name: string;
    language: string | null;
    country: string | null;
    timezone: string | null;
}

interface SignInResponse {
    status: number;
    data: SignInResponseData;
}

const AuthContext = createContext<AuthContextType | null>(null);

export const useAuth = (): AuthContextType => {
    const context = useContext(AuthContext);
    if (context === null) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

function isTokenExpired(token: string): boolean {
    try {
        const decoded = jwtDecode<JwtPayload>(token.replace("Bearer ", ""));
        const now = Date.now() / 1000;
        return typeof decoded.exp === 'number' && decoded.exp < now;
    } catch (e) {
        console.error("Error decoding token:", e);
        return true;
    }
}

const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {

    const token = localStorage.getItem("jwt");
    const valid = token ? !isTokenExpired(token) : false;
    const storedUser = localStorage.getItem("user");
    const parsedUser: Partial<SignInResponseData> = storedUser ? JSON.parse(storedUser) : {};

    const [authState, setAuthState] = useState<AuthState>({
        isAuthenticated: valid,
        name: parsedUser.name || null,
        token: token,
        settings: {
            language: parsedUser.language || null,
            country: parsedUser.country || null,
            timezone: parsedUser.timezone || null
        }
    });

    async function login(email: string, password: string): Promise<boolean> {
        try {
            const response: SignInResponse = await signIn(email, password);

            if (response.status === 200) {
                const rawToken = response.data.token;
                const jwt = "Bearer " + rawToken;

                localStorage.setItem("jwt", jwt);
                localStorage.setItem("user", JSON.stringify({
                    name: response.data.name,
                    language: response.data.language,
                    country: response.data.country,
                    timezone: response.data.timezone
                }));

                setAuthState({
                    isAuthenticated: true,
                    name: response.data.name,
                    settings: {
                        language: response.data.language,
                        country: response.data.country,
                        timezone: response.data.timezone
                    },
                    token: jwt,
                });

                return true;
            } else {
                console.log('Login failed. Please check credentials.');
                return false;
            }

        } catch (e: any) {
            console.error("Login API error:", e);
            return false;
        }
    };

    async function loginWithGoogle(authCode: string) {
        try {
            console.log('🔑 Google Auth Code to send to backend:', authCode);
            console.log('🔍 Type of code:', typeof authCode);

            const response = await signInWithGoogle(authCode);

            if (response?.status === 200) {
                 console.log('Backend Google login successful:', response.data);

                 const jwt = "Bearer " + response.data.token;
                 localStorage.setItem("jwt", jwt);

                 const userData = {
                     name: response.data.name,
                     image: response.data.image,
                     language: response.data.settings?.language, 
                     country: response.data.settings?.country,
                     timezone: response.data.settings?.timezone
                 };
                 localStorage.setItem("user", JSON.stringify(userData));


                 setAuthState({
                     isAuthenticated: true,
                     name: userData.name,
                     token: jwt,
                     settings: {
                         language: userData.language,
                         country: userData.country,
                         timezone: userData.timezone
                     }
                 });
                 return true; // 로그인 성공
            } else {
                console.error('Google 로그인 실패 (백엔드 응답 오류):', response?.data?.message || '알 수 없는 오류');
                // TODO: 사용자에게 에러 알림
                return false; // 로그인 실패
            }
        } catch (error: any) {
            console.error('Google 로그인 요청 에러 (백엔드 통신 중 오류):', error);
             if (error.response) {
                 console.error('Backend response data:', error.response.data);
                 console.error('Backend response status:', error.response.status);
            }
            // TODO: 사용자에게 에러 알림
            return false; // 로그인 실패
        }
    };

    async function loginWithNaver(authCode: string): Promise<boolean> {
        try {
            console.log('🔑 Naver Auth Code to send to backend:', authCode);

            const response = await signInWithNaver(authCode); // signInWithNaver 함수 호출

            if (response?.status === 200) { // response가 undefined가 아니고 status가 200인지 확인
                 console.log('Backend Naver login successful:', response.data);

                 const jwt = "Bearer " + response.data.token;
                 localStorage.setItem("jwt", jwt);

                  // localStorage user 정보 저장 시 image 필드 추가
                 const userData = {
                      name: response.data.name,
                      image: response.data.image, // image 필드 추가
                      language: response.data.settings?.language, // settings 구조 변경에 따라 수정
                      country: response.data.settings?.country, // settings 구조 변경에 따라 수정
                      timezone: response.data.settings?.timezone // settings 구조 변경에 따라 수정
                 };
                 localStorage.setItem("user", JSON.stringify(userData));

                 setAuthState({
                    isAuthenticated: true,
                    name: userData.name,
                    token: jwt,
                    settings: {
                        language: userData.language || null,
                        country: userData.country || null,
                        timezone: userData.timezone || null
                    }
                });
                 return true; // 로그인 성공
            } else {
                 console.error('Naver 로그인 실패 (백엔드 응답 오류):', response?.message || '알 수 없는 오류');
                 // TODO: 사용자에게 에러 알림
                 return false; // 로그인 실패
            }
        } catch (error: any) {
            console.error('Naver 로그인 요청 에러 (백엔드 통신 중 오류):', error);
            if (error.response) {
                console.error('Backend response data:', error.response.data);
                console.error('Backend response status:', error.response.status);
            }
            // TODO: 사용자에게 에러 알림
            return false; // 로그인 실패
        }
    }

    const logout = (): void => {
        localStorage.removeItem("jwt");
        localStorage.removeItem("user");
        setAuthState({
            isAuthenticated: false,
            name: null,
            token: null,
            settings: {
                language: null,
                country: null,
                timezone: null
            }
        });
    };

    const contextValue: AuthContextType = {
        authState,
        login,
        loginWithGoogle,
        loginWithNaver,
        logout
    };

    return (
        <AuthContext.Provider value={contextValue}>
            {children}
        </AuthContext.Provider>
    );
}

export { AuthContext, AuthProvider };