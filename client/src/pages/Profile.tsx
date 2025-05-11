import React, { useState, useEffect, useRef } from 'react';
import { getUserProfile, postUserProfileImage } from "apis/profileApi";
import { AxiosProgressEvent } from 'axios';

import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import Avatar from '@mui/material/Avatar';
import Stack from '@mui/material/Stack';
import IconButton from '@mui/material/IconButton';
import EditIcon from '@mui/icons-material/Edit';
import CircularProgress from '@mui/material/CircularProgress';
import Header from 'components/layout/Header';
import Footer from 'components/layout/Footer';

interface UserProfile {
    email: string;
    name: string;
    image: string | null;
}

const getInitials = (name: string | null | undefined): string => {
    if (!name) return '';
    return name.split(' ').map(word => word[0]).join('').toUpperCase();
};


function Profile() {
    const [profileData, setProfileData] = useState<UserProfile | null>(null);
    const [isUploading, setIsUploading] = useState<boolean>(false);
    const [uploadProgress, setUploadProgress] = useState<number>(0);

    const fileInputRef = useRef<HTMLInputElement>(null);

    useEffect(() => {
        const fetchProfile = async () => {
            try {
                const response = await getUserProfile();
                setProfileData(response.data as UserProfile);
            } catch (error) {
                console.error("프로필 정보를 가져오는데 실패했습니다:", error);
            }
        };
        fetchProfile();
    }, []);

    const handleEditIconClick = () => {
        fileInputRef.current?.click();
    };

    const handleFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];

        if (!file) return;

        const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB 제한
        const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']; // 허용할 이미지 타입

        if (file.size > MAX_FILE_SIZE) {
            alert('파일 크기가 너무 큽니다 (최대 5MB).');
            event.target.value = ''; // input 초기화
            return;
        }
        if (!allowedTypes.includes(file.type)) {
            alert('지원하지 않는 파일 형식입니다. JPG, PNG, GIF, WebP만 가능합니다.');
            event.target.value = ''; // input 초기화
            return;
        }


        setIsUploading(true);
        setUploadProgress(0);

        const onProgressCallback = (progressEvent: AxiosProgressEvent) => {
            const percentCompleted = Math.round((progressEvent.loaded * 100) / (progressEvent.total || file.size));
            setUploadProgress(percentCompleted);
        };

        try {
            const response = await postUserProfileImage(file, onProgressCallback);

            if (response.status >= 200 && response.status < 300 && response.data) {
                setProfileData(response.data);
                console.log('프로필 이미지가 성공적으로 업데이트되었습니다.');
            } else {
                console.error('이미지 업로드에 실패했습니다. 백엔드 응답:', response);
                alert('이미지 업로드에 실패했습니다. 서버 응답 오류.');
            }

        } catch (error) {
            console.error("이미지 업로드 중 오류 발생:", error);
            if ((error as any).response && (error as any).response.data) {
                alert(`이미지 업로드 실패: ${(error as any).response.data.message || (error as any).response.statusText}`);
            } else {
                alert('이미지 업로드 중 알 수 없는 오류가 발생했습니다.');
            }
        } finally {
            setIsUploading(false);
            setUploadProgress(0);
            event.target.value = '';
        }
    };

    if (!profileData) {
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
                <Box component="main" sx={{ flexGrow: 1, py: 8, textAlign: 'center' }}>
                    <Typography variant="h6">...</Typography>
                </Box>
                <Footer />
             </Box>
         );
    }

    const initials = getInitials(profileData.name);

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
            <Box component="main" sx={{ flexGrow: 1, py: 8 }}>
                <Container maxWidth="md">
                    {isUploading && (
                        <Box sx={{
                            position: 'fixed', // 전체 화면 오버레이
                            top: 0, left: 0, right: 0, bottom: 0,
                            display: 'flex', justifyContent: 'center', alignItems: 'center',
                            bgcolor: 'rgba(255, 255, 255, 0.7)', // 반투명 배경
                            zIndex: 1500,
                            flexDirection: 'column',
                        }}>
                            <CircularProgress variant="determinate" value={uploadProgress} size={60} />

                            <Typography variant="h6" sx={{ mt: 2 }}>
                                {`... ${uploadProgress}%`}
                            </Typography>
                        </Box>
                    )}

                    {profileData ? (
                        <Box sx={{ textAlign: 'center', mb: 4, position: 'relative' }}>

                            <Box sx={{ position: 'relative', display: 'inline-block', mb: 3 }}>
                                <Avatar
                                    alt={profileData.name || 'User'}
                                    src={profileData.image || ''}
                                    sx={{
                                        width: 120,
                                        height: 120,
                                        fontSize: '36px',
                                        opacity: isUploading ? 0.5 : 1,
                                        transition: 'opacity 0.3s ease-in-out',
                                    }}
                                >
                                     {!profileData.image && initials}
                                </Avatar>

                                <IconButton
                                    sx={{
                                        position: 'absolute',
                                        bottom: 0,
                                        right: 0,
                                        transform: 'translate(5%, 5%)',
                                        bgcolor: 'background.paper',
                                        boxShadow: 1,
                                        width: 32,
                                        height: 32,
                                        padding: 0,
                                        '&:hover': {
                                             bgcolor: 'grey.200',
                                        },
                                    }}
                                    onClick={handleEditIconClick}
                                    aria-label="change profile picture"
                                    disabled={isUploading}
                                >
                                    <EditIcon sx={{ fontSize: 18 }} />
                                </IconButton>
                            </Box>

                            <input
                                type="file"
                                accept="image/*"
                                ref={fileInputRef}
                                style={{ display: 'none' }}
                                onChange={handleFileChange}
                            />

                            <Stack spacing={1} sx={{ alignItems: 'center' }}>
                                <Typography variant="h4" component="h1" gutterBottom>
                                    {profileData.name || '이름 없음'}
                                </Typography>
                                <Typography variant="body1" color="text.secondary" gutterBottom>
                                    {profileData.email || '이메일 없음'}
                                </Typography>
                            </Stack>
                        </Box>
                    ) : (
                        <Typography variant="h6" textAlign="center">...</Typography>
                    )}
                </Container>
            </Box>
            <Footer />
        </Box>
    );
}

export default Profile;