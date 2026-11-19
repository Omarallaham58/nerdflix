import { useSelector } from "react-redux";
import { Navigate } from "react-router-dom";

export default function withAuth(WrappedComponent) {
    return function AuthenticatedComponent(props) {
        const token = localStorage.getItem("token");
        const username = useSelector(state => state.user.username);

        if (!token || !username) {
            return <Navigate to="/auth" replace />;
        }

        return <WrappedComponent {...props} />;
    };
}