import React, { createContext, useContext, useState, ReactNode, useEffect } from 'react';
import { jwtDecode, JwtPayload } from 'jwt-decode';
import { signIn, signInWithGoogle, signInWithNaver, signOut, refreshAccessToken } from 'apis/authApi';
import { setupInterceptors } from 'apis/setupInterceptors';

interface AuthSettings {
    language: string | null;
    country: string | null;
    timezone: string | null;
}

interface AuthState {
    isAuthenticated: boolean;
    name: string | null;
    accessToken: string | null;
    refreshToken: string | null;
    settings: AuthSettings;
    isLoading: boolean; // isLoading 상태 추가
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
    accessToken: string;
    refreshToken: string;
    name: string;
    image?: string | null;
    settings?: {
        language: string | null;
        country: string | null;
        timezone: string | null;
    };
}

const AuthContext = createContext<AuthContextType | null>(null);

export const useAuth = (): AuthContextType => {
    const context = useContext(AuthContext);
    if (context === null) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

function isAccessTokenExpired(token: string): boolean {
    if (!token) return true;
    try {
        const decoded = jwtDecode<JwtPayload>(token.replace("Bearer ", ""));
        const now = Date.now() / 1000;
        return typeof decoded.exp === 'number' && decoded.exp < (now + 60);
    } catch (e) {
        console.error("Error decoding access token:", e);
        return true;
    }
}

const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {

    const [authState, setAuthState] = useState<AuthState>({
        isAuthenticated: false,
        name: null,
        accessToken: null,
        refreshToken: null,
        settings: {
            language: null,
            country: null,
            timezone: null
        },
        isLoading: true // 초기 로딩 상태를 true로 설정
    });

    useEffect(() => {
        setupInterceptors(); // 앱 시작 시 인터셉터 설정

        const initializeAuthState = async () => {
            const storedAccessToken = localStorage.getItem("accessToken");
            const storedRefreshToken = localStorage.getItem("refreshToken");
            const storedUser = localStorage.getItem("user");

            let initialName: string | null = null;
            let initialSettings: AuthSettings = { language: null, country: null, timezone: null };

            if (storedUser) {
                try {
                    const user = JSON.parse(storedUser);
                    initialName = user.name || null;
                    initialSettings = user.settings || initialSettings;
                } catch (e) {
                    console.error("Failed to parse stored user data:", e);
                    localStorage.removeItem("user");
                }
            }

            if (storedAccessToken && storedRefreshToken) {
                if (isAccessTokenExpired(storedAccessToken)) {
                    // Access Token이 만료되었다면 Refresh Token으로 갱신 시도
                    await refreshAuthTokens(storedRefreshToken); // await 추가
                } else {
                    setAuthState({
                        isAuthenticated: true,
                        name: initialName,
                        accessToken: storedAccessToken,
                        refreshToken: storedRefreshToken,
                        settings: initialSettings,
                        isLoading: false // 로딩 완료
                    });
                }
            } else {
                // 토큰이 없으면 초기화
                setAuthState({
                    isAuthenticated: false,
                    name: null,
                    accessToken: null,
                    refreshToken: null,
                    settings: { language: null, country: null, timezone: null },
                    isLoading: false // 로딩 완료
                });
            }
        };

        initializeAuthState();
    }, []); // 첫 렌더링 시에만 실행

    const refreshAuthTokens = async (refreshToken: string) => {
        try {
            const response = await refreshAccessToken(refreshToken);
            const newAccessToken = "Bearer " + response.accessToken;
            const newRefreshToken = response.refreshToken;

            localStorage.setItem('accessToken', newAccessToken);
            localStorage.setItem('refreshToken', newRefreshToken);

            setAuthState(prevState => ({
                ...prevState,
                isAuthenticated: true,
                accessToken: newAccessToken,
                refreshToken: newRefreshToken,
                isLoading: false, // 로딩 완료
            }));
            console.log("Access token refreshed successfully.");
        } catch (error) {
            console.error("Failed to refresh access token in AuthContext:", error);
            // 갱신 실패 시 로그아웃 처리
            // 이 경우 AuthState가 isAuthenticated: false로 즉시 업데이트되므로,
            // 별도로 isLoading: false를 설정할 필요는 없지만, 명시적으로 설정해도 무방.
            // logout() 함수가 호출되면서 최종적으로 isLoading: false가 될 것임.
            logout();
        }
    };


    async function login(email: string, password: string): Promise<boolean> {
        setAuthState(prevState => ({ ...prevState, isLoading: true })); // 로그인 시작 시 로딩
        try {
            const response = await signIn(email, password);

            if (response.status === 200) {
                const accessToken = "Bearer " + response.data.accessToken;
                const refreshToken = response.data.refreshToken;

                localStorage.setItem("accessToken", accessToken);
                localStorage.setItem("refreshToken", refreshToken);

                const userData = {
                    name: response.data.name,
                    image: response.data.image,
                    settings: response.data.settings
                };
                localStorage.setItem("user", JSON.stringify(userData));

                setAuthState({
                    isAuthenticated: true,
                    name: response.data.name,
                    accessToken: accessToken,
                    refreshToken: refreshToken,
                    settings: {
                        language: response.data.settings?.language || null,
                        country: response.data.settings?.country || null,
                        timezone: response.data.settings?.timezone || null
                    },
                    isLoading: false // 로그인 성공 시 로딩 완료
                });
                return true;
            } else {
                console.log('Login failed. Please check credentials.');
                setAuthState(prevState => ({ ...prevState, isLoading: false })); // 로그인 실패 시 로딩 완료
                return false;
            }

        } catch (e: any) {
            console.error("Login API error:", e);
            setAuthState(prevState => ({ ...prevState, isLoading: false })); // 로그인 에러 시 로딩 완료
            return false;
        }
    };

    async function loginWithGoogle(authCode: string) {
        setAuthState(prevState => ({ ...prevState, isLoading: true })); // 로그인 시작 시 로딩
        try {
            console.log('🔑 Google Auth Code to send to backend:', authCode);
            const response = await signInWithGoogle(authCode);

            if (response?.status === 200) {
                console.log('Backend Google login successful:', response.data);

                const accessToken = "Bearer " + response.data.accessToken;
                const refreshToken = response.data.refreshToken;

                localStorage.setItem("accessToken", accessToken);
                localStorage.setItem("refreshToken", refreshToken);

                const userData = {
                    name: response.data.name,
                    image: response.data.image,
                    settings: response.data.settings
                };
                localStorage.setItem("user", JSON.stringify(userData));

                setAuthState({
                    isAuthenticated: true,
                    name: userData.name,
                    accessToken: accessToken,
                    refreshToken: refreshToken,
                    settings: {
                        language: userData.settings?.language || null,
                        country: userData.settings?.country || null,
                        timezone: userData.settings?.timezone || null
                    },
                    isLoading: false // 로그인 성공 시 로딩 완료
                });
                return true;
            } else {
                console.error('Google 로그인 실패 (백엔드 응답 오류):', response?.data?.message || '알 수 없는 오류');
                setAuthState(prevState => ({ ...prevState, isLoading: false })); // 로그인 실패 시 로딩 완료
                return false;
            }
        } catch (error: any) {
            console.error('Google 로그인 요청 에러 (백엔드 통신 중 오류):', error);
            if (error.response) {
                console.error('Backend response data:', error.response.data);
                console.error('Backend response status:', error.response.status);
            }
            setAuthState(prevState => ({ ...prevState, isLoading: false })); // 로그인 에러 시 로딩 완료
            return false;
        }
    };

    async function loginWithNaver(authCode: string): Promise<boolean> {
        setAuthState(prevState => ({ ...prevState, isLoading: true })); // 로그인 시작 시 로딩
        try {
            console.log('🔑 Naver Auth Code to send to backend:', authCode);
            const response = await signInWithNaver(authCode);

            if (response?.status === 200 && response.data) {
                console.log('Backend Naver login successful:', response.data);

                const accessToken = "Bearer " + response.data.accessToken;
                const refreshToken = response.data.refreshToken;

                localStorage.setItem("accessToken", accessToken);
                localStorage.setItem("refreshToken", refreshToken);

                const userData = {
                    name: response.data.name,
                    image: response.data.image,
                    settings: response.data.settings
                };
                localStorage.setItem("user", JSON.stringify(userData));

                setAuthState({
                    isAuthenticated: true,
                    name: userData.name,
                    accessToken: accessToken,
                    refreshToken: refreshToken,
                    settings: {
                        language: userData.settings?.language || null,
                        country: userData.settings?.country || null,
                        timezone: userData.settings?.timezone || null
                    },
                    isLoading: false // 로그인 성공 시 로딩 완료
                });
                return true;
            } else {
                console.error('Naver 로그인 실패 (백엔드 응답 오류):', response?.message || '알 수 없는 오류');
                setAuthState(prevState => ({ ...prevState, isLoading: false })); // 로그인 실패 시 로딩 완료
                return false;
            }
        } catch (error: any) {
            console.error('Naver 로그인 요청 에러 (백엔드 통신 중 오류):', error);
            if (error.response) {
                console.error('Backend response data:', error.response.data);
                console.error('Backend response status:', error.response.status);
            }
            setAuthState(prevState => ({ ...prevState, isLoading: false })); // 로그인 에러 시 로딩 완료
            return false;
        }
    }

    const logout = async (): Promise<void> => {
        setAuthState(prevState => ({ ...prevState, isLoading: true })); // 로그아웃 시작 시 로딩
        const currentRefreshToken = localStorage.getItem("refreshToken");

        if (currentRefreshToken) {
            try {
                await signOut(currentRefreshToken);
                console.log("Refresh token successfully invalidated on server.");
            } catch (error) {
                console.error("Failed to invalidate refresh token on server:", error);
            }
        }

        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("user");

        setAuthState({
            isAuthenticated: false,
            name: null,
            accessToken: null,
            refreshToken: null,
            settings: {
                language: null,
                country: null,
                timezone: null
            },
            isLoading: false // 로그아웃 완료 시 로딩 완료
        });
        window.location.href = '/signin';
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