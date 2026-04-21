import { useEffect, useState, useRef } from "react";
import { Link, useLocation, useSearchParams } from "react-router-dom";
import { fetchJobsInRange, fetchRecentJobs } from "../api/jobs";
import JobsTable from "../components/JobsTable";

const REFRESH_INTERVAL = 1000;

export default function JobsBoard() {
  const gridRef = useRef(null);
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const refreshInterval = useRef(null)
  const [searchParams] = useSearchParams();
  const location = useLocation();
  const headerName = location.pathname === '/multi-submission'
          ? "Submission Details" : "Recent Jobs";

  async function load(isInitial = false) {
    try {
      if (isInitial) setLoading(true);

      let data;
      if (location.pathname === "/multi-submission") {
        const minId = searchParams.get("from");
        const maxId = searchParams.get("to");
        data = await fetchJobsInRange(minId, maxId);
      } else {
        data = await fetchRecentJobs();
      }

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
  }, [location]);

  return (
    <div className="jobs-page" style={{ overflow: "hidden" }}>
      <div className="jobs-board-shell">
        <div className="top-bar">
          <h1 className="page-title">{headerName}</h1>
          {location.pathname !== '/multi-submission' && (
              <Link to="/submit" className="button nav-button">
                 New Job
              </Link>
              )}
          {location.pathname === '/multi-submission' && (
              <Link to="/jobs" className="button nav-button">
                  Back to Jobs
              </Link>
              )}
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