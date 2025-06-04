import { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from 'components/auth/AuthContext';
import LoadingSpinner from 'components/common/LoadingSpinner';

function NaverCallback() {
    const location = useLocation();
    const navigate = useNavigate();
    const { loginWithNaver } = useAuth(); // useAuth 훅 사용

    useEffect(() => {
        const params = new URLSearchParams(location.search);
        const authCode = params.get('code');
        const receivedState = params.get('state');
        const error = params.get('error'); // 에러 발생 시 파라미터 확인

        // Naver 인증 실패 시 에러 처리
        if (error) {
            console.error('Naver OAuth Error:', error);
            // TODO: 사용자에게 에러 메시지 표시
            navigate('/signin', { replace: true }); // 로그인 페이지로 리다이렉트
            return;
        }

        // authCode와 state가 모두 있는지 확인
        if (!authCode || !receivedState) {
            console.error('Naver OAuth callback missing code or state.');
            // TODO: 사용자에게 에러 메시지 표시
            navigate('/signin', { replace: true }); // 로그인 페이지로 리다이렉트
            return;
        }

        // 저장된 state 가져와서 검증 (CSRF 방지)
        const storedState = sessionStorage.getItem('naver_auth_state');
        sessionStorage.removeItem('naver_auth_state'); // 사용 후 즉시 삭제

        if (!storedState || receivedState !== storedState) {
            console.error('Naver OAuth state mismatch.');
            // TODO: 보안 경고 및 사용자에게 에러 메시지 표시
            navigate('/signin', { replace: true }); // 로그인 페이지로 리다이렉트
            return;
        }

        // 유효성 검증 통과 시 서버에 인증 코드 전달
        const processLogin = async () => {
            const success = await loginWithNaver(authCode); // AuthContext 함수 호출

            if (success) {
                // 로그인 성공 처리 (예: 프로필 페이지로 이동)
                 navigate('/profile', { replace: true });
            } else {
                // 로그인 실패 처리 (예: 에러 메시지 표시 후 로그인 페이지로 이동)
                console.error('Failed to login with Naver code on backend.');
                // TODO: 사용자에게 에러 메시지 표시
                navigate('/signin', { replace: true });
            }
        };

        processLogin();

    }, [location, navigate, loginWithNaver]);

    return (
        <div>
            <LoadingSpinner />
        </div>
    );
}

export default NaverCallback;