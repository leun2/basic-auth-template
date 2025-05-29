import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Box from "@mui/material/Box";
import CssBaseline from '@mui/material/CssBaseline';

import SignIn from "pages/SignIn";
import SignUp from "pages/SignUp";
import { AuthProvider } from "components/auth/AuthContext";
import { GoogleOAuthProvider } from '@react-oauth/google';
import AuthenticatedRoute from "components/auth/AuthenticatedRoute";
import Settings from "pages/Settings";
import Home from "pages/Home";
import Profile from "pages/Profile";
import NaverCallback from "pages/NaverCallback"; 

function App() {
    return (
        <GoogleOAuthProvider clientId={import.meta.env.VITE_GOOGLE_CLIENT_ID}>
            <AuthProvider>
                <Router>
                    <Box
                        sx={{
                            display: 'flex',
                            flexDirection: 'column',
                            minHeight: '100vh',
                            overflowX: 'hidden'
                        }}
                    >
                        <CssBaseline />
                        <Box component="main" sx={{ flexGrow: 1 }}>
                            <Routes>
                                <Route path="/" element={<Home />} />
                                
                                <Route
                                    path="/settings"
                                    element={<AuthenticatedRoute><Settings /></AuthenticatedRoute>}
                                />
                                <Route
                                    path="/profile"
                                    element={<AuthenticatedRoute><Profile /></AuthenticatedRoute>}
                                />
                                <Route path="/signin" element={<SignIn />} />
                                <Route path="/signup" element={<SignUp />} />
                                <Route path="/naver-callback" element={<NaverCallback />} />
                                
                                <Route path='*' element={<Home />} />
                            </Routes>
                        </Box>
                    </Box>
                </Router>
            </AuthProvider>
        </GoogleOAuthProvider>
    );
}

export default App;