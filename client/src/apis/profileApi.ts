import { apiClient } from "apis/apiClient";
import { AxiosProgressEvent } from 'axios';


export const getUserProfile = () =>
    apiClient.get('/v1/user/profile');

export const postUserProfileImage = (
    file: File,
    onProgress?: (progressEvent: AxiosProgressEvent) => void
): Promise<any> => {

    const formData = new FormData();
    formData.append('image', file);

    const config = {
        headers: {
            'Content-Type': 'multipart/form-data'
        },
        onUploadProgress: onProgress,
    };

    return apiClient.post('/v1/user/profile/image', formData, config);
};
