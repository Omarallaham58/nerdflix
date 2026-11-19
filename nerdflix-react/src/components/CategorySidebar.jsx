import { useDispatch, useSelector } from "react-redux";
import { isSubscribedToCategory } from "../redux/selectors/userSelectors";
import { addSubscription, removeSubscription } from "../redux/slices/userSlice";
import { subscribe, unsubscribe } from "../api/subscriptionApi";
import { useState, useEffect } from "react";
import SignInPromptModal from "./SignInPromptModal";
import { fetchCategories } from "../redux/slices/categorySlice";

function CategoryItem({ cat }) {
  const dispatch = useDispatch();
  const isAuthenticated = useSelector((state) => state.user.isAuthenticated);
  const subscribed = useSelector((state) =>
    isSubscribedToCategory(state, cat.id),
  );
  const [showSignInModal, setShowSignInModal] = useState(false);

  return (
    <div style={styles.catItem}>
      <div style={styles.catRow}>
        <span style={styles.catDot(subscribed)} />
        <span style={styles.catName}>{cat.name}</span>
      </div>
      {isAuthenticated &&
        (subscribed ? (
          <button
            style={{ ...styles.btn, ...styles.unsubBtn }}
            onClick={async () => {
              try {
                await unsubscribe(cat.id);
                dispatch(removeSubscription(cat.id));
              } catch (err) {
                console.error("Unsubscription failed", err);
              }
            }}
            onMouseEnter={(e) => (e.currentTarget.style.background = "#555")}
            onMouseLeave={(e) => (e.currentTarget.style.background = "#2e2e2e")}
          >
            Unsubscribe
          </button>
        ) : (
          <button
            style={{ ...styles.btn, ...styles.subBtn }}
            onClick={async () => {
              try {
                await subscribe(cat.id);
                dispatch(addSubscription(cat.id));
              } catch (err) {
                console.error("Subscription failed", err);
              }
            }}
            onMouseEnter={(e) => (e.currentTarget.style.background = "#f6121d")}
            onMouseLeave={(e) => (e.currentTarget.style.background = "#e50914")}
          >
            Subscribe
          </button>
        ))}
      <SignInPromptModal
        show={showSignInModal}
        onCancel={() => setShowSignInModal(false)}
      />
    </div>
  );
}

export default function CategorySidebar() {
  const dispatch = useDispatch();
  const categories = useSelector((s) => s.categories.data);
  const categoriesLoading = useSelector((s) => s.categories.loading);

  // Fetch categories if they are not already loaded
  useEffect(() => {
    if (categories.length === 0 && !categoriesLoading) {
      dispatch(fetchCategories());
    }
  }, [categories.length, categoriesLoading, dispatch]);

  return (
    <aside style={styles.sidebar}>
      <div style={styles.sidebarHeader}>
        <span style={styles.sidebarIcon}>≡</span>
        <span style={styles.sidebarTitle}>Categories</span>
      </div>
      <div style={styles.list}>
        {categories.map((cat) => (
          <CategoryItem key={cat.id} cat={cat} />
        ))}
        {/* Optional: show a simple loading indicator while fetching */}
        {categoriesLoading && <div style={styles.loading}>Loading...</div>}
      </div>
    </aside>
  );
}

const styles = {
  sidebar: {
    width: "210px",
    minWidth: "210px",
    background: "#141414",
    borderRight: "1px solid #1f1f1f",
    minHeight: "calc(100vh - 60px)",
    padding: "20px 0",
  },
  sidebarHeader: {
    display: "flex",
    alignItems: "center",
    gap: "8px",
    padding: "0 16px 16px",
    borderBottom: "1px solid #222",
    marginBottom: "10px",
  },
  sidebarIcon: {
    color: "#e50914",
    fontSize: "18px",
    lineHeight: 1,
  },
  sidebarTitle: {
    fontFamily: "'Segoe UI', sans-serif",
    fontWeight: "700",
    fontSize: "13px",
    letterSpacing: "1.5px",
    textTransform: "uppercase",
    color: "#aaa",
  },
  list: {
    display: "flex",
    flexDirection: "column",
    gap: "4px",
    padding: "0 10px",
  },
  catItem: {
    padding: "10px 8px",
    borderRadius: "8px",
    background: "#1a1a1a",
    display: "flex",
    flexDirection: "column",
    gap: "8px",
    transition: "background 0.2s",
  },
  catRow: {
    display: "flex",
    alignItems: "center",
    gap: "8px",
  },
  catDot: (subscribed) => ({
    width: "7px",
    height: "7px",
    borderRadius: "50%",
    background: subscribed ? "#e50914" : "#444",
    flexShrink: 0,
    transition: "background 0.3s",
  }),
  catName: {
    fontSize: "14px",
    color: "#ddd",
    fontFamily: "'Segoe UI', sans-serif",
    fontWeight: "500",
  },
  btn: {
    border: "none",
    borderRadius: "5px",
    padding: "5px 10px",
    fontSize: "12px",
    fontWeight: "600",
    cursor: "pointer",
    width: "100%",
    transition: "background 0.2s",
    letterSpacing: "0.3px",
  },
  subBtn: {
    background: "#e50914",
    color: "#fff",
  },
  unsubBtn: {
    background: "#2e2e2e",
    color: "#bbb",
  },
  loading: {
    color: "#666",
    fontSize: "12px",
    textAlign: "center",
    padding: "10px",
  },
};