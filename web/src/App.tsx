import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";

type User = { id: string; email: string; platformAdmin: boolean };
type Team = { id: string; name: string; role: "OWNER" | "ADMIN" | "MEMBER" };
type TeamMember = { userId: string; email: string; role: "OWNER" | "ADMIN" | "MEMBER" };
type Project = {
  id: string;
  name: string;
  visibility: "PUBLIC" | "PRIVATE";
  teamId: string | null;
  role: "OWNER" | "ADMIN" | "EDITOR" | "VIEWER" | null;
};
type ProjectMember = { userId: string; email: string; role: "OWNER" | "ADMIN" | "EDITOR" | "VIEWER" };
type Version = {
  id: string;
  projectId: string;
  format: string;
  ontologyIri: string | null;
  axiomCount: number;
  fileName: string;
  createdAt: string;
};
type VersionPage = { versions: Version[]; pageNum: number; pageSize: number; total: number };
type AiStatus = { configured: boolean; provider: string; model: string };
type ChatMessage = { role: "user" | "assistant"; content: string };
type ReasoningValidation = {
  versionId: string;
  engine: string;
  inOwl2DlProfile: boolean;
  profileViolations: string[];
  consistent: boolean | null;
  unsatisfiableClassIris: string[];
  classExplanations: { classIri: string; axioms: string[] }[];
  inconsistencyExplanationStatus: "NOT_APPLICABLE" | "GENERATED" | "AXIOM_LIMIT_EXCEEDED";
  inconsistencyExplanationAxioms: string[];
  axiomCount: number;
  elapsedMillis: number;
};
type ClassHierarchy = {
  classIri: string;
  consistent: boolean;
  direct: boolean;
  superClassIris: string[];
  subClassIris: string[];
  elapsedMillis: number;
};
type IndividualClassification = {
  individualIri: string;
  consistent: boolean;
  allTypeIris: string[];
  inferredTypeIris: string[];
  elapsedMillis: number;
};
type RuleApplication = {
  individualIri: string;
  consistent: boolean;
  inferredTypeIris: string[];
  elapsedMillis: number;
};

let csrfToken = "";

async function getCsrfToken() {
  if (!csrfToken) {
    const response = await fetch("/api/auth/csrf", { credentials: "same-origin" });
    if (!response.ok) throw new Error("Could not start a secure session.");
    csrfToken = (await response.json() as { token: string }).token;
  }
  return csrfToken;
}

async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? "GET").toUpperCase();
  const headers = new Headers(init.headers);
  if (init.body && !(init.body instanceof FormData)) headers.set("Content-Type", "application/json");
  if (!["GET", "HEAD", "OPTIONS"].includes(method)) headers.set("X-XSRF-TOKEN", await getCsrfToken());
  headers.set("Accept", "application/json");
  const response = await fetch(path, { ...init, method, headers, credentials: "same-origin" });
  if (!response.ok) {
    let message = `Request failed (${response.status}).`;
    try {
      const body = await response.json() as { message?: string; error?: string };
      message = body.message ?? body.error ?? message;
    } catch {
      // Use the HTTP status when the server did not return JSON.
    }
    throw new Error(message);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

function jsonBody(value: unknown): string {
  return JSON.stringify(value);
}

function App() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [teams, setTeams] = useState<Team[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedId, setSelectedId] = useState("");
  const [versions, setVersions] = useState<Version[]>([]);
  const [restoreBusyId, setRestoreBusyId] = useState("");
  const [members, setMembers] = useState<ProjectMember[]>([]);
  const [teamMembers, setTeamMembers] = useState<TeamMember[]>([]);
  const [aiStatus, setAiStatus] = useState<AiStatus | null>(null);
  const [chat, setChat] = useState<ChatMessage[]>([]);
  const [chatBusy, setChatBusy] = useState(false);
  const [chatOpen, setChatOpen] = useState(true);
  const [invitationToken, setInvitationToken] = useState("");
  const [reasoningVersionId, setReasoningVersionId] = useState("");
  const [reasoningValidation, setReasoningValidation] = useState<ReasoningValidation | null>(null);
  const [classHierarchy, setClassHierarchy] = useState<ClassHierarchy | null>(null);
  const [reasoningBusy, setReasoningBusy] = useState(false);
  const [reasoningTab, setReasoningTab] = useState<"reasoner" | "rules">("reasoner");
  const [reasonerEngine, setReasonerEngine] = useState("HERMIT");
  const [individualClassification, setIndividualClassification] = useState<IndividualClassification | null>(null);
  const [ruleText, setRuleText] = useState("");
  const [ruleApplication, setRuleApplication] = useState<RuleApplication | null>(null);

  const selectedProject = useMemo(
    () => projects.find((project) => project.id === selectedId) ?? null,
    [projects, selectedId],
  );
  const activeTeam = teams.find((team) => team.id === selectedProject?.teamId);
  const canManageProject = selectedProject?.role === "OWNER" || selectedProject?.role === "ADMIN";
  const canImport = canManageProject || selectedProject?.role === "EDITOR";

  const refreshWorkspace = useCallback(async () => {
    const [teamList, projectList] = await Promise.all([
      api<Team[]>("/api/teams"),
      api<Project[]>("/api/projects"),
    ]);
    setTeams(teamList);
    setProjects(projectList);
    setSelectedId((previous) => projectList.some((project) => project.id === previous)
      ? previous
      : projectList[0]?.id ?? "");
  }, []);

  const refreshProject = useCallback(async (project: Project) => {
    const page = await api<VersionPage>(
      `/api/projects/${project.id}/ontologies/versions?pageNum=1&pageSize=50`,
    );
    setVersions(page.versions);
    if (project.role === "OWNER" || project.role === "ADMIN") {
      const projectMembers = await api<ProjectMember[]>(`/api/projects/${project.id}/members`);
      setMembers(projectMembers);
    } else {
      setMembers([]);
    }
  }, []);

  useEffect(() => {
    let mounted = true;
    void getCsrfToken()
      .then(() => api<User>("/api/auth/me"))
      .then(async (currentUser) => {
        if (!mounted) return;
        setUser(currentUser);
        const status = await api<AiStatus>("/api/ai/status");
        if (mounted) setAiStatus(status);
        await refreshWorkspace();
      })
      .catch(() => {
        if (mounted) setUser(null);
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });
    return () => { mounted = false; };
  }, [refreshWorkspace]);

  useEffect(() => {
    if (!selectedProject) {
      setVersions([]);
      setMembers([]);
      setTeamMembers([]);
      setReasoningVersionId("");
      setReasoningValidation(null);
      setClassHierarchy(null);
      setIndividualClassification(null);
      setRuleApplication(null);
      return;
    }
    void refreshProject(selectedProject).catch((reason: Error) => setError(reason.message));
    if (activeTeam && (activeTeam.role === "OWNER" || activeTeam.role === "ADMIN")) {
      void api<TeamMember[]>(`/api/teams/${activeTeam.id}/members`)
        .then(setTeamMembers)
        .catch((reason: Error) => setError(reason.message));
    } else {
      setTeamMembers([]);
    }
  }, [activeTeam, refreshProject, selectedProject]);

  useEffect(() => {
    function handleReasonerShortcut(event: KeyboardEvent) {
      const target = event.target;
      const isEditing = target instanceof HTMLElement
        && (target.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(target.tagName));
      if (isEditing || !selectedProject || !(event.ctrlKey || event.metaKey)
          || event.altKey || event.key.toLowerCase() !== "r" || versions.length === 0) {
        return;
      }
      event.preventDefault();
      const version = versions.find((candidate) => candidate.id === reasoningVersionId) ?? versions[0];
      void validateVersion(version);
    }
    window.addEventListener("keydown", handleReasonerShortcut);
    return () => window.removeEventListener("keydown", handleReasonerShortcut);
  }, [reasonerEngine, reasoningVersionId, selectedProject, versions]);

  function reportError(reason: unknown) {
    setError(reason instanceof Error ? reason.message : "The request could not be completed.");
    setNotice("");
  }

  async function signIn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    const form = new FormData(event.currentTarget);
    try {
      await api<void>("/api/auth/login", {
        method: "POST",
        body: jsonBody({ email: form.get("email"), password: form.get("password") }),
      });
      const currentUser = await api<User>("/api/auth/me");
      setUser(currentUser);
      setAiStatus(await api<AiStatus>("/api/ai/status"));
      await refreshWorkspace();
    } catch (reason) {
      reportError(reason);
    }
  }

  async function acceptInvitation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    const form = new FormData(event.currentTarget);
    try {
      await api<void>("/api/auth/invitations/accept", {
        method: "POST",
        body: jsonBody({ token: form.get("token"), password: form.get("password") }),
      });
      setNotice("Invitation accepted. Sign in with your invited email and new password.");
    } catch (reason) {
      reportError(reason);
    }
  }

  async function signOut() {
    try {
      await api<void>("/api/auth/logout", { method: "POST" });
      setUser(null);
      setTeams([]);
      setProjects([]);
      setSelectedId("");
      setChat([]);
      setNotice("You have signed out.");
    } catch (reason) {
      reportError(reason);
    }
  }

  async function createTeam(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    try {
      await api<Team>("/api/teams", { method: "POST", body: jsonBody({ name: form.get("name") }) });
      formElement.reset();
      await refreshWorkspace();
      setNotice("Team created.");
    } catch (reason) {
      reportError(reason);
    }
  }

  async function createProject(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const teamId = String(form.get("teamId") ?? "");
    try {
      const project = await api<Project>("/api/projects", {
        method: "POST",
        body: jsonBody({
          name: form.get("name"),
          visibility: form.get("visibility"),
          teamId: teamId || null,
        }),
      });
      formElement.reset();
      await refreshWorkspace();
      setSelectedId(project.id);
      setNotice("Project created.");
    } catch (reason) {
      reportError(reason);
    }
  }

  async function updateProject(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedProject) return;
    const form = new FormData(event.currentTarget);
    try {
      await api<Project>(`/api/projects/${selectedProject.id}`, {
        method: "PUT",
        body: jsonBody({ name: form.get("name"), visibility: form.get("visibility") }),
      });
      await refreshWorkspace();
      setNotice("Project settings saved.");
    } catch (reason) {
      reportError(reason);
    }
  }

  async function uploadOntology(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedProject) return;
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    try {
      await api<void>(`/api/projects/${selectedProject.id}/ontologies/import`, {
        method: "POST",
        body: form,
      });
      formElement.reset();
      await refreshProject(selectedProject);
      setNotice("Ontology snapshot imported.");
    } catch (reason) {
      reportError(reason);
    }
  }

  async function exportVersion(version: Version, format?: string) {
    if (!selectedProject) return;
    try {
      const query = format ? `?format=${encodeURIComponent(format)}` : "";
      const response = await fetch(
        `/api/projects/${selectedProject.id}/ontologies/versions/${version.id}/export${query}`,
        { credentials: "same-origin" },
      );
      if (!response.ok) throw new Error(`Export failed (${response.status}).`);
      const blob = await response.blob();
      const disposition = response.headers.get("Content-Disposition") ?? "";
      const name = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
      const anchor = document.createElement("a");
      anchor.href = URL.createObjectURL(blob);
      anchor.download = name ? decodeURIComponent(name) : version.fileName;
      anchor.click();
      URL.revokeObjectURL(anchor.href);
    } catch (reason) {
      reportError(reason);
    }
  }

  async function restoreVersion(version: Version) {
    if (!selectedProject || !window.confirm(
      `Restore "${version.fileName}" as a new immutable version? Existing versions will be kept.`,
    )) return;
    setRestoreBusyId(version.id);
    try {
      await api<Version>(
        `/api/projects/${selectedProject.id}/ontologies/versions/${version.id}/restore`,
        { method: "POST", body: jsonBody({}) },
      );
      await refreshProject(selectedProject);
      setNotice(`Restored ${version.fileName} as a new version.`);
    } catch (reason) {
      reportError(reason);
    } finally {
      setRestoreBusyId("");
    }
  }

  async function validateVersion(version: Version) {
    if (!selectedProject) return;
    setReasoningBusy(true);
    setReasoningVersionId(version.id);
    setReasoningValidation(null);
    setClassHierarchy(null);
    try {
      setReasoningValidation(await api<ReasoningValidation>(
        `/api/projects/${selectedProject.id}/ontologies/versions/${version.id}/reasoning/start`,
        { method: "POST", body: jsonBody({ engine: reasonerEngine }) },
      ));
    } catch (reason) {
      reportError(reason);
    } finally {
      setReasoningBusy(false);
    }
  }

  async function queryClassHierarchy(event: FormEvent<HTMLFormElement>, version: Version) {
    event.preventDefault();
    if (!selectedProject) return;
    const form = new FormData(event.currentTarget);
    setReasoningBusy(true);
    try {
      setClassHierarchy(await api<ClassHierarchy>(
        `/api/projects/${selectedProject.id}/ontologies/versions/${version.id}/reasoning/hierarchy`,
        {
          method: "POST",
          body: jsonBody({ classIri: form.get("classIri"), direct: form.get("direct") === "true" }),
        },
      ));
      setReasoningVersionId(version.id);
    } catch (reason) {
      reportError(reason);
    } finally {
      setReasoningBusy(false);
    }
  }

  async function classifyIndividual(event: FormEvent<HTMLFormElement>, version: Version) {
    event.preventDefault();
    if (!selectedProject) return;
    const form = new FormData(event.currentTarget);
    setReasoningBusy(true);
    setRuleApplication(null);
    try {
      setIndividualClassification(await api<IndividualClassification>(
        `/api/projects/${selectedProject.id}/ontologies/versions/${version.id}/reasoning/classify`,
        { method: "POST", body: jsonBody({ individualIri: form.get("individualIri") }) },
      ));
      setReasoningVersionId(version.id);
    } catch (reason) {
      reportError(reason);
    } finally {
      setReasoningBusy(false);
    }
  }

  async function applySwrlRule(event: FormEvent<HTMLFormElement>, version: Version) {
    event.preventDefault();
    if (!selectedProject) return;
    const form = new FormData(event.currentTarget);
    setReasoningBusy(true);
    setIndividualClassification(null);
    try {
      setRuleApplication(await api<RuleApplication>(
        `/api/projects/${selectedProject.id}/ontologies/versions/${version.id}/reasoning/rules/apply`,
        {
          method: "POST",
          body: jsonBody({
            ruleText: form.get("ruleText"),
            individualIri: form.get("individualIri"),
          }),
        },
      ));
      setReasoningVersionId(version.id);
    } catch (reason) {
      reportError(reason);
    } finally {
      setReasoningBusy(false);
    }
  }

  async function loadProjectMembers() {
    if (!selectedProject) return;
    try {
      setMembers(await api<ProjectMember[]>(`/api/projects/${selectedProject.id}/members`));
    } catch (reason) {
      reportError(reason);
    }
  }

  async function addProjectMember(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedProject) return;
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    try {
      await api<void>(`/api/projects/${selectedProject.id}/members`, {
        method: "POST",
        body: jsonBody({ userId: form.get("userId"), role: form.get("role") }),
      });
      formElement.reset();
      await loadProjectMembers();
      setNotice("Project role assigned.");
    } catch (reason) {
      reportError(reason);
    }
  }

  async function loadTeamMembers() {
    if (!activeTeam) return;
    try {
      setTeamMembers(await api<TeamMember[]>(`/api/teams/${activeTeam.id}/members`));
    } catch (reason) {
      reportError(reason);
    }
  }

  async function addTeamMember(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!activeTeam) return;
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    try {
      await api<void>(`/api/teams/${activeTeam.id}/members`, {
        method: "POST",
        body: jsonBody({ userId: form.get("userId"), role: form.get("role") }),
      });
      formElement.reset();
      await loadTeamMembers();
      setNotice("Team role assigned.");
    } catch (reason) {
      reportError(reason);
    }
  }

  async function issueInvitation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    try {
      const result = await api<{ token: string }>("/api/admin/invitations", {
        method: "POST",
        body: jsonBody({ email: form.get("email") }),
      });
      setInvitationToken(result.token);
      setNotice("Invitation issued. Copy and deliver this token through a secure channel.");
    } catch (reason) {
      reportError(reason);
    }
  }

  async function sendChat(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedProject) return;
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const message = String(form.get("message") ?? "").trim();
    if (!message || !aiStatus?.configured) return;
    const priorMessages = chat.slice(-20);
    setChat((previous) => [...previous, { role: "user", content: message }]);
    setChatBusy(true);
    formElement.reset();
    try {
      const path = `/api/ai/projects/${selectedProject.id}/chat`;
      const response = await api<{ answer: string }>(path, {
        method: "POST",
        body: jsonBody({ message, history: priorMessages }),
      });
      setChat((previous) => [...previous, { role: "assistant", content: response.answer }]);
    } catch (reason) {
      reportError(reason);
    } finally {
      setChatBusy(false);
    }
  }

  if (loading) {
    return <main className="loading-screen"><span className="brand-mark">O</span><p>Opening your workspace…</p></main>;
  }

  if (!user) {
    return <AuthScreen onSignIn={signIn} onAcceptInvitation={acceptInvitation} error={error} notice={notice} />;
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="/" aria-label="OpenProtégé home">
          <span className="brand-mark">O</span><span>Open<span className="brand-light">Protégé</span></span>
        </a>
        <div className="topbar-right">
          <span className="connection"><i /> Workspace connected</span>
          <span className="user-email">{user.email}</span>
          <button className="button button-quiet button-small" onClick={() => void signOut()}>Sign out</button>
        </div>
      </header>

      <div className="workspace">
        <aside className="sidebar">
          <div className="sidebar-label">WORKSPACE</div>
          <button className="nav-item nav-active"><span className="nav-icon">▦</span> Projects</button>
          <button className="nav-item" onClick={() => document.getElementById("teams-section")?.scrollIntoView({ behavior: "smooth" })}>
            <span className="nav-icon">♧</span> Teams
          </button>
          {user.platformAdmin && <button className="nav-item" onClick={() => document.getElementById("admin-section")?.scrollIntoView({ behavior: "smooth" })}>
            <span className="nav-icon">⚙</span> Administration
          </button>}
          <div className="sidebar-bottom">
            <div className="sidebar-label">YOUR ROLE</div>
            <div className="account-card"><span className="avatar">{user.email.slice(0, 1).toUpperCase()}</span>
              <span><strong>{user.platformAdmin ? "Platform admin" : "Member"}</strong><small>Local account</small></span></div>
          </div>
        </aside>

        <main className="main-content">
          <div className="page-heading">
            <div><div className="eyebrow">YOUR WORKSPACE</div><h1>Projects</h1>
              <p>Manage ontology projects and collaborate with your team.</p></div>
            <div className="heading-stat"><span className="stat-number">{projects.length}</span><span>projects</span>
              <span className="stat-divider" /><span className="stat-number">{teams.length}</span><span>teams</span></div>
          </div>

          {(error || notice) && <div className={`alert ${error ? "alert-error" : "alert-success"}`} role="status">
            <span>{error || notice}</span><button aria-label="Dismiss" onClick={() => { setError(""); setNotice(""); }}>×</button>
          </div>}

          <div className="dashboard-grid">
            <section className="panel project-list-panel">
              <div className="panel-heading"><div><h2>My projects</h2><p>Projects you can access</p></div>
                <span className="count-pill">{projects.length}</span></div>
              {projects.length ? <div className="project-list">
                {projects.map((project) => <button key={project.id}
                  className={`project-row ${selectedId === project.id ? "project-row-selected" : ""}`}
                  onClick={() => { setSelectedId(project.id); setChat([]); }}>
                  <span className="project-icon">⌘</span>
                  <span className="project-summary"><strong>{project.name}</strong>
                    <small>{project.teamId ? teams.find((team) => team.id === project.teamId)?.name ?? "Team project" : "Personal project"}</small></span>
                  <span className={`visibility-badge ${project.visibility.toLowerCase()}`}>{project.visibility.toLowerCase()}</span>
                  {project.role && <span className="role-tag">{project.role.toLowerCase()}</span>}
                </button>)}
              </div> : <div className="empty-state"><span>⌘</span><strong>No projects yet</strong><p>Create a project to start organizing ontology work.</p></div>}

              <form className="create-form" onSubmit={(event) => void createProject(event)}>
                <div className="form-caption">CREATE PROJECT</div>
                <div className="inline-form">
                  <input name="name" required maxLength={160} placeholder="Project name" aria-label="Project name" />
                  <select name="visibility" aria-label="Project visibility"><option value="PRIVATE">Private</option><option value="PUBLIC">Public</option></select>
                </div>
                <div className="inline-form">
                  <select name="teamId" aria-label="Project team">
                    <option value="">Personal project</option>
                    {teams.filter((team) => team.role === "OWNER" || team.role === "ADMIN")
                      .map((team) => <option value={team.id} key={team.id}>{team.name}</option>)}
                  </select>
                  <button className="button button-primary" type="submit">＋ Create</button>
                </div>
              </form>
            </section>

            <section className="panel detail-panel">
              {selectedProject ? <>
                <div className="detail-heading">
                  <div><div className="eyebrow">{selectedProject.teamId ? "TEAM PROJECT" : "PERSONAL PROJECT"}</div>
                    <h2>{selectedProject.name}</h2><p>{selectedProject.visibility === "PUBLIC" ? "Anyone can view this project." : "Only project members can access this project."}</p></div>
                  <span className={`visibility-badge ${selectedProject.visibility.toLowerCase()}`}>{selectedProject.visibility.toLowerCase()}</span>
                </div>
                <div className="detail-meta"><span>Your role</span><strong>{selectedProject.role?.toLowerCase() ?? "public reader"}</strong>
                  <span>Project ID</span><code>{selectedProject.id}</code>
                </div>
                {canManageProject && <details className="disclosure">
                  <summary>Project settings & members</summary>
                  <form className="settings-form" onSubmit={(event) => void updateProject(event)}>
                    <label>Project name<input name="name" defaultValue={selectedProject.name} required maxLength={160} /></label>
                    <label>Visibility<select name="visibility" defaultValue={selectedProject.visibility}><option value="PRIVATE">Private</option><option value="PUBLIC">Public</option></select></label>
                    <button className="button button-secondary" type="submit">Save settings</button>
                  </form>
                  <div className="member-list">
                    <div className="subheading"><strong>Project members</strong><button className="text-button" onClick={() => void loadProjectMembers()}>Refresh</button></div>
                    {members.map((member) => <MemberLine key={member.userId} email={member.email} role={member.role} id={member.userId} />)}
                  </div>
                  <form className="settings-form" onSubmit={(event) => void addProjectMember(event)}>
                    <label>Account email<input name="email" type="email" required placeholder="Invitee's account email" /></label>
                    <label>Project role<select name="role"><option value="ADMIN">Admin</option><option value="EDITOR">Editor</option><option value="VIEWER">Viewer</option></select></label>
                    <button className="button button-secondary" type="submit">Assign role</button>
                  </form>
                </details>}

                {activeTeam && (activeTeam.role === "OWNER" || activeTeam.role === "ADMIN") && <details className="disclosure">
                  <summary>Team members · {activeTeam.name}</summary>
                  <div className="member-list">
                    <div className="subheading"><strong>Team members</strong><button className="text-button" onClick={() => void loadTeamMembers()}>Refresh</button></div>
                    {teamMembers.map((member) => <MemberLine key={member.userId} email={member.email} role={member.role} id={member.userId} />)}
                  </div>
                  <form className="settings-form" onSubmit={(event) => void addTeamMember(event)}>
                    <label>Account email<input name="email" type="email" required placeholder="Existing account email" /></label>
                    <label>Team role<select name="role"><option value="MEMBER">Member</option>{activeTeam.role === "OWNER" && <option value="ADMIN">Admin</option>}</select></label>
                    <button className="button button-secondary" type="submit">Assign role</button>
                  </form>
                </details>}

                <div className="ontology-section">
                  <div className="subheading"><div><h3>Ontology versions</h3><p>Immutable file snapshots for this project</p></div>
                    <span className="count-pill">{versions.length}</span></div>
                  {canImport && <form className="upload-form" onSubmit={(event) => void uploadOntology(event)}>
                    <label className="file-picker"><span className="upload-icon">↑</span><span><strong>Import an ontology file</strong><small>RDF/XML or Turtle · up to 500 MiB configured</small></span>
                      <input name="file" type="file" accept=".owl,.rdf,.xml,.ttl,.turtle,application/rdf+xml,text/turtle" required /></label>
                    <button className="button button-primary" type="submit">Import file</button>
                  </form>}
                  {versions.length > 0 && <div className="reasoner-toolbar">
                    <label>Reasoner<select value={reasonerEngine} onChange={(event) => setReasonerEngine(event.target.value)}>
                      <option value="HERMIT">HermiT</option>
                    </select></label>
                    <small>Start reasoner · Ctrl+R / Cmd+R</small>
                  </div>}
                  {versions.length ? <div className="version-list">{versions.map((version) => <div className="version-entry" key={version.id}>
                    <div className="version-row">
                      <span className="file-icon">{version.format === "Turtle" ? "TTL" : "OWL"}</span>
                      <span className="version-info"><strong>{version.fileName}</strong>
                        <small>{version.axiomCount.toLocaleString()} axioms · {new Date(version.createdAt).toLocaleString()}</small>
                        {version.ontologyIri && <small className="iri">{version.ontologyIri}</small>}</span>
                      <div className="version-actions"><button className="text-button" onClick={() => void exportVersion(version)}>Download</button>
                        <button className="text-button" onClick={() => void exportVersion(version, version.format === "Turtle" ? "RDF/XML" : "Turtle")}>Convert</button>
                        {canImport && <button className="text-button" disabled={restoreBusyId !== ""} onClick={() => void restoreVersion(version)}>
                          {restoreBusyId === version.id ? "Restoring…" : "Restore"}
                        </button>}
                        <button className="text-button" disabled={reasoningBusy} onClick={() => void validateVersion(version)}>Start reasoner</button></div>
                    </div>
                    {reasoningVersionId === version.id && <div className="reasoning-panel">
                      <strong>{reasoningValidation ? `${reasoningValidation.engine} reasoning result` : "Reasoner starting…"}</strong>
                      <div className="reasoning-tabs" aria-label="Reasoning tools">
                        <button type="button" aria-pressed={reasoningTab === "reasoner"}
                          className={reasoningTab === "reasoner" ? "reasoning-tab-active" : ""}
                          onClick={() => setReasoningTab("reasoner")}>Reasoner</button>
                        <button type="button" aria-pressed={reasoningTab === "rules"}
                          className={reasoningTab === "rules" ? "reasoning-tab-active" : ""}
                          onClick={() => setReasoningTab("rules")}>Rules</button>
                      </div>
                      {reasoningTab === "reasoner" && <div>
                      {reasoningValidation && <>
                        <p>{reasoningValidation.inOwl2DlProfile ? "In OWL 2 DL profile" : "Outside OWL 2 DL profile"}
                          {reasoningValidation.consistent !== null && ` · ${reasoningValidation.consistent ? "Consistent" : "Inconsistent"}`}
                          {` · ${reasoningValidation.axiomCount.toLocaleString()} axioms · ${reasoningValidation.elapsedMillis} ms`}</p>
                        {!!reasoningValidation.profileViolations.length && <details>
                          <summary>Profile violations ({reasoningValidation.profileViolations.length})</summary>
                          <ul>{reasoningValidation.profileViolations.map((violation, index) => <li key={index}>{violation}</li>)}</ul>
                        </details>}
                        {reasoningValidation.unsatisfiableClassIris.length > 0
                          ? <details><summary>Unsatisfiable classes ({reasoningValidation.unsatisfiableClassIris.length})</summary>
                            <ul>{reasoningValidation.unsatisfiableClassIris.map((iri) => {
                              const explanation = reasoningValidation.classExplanations.find((item) => item.classIri === iri);
                              return <li key={iri}><details><summary><code>{iri}</code></summary>
                                {explanation?.axioms.length
                                  ? <ul>{explanation.axioms.map((axiom, index) => <li key={index}><code>{axiom}</code></li>)}</ul>
                                  : <small>Explanation unavailable for this class.</small>}
                              </details></li>;
                            })}</ul></details>
                          : reasoningValidation.consistent !== null && <p>No unsatisfiable named classes reported.</p>}
                        {!reasoningValidation.consistent && reasoningValidation.inconsistencyExplanationAxioms.length > 0
                          && <details><summary>Inconsistency explanation ({reasoningValidation.inconsistencyExplanationAxioms.length} axioms)</summary>
                            <ul>{reasoningValidation.inconsistencyExplanationAxioms.map((axiom, index) => <li key={index}><code>{axiom}</code></li>)}</ul>
                          </details>}
                        {reasoningValidation.inconsistencyExplanationStatus === "AXIOM_LIMIT_EXCEEDED"
                          && <p role="status">Global inconsistency explanation is unavailable because the ontology exceeds the 2,000-logical-axiom explanation limit.</p>}
                        {reasoningValidation.inconsistencyExplanationStatus === "GENERATED"
                          && reasoningValidation.inconsistencyExplanationAxioms.length === 0
                          && <p role="status">No global inconsistency explanation could be generated.</p>}
                      </>}
                      <form className="hierarchy-form" onSubmit={(event) => void queryClassHierarchy(event, version)}>
                        <input name="classIri" type="text" required maxLength={2048} placeholder="Named class IRI" aria-label="Named class IRI" />
                        <select name="direct" defaultValue="false" aria-label="Hierarchy depth">
                          <option value="false">All ancestors and descendants</option><option value="true">Direct only</option>
                        </select>
                        <button className="button button-secondary" disabled={reasoningBusy} type="submit">Query hierarchy</button>
                      </form>
                      <form className="classification-form" onSubmit={(event) => void classifyIndividual(event, version)}>
                        <input name="individualIri" type="text" required maxLength={2048} placeholder="Named individual IRI" aria-label="Named individual IRI" />
                        <button className="button button-secondary" disabled={reasoningBusy} type="submit">Classify individual</button>
                      </form>
                      {individualClassification && reasoningVersionId === version.id && <>
                        <p>{individualClassification.consistent
                          ? `Individual classification · ${individualClassification.elapsedMillis} ms`
                          : "Classification unavailable: ontology is inconsistent."}</p>
                        {individualClassification.consistent && <div className="classification-results">
                          <strong>Inferred types</strong>
                          {individualClassification.inferredTypeIris.length
                            ? <ul>{individualClassification.inferredTypeIris.map((iri) => <li key={iri}><code>{iri}</code></li>)}</ul>
                            : <small>No additional named types inferred.</small>}
                        </div>}
                      </>}
                      {classHierarchy && reasoningVersionId === version.id && <>
                        <p>{classHierarchy.consistent ? `Class hierarchy · ${classHierarchy.elapsedMillis} ms` : "Hierarchy unavailable: ontology is inconsistent."}</p>
                        {classHierarchy.consistent && <div className="hierarchy-results">
                          <div><strong>Superclasses</strong>{classHierarchy.superClassIris.length
                            ? <ul>{classHierarchy.superClassIris.map((iri) => <li key={iri}><code>{iri}</code></li>)}</ul> : <small>None</small>}</div>
                          <div><strong>Subclasses</strong>{classHierarchy.subClassIris.length
                            ? <ul>{classHierarchy.subClassIris.map((iri) => <li key={iri}><code>{iri}</code></li>)}</ul> : <small>None</small>}</div>
                        </div>}
                      </>}
                      </div>}
                      {reasoningTab === "rules" && <div>
                      <form className="swrl-form" onSubmit={(event) => void applySwrlRule(event, version)}>
                        <label>Rule (Manchester SWRL syntax)
                          <textarea name="ruleText" required maxLength={10_000} value={ruleText}
                            onChange={(event) => setRuleText(event.target.value)}
                            placeholder={"Rule: <http://example.org/Student>(?x) -> <http://example.org/Person>(?x)"} />
                        </label>
                        <div className="swrl-submit">
                          <input name="individualIri" type="text" required maxLength={2048}
                            placeholder="Target individual IRI" aria-label="Target individual IRI" />
                          <button className="button button-secondary" disabled={reasoningBusy} type="submit">Apply rule</button>
                        </div>
                        <small>Use full entity IRIs such as <code>&lt;https://example.org/Student&gt;</code>. This result includes all inferred types for the target individual after this rule run; the saved ontology version is not changed.</small>
                      </form>
                      {ruleApplication && reasoningVersionId === version.id && <>
                        <p>{ruleApplication.consistent
                          ? `Rule inference · ${ruleApplication.elapsedMillis} ms`
                          : "Rule result is inconsistent; no inferred types are shown."}</p>
                        {ruleApplication.consistent && <div className="classification-results">
                          <strong>Inferred types for <code>{ruleApplication.individualIri}</code></strong>
                          {ruleApplication.inferredTypeIris.length
                            ? <ul>{ruleApplication.inferredTypeIris.map((iri) => <li key={iri}><code>{iri}</code></li>)}</ul>
                            : <small>No named types inferred.</small>}
                        </div>}
                      </>}
                      </div>}
                    </div>}
                  </div>)}</div> : <div className="empty-versions"><span>⇧</span><p>No ontology snapshots yet.</p></div>}
                </div>
              </> : <div className="empty-detail"><span>⌘</span><h2>Select a project</h2><p>Choose a project from your workspace to view its versions and settings.</p></div>}
            </section>
          </div>

          <section className="panel teams-panel" id="teams-section">
            <div className="panel-heading"><div><h2>Your teams</h2><p>Team membership does not automatically grant access to team projects.</p></div></div>
            <div className="team-grid">
              {teams.map((team) => <div className="team-card" key={team.id}><span className="team-avatar">{team.name.slice(0, 1).toUpperCase()}</span>
                <span><strong>{team.name}</strong><small>{team.role.toLowerCase()} · {teamMembers.length && activeTeam?.id === team.id ? `${teamMembers.length} members` : "team"}</small></span>
              </div>)}
              <form className="team-create" onSubmit={(event) => void createTeam(event)}>
                <input name="name" required maxLength={120} placeholder="New team name" aria-label="New team name" />
                <button className="button button-secondary" type="submit">Create team</button>
              </form>
            </div>
          </section>

          {user.platformAdmin && <section className="panel admin-panel" id="admin-section">
            <div className="panel-heading"><div><div className="eyebrow">PLATFORM ADMIN</div><h2>Invite a user</h2>
              <p>Invitations are single-use tokens. Deliver them privately; email is not sent by this service.</p></div></div>
            <form className="invite-form" onSubmit={(event) => void issueInvitation(event)}>
              <input name="email" type="email" required placeholder="colleague@example.com" aria-label="Invitee email" />
              <button className="button button-primary" type="submit">Issue invitation</button>
            </form>
            {invitationToken && <div className="token-box"><strong>One-time invitation token</strong><code>{invitationToken}</code>
              <button className="text-button" onClick={() => void navigator.clipboard.writeText(invitationToken)}>Copy token</button></div>}
          </section>}

          <footer className="page-footer"><span>OpenProtégé · Ontology work, together.</span><span>AI suggestions are advisory and never write to a project.</span></footer>
        </main>

        <aside className={`assistant-panel ${chatOpen ? "assistant-open" : "assistant-collapsed"}`}>
          <button className="assistant-toggle" onClick={() => setChatOpen((value) => !value)} aria-label={chatOpen ? "Collapse AI assistant" : "Open AI assistant"}>
            <span className="sparkle">✳</span>{chatOpen && <span>AI assistant</span>}<span className="toggle-chevron">{chatOpen ? "›" : "‹"}</span>
          </button>
          {chatOpen && <div className="assistant-content">
            <div className="assistant-title"><span className="assistant-orb">✳</span><div><strong>Ontology assistant</strong>
              <small>{aiStatus?.configured ? `${aiStatus.provider} · ${aiStatus.model}` : "Not configured"}</small></div></div>
            <div className="assistant-messages">
              {chat.length === 0 ? <div className="chat-welcome"><span>✧</span><strong>What are you working on?</strong>
                <p>Ask for ontology design suggestions or help understanding modeling concepts.</p>
                {!aiStatus?.configured && <div className="setup-note">Ask your operator to configure <code>OPENPROTEGE_AI_ENABLED</code>, <code>OPENPROTEGE_AI_BASE_URL</code>, <code>OPENPROTEGE_AI_MODEL</code> and <code>OPENPROTEGE_AI_API_KEY</code>. The key remains server-side.</div>}
              </div> : chat.map((message, index) => <div className={`chat-message ${message.role}`} key={`${index}-${message.role}`}>
                <span>{message.role === "user" ? "You" : "Assistant"}</span><p>{message.content}</p></div>)}
              {chatBusy && <div className="chat-message assistant"><span>Assistant</span><p className="typing">Thinking…</p></div>}
            </div>
            <form className="chat-form" onSubmit={(event) => void sendChat(event)}>
              <textarea name="message" rows={3} maxLength={4000} placeholder={aiStatus?.configured ? "Ask an ontology question…" : "AI is not configured"} disabled={!aiStatus?.configured || chatBusy} required />
              <div className="chat-form-bottom"><small>Your prompt is sent to the configured model provider.</small>
                <button className="send-button" type="submit" disabled={!aiStatus?.configured || chatBusy} aria-label="Send message">↑</button></div>
            </form>
            <div className="assistant-disclaimer">Suggestions only · Review before applying</div>
          </div>}
        </aside>
      </div>
    </div>
  );
}

function MemberLine({ email, role, id }: { email: string; role: string; id: string }) {
  return <div className="member-line"><span className="avatar avatar-small">{email.slice(0, 1).toUpperCase()}</span>
    <span className="member-name"><strong>{email}</strong><code>{id}</code></span><span className="role-tag">{role.toLowerCase()}</span></div>;
}

function AuthScreen({ onSignIn, onAcceptInvitation, error, notice }: {
  onSignIn: (event: FormEvent<HTMLFormElement>) => void;
  onAcceptInvitation: (event: FormEvent<HTMLFormElement>) => void;
  error: string;
  notice: string;
}) {
  const [accepting, setAccepting] = useState(false);
  return <main className="auth-screen">
    <div className="auth-art"><div className="auth-brand"><span className="brand-mark">O</span><span>Open<span className="brand-light">Protégé</span></span></div>
      <div className="art-content"><div className="eyebrow">ONTOLOGY WORK, TOGETHER</div><h1>Build a clearer<br /><em>world of knowledge.</em></h1>
        <p>A thoughtful workspace for teams creating, exploring, and sharing structured knowledge.</p>
        <div className="art-orbit orbit-one" /><div className="art-orbit orbit-two" /><div className="art-node node-one" /><div className="art-node node-two" /><div className="art-node node-three" />
      </div><div className="art-footer">Open knowledge starts with a good question.</div>
    </div>
    <section className="auth-card-wrap"><div className="auth-card">
      <div className="auth-card-icon">⌘</div><div className="eyebrow">{accepting ? "NEW ACCOUNT" : "WELCOME BACK"}</div>
      <h2>{accepting ? "Accept your invitation" : "Sign in to your workspace"}</h2>
      <p>{accepting ? "Set a password for the account you were invited to." : "Use your local OpenProtégé account to continue."}</p>
      {(error || notice) && <div className={`alert ${error ? "alert-error" : "alert-success"}`}>{error || notice}</div>}
      {accepting ? <form className="auth-form" onSubmit={onAcceptInvitation}>
        <label>Invitation token<input name="token" required autoComplete="one-time-code" /></label>
        <label>New password<input name="password" type="password" required minLength={12} maxLength={128} autoComplete="new-password" /></label>
        <button className="button button-primary auth-submit" type="submit">Accept invitation</button>
      </form> : <form className="auth-form" onSubmit={onSignIn}>
        <label>Email address<input name="email" type="email" autoComplete="username" required /></label>
        <label>Password<input name="password" type="password" autoComplete="current-password" required /></label>
        <button className="button button-primary auth-submit" type="submit">Sign in <span>→</span></button>
      </form>}
      <button className="auth-switch" onClick={() => setAccepting((value) => !value)}>
        {accepting ? "Already have an account? Sign in" : "Have an invitation token? Accept it"}
      </button>
      <div className="auth-security"><span>♧</span> Protected by your organization’s secure sign-in</div>
    </div><div className="auth-legal">Self-hosted by your organization · Your data stays under your control</div></section>
  </main>;
}

export default App;
