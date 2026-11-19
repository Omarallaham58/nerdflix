import { useState } from "react";

export default function PasswordInput({ value, onChange, placeholder, required, autoFocus, name }) {
    const [show, setShow] = useState(false);

    return (
        <div style={styles.container}>
            <input
                type={show ? "text" : "password"}
                name={name}
                value={value}
                onChange={onChange}
                placeholder={placeholder}
                required={required}
                autoFocus={autoFocus}
                style={styles.input}
            />
            <button
                type="button"
                style={styles.toggleBtn}
                onClick={() => setShow(!show)}
                tabIndex="-1"
            >
                {show ? (
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                        <line x1="1" y1="1" x2="23" y2="23" />
                    </svg>
                ) : (
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                        <circle cx="12" cy="12" r="3" />
                    </svg>
                )}
            </button>
        </div>
    );
}

const styles = {
    container: {
        position: "relative",
        width: "100%",
    },
    input: {
        width: "100%",
        background: "#222",
        border: "1px solid #333",
        borderRadius: "6px",
        padding: "12px",
        paddingRight: "40px",
        color: "#fff",
        fontSize: "14px",
        outline: "none",
        transition: "border-color 0.2s",
    },
    toggleBtn: {
        position: "absolute",
        right: "8px",
        top: "50%",
        transform: "translateY(-50%)",
        background: "none",
        border: "none",
        cursor: "pointer",
        color: "#888",
        padding: "4px",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
    },
};