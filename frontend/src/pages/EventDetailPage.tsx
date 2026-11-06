import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import { api, type LoginEvent } from "../api";
import { useAuth } from "../auth";
import { Card, ErrorMessage, formatDate, RiskBadge, RiskFactors, StatusBadge } from "../components";

export function EventDetailPage() {
  const { id } = useParams();
  const { me } = useAuth();
  const navigate = useNavigate();
  const [event, setEvent] = useState<LoginEvent | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    api.event(Number(id)).then(setEvent).catch(setError);
  }, [id]);

  async function review(status: "CONFIRMED_FRAUD" | "LEGITIMATE") {
    setError(null);
    try {
      setEvent(await api.reviewEvent(Number(id), status));
    } catch (caught) {
      setError(caught);
    }
  }

  async function remove() {
    if (!window.confirm("Delete this event? This destroys evidence and cannot be undone.")) {
      return;
    }
    try {
      await api.deleteEvent(Number(id));
      navigate("/events");
    } catch (caught) {
      setError(caught);
    }
  }

  return (
    <>
      <h1>Login event #{id}</h1>
      <ErrorMessage error={error} />
      {event && (
        <div className="columns">
          <Card title="Details">
            <dl>
              <dt>User</dt>
              <dd>{event.username}</dd>
              <dt>When</dt>
              <dd>{formatDate(event.occurredAt)}</dd>
              <dt>IP address</dt>
              <dd className="mono">{event.ipAddress}</dd>
              <dt>Country</dt>
              <dd>{event.countryCode}</dd>
              <dt>Device</dt>
              <dd className="mono">{event.deviceId}</dd>
              <dt>Result</dt>
              <dd>{event.successful ? "Success" : "Failure"}</dd>
              <dt>Review</dt>
              <dd>
                <StatusBadge status={event.reviewStatus} />
                {event.reviewedBy && ` by ${event.reviewedBy}, ${formatDate(event.reviewedAt)}`}
              </dd>
            </dl>
            <div className="actions">
              <button className="danger" onClick={() => review("CONFIRMED_FRAUD")}>
                Confirm fraud
              </button>
              <button onClick={() => review("LEGITIMATE")}>Mark legitimate</button>
              {me?.role === "ADMIN" && (
                <button className="link-button" onClick={remove}>
                  Delete
                </button>
              )}
            </div>
          </Card>
          <Card title="Why this score">
            {event.risk ? (
              <>
                <p>
                  <RiskBadge level={event.risk.level} score={event.risk.score} />{" "}
                  <span className="muted">model {event.risk.modelVersion}</span>
                </p>
                {event.risk.factors.length > 0 ? (
                  <>
                    <p className="muted">Contribution of each signal to the score (log-odds).</p>
                    <RiskFactors factors={event.risk.factors} />
                  </>
                ) : (
                  <p className="muted">Scored by another model version: no explanation available.</p>
                )}
              </>
            ) : (
              <p className="muted">This event was recorded before risk scoring existed.</p>
            )}
          </Card>
        </div>
      )}
    </>
  );
}
