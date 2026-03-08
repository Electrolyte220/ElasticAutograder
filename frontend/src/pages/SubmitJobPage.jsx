import { Link } from "react-router-dom";

export default function SubmitJobPage() {
  return (
    <div className="jobs-page">
      <div className="jobs-board-shell">
        <div className="top-bar">
          <h1 className="page-title">Submit Job</h1>
          <Link to="/" className="button nav-button">
            Back to Jobs
          </Link>
        </div>

        <div className="card">
          <div className="form-group">
            <label className="label">Assignment ID</label>
            <input className="input" type="text" placeholder="e.g. hw1" />
          </div>

          <div className="form-group">
            <label className="label">Grader Image</label>
            <input className="input" type="text" placeholder="e.g. python-grader:latest" />
          </div>

          <div className="form-group">
            <label className="label">Upload Submission</label>
            <input className="input" type="file" disabled />
          </div>

          <button className="button" disabled>
            Submit Job
          </button>
        </div>
      </div>
    </div>
  );
}