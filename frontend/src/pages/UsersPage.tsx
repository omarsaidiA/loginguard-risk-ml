import { type FormEvent, useCallback, useEffect, useState } from "react";

import { api, type MonitoredUser } from "../api";
import { useAuth } from "../auth";
import { Card, ErrorMessage, formatDate } from "../components";

const EMPTY = { username: "", email: "", department: "" };

export function UsersPage() {
  const { me } = useAuth();
  const isAdmin = me?.role === "ADMIN";
  const [users, setUsers] = useState<MonitoredUser[]>([]);
  const [form, setForm] = useState(EMPTY);
  const [editing, setEditing] = useState<number | null>(null);
  const [error, setError] = useState<unknown>(null);

  const load = useCallback(() => {
    api
      .users()
      .then((page) => setUsers(page.content))
      .catch(setError);
  }, []);

  useEffect(load, [load]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      if (editing === null) {
        await api.createUser(form);
      } else {
        await api.updateUser(editing, form);
      }
      setForm(EMPTY);
      setEditing(null);
      load();
    } catch (caught) {
      setError(caught);
    }
  }

  async function remove(user: MonitoredUser) {
    if (!window.confirm(`Delete ${user.username} and all of their login events?`)) {
      return;
    }
    try {
      await api.deleteUser(user.id);
      load();
    } catch (caught) {
      setError(caught);
    }
  }

  return (
    <>
      <h1>Monitored users</h1>
      {isAdmin && (
        <Card title={editing === null ? "Add a user" : "Edit user"}>
          <form className="grid-form" onSubmit={submit}>
            <label>
              Username
              <input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} required />
            </label>
            <label>
              Email
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                required
              />
            </label>
            <label>
              Department
              <input value={form.department} onChange={(e) => setForm({ ...form, department: e.target.value })} />
            </label>
            <button type="submit">{editing === null ? "Add" : "Save"}</button>
            {editing !== null && (
              <button
                type="button"
                className="link-button"
                onClick={() => {
                  setEditing(null);
                  setForm(EMPTY);
                }}
              >
                Cancel
              </button>
            )}
          </form>
        </Card>
      )}
      <ErrorMessage error={error} />
      <Card title="Users">
        <table>
          <thead>
            <tr>
              <th>Username</th>
              <th>Email</th>
              <th>Department</th>
              <th>Created</th>
              {isAdmin && <th />}
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>{user.username}</td>
                <td>{user.email}</td>
                <td>{user.department ?? "-"}</td>
                <td>{formatDate(user.createdAt)}</td>
                {isAdmin && (
                  <td className="row-actions">
                    <button
                      className="link-button"
                      onClick={() => {
                        setEditing(user.id);
                        setForm({ username: user.username, email: user.email, department: user.department ?? "" });
                      }}
                    >
                      Edit
                    </button>
                    <button className="link-button" onClick={() => remove(user)}>
                      Delete
                    </button>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </>
  );
}
