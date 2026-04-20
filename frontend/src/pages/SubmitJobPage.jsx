import { Link, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { uploadFile } from "../api/upload_file";
import { runJob } from "../api/run_job";
import { updateDB } from "../api/update_db";
import { removeFile } from "../api/remove_uploaded_file";

const API_BASE = "http://localhost:8080/api";

export default function SubmitJobPage() {
  const [file, setFile] = useState(null);
  const [status, setStatus] = useState("");
  const [graders, setGraders] = useState([]);
  const [selectedGrader, setSelectedGrader] = useState("");
  const navigate = useNavigate();

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
    setFile(e.target.files[0]);
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
      setStatus("Uploading...");
      const message = await uploadFile(file, selectedGrader);
      setStatus(message.message);
      navigate("/jobs");
      const jobResponse = await runJob(message.id, file.name);
      const jobResults = await jobResponse.json();

      await updateDB(message.id, jobResults);
      await removeFile(file.name);

      navigate("/jobs");
    } catch (err) {
      setStatus(err.message);
    }
  };

  return (
    <div className="jobs-page">
      <div className="jobs-board-shell">
        <div className="top-bar">
          <h1 className="page-title">Submit Job</h1>
          <Link to="/jobs" className="button nav-button">
            Back to Jobs
          </Link>
        </div>
        <div className="card">
          <div className="form-group">
            <label className="label">Select Grader</label>
            <select
              className="input"
              value={selectedGrader}
              onChange={handleGraderChange}
            >
              <option value="">Select a grader</option>
              {graders.map((grader) => (
                <option key={grader.key} value={grader.key}>
                  {grader.label}
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label className="label">Upload Submission</label>
            <input
              className="input"
              type="file"
              onChange={handleFileChange}
            />
          </div>
          <button className="button" onClick={handleSubmit}>
            Submit Job
          </button>

          {status && <p>{status}</p>}
        </div>
      </div>
    </div>
  );
}