import { useEffect, useState, useRef } from "react";
import { Link } from "react-router-dom";
import { fetchRecentJobs } from "../api/jobs";
import JobsTable from "../components/JobsTable";

const REFRESH_INTERVAL = 1000;

export default function JobsBoard() {
  const gridRef = useRef(null);
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const refreshInterval = useRef(null)

  async function load(isInitial = false) {
    try {
      if (isInitial) setLoading(true);
      const data = await fetchRecentJobs();

      if(isInitial || !gridRef.current?.api) {
        setJobs(data);
      } else {
        gridRef.current.api.applyTransactionAsync({update: data})
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
      if (isInitial) isInitial = false;
    }
  }

  useEffect(() => {
    load(true);
    refreshInterval.current = setInterval(() => load(false), REFRESH_INTERVAL)
    return () => clearInterval(refreshInterval.current)
  }, []);

  return (
    <div className="jobs-page" style={{ overflow: "hidden" }}>
      <div className="jobs-board-shell">
        <div className="top-bar">
          <h1 className="page-title">Recent Jobs</h1>
          <div style={{ display: "flex", gap: "8px" }}>
            <Link to="/" className="button nav-button">
              Home
            </Link>
            <Link to="/submit" className="button nav-button">
              New Job
            </Link>
          </div>
        </div>
        {loading && <p>Loading jobs...</p>}
        {error && <p className="status-failed">{error}</p>}
        {!loading && !error && jobs.length === 0 && <p className="muted">No jobs found.</p>}
        {!loading && !error && jobs.length > 0 && (
          <div className="card jobs-board-card">
            <JobsTable jobs={jobs} gridRef={gridRef}/>
          </div>
        )}
      </div>
    </div>
  );
}