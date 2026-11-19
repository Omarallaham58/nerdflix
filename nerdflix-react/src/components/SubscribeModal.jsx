import React from "react";
export default function SubscribeModal({ show, onConfirm, onCancel }) {
    if (!show) return null;

    return (
        <div style={styles.overlay} onClick={onCancel}>
            <div style={styles.modal} onClick={e => e.stopPropagation()}>
                <div style={styles.iconWrap}>
                    <span style={styles.icon}>🔒</span>
                </div>
                <h3 style={styles.title}>Members Only</h3>
                <p style={styles.desc}>
                    Subscribe to this category to unlock and watch this video.
                </p>
                <div style={styles.actions}>
                    <button
                        style={styles.confirmBtn}
                        onClick={onConfirm}
                        onMouseEnter={e => e.currentTarget.style.background = "#f6121d"}
                        onMouseLeave={e => e.currentTarget.style.background = "#e50914"}
                    >
                        Subscribe Now
                    </button>
                    <button
                        style={styles.cancelBtn}
                        onClick={onCancel}
                        onMouseEnter={e => e.currentTarget.style.background = "#3a3a3a"}
                        onMouseLeave={e => e.currentTarget.style.background = "#2a2a2a"}
                    >
                        Cancel
                    </button>
                </div>
            </div>
        </div>
    );
}

const styles = {
    overlay: {
        position: "fixed",
        inset: 0,
        background: "rgba(0,0,0,0.75)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 999,
        backdropFilter: "blur(4px)",
    },
    modal: {
        background: "#1c1c1c",
        border: "1px solid #2e2e2e",
        borderRadius: "14px",
        padding: "36px 40px",
        maxWidth: "380px",
        width: "90%",
        textAlign: "center",
        boxShadow: "0 20px 60px rgba(0,0,0,0.8)",
    },
    iconWrap: {
        fontSize: "40px",
        marginBottom: "16px",
        lineHeight: 1,
    },
    icon: {},
    title: {
        margin: "0 0 10px",
        fontSize: "20px",
        fontWeight: "700",
        color: "#fff",
        fontFamily: "'Segoe UI', sans-serif",
        letterSpacing: "0.3px",
    },
    desc: {
        margin: "0 0 24px",
        color: "#999",
        fontSize: "14px",
        lineHeight: "1.5",
        fontFamily: "'Segoe UI', sans-serif",
    },
    actions: {
        display: "flex",
        gap: "12px",
        justifyContent: "center",
    },
    confirmBtn: {
        background: "#e50914",
        border: "none",
        padding: "10px 24px",
        borderRadius: "8px",
        fontSize: "14px",
        fontWeight: "600",
        color: "#fff",
        cursor: "pointer",
        transition: "background 0.2s",
        letterSpacing: "0.3px",
    },
    cancelBtn: {
        background: "#2a2a2a",
        border: "1px solid #3a3a3a",
        padding: "10px 24px",
        borderRadius: "8px",
        fontSize: "14px",
        fontWeight: "600",
        color: "#aaa",
        cursor: "pointer",
        transition: "background 0.2s",
    },
};