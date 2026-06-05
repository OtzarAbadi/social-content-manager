import PageShell from '../components/PageShell.jsx'
import { useState } from "react";
import axios from "axios";
import Cookies from "js-cookie";

function LoginPage({ activeRoute, routes, onNavigate }) {

    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [errorMessage, setErrorMessage] = useState('');

    // הכפתור יהיה מושבת אם אחד השדות ריק
    const isLoginDisabled = !username.trim() || !password.trim();

    const handleLogin = () => {

        axios.post(
            "http://localhost:8081/users/login",
            {
                username,
                password
            },
            {
                withCredentials: true
            }
        )
            .then(response => {

                if (response.data.success) {

                    Cookies.set("token", response.data.token, {
                        expires: 7,
                        secure: window.location.protocol === "https:",
                        sameSite: "strict"
                    });

                    onNavigate("dashboard");

                } else {
                    setErrorMessage("שם משתמש או סיסמה שגויים");
                }
            })
            .catch(() => {
                setErrorMessage("שגיאה בהתחברות לשרת");
            });
    };

    return (
        <PageShell
            activeRoute={activeRoute}
            routes={routes}
            onNavigate={onNavigate}
        >

            <section className="login-page" aria-labelledby="login-title">

                <div className="login-card">

                    <p className="eyebrow">סביבת עבודה ללקוחות</p>

                    <h2 id="login-title">כניסה</h2>

                    <p className="login-note">
                        זהו מסך התחברות למערכת
                    </p>

                    <form
                        className="field-stack"
                        onSubmit={(e) => {
                            e.preventDefault();

                            if (!isLoginDisabled) {
                                handleLogin();
                            }
                        }}
                    >

                        <label>
                            אימייל או שם משתמש

                            <input
                                type="text"
                                placeholder="הקלד אימייל או שם משתמש"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                            />
                        </label>

                        <label>
                            סיסמה

                            <input
                                type="password"
                                placeholder="הקלד סיסמה"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                            />
                        </label>

                        {errorMessage && (
                            <p className="login-error">
                                {errorMessage}
                            </p>
                        )}

                        <button
                            className="login-button"
                            type="submit"
                            disabled={isLoginDisabled}
                            style={{
                                backgroundColor: isLoginDisabled ? "#999999" : "",
                                cursor: isLoginDisabled ? "not-allowed" : "pointer",
                                opacity: isLoginDisabled ? 0.6 : 1
                            }}
                        >
                            התחברות
                        </button>

                    </form>

                </div>

            </section>

        </PageShell>
    );
}

export default LoginPage;
