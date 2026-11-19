import Header from "../components/Header";
import CategorySidebar from "../components/CategorySidebar";
import { Outlet } from "react-router-dom";

export default function Layout() {
    return (
        <div style={styles.root}>
            <Header />
            <div style={styles.body}>
                <CategorySidebar />
                <main style={styles.main}>
                    <Outlet />
                </main>
            </div>
        </div>
    );
}

const styles = {
    root: {
        minHeight: "100vh",
        background: "linear-gradient(135deg, #0f0f0f, #1a1a1a)",
        color: "#fff",
        fontFamily: "'Segoe UI', sans-serif",
    },
    body: {
        display: "flex",
        minHeight: "calc(100vh - 60px)",
    },
    main: {
        flex: 1,
        padding: "28px 28px",
        overflowY: "auto",
    },
};
