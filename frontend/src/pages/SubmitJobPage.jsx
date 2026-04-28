import { Link, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { uploadFile } from "../api/upload_file";
import { runJob } from "../api/run_job";
import { fetchGraders } from "../api/jobs";

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
    const loadGraders = async () => {
      try {
        setGraders(await fetchGraders());
      } catch (err) {
        setStatus(err.message);
      }
    };

    loadGraders();
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

      const uploadResponse = await uploadFile(file, selectedGrader);
      const jobs = Array.isArray(uploadResponse.jobs) ? uploadResponse.jobs : [];

      if (jobs.length === 0) {
        throw new Error("Upload did not return any jobs.");
      }

      setStatus(
        jobs.length === 1
          ? "Submission uploaded. Starting grader..."
          : `Batch uploaded. Starting ${jobs.length} graders...`
      );

      navigate("/jobs");

      await Promise.all(
        jobs.map(async (job) => {
          await runJob(job.id, job.fileName);
        })
      );
    } catch (err) {
      setStatus(err.message || "Failed to submit job.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="submit-page">
      <div className="submit-shell submit-dashboard-shell">
        <div className="submit-header submit-dashboard-header">
          <div className="submit-header-text">
            <h1 className="page-title">Submit Job</h1>
            <p className="submit-subtitle">
              Choose a grader, review the problem notes, and upload your submission in one place.
            </p>
          </div>
          <Link to="/jobs" className="button nav-button">
            Back to Jobs
          </Link>
        </div>

        <div className="card submit-card submit-dashboard-card">
          <div className="submit-dashboard-grid">
            <aside className="submit-panel submit-info-panel">
              {selectedGraderInfo ? (
                <>
                  <div className="submit-panel-header">
                    <span className="submit-panel-eyebrow">Selected grader</span>
                    <h2 className="submit-panel-title">{selectedGraderInfo.label}</h2>
                    <p className="submit-panel-copy">{selectedGraderInfo.summary}</p>
                  </div>

                  {selectedGraderInfo.details?.length > 0 && (
                    <ul className="submit-details-list">
                      {selectedGraderInfo.details.map((detail, index) => (
                        <li key={`${selectedGraderInfo.key}-detail-${index}`} className="submit-details-item">
                          {detail}
                        </li>
                      ))}
                    </ul>
                  )}
                </>
              ) : (
                <div className="submit-empty-state">
                  <span className="submit-panel-eyebrow">Problem details</span>
                  <h2 className="submit-panel-title">Pick a grader to preview the task</h2>
                  <p className="submit-panel-copy">
                    You’ll see a short summary plus extra notes here so it is easier to confirm what the grader expects before you upload.
                  </p>
                </div>
              )}
            </aside>

            <section className="submit-panel submit-form-panel">
              <div className="submit-panel-header">
                <h2 className="submit-panel-title">Choose a grader</h2>
                <p className="submit-panel-copy">
                  Pick the problem you want to run so the correct grading container and checks are used.
                </p>
              </div>

              <div className="form-group">
                <label className="label" htmlFor="grader-select">
                  Grader
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

              <div className="submit-panel-header submit-upload-header">
                <h2 className="submit-panel-title">Upload your submission</h2>
                <p className="submit-panel-copy">
                  Select a single source file or a `.zip` archive of submissions. The upload will finish first, then each job will start automatically.
                </p>
              </div>

              <label className="submit-upload-card" htmlFor="submission-file">
                <span className="submit-upload-title">Choose a file</span>
                <span className="submit-upload-copy">
                  {file
                    ? "Ready to upload the selected submission or batch archive."
                    : "Browse for the source file or zip archive you want to grade."}
                </span>
                <input
                  id="submission-file"
                  className="submit-upload-input"
                  type="file"
                  accept=".py,.zip"
                  onChange={handleFileChange}
                  disabled={isSubmitting}
                />
              </label>

              <div className="file-meta submit-file-meta">
                <span className="file-meta-label">Selected file:</span>
                <span className="file-meta-name">{file ? file.name : "No file selected"}</span>
              </div>

              <div className="submit-actions submit-dashboard-actions">
                <button
                  className="button submit-primary-button"
                  onClick={handleSubmit}
                  disabled={isSubmitting}
                >
                  {isSubmitting ? "Submitting..." : "Submit Job"}
                </button>
              </div>

              {status && <div className="submit-status-message">{status}</div>}
            </section>
          </div>
        </div>
      </div>
    </div>
  );
}
