export default function VideoCardSkeleton() {
    return (
        <div style={styles.card}>
            <div style={styles.thumbWrap}>
                <div style={styles.thumbSkeleton} />
            </div>
            <div style={styles.info}>
                <div style={styles.titleSkeleton} />
                <div style={styles.metaSkeleton} />
            </div>
        </div>
    );
}

const styles = {
    card: {
        background: "#1a1a1a",
        borderRadius: "10px",
        overflow: "hidden",
        border: "1px solid #282828",
    },
    thumbWrap: {
        width: "100%",
        paddingTop: "56.25%",
        background: "#111",
        position: "relative",
    },
    thumbSkeleton: {
        position: "absolute",
        top: 0,
        left: 0,
        width: "100%",
        height: "100%",
        background: "linear-gradient(90deg, #222 25%, #333 50%, #222 75%)",
        backgroundSize: "200% 100%",
        animation: "shimmer 1.5s infinite",
    },
    info: {
        padding: "10px 12px 12px",
    },
    titleSkeleton: {
        height: "16px",
        width: "80%",
        background: "#222",
        borderRadius: "4px",
        marginBottom: "8px",
        animation: "shimmer 1.5s infinite",
        backgroundSize: "200% 100%",
    },
    metaSkeleton: {
        height: "12px",
        width: "40%",
        background: "#222",
        borderRadius: "4px",
        animation: "shimmer 1.5s infinite",
        backgroundSize: "200% 100%",
    },
};
