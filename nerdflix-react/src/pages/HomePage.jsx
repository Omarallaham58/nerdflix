import VideoCard from "../components/VideoCard";
import VideoCardSkeleton from "../components/VideoCardSkeleton";
import withSkeleton from "../hoc/withSkeleton";
import { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { fetchVideos } from "../redux/slices/videoSlice";
import { fetchSubscriptions } from "../redux/slices/userSlice";
import { fetchCategories } from "../redux/slices/categorySlice";

// Create a skeleton‑enabled VideoCard
const VideoCardWithSkeleton = withSkeleton(VideoCard, VideoCardSkeleton);

export default function HomePage() {
    const dispatch = useDispatch();
    const videos = useSelector(s => s.videos.data);
    const loading = useSelector(s => s.videos.loading);
    const isAuthenticated = useSelector(s => s.user.isAuthenticated);

    useEffect(() => {
        dispatch(fetchCategories());
        dispatch(fetchVideos());
        if (isAuthenticated) {
            dispatch(fetchSubscriptions());
        }
    }, [isAuthenticated, dispatch]);

    return (
        <div style={styles.page}>
            <div style={styles.pageHeader}>
                <h2 style={styles.heading}>All Videos</h2>
                <span style={styles.count}>{videos.length} videos</span>
            </div>

            <div style={styles.grid}>
                {loading ? (
                    // Show skeletons while loading
                    Array.from({ length: 6 }).map((_, i) => (
                        <div style={styles.gridItem} key={i}>
                            <VideoCardWithSkeleton loading={true} />
                        </div>
                    ))
                ) : (
                    videos.map(v => (
                        <div style={styles.gridItem} key={v.id}>
                            <VideoCardWithSkeleton video={v} loading={false} />
                        </div>
                    ))
                )}
            </div>
        </div>
    );
}

const styles = {
    // ... your existing styles ...
        page: { fontFamily: "'Segoe UI', sans-serif" },
    pageHeader: {
        display: "flex",
        alignItems: "baseline",
        gap: "14px",
        marginBottom: "22px",
        paddingBottom: "14px",
        borderBottom: "1px solid #222",
    },
    heading: { margin: 0, fontSize: "20px", fontWeight: "700", color: "#fff", letterSpacing: "0.3px" },
    count: { fontSize: "13px", color: "#666" },
    grid: { display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(240px, 1fr))", gap: "20px" },
    gridItem: { minWidth: 0 },
};