import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { api, type LoginEvent, type Stats } from "../api";
import { Card, ErrorMessage, formatDate, RiskBadge } from "../components";

export function DashboardPage() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [queue, setQueue] = useState<LoginEvent[]>([]);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    Promise.all([api.stats(), api.events({ level: "HIGH", status: "PENDING" })])
      .then(([loadedStats, highRisk]) => {
        setStats(loadedStats);
        setQueue(highRisk.content);
      })
      .catch(setError);
  }, []);

  return (
    <>
      <h1>Dashboard</h1>
      <ErrorMessage error={error} />
      {stats && (
        <div className="tiles">
          <div className="tile">
            <span className="tile-value">{stats.total}</span>
            <span className="tile-label">Login events</span>
          </div>
          <div className="tile tile-alert">
            <span className="tile-value">{stats.pendingHighRisk}</span>
            <span className="tile-label">High risk, awaiting review</span>
          </div>
          {(["HIGH", "MEDIUM", "LOW"] as const).map((level) => (
            <div className="tile" key={level}>
              <span className="tile-value">{stats.byRiskLevel[level]}</span>
              <span className="tile-label">
                <RiskBadge level={level} />
              </span>
            </div>
          ))}
          <div className="tile">
            <span className="tile-value">{stats.byReviewStatus.CONFIRMED_FRAUD}</span>
            <span className="tile-label">Confirmed fraud</span>
          </div>
        </div>
      )}
      <Card title="Review queue: high risk, pending">
        {queue.length === 0 ? (
          <p className="muted">Nothing to review.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>When</th>
                <th>User</th>
                <th>Country</th>
                <th>Risk</th>
              </tr>
            </thead>
            <tbody>
              {queue.map((event) => (
                <tr key={event.id}>
                  <td>
                    <Link to={`/events/${event.id}`}>{formatDate(event.occurredAt)}</Link>
                  </td>
                  <td>{event.username}</td>
                  <td>{event.countryCode}</td>
                  <td>
                    <RiskBadge level={event.risk?.level} score={event.risk?.score} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </>
  );
}
