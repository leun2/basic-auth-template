import { apiClient } from "apis/apiClient";

interface SignInResponseData {
    token: string;
    name: string;
    image?: string | null; // 프로필 이미지 URL 필드 추가 (서버 응답에 따라 조정)
    settings?: { // 서버 응답에 settings가 포함된다면 추가
        language: string | null;
        country: string | null;
        timezone: string | null;
    };
}

interface SignInResponse {
    status: number;
    data: SignInResponseData;
    message?: string; // 서버 에러 메시지를 위한 필드 추가
}


export const signUp = (email: string, password: string, name: String) =>
    apiClient.post('/v1/user', {
        email,
        password,
        name
    });

export const signIn = (email: string, password: string) => 
    apiClient.post('/v1/auth/login' , {
        email,
        password
    })

export const signInWithGoogle = (authCode: string) => 
    apiClient.post('/v1/auth/google/login', { 
        code: authCode });

export const signInWithNaver = async (authCode: string): Promise<SignInResponse | undefined> => {
    console.log('Calling server Naver login with code:', authCode);
    try {
        const response = await apiClient.post<SignInResponseData>('/v1/auth/naver/login', { code: authCode });
        // status 필드를 직접 포함하지 않는 경우 axios 응답 객체 사용
        return { status: response.status, data: response.data };
    } catch (error: any) {
        console.error('Server Naver login error:', error);
            // 에러 응답 처리
        if (error.response) {
                return { status: error.response.status, data: error.response.data, message: error.response.data.message };
        }
        throw error; // 네트워크 오류 등 다른 에러는 다시 throw
    }
};

