import { useState } from "react";
import { useDispatch } from "react-redux";
import { useNavigate } from "react-router-dom";
import api from "../api/axiosInstance";
import { setUser } from "../redux/slices/userSlice";
import PasswordInput from "../components/PasswordInput";

export default function AuthPage() {
  const [isLogin, setIsLogin] = useState(true);
  const [formData, setFormData] = useState({
    username: "",
    password: "",
    confirmPassword: "",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setError("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    // Validation
    if (!isLogin && formData.password !== formData.confirmPassword) {
      setError("Passwords do not match");
      setLoading(false);
      return;
    }

    try {
      const endpoint = isLogin ? "/auth/login" : "/auth/register";
      const response = await api.post(endpoint, {
        username: formData.username,
        password: formData.password,
      });

      if (isLogin) {
        // Store token and user data
        localStorage.setItem("token", response.data.token);
        localStorage.setItem("refreshToken", response.data.refreshToken);

        dispatch(
          setUser({
            id: response.data.userId,
            username: response.data.username,
            roles: response.data.roles,
          }),
        );

        // Redirect to home page
        navigate("/");
      } else {
        // After registration, switch to login mode
        setIsLogin(true);
        setFormData({ username: "", password: "", confirmPassword: "" });
        setError("Registration successful! Please login.");
      }
    } catch (err) {
      setError(err.response?.data?.message || "Authentication failed");
    } finally {
      setLoading(false);
    }
  };

  const toggleMode = () => {
    setIsLogin(!isLogin);
    setFormData({ username: "", password: "", confirmPassword: "" });
    setError("");
  };

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <div style={styles.logoWrap}>
          <span style={styles.logoIcon}>▶</span>
          <span style={styles.logoText}>NERDFLIX</span>
        </div>

        <h2 style={styles.title}>{isLogin ? "Sign In" : "Create Account"}</h2>

        {error && (
          <div
            style={
              error.includes("successful") ? styles.successMsg : styles.errorMsg
            }
          >
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={styles.form}>
          <div style={styles.inputGroup}>
            <label style={styles.label}>Username</label>
            <input
              type="text"
              name="username"
              value={formData.username}
              onChange={handleChange}
              style={styles.input}
              required
              autoFocus
            />
          </div>

          <div style={styles.inputGroup}>
            <label style={styles.label}>Password</label>
            {/* <input
                            type="password"
                            name="password"
                            value={formData.password}
                            onChange={handleChange}
                            style={styles.input}
                            required
                        /> */}
            <PasswordInput
              name="password"
              value={formData.password}
              onChange={handleChange}
              required
              autoFocus={isLogin} // only autoFocus on first field
            />
          </div>

          {!isLogin && (
            <div style={styles.inputGroup}>
              <label style={styles.label}>Confirm Password</label>
              {/* <input
                type="password"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                style={styles.input}
                required
              /> */}
              <PasswordInput
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                required
              />
            </div>
          )}

          <button
            type="submit"
            style={styles.submitBtn}
            disabled={loading}
            onMouseEnter={(e) => (e.currentTarget.style.background = "#f6121d")}
            onMouseLeave={(e) => (e.currentTarget.style.background = "#e50914")}
          >
            {loading ? "Please wait..." : isLogin ? "Sign In" : "Sign Up"}
          </button>
        </form>

        <div style={styles.toggleWrap}>
          <span style={styles.toggleText}>
            {isLogin ? "Don't have an account?" : "Already have an account?"}
          </span>
          <button onClick={toggleMode} style={styles.toggleBtn}>
            {isLogin ? "Sign Up" : "Sign In"}
          </button>
        </div>
      </div>
    </div>
  );
}

const styles = {
  container: {
    minHeight: "100vh",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    background: "linear-gradient(135deg, #0f0f0f, #1a1a1a)",
    padding: "20px",
  },
  card: {
    background: "#181818",
    padding: "40px",
    borderRadius: "12px",
    width: "100%",
    maxWidth: "400px",
    boxShadow: "0 20px 60px rgba(0,0,0,0.8)",
    border: "1px solid #282828",
  },
  logoWrap: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    gap: "10px",
    marginBottom: "24px",
  },
  logoIcon: {
    color: "#e50914",
    fontSize: "28px",
  },
  logoText: {
    fontFamily: "'Bebas Neue', 'Impact', sans-serif",
    fontSize: "32px",
    letterSpacing: "4px",
    color: "#fff",
    fontWeight: "700",
  },
  title: {
    margin: "0 0 24px",
    fontSize: "24px",
    fontWeight: "700",
    color: "#fff",
    textAlign: "center",
  },
  form: {
    display: "flex",
    flexDirection: "column",
    gap: "20px",
  },
  inputGroup: {
    display: "flex",
    flexDirection: "column",
    gap: "6px",
  },
  label: {
    fontSize: "13px",
    fontWeight: "600",
    color: "#aaa",
    letterSpacing: "0.5px",
    textTransform: "uppercase",
  },
  input: {
    background: "#222",
    border: "1px solid #333",
    borderRadius: "6px",
    padding: "12px",
    color: "#fff",
    fontSize: "14px",
    outline: "none",
    transition: "border-color 0.2s",
  },
  submitBtn: {
    background: "#e50914",
    border: "none",
    borderRadius: "8px",
    padding: "14px",
    color: "#fff",
    fontSize: "16px",
    fontWeight: "600",
    cursor: "pointer",
    transition: "background 0.2s",
    marginTop: "10px",
  },
  toggleWrap: {
    marginTop: "24px",
    textAlign: "center",
    borderTop: "1px solid #282828",
    paddingTop: "20px",
  },
  toggleText: {
    color: "#888",
    fontSize: "14px",
    marginRight: "8px",
  },
  toggleBtn: {
    background: "none",
    border: "none",
    color: "#e50914",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
    textDecoration: "underline",
  },
  errorMsg: {
    background: "rgba(229,9,20,0.1)",
    border: "1px solid rgba(229,9,20,0.3)",
    color: "#ff5c5c",
    padding: "10px",
    borderRadius: "6px",
    marginBottom: "20px",
    fontSize: "13px",
    textAlign: "center",
  },
  successMsg: {
    background: "rgba(46,204,113,0.1)",
    border: "1px solid rgba(46,204,113,0.3)",
    color: "#2ecc71",
    padding: "10px",
    borderRadius: "6px",
    marginBottom: "20px",
    fontSize: "13px",
    textAlign: "center",
  },
};
