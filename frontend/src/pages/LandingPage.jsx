import { useNavigate } from "react-router-dom";

export default function LandingPage() {
  const navigate = useNavigate();

  return (
    <div style={{
      minHeight: "100vh",
      display: "flex",
      flexDirection: "column",
      justifyContent: "center",
      alignItems: "center",
      padding: "0px 24px",
      background: "var(--bg)",
      color: "var(--text)",
      textAlign: "center",
      marginTop: "-60px"
    }}>
      <h1 style={{
        fontSize: "4rem",
        fontWeight: "800",
        marginBottom: "16px",
        color: "var(--accent)"
      }}>
        Elastic Autograder
      </h1>

      <p style={{
        fontSize: "1.25rem",
        color: "var(--muted)",
        marginBottom: "48px",
        maxWidth: "600px"
      }}>
        A scalable, automated grading pipeline for course staff. Upload submissions, run graders, and view results in real time.
      </p>

      <div style={{
        display: "flex",
        gap: "24px",
        marginBottom: "48px",
        flexWrap: "wrap",
        justifyContent: "center"
      }}>
        <div style={{
          background: "var(--surface)",
          border: "1px solid var(--border)",
          borderRadius: "12px",
          padding: "24px",
          width: "200px"
        }}>
          <div style={{ fontSize: "2rem", marginBottom: "8px" }}>⚡</div>
          <h3 style={{ margin: "0 0 8px 0" }}>Fast Grading</h3>
          <p style={{ color: "var(--muted)", margin: 0, fontSize: "0.9rem" }}>
            Jobs are queued and executed automatically with real-time status updates.
          </p>
        </div>

        <div style={{
          background: "var(--surface)",
          border: "1px solid var(--border)",
          borderRadius: "12px",
          padding: "24px",
          width: "200px"
        }}>
          <div style={{ fontSize: "2rem", marginBottom: "8px" }}>🔒</div>
          <h3 style={{ margin: "0 0 8px 0" }}>Secure Execution</h3>
          <p style={{ color: "var(--muted)", margin: 0, fontSize: "0.9rem" }}>
            Student code runs in isolated containers, keeping your system safe.
          </p>
        </div>

        <div style={{
          background: "var(--surface)",
          border: "1px solid var(--border)",
          borderRadius: "12px",
          padding: "24px",
          width: "200px"
        }}>
          <div style={{ fontSize: "2rem", marginBottom: "8px" }}>📊</div>
          <h3 style={{ margin: "0 0 8px 0" }}>Real-time Results</h3>
          <p style={{ color: "var(--muted)", margin: 0, fontSize: "0.9rem" }}>
            View scores, test results, and error messages as soon as grading completes.
          </p>
        </div>
      </div>

      <button
        className="button"
        style={{ fontSize: "1.1rem", padding: "14px 32px" }}
        onClick={() => navigate("/jobs")}
      >
        Get Started
      </button>
    </div>
  );
}