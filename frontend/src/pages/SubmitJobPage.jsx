import { Link, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { uploadFile } from "../api/upload_file";
import { runJob } from "../api/run_job";
import { updateDB } from "../api/update_db";

const API_BASE = "http://localhost:8080/api";

export default function SubmitJobPage() {
  const [file, setFile] = useState(null);
  const [status, setStatus] = useState("");
  const [graders, setGraders] = useState([]);
  const [selectedGrader, setSelectedGrader] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();

  const selectedGraderInfo = graders.find(
    (grader) => grader.key === selectedGrader
  );

  useEffect(() => {
    const fetchGraders = async () => {
      try {
        const response = await fetch(`${API_BASE}/graders`);
        if (!response.ok) {
          throw new Error("Failed to load graders.");
        }

        const graderOptions = await response.json();
        setGraders(graderOptions);
      } catch (err) {
        setStatus(err.message);
      }
    };

    fetchGraders();
  }, []);

  const handleFileChange = (e) => {
    setFile(e.target.files[0] ?? null);
  };

  const handleGraderChange = (e) => {
    setSelectedGrader(e.target.value);
  };

  const handleSubmit = async () => {
    if (!file) {
      setStatus("Please select a file to upload.");
      return;
    }

    if (!selectedGrader) {
      setStatus("Please select a grader.");
      return;
    }

    try {
      setIsSubmitting(true);
      setStatus("Uploading submission...");

      const message = await uploadFile(file, selectedGrader);
      setStatus("Submission uploaded. Starting grader...");

      navigate("/jobs");

      const jobResponse = await runJob(message.id, file.name);
      const jobResults = await jobResponse.json();

      await updateDB(message.id, jobResults);
    } catch (err) {
      setStatus(err.message || "Failed to submit job.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="submit-page">
  <div className="submit-shell compact-submit-shell">
    <div className="submit-header compact-submit-header">
      <h1 className="page-title">Submit Job</h1>
      <Link to="/jobs" className="button nav-button">
        Back to Jobs
      </Link>
    </div>

    <div className="card submit-card compact-submit-card">
      <div className="submit-form-grid compact-submit-form-grid">
        <div className="form-group">
          <label className="label" htmlFor="grader-select">
            Select Grader
          </label>
          <select
            id="grader-select"
            className="input"
            value={selectedGrader}
            onChange={handleGraderChange}
            disabled={isSubmitting}
          >
            <option value="">Select a grader</option>
            {graders.map((grader) => (
              <option key={grader.key} value={grader.key}>
                {grader.label}
              </option>
            ))}
          </select>
        </div>

        {selectedGraderInfo?.description && (
          <div className="grader-description-card compact-description-card">
            <div className="grader-description-title">
              {selectedGraderInfo.label}
            </div>
            <p className="grader-description-text">
              {selectedGraderInfo.description}
            </p>
          </div>
        )}

        <div className="form-group">
          <label className="label" htmlFor="submission-file">
            Upload Submission
          </label>
          <input
            id="submission-file"
            className="file-input"
            type="file"
            onChange={handleFileChange}
            disabled={isSubmitting}
          />
          <div className="file-meta">
            <span className="file-meta-label">Selected file:</span>
            <span className="file-meta-name">
              {file ? file.name : "No file selected"}
            </span>
          </div>
        </div>

        <div className="submit-actions compact-submit-actions">
          <button
            className="button submit-primary-button"
            onClick={handleSubmit}
            disabled={isSubmitting}
          >
            {isSubmitting ? "Submitting..." : "Submit Job"}
          </button>
        </div>

        {status && <div className="submit-status-message">{status}</div>}
      </div>
    </div>
  </div>
</div>
  );
}