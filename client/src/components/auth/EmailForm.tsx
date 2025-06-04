import {
  Box,
  Button,
  Stack,
  TextField,
  Typography
} from '@mui/material';
import { useFormik } from 'formik';
import * as Yup from 'yup';
import AppNavigation from 'hooks/AppNavigation';
import OAuthButtons from 'components/auth/OAuthButtons';

interface EmailFormProps {
    mode: 'signin' | 'signup';
    onNext: (email: string) => void;
}

function EmailForm({ mode, onNext }: EmailFormProps) {

    const formik = useFormik({
            initialValues: {
                email: ''
            },
            validationSchema: Yup.object({
              email: Yup.string()
                .email('유효한 이메일 주소를 입력해주세요')
                .required('이메일은 필수입니다')
            }),
            onSubmit: async (values) => {
                // const success = await checkEmailAvailability(email);
                // if(success)
                onNext(values.email);
            }
            // onSubmit: ({ email }) => {
            //     onNext(email);
            // }
    });

    const { handleNavigateHome, handleNavigateSignIn, handleNavigateSignUp } = AppNavigation();

    return (
        <Box
            sx={{
                minHeight: '100vh',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                backgroundColor: '#fff',
                width: { xs: '100vw', md: '100vw' }
            }}
        >   
            <Box component="form" onSubmit={formik.handleSubmit} sx={{ width: '100%', maxWidth: 300 }}>
                
                <Stack spacing={2}>
                    {
                        mode === "signin" ?
                        (
                            <Stack spacing={1}>
                                <Typography variant="h5" fontWeight="bold">
                                    로그인
                                </Typography>

                                <Typography sx={{color:'#666666', fontSize:'16px'}}>
                                    profile로 계속
                                </Typography>
                            </Stack>
                        ) : (
                            <Stack spacing={1}>
                                <Typography variant="h5" fontWeight="bold">
                                    계정 생성
                                </Typography>

                                <Typography sx={{color:'#666666', fontSize:'12px'}}>
                                    이를 클릭함으로써 귀하는 {}
                                    <Typography
                                        component="span" 
                                        sx={{ textDecoration: 'none', fontSize:'12px', color: '#1976d2', cursor: 'pointer' }}
                                        onClick={handleNavigateHome} 
                                    >
                                        서비스 약관
                                    </Typography>과 {}
                                    <Typography
                                        component="span" 
                                        sx={{ textDecoration: 'none', fontSize:'12px', color: '#1976d2', cursor: 'pointer' }}
                                        onClick={handleNavigateHome} 
                                    >
                                        개인정보 처리방침
                                    </Typography>
                                    을 읽고 이에 동의한 것으로 간주됩니다.
                                </Typography>
                            </Stack>
                        )
                    }
                    <Stack spacing={2}>
                        <TextField 
                            fullWidth 
                            label="이메일"
                            name="email"
                            value={formik.values.email}
                            onChange={formik.handleChange}
                            onBlur={formik.handleBlur}
                            error={formik.touched.email && Boolean(formik.errors.email)}
                            helperText={formik.touched.email && formik.errors.email} />

                        <Button 
                            type="submit" 
                            variant="contained" 
                            fullWidth
                            size="large"
                            sx={{
                                '&:focus': {
                                    outline: 'none'
                                }
                            }}>
                            계속
                        </Button>
                        {
                            mode === "signin" ? 
                                (
                                    <Typography fontSize={14}>
                                        profile를 처음 사용하시나요? {' '}
                                        <Typography
                                            fontSize={14}
                                            component="span" 
                                            sx={{ textDecoration: 'none', color: '#1976d2', cursor: 'pointer' }}
                                            onClick={handleNavigateSignUp} 
                                        >
                                            계정 생성
                                        </Typography>
                                    </Typography>
                                ) : (
                                    <Typography fontSize={14}>
                                        이미 계정이 있나요?{' '}
                                        <Typography
                                            fontSize={14}
                                            component="span" 
                                            sx={{ textDecoration: 'none', color: '#1976d2', cursor: 'pointer' }}
                                            onClick={handleNavigateSignIn}
                                        >
                                            로그인
                                        </Typography>
                                    </Typography>
                                )
                        }
                        
                    </Stack>
                    <OAuthButtons />
                </Stack>
            </Box>
        </Box>
    );
}

export default EmailForm;