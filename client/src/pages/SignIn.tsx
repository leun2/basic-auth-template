import { useState } from 'react';
import EmailForm from 'components/auth/EmailForm'
import SignInPasswordForm from 'components/auth/SignInPasswordForm';
import Header from 'components/layout/Header';
import Footer from 'components/layout/Footer';

function SignIn() {

    const [isEmailValid, setSignUpState] = useState(false);
    const [email, setEmail] = useState('');

    return (
        <>
            <Header />
            {
                isEmailValid
                    
                    ? <SignInPasswordForm 
                        email={email}
                        onPrev={() => setSignUpState(false)} />
                    : <EmailForm 
                        mode={"signin"}
                        onNext={
                            (enteredEmail : string) => {
                                setEmail(enteredEmail);
                                setSignUpState(true);
                            }
                        } /> 
            }
            <Footer /> 
        </>
    );
}

export default SignIn;
