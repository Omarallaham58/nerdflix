import { Navigate } from "react-router-dom";
import { useSelector } from "react-redux";

export default function ProtectedRoute({ children }) {
    const token = localStorage.getItem("token");
    const username = useSelector(state => state.user.username);

    if (!token || !username) {
        return <Navigate to="/auth" replace />;
    }

    return children;
}