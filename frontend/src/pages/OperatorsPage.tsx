import { type FormEvent, useCallback, useEffect, useState } from "react";

import { api, type Operator, type Role } from "../api";
import { useAuth } from "../auth";
import { Card, ErrorMessage, formatDate } from "../components";

export function OperatorsPage() {
  const { me } = useAuth();
  const [operators, setOperators] = useState<Operator[]>([]);
  const [form, setForm] = useState<{ username: string; password: string; role: Role }>({
    username: "",
    password: "",
    role: "ANALYST",
  });
  const [error, setError] = useState<unknown>(null);

  const load = useCallback(() => {
    api.operators().then(setOperators).catch(setError);
  }, []);

  useEffect(load, [load]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      await api.createOperator(form);
      setForm({ username: "", password: "", role: "ANALYST" });
      load();
    } catch (caught) {
      setError(caught);
    }
  }

  async function remove(operator: Operator) {
    if (!window.confirm(`Delete operator ${operator.username}?`)) {
      return;
    }
    try {
      await api.deleteOperator(operator.id);
      load();
    } catch (caught) {
      setError(caught);
    }
  }

  return (
    <>
      <h1>Operators</h1>
      <Card title="Add an operator">
        <form className="grid-form" onSubmit={submit}>
          <label>
            Username
            <input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} required />
          </label>
          <label>
            Password (12 to 72 characters)
            <input
              type="password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              autoComplete="new-password"
              minLength={12}
              maxLength={72}
              required
            />
          </label>
          <label>
            Role
            <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value as Role })}>
              <option value="ANALYST">Analyst</option>
              <option value="ADMIN">Admin</option>
            </select>
          </label>
          <button type="submit">Add</button>
        </form>
      </Card>
      <ErrorMessage error={error} />
      <Card title="Accounts">
        <table>
          <thead>
            <tr>
              <th>Username</th>
              <th>Role</th>
              <th>Locked until</th>
              <th>Created</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {operators.map((operator) => (
              <tr key={operator.id}>
                <td>{operator.username}</td>
                <td>{operator.role}</td>
                <td>{formatDate(operator.lockedUntil)}</td>
                <td>{formatDate(operator.createdAt)}</td>
                <td className="row-actions">
                  {operator.username !== me?.username && (
                    <button className="link-button" onClick={() => remove(operator)}>
                      Delete
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </>
  );
}
