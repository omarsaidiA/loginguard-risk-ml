import { BrowserRouter, Navigate, NavLink, Outlet, Route, Routes } from "react-router-dom";

import { AuthProvider, useAuth } from "./auth";
import { DashboardPage } from "./pages/DashboardPage";
import { EventDetailPage } from "./pages/EventDetailPage";
import { EventsPage } from "./pages/EventsPage";
import { LoginPage } from "./pages/LoginPage";
import { ModelPage } from "./pages/ModelPage";
import { OperatorsPage } from "./pages/OperatorsPage";
import { UsersPage } from "./pages/UsersPage";

/**
 * Hiding a page is only a convenience: every authorization decision is made by the API, which
 * answers 401/403 whatever the browser shows.
 */
function RequireAuth({ adminOnly = false }: { adminOnly?: boolean }) {
  const { me, loading } = useAuth();
  if (loading) {
    return <p className="muted center">Loading...</p>;
  }
  if (!me) {
    return <Navigate to="/login" replace />;
  }
  if (adminOnly && me.role !== "ADMIN") {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}

function Layout() {
  const { me, logout } = useAuth();
  return (
    <div className="layout">
      <header>
        <span className="brand">LoginGuard</span>
        <nav>
          <NavLink to="/" end>
            Dashboard
          </NavLink>
          <NavLink to="/events">Events</NavLink>
          <NavLink to="/users">Users</NavLink>
          <NavLink to="/model">Model</NavLink>
          {me?.role === "ADMIN" && <NavLink to="/operators">Operators</NavLink>}
        </nav>
        <span className="who">
          {me?.username} ({me?.role.toLowerCase()})
          <button className="link-button" onClick={logout}>
            Sign out
          </button>
        </span>
      </header>
      <main>
        <Outlet />
      </main>
    </div>
  );
}

export function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<RequireAuth />}>
            <Route element={<Layout />}>
              <Route path="/" element={<DashboardPage />} />
              <Route path="/events" element={<EventsPage />} />
              <Route path="/events/:id" element={<EventDetailPage />} />
              <Route path="/users" element={<UsersPage />} />
              <Route path="/model" element={<ModelPage />} />
              <Route element={<RequireAuth adminOnly />}>
                <Route path="/operators" element={<OperatorsPage />} />
              </Route>
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
