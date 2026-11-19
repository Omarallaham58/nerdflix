import { useSelector, useDispatch } from "react-redux";
import { isSubscribedToCategory } from "../redux/selectors/userSelectors";
import { addSubscription } from "../redux/slices/userSlice";
import { subscribe } from "../api/subscriptionApi";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import SubscribeModal from "./SubscribeModal";
import SignInPromptModal from "./SignInPromptModal";

function formatDuration(seconds) {
    if (!seconds) return null;
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, "0")}`;
}

function formatCount(n) {
    if (n == null) return null;
    if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
    if (n >= 1_000)     return `${(n / 1_000).toFixed(1)}K`;
    return String(n);
}

export default function VideoCard({ video }) {
    const navigate   = useNavigate();
    const dispatch   = useDispatch();
    const [showSubscribeModal, setShowSubscribeModal] = useState(false);
    const [showSignInModal, setShowSignInModal] = useState(false);
    const [hovered,   setHovered]   = useState(false);

    const isAuthenticated = useSelector(state => state.user.isAuthenticated);
    const isSubscribed = useSelector(state =>
        isSubscribedToCategory(state, video.category?.id)
    );

    const handleClick = () => {
        if (isSubscribed) {
            navigate(`/video/${video.id}`);
        } else {
            if (isAuthenticated) {
                setShowSubscribeModal(true);
            } else {
                setShowSignInModal(true);
            }
        }
    };

    const handleSubscribe = async () => {
    try {
      await subscribe(video.category?.id);
      dispatch(addSubscription(video.category?.id));
      setShowSubscribeModal(false);
      navigate(`/video/${video.id}`);
    } catch (err) {
      console.error("Subscription failed", err);
    }
  };

    const duration = formatDuration(video.stats?.duration ?? video.duration);
    const likes    = formatCount(video.stats?.likes  ?? video.likes);
    const views    = formatCount(video.stats?.views  ?? video.views);

    return (
        <>
            <div
                style={{
                    ...s.card,
                    ...(hovered ? s.cardHover : {}),
                }}
                onClick={handleClick}
                onMouseEnter={() => setHovered(true)}
                onMouseLeave={() => setHovered(false)}
            >
                {/* Thumbnail */}
                <div style={s.thumbWrap}>
                    <img
                        src={`http://localhost:8080/thumbnails/${video.id}.jpg`}
                        alt={video.title}
                        style={s.thumb}
                        onError={e => { e.target.style.display = "none"; }}
                    />

                    {!isSubscribed && (
                        <div style={s.lockOverlay}>
                            <span style={s.lockIcon}>🔒</span>
                        </div>
                    )}

                    {duration && (
                        <span style={s.duration}>{duration}</span>
                    )}
                </div>

                {/* Info */}
                <div style={s.info}>
                    <p style={s.title}>{video.title}</p>

                    <div style={s.meta}>
                        {/* Category tag */}
                        {video.category?.name && (
                            <span style={s.categoryTag}>{video.category.name}</span>
                        )}

                        {/* Stats: views + likes */}
                        <div style={s.stats}>
                            {views !== null && (
                                <span style={s.stat}>
                                    <svg style={s.statIcon} viewBox="0 0 24 24" fill="currentColor">
                                        <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zm0 12.5c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>
                                    </svg>
                                    {views}
                                </span>
                            )}
                            {likes !== null && (
                                <span style={s.stat}>
                                    <span style={{ color: "#e50914", fontSize: "11px" }}>♥</span>
                                    {likes}
                                </span>
                            )}
                        </div>
                    </div>
                </div>
            </div>

            <SubscribeModal
                show={showSubscribeModal}
                onConfirm={handleSubscribe}
                onCancel={() => setShowSubscribeModal(false)}
            />
            <SignInPromptModal
                show={showSignInModal}
                onCancel={() => setShowSignInModal(false)}
            />
        </>
    );
}

const s = {
    card: {
        background: "#1a1a1a",
        borderRadius: "10px",
        overflow: "hidden",
        cursor: "pointer",
        border: "1px solid #282828",
        transition: "transform 0.2s, box-shadow 0.2s, border-color 0.2s",
    },
    cardHover: {
        transform: "translateY(-3px)",
        boxShadow: "0 8px 30px rgba(229,9,20,0.2)",
        borderColor: "#3a3a3a",
    },
    thumbWrap: {
        position: "relative",
        width: "100%",
        paddingTop: "56.25%",
        background: "#111",
        overflow: "hidden",
    },
    thumb: {
        position: "absolute",
        top: 0, left: 0,
        width: "100%", height: "100%",
        objectFit: "cover",
    },
    lockOverlay: {
        position: "absolute",
        inset: 0,
        background: "rgba(0,0,0,0.55)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
    },
    lockIcon: { fontSize: "26px" },
    duration: {
        position: "absolute",
        bottom: "7px",
        right: "8px",
        background: "rgba(0,0,0,0.82)",
        color: "#fff",
        fontSize: "12px",
        fontWeight: "600",
        padding: "2px 6px",
        borderRadius: "4px",
        letterSpacing: "0.3px",
    },
    info: {
        padding: "10px 12px 12px",
    },
    title: {
        margin: "0 0 8px",
        fontSize: "14px",
        fontWeight: "600",
        color: "#f0f0f0",
        lineHeight: "1.35",
        display: "-webkit-box",
        WebkitLineClamp: 2,
        WebkitBoxOrient: "vertical",
        overflow: "hidden",
    },
    meta: {
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        gap: "6px",
    },
    categoryTag: {
        fontSize: "11px",
        color: "#888",
        background: "#252525",
        padding: "2px 8px",
        borderRadius: "20px",
        letterSpacing: "0.3px",
        whiteSpace: "nowrap",
        flexShrink: 0,
    },
    stats: {
        display: "flex",
        alignItems: "center",
        gap: "10px",
        flexShrink: 0,
    },
    stat: {
        display: "flex",
        alignItems: "center",
        gap: "3px",
        fontSize: "12px",
        color: "#777",
    },
    statIcon: {
        width: "12px",
        height: "12px",
        color: "#666",
        flexShrink: 0,
    },
};