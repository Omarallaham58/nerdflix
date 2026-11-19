import { useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import api from "../api/axiosInstance";
import PasswordInput from "../components/PasswordInput";
import { setUser } from "../redux/slices/userSlice";

export default function AccountPage() {
    const dispatch = useDispatch();
    const navigate = useNavigate();
    const user = useSelector(state => state.user);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    const [formData, setFormData] = useState({
        username: "",
        newPassword: "",
        confirmPassword: "",
    });

    // Initialize with current username
    useEffect(() => {
        if (user.username) {
            setFormData(prev => ({ ...prev, username: user.username }));
        }
    }, [user.username]);

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
        setError("");
        setSuccess("");
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError("");
        setSuccess("");

        // Basic validation
        if (!formData.username.trim()) {
            setError("Username cannot be empty");
            setLoading(false);
            return;
        }

        if (formData.newPassword || formData.confirmPassword) {
            if (formData.newPassword !== formData.confirmPassword) {
                setError("New passwords do not match");
                setLoading(false);
                return;
            }
            if (formData.newPassword.length < 1) {
                setError("Password cannot be empty");
                setLoading(false);
                return;
            }
        }

        try {
            // Prepare payload – only send fields that changed
            const payload = {
                username: formData.username,
                password: formData.newPassword || "", // empty = no change
            };

            const response = await api.put(`/auth/manage/${user.id}`, payload);

            // Update Redux store with new username if changed
            if (response.data.username !== user.username) {
                dispatch(setUser({
                    id: user.id,
                    username: response.data.username,
                    roles: user.roles,
                }));
                // Update localStorage
                localStorage.setItem("username", response.data.username);
            }

            setSuccess("Account updated successfully");
            // Clear password fields
            setFormData(prev => ({ ...prev, newPassword: "", confirmPassword: "" }));
        } catch (err) {
            const msg = err.response?.data?.message || "Failed to update account";
            setError(msg);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                <h1 style={styles.title}>Account Settings</h1>

                {error && <div style={styles.errorMsg}>{error}</div>}
                {success && <div style={styles.successMsg}>{success}</div>}

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
                        />
                    </div>

                    <div style={styles.inputGroup}>
                        <label style={styles.label}>New Password (leave blank to keep current)</label>
                        <PasswordInput
                            name="newPassword"
                            value={formData.newPassword}
                            onChange={handleChange}
                            placeholder="New password"
                            style={styles.input}
                        />
                    </div>

                    <div style={styles.inputGroup}>
                        <label style={styles.label}>Confirm New Password</label>
                        <PasswordInput
                            name="confirmPassword"
                            value={formData.confirmPassword}
                            onChange={handleChange}
                            placeholder="Confirm new password"
                            style={styles.input}
                        />
                    </div>

                    <div style={styles.buttonGroup}>
                        <button
                            type="submit"
                            style={styles.saveBtn}
                            disabled={loading}
                            onMouseEnter={e => e.currentTarget.style.background = "#f6121d"}
                            onMouseLeave={e => e.currentTarget.style.background = "#e50914"}
                        >
                            {loading ? "Saving..." : "Save Changes"}
                        </button>
                        <button
                            type="button"
                            style={styles.cancelBtn}
                            onClick={() => navigate("/")}
                            onMouseEnter={e => e.currentTarget.style.background = "#3a3a3a"}
                            onMouseLeave={e => e.currentTarget.style.background = "#2a2a2a"}
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

const styles = {
    container: {
        maxWidth: "500px",
        margin: "40px auto",
        padding: "0 20px",
    },
    card: {
        background: "#1a1a1a",
        borderRadius: "12px",
        padding: "30px",
        border: "1px solid #282828",
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
    buttonGroup: {
        display: "flex",
        gap: "12px",
        marginTop: "10px",
    },
    saveBtn: {
        flex: 1,
        background: "#e50914",
        border: "none",
        borderRadius: "6px",
        padding: "12px",
        color: "#fff",
        fontSize: "14px",
        fontWeight: "600",
        cursor: "pointer",
        transition: "background 0.2s",
    },
    cancelBtn: {
        flex: 1,
        background: "#2a2a2a",
        border: "1px solid #3a3a3a",
        borderRadius: "6px",
        padding: "12px",
        color: "#aaa",
        fontSize: "14px",
        fontWeight: "600",
        cursor: "pointer",
        transition: "background 0.2s",
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