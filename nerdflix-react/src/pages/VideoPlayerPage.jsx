import { useEffect, useRef, useState, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import api from "../api/axiosInstance";
import {
  incrementLocalLike,
  incrementLocalView,
  decrementLocalLike,
  setVideo,
} from "../redux/slices/videoSlice";
import Hls from "hls.js";

function formatCount(n) {
  if (n == null) return "0";
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
  return String(n);
}

export default function VideoPlayerPage() {
  const { videoId } = useParams();
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const videoFromStore = useSelector((s) =>
    s.videos.data.find((v) => String(v.id) === String(videoId))
  );
  const isAuthenticated = useSelector((s) => s.user.isAuthenticated);

  const [video, setLocalVideo] = useState(videoFromStore);
  const [videoLoading, setVideoLoading] = useState(false);
  const [streamInfo, setStreamInfo] = useState(null);
  const [streamLoading, setStreamLoading] = useState(true);
  const [error, setError] = useState(null);
  const [fatalError, setFatalError] = useState(false);

  const [liked, setLiked] = useState(video?.stats?.likedByCurrentUser || false);
  const [likeLoading, setLikeLoading] = useState(false);
  const [viewTracked, setViewTracked] = useState(false);

  // Quality selector state
  const [availableLevels, setAvailableLevels] = useState([]);
  const [currentLevel, setCurrentLevel] = useState(-1);

  const videoRef = useRef(null);
  const hlsRef = useRef(null);

  const safePlay = useCallback(async () => {
    const el = videoRef.current;
    if (!el) return;
    try {
      await el.play();
    } catch (e) {
      console.warn("[PLAYER] play() blocked:", e?.message || e);
    }
  }, []);

  // Always fetch authenticated video details on mount
  useEffect(() => {
    if (!videoId) return;
    let isMounted = true;
    setVideoLoading(true);
    api
      .get(`/api/videos/${videoId}`)
      .then((res) => {
        if (isMounted) {
          setLocalVideo(res.data);
          dispatch(setVideo(res.data)); // update Redux store with authenticated data
          console.log("Fetched video details from API:", res.data);
        }
      })
      .catch((err) => {
        console.error("Failed to fetch video details", err);
        if (isMounted) {
          setError("Video not found or unavailable.");
        }
      })
      .finally(() => {
        if (isMounted) setVideoLoading(false);
      });
    return () => {
      isMounted = false;
    };
  }, [videoId, dispatch]);

  // Sync liked state when video is loaded or updated (including optimistic updates)
  useEffect(() => {
    if (video?.stats?.likedByCurrentUser !== undefined) {
      console.log("Syncing liked state from video data:", video.stats.likedByCurrentUser);
      setLiked(video.stats.likedByCurrentUser);
    } else {
      console.log("Video stats or likedByCurrentUser is undefined, cannot sync liked state");
    }
  }, [video]);

  // Fetch stream info
  useEffect(() => {
    if (!isAuthenticated) {
      navigate("/auth");
      return;
    }

    const fetchStreamInfo = async () => {
      try {
        setStreamLoading(true);
        setError(null);
        setFatalError(false);
        const response = await api.get(`/api/videos/${videoId}/stream-info`);
        setStreamInfo(response.data);
      } catch (err) {
        if (err.response?.status === 403) {
          setError("You need to subscribe to this category to watch this video.");
        } else {
          setError("Failed to load video stream.");
        }
        console.error("Stream info error:", err);
      } finally {
        setStreamLoading(false);
      }
    };

    fetchStreamInfo();
  }, [videoId, isAuthenticated, navigate]);

  const destroyHls = useCallback(() => {
    if (hlsRef.current) {
      try {
        hlsRef.current.destroy();
      } catch {}
      hlsRef.current = null;
    }
    // Reset quality selector state
    setAvailableLevels([]);
    setCurrentLevel(-1);
  }, []);

  useEffect(() => {
    const el = videoRef.current;
    const manifestUrl = streamInfo?.manifestUrl;

    if (!el || !manifestUrl) return;
    if (fatalError) return;

    destroyHls();

    el.pause();
    el.removeAttribute("src");
    el.load();

    const isSafari = /^((?!chrome|android).)*safari/i.test(navigator.userAgent);
    if (isSafari) {
      console.log("[HLS] Using native Safari playback:", manifestUrl);
      el.src = manifestUrl;
      safePlay();
      return;
    }

    if (Hls.isSupported()) {
      console.log("[HLS] Using hls.js:", manifestUrl);

      const hls = new Hls({
        maxLoadingDelay: 4,
        minAutoBitrate: 0,
        lowLatencyMode: false,
        enableWorker: true,
        startLevel: 0,
        capLevelToPlayerSize: true,
        debug: true,
        manifestLoadPolicy: { default: { maxTimeToFirstByteMs: 30000, maxLoadTimeMs: 120000 } },
        playlistLoadPolicy: { default: { maxTimeToFirstByteMs: 30000, maxLoadTimeMs: 120000 } },
        fragmentLoadPolicy: { default: { maxTimeToFirstByteMs: 30000, maxLoadTimeMs: 120000 } },
      });

      hlsRef.current = hls;

      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        console.log("[HLS] MANIFEST_PARSED");
        // Build level list for UI
        const levels = hls.levels.map((level, idx) => ({
          index: idx,
          height: level.height,
          name: level.height ? `${level.height}p` : `Quality ${idx}`,
          bitrate: level.bitrate,
        }));
        setAvailableLevels(levels);
        setCurrentLevel(hls.currentLevel);
        safePlay();
      });

      // Listen for level switches (auto or manual)
      hls.on(Hls.Events.LEVEL_SWITCHED, (event, data) => {
        console.log("[HLS] LEVEL_SWITCHED to level", data.level);
        setCurrentLevel(data.level);
      });

      hls.on(Hls.Events.ERROR, (event, data) => {
        console.error("[HLS ERROR]", {
          type: data?.type,
          details: data?.details,
          fatal: data?.fatal,
          response: data?.response,
          reason: data?.reason,
          url: data?.url,
        });

        if (data.fatal) {
          let userMessage = "Playback error. Please try again.";
          if (data.details?.includes("key") || data.details?.includes("decrypt")) {
            userMessage = "Decryption error. Check your connection and try again.";
          } else if (data.type === Hls.ErrorTypes.NETWORK_ERROR) {
            userMessage = "Network connection error. Please check your internet and try again.";
          } else if (data.type === Hls.ErrorTypes.MEDIA_ERROR) {
            userMessage = "Media error. The video may be corrupted. Try again later.";
          }

          setError(userMessage);
          setFatalError(true);
          destroyHls();
        } else {
          console.warn("[HLS] Non‑fatal error, hls.js will retry");
        }
      });

      hls.loadSource(manifestUrl);
      hls.attachMedia(el);

      return destroyHls;
    }

    setError("Your browser does not support HLS playback.");
    setFatalError(true);
  }, [streamInfo?.manifestUrl, safePlay, destroyHls, fatalError]);

  // --- View tracking handler (attached directly to <video>) ---
  const handleTimeUpdate = useCallback((e) => {
    const currentTime = e.target.currentTime;
    console.log("[VIEW] timeupdate, currentTime:", currentTime);
    if (currentTime >= 10 && !viewTracked && video) {
      console.log("[VIEW] 10 seconds reached – sending view");
      setViewTracked(true);

      // Optimistically update local video state
      setLocalVideo(prev => ({
        ...prev,
        stats: {
          ...prev.stats,
          views: prev.stats.views + 1,
        },
      }));
      dispatch(incrementLocalView(video.id));

      api
        .post(`/api/videos/${video.id}/view`)
        .then(() => console.log("[VIEW] view sent successfully"))
        .catch((e) => console.warn("[VIEW] View tracking failed", e));
    }
  }, [video, viewTracked, dispatch]);

  // --- Like handler with optimistic count and likedByCurrentUser update ---
  const handleLike = async () => {
    if (likeLoading || !video) return;

    setLikeLoading(true);
    const wasLiked = liked;
    try {
      if (wasLiked) {
        await api.post(`/api/videos/${video.id}/unlike`);
        setLocalVideo(prev => ({
          ...prev,
          stats: {
            ...prev.stats,
            likes: prev.stats.likes - 1,
            likedByCurrentUser: false,
          },
        }));
        dispatch(decrementLocalLike(video.id));
      } else {
        await api.post(`/api/videos/${video.id}/like`);
        setLocalVideo(prev => ({
          ...prev,
          stats: {
            ...prev.stats,
            likes: prev.stats.likes + 1,
            likedByCurrentUser: true,
          },
        }));
        dispatch(incrementLocalLike(video.id));
      }
      setLiked(!wasLiked);
    } catch (e) {
      console.error("Like action failed", e);
    } finally {
      setLikeLoading(false);
    }
  };

  const handleRetry = async () => {
    setError(null);
    setFatalError(false);
    setStreamLoading(true);
    try {
      const response = await api.get(`/api/videos/${videoId}/stream-info`);
      setStreamInfo(response.data);
    } catch (err) {
      setError("Failed to reload stream. Please try again later.");
      console.error("Retry error:", err);
    } finally {
      setStreamLoading(false);
    }
  };

  if (!isAuthenticated) return null;

  if (streamLoading || videoLoading) {
    return (
      <div style={s.center}>
        <div style={s.spinner} />
        <p style={s.loadingText}>
          {videoLoading ? "Loading video details..." : "Loading video stream..."}
        </p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={s.center}>
        <div style={s.errorIcon}>⚠️</div>
        <p style={s.errorText}>{error}</p>
        <button style={s.backBtn} onClick={handleRetry}>Try Again</button>
        <button style={{ ...s.backBtn, background: "#333", marginTop: "8px" }} onClick={() => navigate("/")}>Back to Home</button>
      </div>
    );
  }

  if (!video) {
    return (
      <div style={s.center}>
        <p style={s.errorText}>Video not found</p>
        <button style={s.backBtn} onClick={() => navigate("/")}>Back to Home</button>
      </div>
    );
  }

  const views = formatCount(video.stats?.views ?? video.views);
  const likes = formatCount(video.stats?.likes ?? video.likes);

  return (
    <div style={s.page}>
      <div style={s.playerWrap}>
        <video
          ref={videoRef}
          style={s.video}
          controls
          playsInline
          muted
          poster={`http://localhost:8080/thumbnails/${video.id}.jpg`}
          onClick={safePlay}
          onTimeUpdate={handleTimeUpdate}   // view tracking
        />
      </div>

      <div style={s.infoWrap}>
        <h1 style={s.title}>{video.title}</h1>

        <div style={s.metaRow}>
          <div style={s.metaLeft}>
            {video.category?.name && <span style={s.categoryBadge}>{video.category.name}</span>}
            <span style={s.metaStat}>
              <svg style={s.metaIcon} viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zm0 12.5c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z" />
              </svg>
              {views} views
            </span>
          </div>

          <button
            style={{
              ...s.likeBtn,
              ...(liked ? s.likeBtnActive : {}),
              ...(likeLoading ? s.likeBtnDisabled : {}),
            }}
            onClick={handleLike}
            disabled={likeLoading}
          >
            <svg
              style={s.likeIcon}
              viewBox="0 0 24 24"
              fill={liked ? "currentColor" : "none"}
              stroke="currentColor"
              strokeWidth="2"
            >
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
            </svg>
            {liked ? "Liked" : "Like"}
            <span style={s.likeCount}>{likes}</span>
          </button>
        </div>

        {/* Quality Selector */}
        {availableLevels.length > 0 && (
          <div style={s.qualitySelector}>
            <label htmlFor="quality" style={s.qualityLabel}>Quality: </label>
            <select
              id="quality"
              value={currentLevel}
              onChange={(e) => {
                const level = parseInt(e.target.value, 10);
                if (hlsRef.current) {
                  hlsRef.current.currentLevel = level;
                  setCurrentLevel(level);
                }
              }}
              style={s.qualitySelect}
            >
              <option value="-1">Auto</option>
              {availableLevels.map((level) => (
                <option key={level.index} value={level.index}>
                  {level.name}
                </option>
              ))}
            </select>
          </div>
        )}

        <div style={s.divider} />

        <div style={s.descriptionWrap}>
          <h3 style={s.descLabel}>Description</h3>
          <p style={s.description}>
            {video.description || <span style={{ color: "#555" }}>No description provided.</span>}
          </p>
        </div>
      </div>
    </div>
  );
}

const s = {
  page: { maxWidth: "900px", margin: "0 auto", fontFamily: "'Segoe UI', sans-serif" },
  playerWrap: {
    width: "100%",
    borderRadius: "12px",
    overflow: "hidden",
    background: "#000",
    boxShadow: "0 4px 40px rgba(0,0,0,0.7)",
    aspectRatio: "16 / 9",
  },
  video: { width: "100%", height: "100%", display: "block", background: "#000" },
  infoWrap: {
    marginTop: "20px",
    background: "#1a1a1a",
    borderRadius: "12px",
    padding: "22px 24px",
    border: "1px solid #282828",
  },
  title: { margin: "0 0 14px", fontSize: "20px", fontWeight: "700", color: "#f0f0f0", lineHeight: "1.3" },
  metaRow: { display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: "12px" },
  metaLeft: { display: "flex", alignItems: "center", gap: "12px", flexWrap: "wrap" },
  categoryBadge: {
    fontSize: "12px",
    color: "#e50914",
    background: "rgba(229,9,20,0.12)",
    border: "1px solid rgba(229,9,20,0.25)",
    padding: "3px 10px",
    borderRadius: "20px",
    fontWeight: "600",
    letterSpacing: "0.3px",
  },
  metaStat: { display: "flex", alignItems: "center", gap: "5px", fontSize: "13px", color: "#777" },
  metaIcon: { width: "14px", height: "14px", color: "#666", flexShrink: 0 },
  likeBtn: {
    display: "flex",
    alignItems: "center",
    gap: "7px",
    background: "#252525",
    border: "1px solid #383838",
    color: "#aaa",
    padding: "8px 18px",
    borderRadius: "8px",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
    transition: "background 0.2s, color 0.2s, border-color 0.2s",
    fontFamily: "'Segoe UI', sans-serif",
  },
  likeBtnActive: { background: "rgba(229,9,20,0.14)", border: "1px solid rgba(229,9,20,0.4)", color: "#e50914" },
  likeBtnDisabled: { opacity: 0.6, cursor: "not-allowed" },
  likeIcon: { width: "16px", height: "16px", flexShrink: 0 },
  likeCount: { fontSize: "13px", color: "inherit", marginLeft: "2px" },
  divider: { height: "1px", background: "#282828", margin: "18px 0" },
  descriptionWrap: {},
  descLabel: {
    margin: "0 0 8px",
    fontSize: "13px",
    fontWeight: "700",
    letterSpacing: "0.8px",
    textTransform: "uppercase",
    color: "#666",
  },
  description: { margin: 0, fontSize: "14px", color: "#bbb", lineHeight: "1.65", whiteSpace: "pre-wrap" },
  center: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    justifyContent: "center",
    minHeight: "400px",
    gap: "16px",
  },
  spinner: {
    width: "44px",
    height: "44px",
    border: "4px solid #333",
    borderTop: "4px solid #e50914",
    borderRadius: "50%",
    animation: "spin 0.9s linear infinite",
  },
  loadingText: { color: "#666", fontSize: "14px", margin: 0 },
  errorIcon: { fontSize: "40px", marginBottom: "8px" },
  errorText: { color: "#ff5c5c", fontSize: "15px", textAlign: "center", maxWidth: "400px", lineHeight: "1.6" },
  backBtn: {
    background: "#e50914",
    border: "none",
    padding: "10px 24px",
    borderRadius: "8px",
    fontSize: "14px",
    fontWeight: "600",
    color: "#fff",
    cursor: "pointer",
    transition: "background 0.2s",
    marginTop: "8px",
  },
  // Quality selector styles
  qualitySelector: {
    marginTop: "16px",
    display: "flex",
    alignItems: "center",
    gap: "8px",
  },
  qualityLabel: {
    fontSize: "13px",
    color: "#aaa",
  },
  qualitySelect: {
    background: "#252525",
    color: "#fff",
    border: "1px solid #383838",
    borderRadius: "4px",
    padding: "4px 8px",
    fontSize: "13px",
    cursor: "pointer",
    outline: "none",
  },
};