import { useEffect, useState } from "react";

import { api, type RiskModel } from "../api";
import { Card, ErrorMessage } from "../components";

const METRIC_LABELS: Record<string, string> = {
  rocAuc: "ROC AUC",
  averagePrecision: "Average precision",
  precisionAtMedium: "Precision at MEDIUM",
  recallAtMedium: "Recall at MEDIUM",
  precisionAtHigh: "Precision at HIGH",
  recallAtHigh: "Recall at HIGH",
};

export function ModelPage() {
  const [model, setModel] = useState<RiskModel | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    api.model().then(setModel).catch(setError);
  }, []);

  return (
    <>
      <h1>Risk model</h1>
      <ErrorMessage error={error} />
      {model && (
        <>
          {model.syntheticTrainingData && (
            <p className="notice">
              Trained on synthetic data: the model reflects documented assumptions about account takeover, not
              measured real-world behaviour.
            </p>
          )}
          <div className="columns">
            <Card title={`Version ${model.version}`}>
              <dl>
                <dt>Algorithm</dt>
                <dd>{model.algorithm}</dd>
                <dt>MEDIUM from</dt>
                <dd>{Math.round(model.thresholds.medium * 100)}%</dd>
                <dt>HIGH from</dt>
                <dd>{Math.round(model.thresholds.high * 100)}%</dd>
                {Object.entries(model.metrics).flatMap(([name, value]) => [
                  <dt key={`${name}-label`}>{METRIC_LABELS[name] ?? name}</dt>,
                  <dd key={`${name}-value`}>{value.toFixed(3)}</dd>,
                ])}
              </dl>
            </Card>
            <Card title="Signals and weights">
              <table>
                <thead>
                  <tr>
                    <th>Signal</th>
                    <th>Weight</th>
                  </tr>
                </thead>
                <tbody>
                  {model.features.map((feature) => (
                    <tr key={feature.name}>
                      <td>{feature.label}</td>
                      <td className="mono">{feature.coefficient.toFixed(3)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </Card>
          </div>
        </>
      )}
    </>
  );
}
