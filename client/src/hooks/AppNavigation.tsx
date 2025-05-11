import { useNavigate, NavigateFunction } from 'react-router-dom';

interface UseNavigationHandlers {
    handleNavigateHome: () => void;
    handleNavigateSignUp: () => void;
    handleNavigateSignIn: () => void;
    handleNavigateProfile: () => void;
    handleNavigateProfileEdit: () => void;
    handleNavigateSettings: () => void;
}

const useNavigation = (): UseNavigationHandlers => {
    const navigate: NavigateFunction = useNavigate();

    const handleNavigateHome = (): void => {
        navigate('/');
    };

    const handleNavigateSignUp = (): void => {
        navigate('/signup');
    };

    const handleNavigateSignIn = (): void => {
        navigate('/signin');
    };

    const handleNavigateProfile = (): void => {
        navigate(`/profile`);
    };

    // const handleNavigateProfileById = (id: string): void => {
    //     navigate(`/profile/${id}`);
    // };

    const handleNavigateProfileEdit = (): void => {
        navigate('/profile/edit');
    };

    const handleNavigateSettings = (): void => {
        navigate('/setting');
    };

    return {
        handleNavigateHome,
        handleNavigateSignUp,
        handleNavigateSignIn,
        handleNavigateProfile,
        handleNavigateProfileEdit,
        handleNavigateSettings,
    };
};

export default useNavigation;


