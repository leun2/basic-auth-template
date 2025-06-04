import { useState } from 'react';
import {
  Box,
  Button,
  InputAdornment,
  Stack,
  TextField,
  Typography
} from '@mui/material';
import { IconButton } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';
import { useFormik } from 'formik';
import * as Yup from 'yup';
import { useAuth } from "components/auth/AuthContext";
import AppNavigation from "hooks/AppNavigation";

interface SignInPasswordFormProps {
    email: string; // 이메일은 문자열로 받습니다.
    onPrev: () => void; // onPrev 함수는 인자를 받지 않고 아무것도 반환하지 않습니다.
}

function SignInPasswordForm({ email, onPrev }: SignInPasswordFormProps) {
    const authContext = useAuth();
    const [showPassword, setShowPassword] = useState(false);
    const togglePassword = () => setShowPassword((prev) => !prev);

    const { handleNavigateProfile, handleNavigateSignUp,} = AppNavigation();

    const formik = useFormik({
        initialValues: {
            password: '',
        },
        validationSchema: Yup.object({
            password: Yup.string()
                .matches(
                    /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&]).{8,}$/,
                    '영문, 숫자, 특수문자 포함 8자 이상 입력해주세요'
                )
                .required('비밀번호는 필수입니다')
        }),
        onSubmit: async (values) => {

            const success = await authContext.login(email, values.password)
            if (success) handleNavigateProfile()
            else {
                formik.setErrors({
                    password: '아이디 또는 비밀번호가 잘못 되었습니다. 아이디와 비밀번호를 정확히 입력해 주세요'
                });
            }
        }
    });

    return (
        <Box
            sx={{
                minHeight: '100vh',
                display: 'flex',
                alignItems: 'center', // 수직 정중앙
                justifyContent: 'center', // 수평 정중앙
                px: 2, // 반응형 여백
                backgroundColor: '#fff',
                width: { xs: '100vw', md: '100vw' }
            }}>

            <Box component="form" onSubmit={formik.handleSubmit} sx={{ width: '100%', maxWidth: 300 }}>
                <Stack spacing={2}>

                    {/* Heading */}
                    <Typography variant="h5" fontWeight="bold">
                        로그인
                    </Typography>
                    <Stack direction="row" alignItems="center" spacing={1}>
                        <Box>
                            <IconButton
                                onClick={onPrev}
                                disableRipple
                                aria-label="돌아가기"
                                sx={{
                                    borderRadius: 2,
                                    '&:hover': { backgroundColor: 'rgba(0, 0, 0, 0.04)' },
                                    '&:active': { backgroundColor: 'rgba(0, 0, 0, 0.1)' },
                                    '&:focus': {outline: 'none'}
                                }}
                            >
                                <ArrowBackIcon />
                            </IconButton>
                        </Box>

                        <Typography variant="body1" sx={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontSize:'14px', color:'#666666' }}>
                            {email}
                        </Typography>
                    </Stack>
                    {/* Input fields */}
                    <TextField
                        fullWidth
                        label="비밀번호"
                        name="password"
                        type={showPassword ? 'text' : 'password'}
                        value={formik.values.password}
                        onChange={formik.handleChange}
                        onBlur={formik.handleBlur}
                        error={formik.touched.password && Boolean(formik.errors.password)}
                        helperText={formik.touched.password && formik.errors.password}
                        InputProps={{
                            endAdornment: (
                                <InputAdornment position="end">
                                    <IconButton
                                        disableRipple
                                        disableFocusRipple
                                        onClick={togglePassword}
                                        aria-label="비밀번호 보기 전환"
                                        sx={{
                                            borderRadius: 2,
                                            '&:hover': { backgroundColor: 'rgba(0, 0, 0, 0.04)' },
                                            '&:active': { backgroundColor: 'rgba(0, 0, 0, 0.1)' },
                                            '&:focus': {outline: 'none'}
                                        }}
                                    >
                                        {showPassword ? <VisibilityOff /> : <Visibility />}
                                    </IconButton>
                                </InputAdornment>
                            ),
                        }}
                    />
                    

                    {/* Signup button */}
                    <Button type="submit" variant="contained" fullWidth size="large" sx={{'&:focus': {outline: 'none'}}}>
                        등록
                    </Button>

                    {/* Login link */}
                    <Typography fontSize={14}>
                        profile을 처음 사용하시나요? {' '}
                        <Typography
                            fontSize={14}
                            component="span" 
                            sx={{ textDecoration: 'none', color: '#1976d2', cursor: 'pointer' }}
                            onClick={handleNavigateSignUp} 
                        >
                            계정 생성
                        </Typography>
                    </Typography>
                </Stack>
            </Box>
        </Box>
    );
}

export default SignInPasswordForm;