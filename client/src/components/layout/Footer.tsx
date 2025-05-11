import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import Link from '@mui/material/Link';
import Stack from '@mui/material/Stack';
import GitHubIcon from '@mui/icons-material/GitHub';

function Footer() {

    const mailtoLink = `mailto:leun12968@gmail.com`;

    const footerLinks = [
        { text: 'Contact', url: mailtoLink },
        { text: 'Repository', url: 'https://github.com/leun2/jwt-auth-template' },
        { text: 'Docs', url: 'http://localhost:8080/docs' },
    ];

    return (
        <Box
            sx={{
                bgcolor: 'primary.main',
                color: 'white',
                py: 3,
            }}
            component="footer"
        >
            <Container maxWidth="lg">
                <Stack
                    direction="row"
                    spacing={{ xs: 1.5, sm: 2, md: 3 }} // 요소들 간의 간격 (반응형)
                    justifyContent="center" // 가로 가운데 정렬
                    alignItems="center" // 세로 가운데 정렬
                    flexWrap="wrap" // 내용이 넘치면 줄바꿈 허용
                >

                    <Link
                        color="inherit" // 부모 요소의 색상 상속
                        href="https://github.com/leun2" // GitHub 메인 페이지 링크 예시
                        target="_blank" // 새 탭에서 열기
                        rel="noopener noreferrer" // 보안 설정
                        aria-label="GitHub homepage" // 스크린 리더를 위한 라벨
                        sx={{ display: 'flex', alignItems: 'center', color: 'inherit', '&:hover': { textDecoration: 'none', color: 'inherit' } }}
                    >
                        <GitHubIcon />
                    </Link>

                    <Typography variant="body2" color="inherit" sx={{ whiteSpace: 'nowrap', flexShrink: 0 }}> {/* 줄바꿈 방지, 축소 방지 */}
                        {'© '}
                        {new Date().getFullYear()}
                        {' leun.'}
                    </Typography>

                    {footerLinks.map((link) => (
                        <Link
                            key={link.text}
                            color="inherit" // 부모 요소 색상 상속
                            href={link.url} // 링크 URL
                            variant="body2" // Typography 스타일 적용
                            sx={{
                                textDecoration: 'none', // 기본 밑줄 제거
                                '&:hover': { textDecoration: 'none', color: 'inherit' }, // 호버 시 밑줄 표시
                                color: 'inherit', // 색상 상속
                                flexShrink: 0 // 링크 요소가 줄어들지 않도록 설정
                            }}
                        >
                            {link.text}
                        </Link>
                    ))}

                </Stack>
            </Container>
        </Box>
    );
}

export default Footer;