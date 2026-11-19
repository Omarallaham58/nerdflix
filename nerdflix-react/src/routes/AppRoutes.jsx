import { BrowserRouter, Routes, Route } from "react-router-dom";
import HomePage from "../pages/HomePage";
import VideoPlayerPage from "../pages/VideoPlayerPage";
import AuthPage from "../pages/AuthPage";
import AccountPage from "../pages/AccountPage";
import withAuth from "../hoc/withAuth";
import Layout from "../pages/Layout";

// Wrap protected pages
const ProtectedVideoPlayer = withAuth(VideoPlayerPage);
const ProtectedAccountPage = withAuth(AccountPage);

export default function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/auth" element={<AuthPage />} />
        <Route element={<Layout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/video/:videoId" element={<ProtectedVideoPlayer />} />
          <Route path="/account" element={<ProtectedAccountPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
