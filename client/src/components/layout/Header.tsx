import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import { Button } from '@mui/material';
import AppNavigation from 'hooks/AppNavigation';
import { useAuth } from 'components/auth/AuthContext';

function Header() {

    const { handleNavigateHome, handleNavigateSignIn, handleNavigateSignUp } = AppNavigation();

    const authContext = useAuth();

    const handleLogOutClick = () => {
        authContext.logout();
    }

    return (

        <AppBar position="static">
            <Toolbar>
                
                <Typography 
                    variant="h6" 
                    component="div" 
                    onClick={handleNavigateHome} 
                    sx={{ flexGrow: 1, cursor: 'pointer' }}>
                    Home
                </Typography>

                {
                    authContext.authState.isAuthenticated 
                        ? (
                            <>
                                <Button 
                                    color="inherit" 
                                    onClick={handleLogOutClick} 
                                    sx={{ '&:focus': {outline: 'none'} }}>로그아웃</Button>
                            </>
                        ) : (
                            <>
                                <Button 
                                    color="inherit" 
                                    onClick={handleNavigateSignIn} 
                                    sx={{ '&:focus': {outline: 'none'} }}>로그인</Button>
                                <Button 
                                    color="inherit" 
                                    onClick={handleNavigateSignUp} 
                                    sx={{ '&:focus': {outline: 'none'} }}>회원가입</Button>
                            </>
                        )
                }
            </Toolbar>
        </AppBar>
    );
}

export default Header;