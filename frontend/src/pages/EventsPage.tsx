import { type FormEvent, useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { api, type LoginEvent, type MonitoredUser, type Page, type ReviewStatus, type RiskLevel } from "../api";
import { Card, ErrorMessage, formatDate, RiskBadge, StatusBadge } from "../components";

export function EventsPage() {
  const [level, setLevel] = useState<RiskLevel | "">("");
  const [status, setStatus] = useState<ReviewStatus | "">("");
  const [pageNumber, setPageNumber] = useState(0);
  const [page, setPage] = useState<Page<LoginEvent> | null>(null);
  const [error, setError] = useState<unknown>(null);

  const load = useCallback(() => {
    api.events({ level, status, page: pageNumber }).then(setPage).catch(setError);
  }, [level, status, pageNumber]);

  useEffect(load, [load]);

  return (
    <>
      <h1>Login events</h1>
      <RecordEventForm onRecorded={load} />
      <Card title="History">
        <div className="filters">
          <label>
            Risk
            <select
              value={level}
              onChange={(e) => {
                setLevel(e.target.value as RiskLevel | "");
                setPageNumber(0);
              }}
            >
              <option value="">All</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>
          </label>
          <label>
            Review
            <select
              value={status}
              onChange={(e) => {
                setStatus(e.target.value as ReviewStatus | "");
                setPageNumber(0);
              }}
            >
              <option value="">All</option>
              <option value="PENDING">Pending</option>
              <option value="CONFIRMED_FRAUD">Confirmed fraud</option>
              <option value="LEGITIMATE">Legitimate</option>
            </select>
          </label>
        </div>
        <ErrorMessage error={error} />
        {page && (
          <>
            <table>
              <thead>
                <tr>
                  <th>When</th>
                  <th>User</th>
                  <th>IP</th>
                  <th>Country</th>
                  <th>Device</th>
                  <th>Result</th>
                  <th>Risk</th>
                  <th>Review</th>
                </tr>
              </thead>
              <tbody>
                {page.content.map((event) => (
                  <tr key={event.id}>
                    <td>
                      <Link to={`/events/${event.id}`}>{formatDate(event.occurredAt)}</Link>
                    </td>
                    <td>{event.username}</td>
                    <td className="mono">{event.ipAddress}</td>
                    <td>{event.countryCode}</td>
                    <td className="mono">{event.deviceId}</td>
                    <td>{event.successful ? "Success" : "Failure"}</td>
                    <td>
                      <RiskBadge level={event.risk?.level} score={event.risk?.score} />
                    </td>
                    <td>
                      <StatusBadge status={event.reviewStatus} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="pager">
              <button disabled={pageNumber === 0} onClick={() => setPageNumber(pageNumber - 1)}>
                Previous
              </button>
              <span>
                Page {page.page.number + 1} of {Math.max(page.page.totalPages, 1)} ({page.page.totalElements} events)
              </span>
              <button
                disabled={pageNumber + 1 >= page.page.totalPages}
                onClick={() => setPageNumber(pageNumber + 1)}
              >
                Next
              </button>
            </div>
          </>
        )}
      </Card>
    </>
  );
}

function RecordEventForm({ onRecorded }: { onRecorded: () => void }) {
  const [users, setUsers] = useState<MonitoredUser[]>([]);
  const [form, setForm] = useState({
    userId: "",
    occurredAt: new Date().toISOString().slice(0, 16),
    ipAddress: "",
    countryCode: "",
    deviceId: "",
    successful: true,
  });
  const [result, setResult] = useState<LoginEvent | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    api
      .users()
      .then((page) => setUsers(page.content))
      .catch(setError);
  }, []);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      const recorded = await api.recordEvent({
        userId: Number(form.userId),
        // The datetime input has no time zone: it is entered and sent as UTC
        occurredAt: new Date(`${form.occurredAt}:00Z`).toISOString(),
        ipAddress: form.ipAddress.trim(),
        countryCode: form.countryCode.trim().toUpperCase(),
        deviceId: form.deviceId.trim(),
        successful: form.successful,
      });
      setResult(recorded);
      onRecorded();
    } catch (caught) {
      setError(caught);
    }
  }

  return (
    <Card title="Record a login">
      <form className="grid-form" onSubmit={submit}>
        <label>
          User
          <select value={form.userId} onChange={(e) => setForm({ ...form, userId: e.target.value })} required>
            <option value="">Choose...</option>
            {users.map((user) => (
              <option key={user.id} value={user.id}>
                {user.username}
              </option>
            ))}
          </select>
        </label>
        <label>
          Date and time (UTC)
          <input
            type="datetime-local"
            value={form.occurredAt}
            onChange={(e) => setForm({ ...form, occurredAt: e.target.value })}
            required
          />
        </label>
        <label>
          IP address
          <input value={form.ipAddress} onChange={(e) => setForm({ ...form, ipAddress: e.target.value })} required />
        </label>
        <label>
          Country (ISO code)
          <input
            value={form.countryCode}
            onChange={(e) => setForm({ ...form, countryCode: e.target.value })}
            maxLength={2}
            placeholder="FR"
            required
          />
        </label>
        <label>
          Device id
          <input
            value={form.deviceId}
            onChange={(e) => setForm({ ...form, deviceId: e.target.value })}
            maxLength={64}
            required
          />
        </label>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={form.successful}
            onChange={(e) => setForm({ ...form, successful: e.target.checked })}
          />
          Successful login
        </label>
        <button type="submit">Record and score</button>
      </form>
      <ErrorMessage error={error} />
      {result && (
        <p className="result">
          Scored <RiskBadge level={result.risk?.level} score={result.risk?.score} />{" "}
          <Link to={`/events/${result.id}`}>See why</Link>
        </p>
      )}
    </Card>
  );
}
