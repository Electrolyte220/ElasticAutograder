import { useEffect, useState, useRef } from "react";
import { Link } from "react-router-dom";
import { fetchRecentJobs, fetchGraders } from "../api/jobs";
import JobsTable from "../components/JobsTable";

const REFRESH_INTERVAL = 1000;

export default function JobsBoard() {
  const gridRef = useRef(null);
  const refreshInterval = useRef(null);

  const [jobs, setJobs] = useState([]);
  const [graders, setGraders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  async function load(isInitial = false) {
    try {
      if (isInitial) {
        setLoading(true);
        setError("");
      }

      if (isInitial) {
        const [jobsData, gradersData] = await Promise.all([
          fetchRecentJobs(),
          fetchGraders()
        ]);

        setJobs(jobsData);
        setGraders(gradersData);
      } else {
        const jobsData = await fetchRecentJobs();
        setJobs(jobsData);
      }
    } catch (err) {
      setError(err.message || "Failed to load jobs.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load(true);

    refreshInterval.current = setInterval(() => {
      load(false);
    }, REFRESH_INTERVAL);

    return () => clearInterval(refreshInterval.current);
  }, []);

  return (
    <div className="jobs-page">
      <div className="jobs-board-shell">
        <div className="jobs-top-bar">
          <div className="jobs-top-bar-text">
            <h1 className="page-title">Recent Jobs</h1>
            <p className="jobs-subtitle">Track grading progress and review completed runs.</p>
          </div>

          <div className="jobs-top-bar-actions">
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
        {!loading && !error && jobs.length === 0 && (
          <p className="muted">No jobs found.</p>
        )}

        {!loading && !error && jobs.length > 0 && (
          <div className="card jobs-board-card">
            <JobsTable jobs={jobs} graders={graders} gridRef={gridRef} />
          </div>
        )}
      </div>
    </div>
  );
}