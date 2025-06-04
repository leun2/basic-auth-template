import React from "react";
import { useAuth } from "components/auth/AuthContext"
import { Navigate } from "react-router-dom";
import LoadingSpinner from "components/common/LoadingSpinner";

function AuthenticatedRoute({ children }: { children: React.ReactNode }) {
    const { authState } = useAuth();

    if (authState.isLoading) {
        return <div><LoadingSpinner /></div>; // 또는 <LoadingSpinner /> 컴포넌트
    }

    // 2. 로딩이 끝났고, 인증되었다면 children을 렌더링합니다.
    if (authState.isAuthenticated) {
        return <>{children}</>;
    }

    // 3. 로딩이 끝났고, 인증되지 않았다면 로그인 페이지로 리디렉션합니다.
    return <Navigate to="/signin" />;
}

export default AuthenticatedRoute;