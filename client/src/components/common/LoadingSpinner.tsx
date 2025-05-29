import { Box, CircularProgress, Typography } from '@mui/material';

/**
 * 전역 로딩 스피너 컴포넌트
 * 페이지 로딩 중이거나 데이터를 불러오는 동안 표시됩니다.
 */
function LoadingSpinner() {
    return (
        <Box
            sx={{
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'center',
                alignItems: 'center',
                height: '100vh', // 전체 뷰포트 높이를 차지하도록 설정
                width: '100vw',  // 전체 뷰포트 너비를 차지하도록 설정
                position: 'fixed', // 스크롤과 상관없이 화면 중앙에 고정
                top: 0,
                left: 0,
                backgroundColor: 'rgba(255, 255, 255, 0.8)', // 반투명 배경
                zIndex: 9999, // 다른 요소들 위에 표시되도록 높은 z-index 설정
            }}
        >
            <CircularProgress size={50} />
            <Typography variant="h6" sx={{ mt: 2 }}>
                
            </Typography>
        </Box>
    );
}

export default LoadingSpinner;