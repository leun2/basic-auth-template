import { apiClient } from "apis/apiClient";
import { refreshAccessToken, signOut } from "apis/authApi";

let isRefreshing = false;
let failedQueue: Array<{ resolve: (token: string) => void, reject: (error: any) => void }> = [];

const processQueue = (error: any, token: string | null = null) => {
    failedQueue.forEach(prom => {
        if (error) {
            prom.reject(error);
        } else {
            prom.resolve(token as string);
        }
    });
    failedQueue = [];
};

// 인터셉터가 이미 설정되었는지 확인하는 플래그
let interceptorsSetup = false;

export function setupInterceptors() {
    if (interceptorsSetup) {
        return;
    }

    apiClient.interceptors.request.use(
        (config) => {
            const accessToken = localStorage.getItem("accessToken");
            if (accessToken) {
                // 요청 시마다 최신 accessToken을 헤더에 설정
                config.headers.Authorization = accessToken;
            }
            return config;
        },
        (error) => Promise.reject(error)
    );

    apiClient.interceptors.response.use(
        response => response,
        async (error) => {
            const originalRequest = error.config;

            // 401 에러이고, 재시도 플래그가 없으며, 로그아웃 또는 토큰 갱신 요청이 아닌 경우
            if (error.response?.status === 401 && !originalRequest._retry &&
                originalRequest.url !== '/v1/auth/refresh-token' && originalRequest.url !== '/v1/auth/logout') {
                originalRequest._retry = true; // 재시도 플래그 설정

                const refreshToken = localStorage.getItem("refreshToken"); // refreshToken 가져오기

                if (!refreshToken) {
                    // Refresh Token이 없으면 모든 로컬 저장소 항목 삭제 후 로그인 페이지로 리다이렉트
                    localStorage.removeItem('accessToken');
                    localStorage.removeItem('refreshToken');
                    localStorage.removeItem('user');
                    window.location.href = '/signin'; // 로그인 페이지 경로
                    return Promise.reject(error);
                }

                if (isRefreshing) {
                    // 이미 토큰 갱신 중이면 큐에 추가하여 대기
                    return new Promise((resolve, reject) => {
                        failedQueue.push({ resolve, reject });
                    }).then(token => {
                        originalRequest.headers['Authorization'] = token;
                        return apiClient(originalRequest);
                    }).catch(err => {
                        return Promise.reject(err);
                    });
                }

                isRefreshing = true; // 토큰 갱신 시작 플래그 설정

                try {
                    // Refresh Token으로 새로운 Access Token 요청
                    const response = await refreshAccessToken(refreshToken);
                    const newAccessToken = "Bearer " + response.accessToken;
                    const newRefreshToken = response.refreshToken;

                    localStorage.setItem('accessToken', newAccessToken);
                    localStorage.setItem('refreshToken', newRefreshToken);

                    // 새로운 accessToken으로 apiClient의 기본 헤더도 업데이트
                    // 하지만 실제 요청 시에는 request 인터셉터에서 다시 설정되므로 여기는 선택 사항
                    // apiClient.defaults.headers.common['Authorization'] = newAccessToken;

                    processQueue(null, newAccessToken); // 대기 큐 처리

                    // 원본 요청의 헤더를 업데이트하고 재시도
                    originalRequest.headers['Authorization'] = newAccessToken;
                    return apiClient(originalRequest);

                } catch (refreshError: any) {
                    // Refresh Token 갱신 실패 (Refresh Token도 만료되었거나 유효하지 않음)
                    console.error("Failed to refresh token:", refreshError);

                    // 클라이언트에서 가지고 있던 Refresh Token을 서버에 삭제 요청 (선택 사항이지만 추천)
                    try {
                        await signOut(refreshToken);
                        console.log("Server side refresh token invalidated.");
                    } catch (logoutError) {
                        console.error("Failed to invalidate refresh token on server:", logoutError);
                    }

                    // 모든 토큰 삭제 후 로그인 페이지로 리다이렉트
                    localStorage.removeItem('accessToken');
                    localStorage.removeItem('refreshToken');
                    localStorage.removeItem('user');
                    processQueue(refreshError); // 대기 큐에 에러 전파
                    window.location.href = '/signin'; // 로그인 페이지 경로
                    return Promise.reject(refreshError);
                } finally {
                    isRefreshing = false; // 토큰 갱신 완료
                }
            }

            return Promise.reject(error);
        }
    );

    interceptorsSetup = true; // 인터셉터 설정 완료 플래그
}