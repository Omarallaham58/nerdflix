import { useDispatch, useSelector } from "react-redux";
import { logout } from "../redux/slices/userSlice";
import { useNavigate } from "react-router-dom";
import AccountPage from "../pages/AccountPage";

export default function Header() {
    const username = useSelector(s => s.user.username);
    const isAuthenticated = useSelector(s => s.user.isAuthenticated);
    const dispatch = useDispatch();
    const navigate = useNavigate();

    const handleLogout = () => {
        dispatch(logout());
    };

    const handleSignIn = () => {
        navigate("/auth");
    };

    return (
        <header style={styles.header}>
            <div style={styles.logo} onClick={() => navigate("/")}>
                <span style={styles.logoIcon}>▶</span>
                <span style={styles.logoText}>NERDFLIX</span>
            </div>

            <div style={styles.right}>
                {isAuthenticated ? (
                    <>
                        <div style={styles.avatar} onClick={() => navigate("/account")}>
                            {username?.[0]?.toUpperCase() ?? "U"}
                        </div>
                        <span style={styles.username}>{username}</span>
                        <button 
                            style={styles.logoutBtn} 
                            onClick={handleLogout}
                            onMouseEnter={e => e.target.style.background = "#f6121d"}
                            onMouseLeave={e => e.target.style.background = "#e50914"}
                        >
                            Logout
                        </button>
                    </>
                ) : (
                    <button 
                        style={styles.signInBtn} 
                        onClick={handleSignIn}
                        onMouseEnter={e => e.target.style.background = "#f6121d"}
                        onMouseLeave={e => e.target.style.background = "#e50914"}
                    >
                        Sign In
                    </button>
                )}
            </div>
        </header>
    );
}

const styles = {
    header: {
        position: "sticky",
        top: 0,
        zIndex: 100,
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        padding: "0 28px",
        height: "60px",
        background: "#0f0f0f",
        borderBottom: "1px solid #1f1f1f",
        boxShadow: "0 2px 20px rgba(0,0,0,0.8)",
    },
    logo: {
        display: "flex",
        alignItems: "center",
        gap: "10px",
        cursor: "pointer",
    },
    logoIcon: {
        color: "#e50914",
        fontSize: "20px",
    },
    logoText: {
        fontFamily: "'Bebas Neue', 'Impact', sans-serif",
        fontSize: "26px",
        letterSpacing: "4px",
        color: "#fff",
        fontWeight: "700",
    },
    right: {
        display: "flex",
        alignItems: "center",
        gap: "14px",
    },
    avatar: {
        width: "34px",
        height: "34px",
        borderRadius: "50%",
        background: "#e50914",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        fontWeight: "700",
        fontSize: "14px",
        color: "#fff",
        cursor: "pointer",
    },
    username: {
        fontSize: "14px",
        color: "#ccc",
        fontFamily: "'Segoe UI', sans-serif",
        cursor: "default",
    },
    logoutBtn: {
        background: "#e50914",
        border: "none",
        padding: "7px 16px",
        borderRadius: "6px",
        fontSize: "13px",
        color: "white",
        cursor: "pointer",
        fontWeight: "600",
        letterSpacing: "0.5px",
        transition: "background 0.2s",
    },
    signInBtn: {
        background: "#e50914",
        border: "none",
        padding: "7px 16px",
        borderRadius: "6px",
        fontSize: "13px",
        color: "white",
        cursor: "pointer",
        fontWeight: "600",
        letterSpacing: "0.5px",
        transition: "background 0.2s",
    },
};