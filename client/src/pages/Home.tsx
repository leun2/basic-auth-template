import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Header from 'components/layout/Header';
import Footer from 'components/layout/Footer';
import AppNavigation from 'hooks/AppNavigation';

function Home() {

    const { handleNavigateProfile } = AppNavigation();

    return (
        <Box
            sx={{
                display: 'flex',
                flexDirection: 'column',
                minHeight: '100vh',
                minWidth: '100vw',
                overflowX: 'hidden'
            }}
        >   
            <Header />
            <Box component="main" sx={{ flexGrow: 1, py: 8, px: 4 }}>
                <Box sx={{ textAlign: 'center', mb: 6 }}>
                    <Typography variant="h2" component="h1" gutterBottom>
                        여러분의 프로필을 편리하게 관리하세요!
                    </Typography>
                    <Typography variant="h5" color="text.secondary" paragraph>
                        몇 번의 클릭만으로 당신의 프로필을 체계적으로 관리하고, 필요한 사람들과 쉽게 공유하세요.
                    </Typography>
                    <Stack
                        direction="row"
                        spacing={2}
                        justifyContent="center"
                        sx={{ mt: 4 }}
                    >
                        <Button variant="contained" size="large" onClick={handleNavigateProfile} sx={{ '&:focus': {outline: 'none'} }}>
                            지금 시작하기
                        </Button>
                        <Button variant="outlined" size="large" sx={{ '&:focus': {outline: 'none'} }}>
                            자세히 알아보기
                        </Button>
                    </Stack>
                    </Box>
                    <Box sx={{ my: 6,  textAlign: 'center' }}>
                    <Typography variant="h4" component="h2" gutterBottom>
                        주요 기능
                    </Typography>
                    <Typography variant="body1" color="text.secondary">
                        개인 정보, 경력, 학력, 프로젝트 등 다양한 정보를 한 곳에 모아 관리하고,<br/> 필요에 따라 맞춤형 프로필을 생성하여 공유할 수 있습니다.
                    </Typography>
                </Box>
            </Box>
            <Footer />
        </Box>
    );
}

export default Home;
