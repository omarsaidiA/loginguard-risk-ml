import type { ReactNode } from "react";

import { ApiError, type ReviewStatus, type RiskFactor, type RiskLevel } from "./api";

export function RiskBadge({ level, score }: { level: RiskLevel | undefined; score?: number }) {
  if (!level) {
    return <span className="badge badge-none">not scored</span>;
  }
  return (
    <span className={`badge badge-${level.toLowerCase()}`}>
      {level}
      {score !== undefined && ` ${Math.round(score * 100)}%`}
    </span>
  );
}

const STATUS_LABELS: Record<ReviewStatus, string> = {
  PENDING: "Pending",
  CONFIRMED_FRAUD: "Confirmed fraud",
  LEGITIMATE: "Legitimate",
};

export function StatusBadge({ status }: { status: ReviewStatus }) {
  return <span className={`status status-${status.toLowerCase()}`}>{STATUS_LABELS[status]}</span>;
}

/**
 * Horizontal bars for each feature's contribution to the score (log-odds): red pushes towards
 * "risky", green towards "normal". Widths are set through the style property, which a CSP
 * without 'unsafe-inline' still allows.
 */
export function RiskFactors({ factors }: { factors: RiskFactor[] }) {
  const largest = Math.max(...factors.map((factor) => Math.abs(factor.value)), 0.01);
  return (
    <ul className="factors">
      {factors.map((factor) => (
        <li key={factor.feature}>
          <span className="factor-label">{factor.label}</span>
          <span className="factor-track">
            <span
              className={factor.value >= 0 ? "factor-bar risk" : "factor-bar safe"}
              style={{ width: `${(Math.abs(factor.value) / largest) * 100}%` }}
            />
          </span>
          <span className="factor-value">
            {factor.value >= 0 ? "+" : ""}
            {factor.value.toFixed(2)}
          </span>
        </li>
      ))}
    </ul>
  );
}

/** Problem details from the API, with the invalid fields when there are some. */
export function ErrorMessage({ error }: { error: unknown }) {
  if (!error) {
    return null;
  }
  const apiError = error instanceof ApiError ? error : null;
  return (
    <div className="error" role="alert">
      <p>{apiError ? apiError.message : "Something went wrong"}</p>
      {apiError && apiError.fieldErrors.length > 0 && (
        <ul>
          {apiError.fieldErrors.map((fieldError) => (
            <li key={fieldError.field}>
              <strong>{fieldError.field}</strong> {fieldError.message}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export function Card({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="card">
      <h2>{title}</h2>
      {children}
    </section>
  );
}

export function formatDate(value: string | null): string {
  return value ? new Date(value).toLocaleString() : "-";
}
