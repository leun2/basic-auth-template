import { useState } from 'react';
import EmailForm from 'components/auth/EmailForm'
import SignUpDetailsForm from 'components/auth/SignUpDetailsForm';
import Header from 'components/layout/Header';
import Footer from 'components/layout/Footer';

function SignUp() {

    const [isEmailValid, setSignUpState] = useState(false);
    const [email, setEmail] = useState('');

    return (
        <>
            <Header />
            {
                isEmailValid
                    
                    ? <SignUpDetailsForm 
                        email={email}
                        onPrev={() => setSignUpState(false)} />
                    : <EmailForm 
                        mode={"signup"}
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

export default SignUp;
